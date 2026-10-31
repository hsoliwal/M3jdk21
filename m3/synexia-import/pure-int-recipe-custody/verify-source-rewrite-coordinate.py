#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed vendor/custody dependency-coordinate gate (not Maven build proof)."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
EXPECTED = "8.17.1-m3-source-1"

def pom(path):
    return ET.parse(ROOT / path).getroot()

def field(root, name):
    value = root.find("m:properties/m:" + name, NS)
    if value is None or value.text is None:
        raise AssertionError("missing " + name)
    return value.text.strip()

custody = pom("m3/synexia-import/pure-int-recipe-custody/pom.xml")
if field(custody, "openrewrite.version") != EXPECTED:
    raise AssertionError("custody OpenRewrite coordinate drift")

for module in ("rewrite-core", "rewrite-java", "rewrite-java-21"):
    vendor = pom(f"m3/vendor/third_party/{module}/pom.xml")
    version = vendor.find("m:version", NS)
    if version is None or version.text != EXPECTED:
        raise AssertionError(f"source-built {module} does not publish {EXPECTED}")

deps = custody.findall("m:dependencies/m:dependency", NS)
for artifact in ("rewrite-core", "rewrite-java", "rewrite-java-21"):
    matches = [d for d in deps if d.findtext("m:artifactId", namespaces=NS) == artifact]
    if len(matches) != 1 or matches[0].findtext("m:version", namespaces=NS) != "${openrewrite.version}":
        raise AssertionError("custody dependency mismatch: " + artifact)
print("SOURCE_REWRITE_COORDINATES_PASS modules=3")
