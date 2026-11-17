#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed M3JDK21 receiver preflight; does NOT certify a JDK runtime.

The canonical source recipes remain in hsoliwal/com.synexia. The target JDK
owns its Java/HotSpot/JNI implementations. This script only verifies that the
receiver names existing target owners and preserves exact source provenance,
the current String-first phase, and all still-pending acceptance obligations.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[3]
RECEIPT = "m3/synexia-import/qualification/qualified-composites-20261117.json"
HEX40 = re.compile(r"[0-9a-f]{40}\Z")

# Exact merged Synexia recipe seals, not a second recipe implementation.
RECIPES = {
    "qualified-scope": {
        "pull_request": 18252,
        "head_commit": "e4be444b6a4aeb6e12728bfc5e5992c0ecfa21da",
        "merge_commit": "8f8a26e14709c1053a909023a5905b6b3ee1ab1d",
        "source_manifest_git_blob": "e3029aab2490b59451bcbad8c135f05ee4215792",
        "source_postimages": {"MIndexQualifiedScopeRegistry.java": "117d12c3c1b50f39935e0054fd7947ed9c29c36767af1628c45a4e55cb6f9174"},
    },
    "qualified-bridges": {
        "pull_request": 18269,
        "head_commit": "a0839334816735ea7f5bf3b77ef67fb1c468a312",
        "merge_commit": "751f371d1d0a94580dabd463a7f0556c85f8e546",
        "source_manifest_git_blob": "6d80fa0b67acc1493c04f5fde2121fa963136515",
        "source_postimages": {
            "MIndexCanonicalStructureBridge.java": "6dac081c324db73885762fb8e1d098e1c03e7ee69ba5ebb54f134485157ebd5e",
            "MIndexJvmDescriptorInterner.java": "f98ce4b005758f5790cd6d9c344159a2cb23af1d977e40565d14bd7f59b887cd",
        },
    },
}

# A target's public JDK owner retains its ABI. These are preimage source pins,
# not an assertion that the associated pending feature has been implemented.
OWNER_PATHS = {
    "java.lang.String": "src/java.base/share/classes/java/lang/String.java",
    "java.lang.M3StringOwner": "src/java.base/share/classes/java/lang/M3StringOwner.java",
    "jdk.internal.mindex.M3CI": "src/java.base/share/classes/jdk/internal/mindex/M3CI.java",
    "jdk.internal.mindex.M3Class": "src/java.base/share/classes/jdk/internal/mindex/M3Class.java",
    "jdk.internal.mindex.M3Method": "src/java.base/share/classes/jdk/internal/mindex/M3Method.java",
    "jdk.internal.mindex.M3Descriptor": "src/java.base/share/classes/jdk/internal/mindex/M3Descriptor.java",
    "java.util.regex.Pattern": "src/java.base/share/classes/java/util/regex/Pattern.java",
}

# Full planned family inventory, with admission ordering. This is a receiver
# coverage assertion, not a claim that a parallel public M3 API exists.
FAMILY_STAGES = {
    "M3String": "STRING",
    "M3Array": "ARRAYS",
    "M3Primitive": "ARRAYS",
    "M3Path": "REMAINING",
    "M3URI_URL": "REMAINING",
    "M3List_Tree": "COLLECTIONS",
    "M3Map_Set": "COLLECTIONS",
    "M3AST": "AST_COMPILER",
    "M3Metadata": "REMAINING",
    "M3Class": "REMAINING",
    "M3Method": "REMAINING",
    "M3Module": "REMAINING",
    "M3Object": "REMAINING",
    "M3Runtime": "REMAINING",
    "M3File": "REMAINING",
    "M3SQL": "REMAINING",
    "M3Regex": "REMAINING",
    "M3Graph": "REMAINING",
    "M3QualifiedLexicon": "REMAINING",
    "M3Const": "REMAINING",
}

SCOPE_FIELDS = frozenset({
    "domain", "program", "module", "library_coordinate", "library_revision",
    "defining_loader", "filesystem_provider", "schema_version",
    "source_identity", "source_sha256", "source_generation",
})


