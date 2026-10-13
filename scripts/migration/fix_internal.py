#!/usr/bin/env python3
"""Step 3 helper: make public exactly the `internal` declarations a compile reports as inaccessible.

    ./gradlew :core:ui:compileDebugKotlin > build.log 2>&1
    python3 fix_internal.py --config migration.json build.log

Reads "Cannot access '...': it is internal" errors and removes `internal` from declarations with
those names, nothing else. Exits 1 when nothing changed. Run, recompile, repeat.
"""
import re

from config import parse

cfg, args = parse(__doc__.splitlines()[0], lambda p: p.add_argument("log"))
log = open(args.log).read()
names = set()
for m in re.finditer(r"Cannot access '([^']+)': it is internal", log):
    n = re.search(r"\b(?:fun|class|interface|object|val|var|typealias)\s+(?:<[^>]*>\s+)?(?:[\w.<>?, ]+\.)?(\w+)", m.group(1))
    if n:
        names.add(n.group(1))
changed = []
for f in cfg.repo.rglob("*.kt"):
    parts = f.relative_to(cfg.repo).parts
    if "build" in parts or "build-logic" in parts:
        continue
    s = o = f.read_text()
    for n in names:
        s = re.sub(
            r"\binternal\s+((?:(?:data|sealed|enum|abstract|open|inline|value|const|suspend|operator|infix|override|lateinit|tailrec)\s+)*"
            r"(?:fun|class|interface|object|val|var|typealias)\s+(?:<[^>]*>\s+)?(?:[\w.<>?, ]+\.)?" + re.escape(n) + r"\b)",
            r"\1", s)
    if s != o:
        f.write_text(s)
        changed.append(str(f.relative_to(cfg.repo)))
print(f"names: {sorted(names)}")
print("\n".join(f"  {c}" for c in changed) or "nothing changed")
raise SystemExit(0 if changed else 1)
