"""Shared configuration for the migration scripts (see docs/MIGRATE_EXISTING_APP.md).

Every script takes `--config <path>` pointing at a JSON file like migration.example.json.
Paths in the config are relative to `repo`. Generated files (assignment.json, module_deps.json)
are written next to the config file.
"""
import argparse
import json
import pathlib
import re
import sys


class Config:
    def __init__(self, path):
        self.path = pathlib.Path(path).resolve()
        raw = json.loads(self.path.read_text())
        self.raw = raw
        self.repo = (self.path.parent / raw["repo"]).resolve()
        self.base_package = raw["base_package"]
        self.source_module = raw.get("source_module", "app")
        self.source_root = self.repo / raw.get("source_root", "app/src/main/java")
        self.levels = raw["module_levels"]
        self.shared_module = raw.get("shared_module", "core:ui")
        self.anchors = [(re.compile(rx), module) for rx, module in raw.get("anchors", [])]
        self.overrides = raw.get("overrides", {})
        self.plugin_prefix = raw.get("plugin_prefix", "foundation")
        self.feature_implicit_deps = set(raw.get("feature_implicit_deps", []))
        self.library_imports = [(re.compile(rx, re.M), acc) for rx, acc in raw.get("library_imports", [])]
        self.resource_module = raw.get("resource_module", "core:designsystem")
        self.keep_resource_dirs = re.compile(raw.get("keep_resource_dirs", r"^(mipmap.*|xml)$"))
        self.keep_resource_files = re.compile(raw.get("keep_resource_files", r"^(themes.*|ic_launcher_background)\.xml$"))
        self.package_rules = [
            (re.compile(r["module"]), re.compile(r["pattern"]), r["replace"]) for r in raw.get("package_rules", [])
        ]
        for module in self.levels:
            if module != self.source_module and ":" not in module:
                sys.exit(f"module_levels: '{module}' must look like 'core:x' or 'feature:x'")

    @property
    def base_dir(self):
        return self.source_root / self.base_package.replace(".", "/")

    def module_dir(self, module):
        return self.repo / module.replace(":", "/")

    def module_source_root(self, module):
        if module == self.source_module:
            return self.source_root
        return self.module_dir(module) / "src/main" / self.source_root.name

    def namespace(self, module):
        kind, name = module.split(":")
        return f"{self.base_package}.{kind}.{name}"

    def output(self, name):
        return self.path.parent / name


def parse(description, extra=None):
    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--config", required=True, help="migration JSON config")
    if extra:
        extra(parser)
    args = parser.parse_args()
    return Config(args.config), args


def top_level_names(text):
    """Public top-level declarations of a Kotlin file (approximate, line based)."""
    names = set()
    for line in text.splitlines():
        if not line or line[0] in " \t}/*@)" or line.startswith(("import", "package")):
            continue
        if re.match(r"(private|protected)\b", line):
            continue
        m = re.search(r"\b(?:class|interface|object|typealias|fun|val|var)\s+(?:<[^>]+>\s+)?(?:[\w.<>, ?]+\.)?(\w+)", line)
        if m:
            names.add(m.group(1))
    return names


def package_of(text):
    m = re.search(r"^package\s+([\w.]+)", text, re.M)
    return m.group(1) if m else ""
