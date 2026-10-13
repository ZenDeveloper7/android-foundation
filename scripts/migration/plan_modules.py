#!/usr/bin/env python3
"""Step 1 (dry run, changes nothing): assign every Kotlin file of the source module to a module.

    python3 plan_modules.py --config migration.json [--show 60]

Anchors (regexes on the path relative to the base package; the first match wins, and a null
module means "place by usage") and overrides decide the obvious files. The rest are placed by who references them, repeated until stable: used by exactly one
feature -> that feature; used by several features or by core -> the shared module. Prints module
counts and every dependency-direction violation, and writes assignment.json and module_deps.json
next to the config. Iterate on the config until there are no violations.
"""
import collections
import json
import re

from config import package_of, parse, top_level_names

cfg, args = parse(__doc__.splitlines()[0], lambda p: p.add_argument("--show", type=int, default=60))
BASE = cfg.base_dir
if not BASE.is_dir():
    raise SystemExit(f"no sources at {BASE}")


def rel(path):
    return str(path.relative_to(BASE))


def anchor(r):
    if r in cfg.overrides:
        return cfg.overrides[r]
    for rx, module in cfg.anchors:
        if rx.search(r):
            return module
    return None


files = sorted(BASE.rglob("*.kt"))
text = {rel(f): f.read_text() for f in files}
pkg = {r: package_of(s) for r, s in text.items()}
by_fq, by_pkg_name = collections.defaultdict(set), collections.defaultdict(set)
for r, s in text.items():
    for n in top_level_names(s):
        by_fq[f"{pkg[r]}.{n}"].add(r)
        by_pkg_name[(pkg[r], n)].add(r)

uses = collections.defaultdict(set)
for r, s in text.items():
    body = re.sub(r"//.*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"", "", s, flags=re.S)
    for imp in re.findall(r"^import\s+([\w.]+)(?:\s+as\s+\w+)?", s, re.M):
        uses[r] |= by_fq.get(imp, set()) - {r}
    for w in set(re.findall(r"\b[A-Za-z_]\w*\b", body)):
        uses[r] |= by_pkg_name.get((pkg[r], w), set()) - {r}
users = collections.defaultdict(set)
for r, targets in uses.items():
    for t in targets:
        users[t].add(r)

unknown = {m for m in set(cfg.overrides.values()) | {m for _, m in cfg.anchors} if m and m not in cfg.levels}
if unknown:
    raise SystemExit(f"modules missing from module_levels: {sorted(unknown)}")

assign = {r: anchor(r) for r in text}
for _ in range(50):
    changed = False
    for r in text:
        if anchor(r) is not None:
            continue
        mods = {assign[u] for u in users[r] if assign.get(u)}
        feats = {m for m in mods if m.startswith("feature:")}
        lower = {m for m in mods if not m.startswith("feature:") and m != cfg.source_module}
        if lower or len(feats) > 1 or (not feats and mods):
            new = cfg.shared_module
        elif len(feats) == 1:
            new = next(iter(feats))
        else:
            new = assign[r]
        if new != assign[r]:
            assign[r], changed = new, True
    if not changed:
        break
for r, m in assign.items():
    if m is None:
        assign[r] = cfg.shared_module

level = cfg.levels
violations = []
deps = collections.defaultdict(set)
for r, targets in uses.items():
    for t in targets:
        a, b = assign[r], assign[t]
        if a == b:
            continue
        deps[a].add(b)
        if level[b] > level[a] or (a.startswith("feature:") and b.startswith("feature:")):
            violations.append((a, r, b, t))

print(json.dumps(collections.Counter(assign.values()), indent=1, sort_keys=True))
print(f"violations: {len(violations)}")
for a, r, b, t in sorted(violations)[: args.show]:
    print(f"  {a:20} {r}\n  {'':20}   uses {b}: {t}")
json.dump(assign, open(cfg.output("assignment.json"), "w"), indent=1, sort_keys=True)
json.dump({k: sorted(v) for k, v in deps.items()}, open(cfg.output("module_deps.json"), "w"), indent=1, sort_keys=True)
print(f"wrote {cfg.output('assignment.json')} and module_deps.json")
