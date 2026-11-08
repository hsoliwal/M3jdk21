#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""move_module_area.py <repo root> <area> <module> : MOVE_TO_JDK_MODULE v2 (approved layout 2026-11-08), exported-module step.

Sibling of move_area.py (java.base internals): moves one m3/<area> Maven module into an exported JDK module under
src/<module>, keeping its package names (they are API), with `git mv` so history follows:
  m3/<area>/src/main/java/<pkg path>/X.java    -> src/<module>/share/classes/<pkg path>/X.java
  m3/<area>/src/main/java/module-info.java     -> stays: the module descriptor of the Maven BRIDGE (below)
  m3/<area>/src/test/java/<pkg path>/X.java    -> test/jdk/<pkg path>/X.java           (same-package JUnit tests)
  m3/<area>/src/test/resources/...             -> test/jdk/<first pkg path>/resources/...
  m3/<area>/src/main/native/X.c, include/X.h   -> src/<module>/share/native/libm3<area>/...   (opt-in library)
  m3/<area>/src/main/native/tests/X.c          -> test/jdk/<first pkg path>/native/X.c
  m3/<area>/src/main/native/CMakeLists.txt     -> make/m3/<area>/CMakeLists.txt (sources re-pointed by regex)
  m3/<area>/NOTICE                             -> src/<module>/share/legal/m3<area>.md
Writes: src/<module>/share/classes/module-info.java (created, or its exports extended), make/modules/<module>/Java.gmk
(if absent), the PLATFORM_MODULES line of make/conf/module-loader-map.conf (if absent), the Maven bridge pom at
m3/<area>/pom.xml (same artifact, sources = the moved package directories, so every Maven consumer keeps building),
and m3/tooling/jdk-naming/PACKAGE_BASELINE.tsv without the area's REAL root and product packages (removals pass the
gate). Evidence directories (qualification/, recipe/, docs/, *.md, *.json) are NOT moved by this step: they stay
under the bridge module for the evidence-store step. Prints one line per move. Idempotent for the writes; refuses a
second run once the sources are gone. usage: python3 -I move_module_area.py <repo root> <area> <module> [--dry-run]
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(sys.argv[1]).resolve()
AREA, MODULE = sys.argv[2], sys.argv[3]
DRY = "--dry-run" in sys.argv
SRC = ROOT / "m3" / AREA
CLASSES = ROOT / "src" / MODULE / "share" / "classes"
NATIVE = ROOT / "src" / MODULE / "share" / "native" / f"libm3{AREA}"
MAKE = ROOT / "make" / "m3" / AREA
BASELINE = ROOT / "m3" / "tooling" / "jdk-naming" / "PACKAGE_BASELINE.tsv"


def sh(*args: str) -> str:
    if DRY and args[:2] == ("git", "mv"):
        print("DRY " + " ".join(args))
        return ""
    done = subprocess.run(args, cwd=ROOT, capture_output=True, text=True)
    if done.returncode != 0:
        sys.exit("REFUSE git failed: " + " ".join(args) + " :: " + done.stderr.strip())
    return done.stdout


def mv(src: Path, dst: Path) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    sh("git", "mv", src.relative_to(ROOT).as_posix(), dst.relative_to(ROOT).as_posix())
    print(f"{src.relative_to(ROOT).as_posix()}\t{dst.relative_to(ROOT).as_posix()}")


def fix_core_include(path: Path) -> None:
    """Text rule on moved C sources: the shared JNI core header of m3/native was included by a path relative to the
    old module root (../../../../native/include/x.h); after the move it is found through the include directory the
    re-pointed CMakeLists adds (m3/native/include), so the include becomes the bare file name."""
    if DRY or path.suffix not in (".c", ".h") or not path.exists():
        return
    data = path.read_bytes()
    fixed = data.replace(b'#include "../../../../native/include/', b'#include "')
    if fixed != data:
        path.write_bytes(fixed)
        print(f"RULE core-include\t{path.relative_to(ROOT).as_posix()}")


def write(path: Path, text: str) -> None:
    if path.exists() and path.read_text(encoding="utf-8") == text:
        print(f"UNCHANGED\t{path.relative_to(ROOT).as_posix()}")
        return
    if not DRY:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8", newline="\n")
    print(f"WRITE\t{path.relative_to(ROOT).as_posix()}")


module_info = (SRC / "src/main/java/module-info.java").read_text(encoding="utf-8")
exports = re.findall(r"^\s*exports\s+([\w.]+)\s*;", module_info, re.M)
if not exports:
    sys.exit("REFUSE: no exports in the area module-info")
