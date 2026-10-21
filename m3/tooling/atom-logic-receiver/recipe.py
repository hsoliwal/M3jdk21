#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""One-atom Maven XML receipt: opt-in Synexia AtomLogic OpenRewrite scanner.

The implementation remains in canonical Synexia. This recipe only receives
an opt-in M3JDK21 control-reactor profile and refuses changed source provenance.
"""
from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import tempfile
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
OWNER = Path("m3/pom.xml")
PRE_BLOB = "693156006961717eccfc649585e724d67682b3df"
ANCHOR = "  </profiles>\n\n  <build>\n"
PROFILE = """    <profile>
      <id>m3-synexia-atomlogic-readonly</id>
      <build><plugins><plugin>
        <groupId>org.openrewrite.maven</groupId>
        <artifactId>rewrite-maven-plugin</artifactId>
        <version>5.23.1</version>
        <dependencies><dependency>
          <groupId>com.synexia</groupId>
          <artifactId>synexia-openrewrite-recipes</artifactId>
          <version>1.0.0-SNAPSHOT</version>
        </dependency></dependencies>
        <executions><execution>
          <id>m3-atomlogic-candidate-scan</id>
          <phase>verify</phase><goals><goal>dryRun</goal></goals>
        </execution></executions>
        <configuration>
          <activeRecipes>
            <recipe>com.synexia.rewrite.M3AtomLogicClusterRecipe</recipe>
          </activeRecipes>
          <exportDatatables>true</exportDatatables>
          <runPerSubmodule>false</runPerSubmodule>
          <failOnInvalidActiveRecipes>true</failOnInvalidActiveRecipes>
          <failOnDryRunResults>false</failOnDryRunResults>
        </configuration>
      </plugin></plugins></build>
    </profile>
"""

def sha256(contents: bytes) -> str:
    return hashlib.sha256(contents).hexdigest()

def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()

def generate(before: bytes) -> bytes:
    if git_blob(before) != PRE_BLOB:
        raise ValueError("refused: baseline POM changed")
    source = before.decode("utf-8", errors="strict")
    if source.count(ANCHOR) != 1 or "m3-synexia-atomlogic-readonly" in source:
        raise ValueError("refused: missing/nonunique anchor or duplicate profile")
    output = source.replace(ANCHOR, PROFILE + ANCHOR, 1).encode("utf-8")
    validate_xml(output)
    if output.replace(PROFILE.encode("utf-8"), b"", 1) != before:
        raise AssertionError("XML recipe changed other source")
    return output

def validate_xml(post: bytes) -> None:
    root = ET.fromstring(post)
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    profiles = root.findall(".//m:profiles/m:profile", ns)
    matches = [p for p in profiles if p.findtext("m:id", namespaces=ns)
               == "m3-synexia-atomlogic-readonly"]
    if len(matches) != 1:
        raise AssertionError("atom logic profile not unique")
    p = matches[0]
    recipes = p.findall(".//m:activeRecipes/m:recipe", ns)
    if [recipe.text for recipe in recipes] != ["com.synexia.rewrite.M3AtomLogicClusterRecipe"]:
        raise AssertionError("must use canonical read-only Synexia recipe only")
    if p.find(".//m:goal", ns).text != "dryRun":
        raise AssertionError("mutation-capable rewrite goal refused")
    if p.find(".//m:exportDatatables", ns).text != "true":
        raise AssertionError("missing read-only evidence")
    if p.find(".//m:failOnInvalidActiveRecipes", ns).text != "true":
        raise AssertionError("silently missing canonical recipe refused")

def self_test(before: bytes) -> bytes:
    after = generate(before)
    for candidate in (before + b"\n<!-- drift -->\n",
                      before.replace(ANCHOR.encode(), b"  </profiles>\n", 1),
                      before.replace(b"<id>m3-extended</id>",
                                     b"<id>m3-synexia-atomlogic-readonly</id>", 1)):
        try:
            generate(candidate)
        except ValueError:
            continue
        raise AssertionError("source drift/duplicate admission missing")
    print(f"M3_ATOMLOGIC_PROFILE_RECIPE_PASS before={sha256(before)} after={sha256(after)}")
    return after

def install(path: Path, data: bytes) -> None:
    if path.is_symlink() or not path.is_file():
        raise ValueError("refused: unsafe or missing owner")
    fd, tmp = tempfile.mkstemp(prefix=".m3-atomlogic-", dir=path.parent)
    try:
        with os.fdopen(fd, "wb") as out:
            out.write(data)
        os.replace(tmp, path)
    finally:
        if os.path.exists(tmp):
            os.unlink(tmp)

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["self-test", "verify", "apply"])
    parser.add_argument("--root", type=Path, default=ROOT)
    args = parser.parse_args()
    frozen = (HERE / "before/pom.xml")
    if frozen.is_symlink() or not frozen.is_file():
        raise ValueError("refused: missing immutable before image")
    baseline = frozen.read_bytes()
    after = self_test(baseline)
    if args.mode == "self-test":
        return
    target = args.root.resolve(strict=True) / OWNER
    if target.is_symlink() or not target.is_file():
        raise ValueError("refused: target missing or symlinked")
    old = target.read_bytes()
    if args.mode == "apply" and old == baseline:
        install(target, after)
    if target.read_bytes() != after:
        raise ValueError("refused: unqualified profile source or missing candidate")
    print("M3_ATOMLOGIC_PROFILE_FIXED_POINT sha256=" + sha256(after))

if __name__ == "__main__":
    main()
