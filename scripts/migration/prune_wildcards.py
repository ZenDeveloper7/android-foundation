#!/usr/bin/env python3
"""Step 2/4 helper: remove wildcard imports of project packages a module can no longer see.

    python3 prune_wildcards.py --config migration.json

After extraction, `import com.example.feature.x.*` in a module that doesn't depend on the module
owning that package breaks the build ("unresolved reference"). This removes only wildcard
imports whose package exists in no module visible to the importing file (its module_deps.json
dependencies, transitively, plus feature_implicit_deps for features).
"""
import collections
import json
import re

from config import package_of, parse

cfg, _ = parse(__doc__.splitlines()[0])
deps = json.load(open(cfg.output("module_deps.json")))
assign = json.load(open(cfg.output("assignment.json")))
modules = [cfg.source_module] + sorted(set(assign.values()) - {cfg.source_module})
owners = collections.defaultdict(set)
for m in modules:
    for f in cfg.module_source_root(m).rglob("*.kt"):
        owners[package_of(f.read_text())].add(m)


def visible(m):
    seen, stack = {m}, [m]
    while stack:
        for d in deps.get(stack.pop(), []):
            if d not in seen:
                seen.add(d)
                stack.append(d)
    if m.startswith("feature:"):
        seen |= cfg.feature_implicit_deps
    return seen


removed = 0
G = re.escape(cfg.base_package)
for m in modules:
    vis = visible(m)
    for f in cfg.module_source_root(m).rglob("*.kt"):
        s = o = f.read_text()
        for imp in re.findall(r"^import (" + G + r"[\w.]*)\.\*\n", s, re.M):
            if not owners.get(imp, set()) & vis:
                s = s.replace(f"import {imp}.*\n", "")
                removed += 1
        if s != o:
            f.write_text(s)
print("removed wildcard imports:", removed)
