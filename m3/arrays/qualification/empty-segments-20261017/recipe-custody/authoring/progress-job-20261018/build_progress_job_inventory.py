#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Freeze source, recipe and honest bounded evidence for the additive ProgressJob handoff."""

import argparse
import base64
import hashlib
import json
from pathlib import Path, PurePosixPath


TASK = "m3-collections-contract-refinement-20261017"
DATASET = "m3jdk21-progress-job"
PREFIX = "synexia-openrewrite-recipes/recipe-crates/" + TASK
LABEL = "progress-job-20261018"
GPL = "GPL-2.0-only WITH Classpath-exception-2.0"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def json_bytes(value):
    return (json.dumps(value, indent=2) + "\n").encode()


def archive_name(relative):
    name = str(relative)
    if name.endswith((".java", ".log", ".patch", ".MF")):
        name += ".txt"
    return name


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--task-root", type=Path, required=True)
    parser.add_argument("--recipe-owner", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    task = args.task_root.resolve()
    owner = args.recipe_owner.resolve()
    crate = owner / "recipe-crates" / TASK
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    files = {}

    def add(source, destination, classification="GENERATED_EVIDENCE", license_name="Apache-2.0", role=None, **extra):
        relative = PurePosixPath(destination)
        if relative.is_absolute() or ".." in relative.parts or str(relative) != destination or destination in files:
            raise ValueError("duplicate or unsafe inventory path: " + destination)
        if source.is_symlink() or not source.is_file():
            raise ValueError("source must be a regular non-symlink file")
        data = source.read_bytes()
        data.decode("utf-8", errors="strict")
        if b"\0" in data or data.startswith(b"\xef\xbb\xbf"):
            raise ValueError("non-admitted text source")
        record = {"source": str(source.resolve()), "path": destination, "sha256": digest(data),
                  "size_bytes": len(data), "classification": classification, "source_license": license_name}
        if role:
            record["role"] = role
        record.update(extra)
        files[destination] = record

    def license_of(source):
        data = source.read_bytes()
        if b"SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0" in data[:2000] or b"DO NOT ALTER OR REMOVE COPYRIGHT NOTICES" in data[:2000]:
            return GPL
        if b"SPDX-License-Identifier: EPL-2.0" in data[:2000]:
            return "EPL-2.0"
        return "Apache-2.0"

    def add_tree(root, target, role, classification="GENERATED_EVIDENCE"):
        for source in sorted(root.rglob("*")):
            if not source.is_file():
                continue
            add(source, target + "/" + archive_name(source.relative_to(root)), classification,
                license_of(source), role)

    behavior = json.loads((task / "proof/behavior-v2/receipt.json").read_text())
    receiving = json.loads((task / "proof/receiving-v2/receipt.json").read_text())
    historical_behavior = json.loads((task / "proof/behavior-v1/receipt.json").read_text())
    if any(receipt["result"] != "PASS" for receipt in (behavior, receiving, historical_behavior)):
        raise ValueError("expected actual completed PASS receipts")
    if behavior["source_sha256"] != historical_behavior["source_sha256"]:
        raise ValueError("unexpected product change between diagnostic fixture passes")
    if set(behavior["new_runnable_subclass"]["diagnostic_keys"]) != {
            "compiler.err.override.incompatible.ret", "compiler.err.override.weaker.access"}:
        raise ValueError("final explicit Runnable conflict evidence missing")
    for source in sorted((task / "authoring").glob("*.py")):
        data = source.read_text()
        if "SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project" not in data.splitlines()[:5]:
            if not any(line == "# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project" for line in data.splitlines()[:5]):
                raise ValueError("canonical first-party header missing")
        add(source, PREFIX + "/authoring/" + LABEL + "/" + source.name, "FIRST_PARTY_PREPARATION_SOURCE",
            role="Reusable authorer, source verifier or receiving/evidence driver; no receiving output patched")
    for source in sorted((task / "authoring/templates").glob("*")):
        license_name = license_of(source)
        classification = "FIRST_PARTY_TARGET_LICENSED_TEMPLATE" if license_name == GPL else "FIRST_PARTY_PREPARATION_SOURCE"
        add(source, PREFIX + "/authoring/" + LABEL + "/templates/" + source.name, classification, license_name,
            "First-party task source; no upstream or third-party implementation copied into this template",
            source_origin="First-party LLM-assisted task authorship; original declared target/proof license retained")
    add_tree(crate / "datasets" / DATASET, PREFIX + "/datasets/" + DATASET,
             "Current qualified original Maven/OpenRewrite dataset; exact source postimages retain their own rights",
             "QUALIFIED_RECIPE_INPUT")
    add_tree(task / "preparation-v2", PREFIX + "/authoring/" + LABEL + "/inputs-v2",
             "Exact pinned existing dependency source and current candidate inputs; generated target sources are not hand-edited")
    add_tree(task / "proof/receiving-v2/sdk-results", PREFIX + "/evidence/" + LABEL + "/sdk-results-v2",
             "Actual OpenRewrite SDK results, input source bindings and patches")
    add_tree(task / "proof/receiving-v2/surefire-reports", PREFIX + "/evidence/" + LABEL + "/surefire-v2",
             "Actual original five-test SDK report; not a substituted runner")
    add_tree(task / "proof/recipe-v2/receiver/evidence", PREFIX + "/evidence/" + LABEL + "/receiver-contract-v2",
             "Actual nine original SDK receiver integrity cases")
    for source in sorted((task / "proof/receiving-v2").iterdir()):
        if source.is_file():
            add(source, PREFIX + "/evidence/" + LABEL + "/receiving-v2/" + archive_name(source.name),
                role="Actual ten-target check/apply and zero-write fixed point")
    for source in sorted((task / "proof/behavior-v2").iterdir()):
        if source.is_file():
            add(source, PREFIX + "/evidence/" + LABEL + "/behavior-v2/" + archive_name(source.name),
                role="Strict compiler, frozen stock client, real status and patched-image behavior evidence; no full new image claim")
    add(task / "recipe-v2.log", PREFIX + "/evidence/" + LABEL + "/maven-v2.log.txt",
        role="Full unchanged-host Maven verify output: five SDK, eight sealing and nine receiver cases")
    donor_root = task / "donors/eclipse-3.35.0"
    donor_receipt = json.loads((donor_root / "receipt.json").read_text())
    add(donor_root / "receipt.json", PREFIX + "/evidence/" + LABEL + "/eclipse/release-receipt.json",
        role="Artifact checksums and exact Maven source/POM SCM identities; jars remain external test dependencies")
    for artifact in donor_receipt["files"]:
        source = donor_root / artifact["file"]
        if digest(source.read_bytes()) != artifact["sha256"]:
            raise ValueError("frozen donor drift")
        if source.name.endswith(".pom"):
            add(source, PREFIX + "/evidence/" + LABEL + "/eclipse/" + source.name, "UPSTREAM_UNMODIFIED", "EPL-2.0",
                "Exact Eclipse-published Maven metadata; declared upstream rights retained", source_url=artifact["url"])
        for reviewed in artifact.get("reviewed_sources", []):
            add(donor_root / reviewed["file"], PREFIX + "/evidence/" + LABEL + "/eclipse/" + reviewed["file"],
                "UPSTREAM_UNMODIFIED", "EPL-2.0", "Exact original Eclipse source review input; not java.base donated code",
                source_revision=artifact["scm"]["tag"], source_url=artifact["scm"]["url"],
                git_blob=reviewed["git_blob"])
    github_root = task / "proof/eclipse-github-source"
    add(github_root / "receipt.json", PREFIX + "/evidence/" + LABEL + "/eclipse/github-crosscheck.json",
        role="Four release source files exactly match the pinned GitHub SCM sources")
    for source in sorted((github_root / "notices").glob("*")):
        add(source, PREFIX + "/evidence/" + LABEL + "/eclipse/notices/" + source.name,
            "UPSTREAM_UNMODIFIED", "EPL-2.0 AND embedded upstream notices", "Original Eclipse distribution about notice")

    binary_bundle = {"schema": "M3_PROGRESS_JOB_FROZEN_STOCK_CLIENT_BINARIES_V1",
                     "source_license": "Apache-2.0", "source": "OldCallableClient.java",
                     "purpose": "Lossless actual stock compiler outputs used unchanged in the reported finite client runs",
                     "classes": []}
    for source in sorted((task / "proof/behavior-v2/frozen-stock-client").rglob("*.class")):
        data = source.read_bytes()
        encoded = base64.b64encode(data).decode("ascii")
        if base64.b64decode(encoded) != data:
            raise ValueError("class evidence round trip failed")
        binary_bundle["classes"].append({"path": source.name, "sha256": digest(data), "bytes": len(data), "base64": encoded})
    binary_path = output / "frozen-stock-client-classes.json"
    binary_path.write_bytes(json_bytes(binary_bundle))
    add(binary_path, PREFIX + "/evidence/" + LABEL + "/frozen-stock-client-classes.json",
        role="Lossless exact three stock client class outputs; no product or donor runtime jar included")

    historical_files = []
    for relative in ("preparation-v1", "proof/receiving-v1", "proof/recipe-v1", "proof/behavior-v1"):
        for source in sorted((task / relative).rglob("*")):
            if not source.is_file() or source.suffix == ".class":
                continue
            data = source.read_bytes()
            try:
                value = data.decode("utf-8", errors="strict")
            except UnicodeDecodeError:
                raise ValueError("unexpected non-text historical evidence")
            if value.encode("utf-8") != data:
                raise ValueError("historical text round trip failed")
            historical_files.append({"path": source.relative_to(task).as_posix(), "sha256": digest(data),
                                     "bytes": len(data), "source_license": license_of(source), "utf8": value})
    history = {"schema": "M3_PROGRESS_JOB_PRIOR_QUALIFIED_PASS_V1", "status": "PRESERVED_HISTORY_NOT_FINAL_INPUT",
               "reason_for_successor": "Add public-hook return-type conflict witness alongside protected-access conflict; production ProgressJob source unchanged.",
               "validation": "Original v1 five SDK/eight sealing/nine receiver and 81 core/37 real Eclipse checks passed. V2 additionally asserts both compiler diagnostics.",
               "rights": "Per-entry original source license and UTF-8 content retained; enclosing generated evidence does not relicense embedded source.",
               "excluded": "Derived class files omitted; their actual compiler output hashes remain in the retained original receipt.",
               "files": historical_files}
    history_path = output / "prior-qualified-pass-v1.json"
    history_path.write_bytes(json_bytes(history))
    reread = json.loads(history_path.read_text())
    if any(digest(entry["utf8"].encode("utf-8")) != entry["sha256"] or len(entry["utf8"].encode("utf-8")) != entry["bytes"] for entry in reread["files"]):
        raise ValueError("historical evidence bundle failed independent decode/hash verification")
    add(history_path, PREFIX + "/evidence/" + LABEL + "/prior-qualified-pass-v1.json",
        "GENERATED_EVIDENCE_WITH_PRESERVED_PAYLOAD_RIGHTS", "Apache-2.0 AND GPL-2.0-only WITH Classpath-exception-2.0",
        "Lossless prior qualified source/recipe/test evidence with per-entry rights; no invented rerun")

    summary = {"schema": "M3_PROGRESS_JOB_ADDITIVE_HANDOFF_V1", "result": "BOUNDED_GATES_PASS_ADMISSION_PENDING",
               "dataset": DATASET, "target_base": "86528a90079b89a50b51ef13da08e7e70d468cb4",
               "source_sha256": behavior["source_sha256"], "compiled_class_sha256": behavior["new_class_sha256"],
               "recipe_gates": {"sdk": 5, "sealer": 8, "receiver": 9},
               "receiving": {"check": 10, "apply": 10, "replay": 0}, "behavior": behavior["checks"],
               "real_eclipse_dependency": "org.eclipse.platform:org.eclipse.equinox.common:3.21.0; original API types and jar bytes",
               "new_runnable_subclass": behavior["new_runnable_subclass"],
               "full_image_with_new_api": "NOT_RUN_PATCHED_IMAGE_ONLY", "jtreg_harness_new_test": "PENDING_FULL_FEATURE_IMAGE",
               "Object_universal_no_break": "HOLD_UNCHANGED", "phase": "STRING_UNCHANGED",
               "source_review": "Canonical agent independently inspected the source and fixtures; no material blocker for the approved Callable-only additive scope; no external certification claim.",
               "runtime_mutation": "No frozen repair worktree, JDK image or GitHub ref changed by this task",
               "composition": "May compose with separately qualified Runtime facade; no shared product target or mapping path.",
               "native": "No JNI/native code changed; reuses existing Java owners and FutureTask."}
    summary_path = output / "HANDOFF.json"
    summary_path.write_bytes(json_bytes(summary))
    add(summary_path, PREFIX + "/evidence/" + LABEL + "/HANDOFF.json", role="Actual bounded gates and explicit remaining admission limits")
    inventory = {"schema": "M3_PROGRESS_JOB_CANONICAL_ARTIFACTS_V1",
                 "canonical_baseline": "8f8200375d8d403b58e855ee5db02f2e8fac33a8",
                 "receiving_baseline": summary["target_base"], "dataset": DATASET,
                 "files": [files[key] for key in sorted(files)], "file_count": len(files),
                 "size_bytes": sum(value["size_bytes"] for value in files.values()),
                 "notes": ["Current original host and engines remain canonical unchanged owners; only new dataset/input/evidence paths are included.",
                           "Frozen v1 artifacts are retained losslessly as history; current v2 proof is not substituted for earlier bytes.",
                           "Eclipse binary/source jars remain external checksum-pinned dependencies; original source extracts/notices retain EPL rights.",
                           "Maven qualification and patch-only image witnesses are bounded; new API and ordered phase remain pending."]}
    for entry in inventory["files"]:
        if digest(Path(entry["source"]).read_bytes()) != entry["sha256"]:
            raise ValueError("source changed during inventory preparation")
    inventory_path = output / "canonical-artifacts.json"
    inventory_path.write_bytes(json_bytes(inventory))
    print(json.dumps({"inventory": str(inventory_path), "sha256": digest(inventory_path.read_bytes()),
                      "files": len(files), "bytes": inventory["size_bytes"], "historical_text_files": len(historical_files)}, sort_keys=True))


if __name__ == "__main__":
    main()
