#!/usr/bin/env python3
"""Step 4: rename packages so each module owns its own package tree.

    python3 rename_packages.py --config migration.json           dry run: mapping + collisions
    python3 rename_packages.py --config migration.json --apply   rewrite and move files

For each main-source file, the first `package_rules` entry whose `module` regex matches the file's
module and whose `pattern` matches its package gives the new package (`{module_name}` is the part
after the colon; files with no matching rule keep their package). Aborts on collisions (two
declarations with the same name in one new package). With --apply it rewrites package lines,
fully-qualified references and imports (expanding wildcard imports of split packages), adds
imports for same-package references that moved, moves files, and updates manifest class names
and ProGuard `-keep class pkg.**` rules. Test files follow the package of what they test.
Afterwards: clean the build (stale KSP output), compile, run tests.
"""
import collections
import re

from config import package_of, parse, top_level_names

cfg, args = parse(__doc__.splitlines()[0], lambda p: p.add_argument("--apply", action="store_true"))
REPO, G = cfg.repo, cfg.base_package


def module_of(path):
    parts = path.relative_to(REPO).parts
    i = parts.index("src")
    return cfg.source_module if i == 1 and parts[0] == cfg.source_module else ":".join(parts[:i])


def source_set(path):
    parts = path.relative_to(REPO).parts
    return parts[parts.index("src") + 1]


def new_package(module, pkg):
    for module_rx, pattern, replace in cfg.package_rules:
        if module_rx.search(module):
            m = pattern.match(pkg)
            if m:
                name = module.split(":")[-1]
                return re.sub(r"\\(\d)", lambda g: m.group(int(g.group(1))) or "", replace.replace("{module_name}", name))
    return pkg


files = [p for p in REPO.rglob("*.kt")
         if "src" in p.relative_to(REPO).parts and not {"build", "build-logic"} & set(p.relative_to(REPO).parts)]
text = {p: p.read_text() for p in files}
pkg = {p: package_of(s) for p, s in text.items()}
main = [p for p in files if source_set(p) == "main" and pkg[p].startswith(G)]
newpkg = {p: new_package(module_of(p), pkg[p]) for p in main}

sym, collide = {}, collections.defaultdict(set)
for p in main:
    for n in top_level_names(text[p]):
        sym[(pkg[p], n)] = newpkg[p]
        collide[(newpkg[p], n)].add(str(p.relative_to(REPO)))
collisions = {k: v for k, v in collide.items() if len(v) > 1}

for p in files:  # tests and other source sets follow their subject
    if p in newpkg:
        continue
    words = set(re.findall(r"\b\w+\b", text[p]))
    targets = {sym[(pkg[p], w)] for w in words if (pkg[p], w) in sym}
    olds = sorted({np for (op, _), np in sym.items() if op == pkg[p]})
    newpkg[p] = targets.pop() if len(targets) == 1 else (olds[0] if olds else pkg[p])

old_to_new = collections.defaultdict(set)
for (op, _), np in sym.items():
    old_to_new[op].add(np)
moves = collections.Counter((pkg[p], newpkg[p]) for p in main if pkg[p] != newpkg[p])
print(f"files: {len(files)}  changing package: {sum(1 for p in files if newpkg[p] != pkg[p])}")
for (a, b), n in sorted(moves.items()):
    print(f"  {a:60} -> {b}  ({n})")
print(f"collisions: {len(collisions)}")
for k, v in list(collisions.items())[:30]:
    print("  ", k, sorted(v))
if collisions:
    raise SystemExit(1)
if not args.apply:
    raise SystemExit(0)

FQN = re.compile(r"\b(" + re.escape(G) + r"(?:\.\w+)+)\b")


def remap(fq):
    parts = fq.split(".")
    for i in range(len(parts) - 1, 1, -1):
        key = (".".join(parts[:i]), parts[i])
        if key in sym:
            return ".".join([sym[key]] + parts[i:])
    return fq


for p in files:
    s, op, np = text[p], pkg[p], newpkg[p]
    s = re.sub(r"^package\s+[\w.]+", f"package {np}", s, count=1, flags=re.M)
    out = []
    for line in s.split("\n"):
        m = re.match(r"^import\s+(" + re.escape(G) + r"[\w.]*)\.\*\s*$", line)
        if m:
            out += [f"import {t}.*" for t in sorted(old_to_new.get(m.group(1), {m.group(1)})) if t != np]
            continue
        if not line.startswith("package"):
            line = FQN.sub(lambda mm: remap(mm.group(1)), line)
        out.append(line)
    s = "\n".join(out)
    body_words = set(re.findall(r"\b\w+\b", re.sub(r"^import .*$", "", s, flags=re.M)))
    extra = {f"import {sym[(op, w)]}.{w}" for w in body_words - top_level_names(text[p])
             if (op, w) in sym and sym[(op, w)] != np}
    if extra:
        s = re.sub(r"(^package [^\n]+\n)", lambda m: m.group(1) + "\n" + "\n".join(sorted(extra)) + "\n", s, count=1, flags=re.M)
    s = re.sub(r"^import ([\w.]+)\.(\w+)\n", lambda m: "" if m.group(1) == np else m.group(0), s, flags=re.M)
    seen, lines = set(), []
    for line in s.split("\n"):
        if line.startswith("import "):
            if line in seen:
                continue
            seen.add(line)
        lines.append(line)
    s = "\n".join(lines)
    parts = p.relative_to(REPO).parts
    root = REPO.joinpath(*parts[: parts.index("src") + 3])
    target = root / np.replace(".", "/") / p.name
    target.parent.mkdir(parents=True, exist_ok=True)
    if target != p:
        p.unlink()
    target.write_text(s)

for d in sorted(REPO.rglob("*"), key=lambda q: -len(q.parts)):
    rel_parts = d.relative_to(REPO).parts
    if d.is_dir() and "src" in rel_parts and not {"build", ".git"} & set(rel_parts) and not any(d.iterdir()):
        d.rmdir()


def fix_name(m):
    name = m.group(1)
    return f'android:name="{remap(G + name if name.startswith(".") else name)}"'


for manifest in REPO.glob("**/src/main/AndroidManifest.xml"):
    if "build" in manifest.relative_to(REPO).parts:
        continue
    s = manifest.read_text()
    manifest.write_text(re.sub(r'android:name="(\.[\w.]+|' + re.escape(G) + r'\.[\w.]+)"', fix_name, s))
for rules in REPO.glob("**/proguard-rules.pro"):
    s = rules.read_text()
    for op, nps in old_to_new.items():
        if len(nps) == 1:
            s = s.replace(f"{op}.**", f"{next(iter(nps))}.**")
        elif f"{op}.**" in s:
            print(f"CHECK {rules.relative_to(REPO)}: {op}.** now spans {sorted(nps)}; add a rule per package")
    rules.write_text(s)
print("applied; now clean the build, compile and run the tests")