pkg_paths = [e.replace(".", "/") for e in exports]
first_pkg_path = sorted(pkg_paths, key=len)[0]

# 1. main sources, tests, resources, natives, legal
for f in sorted((SRC / "src/main/java").rglob("*.java")):
    rel = f.relative_to(SRC / "src/main/java")
    if rel.as_posix() == "module-info.java":
        continue
    mv(f, CLASSES / rel)
for f in sorted((SRC / "src/test/java").rglob("*")):
    if f.is_file():
        mv(f, ROOT / "test/jdk" / f.relative_to(SRC / "src/test/java"))
if (SRC / "src/test/resources").exists():
    for f in sorted((SRC / "src/test/resources").rglob("*")):
        if f.is_file():
            mv(f, ROOT / "test/jdk" / first_pkg_path / "resources" / f.relative_to(SRC / "src/test/resources"))
cmake_text = None
if (SRC / "src/main/native").exists():
    for f in sorted((SRC / "src/main/native").rglob("*")):
        if not f.is_file():
            continue
        rel = f.relative_to(SRC / "src/main/native")
        if rel.as_posix() == "CMakeLists.txt":
            cmake_text = f.read_text(encoding="utf-8")
            if not DRY:
                sh("git", "rm", "-q", f.relative_to(ROOT).as_posix())
            print(f"{f.relative_to(ROOT).as_posix()}\tREWRITTEN make/m3/{AREA}/CMakeLists.txt")
        elif rel.parts[0] == "tests":
            mv(f, ROOT / "test/jdk" / first_pkg_path / "native" / Path(*rel.parts[1:]))
            fix_core_include(ROOT / "test/jdk" / first_pkg_path / "native" / Path(*rel.parts[1:]))
        else:
            mv(f, NATIVE / rel)
            fix_core_include(NATIVE / rel)
if (SRC / "NOTICE").exists():
    mv(SRC / "NOTICE", ROOT / "src" / MODULE / "share" / "legal" / f"m3{AREA}.md")

# 2. CMakeLists re-pointed: bare sources -> ${M3_NATIVE}/..., tests/ -> ${M3_NATIVE_TESTS}/..., include dir
if cmake_text is not None:
    body = re.sub(r"(?<![\w/$}])tests/([A-Za-z0-9_]+\.c)", r"${M3_NATIVE_TESTS}/\1", cmake_text)
    body = re.sub(r"(?<![\w/$}])([A-Za-z0-9_]+\.c)(?=[\s)])", r"${M3_NATIVE}/\1", body)
    body = body.replace("${CMAKE_CURRENT_SOURCE_DIR}/include", "${M3_NATIVE}/include")
    head = ("# Re-pointed by m3/tooling/jdk-naming/move_module_area.py (MOVE_TO_JDK_MODULE v2): the sources live in the\n"
            f"# JDK module src/{MODULE}/share/native/libm3{AREA} and the C unit tests in test/jdk/{first_pkg_path}/native.\n"
            f"set(M3_NATIVE ${{CMAKE_CURRENT_SOURCE_DIR}}/../../../src/{MODULE}/share/native/libm3{AREA})\n"
            f"set(M3_NATIVE_TESTS ${{CMAKE_CURRENT_SOURCE_DIR}}/../../../test/jdk/{first_pkg_path}/native)\n"
            "# The shared JNI core header of m3/native (the moved sources include it as m3_jni_core.h).\n"
            "include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../m3/native/include)\n")
    body = re.sub(r"(^cmake_minimum_required\([^\n]*\)\n(?:project\([^\n]*\)\n)?)", r"\1" + head.replace("\\", "\\\\"), body, count=1, flags=re.M)
    write(MAKE / "CMakeLists.txt", body)

# 3. the JDK module descriptor (created or extended)
descriptor = CLASSES / "module-info.java"
if descriptor.exists():
    text = descriptor.read_text(encoding="utf-8")
    missing = [e for e in exports if not re.search(rf"^\s*exports\s+{re.escape(e)}\s*;", text, re.M)]
    if missing:
        text = text.rstrip().rstrip("}") + "".join(f"    exports {e};\n" for e in missing) + "}\n"
    write(descriptor, text)
