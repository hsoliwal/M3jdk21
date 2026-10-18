#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Prepare one bounded native recipe candidate; never edit a receiving source tree."""
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import sys

sys.dont_write_bytecode = True
WORK = Path(__file__).resolve().parents[2]
HERE = WORK / "runtime-refinement"
BASE = "4b24824c3aa09d802121447f8970d442a855f064"
OWNER = HERE / "canonical/synexia-openrewrite-recipes"
CRATE_NAME = "m3-collections-contract-refinement-20261017"
CRATE = OWNER / "recipe-crates" / CRATE_NAME
ORIGINAL = WORK / "canonical-recovery/source/synexia-openrewrite-recipes"
CAPPED = WORK / "collection-refinement/canonical/synexia-openrewrite-recipes/recipe-crates" / CRATE_NAME
BASELINE = WORK / "runtime-recovery/current-tree"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def copy_exact(source, destination):
    data = source.read_bytes()
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists():
        if destination.read_bytes() != data:
            raise ValueError("existing recipe apparatus changed: " + str(destination))
    else:
        destination.write_bytes(data)
    return digest(data)


def main():
    original_crate = ORIGINAL / "recipe-crates" / CRATE_NAME
    origins = {}
    for relative in ("prepare.py", "receive.py", "ENGINE_SOURCES.tsv", "HARNESS_LINEAGE.json",
                     "LICENSE.txt", "NOTICE.md"):
        origins[relative] = copy_exact(original_crate / relative, CRATE / relative)
    for domain in ("src", "contract"):
        for source in sorted((original_crate / domain).rglob("*")):
            if source.is_file():
                relative = source.relative_to(original_crate)
                origins[str(relative)] = copy_exact(source, CRATE / relative)
    origins["pom.xml"] = copy_exact(CAPPED / "pom.xml", CRATE / "pom.xml")
    if origins["pom.xml"] != "4c90faaa9e64aebeeba14fb2196731c6334293781d0fdcafbf190f2f5662eb7d":
        raise ValueError("unreviewed capped Maven host")
    for name in ("M3HashPinnedJavaSnapshotRecipe", "M3HashPinnedTextSnapshotRecipe",
                 "M3ScopeInference", "M3EditScope"):
        relative = Path("src/main/java/com/synexia/rewrite") / (name + ".java")
        origins["owner/" + str(relative)] = copy_exact(ORIGINAL / relative, OWNER / relative)

    path = "src/hotspot/share/classfile/classLoader.cpp"
    before = (BASELINE / path).read_bytes()
    if digest(before) != "665bb788428c8d3837d052ac4f69a2756bb1823918726b41a7a7008eadfafd8d":
        raise ValueError("native target preimage drift")
    object_bytes = (BASELINE / "src/java.base/share/classes/java/lang/Object.java").read_bytes()
    if digest(object_bytes) != "829fb37093cf3e8d11b3c1e3d42f898f0c86f3b697e7a66cefe09934be3bb259":
        raise ValueError("Object source binding drift")
    original = b"""int ClassLoader::compute_Object_vtable() {
  // hardwired for JDK1.2 -- would need to duplicate class file parsing
  // code to determine actual value from file
  // Would be value '11' if finals were in vtable
  int JDK_1_2_Object_vtable_size = 5;
  return JDK_1_2_Object_vtable_size * vtableEntry::size();
}"""
    replacement = b"""int ClassLoader::compute_Object_vtable() {
  // Object declares five legacy virtual methods and M3's non-final toAdapter.
  // This count must match java.lang.Object before array klasses are created.
  const int Object_vtable_size = 6;
  return Object_vtable_size * vtableEntry::size();
}"""
    if before.count(original) != 1:
        raise ValueError("native behavioral atom no longer matches")
    after = before.replace(original, replacement)
    candidate = HERE / "candidates/native" / path
    candidate.parent.mkdir(parents=True, exist_ok=True)
    candidate.write_bytes(after)
    spec = importlib.util.spec_from_file_location("runtime_recipe_prepare", CRATE / "prepare.py")
    prepare = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(prepare)
    metadata = prepare.seal(CRATE, "m3jdk21-object-vtable", "hsoliwal/M3jdk21", BASELINE,
                            [HERE / "candidates/native"], BASE, OWNER)
    receipt = {"schema": "M3_RUNTIME_NATIVE_CANDIDATE_V1", "baseline": BASE,
               "path": path, "before_sha256": digest(before), "after_sha256": digest(after),
               "object_sha256": digest(object_bytes), "apparatus": origins,
               "receiving_source_written": False, "metadata": metadata,
               "runtime_qualification": "NOT_RUN", "Object_universal_compatibility": "HOLD"}
    (HERE / "authoring/native-candidate-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
