#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Freeze only the current recovery's actual SDK-bound sources and compact evidence."""

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath

from prepare_public_recovery import INNER, PIN, TASK


PREFIX = "synexia-openrewrite-recipes/recipe-crates/" + TASK
LABEL = "current-runtime-recovery-20261018"
DELIVERED = "86528a90079b89a50b51ef13da08e7e70d468cb4"
GPL = "GPL-2.0-only WITH Classpath-exception-2.0"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def archive_name(name):
    return name + ".txt" if name.endswith((".java", ".log", ".patch", ".MF", ".pom", ".cpp", ".c")) else name


def target_rights(path):
    if path.startswith("src/hotspot/"):
        return "GPL-2.0-only"
    if path.startswith(("src/java.base/", "test/jdk/")):
        return GPL
    if path.startswith("m3/"):
        return "Apache-2.0"
    raise ValueError("unclassified target owner: " + path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("task-root", "recipe-owner", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    task = args.task_root.resolve()
    crate = args.recipe_owner.resolve() / "recipe-crates" / TASK
    proof = task / "proof/receiving-v2"
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    receipt = json.loads((proof / "RECEIPT.json").read_text())
    if receipt["result"] != "PASS" or receipt["baseline"] != PIN or len(receipt["files"]) != 31:
        raise ValueError("current actual product qualification is missing")
    targets = {entry["path"]: entry for entry in receipt["files"]}
    records = {}

    def add(source, relative, classification="GENERATED_EVIDENCE", license_name="Apache-2.0", role=None, **extra):
        destination = PREFIX + "/" + relative
        path = PurePosixPath(destination)
        if path.is_absolute() or ".." in path.parts or str(path) != destination or destination in records:
            raise ValueError("duplicate or unsafe inventory destination")
        if not source.is_file() or source.is_symlink():
            raise ValueError("inventory source must be a regular file")
        data = source.read_bytes()
        data.decode("utf-8", errors="strict")
        if b"\0" in data or data.startswith(b"\xef\xbb\xbf"):
            raise ValueError("non-admitted source bytes")
        entry = {"source": str(source.resolve()), "path": destination, "sha256": sha(data),
                 "size_bytes": len(data), "classification": classification, "source_license": license_name}
        if role:
            entry["role"] = role
        entry.update(extra)
        records[destination] = entry

    for source in sorted((task / "authoring").glob("*.py")):
        header = source.read_text().splitlines()[:5]
        if ("# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project" not in header
                or "# SPDX-License-Identifier: Apache-2.0" not in header):
            raise ValueError("new first-party authorer must retain the full canonical SPDX pair")
        add(source, "authoring/" + LABEL + "/" + source.name, "FIRST_PARTY_PREPARATION_SOURCE",
            role="Reusable current-source admission, tests, original receiver or custody preparation; no target output patching")

    dataset = crate / "datasets" / INNER
    payloads = {}
    for manifest in sorted((dataset / "resources").rglob("manifest.tsv")):
        for line in manifest.read_text().splitlines():
            if not line or line.startswith("#"):
                continue
            target, before, after, template = line.split("\t")
            for source, identity, kind in ((manifest.parent / template, after, "postimage"),
                                           (manifest.parent / "pre" / template, before, "preimage")):
                if identity == "ABSENT":
                    continue
                if sha(source.read_bytes()) != identity:
                    raise ValueError("manifest source payload drift")
                payloads[source.relative_to(crate).as_posix()] = (target, kind)
    bindings = {}
    for line in (proof / "sdk-results/SDK_INPUTS.tsv").read_text().splitlines():
        if line and not line.startswith("#"):
            key, identity = line.split("\t")
            if key in bindings:
                raise ValueError("duplicate SDK input binding")
            bindings[key] = identity
    bound_dataset = {key.removeprefix("crate/"): identity for key, identity in bindings.items()
                     if key.startswith("crate/datasets/" + INNER + "/")}
    actual_dataset = {source.relative_to(crate).as_posix() for source in dataset.rglob("*") if source.is_file()}
    if set(bound_dataset) != actual_dataset or len(bound_dataset) != 55:
        raise ValueError("current SDK-bound dataset closure changed")
    for relative, identity in sorted(bound_dataset.items()):
        source = crate / relative
        if sha(source.read_bytes()) != identity:
            raise ValueError("qualified dataset input changed")
        if relative in payloads:
            target, kind = payloads[relative]
            license_name = target_rights(target)
            revision = PIN if kind == "preimage" else DELIVERED
            if kind == "postimage" and (target == "m3/backports/ALGORITHM_CATALOGUE.tsv"
                                        or "merged-repair-recovery-20261018" in target):
                revision = "FIRST_PARTY_CURRENT_SOURCE_COMPOSITION_NO_REMOTE_COMMIT_CLAIM"
            add(source, relative, "QUALIFIED_RECIPE_INPUT_WITH_PRESERVED_PAYLOAD_RIGHTS", license_name,
                "Exact " + kind + " payload; target rights retained independently of the recipe host license",
                target_path=target, source_revision=revision, payload_kind=kind)
        else:
            add(source, relative, "QUALIFIED_RECIPE_INPUT", role="Exact source binding from the original real SDK execution")

    sdk = crate / "target" / INNER / "candidate"
    for source in sorted((proof / "sdk-results").rglob("*")):
        if not source.is_file():
            continue
        relative = source.relative_to(proof / "sdk-results").as_posix()
        live = sdk / relative
        if source.read_bytes() != live.read_bytes():
            raise ValueError("current SDK output differs from retained actual proof")
        if relative in targets:
            if sha(source.read_bytes()) != targets[relative]["sha256"]:
                raise ValueError("qualified product output drift")
            rights = target_rights(relative)
        elif relative == "java.patch":
            rights = "Apache-2.0 AND (" + GPL + ")"
        elif relative == "text.patch":
            rights = "Apache-2.0 AND GPL-2.0-only AND (" + GPL + ")"
        elif relative in ("SDK_INPUTS.tsv", "SDK_RESULTS.tsv"):
            rights = "Apache-2.0"
        else:
            raise ValueError("unexpected SDK output")
        add(live, "evidence/" + LABEL + "/sdk-results/" + archive_name(relative),
            "GENERATED_EVIDENCE_WITH_PRESERVED_PAYLOAD_RIGHTS", rights,
            "Actual SDK output; patch rights derive from included target owners, not header-text heuristics")

    for source in sorted((proof / "surefire-reports").iterdir()):
        if source.is_file():
            live = crate / "target" / INNER / "surefire-reports" / source.name
            if live.read_bytes() != source.read_bytes():
                raise ValueError("retained SDK report differs from the live qualification binding")
            add(live, "evidence/" + LABEL + "/surefire/" + archive_name(source.name),
                role="Actual original five SDK test report")
    for source in sorted((proof / "original-contract-proof/evidence").iterdir()):
        if source.is_file():
            add(source, "evidence/" + LABEL + "/receiver-contract/" + archive_name(source.name),
                role="Actual original nine receiving-integrity cases")
    for source in sorted(proof.iterdir()):
        if source.is_file():
            add(source, "evidence/" + LABEL + "/receiving/" + archive_name(source.name),
                role="Actual product original Maven5/8/9 and admission9/check31/apply31/replay0 evidence")
    add(task / "preparation-v1/PREPARATION.json", "evidence/" + LABEL + "/PREPARATION.json",
        role="Exact current07 source admission and immutable earlier-delivery identities")
    add(task / "proof/receiver-driver-path-failure-v1.json", "evidence/" + LABEL + "/receiver-driver-path-failure-v1.json",
        role="Lossless retained Apache driver-path failure, prior to correcting only the evidence lookup; no product recipe change")
    summary = {"schema": "M3_CURRENT_RUNTIME_RESTORATION_HANDOFF_V1", "result": "BOUNDED_GATES_PASS",
               "baseline": PIN, "earlier_delivery": DELIVERED, "dataset": INNER,
               "targets": 31, "historical_targets": 27, "exact_original_preimages": 26,
               "current_catalogue": "Preserve the complete current byte prefix including ALG-NUMERIC-COPY-RUNS; append five missing exact historical runtime rows.",
               "production": "Fourteen exact earlier delivered postimages; the earlier image identity is not relabelled as a current baseline build.",
               "gates": {"sdk": 5, "sealing": 8, "receiver": 9, "preparation": 9,
                         "check": 31, "apply": 31, "replay": 0},
               "current_image": "Separate parent-owned all-build-input identity and image gate; no image run by this source/custody authorer.",
               "Object_no_break": "HOLD_UNCHANGED", "phase": "STRING_UNCHANGED",
               "source_closure": "Only all55 current SDK-bound dataset files, exact candidate outputs and compact proof. Original seven recipes are consumed by immutable identity, not recopied.",
               "custody": "Separate original-host text recipe and fresh public-sdk-owner-only replay follow; no recursive self-inclusion."}
    summary_path = output / "HANDOFF.json"
    summary_path.write_text(json.dumps(summary, indent=2) + "\n")
    add(summary_path, "evidence/" + LABEL + "/HANDOFF.json", role="Bounded actual gate results and explicit source/evidence scope")
    inventory = {"schema": "M3_CURRENT_RUNTIME_RESTORATION_CANONICAL_ARTIFACTS_V1", "dataset": INNER,
                 "canonical_baseline": "8f8200375d8d403b58e855ee5db02f2e8fac33a8", "receiving_baseline": PIN,
                 "files": [records[key] for key in sorted(records)], "file_count": len(records),
                 "size_bytes": sum(entry["size_bytes"] for entry in records.values()),
                 "complete_sdk_bound_dataset_file_count": len(bound_dataset),
                 "rights": "Per-target GPL-only, GPL+Classpath or Apache source rights are retained in every payload and generated diff."}
    for entry in inventory["files"]:
        if sha(Path(entry["source"]).read_bytes()) != entry["sha256"]:
            raise ValueError("inventory source changed while freezing")
    path = output / "canonical-artifacts.json"
    path.write_text(json.dumps(inventory, indent=2) + "\n")
    print(json.dumps({"inventory": str(path), "sha256": sha(path.read_bytes()),
                      "files": len(records), "bytes": inventory["size_bytes"]}, sort_keys=True))


if __name__ == "__main__":
    main()
