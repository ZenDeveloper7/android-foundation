#!/usr/bin/env python3
"""Repository policies that Gradle, lint and ktlint do not enforce.

Scans production sources only (anything under a src/main directory, skipping build output).
Prints "path:line: message" for each violation and exits 1; otherwise prints one line and exits 0.
Standard library only. Run from anywhere: python3 scripts/check_policies.py
"""

import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SKIP_DIRS = {"build", ".gradle", ".git", ".idea"}
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"

KOTLIN_MAIN = re.compile(r"(^|/)src/main/.+\.kt$")
MANIFEST_MAIN = re.compile(r"(^|/)src/main/AndroidManifest\.xml$")
XML_MAIN = re.compile(r"(^|/)src/main/res/xml/[^/]+\.xml$")
NETWORK_CONFIG = re.compile(r"(^|/)src/main/res/xml/[^/]*network_security_config[^/]*\.xml$")
FEATURE_PATH = re.compile(r"(^|/)feature/")
APP_BUILD_FILE = "app/build.gradle.kts"

# Strings and comments are matched together so that a "//" inside a string is not a comment.
COMMENT_OR_STRING = re.compile(
    r'"""[\s\S]*?"""'
    r'|"(?:\\.|[^"\\\n])*"'
    r"|'(?:\\.|[^'\\\n])*'"
    r"|//[^\n]*"
    r"|/\*[\s\S]*?\*/"
)
XML_COMMENT = re.compile(r"<!--[\s\S]*?-->")
IMPORT = re.compile(r"^import\s+([\w.*]+)(?:\s+as\s+\w+)?[ \t]*$", re.MULTILINE)
GET_CALL = re.compile(r"\s*\.\s*get\s*\(\s*\)")
DEBUG_SIGNING = re.compile(r'signingConfigs\s*(?:\.\s*(?:getByName|findByName)\s*\(|\[)\s*"debug"')

BANNED_CALLS = [
    (re.compile(r"\bGlobalScope\b"), "GlobalScope is banned; use an injected, owned scope"),
    (re.compile(r"\brunBlocking\b"), "runBlocking is banned in production code; use suspend APIs"),
    (re.compile(r"\bThread\.sleep\("), "Thread.sleep blocks; use delay() inside a coroutine"),
    (
        re.compile(r"\ballowMainThreadQueries\("),
        "allowMainThreadQueries blocks the main thread; use suspend DAO functions",
    ),
    (
        re.compile(r"\bcatch\s*\(\s*\w+\s*:\s*(?:[\w.]+\.)?Throwable\??\s*\)"),
        "do not catch Throwable; catch the specific exceptions you expect",
    ),
]
VIEWMODEL_FORBIDDEN_IMPORTS = {
    "android.content.Context",
    "android.app.Application",
    "android.app.Activity",
    "androidx.lifecycle.AndroidViewModel",
}
FEATURE_FORBIDDEN_PREFIXES = (
    "androidx.room.",
    "retrofit2.",
    "okhttp3.",
    "androidx.datastore.",
)


def read(path):
    return path.read_text(encoding="utf-8")


def line_of(text, index):
    return text.count("\n", 0, index) + 1


def blank_out(match_text):
    return "".join(c if c == "\n" else " " for c in match_text)


def strip_comments(text):
    """Blank out comments but keep strings and newlines, so line numbers stay correct."""

    def replace(match):
        token = match.group(0)
        return blank_out(token) if token.startswith(("//", "/*")) else token

    return COMMENT_OR_STRING.sub(replace, text)


def matching_close(text, open_index, open_char, close_char):
    depth = 0
    for i in range(open_index, len(text)):
        if text[i] == open_char:
            depth += 1
        elif text[i] == close_char:
            depth -= 1
            if depth == 0:
                return i
    return None


def iter_files(pattern):
    found = []
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            rel = Path(dirpath, name).relative_to(ROOT).as_posix()
            if pattern.search(rel):
                found.append(rel)
    return sorted(found)


def check_kotlin(rel, errors):
    text = strip_comments(read(ROOT / rel))

    for pattern, message in BANNED_CALLS:
        for match in pattern.finditer(text):
            errors.append((rel, line_of(text, match.start()), message))

    for match in re.finditer(r"\.submit\s*\(", text):
        close = matching_close(text, match.end() - 1, "(", ")")
        if close is not None and GET_CALL.match(text, close + 1):
            errors.append(
                (rel, line_of(text, match.start()), "Future.get() blocks; use a suspend API")
            )

    imports = [(m.group(1), m.start()) for m in IMPORT.finditer(text)]
    if rel.endswith("ViewModel.kt"):
        for name, pos in imports:
            if name in VIEWMODEL_FORBIDDEN_IMPORTS:
                errors.append(
                    (rel, line_of(text, pos), f"ViewModel must not import {name}")
                )

    if FEATURE_PATH.search(rel):
        for name, pos in imports:
            if name.startswith(FEATURE_FORBIDDEN_PREFIXES):
                errors.append(
                    (rel, line_of(text, pos), f"feature code must not import {name}")
                )


def elements_with_lines(root, xml_text):
    """Yield (element, line) in document order, using the source text for line numbers."""
    stripped = XML_COMMENT.sub(lambda m: blank_out(m.group(0)), xml_text)
    lines = stripped.splitlines()
    tag_lines = {}
    seen = {}
    for element in root.iter():
        tag = element.tag
        if tag not in tag_lines:
            pattern = re.compile(r"<\s*" + re.escape(tag) + r"[\s>/]")
            tag_lines[tag] = [i for i, line in enumerate(lines, 1) if pattern.search(line)]
        index = seen.get(tag, 0)
        seen[tag] = index + 1
        candidates = tag_lines[tag]
        yield element, (candidates[index] if index < len(candidates) else 1)


