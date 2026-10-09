#!/usr/bin/env python3
"""Fail-closed source proof for the hosted M3 arrays build lanes."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
workflow = (ROOT / ".github/workflows/m3-arrays.yml").read_text(encoding="utf-8")
pom = (ROOT / "m3/arrays/pom.xml").read_text(encoding="utf-8")
arrays_test = (
    ROOT / "m3/arrays/src/test/java/com/m3/arrays/M3ArraysTest.java"
).read_text(encoding="utf-8")
native_test = (
    ROOT / "m3/arrays/src/test/java/com/m3/arrays/M3ArrayNativeTest.java"
).read_text(
    encoding="utf-8"
)

checks = []


def require(label: str, source: str, fragment: str) -> None:
    if fragment not in source:
        raise SystemExit(f"M3_ARRAYS_BUILD_WIRING_SOURCE_FAIL|{label}|missing={fragment}")
    checks.append(label)


for fragment in [
    "name: M3 arrays contracts",
    "m3/arrays/pom.xml",
    "m3/runtime-integration/check-m3arrays-build-wiring.py",
    "actions/checkout@11d5960a326750d5838078e36cf38b85af677262",
    "OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz",
    "sha256sum --check -",
    "maven.repo.local=",
    "mvn --batch-mode --no-transfer-progress --file m3/arrays/pom.xml",
    "-Pnative-linux test",
]:
    require("workflow:" + fragment, workflow, fragment)

for fragment in [
    "<maven.compiler.release>21</maven.compiler.release>",
    "<artifactId>junit-jupiter</artifactId>",
    "<version>5.10.2</version>",
    "<arg>-Werror</arg>",
    "<id>native-linux</id>",
    "<artifactId>exec-maven-plugin</artifactId>",
    "<executable>ctest</executable>",
]:
    require("pom:" + fragment, pom, fragment)

for fragment in [
    "class M3ArraysTest",
    "class M3ArrayNativeTest",
    "m3.arrays.native.path",
    "optionalNativeProviderMatchesJavaAcrossChunkAndUnicodeBoundaries",
]:
    require("tests:" + fragment, arrays_test + native_test, fragment)

print(
    "M3_ARRAYS_BUILD_WIRING_SOURCE_PASS "
    f"checks={len(checks)}/20|java_lane=true|native_lane=true|runtime=NOT_RUN"
)
