#!/usr/bin/env python3
"""Step 2: move files into modules per assignment.json, keeping package names.

    python3 extract_modules.py --config migration.json            dry run (prints the plan)
    python3 extract_modules.py --config migration.json --apply    move files and write build files

Moves Kotlin files with `git mv`, moves resources to the resource module, points `R` imports at
the resource module's R, gives modules that read BuildConfig their own, writes each module's
build.gradle.kts from the convention plugins and detected dependencies, and registers modules in
settings.gradle.kts. Review every generated build file; SDK-specific fields are not guessed.
"""
import json
import re
import subprocess

from config import parse

cfg, args = parse(__doc__.splitlines()[0], lambda p: p.add_argument("--apply", action="store_true"))
assign = json.load(open(cfg.output("assignment.json")))
usage = json.load(open(cfg.output("module_deps.json")))
BASE = cfg.base_dir
G = cfg.base_package
modules = sorted(set(assign.values()) - {cfg.source_module})


def git_mv(src, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["git", "mv", str(src), str(dst)], cwd=cfg.repo, check=True)


moves = [(BASE / r, cfg.module_source_root(m) / G.replace(".", "/") / r) for r, m in assign.items() if m != cfg.source_module]
print(f"{len(moves)} files into {len(modules)} modules: {modules}")
if not args.apply:
    for m in modules:
        print(f"  {m:24} {sum(1 for a in assign.values() if a == m)} files, deps {usage.get(m, [])}")
    raise SystemExit(0)

for src, dst in moves:
    git_mv(src, dst)

# R and BuildConfig imports
DS_R = f"{cfg.namespace(cfg.resource_module)}.R"
needs_buildconfig = set()
for m in [cfg.source_module] + modules:
    for f in cfg.module_source_root(m).rglob("*.kt"):
        s = o = f.read_text()
        s = s.replace(f"import {G}.R\n", f"import {DS_R}\n")
        if m != cfg.source_module and f"import {G}.BuildConfig\n" in s:
            s = s.replace(f"import {G}.BuildConfig\n", f"import {cfg.namespace(m)}.BuildConfig\n")
            needs_buildconfig.add(m)
        if re.search(rf"^package {re.escape(G)}$", s, re.M) and re.search(r"\bR\.\w", s) and DS_R not in s:
            s = re.sub(r"(^package [^\n]+\n\n?)", r"\1" + f"import {DS_R}\n", s, count=1, flags=re.M)
        if s != o:
            f.write_text(s)
for f in (cfg.repo / cfg.source_module / "src").rglob("*.kt"):
    s = f.read_text()
    if f"import {G}.R\n" in s:
        f.write_text(s.replace(f"import {G}.R\n", f"import {DS_R}\n"))

# resources
res = cfg.repo / cfg.source_module / "src/main/res"
target_res = cfg.module_dir(cfg.resource_module) / "src/main/res"
for d in sorted(p for p in res.iterdir() if p.is_dir()):
    if cfg.keep_resource_dirs.match(d.name):
        continue
    for f in sorted(d.iterdir()):
        if d.name.startswith("values") and cfg.keep_resource_files.match(f.name):
            continue
        git_mv(f, target_res / d.name / f.name)

# build files
for m in modules:
    text = "\n".join(f.read_text() for f in cfg.module_source_root(m).rglob("*.kt"))
    feature = m.startswith("feature:")
    p = cfg.plugin_prefix
    plugins = [f"alias(libs.plugins.{p}.android.feature)"] if feature else [f"alias(libs.plugins.{p}.android.library)"]
    if not feature:
        if re.search(r"^import androidx\.compose\.", text, re.M):
            plugins.append(f"alias(libs.plugins.{p}.android.compose)")
        if re.search(r"^import (dagger\.|javax\.inject\.)", text, re.M):
            plugins.append(f"alias(libs.plugins.{p}.hilt)")
        if "@Serializable" in text:
            plugins.append("alias(libs.plugins.kotlin.serialization)")
    conf = "implementation" if feature else "api"
    lines = {f"    {conf}(projects.{d.replace(':', '.')})" for d in usage.get(m, [])
             if d not in (m, cfg.source_module) and not (feature and d in cfg.feature_implicit_deps)}
    lines |= {f"    {conf}({acc})" for rx, acc in cfg.library_imports if rx.search(text)}
    android = [f'    namespace = "{cfg.namespace(m)}"']
    if m in needs_buildconfig:
        android.append("    buildFeatures {\n        buildConfig = true\n    }\n    // TODO: move the buildConfigField(...) values this module reads")
    body = "plugins {\n" + "\n".join("    " + x for x in plugins) + "\n}\n\nandroid {\n" + "\n".join(android) + "\n}\n"
    if lines:
        body += "\ndependencies {\n" + "\n".join(sorted(lines)) + "\n}\n"
    (cfg.module_dir(m) / "build.gradle.kts").write_text(body)

settings = cfg.repo / "settings.gradle.kts"
s = settings.read_text()
s = s.replace(f'include(":{cfg.source_module}")\n', f'include(":{cfg.source_module}")\n' + "".join(f'include(":{m}")\n' for m in modules if f'":{m}"' not in s))
settings.write_text(s)
print("BuildConfig needed in:", sorted(needs_buildconfig))
print(f"Next: add `implementation(projects.<module>)` for each module to {cfg.source_module}/build.gradle.kts, then compile module by module.")
