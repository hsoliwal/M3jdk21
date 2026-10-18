#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Stage exact qualified parser/facade custody for the original Maven recipes.

The existing public host remains unchanged. This input builder creates a separate
host path and explicit source/evidence inventories; prepare/Maven/receive remains
the only transformation and receiving implementation.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json


OLD_PREFIX = "synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017"
NEW_PREFIX = "synexia-openrewrite-recipes/recipe-crates/m3-runtime-progress-facade-20261018"
PUBLIC_OLD = "m3/arrays/qualification/empty-segments-20261017/recipe-custody"
PUBLIC_NEW = "m3/runtime-integration/runtime-progress-facade-20261018/recipe-custody"
OLD_POM = "4c90faaa9e64aebeeba14fb2196731c6334293781d0fdcafbf190f2f5662eb7d"
NEW_POM = "824ced27a427e35901d413eb8c8beac0ee657d991a03079a5d1cc59e29f76092"
PARSER_JAR = "bc5fef92a854c72e8706fc58f545a8ac3e9e4395a052b50810366b94869aef52"
PUBLIC_BASELINE = "86528a90079b89a50b51ef13da08e7e70d468cb4"
CANONICAL_BASELINE = "8f8200375d8d403b58e855ee5db02f2e8fac33a8"
CONTROLS = ("prepare.py", "receive.py", "ENGINE_SOURCES.tsv",
            "src/test/java/com/synexia/rewrite/M3TaskRecipeTest.java",
            "contract/test_prepare.py", "contract/verify_receiver.py")
ENGINES = ("M3HashPinnedJavaSnapshotRecipe.java", "M3HashPinnedTextSnapshotRecipe.java",
           "M3ScopeInference.java", "M3EditScope.java")


def sha(data):
    return hashlib.sha256(data).hexdigest()


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def archived(relative):
    """Retain exact source bytes using an admitted text suffix for evidence."""
    relative = relative.replace("$", "_inner_")
    path = Path(relative)
    if path.suffix in (".java", ".log", ".patch", ".MF", ".sha256") or not path.suffix:
        return relative + ".txt"
    return relative


