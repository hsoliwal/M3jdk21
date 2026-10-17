#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Seal one final central-registry snapshot through two exact preimage routes.

This script writes proof resources only. Product files are emitted by the existing
Maven/OpenRewrite text snapshot recipe exercised by M3ASTRegistryRecipeTest.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
FRONTEND = HERE.parent
SYNEXIA = HERE.parents[4]
MAP_PATH = "m3/docs/name-mapping.json"
SCHEMA_PATH = "m3/docs/name-mapping.schema.json"
BASE_SHA = "03434d125f8d758a5da24f55adfdc399f6cc828f8fa5b214f1cfe8777d635283"
INTERMEDIATE_SHA = "d427edb3c78cd931204ee92d6fc734a769d841c483c3f1180e600b5a0b6c2561"
SCHEMA_SHA = "c29f34e5b5d858b60cddd43b2e7f273d9c01ad553cb0e3a5dcc568698646f70b"
SOURCE_BASE = "e244d68fdee88d4a406be48e4ff1d31b60a17988"
TARGET_BASE = "4e29dc9d4141261e8763fbe9ffd6a20e017b1aff"
CRATE = "m3-ast-registry-20261017"
SUCCESSOR_CRATE = "m3-ast-registry-hierarchy-20261017"
PENDING = "synexia.counterpart.MIndexCompilerASTPrecompute"
HANDOFF = "m3/compiler/ast-sharing-20261017/handoff.json"
VENDOR = "m3/vendor/synexia/"
FRONTEND_REL = str(FRONTEND.relative_to(SYNEXIA))
CACHE_REL = str((FRONTEND.parent / "cache").relative_to(SYNEXIA))
HIERARCHY_REL = str((FRONTEND / "hierarchy").relative_to(SYNEXIA))
HIERARCHY_PROOF = VENDOR + HIERARCHY_REL + "/evidence/QUALIFICATION.json"
PROOF = VENDOR + FRONTEND_REL + "/runtime-tests/evidence/QUALIFICATION.json"
OWNERS = {
    "M3HashPinnedJavaSnapshotRecipe.java": "67f7b81126e4736c7b61b444fa6545153a61e23c012975ca796fcee623ab6625",
    "M3HashPinnedTextSnapshotRecipe.java": "fbf1ea9efad784afef5ac201ff7fc324551f77a099ebfe2f79b70c3a17c731ef",
}
LEVELS = {
    "L0": "per-file facts and atoms",
    "L1": "file summary",
    "L2": "package",
    "L3": "module",
    "L4": "project",
    "L5": "repository",
}
RESOURCE = HERE / "src/main/resources/com/synexia/rewrite/hash-pinned-text" / CRATE


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def encoded(value: object) -> bytes:
    return (json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def template(manifest: Path, target: str, inputs: dict[str, str]) -> dict[str, str]:
    inputs[str(manifest.relative_to(SYNEXIA))] = sha(manifest.read_bytes())
    rows = {}
    for line in manifest.read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        path, before, after, resource = line.split("\t")
        require(path not in rows, "duplicate source manifest target")
        rows[path] = (before, after, resource)
    before, after, resource = rows[target]
    path = manifest.parent / resource
    require(sha(path.read_bytes()) == after, "source template drift: " + target)
    relative = str(path.relative_to(SYNEXIA))
    inputs[relative] = after
    return {"path": target, "preimage_sha256": before,
            "template": VENDOR + relative, "template_sha256": after}


def manifest(root: Path, resource_root: str, name: str) -> Path:
    return root / resource_root / "com/synexia/rewrite/hash-pinned-java" / name / "manifest.tsv"


def prepare(target_root: Path) -> None:
    fixture = HERE / "preimages" / MAP_PATH
    schema_fixture = HERE / "guards" / SCHEMA_PATH
    baseline = fixture.read_bytes() if fixture.exists() else (target_root / MAP_PATH).read_bytes()
    intermediate_fixture = HERE / "preimages/hierarchy" / MAP_PATH
    intermediate = intermediate_fixture.read_bytes()
    schema = (target_root / SCHEMA_PATH).read_bytes()
    require(sha(baseline) == BASE_SHA, "unrecognized central registry preimage")
    require(sha(intermediate) == INTERMEDIATE_SHA, "unrecognized hierarchy registry preimage")
    require(sha(schema) == SCHEMA_SHA, "name-mapping schema drift")
    for name, expected in OWNERS.items():
        require(sha((SYNEXIA / "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite" / name).read_bytes()) == expected,
                "shared exact recipe owner drift: " + name)
    before = json.loads(baseline)
    after = copy.deepcopy(before)
    inputs: dict[str, str] = {}
    canonical = manifest(FRONTEND, "src/main/resources", "m3-ast-frontend-canonical-20261017")
    receiver = manifest(FRONTEND, "src/main/resources", "m3-ast-frontend-receiver-20261017")
    cache = FRONTEND.parent / "cache"
    cache_source = manifest(cache, "src/main/resources", "m3-ast-cache-canonical-20261017")
    cache_target = manifest(cache, "src/main/resources", "m3-ast-cache-target-20261017")
    rules = {
        "M3File": "Immutable admitted parser characters with independent original-byte and exact UTF16 source identities.",
        "M3CompilationContext": "Ordered content and generation facts define context identity; incomplete environments receive isolated non-reusable session authority.",
        "M3AST": "Immutable source-qualified syntax projection over the existing canonical MIndexAST payload owner.",
        "M3JavacASTIndex": "Lazy shared projection of the actual JavacTask unit; live attributed objects are borrowed only during synchronous compiler-thread callbacks.",
        "M3ASTCodec": "Bounded checksummed syntax/source records preserve the raw UTF16 digest and exact atom-owner identity.",
        "M3ASTPlugin": "Optional build-tool plugin observes the actual compiler invocation and records its actual diagnostic error count.",
        "M3FileSourceFacts": "L0 exact-source/context/provider/schema/phase facts; SHA-256 identities preserve raw UTF16 without normalization.",
        "M3ASTWorkspaceCache": "Thin isolated adapter over existing TieredSwapCache and SwapStore owners; active leases protect projections, and declared resident weight is separate from process memory.",
    }
    additions = []
    for name, rule in rules.items():
        is_cache = name in {"M3FileSourceFacts", "M3ASTWorkspaceCache"}
        source_path = "synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/ast/" + name + ".java"
        target_path = "m3/ports/compiletime/src/main/java/com/m3/compiletime/ast/" + name + ".java"
        source = template(cache_source if is_cache else canonical, source_path, inputs)
        destination = template(cache_target if is_cache else receiver, target_path, inputs)
        entry = {
            "id": "m3.ast-sharing.20261017." + name,
            "source": "com.synexia.mindex.compiler.ast." + name,
            "target": "com.m3.compiletime.ast." + name,
            "status": "finite source/compiler contract qualification; global receiving gates unchanged",
            "source_repository": "hsoliwal/com.synexia",
            "source_base_revision": SOURCE_BASE,
            "source_commit": None,
            "source_commit_receipt": HANDOFF,
            "source_path": source["path"],
            "source_template": source["template"],
            "source_template_sha256": source["template_sha256"],
            "target_path": destination["path"],
            "target_preimage_sha256": destination["preimage_sha256"],
            "target_template": destination["template"],
            "target_template_sha256": destination["template_sha256"],
            "source_owner": "synexia-mindex/compiler",
            "target_owner": "m3/ports/compiletime",
            "recipe": "com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe",
            "source_recipe_crate": "m3-ast-cache-canonical-20261017" if is_cache else "m3-ast-frontend-canonical-20261017",
            "target_recipe_crate": "m3-ast-cache-target-20261017" if is_cache else "m3-ast-frontend-receiver-20261017",
            "target_rule": rule,
            "proof": VENDOR + CACHE_REL + "/evidence/QUALIFICATION.json" if is_cache else PROOF,
            "handoff": HANDOFF,
            "license": "Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors",
        }
        if name == "M3FileSourceFacts":
            entry["leaf_first_hierarchy"] = LEVELS
            entry["hierarchy_scope"] = "This mapping owns exact L0 source facts. The separately qualified M3ASTHierarchy mapping implements lazy provider evaluation and dependency-qualified aggregation from L0 through L5."
            entry["hierarchy_mapping"] = "m3.ast-sharing.20261017.M3ASTHierarchy"
        if name == "M3ASTPlugin":
            entry["plugin_name"] = "M3ASTIndex"
            entry["existing_builtin_plugin"] = "M3AST at com.sun.tools.javac.m3.M3ASTPC remains the existing compiler-owned service"
            entry["build_support"] = {
                "target_guide": "m3/tooling/module-packs/BUILD_ADAPTERS.md",
                "recipe_custody": VENDOR + "synexia-openrewrite-recipes/recipe-crates/m3-build-support-20261017/",
                "adapter_proof": VENDOR + "synexia-openrewrite-recipes/recipe-crates/m3-build-support-20261017/evidence/FINAL_BUILD_ADAPTER_RECEIPT.json",
            }
        additions.append(entry)

    compiler = manifest(FRONTEND, "src/main/resources", "m3-ast-frontend-compiler-20261017")
    hook = template(compiler, "src/jdk.compiler/share/classes/com/sun/tools/javac/main/JavaCompiler.java", inputs)
    observer = template(compiler, "src/jdk.compiler/share/classes/com/sun/tools/javac/m3/M3ASTPC.java", inputs)
    public_api = template(compiler, "src/jdk.compiler/share/classes/com/sun/source/util/M3AST.java", inputs)
    additions.append({
        "id": "m3.ast-sharing.20261017.JavaCompiler-source-observer",
        "source": "com.sun.tools.javac.main.JavaCompiler parser input/result hook",
        "target": "com.sun.tools.javac.m3.M3ASTPC",
        "status": "limited exact-source observer qualified; historical full compiler-precompute counterpart remains pending",
        "source_repository": "hsoliwal/M3jdk21",
        "source_base_revision": TARGET_BASE,
        "source_commit": None,
        "source_commit_receipt": HANDOFF,
        "source_path": hook["path"],
        "source_preimage_sha256": hook["preimage_sha256"],
        "source_template": hook["template"],
        "source_template_sha256": hook["template_sha256"],
        "target_path": observer["path"],
        "target_preimage_sha256": observer["preimage_sha256"],
        "target_template": observer["template"],
        "target_template_sha256": observer["template_sha256"],
        "source_owner": "jdk.compiler; Synexia stores recipe custody only",
        "target_owner": "existing reserved com.sun.tools.javac.m3.M3ASTPC",
        "recipe": "com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe",
        "target_recipe_crate": "m3-ast-frontend-compiler-20261017",
        "target_rule": "Task-scoped publication of the exact parser input and existing compilation unit, including generated sources, composed into the existing M3ASTPC plugin and public M3AST provider API; no extra parser and no Synexia dependency in jdk.compiler.",
        "preserved_public_api": public_api,
        "upstream_contract_proof": VENDOR + FRONTEND_REL + "/upstream-contract/evidence/QUALIFICATION.json",
        "full_counterpart_record": PENDING,
        "proof": PROOF,
        "handoff": HANDOFF,
        "license": "Preserve OpenJDK/module per-file GPL-2.0-only WITH Classpath-exception-2.0 and copyright notices.",
    })

    name = "M3OpenRewriteSharedAST"
    source = template(manifest(FRONTEND / "openrewrite", "resources", "m3-openrewrite-shared-ast-20261017"),
                      "synexia-mindex/compiler-openrewrite/src/main/java/com/synexia/mindex/compiler/ast/" + name + ".java", inputs)
    destination = template(manifest(FRONTEND / "module-wiring", "resources", "m3-ast-wiring-receiver-20261017"),
                           "m3/ports/compiletime-openrewrite/src/main/java/com/m3/compiletime/ast/" + name + ".java", inputs)
    additions.append({
        "id": "m3.ast-sharing.20261017." + name,
        "source": "com.synexia.mindex.compiler.ast." + name,
        "target": "com.m3.compiletime.ast." + name,
        "status": "optional source-sharing adapter; exact qualification scope is in the handoff",
        "source_repository": "hsoliwal/com.synexia",
        "source_base_revision": SOURCE_BASE,
        "source_commit": None,
        "source_commit_receipt": HANDOFF,
        "source_path": source["path"],
        "source_template": source["template"],
        "source_template_sha256": source["template_sha256"],
        "target_path": destination["path"],
        "target_preimage_sha256": destination["preimage_sha256"],
        "target_template": destination["template"],
        "target_template_sha256": destination["template_sha256"],
        "source_owner": "synexia-mindex/compiler-openrewrite",
        "target_owner": "m3/ports/compiletime-openrewrite",
        "recipe": "com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe",
        "source_recipe_crate": "m3-openrewrite-shared-ast-20261017",
        "target_recipe_crate": "m3-ast-wiring-receiver-20261017",
        "target_rule": "Optional OpenRewrite projection from already captured source; retain the qualified adapter's scope and isolate its dependencies in the dedicated optional module.",
        "proof": VENDOR + FRONTEND_REL + "/openrewrite/evidence/CLASSLOAD_RECEIPT.json",
        "handoff": HANDOFF,
        "license": "Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors",
    })
    hierarchy = FRONTEND / "hierarchy"
    hierarchy_source = manifest(hierarchy, "src/main/resources", "m3-ast-hierarchy-canonical-20261017")
    hierarchy_target = manifest(hierarchy, "src/main/resources", "m3-ast-hierarchy-receiver-20261017")
    for name, rule in (
            ("MIndexASTHierarchy", "Actual lazy L0 file facts and atoms, L1 file summary, L2 package, L3 module, L4 project, and L5 repository provider evaluation; full source/compiler/provider identities, bounded dependency closure and precise invalidation reuse the existing partial-AST and leased workspace cache owners."),
            ("MIndexASTHierarchyFacts", "Immutable level/context/provider-qualified aggregate vectors with bounded arity, defensive value arrays, and checksummed exact UTF16 codec through the existing swap frame.")):
        target_name = name.replace("MIndex", "M3", 1)
        source = template(hierarchy_source, "synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/ast/" + name + ".java", inputs)
        destination = template(hierarchy_target, "m3/ports/compiletime/src/main/java/com/m3/compiletime/ast/" + target_name + ".java", inputs)
        entry = {
            "id": "m3.ast-sharing.20261017." + target_name,
            "source": "com.synexia.mindex.compiler.ast." + name,
            "target": "com.m3.compiletime.ast." + target_name,
            "status": "finite lazy hierarchy and codec contracts qualified in canonical and receiver packages; global receiving gates unchanged",
            "source_repository": "hsoliwal/com.synexia", "source_base_revision": SOURCE_BASE,
            "source_commit": None, "source_commit_receipt": HANDOFF,
            "source_path": source["path"], "source_template": source["template"],
            "source_template_sha256": source["template_sha256"],
            "target_path": destination["path"], "target_preimage_sha256": destination["preimage_sha256"],
            "target_template": destination["template"], "target_template_sha256": destination["template_sha256"],
            "source_owner": "synexia-mindex/compiler", "target_owner": "m3/ports/compiletime",
            "recipe": "com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe",
            "source_recipe_crate": "m3-ast-hierarchy-canonical-20261017",
            "target_recipe_crate": "m3-ast-hierarchy-receiver-20261017",
            "target_rule": rule, "leaf_first_hierarchy": LEVELS,
            "proof": HIERARCHY_PROOF, "handoff": HANDOFF,
            "license": "Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors",
        }
        if name == "MIndexASTHierarchy":
            entry["supporting_existing_owner_atoms"] = []
            for path, correction in (
                    ("synexia-mindex/runtime/src/main/java/com/synexia/mindex/ast/MIndexAST.java", "Bounded immutable coordinate-prefix snapshot preserves original atom coordinates and excludes later unrelated pool appends without reimporting rows."),
                    ("synexia-mindex/runtime/src/main/java/com/synexia/mindex/ast/partial/MIndexPartialAstCache.java", "Overflow-safe percentages and trim-before-add weight accounting preserve the existing segmented cache policy.")):
                original = template(hierarchy_source, path, inputs)
                mirror = template(hierarchy_target, VENDOR + path, inputs)
                entry["supporting_existing_owner_atoms"].append({"canonical": original, "exact_vendor_counterpart": mirror,
                                                               "behavior": correction, "proof": HIERARCHY_PROOF})
            entry["related_upstream_owner"] = {
                "source": "com.synexia.mindex.ast.MIndexASTLeafIndex",
                "source_revision": "892e3f6c51d78bc0948e8a8cf736055b4c70eafa",
                "source_blob": "fbce9f518890cd144bd4576e97031f017563e07f",
                "relationship": "Existing detached L0-L5 primitive atom/state evidence and selection view; the lazy hierarchy adds compiler-context, provider, dependency and cache resolution contracts.",
                "read_only_review": VENDOR + HIERARCHY_REL + "/evidence/upstream-892e/REVIEW.md",
            }
        additions.append(entry)
    require(len(additions) == 12, "unexpected registry scope")
    require(not any(row.get("id") in {a["id"] for a in additions} for row in before["mappings"]), "mapping already exists in baseline")
    after["mappings"].extend(additions)
    records = [r for r in after["migration"]["records"] if r["id"] == PENDING]
    require(len(records) == 1, "expected one historical compiler counterpart")
    records[0]["reason"] = "Full MIndexCompilerASTPrecompute counterpart remains pending: source-family dependency closure and runtime effective implementation selection are unimplemented. A separately qualified exact-source observer uses the reserved com.sun.tools.javac.m3.M3ASTPC owner without implementing the historical full-family contract."
    records[0]["observation"] = "A task-scoped JavaCompiler exact-source observer now exists at com.sun.tools.javac.m3.M3ASTPC, including annotation-generated parser inputs. Limited observer proof: " + PROOF + "; final source/target commit handoff: " + HANDOFF + ". The full counterpart and its existing dependency/runtime gates remain pending; see m3/docs/counterpart-naming.md."
    output = encoded(after)
    fixture.parent.mkdir(parents=True, exist_ok=True)
    fixture.write_bytes(baseline)
    schema_fixture.parent.mkdir(parents=True, exist_ok=True)
    schema_fixture.write_bytes(schema)
    routes = []
    for crate, expected in ((CRATE, BASE_SHA), (SUCCESSOR_CRATE, INTERMEDIATE_SHA)):
        resource = RESOURCE.parent / crate
        resource.mkdir(parents=True, exist_ok=True)
        (resource / "000-name-mapping.json.txt").write_bytes(output)
        (resource / "manifest.tsv").write_text(MAP_PATH + "\t" + expected + "\t" + sha(output) + "\t000-name-mapping.json.txt\n")
        routes.append({"crate": crate, "before_sha256": expected, "after_sha256": sha(output),
                       "manifest_sha256": sha((resource / "manifest.tsv").read_bytes())})
    lineage = {"schema": 1, "recipe": "com.synexia.rewrite.M3HashPinnedTextSnapshotRecipe", "crate": CRATE,
               "canonical_base": SOURCE_BASE, "target_base": TARGET_BASE, "target_path": MAP_PATH,
               "before_sha256": BASE_SHA, "after_sha256": sha(output), "schema_sha256": SCHEMA_SHA,
               "intermediate_sha256": INTERMEDIATE_SHA, "routes": routes, "recipe_owners": OWNERS,
               "source_template_inputs": inputs, "new_mapping_ids": [e["id"] for e in additions],
               "new_mappings_sha256": sha(encoded(additions)), "final_commit_handoff": HANDOFF,
               "changed_historical_record": PENDING, "allowed_historical_fields": ["reason", "observation"]}
    (HERE / "SOURCE_LINEAGE.json").write_bytes(encoded(lineage))
    print(json.dumps({"prepared": MAP_PATH, "new_mappings": len(additions), "before": BASE_SHA, "intermediate": INTERMEDIATE_SHA, "after": sha(output)}))


def scope_guard(before: dict, after: dict, lineage: dict) -> None:
    old_count = len(before["mappings"])
    require(after["mappings"][:old_count] == before["mappings"], "existing mappings changed")
    additions = after["mappings"][old_count:]
    require([e["id"] for e in additions] == lineage["new_mapping_ids"], "added mapping identities differ")
    require(sha(encoded(additions)) == lineage["new_mappings_sha256"], "added mapping/source hash drift")
    require(all(not e["target_path"].startswith("src/java.base/") for e in additions), "new java.base owner")
    facts = next(e for e in additions if e["id"].endswith(".M3FileSourceFacts"))
    require(facts["leaf_first_hierarchy"] == LEVELS, "leaf-first hierarchy differs")
    for name in (".M3ASTHierarchy", ".M3ASTHierarchyFacts"):
        entry = next(e for e in additions if e["id"].endswith(name))
        require(entry["leaf_first_hierarchy"] == LEVELS and entry["proof"] == HIERARCHY_PROOF,
                "actual hierarchy mapping/proof differs")
    require(after["porting_invariant"]["receiving_sequence"] == before["porting_invariant"]["receiving_sequence"], "global receiving sequence changed")
    require(after["migration"]["gates"] == before["migration"]["gates"], "global migration gates changed")
    normalized = copy.deepcopy(after)
    normalized["mappings"] = before["mappings"]
    old = next(e for e in before["migration"]["records"] if e["id"] == PENDING)
    current = next(e for e in normalized["migration"]["records"] if e["id"] == PENDING)
    require(current["kind"] == "pending" and current["status"] == "pending", "full historical counterpart was promoted")
    require("exact-source observer" in current["reason"] and HANDOFF in current["observation"], "limited seam proof is absent")
    for field in ("reason", "observation"):
        current[field] = old[field]
    require(normalized == before, "change outside the twelve additions and two observation fields")


def verify(mapping: Path, output: Path) -> None:
    import importlib.metadata
    import jsonschema

    lineage = json.loads((HERE / "SOURCE_LINEAGE.json").read_bytes())
    before_bytes = (HERE / "preimages" / MAP_PATH).read_bytes()
    intermediate_bytes = (HERE / "preimages/hierarchy" / MAP_PATH).read_bytes()
    schema_bytes = (HERE / "guards" / SCHEMA_PATH).read_bytes()
    actual = mapping.read_bytes()
    require(sha(before_bytes) == BASE_SHA and sha(schema_bytes) == SCHEMA_SHA, "proof guard drift")
    require(sha(intermediate_bytes) == INTERMEDIATE_SHA, "intermediate proof guard drift")
    require(sha(actual) == lineage["after_sha256"], "candidate differs from sealed postimage")
    schema = json.loads(schema_bytes)
    jsonschema.Draft202012Validator.check_schema(schema)
    validator = jsonschema.Draft202012Validator(schema)
    before, after = json.loads(before_bytes), json.loads(actual)
    validator.validate(before)
    validator.validate(json.loads(intermediate_bytes))
    validator.validate(after)
    scope_guard(before, after, lineage)
    for path, expected in lineage["source_template_inputs"].items():
        require(sha((SYNEXIA / path).read_bytes()) == expected, "source recipe/template drift: " + path)
    for name, expected in OWNERS.items():
        require(sha((SYNEXIA / "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite" / name).read_bytes()) == expected,
                "shared exact recipe owner drift: " + name)
    for route in lineage["routes"]:
        resource = RESOURCE.parent / route["crate"]
        require(sha((resource / "manifest.tsv").read_bytes()) == route["manifest_sha256"], "route manifest drift")
        require((resource / "000-name-mapping.json.txt").read_bytes() == actual, "route postimages differ")
    refused = []
    for mutation in ("receiving-phase", "migration-gate", "java-base-reservation", "full-counterpart-promotion", "package-level", "target-source-hash"):
        changed = copy.deepcopy(after)
        if mutation == "receiving-phase":
            changed["porting_invariant"]["receiving_sequence"]["active_phase"] = "AST_COMPILER"
        elif mutation == "migration-gate":
            changed["migration"]["gates"][0]["reason"] += " drift"
        elif mutation == "java-base-reservation":
            record = next(r for r in changed["migration"]["records"] if any((a.get("path") or "").startswith("src/java.base/") for a in r["targets"]))
            record["owner"] += ".drift"
        elif mutation == "full-counterpart-promotion":
            next(r for r in changed["migration"]["records"] if r["id"] == PENDING)["status"] = "implemented-tested"
        elif mutation == "package-level":
            next(r for r in changed["mappings"] if r.get("id", "").endswith(".M3FileSourceFacts"))["leaf_first_hierarchy"]["L2"] = "project"
        else:
            changed["mappings"][-1]["target_template_sha256"] = "f" * 64
        try:
            scope_guard(before, changed, lineage)
        except ValueError:
            refused.append(mutation)
        else:
            raise ValueError("scope mutation was admitted: " + mutation)
    receipt = {"status": "passed", "schema": "Draft2020-12", "jsonschema_version": importlib.metadata.version("jsonschema"),
               "candidate_sha256": sha(actual), "baseline_sha256": BASE_SHA, "schema_sha256": SCHEMA_SHA,
               "new_mappings": len(lineage["new_mapping_ids"]), "exact_preimage_routes": lineage["routes"],
               "unchanged_receiving_sequence": True, "unchanged_global_gates": True,
               "unchanged_java_base_reservations": True, "full_counterpart_status": "pending",
               "hierarchy": LEVELS, "scope_mutations_refused": refused, "source_template_hashes_verified": len(lineage["source_template_inputs"]),
               "final_commit_handoff": HANDOFF}
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(encoded(receipt))
    print(json.dumps(receipt))


def main() -> None:
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)
    prepare_parser = commands.add_parser("prepare")
    prepare_parser.add_argument("--target", type=Path, required=True)
    verify_parser = commands.add_parser("verify")
    verify_parser.add_argument("--mapping", type=Path, required=True)
    verify_parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "prepare":
        prepare(args.target)
    else:
        verify(args.mapping, args.output)


if __name__ == "__main__":
    main()
