# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""slf4j_placeholder.py <source root>

SLF4J_TO_PLACEHOLDER_LOGGER v1 (2026-11-09; user decision "Internal {} facade"). This rewrites relocated Java
sources that log through lombok @Slf4j or slf4j so that they log through jdk.internal.m3.collections.PlaceholderLogger.
Message strings and call shapes are left unchanged:

- The imports lombok.extern.slf4j.Slf4j, org.slf4j.Logger and org.slf4j.LoggerFactory are removed. A file outside
  jdk.internal.m3.collections gets `import jdk.internal.m3.collections.PlaceholderLogger;`.
- `@Slf4j` is removed, and the field lombok would generate is written as the first member of the annotated type:
  `private static final PlaceholderLogger log = PlaceholderLogger.of(<Type>.class);`. lombok puts its field first,
  so <clinit> order and member order match the donor.
- An explicit slf4j field (`Logger` + `LoggerFactory.getLogger(...)`) keeps its own name and position.
  `Logger` becomes `PlaceholderLogger` and `LoggerFactory.getLogger` becomes `PlaceholderLogger.of`. This applies only
  to files that imported org.slf4j.Logger, and always to fully qualified org.slf4j names.

The tool prints one line per rewritten file and refuses a file that uses any other org.slf4j type (MDC, Marker, ...)."""
import os
import re
import sys

root = sys.argv[1]
FACADE = "jdk.internal.m3.collections.PlaceholderLogger"
IMPORTS = re.compile(r"^import\s+(lombok\.extern\.slf4j\.Slf4j|org\.slf4j\.Logger|org\.slf4j\.LoggerFactory)\s*;\s*\n", re.M)
TYPE_DECL = re.compile(r"\b(class|interface|enum|record)\s+(\w+)[^{;]*\{")
changed = 0
for d, _, fs in os.walk(root):
    for f in fs:
        if not f.endswith(".java"):
            continue
        p = os.path.join(d, f)
        text = open(p, encoding="utf-8").read()
        if "@Slf4j" not in text and "org.slf4j" not in text:
            continue
        other = re.findall(r"\borg\.slf4j\.(?!Logger\b|LoggerFactory\b)\w+", text)
        if other:
            raise SystemExit(f"SLF4J_TO_PLACEHOLDER_LOGGER refuses {p}: uses {sorted(set(other))}")
        imported_logger = re.search(r"^import\s+org\.slf4j\.Logger\s*;", text, re.M) is not None
        new = IMPORTS.sub("", text)
        new = new.replace("org.slf4j.LoggerFactory.getLogger(", "PlaceholderLogger.of(")
        new = new.replace("org.slf4j.Logger", "PlaceholderLogger")
        if imported_logger:
            new = re.sub(r"\bLoggerFactory\.getLogger\(", "PlaceholderLogger.of(", new)
            new = re.sub(r"(?<![\w.])Logger\b", "PlaceholderLogger", new)
        while True:
            m = re.search(r"@Slf4j\b[ \t]*\n?", new)
            if not m:
                break
            decl = TYPE_DECL.search(new, m.end())
            if decl is None:
                raise SystemExit(f"SLF4J_TO_PLACEHOLDER_LOGGER: no type after @Slf4j in {p}")
            body = decl.end()
            indent = re.match(r"[ \t]*", new[new.rfind("\n", 0, decl.start()) + 1:]).group(0) + "    "
            field = f"\n{indent}private static final PlaceholderLogger log = PlaceholderLogger.of({decl.group(2)}.class);\n"
            new = new[:body] + field + new[body:]
            new = new[:m.start()] + new[m.end():]
        package = (re.search(r"^package\s+([\w.]+);", new, re.M) or [None, ""])[1]
        if package != "jdk.internal.m3.collections" and f"import {FACADE};" not in new:
            pm = re.search(r"^package\s+[\w.]+;\n", new, re.M)
            new = new[:pm.end()] + f"\nimport {FACADE};\n" + new[pm.end():]
        if new != text:
            with open(p, "w", encoding="utf-8", newline="\n") as w:
                w.write(new)
            changed += 1
            print("SLF4J_TO_PLACEHOLDER_LOGGER", os.path.relpath(p, root).replace(os.sep, "/"))
print("SLF4J_TO_PLACEHOLDER_LOGGER files", changed)
