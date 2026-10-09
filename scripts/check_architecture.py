#!/usr/bin/env python3
"""Bounded source/build/manifest guardrails; not a Kotlin semantic analyzer."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ALLOWED = {
    "app": {":core:data", ":core:designsystem", ":feature:tasks"},
    "core/data": set(),
    "core/designsystem": set(),
    "feature/tasks": {":core:data", ":core:designsystem"},
}
errors = []
for module, allowed in ALLOWED.items():
    build = (ROOT / module / "build.gradle.kts").read_text()
    dependencies = set(re.findall(r'project\("([^"]+)"\)', build))
    for dependency in dependencies - allowed:
        errors.append(f"{module}: forbidden dependency {dependency}")
    if 'signingConfigs.getByName("debug")' in build:
        errors.append(f"{module}: debug signing must not be used for release")
    for path in (ROOT / module / "src/main").rglob("*.kt"):
        text = path.read_text()
        label = str(path.relative_to(ROOT))
        if re.search(r"^import\s+.*\.\*", text, re.MULTILINE):
            errors.append(f"{label}: wildcard import")
        for pattern in (r"\bGlobalScope\b", r"\brunBlocking\b", r"Thread\.sleep\(",
                        r"allowMainThreadQueries\(", r"\.submit\([^\n]*\)\.get\("):
            if re.search(pattern, text):
                errors.append(f"{label}: forbidden blocking/lifetime pattern {pattern}")
        if path.name.endswith("ViewModel.kt") and re.search(
                r"^import (android\.(content\.Context|app\.(Activity|Application))|"
                r"androidx\.lifecycle\.AndroidViewModel)\b", text, re.MULTILINE):
            errors.append(f"{label}: Android context/lifecycle dependency in ViewModel")
        if module.startswith("feature/") and re.search(r"^import androidx\.room\.", text, re.MULTILINE):
            errors.append(f"{label}: feature imports Room directly")

ANDROID = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
application = manifest.find("application")
if application.get(ANDROID + "usesCleartextTraffic") != "false":
    errors.append("app: cleartext must be disabled")
for component in application:
    if component.get(ANDROID + "exported") == "true":
        if component.tag != "activity" or component.get(ANDROID + "name") != ".MainActivity":
            errors.append("app: newly exported component requires an explicit reviewed policy")
for path in (ROOT / "app/src/main/res/xml").glob("*.xml"):
    for element in ET.parse(path).getroot().iter():
        if "excludeKeys" in element.attrib:
            errors.append(f"{path.name}: Android backup does not support excludeKeys")

if errors:
    print("Foundation policy failed:\n" + "\n".join(errors), file=sys.stderr)
    sys.exit(1)
print("Foundation module, Kotlin, manifest and backup policies passed.")
