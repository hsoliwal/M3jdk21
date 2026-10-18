#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Preserve all public-host gate traces in a bounded, losslessly bundled evidence input."""

import argparse
import hashlib
import json
from pathlib import Path

PREFIX = "synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/"
EVIDENCE = "evidence/runtime-public-host-20261018/"
PUBLIC = "m3/arrays/qualification/empty-segments-20261017/recipe-custody"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--public-repository", type=Path, required=True)
    parser.add_argument("--proof", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    args = parser.parse_args()
    work = args.work_dir.resolve()
    work.mkdir(parents=True, exist_ok=False)
    proof = args.proof.resolve()
    receipt = json.loads((proof / "receipt.json").read_text())
    if receipt["status"] != "PASS" or len(receipt["qualifications"]) != 7 or receipt["private_recipe_owner_used"]:
        raise ValueError("public-host seven-dataset qualification is not complete")
    public = args.public_repository.resolve() / PUBLIC
    rows = {}

    def add(source, relative, classification="GENERATED_TEST_EVIDENCE"):
        data = source.read_bytes()
        if relative.startswith("evidence/") and source.suffix in (".log", ".patch", ".MF", ".java", ".c", ".cpp"):
            relative += ".txt"
        path = PREFIX + relative
        if path in rows:
            raise ValueError("duplicate public-host evidence target")
        rows[path] = {"source": str(source.resolve()), "path": path, "sha256": digest(data),
                      "size_bytes": len(data), "classification": classification}

    bundles = []
    for qualification in receipt["qualifications"]:
        dataset = qualification["dataset"]
        evidence = proof / (dataset + "-receiver-proof/evidence")
        files = []
        for source in sorted(evidence.glob("*")):
            if source.is_file():
                data = source.read_bytes()
                text = data.decode("utf-8", errors="strict")
                if text.encode("utf-8") != data:
                    raise ValueError("receiver trace is not round-trip UTF-8")
                files.append({"path": source.name, "sha256": digest(data),
                              "size_bytes": len(data), "utf8": text})
        if len(files) != 19:
            raise ValueError("expected18 stdout/stderr files plus receiver contract receipt")
        payload = {"schema": "M3_LOSSLESS_PUBLIC_RECEIVER_TRACE_BUNDLE_V1", "dataset": dataset,
                   "encoding": "Each utf8 value re-encodes to the exact original bytes, including final newlines",
                   "files": files}
        bundle = work / (dataset + "-receiver-traces.json")
        bundle.write_text(json.dumps(payload, indent=2) + "\n")
        decoded = json.loads(bundle.read_text())
        for entry in decoded["files"]:
            raw = entry["utf8"].encode("utf-8")
            if digest(raw) != entry["sha256"] or len(raw) != entry["size_bytes"] or raw != (evidence / entry["path"]).read_bytes():
                raise ValueError("lossless evidence bundle failed its round-trip check")
        add(bundle, EVIDENCE + dataset + "/receiver-traces.json")
        bundles.append({"dataset": dataset, "files": len(files), "bundle_sha256": digest(bundle.read_bytes())})
        build = public / "target" / dataset
        for leaf in ("SDK_INPUTS.tsv", "SDK_RESULTS.tsv"):
            add(build / "candidate" / leaf, EVIDENCE + dataset + "/" + leaf)
        for source in sorted((build / "surefire-reports").glob("*")):
            if source.is_file():
                add(source, EVIDENCE + dataset + "/" + source.name)
    for source in sorted(proof.glob("*.log")):
        add(source, EVIDENCE + source.name)
    add(proof / "receipt.json", EVIDENCE + "RECEIPT.json")
    authoring = Path(__file__).resolve().parent
    for name in ("verify_public_host_seven.py", "prepare_public_host_evidence.py"):
        add(authoring / name, "authoring/runtime-public-host-20261018/" + name,
            "FIRST_PARTY_PREPARATION_SOURCE")
    readme = work / "README.md"
    readme.write_text("""<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Public runtime recipe replay

All seven runtime datasets were freshly qualified by the public repository's
existing Maven host with its checked-in `sdk-owner` override. The run used no
private recipe source dependency. Each dataset passed the original five actual
OpenRewrite SDK cases, eight input-sealing tests and nine isolated receiver cases.

The recipes were then applied serially to source exported from the immutable
receiving commit. Each pass had an immediate zero-write replay. The 27 final
composed target files matched the built JDK receiving tree byte for byte, and
all 31 current receiver regression methods passed. Historical predecessor
manifests remain exact. An old documentation dataset is not silently allowed to
overwrite a newer scope-annotation successor.

`RECEIPT.json` records commands, source bindings and results. SDK input/result
manifests and complete JUnit XML/text reports accompany each dataset. Full Maven
and checker stdout/stderr are retained with `.txt` suffixes. Each dataset's
`receiver-traces.json` losslessly contains all 18 individual receiver stdout/stderr
files plus the case receipt. Re-encoding a `utf8` field gives the original bytes;
every length and SHA-256 was round-trip checked before this input was sealed.

These are source transformation and custody gates. The separately retained full
image/JNI results determine those runtime gates. The known universal Object
adaptation compatibility counterexamples remain a hold.
""")
    add(readme, EVIDENCE + "README.md", "FIRST_PARTY_EVIDENCE_DOCUMENTATION")
    bundle_receipt = work / "bundle-receipt.json"
    bundle_receipt.write_text(json.dumps({"schema": "M3_PUBLIC_HOST_EVIDENCE_BUNDLE_RECEIPT_V1",
                                          "status": "PASS", "datasets": bundles,
                                          "original_trace_files": sum(row["files"] for row in bundles),
                                          "trace_files_lost": 0, "bytes_changed": 0}, indent=2) + "\n")
    add(bundle_receipt, EVIDENCE + "bundle-receipt.json")
    output = {"schema": "M3_PUBLIC_HOST_SEVEN_CANONICAL_EVIDENCE_INPUT_V1",
              "files": [rows[path] for path in sorted(rows)]}
    path = work / "canonical-artifacts.json"
    path.write_text(json.dumps(output, indent=2) + "\n")
    print(json.dumps({"inventory": str(path), "files": len(rows),
                      "bytes": sum(row["size_bytes"] for row in rows.values()),
                      "sha256": digest(path.read_bytes()), "losslessly_bundled_trace_files": 133}))


if __name__ == "__main__":
    main()
