#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Maintainer-only recipe resource sealing; reads a pinned target and never writes it."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

PIN = "d47a4b4fdb8e3794ec56768b06aa9e23730c6b8e"
PRIVATE = "81c4f19a5ce5a68da316ff6f2802f2041a684f31"
JAVA_TARGETS = [
    "m3/ports/compiletime/src/main/java/com/m3/compiletime/M3EscapeFlow.java",
    "m3/ports/compiletime/src/main/java/com/m3/compiletime/M3GvasLiteralHarvester.java",
    "m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ItemReferenceScan.java",
    "m3/ports/compiletime/src/test/java/com/m3/compiletime/M3EscapePreparedTest.java",
]
TEXT_TARGET = "m3/docs/name-mapping.d/m3-program-escape-precompute.json"
HISTORY = ("m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/"
           "jdk21-hash-pinned/m3-compiletime-regex-precompute/manifest.tsv")


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", required=True, type=Path)
    args = parser.parse_args()
    crate = Path(__file__).resolve().parent.parent
    resources = crate / "src/main/resources/com/synexia/rewrite"
    java = resources / "hash-pinned-java" / crate.name
    text = resources / "hash-pinned-text" / crate.name
    head = subprocess.check_output(["git", "-C", str(args.target), "rev-parse", "HEAD"], text=True).strip()
    if head != PIN:
        raise ValueError("target baseline differs from reviewed revision")
    targets = []
    for path in JAVA_TARGETS:
        name = path.replace("/", "__") + ".txt"
        before = java / "pre" / name
        targets.append([path, sha(before.read_bytes()) if before.exists() else "ABSENT",
                        sha((java / name).read_bytes()), name])
    (java / "manifest.tsv").write_text("".join("\t".join(row) + "\n" for row in targets))
    name = TEXT_TARGET.replace("/", "__") + ".txt"
    mapping = json.loads((text / name).read_text())
    mapping["targets"] = [{"path": row[0], "before_sha256": row[1], "after_sha256": row[2]}
                          for row in targets]
    (text / name).write_text(json.dumps(mapping, indent=2) + "\n")
    targets.append([TEXT_TARGET, "ABSENT", sha((text / name).read_bytes()), name])
    (text / "manifest.tsv").write_text("\t".join(targets[-1]) + "\n")
    (crate / "payload-manifest.json").write_text(json.dumps({
        "schema": "M3_PROGRAM_ESCAPE_PAYLOAD_V1", "target_revision": PIN,
        "source_recipe_revision": PRIVATE,
        "targets": [{"path": r[0], "before_sha256": r[1], "after_sha256": r[2], "resource": r[3]}
                    for r in targets]}, indent=2) + "\n")
    tracked = subprocess.check_output(["git", "-C", str(args.target), "ls-tree", "-r", "--name-only", PIN],
                                      text=True).splitlines()
    prefixes = ["m3/ports/compiletime/src/", "m3/ports/precompute/src/main/java/"]
    prefixes += ["m3/vendor/third_party/" + folder + "/src/main/java/"
                 for folder in ("asm-9.6", "asm-tree", "asm-analysis")]
    selected = {p for p in tracked if p.endswith(".java") and any(p.startswith(q) for q in prefixes)}
    selected.update(["m3/ports/compiletime/pom.xml", "m3/ports/precompute/pom.xml",
                     "m3/ports/compiletime/verify_compiletime_precompute.py", HISTORY])
    selected.difference_update(JAVA_TARGETS)
    parents = []
    for path in sorted(selected):
        data = subprocess.check_output(["git", "-C", str(args.target), "show", PIN + ":" + path])
        parents.append(path + "\t" + sha(data) + "\n")
    inventory = "".join(parents).encode()
    (crate / "contract/parent-inputs.tsv").write_bytes(inventory)
    owners = crate.parent.parent / "src/main/java/com/synexia/rewrite"
    seals = {
        "target_revision": PIN, "source_recipe_revision": PRIVATE,
        "manifest_hashes": {kind: sha((resources / ("hash-pinned-" + kind) / crate.name / "manifest.tsv").read_bytes())
                            for kind in ("java", "text")},
        "manifest_counts": {"java": 4, "text": 1},
        "parent_inputs_sha256": sha(inventory), "parent_input_count": len(parents),
        "owners": {name: sha((owners / name).read_bytes()) for name in
                   ("M3EditScope.java", "M3HashPinnedJavaSnapshotRecipe.java",
                    "M3HashPinnedTextSnapshotRecipe.java", "M3ScopeInference.java")},
    }
    (crate / "contract/input-seals.json").write_text(json.dumps(seals, indent=2) + "\n")
    print("M3_ESCAPE_RECIPE_INPUTS_SEALED targets=5 parents=" + str(len(parents)))


if __name__ == "__main__":
    main()
