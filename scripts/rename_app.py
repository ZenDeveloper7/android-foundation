#!/usr/bin/env python3
"""Turn this foundation into a new app: package/applicationId, app name and project name.

    python3 scripts/rename_app.py --package com.acme.notes --name "Acme Notes" [--dry-run]

Run it once, on a clean git tree, right after copying the template. It rewrites text files and
moves source directories; review the result with `git status` and `git diff`, then run
scripts/verify.sh.
"""

from __future__ import annotations

import argparse
import re
import shutil
import subprocess
import sys
import xml.dom.minidom
from xml.sax.saxutils import escape
from pathlib import Path

OLD_PACKAGE = "com.zen.foundation"
OLD_APP_NAME = "Foundation"
OLD_PROJECT_NAME = "android-foundation"

ROOT = Path(__file__).resolve().parent.parent
SKIP_DIRS = {".git", ".gradle", ".idea", ".kotlin", "build"}
TEXT_SUFFIXES = {".kt", ".kts", ".xml", ".pro", ".md", ".toml", ".properties", ".json", ".yml", ".sh", ".py"}
PACKAGE_RE = re.compile(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$")
PROJECT_NAME_RE = re.compile(r"^[a-z0-9][a-z0-9-]*$")
# Kotlin hard keywords cannot be package segments.
KOTLIN_KEYWORDS = {
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
    "interface", "is", "null", "object", "package", "return", "super", "this", "throw", "true",
    "try", "typealias", "typeof", "val", "var", "when", "while",
}


def tracked_files() -> list[Path]:
    files = []
    for path in ROOT.rglob("*"):
        if any(part in SKIP_DIRS for part in path.relative_to(ROOT).parts):
            continue
        if path.is_file() and path.suffix in TEXT_SUFFIXES and path.name != Path(__file__).name:
            files.append(path)
    return files


def git_is_clean() -> bool:
    result = subprocess.run(
        ["git", "status", "--porcelain"], cwd=ROOT, capture_output=True, text=True, check=False
    )
    return result.returncode == 0 and not result.stdout.strip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--package", required=True, help="new package and applicationId, e.g. com.acme.notes")
    parser.add_argument("--name", required=True, help="user-visible app name, e.g. 'Acme Notes'")
    parser.add_argument("--project-name", help="Gradle root project name (default: derived from --name)")
    parser.add_argument("--dry-run", action="store_true", help="print what would change without writing")
    parser.add_argument("--allow-dirty", action="store_true", help="run even with uncommitted changes")
    args = parser.parse_args()

    if not PACKAGE_RE.match(args.package):
        parser.error(f"invalid package name: {args.package}")
    if args.package == OLD_PACKAGE:
        parser.error("package is unchanged")
    keywords = [part for part in args.package.split(".") if part in KOTLIN_KEYWORDS]
    if keywords:
        parser.error(f"package segments must not be Kotlin keywords: {', '.join(keywords)}")
    if not args.name.strip():
        parser.error("app name must not be blank")
    if not args.dry_run and not args.allow_dirty and not git_is_clean():
        print("Refusing to run on a dirty git tree. Commit or stash first, or pass --allow-dirty.")
        return 1

    project_name = args.project_name or re.sub(r"[^a-z0-9]+", "-", args.name.lower()).strip("-")
    if not PROJECT_NAME_RE.match(project_name):
        parser.error(f"invalid project name {project_name!r}; pass --project-name explicitly")
    # Android string resources treat ' and " specially, besides the XML escapes.
    xml_name = escape(args.name).replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    old_path = OLD_PACKAGE.replace(".", "/")
    new_path = args.package.replace(".", "/")

    # 1. Text replacements. Order matters: the package first, then names.
    changed = 0
    for path in tracked_files():
        original = path.read_text(encoding="utf-8")
        updated = original.replace(OLD_PACKAGE, args.package).replace(old_path, new_path)
        if path.name == "settings.gradle.kts":
            updated = updated.replace(f'rootProject.name = "{OLD_PROJECT_NAME}"', f'rootProject.name = "{project_name}"')
        if path.name == "strings.xml" and path.parts[-4:-2] == ("main", "res"):
            updated = updated.replace(
                f'<string name="app_name">{OLD_APP_NAME}</string>', f'<string name="app_name">{xml_name}</string>'
            )
        if updated != original and path.suffix == ".xml":
            try:
                xml.dom.minidom.parseString(updated)
            except Exception as error:  # noqa: BLE001 - report any parser failure clearly
                print(f"Refusing to write invalid XML to {path.relative_to(ROOT)}: {error}")
                return 1
        if updated != original:
            changed += 1
            print(f"edit  {path.relative_to(ROOT)}")
            if not args.dry_run:
                path.write_text(updated, encoding="utf-8")

    # 2. Move package directories (src/*/kotlin/com/zen/foundation -> new path).
    moves = []
    for source_root in ROOT.rglob("kotlin"):
        if any(part in SKIP_DIRS for part in source_root.relative_to(ROOT).parts):
            continue
        old_dir = source_root / old_path
        if old_dir.is_dir():
            moves.append((old_dir, source_root / new_path))
    # 3. Room schema folders are named after the database class's full name.
    for schema_dir in ROOT.rglob("schemas/*"):
        if schema_dir.is_dir() and schema_dir.name.startswith(OLD_PACKAGE + "."):
            moves.append((schema_dir, schema_dir.with_name(schema_dir.name.replace(OLD_PACKAGE, args.package, 1))))

    for old_dir, new_dir in moves:
        print(f"move  {old_dir.relative_to(ROOT)} -> {new_dir.relative_to(ROOT)}")
        if args.dry_run:
            continue
        new_dir.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(old_dir), str(new_dir))
        # Remove now-empty parents of the old package path (for example com/zen).
        parent = old_dir.parent
        while parent.name != "kotlin" and parent.is_dir() and not any(parent.iterdir()):
            parent.rmdir()
            parent = parent.parent

    print(f"\n{changed} files edited, {len(moves)} directories moved{' (dry run)' if args.dry_run else ''}.")
    if not args.dry_run:
        print("Next: review `git diff`, then run scripts/verify.sh.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