def main():
    parser = argparse.ArgumentParser()
    for name in ("original-crate", "recipe-owner", "public-root", "authoring", "proof-root", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    origin = args.original_crate.resolve()
    owner = args.recipe_owner.resolve()
    public = args.public_root.resolve()
    authoring = args.authoring.resolve()
    proof = args.proof_root.resolve()
    output = args.output.resolve()
    if output.exists() or output.is_relative_to(public):
        raise ValueError("a fresh non-receiving staging directory is required")
    prepare = load("facade_custody_original_prepare", origin / "prepare.py")
    receiver = load("facade_custody_original_receiver", origin / "receive.py")
    if sha((origin / "pom.xml").read_bytes()) != OLD_POM:
        raise ValueError("original host epoch changed")
    parser_sdk = receiver.qualification(origin, "m3-openrewrite-javadoc", owner)
    host_sdk = receiver.qualification(origin, "m3-runtime-parser-host", owner)
    host = host_sdk["candidate"] / NEW_PREFIX
    if sha((host / "pom.xml").read_bytes()) != NEW_POM:
        raise ValueError("successor host epoch changed")
    successor = load("facade_custody_successor_receiver", host / "receive.py")
    fixture_sdk = successor.qualification(host, "fixture", owner)
    facade_sdk = successor.qualification(host, "m3jdk21-runtime-facade", owner)
    for relative in CONTROLS:
        data = (origin / relative).read_bytes()
        if (host / relative).read_bytes() != data or (public / PUBLIC_OLD / relative).read_bytes() != data:
            raise ValueError("original/public/successor control differs: " + relative)
    for name in ENGINES:
        relative = "src/main/java/com/synexia/rewrite/" + name
        if (owner / relative).read_bytes() != (public / PUBLIC_OLD / "sdk-owner" / relative).read_bytes():
            raise ValueError("existing public engine differs: " + name)
    for label in ("openrewrite-parser-module-v4", "runtime-facade-patch-v1"):
        receipt = json.loads((proof / label / "receipt.json").read_text())
        if receipt["status"] != "PASS":
            raise ValueError("required semantic proof is not qualified: " + label)
        qualified = parser_sdk if label.startswith("openrewrite") else facade_sdk
        if receipt["sdk_inputs_sha256"] != sha((qualified["candidate"] / "SDK_INPUTS.tsv").read_bytes()):
            raise ValueError("semantic proof binds different recipe inputs: " + label)
        if receipt["sdk_results_sha256"] != sha((qualified["candidate"] / "SDK_RESULTS.tsv").read_bytes()):
            raise ValueError("semantic proof binds different recipe outputs: " + label)
    parser_receipt = json.loads((proof / "openrewrite-parser-module-v4/receipt.json").read_text())
    if parser_receipt["artifact_sha256"] != PARSER_JAR:
        raise ValueError("source-qualified parser artifact changed")
    selected = {}

    def add(source, relative, canonical=None, classification="GENERATED"):
        prepare.relative_path(relative)
        if source.is_symlink() or not source.is_file():
            raise ValueError("custody source must be an ordinary file: " + str(source))
        data = prepare.text_bytes(source)
        target = PUBLIC_NEW + "/" + relative
        canonical = canonical or NEW_PREFIX + "/" + relative
        prepare.relative_path(canonical)
        row = {"source": str(source), "path": canonical, "public_path": target,
               "sha256": sha(data), "size_bytes": len(data), "classification": classification}
        if target in selected and selected[target][0] != data:
            raise ValueError("conflicting custody sources: " + target)
        selected[target] = (data, row)

    # Only the 57 qualified host outputs are copied; no target/classes/cache files.
    for relative in sorted(host_sdk["outputs"]):
        if not relative.startswith(NEW_PREFIX + "/"):
            raise ValueError("bootstrap recipe contains a different host owner")
        add(host_sdk["candidate"] / relative, relative[len(NEW_PREFIX) + 1:])
    # Original generating recipes are retained as reviewable bootstrap sources.
    # Their whole source binding is kept, including pre-seal historical payloads.
    for dataset in ("m3-openrewrite-javadoc", "m3-runtime-parser-host"):
        for source in sorted((origin / "datasets" / dataset).rglob("*")):
            if source.is_file():
                relative = source.relative_to(origin).as_posix()
                add(source, "bootstrap-source/" + relative, OLD_PREFIX + "/" + relative)
    for name in ("prepare_runtime_facade.py", "prepare_parser_successor.py", "prepare_runtime_facade_host.py",
                 "verify_runtime_facade.py", "verify_parser_successor.py", "build_runtime_facade_handoff.py",
                 "BASELINE_SOURCES.json", "FOSS_REUSE_DECISION.tsv", "PARSER_DONOR_DECISION.json"):
        classification = "FIRST_PARTY" if Path(name).suffix == ".py" else "FIRST_PARTY_NO_HEADER"
        add(authoring / name, "authoring/" + name, classification=classification)
    for source in sorted((authoring / "templates").glob("*.java")):
        add(source, "authoring/templates/src/test/java/" + source.name, classification="FIRST_PARTY")
    for source in sorted((authoring / "donor-inspection").rglob("*")):
        if source.is_file():
            add(source, "authoring/donor-inspection/" + archived(source.relative_to(authoring / "donor-inspection").as_posix()),
                classification="FIRST_PARTY_NO_HEADER" if source.suffix == ".json" else "UPSTREAM")
    for name in ("UPSTREAM_RECEIPT.json", "GITHUB_SOURCE_BLOBS.json", "rewrite-java-21-8.17.1.pom", "UPSTREAM_BUILD.gradle.kts.txt"):
        source = authoring / "parser-upstream" / name
        if not source.is_file():
            raise ValueError("missing pinned donor identity: " + name)
        add(source, "authoring/parser-upstream/" + (name + ".txt" if name.endswith(".pom") else name),
            classification="UPSTREAM" if name.endswith((".pom", ".txt")) else "FIRST_PARTY_NO_HEADER")
    # Fresh recipe results and all original receiver observation logs.
    recipe_proofs = (
        ("recipe-openrewrite-javadoc-v3", parser_sdk),
        ("recipe-runtime-parser-host-v1", host_sdk),
        ("recipe-runtime-successor-fixture-v1", fixture_sdk),
        ("recipe-runtime-facade-successor-v1", facade_sdk),
    )
    for label, qualified in recipe_proofs:
        for source in sorted((proof / label).rglob("*")):
            if source.is_file():
                add(source, "evidence/" + label + "/" + archived(source.relative_to(proof / label).as_posix()))
        for source, name in ((qualified["candidate"] / "SDK_INPUTS.tsv", "SDK_INPUTS.tsv"),
                             (qualified["candidate"] / "SDK_RESULTS.tsv", "SDK_RESULTS.tsv"),
                             (qualified["report"], "SDK_TESTS.xml")):
            add(source, "evidence/" + label + "/" + name)
    # Preserve the original parser refusal and the unsuccessful source/POM atoms.
    for label in ("recipe-runtime-facade-v1", "recipe-openrewrite-javadoc-v1", "recipe-openrewrite-javadoc-v2",
                  "recipe-runtime-facade-custody-1-v1"):
        base = proof / label
        for name in ("maven.log", "receipt.json"):
            add(base / name, "evidence/" + label + "/" + archived(name))
        for source in sorted((base / "retained").rglob("*")):
            if not source.is_file():
                continue
            relative = source.relative_to(base).as_posix()
            # Each failed dataset already retains its exact source templates.
            # Keep SDK bindings, reports and patches without duplicate candidate trees.
            if "/candidate/" in relative and source.name not in ("SDK_INPUTS.tsv", "SDK_RESULTS.tsv", "java.patch", "text.patch"):
                continue
            add(source, "evidence/" + label + "/" + archived(relative))
    for label in ("openrewrite-parser-module-v1", "openrewrite-parser-module-v2",
                  "openrewrite-parser-module-v3", "openrewrite-parser-module-v4"):
        base = proof / label
        for source in sorted(base.iterdir()):
            if source.is_file():
                add(source, "evidence/" + label + "/" + archived(source.name))
        for variant in ("baseline", "candidate"):
            report = base / variant / "rewrite-java-21/target/surefire-reports/TEST-M3RuntimeJavadocParserTest.xml"
            if report.is_file():
                add(report, "evidence/" + label + "/" + variant + "-TESTS.xml")
    # Preserve only logs/receipts; the published recipe retains the tested Java.
    for label in ("runtime-facade-patch-v1", "runtime-facade-baseline-compile", "openrewrite-parser-debug-line-audit"):
        base = proof / label
        for source in sorted(base.iterdir()):
            if source.is_file() and source.suffix in (".json", ".log", ".txt"):
                add(source, "evidence/" + label + "/" + archived(source.name))
    source_provenance = [row for _, row in sorted(selected.values(), key=lambda item: item[1]["path"])]
    metadata = {
        "schema": "M3_RUNTIME_FACADE_SEPARATE_PUBLIC_CUSTODY_V1",
        "canonical_baseline": CANONICAL_BASELINE, "receiving_baseline": PUBLIC_BASELINE,
        "canonical_host": NEW_PREFIX, "public_host": PUBLIC_NEW,
        "existing_public_engine_owner": PUBLIC_OLD + "/sdk-owner",
        "old_host_pom_sha256": OLD_POM, "successor_pom_sha256": NEW_POM,
        "parser_artifact_sha256": PARSER_JAR, "unchanged_original_controls": len(CONTROLS),
        "unchanged_existing_public_engines": len(ENGINES),
        "qualification": {"parser_source_targets": 15, "host_bootstrap_targets": 57,
            "runtime_facade_targets": 6, "sdk_tests_per_dataset": 5, "sealing_tests": 8,
            "receiver_cases": 9, "successor_parser_identity_cases": 4,
            "parser_baseline_tests": 4, "parser_baseline_failures": 3,
            "parser_candidate_tests": 4, "parser_candidate_failures": 0,
            "runtime_patch_commands": 22, "legacy_checks": 12, "facade_checks_each_of_four_modes": 84},
        "phase": "STRING", "promotion": "PENDING_FULL_IMAGE_API_REVIEW_AND_ORDERED_PHASE",
        "publication": "A later receipt pins the canonical successor commit; this inventory itself makes no publication claim.",
        "bootstrap_source_note": "Complete qualified source bindings are retained. Unreferenced payload names from a pre-qualification host staging revision remain bound input bytes; the active manifests select exactly 57 host outputs. They do not add receiving targets.",
        "files": source_provenance,
    }
    output.mkdir(parents=True)
    generated = output / "generated"
    generated.mkdir()
    metadata_path = generated / "CUSTODY-SOURCE.json"
    metadata_path.write_text(json.dumps(metadata, indent=2) + "\n")
    add(metadata_path, "CUSTODY-SOURCE.json", classification="FIRST_PARTY_NO_HEADER")
    files = [row for _, row in sorted(selected.values(), key=lambda item: item[1]["path"])]
    # Deterministic batches remain below the existing 256-target text budget.
    # The original Java and text engines still enforce their own scopes/budgets.
    public_rows = sorted(selected.items())
    batches = []
    for index, begin in enumerate(range(0, len(public_rows), 250), 1):
        batch_root = output / ("public-candidates-" + str(index))
        rows = public_rows[begin:begin + 250]
        for relative, (data, _) in rows:
            target = batch_root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
        batches.append({"dataset": "m3-runtime-facade-custody-v2-" + str(index),
                        "candidate_root": str(batch_root), "targets": len(rows)})
    inventory = {"schema": "M3_CANONICAL_RUNTIME_FACADE_INVENTORY_V1",
        "canonical_baseline": CANONICAL_BASELINE, "receiving_baseline": PUBLIC_BASELINE,
        "notes": "Exact source-built parser, separate host, recipe sources and retained paired evidence. No receiving product mutation or phase promotion is performed by this builder.",
        "files": files}
    (output / "canonical-artifacts.json").write_text(json.dumps(inventory, indent=2) + "\n")
    (output / "custody-batches.json").write_text(json.dumps({"public_host": PUBLIC_NEW,
        "public_baseline": PUBLIC_BASELINE, "batches": batches}, indent=2) + "\n")
    print(json.dumps({"files": len(files), "size_bytes": sum(row["size_bytes"] for row in files),
        "batches": batches, "original_controls_verified": 6, "public_engine_sources_verified": 4}))


if __name__ == "__main__":
    main()