def parse_xml(rel, errors):
    try:
        text = read(ROOT / rel)
        return ET.fromstring(text), text
    except ET.ParseError as error:
        line = error.position[0] if error.position else 1
        errors.append((rel, line, f"could not parse XML: {error}"))
        return None, None


def check_manifest(rel, errors):
    root, text = parse_xml(rel, errors)
    if root is None:
        return
    for element, line in elements_with_lines(root, text):
        if element.get(ANDROID_NS + "exported") != "true":
            continue
        name = element.get(ANDROID_NS + "name")
        if element.tag == "activity" and name == ".MainActivity":
            continue
        errors.append(
            (rel, line, f'<{element.tag} android:name="{name}"> must not be exported')
        )


def check_network_config(rel, errors):
    root, text = parse_xml(rel, errors)
    if root is None:
        return
    for element, line in elements_with_lines(root, text):
        if element.tag == "base-config" and element.get("cleartextTrafficPermitted") == "true":
            errors.append((rel, line, "base-config must not permit cleartext traffic"))


def check_backup_xml(rel, text, errors):
    stripped = XML_COMMENT.sub(lambda m: blank_out(m.group(0)), text)
    for match in re.finditer(r"\bexcludeKeys\s*=", stripped):
        errors.append(
            (rel, line_of(stripped, match.start()),
             "excludeKeys is not supported in backup rules; use an allowlist of files")
        )


def check_string_resources(errors):
    """`\\"` is a literal quote character in Android resources; use plain "..." to keep spaces."""
    for path in sorted(ROOT.glob("**/src/main/res/values*/strings*.xml")):
        if "build" in path.relative_to(ROOT).parts:
            continue
        rel = str(path.relative_to(ROOT))
        text = path.read_text(encoding="utf-8")
        for match in re.finditer(r"<(?:string|item)\b[^>]*>(.*?)</(?:string|item)>", text, re.S):
            value = match.group(1)
            if value.startswith('\\"') or value.endswith('\\"'):
                line = text.count("\n", 0, match.start()) + 1
                errors.append((rel, line, "string starts/ends with an escaped (literal) quote"))


# Known violations in migrated legacy code, one "path: message" per line (`#` starts a comment).
# Entries are matched without line numbers so edits elsewhere in the file don't break them. A
# listed file may have as many violations with that message as it has entries. Entries that no
# longer match fail the check, so the list can only shrink.
BASELINE_FILE = "scripts/policy-baseline.txt"


def apply_baseline(errors):
    path = ROOT / BASELINE_FILE
    if not path.is_file():
        return errors, 0, []
    allowed = {}
    for raw in read(path).splitlines():
        entry = raw.split("#", 1)[0].strip()
        if entry:
            allowed[entry] = allowed.get(entry, 0) + 1
    remaining = []
    for rel, line, message in errors:
        key = f"{rel}: {message}"
        if allowed.get(key, 0) > 0:
            allowed[key] -= 1
        else:
            remaining.append((rel, line, message))
    stale = sorted(key for key, count in allowed.items() for _ in range(count))
    return remaining, len(errors) - len(remaining), stale


def check_release_signing(rel, errors):
    text = strip_comments(read(ROOT / rel))
    build_types = re.search(r"\bbuildTypes\s*\{", text)
    if build_types is None:
        errors.append((rel, 1, "buildTypes block not found; cannot check release signing"))
        return
    types_close = matching_close(text, build_types.end() - 1, "{", "}")
    release = re.compile(r"\brelease\s*\{").search(text, build_types.end(), types_close)
    if release is None:
        errors.append((rel, 1, "release block not found inside buildTypes"))
        return
    release_close = matching_close(text, release.end() - 1, "{", "}")
    block = text[release.start():release_close + 1]
    for match in DEBUG_SIGNING.finditer(block):
        line = line_of(text, release.start() + match.start())
        errors.append(
            (rel, line, "release build type must not use the debug signing config")
        )


def main():
    errors = []
    kotlin = iter_files(KOTLIN_MAIN)
    manifests = iter_files(MANIFEST_MAIN)
    xml_files = iter_files(XML_MAIN)

    for rel in kotlin:
        check_kotlin(rel, errors)
    for rel in manifests:
        check_manifest(rel, errors)
    for rel in xml_files:
        if NETWORK_CONFIG.search(rel):
            check_network_config(rel, errors)
        check_backup_xml(rel, read(ROOT / rel), errors)
    check_string_resources(errors)
    if (ROOT / APP_BUILD_FILE).is_file():
        check_release_signing(APP_BUILD_FILE, errors)

    errors, baselined, stale = apply_baseline(errors)
    for entry in stale:
        print(f"{BASELINE_FILE}: no longer needed, remove: {entry}")
    if errors:
        for rel, line, message in sorted(errors):
            print(f"{rel}:{line}: {message}")
        print(f"check_policies: {len(errors)} violation(s)")
        return 1
    if stale:
        print(f"check_policies: {len(stale)} stale baseline entr(ies)")
        return 1
    if baselined:
        print(f"check_policies: {baselined} known violation(s) in {BASELINE_FILE}")

    scanned = len(kotlin) + len(manifests) + len(xml_files) + 1
    print(f"check_policies: OK ({scanned} production files checked)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