class ReceiverRefusal(ValueError):
    """An unqualified product receiving claim or changed source preimage."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ReceiverRefusal(message)


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def read_regular(path: Path, message: str, root: Path | None = None) -> bytes:
    require(path.is_file() and not path.is_symlink(), message)
    if root is not None:
        require(path.resolve().is_relative_to(root), message)
    return path.read_bytes()


def verify(root: Path) -> dict[str, int | str]:
    """Check the current receipt against its *unchanged* target preimages."""
    root = root.resolve()
    raw = read_regular(root / RECEIPT, "RECEIVER_RECEIPT_MISSING", root)
    try:
        data = json.loads(raw)
    except (ValueError, UnicodeError) as exc:
        raise ReceiverRefusal("RECEIVER_RECEIPT_INVALID") from exc
    require(isinstance(data, dict), "RECEIVER_RECEIPT_INVALID")
    require(data.get("schema") == "M3JDK21_QUALIFIED_COMPOSITES_RECEIVER_V1", "RECEIVER_SCHEMA_DRIFT")
    require(data.get("product_repository") == "hsoliwal/M3jdk21", "RECEIVER_PRODUCT_DRIFT")
    require(data.get("canonical_recipe_repository") == "hsoliwal/com.synexia", "RECEIVER_RECIPE_OWNER_DRIFT")
    require(data.get("license") == "Apache-2.0", "RECEIVER_LICENSE_DRIFT")
    require(isinstance(data.get("product_baseline_commit"), str)
            and HEX40.fullmatch(data["product_baseline_commit"]) is not None,
            "RECEIVER_BASELINE_INVALID")

    qualification = data.get("qualification")
    require(isinstance(qualification, dict), "QUALIFICATION_MISSING")
    require(qualification.get("active_stage") == "STRING"
            and qualification.get("phase_advance") is False
            and qualification.get("target_runtime_source_written_by_this_receipt") is False
            and qualification.get("target_admission") is False
            and qualification.get("status") == "SOURCE_PINNED_RECEIVER_PLAN_ONLY",
            "QUALIFICATION_PREMATURE")
    for gate in ("source_recipe_replay", "target_java21_build", "target_jtreg",
                 "target_jni_gc_hotspot", "installed_image_abi",
                 "comparative_cpu_heap_native_memory"):
        require(qualification.get(gate) == "NOT_RUN", "QUALIFICATION_GATE_UNPROVEN:" + gate)
    require(qualification.get("license_review") == "PENDING", "QUALIFICATION_LICENSE_UNPROVEN")

    canonical = data.get("canonicalization")
    require(isinstance(canonical, dict), "CANONICALIZATION_MISSING")
    require(canonical.get("immutable_leaf_qualification") == "GLOBAL_CONTENT_ONLY_WITH_EXACT_EQUALITY",
            "CANONICAL_LEAF_SCOPE_DRIFT")
    require(canonical.get("semantic_namespace") == "VM_LOCAL_POSITIVE_LONG_UID"
            and canonical.get("qualified_occurrence") == "OWNER_GENERATION_OCCURRENCE"
            and canonical.get("source_sha256") == "PROVENANCE_CHECK_NOT_NAMESPACE_ID"
            and canonical.get("class_runtime_identity") == "BINARY_NAME_AND_DEFINING_CLASS_LOADER",
            "SEMANTIC_IDENTITY_DRIFT")
    require(canonical.get("const_alias_and_jni_array_lowering") == "PROPOSAL_ONLY_NOT_JAVA21_ADMITTED",
            "CONST_SYNTAX_UNVERIFIED")
    require(isinstance(canonical.get("source_scope_fields"), list)
            and len(canonical["source_scope_fields"]) == len(SCOPE_FIELDS)
            and set(canonical["source_scope_fields"]) == SCOPE_FIELDS,
            "SOURCE_QUALIFIER_DRIFT")

    source_recipes = data.get("source_recipes")
    require(isinstance(source_recipes, list) and len(source_recipes) == len(RECIPES),
            "SOURCE_RECIPE_COVERAGE")
    require(len({r.get("id") for r in source_recipes if isinstance(r, dict)}) == len(RECIPES),
            "SOURCE_RECIPE_DUPLICATE")
    for recipe in source_recipes:
        require(isinstance(recipe, dict), "SOURCE_RECIPE_INVALID")
        expected = RECIPES.get(recipe.get("id"))
        require(expected is not None, "SOURCE_RECIPE_UNKNOWN")
        for field in ("pull_request", "head_commit", "merge_commit", "source_manifest_git_blob"):
            require(recipe.get(field) == expected[field], "SOURCE_SEAL_DRIFT:" + field)
        require(recipe.get("merged") is True and recipe.get("product_admitted") is False,
                "SOURCE_PROMOTION_UNSUPPORTED")
        require(recipe.get("maven_openrewrite") == "NOT_RUN"
                and recipe.get("foundation_build") == "NOT_RUN"
                and recipe.get("installed_jdk") == "NOT_RUN", "SOURCE_GATES_UNPROVEN")
        observed_postimages = recipe.get("source_postimages")
        require(isinstance(observed_postimages, list), "SOURCE_POSTIMAGES_INVALID")
        try:
            mapping = {p["name"]: p["sha256"] for p in observed_postimages}
        except (KeyError, TypeError) as exc:
            raise ReceiverRefusal("SOURCE_POSTIMAGES_INVALID") from exc
        require(len(mapping) == len(observed_postimages)
                and mapping == expected["source_postimages"], "SOURCE_SEAL_DRIFT:postimages")

    targets = data.get("target_owner_preimages")
    require(isinstance(targets, list) and len(targets) == len(OWNER_PATHS),
            "TARGET_OWNER_COVERAGE")
    symbols = set()
    for owner in targets:
        require(isinstance(owner, dict), "TARGET_OWNER_INVALID")
        symbol = owner.get("symbol")
        require(isinstance(symbol, str) and symbol in OWNER_PATHS
                and symbol not in symbols, "TARGET_OWNER_UNKNOWN_OR_DUPLICATE")
        symbols.add(symbol)
        path = owner.get("path")
        require(path == OWNER_PATHS[symbol], "TARGET_OWNER_PATH_DRIFT")
        preimage = owner.get("git_blob")
        require(isinstance(preimage, str) and HEX40.fullmatch(preimage) is not None,
                "TARGET_OWNER_BLOB_INVALID")
        actual = read_regular(root / path, "TARGET_SOURCE_MISSING_OR_SYMLINK:" + symbol, root)
        require(git_blob(actual) == preimage, "TARGET_SOURCE_GIT_BLOB_DRIFT:" + symbol)
    require(symbols == set(OWNER_PATHS), "TARGET_OWNER_COVERAGE")

    families = data.get("families")
    require(isinstance(families, list) and len(families) == len(FAMILY_STAGES),
            "REQUIRED_FAMILY_MISSING")
    names = set()
    for family in families:
        require(isinstance(family, dict), "FAMILY_ROW_INVALID")
        name = family.get("name")
        require(isinstance(name, str) and name in FAMILY_STAGES
                and name not in names, "FAMILY_MISSING_OR_DUPLICATE")
        names.add(name)
        require(family.get("stage") == FAMILY_STAGES[name], "FAMILY_STAGE_DRIFT:" + name)
        require(family.get("state") == "PENDING_TARGET_ADMISSION"
                and isinstance(family.get("jdk_owner"), str)
                and bool(family["jdk_owner"].strip()), "FAMILY_PREMATURE_PROMOTION:" + name)
    require(names == set(FAMILY_STAGES), "REQUIRED_FAMILY_MISSING")
    return {"receipt": "SOURCE_PINNED_NOT_RUNTIME_ADMITTED",
            "owners_verified": len(symbols), "families_pending": len(names),
            "merged_recipe_seals": len(source_recipes)}


if __name__ == "__main__":
    try:
        print(json.dumps(verify(ROOT), sort_keys=True))
    except (ReceiverRefusal, OSError) as failure:
        print("M3_QUALIFIED_RECEIVER_REFUSED: " + str(failure), file=sys.stderr)
        sys.exit(4)