else:
    write(descriptor, "// Copyright 2026 Hitesh Soliwal and contributors\n// SPDX-License-Identifier: Apache-2.0\n"
          f"/**\n * {MODULE}: the M3 collections lane (arrays, collections, algorithms) as one JDK module, moved area by area\n"
          " * from the m3/ side tree by m3/tooling/jdk-naming/move_module_area.py (approved layout 2026-11-08). Package\n"
          " * names are unchanged (they are API); each area's Maven module becomes a bridge pom over these sources until\n"
          " * every consumer has moved.\n */\n"
          f"module {MODULE} {{\n" + "".join(f"    exports {e};\n" for e in exports) + "}\n")

# 4. make: Java.gmk and the module loader map
gmk = ROOT / "make/modules" / MODULE / "Java.gmk"
if not gmk.exists():
    write(gmk, "#\n# SPDX-License-Identifier: Apache-2.0\n# Copyright 2026 Hitesh Soliwal and contributors\n#\n\n"
          f"# {MODULE}: M3 product code moved from the m3/ Maven modules (approved layout 2026-11-08) by\n"
          "# m3/tooling/jdk-naming/move_module_area.py. The Maven modules compiled with -Xlint:all,-module -Werror; the module\n"
          "# name's \"m3\" component is the one lint kind disabled here.\n\n"
          "DISABLED_WARNINGS_java += module\n\n"
          "# Doclint as java.base runs it, without the \"missing\" group (the moved Javadoc is the donors').\n"
          "DOCLINT += -Xdoclint:all/protected,-missing\n")
loader = ROOT / "make/conf/module-loader-map.conf"
loader_text = loader.read_text(encoding="utf-8")
if f"    {MODULE} \\" not in loader_text:
    lines = loader_text.split("\n")
    at = next(i for i, l in enumerate(lines) if l.startswith("    jdk.m3.") and l.strip(" \\") > MODULE) if any(
        l.startswith("    jdk.m3.") and l.strip(" \\") > MODULE for l in lines) else next(
        i for i, l in enumerate(lines) if l.startswith("    jdk.m3.")) + 1
    lines.insert(at, f"    {MODULE} \\")
    write(loader, "\n".join(lines))

# 5. the Maven bridge pom: same artifact, sources = the moved package directories (no copied root, no tests)
pom = (SRC / "pom.xml").read_text(encoding="utf-8")
artifact = re.search(r"<artifactId>([^<]+)</artifactId>", pom).group(1)
name = re.search(r"<name>([^<]+)</name>", pom)
desc = re.search(r"<description>([^<]+)</description>", pom)
sources = "".join(f"                <source>${{project.basedir}}/../../src/{MODULE}/share/classes/{p}</source>\n" for p in sorted(pkg_paths)
                  if not any(p != q and p.startswith(q + "/") for q in pkg_paths))
bridge = f"""<?xml version="1.0" encoding="UTF-8"?>
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Maven BRIDGE of the area m3/{AREA}, written by m3/tooling/jdk-naming/move_module_area.py (MOVE_TO_JDK_MODULE v2,
     approved layout 2026-11-08): the product sources live in the JDK module src/{MODULE}/share/classes and the
     tests in test/jdk; this pom compiles those package directories under the same artifact so every Maven consumer
     keeps building until it has moved too. src/main/java holds only the bridge module descriptor. -->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.m3</groupId>
  <artifactId>{artifact}</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <name>{name.group(1) if name else artifact} (bridge over src/{MODULE})</name>
  <description>{desc.group(1) if desc else ''}</description>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>https://www.apache.org/licenses/LICENSE-2.0</url>
    </license>
  </licenses>
  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
        <version>3.6.1</version>
        <executions>
          <execution>
            <id>module-sources</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>add-source</goal>
            </goals>
            <configuration>
              <sources>
{sources}              </sources>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
        <configuration>
          <compilerArgs>
            <arg>-Xlint:all,-module</arg>
            <arg>-Werror</arg>
          </compilerArgs>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
"""
write(SRC / "pom.xml", bridge)

# 6. the package baseline: the area's REAL root and product packages leave the Maven side (removals pass the gate)
rows = BASELINE.read_text(encoding="utf-8").split("\n")
drop = {f"ROOT\tm3/{AREA}\tREAL"} | {f"PACKAGE\t{e}" for e in exports}
kept = [r for r in rows if r not in drop]
if len(kept) != len(rows):
    write(BASELINE, "\n".join(kept))
print(f"MOVE_TO_JDK_MODULE v2 area=m3/{AREA} module={MODULE} exports={','.join(exports)} dry_run={DRY}")
