#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Validate the source-bound Phase-1 Synexia MIndexString -> M3JDK M3String mapping."""

from __future__ import annotations

import csv
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MAP = Path(__file__).with_name("string-source-target-map.tsv")
ESTATE = Path(__file__).with_name("synexia-estate.tsv")
NAME_MAP = ROOT / "m3/docs/name-mapping.json"
WORK = Path(__file__).with_name("string-phase-work-orders.tsv")
HEX40 = re.compile(r"^[0-9a-f]{40}$")
SUCCESSORS = Path(__file__).with_name("string-target-successors.tsv")
SUCCESSOR_HEADER = [
    "schema",
    "target_path",
    "historical_git_blob",
    "successor_git_blob",
    "synexia_prs",
    "m3jdk_prs",
    "semantic_evidence",
    "state",
]
EXPECTED_SUCCESSOR_PATHS = {
    "src/java.base/share/classes/java/lang/M3String.java",
    "src/java.base/share/classes/java/lang/M3StringFacts.java",
    "src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java",
    "src/java.base/share/classes/java/lang/M3StringPool.java",
}
# Additive, candidate-only source chronology; historical four-row successor receipt remains immutable.
SUCCESSOR_CHAIN = Path(__file__).with_name("string-target-successor-chain.tsv")
SUCCESSOR_CHAIN_HEADER = [
    "schema", "target_path", "predecessor_git_blob", "successor_git_blob",
    "synexia_pr", "m3jdk_pr", "state",
]
# Separate exact source observation of a two-parent integration merge.
# This is not a serial recipe reconstruction or runtime/performance acceptance.
MERGED_SOURCE_OBSERVATION = Path(__file__).with_name("string-target-merged-source-observation.tsv")
MERGED_SOURCE_OBSERVATION_HEADER = ["schema","role","target_path","source_commit","git_blob","first_parent_commit","second_parent_commit","synexia_pr","synexia_source_head","canonical_resource_path","canonical_resource_git_blob","canonical_resource_sha256","state","runtime_acceptance"]
EXPECTED_MERGED_SOURCE_OBSERVATION = (
    ("M3JDK21_STRING_MERGED_SOURCE_OBSERVATION_V1", "V1_TIP", "src/java.base/share/classes/java/lang/M3String.java", "104978e5118dfb49f7514eb1b0b01631e09a5aa5", "35ed998540e044eed8cb7596e2e2c019ae369bd3", "NOT_APPLICABLE", "NOT_APPLICABLE", "11448", "4f9b2646d74ab39031a54d1f56d6777c52a21852", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-utf8-cursor-20261014/M3String.java.txt", "35ed998540e044eed8cb7596e2e2c019ae369bd3", "cd1192e27c981da03aeae2385d3d54ad31d912f043cf8c7dfba18c7b9106b728", "HISTORICAL_V1_TIP_SOURCE_ONLY", "NOT_CLAIMED"),
    ("M3JDK21_STRING_MERGED_SOURCE_OBSERVATION_V1", "FIRST_PARENT", "src/java.base/share/classes/java/lang/M3String.java", "51f3fa1c0985ecff613ceac5c190ce685c04951f", "1c9574a31bc50013d6d0827f740b4d6fa32d636e", "NOT_APPLICABLE", "NOT_APPLICABLE", "NOT_ESTABLISHED", "NOT_ESTABLISHED", "NOT_ESTABLISHED", "NOT_ESTABLISHED", "NOT_ESTABLISHED", "OBSERVED_PARENT_SOURCE_ONLY", "NOT_CLAIMED"),
    ("M3JDK21_STRING_MERGED_SOURCE_OBSERVATION_V1", "SECOND_PARENT", "src/java.base/share/classes/java/lang/M3String.java", "4b67f33bdc9c874d602a5ac38b9ec7f9948c9426", "1aea17959495c7fee359d7c13ea4b876f4fb8b31", "NOT_APPLICABLE", "NOT_APPLICABLE", "12774", "415da87f2e14d4a125496aa8e43347a1429e3a6f", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3regex-adaptive-transfer-20261021/00-M3String.java.txt", "1aea17959495c7fee359d7c13ea4b876f4fb8b31", "1b7ae3bfa1132d8040780c2ba0bd4f55281f51a6c3cc374df4cb45f175d69e78", "OBSERVED_RECIPE_POSTIMAGE_SOURCE_ONLY", "NOT_CLAIMED"),
    ("M3JDK21_STRING_MERGED_SOURCE_OBSERVATION_V1", "MERGED_RESULT", "src/java.base/share/classes/java/lang/M3String.java", "5eeb841168ed420085223806fefcfca8d1b6b3ca", "8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e", "51f3fa1c0985ecff613ceac5c190ce685c04951f", "4b67f33bdc9c874d602a5ac38b9ec7f9948c9426", "12774", "415da87f2e14d4a125496aa8e43347a1429e3a6f", "NOT_RECONSTRUCTED", "NOT_RECONSTRUCTED", "NOT_RECONSTRUCTED", "OBSERVED_MERGED_SOURCE_NOT_RECIPE_RECONSTRUCTED", "NOT_CLAIMED"),
)

# Exact ordered canonical Facts postimages; historical successor receipts remain immutable.
# Source custody only: no runtime, whole-image, JNI, JIT/GC or performance admission.
M3_STRING_FACTS_PATH = "src/java.base/share/classes/java/lang/M3StringFacts.java"
FACTS_SUCCESSOR_CHAIN = Path(__file__).with_name("string-facts-target-successor-chain.tsv")
FACTS_SUCCESSOR_CHAIN_HEADER = ["schema","step","target_path","predecessor_git_blob","successor_git_blob","predecessor_sha256","successor_sha256","product_commit","m3jdk_pr","synexia_pr","synexia_source_head","canonical_resource_path","canonical_resource_git_blob","canonical_resource_sha256","state","runtime_acceptance"]
EXPECTED_FACTS_SUCCESSOR_CHAIN = (
    ("M3JDK21_STRING_FACTS_SUCCESSOR_SOURCE_V1", "1", "src/java.base/share/classes/java/lang/M3StringFacts.java", "fcd108ab6181fec11d16eda811b29d18298f3fb1", "d8dd7dc30db6a9e3897146572bd4a7755c954ffb", "f1cc0ec9d4fd0bbc15d9c684e80b9907b60321cb15ad4fd7fa39f3cf973fc4cd", "2152c154d3cdece330667ee09675fd21d86f3b483a23a3156b8a3626c3f9f420", "56d29d32b128de0d998e933f957e090383e8d2dc", "873", "11288", "0b10db35ec0cf1af442077bcd6200cf7f5896eb3", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-modified-utf8-dp-20261014/00-M3StringFacts.java.txt", "d8dd7dc30db6a9e3897146572bd4a7755c954ffb", "2152c154d3cdece330667ee09675fd21d86f3b483a23a3156b8a3626c3f9f420", "CANONICAL_RECIPE_POSTIMAGE_SOURCE_ONLY", "NOT_CLAIMED"),
    ("M3JDK21_STRING_FACTS_SUCCESSOR_SOURCE_V1", "2", "src/java.base/share/classes/java/lang/M3StringFacts.java", "d8dd7dc30db6a9e3897146572bd4a7755c954ffb", "3e76b8c52eefc3b1983c701a0b9dfc953262a554", "2152c154d3cdece330667ee09675fd21d86f3b483a23a3156b8a3626c3f9f420", "34663f5b64dbd44b566e655a17170b36f55c19f9e0e630c02ed291f78422e267", "433639fbd919181da0724aa77bafd6a58090afc3", "1205", "12189", "e3398abf35da30f13ba7ed626c5391671c5b3dcd", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-string-utf8-fact-overflow-20261017/M3StringFacts.java.txt", "3e76b8c52eefc3b1983c701a0b9dfc953262a554", "34663f5b64dbd44b566e655a17170b36f55c19f9e0e630c02ed291f78422e267", "CANONICAL_RECIPE_POSTIMAGE_SOURCE_ONLY", "NOT_CLAIMED"),
    ("M3JDK21_STRING_FACTS_SUCCESSOR_SOURCE_V1", "3", "src/java.base/share/classes/java/lang/M3StringFacts.java", "3e76b8c52eefc3b1983c701a0b9dfc953262a554", "14fc79e035ac7c8fe610b0fc9191ba428b683133", "34663f5b64dbd44b566e655a17170b36f55c19f9e0e630c02ed291f78422e267", "e2d6056aecffae091ea8b84d21e2d899a8959d142c9da3ed24915753c413c92a", "7f0cd03024155b11f22c88990192f11e1d1014b7", "1320", "12324", "7ca15c181af290f69d3b48fcf73eda6cdae71c41", "synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/datasets/m3-regex-facts-v2-20261018/resources/com/synexia/rewrite/hash-pinned-java/m3-collections-contract-refinement-20261017-m3-regex-facts-v2-20261018/0001-M3StringFacts.java.txt", "14fc79e035ac7c8fe610b0fc9191ba428b683133", "e2d6056aecffae091ea8b84d21e2d899a8959d142c9da3ed24915753c413c92a", "CANONICAL_RECIPE_POSTIMAGE_SOURCE_ONLY", "NOT_CLAIMED"),
    ("M3JDK21_STRING_FACTS_SUCCESSOR_SOURCE_V1", "4", "src/java.base/share/classes/java/lang/M3StringFacts.java", "14fc79e035ac7c8fe610b0fc9191ba428b683133", "bd4d7878ceb9d3c74f8f012c05cac4aff392a128", "e2d6056aecffae091ea8b84d21e2d899a8959d142c9da3ed24915753c413c92a", "0cc1b4726cb2637b184184a60c75964180d58790530101fcf46900f74f1dc72a", "ddb44fdbb37163c59b4c6be04dd3affe2dbac1bc", "1829", "12871", "bcecde65aa6ba35fc3c3a908889f3b8af04aba70", "synexia-openrewrite-recipes/recipe-crates/m3-regex-language-mastery-20261021/recipe-host/datasets/combined-c831/resources/com/synexia/rewrite/hash-pinned-java/m3-regex-language-mastery-20261021-combined-c831/0001-M3StringFacts.java.txt", "bd4d7878ceb9d3c74f8f012c05cac4aff392a128", "0cc1b4726cb2637b184184a60c75964180d58790530101fcf46900f74f1dc72a", "CANONICAL_RECIPE_POSTIMAGE_SOURCE_ONLY", "NOT_CLAIMED"),
)

# Frozen Pool source-DAG observations are not an ordered semantic recipe chain.
# Historical514 -> common-basee5ab remains unresolved; no runtime acceptance follows.
M3_STRING_POOL_PATH = "src/java.base/share/classes/java/lang/M3StringPool.java"
POOL_SOURCE_OBSERVATION = Path(__file__).with_name("string-pool-source-observation.tsv")
POOL_SOURCE_OBSERVATION_HEADER = ["schema","role","target_path","source_commit","git_blob","sha256","utf8_bytes","artifact_kind","artifact_repository","artifact_ref","artifact_path","artifact_git_blob","producer_class","producer_git_blob","canonical_history","history_gap","runtime_acceptance","state"]
EXPECTED_POOL_SOURCE_OBSERVATION = (
    ("M3JDK21_STRING_POOL_FROZEN_DAG_OBSERVATION_V1", "HISTORICAL_TIP", "src/java.base/share/classes/java/lang/M3StringPool.java", "2442a6b242953f4ec8bd63d52ac48708d0383782", "514247a2e6c3704e03a9b47682f74e481f66585f", "f8bac45d1623d25470e1b79beffa276f271db10e297ef5b434d5335c9d4c908b", "58344", "IMMUTABLE_HISTORICAL_RECEIPT", "hsoliwal/M3jdk21", "e854e8596464557917ddd8809637d815420573b1", "m3/synexia-import/current-full-borrow/string-target-successors.tsv", "141806b3119342dfba2efe6487664ba3a0c53158", "com.synexia.rewrite.M3Jdk21StringConcatOverflowCurrentReceive", "NOT_ESTABLISHED", "PARTIAL", "HISTORICAL_514_TO_COMMON_BASE_E5AB_UNRESOLVED", "NOT_ACCEPTED", "SOURCE_OBSERVATION_ONLY"),
    ("M3JDK21_STRING_POOL_FROZEN_DAG_OBSERVATION_V1", "COMMON_BASE", "src/java.base/share/classes/java/lang/M3StringPool.java", "6ac3d5834844d444979ab1f7b8c85e68982759b7", "e5ab6c95526d9170fc88d9e3ac7c5040606a3367", "09916fd6c69828d5703113ed79e684245a4684a801b573ff2e4f7b973fa75185", "75963", "CANONICAL_MANIFEST_PREIMAGE_AND_OBSERVED_SOURCE", "hsoliwal/com.synexia", "42b5098718732964f711bd34908990cdba58b9ac", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-string-leafpath-jni-shadow-20261021/manifest.tsv", "637763bfcacc797f3ca370b572ed05c0ed1c43a2", "com.synexia.m3.Jdk21StringLeafPathJni20261021", "NOT_ESTABLISHED", "PARTIAL", "HISTORICAL_514_TO_COMMON_BASE_E5AB_UNRESOLVED", "NOT_ACCEPTED", "SOURCE_OBSERVATION_ONLY"),
    ("M3JDK21_STRING_POOL_FROZEN_DAG_OBSERVATION_V1", "CANONICAL_LEAF_POSTIMAGE", "src/java.base/share/classes/java/lang/M3StringPool.java", "52d147a42877434a66f785866135f8b2184bd38c", "1e4b2deee939fdb2840ea3f059a763693f97d545", "8d328737e6f7c6f727f5c25b164bac3cdacad12a99f35a2531c907534b0ff5e0", "77501", "CANONICAL_RESOURCE_POSTIMAGE", "hsoliwal/com.synexia", "42b5098718732964f711bd34908990cdba58b9ac", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-string-leafpath-jni-shadow-20261021/M3StringPool.java.txt", "1e4b2deee939fdb2840ea3f059a763693f97d545", "com.synexia.m3.Jdk21StringLeafPathJni20261021", "NOT_ESTABLISHED", "PARTIAL", "HISTORICAL_514_TO_COMMON_BASE_E5AB_UNRESOLVED", "NOT_ACCEPTED", "SOURCE_OBSERVATION_ONLY"),
    ("M3JDK21_STRING_POOL_FROZEN_DAG_OBSERVATION_V1", "CANONICAL_NATIVE_FORK", "src/java.base/share/classes/java/lang/M3StringPool.java", "33668cff903b7c417932f1b573d2f2974baeeca2", "7cbccf212f7271f299a0b794f4d38d8eff3e2dd8", "aa6470c1ea5f32f840cb1cd91f64c79c14b08b18e0e2a10553f1c59456f120b7", "76888", "CANONICAL_EXACT_EDIT_RULE_AND_OBSERVED_SOURCE", "hsoliwal/com.synexia", "42b5098718732964f711bd34908990cdba58b9ac", "synexia-openrewrite-recipes/recipe-crates/m3-os-interned-jni-concat-20261020/src/main/resources/m3-os-interned-jni/target_patches.json", "9d6dd4a210a885a4410c86b8dc9d4d2c9be758dd", "com.synexia.rewrite.m3jni.M3OsInternedConcatRecipe", "026c24c5c8e82135a85f0ebbb17940f6183217d8", "PARTIAL", "HISTORICAL_514_TO_COMMON_BASE_E5AB_UNRESOLVED", "NOT_ACCEPTED", "SOURCE_OBSERVATION_ONLY"),
    ("M3JDK21_STRING_POOL_FROZEN_DAG_OBSERVATION_V1", "OBSERVED_COMPONENT_INTEGRATION", "src/java.base/share/classes/java/lang/M3StringPool.java", "78814421aaa2ff8892ce5cbffa1cabf029fd3727", "394af2f062e9d99abf73e5a6f15df015a59b8072", "0b806777f319e48a6519dcac9162d76f37019888c698d8ef519f7a4d2fb7b197", "78426", "EXACT_TARGET_RECIPE_PREIMAGE_AND_SOURCE_COMPOSITION", "hsoliwal/M3jdk21", "e854e8596464557917ddd8809637d815420573b1", "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3StringLeafCursorConvergenceRecipe.java", "a29b26e81cc9d06504b9bd13b58a066307330812", "com.m3.rewrite.backport.M3StringLeafCursorConvergenceRecipe", "a29b26e81cc9d06504b9bd13b58a066307330812", "PARTIAL", "HISTORICAL_514_TO_COMMON_BASE_E5AB_UNRESOLVED", "NOT_ACCEPTED", "SOURCE_OBSERVATION_ONLY"),
    ("M3JDK21_STRING_POOL_FROZEN_DAG_OBSERVATION_V1", "ALTERNATIVE_PREDECESSOR", "src/java.base/share/classes/java/lang/M3StringPool.java", "dc8b9a259ba57d950993e293ca20f33279c089f5", "15231ee5a2c48153550c700f98722e15c0f4451c", "4053ede4b66e066742dcc3c8cf4c4185452f1588d5ce6e6f1593fb3b2ccf9972", "71175", "EXACT_ALTERNATIVE_TARGET_RECIPE_PREDECESSOR", "hsoliwal/M3jdk21", "e854e8596464557917ddd8809637d815420573b1", "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3StringLeafCursorConvergenceRecipe.java", "a29b26e81cc9d06504b9bd13b58a066307330812", "com.m3.rewrite.backport.M3StringLeafCursorConvergenceRecipe", "a29b26e81cc9d06504b9bd13b58a066307330812", "PARTIAL", "HISTORICAL_514_TO_COMMON_BASE_E5AB_UNRESOLVED", "NOT_ACCEPTED", "SOURCE_OBSERVATION_ONLY"),
    ("M3JDK21_STRING_POOL_FROZEN_DAG_OBSERVATION_V1", "TARGET_SPECIFIC_RECIPE_POSTIMAGE", "src/java.base/share/classes/java/lang/M3StringPool.java", "c76ddb4d5cf125f1a42b298bb57f10cc809b7190", "7c05d6ab0082eb26f39e4e6dc93c422237588cae", "1eaeaec2e526d11cad70a37b67f48df2fcdbe538e10e44079d7437bc889fd2dc", "74102", "JDK_TARGET_SPECIFIC_EXACT_RESOURCE_POSTIMAGE", "hsoliwal/M3jdk21", "e854e8596464557917ddd8809637d815420573b1", "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/m3string-leaf-cursor-20261021/M3StringPool.after.java.txt", "7c05d6ab0082eb26f39e4e6dc93c422237588cae", "com.m3.rewrite.backport.M3StringLeafCursorConvergenceRecipe", "a29b26e81cc9d06504b9bd13b58a066307330812", "PARTIAL", "HISTORICAL_514_TO_COMMON_BASE_E5AB_UNRESOLVED", "NOT_ACCEPTED", "SOURCE_OBSERVATION_ONLY"),
    ("M3JDK21_STRING_POOL_FROZEN_DAG_OBSERVATION_V1", "CURRENT_COMMENT_SUCCESSOR", "src/java.base/share/classes/java/lang/M3StringPool.java", "87b43da97583e30191781a9592a23fc92dd8a8fb", "0d47d7e969086262705460c153583f659237c894", "6d5840c1d44aef8bb1433e79b81c1261fe3f0f6dbc5dc3b44a2cfbafe4d2e138", "74100", "OBSERVED_EXACT_ONE_COMMENT_SUCCESSOR", "hsoliwal/M3jdk21", "e854e8596464557917ddd8809637d815420573b1", "src/java.base/share/classes/java/lang/M3StringPool.java", "0d47d7e969086262705460c153583f659237c894", "EXACT_COMMENT_OPERATION_OBSERVED", "NOT_ESTABLISHED", "PARTIAL", "HISTORICAL_514_TO_COMMON_BASE_E5AB_UNRESOLVED", "NOT_ACCEPTED", "SOURCE_OBSERVATION_ONLY"),
)

# Separate six-owner source custody; the historical four-owner successor API is unchanged.
# Atom's final canonical edge remains unreconstructed, and no row accepts runtime behavior.
ADDITIONAL_OWNER_SUCCESSORS = Path(__file__).with_name("string-additional-owner-successors.tsv")
ADDITIONAL_OWNER_HEADER = ["schema","owner","step","target_path","predecessor_git_blob","successor_git_blob","predecessor_sha256","successor_sha256","product_commit","m3jdk_pr","synexia_pr","canonical_source_ref","canonical_recipe","canonical_manifest_path","canonical_manifest_git_blob","canonical_resource_path","canonical_resource_git_blob","canonical_resource_sha256","edge_custody","owner_history","state","runtime_acceptance"]
ADDITIONAL_OWNER_BASES = {
    "src/java.base/share/classes/java/lang/M3StringOwner.java": ("acd2a9584c65c384ddebd9159a8775d196ea0f8c", "329c0ba4d3751a2d76d686237439a2f94831658e"),
    "src/java.base/share/classes/java/lang/M3StringAtom.java": ("7e7645497d08d55cf9b5174e4f04e11c87a06bae", "84a0227edbd5c3f73215289619f95fc18d57eaf1"),
    "src/java.base/share/classes/java/lang/M3StringTuple.java": ("0d4cfb75317e68d54244588a474eba98bf7df27b", "ede9e34a4d78272255f7ea7a53efaa8a54c118d9"),
    "src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java": ("6cbb277119f387aac772373d31621af64bea68d5", "011377436faba14db9fb15bb8f63ecbbf7501dae"),
    "src/java.base/share/classes/jdk/internal/mindex/M3TQ.java": ("533a07b77061e757d476627557413f6df1067d34", "e353a1c656c573d832d50a310ce17a52ae8ee4cd"),
    "src/java.base/share/native/libjava/String.c": ("69a57659e74c6433491932e64f15bc037e887d81", "571032d70eed8579aff225a86aa4cac41836dd4f"),
}
EXPECTED_ADDITIONAL_OWNER_SUCCESSORS = (
    ("M3JDK21_STRING_ADDITIONAL_OWNER_SOURCE_SUCCESSOR_V1", "M3StringOwner", "1", "src/java.base/share/classes/java/lang/M3StringOwner.java", "acd2a9584c65c384ddebd9159a8775d196ea0f8c", "329c0ba4d3751a2d76d686237439a2f94831658e", "cd01663a0b50d842ecc2031c485bd2d050f98b5226f5cdddce63dd0338ec0d74", "ef2a5b69ca7c11154d4a2eede64212fe55f27ddba0f68408ce40750aeb57d20e", "4402dca8d07dc7a98eb70bc6d003a78a1f08803f", "1465", "12475", "78e7252e6d244c03df78441725e55a2bfce7b41a", "com.synexia.m3.ImmediateShortString", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-immediate-string/manifest.tsv", "a9f5eae59ecc958dd5873795f20782a1a99db80a", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-immediate-string/02-M3StringOwner.java.txt", "329c0ba4d3751a2d76d686237439a2f94831658e", "ef2a5b69ca7c11154d4a2eede64212fe55f27ddba0f68408ce40750aeb57d20e", "CANONICAL_POSTIMAGE_BOUND", "BOUNDED_CANONICAL_POSTIMAGE_SEQUENCE", "SOURCE_CUSTODY_ONLY", "NOT_ACCEPTED"),
    ("M3JDK21_STRING_ADDITIONAL_OWNER_SOURCE_SUCCESSOR_V1", "M3StringAtom", "1", "src/java.base/share/classes/java/lang/M3StringAtom.java", "7e7645497d08d55cf9b5174e4f04e11c87a06bae", "e001823fefe0242028b3116d0fbce8512956614a", "20905d45d3e96e0fb1b37b0482eff71888264b351ab001c07e9b0d56d9c3a31f", "26b4138ae6e438615ce6f894e956ca84a1c18579f1aa4b76aff0ce9ebbf04b0b", "604eb846332a60abc169eee798ed1a886fc0a581", "1493", "12498", "7b899aec4ed6eb8d20c4125631fe168e0cb13951", "com.synexia.m3jdk.CollectionsContractRefinement20261017", "synexia-openrewrite-recipes/recipe-crates/m3jdk21-fixed-lexicon-convergence-20261019/datasets/atom-lifetime-20261019/resources/com/synexia/rewrite/hash-pinned-java/m3-collections-contract-refinement-20261017-atom-lifetime-20261019/manifest.tsv", "0167122647b5746744adaa1c3a4939ec40fa92c9", "synexia-openrewrite-recipes/recipe-crates/m3jdk21-fixed-lexicon-convergence-20261019/datasets/atom-lifetime-20261019/resources/com/synexia/rewrite/hash-pinned-java/m3-collections-contract-refinement-20261017-atom-lifetime-20261019/0001-M3StringAtom.java.txt", "e001823fefe0242028b3116d0fbce8512956614a", "26b4138ae6e438615ce6f894e956ca84a1c18579f1aa4b76aff0ce9ebbf04b0b", "CANONICAL_POSTIMAGE_BOUND", "PARTIAL_FINAL_CANONICAL_EDGE_UNRECONSTRUCTED", "SOURCE_CUSTODY_ONLY", "NOT_ACCEPTED"),
    ("M3JDK21_STRING_ADDITIONAL_OWNER_SOURCE_SUCCESSOR_V1", "M3StringAtom", "2", "src/java.base/share/classes/java/lang/M3StringAtom.java", "e001823fefe0242028b3116d0fbce8512956614a", "84a0227edbd5c3f73215289619f95fc18d57eaf1", "26b4138ae6e438615ce6f894e956ca84a1c18579f1aa4b76aff0ce9ebbf04b0b", "193500ea334dc1693b1e4286a1dbc1aefbc24b262ca4bba4903f4fdbe2ac39b3", "53d40c2a696ac62b9872ed0ba62ec91570878b44", "1703", "UNRECONSTRUCTED", "UNRECONSTRUCTED", "UNRECONSTRUCTED", "UNRECONSTRUCTED", "UNRECONSTRUCTED", "UNRECONSTRUCTED", "UNRECONSTRUCTED", "UNRECONSTRUCTED", "TARGET_SOURCE_OBSERVATION_ONLY", "PARTIAL_FINAL_CANONICAL_EDGE_UNRECONSTRUCTED", "SOURCE_CUSTODY_ONLY", "NOT_ACCEPTED"),
    ("M3JDK21_STRING_ADDITIONAL_OWNER_SOURCE_SUCCESSOR_V1", "M3StringTuple", "1", "src/java.base/share/classes/java/lang/M3StringTuple.java", "0d4cfb75317e68d54244588a474eba98bf7df27b", "ede9e34a4d78272255f7ea7a53efaa8a54c118d9", "0788834635dee7c22e94b92fc7d1b42bdc42c7b1d8948efabc4e94062173d4b9", "ab4dd12938aabe30b8d70d52fb6dec2659c9e07d743c1f909df6781ccb315bf4", "52d147a42877434a66f785866135f8b2184bd38c", "1683", "12724", "f500ca0e84150dfff0af5a11a07d3202170a0bb0", "com.synexia.m3.Jdk21StringLeafPathJni20261021", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-string-leafpath-jni-shadow-20261021/manifest.tsv", "637763bfcacc797f3ca370b572ed05c0ed1c43a2", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-string-leafpath-jni-shadow-20261021/M3StringTuple.java.txt", "ede9e34a4d78272255f7ea7a53efaa8a54c118d9", "ab4dd12938aabe30b8d70d52fb6dec2659c9e07d743c1f909df6781ccb315bf4", "CANONICAL_POSTIMAGE_BOUND", "BOUNDED_CANONICAL_POSTIMAGE_SEQUENCE", "SOURCE_CUSTODY_ONLY", "NOT_ACCEPTED"),
    ("M3JDK21_STRING_ADDITIONAL_OWNER_SOURCE_SUCCESSOR_V1", "M3StringCodePointPrecompute", "1", "src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java", "6cbb277119f387aac772373d31621af64bea68d5", "011377436faba14db9fb15bb8f63ecbbf7501dae", "50e1fe55cd5e8a53b72aa8a4772f826f712c7e3edb3239514f9504819a0b0257", "43f65f6e578dc5b3ad59046a52ea70bffae3bbff855c861bc2ffd24d1f4bf3d2", "fd4da259808f11d4e8daefc0d174679669ac9d02", "998", "11966", "834a70ec23c251ce01c98f0b1afd1e1d0ec4be15", "com.synexia.m3.Jdk21CodePointRankCursor20261017", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-codepoint-rank-cursor-20261017/manifest.tsv", "7a92b74a9723f3ce09d00fa38f16f738e0544430", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-codepoint-rank-cursor-20261017/M3StringCodePointPrecompute.java.txt", "011377436faba14db9fb15bb8f63ecbbf7501dae", "43f65f6e578dc5b3ad59046a52ea70bffae3bbff855c861bc2ffd24d1f4bf3d2", "CANONICAL_POSTIMAGE_BOUND", "BOUNDED_CANONICAL_POSTIMAGE_SEQUENCE", "SOURCE_CUSTODY_ONLY", "NOT_ACCEPTED"),
    ("M3JDK21_STRING_ADDITIONAL_OWNER_SOURCE_SUCCESSOR_V1", "M3TQ", "1", "src/java.base/share/classes/jdk/internal/mindex/M3TQ.java", "533a07b77061e757d476627557413f6df1067d34", "757ac3acb2bb32d2dc54f839e3ba64b2c5d4778a", "78aa154b8aa25134edd9a16c746e4fdc7bcd3984b8f76cc96ef81437247e08d4", "79e922eb3d0c3ae28db264bcaeab0a03f31643ad581c3e28bc2545140c043367", "876b018258d9da07ffcfc012f0eae1e1a03305bd", "1679", "12717", "e3adf09c8257dd3058f7a0736afb6af6cab20324", "com.synexia.rewrite.M3TQArrayCompositionRecipe", "synexia-openrewrite-recipes/recipes/m3jdk21-tq-primitive-composition-20261021/manifest.tsv", "06c4615bdec89ee1b735a340e772c93287fb8a9c", "synexia-openrewrite-recipes/recipes/m3jdk21-tq-primitive-composition-20261021/postimages/p00.txt", "757ac3acb2bb32d2dc54f839e3ba64b2c5d4778a", "79e922eb3d0c3ae28db264bcaeab0a03f31643ad581c3e28bc2545140c043367", "CANONICAL_POSTIMAGE_BOUND", "BOUNDED_CANONICAL_POSTIMAGE_SEQUENCE", "SOURCE_CUSTODY_ONLY", "NOT_ACCEPTED"),
    ("M3JDK21_STRING_ADDITIONAL_OWNER_SOURCE_SUCCESSOR_V1", "M3TQ", "2", "src/java.base/share/classes/jdk/internal/mindex/M3TQ.java", "757ac3acb2bb32d2dc54f839e3ba64b2c5d4778a", "e353a1c656c573d832d50a310ce17a52ae8ee4cd", "79e922eb3d0c3ae28db264bcaeab0a03f31643ad581c3e28bc2545140c043367", "f05c47933a11245c697df322fa9aa85ddd1a117f6ba44d68f3d619a1971c0e28", "c8fb46ad6995189c0d46c55ac8483a6615cfb119", "1980", "13226", "e0328114fa329f8ee448d5307548e8aa505f3efe", "M3HashPinnedJavaSnapshotRecipe", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-regex-shortcircuit-20261023/manifest.tsv", "0c8823581e86887620e6e6ebefa2c9dd56bdf1e1", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-regex-shortcircuit-20261023/0003-M3TQ.java.txt", "e353a1c656c573d832d50a310ce17a52ae8ee4cd", "f05c47933a11245c697df322fa9aa85ddd1a117f6ba44d68f3d619a1971c0e28", "CANONICAL_POSTIMAGE_BOUND", "BOUNDED_CANONICAL_POSTIMAGE_SEQUENCE", "SOURCE_CUSTODY_ONLY", "NOT_ACCEPTED"),
    ("M3JDK21_STRING_ADDITIONAL_OWNER_SOURCE_SUCCESSOR_V1", "StringNativeC", "1", "src/java.base/share/native/libjava/String.c", "69a57659e74c6433491932e64f15bc037e887d81", "4be04134aee3093df1776f805dd2712137b4bc81", "ee9d9bfa986259600d6fe95e21e3cb904a4f6541ce24c6391bc50715c6a054ef", "bb2beafaf57c1458f675eed554fc35e08c616f5c8b0a40178f47356768496086", "52d147a42877434a66f785866135f8b2184bd38c", "1683", "12724", "42b5098718732964f711bd34908990cdba58b9ac", "com.synexia.m3.Jdk21StringLeafPathJni20261021", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-leafpath-jni-shadow-20261021/manifest.tsv", "4f1662a58e533e9b1cdf6e6a585e29e32754ce02", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-leafpath-jni-shadow-20261021/String.c.txt", "4be04134aee3093df1776f805dd2712137b4bc81", "bb2beafaf57c1458f675eed554fc35e08c616f5c8b0a40178f47356768496086", "CANONICAL_POSTIMAGE_BOUND", "BOUNDED_CANONICAL_POSTIMAGE_SEQUENCE", "SOURCE_CUSTODY_ONLY", "NOT_ACCEPTED"),
    ("M3JDK21_STRING_ADDITIONAL_OWNER_SOURCE_SUCCESSOR_V1", "StringNativeC", "2", "src/java.base/share/native/libjava/String.c", "4be04134aee3093df1776f805dd2712137b4bc81", "571032d70eed8579aff225a86aa4cac41836dd4f", "bb2beafaf57c1458f675eed554fc35e08c616f5c8b0a40178f47356768496086", "6f710e824e5416c8349dcfc5f81985f095519ced05175a2aaf76dfb436c87c54", "0c0aaf310cf0dd8a5b4eb2d23ec9944d8e547d3d", "1707", "12748", "fb936abe6e3c643f5198b0aaefe5589359a0ec98", "com.synexia.m3.StringArrayCollectionReconcile20261021", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-string-array-collection-reconcile-20261021-014/manifest.tsv", "11cd730e1c6e0ccfe83b9fb5af9ef2410aabbe19", "synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-string-array-collection-reconcile-20261021-014/postimage.txt", "571032d70eed8579aff225a86aa4cac41836dd4f", "6f710e824e5416c8349dcfc5f81985f095519ced05175a2aaf76dfb436c87c54", "CANONICAL_POSTIMAGE_BOUND", "BOUNDED_CANONICAL_POSTIMAGE_SEQUENCE", "SOURCE_CUSTODY_ONLY", "NOT_ACCEPTED"),
)

PRECOMPUTE_OBSERVATION = Path(__file__).with_name("m3-string-precompute-observation-v2.tsv")
PRECOMPUTE_OBSERVATION_HEADER = [
    "schema",
    "snapshot_date_utc",
    "source_symbol",
    "source_path",
    "source_git_blob",
    "source_license",
    "source_relation",
    "cache_symbol",
    "cache_path",
    "cache_git_blob",
    "cache_relation",
    "target_symbol",
    "target_path",
    "target_license",
    "checkpoint_m3jdk_pr",
    "checkpoint_m3jdk_merge_commit",
    "checkpoint_target_git_blob",
    "observation_m3jdk_pr",
    "observation_m3jdk_head",
    "observation_m3jdk_merge_commit",
    "observation_target_git_blob",
    "current_m3jdk_pr",
    "current_m3jdk_merge_commit",
    "current_m3jdk_master_commit",
    "current_target_git_blob",
    "synexia_source_recipe_pr",
    "synexia_source_recipe_head",
    "synexia_source_recipe_state_at_capture",
    "synexia_donor_decision_pr",
    "synexia_donor_decision_role",
    "implementation_origin",
    "runtime_acceptance",
    "performance_evidence",
    "state",
]
EXPECTED_PRECOMPUTE_OBSERVATION = {
    "schema": "M3JDK21_STRING_PRECOMPUTE_OBSERVATION_V2",
    "snapshot_date_utc": "2026-10-21",
    "source_symbol": "com.synexia.indexstring.MIndexStringSearchPlan",
    "source_path": "synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexStringSearchPlan.java",
    "source_git_blob": "402e6d8cf2f99d19ffc9c1291afee7baf80665ec",
    "source_license": "Apache-2.0",
    "source_relation": "canonical operation-search facts; source mapping only",
    "cache_symbol": "com.synexia.mindex.MIndexStringPrecomputeCache",
    "cache_path": "synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexStringPrecomputeCache.java",
    "cache_git_blob": "0e777d4fe11e169aab7c495e334316e443142f16",
    "cache_relation": "CONSUMER_SPLIT_PRECOMPUTE; source mapping only",
    "target_symbol": "java.lang.M3StringSearchPrecompute",
    "target_path": "src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java",
    "target_license": "GPL-2.0-with-Classpath-exception",
    "checkpoint_m3jdk_pr": "864",
    "checkpoint_m3jdk_merge_commit": "fc57c84cbbd48c6fe25113a683992d7495aec7e5",
    "checkpoint_target_git_blob": "dfbb91cc169916cd0a95798f4cd2151c3407df4c",
    "observation_m3jdk_pr": "869",
    "observation_m3jdk_head": "e0422f994559a2d9c2dee3b9b2af768a2b69c70a",
    "observation_m3jdk_merge_commit": "d5abed865bd47c4dd5ad78e95704348fe2095ed2",
    "observation_target_git_blob": "ac68eacd6ee4e6048ab424542ccf5c2cf5595908",
    "current_m3jdk_pr": "920",
    "current_m3jdk_merge_commit": "3d9d44eeed2859fe5c3d353723e7b627b379205d",
    "current_m3jdk_master_commit": "2779aba3f98f31c324be43f731be7914e1a05935",
    "current_target_git_blob": "6f89d8cc25c8d8b7d372faaa7a9a550f819868c3",
    "synexia_source_recipe_pr": "11559",
    "synexia_source_recipe_head": "46d4d4864fcbffda3538e594f6be9babc0f88407",
    "synexia_source_recipe_state_at_capture": "OPEN_DRAFT",
    "synexia_donor_decision_pr": "11270",
    "synexia_donor_decision_role": "FIRST_PARTY_REUSE_INVENTORY_ONLY",
    "implementation_origin": "M3JDK_TARGET_LOCAL_CURSOR_AND_M3TQ_REUSE_NO_SYNEXIA_CODE_COPY",
    "runtime_acceptance": "NOT_CLAIMED",
    "performance_evidence": "NOT_MEASURED",
    "state": "SOURCE_OBSERVATION_NOT_RUNTIME_ACCEPTED",
}
PRECOMPUTE_SEARCH_MAPPING_SOURCE = (
    "com.synexia.indexstring.MIndexStringSearchPlan and canonical operation-search facts"
)
PRECOMPUTE_SEARCH_MAPPING_RULE = (
    "weakly key exact M3 owner+coordinate; retain primitive pattern metadata only; "
    "no String/char[]/byte[]/M3String payload; eviction changes performance only"
)

M3_STRING_PATH = "src/java.base/share/classes/java/lang/M3String.java"
# Each exact transition was independently observed at the named GitHub PR head.
EXPECTED_STRING_CHAIN = (
    ("70d1d03c109126fdda36c54da21b484bbe368fd9",
     "4fb87fd70ffe5c8d75d17db9c28b44bc9b574704", "11220", "862"),
    ("4fb87fd70ffe5c8d75d17db9c28b44bc9b574704",
     "42399d411c1516a56a47dcdb10242960157b4677", "11247", "864"),
    ("42399d411c1516a56a47dcdb10242960157b4677",
     "35ed998540e044eed8cb7596e2e2c019ae369bd3", "11448", "898"),
)

SUCCESSOR_MARKERS = {
    "src/java.base/share/classes/java/lang/M3String.java": (
        "private final M3StringOwner owner;",
        "private final long value;",
        "static M3String joinDesignated(",
        "M3String left = canonicalize(first);",
        "if (left == null) return null;",
        "M3String right = canonicalize(second);",
        "if (right == null) return null;",
        "return M3StringPool.concat(left, right);",
        "static boolean isLiteralRegex(String regex)",
    ),
    "src/java.base/share/classes/java/lang/M3StringFacts.java": (
        "boolean mayContain(String needle)",
        "boolean prefixMayMatch(String prefix)",
        "boolean suffixMayMatch(String suffix)",
        "if ((bitSignal64 & units) != units) return false;",
    ),
    "src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java": (
        "final byte[] firstOffsets;",
        "final long[] masks;",
        "char candidate = source.charAt(blockStart + firstOffsets[mid]);",
        "MAX_SOURCE_UNITS * (Byte.BYTES + Long.BYTES)",
    ),
    "src/java.base/share/classes/java/lang/M3StringPool.java": (
        "if (left.length() == 0) return right;",
        "if (right.length() == 0) return left;",
        "if ((long) left.length() + right.length() > Integer.MAX_VALUE) {",
        "throw new OutOfMemoryError(\"Required length exceeds implementation limit\");",
        "if (left.owner() == right.owner() && left.end() == right.start()) {",
        "return M3String.range(",
    ),
}

EXPECTED_RELATIONS = {
    "VALUE_OWNER",
    "VALUE_POOL",
    "VALUE_OWNER_BASE",
    "VALUE_ATOM",
    "VALUE_TUPLE",
    "RANGE_VIEW",
    "FACTS_METADATA",
    "FACTS_TABLE",
    "PRECOMPUTE_CACHE",
    "PRECOMPUTE_IMAGE",
    "PRECOMPUTE_PROFILE",
    "SEARCH_INDEX",
    "BRIDGE",
    "SHADOW",
}

HEADER = [
    "schema",
    "phase",
    "relation",
    "source_path",
    "source_git_blob",
    "target_path",
    "target_git_blob",
    "relationship",
    "license_class",
    "copyright_class",
    "state",
]


def git_blob(path: Path) -> str:
    data = path.read_bytes()
    return hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest()


def load_tsv(path: Path, header: list[str]) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        if reader.fieldnames != header:
            raise ValueError(f"invalid header: {path}")
        rows = list(reader)
    if not rows:
        raise ValueError(f"empty table: {path}")
    return rows


def approved_successors(mapping_rows: list[dict[str, str]]) -> dict[str, dict[str, str]]:
    """Admit only exact qualified product postimages without rewriting the donor map."""
    baseline: dict[str, str] = {}
    for row in mapping_rows:
        path = row["target_path"]
        pinned = row["target_git_blob"]
        if path in baseline and baseline[path] != pinned:
            raise ValueError("contradictory historical target pin: " + path)
        baseline[path] = pinned

    received: dict[str, dict[str, str]] = {}
    for index, row in enumerate(load_tsv(SUCCESSORS, SUCCESSOR_HEADER), start=2):
        path = row["target_path"]
        if any(not row[field] for field in SUCCESSOR_HEADER):
            raise ValueError(f"blank successor receipt field at row {index}")
        if row["schema"] != "M3JDK21_STRING_TARGET_SUCCESSOR_V1":
            raise ValueError(f"invalid successor receipt schema at row {index}")
        if path in received:
            raise ValueError("duplicate successor owner: " + path)
        if path not in EXPECTED_SUCCESSOR_PATHS or path not in baseline:
            raise ValueError("unqualified successor owner: " + path)
        if row["historical_git_blob"] != baseline[path]:
            raise ValueError("historical target pin altered: " + path)
        if not HEX40.fullmatch(row["successor_git_blob"]):
            raise ValueError("unsealed successor Git blob: " + path)
        if row["successor_git_blob"] == baseline[path]:
            raise ValueError("successor did not advance original target: " + path)
        for key in ("synexia_prs", "m3jdk_prs"):
            if not re.fullmatch(r"[0-9]+(?:,[0-9]+)*", row[key]):
                raise ValueError("unqualified successor PR lineage: " + path)
        if row["state"] != "HISTORY_PRESERVING_SUCCESSOR":
            raise ValueError("successor promotion policy drift: " + path)
        received[path] = row

    if set(received) != EXPECTED_SUCCESSOR_PATHS:
        raise ValueError(
            "successor inventory drift: missing="
            + repr(sorted(EXPECTED_SUCCESSOR_PATHS - set(received)))
            + " unexpected="
            + repr(sorted(set(received) - EXPECTED_SUCCESSOR_PATHS))
        )
    return received


def merged_source_observation(current: dict[str, str], rows: list[dict[str, str]]) -> dict[str, str]:
    """Validate immutable merge coordinates only; do not claim a proved transform."""
    if len(rows) != len(EXPECTED_MERGED_SOURCE_OBSERVATION):
        raise ValueError("merged source observation inventory drift")
    for index, (step, values) in enumerate(zip(rows, EXPECTED_MERGED_SOURCE_OBSERVATION), start=1):
        expected = dict(zip(MERGED_SOURCE_OBSERVATION_HEADER, values))
        if step != expected:
            raise ValueError(f"unreviewed merged source observation: {index}")
    if current[M3_STRING_PATH] != rows[0]["git_blob"]:
        raise ValueError("discontinuous merged source observation")
    observed = dict(current)
    observed[M3_STRING_PATH] = rows[-1]["git_blob"]
    return observed


def facts_successor_observation(current: dict[str, str], rows: list[dict[str, str]]) -> dict[str, str]:
    """Validate ordered canonical Facts source images without promoting runtime state."""
    if len(rows) != len(EXPECTED_FACTS_SUCCESSOR_CHAIN):
        raise ValueError("Facts successor inventory drift")
    observed = dict(current)
    for index, (step, values) in enumerate(zip(rows, EXPECTED_FACTS_SUCCESSOR_CHAIN), start=1):
        expected = dict(zip(FACTS_SUCCESSOR_CHAIN_HEADER, values))
        if step != expected:
            raise ValueError(f"unreviewed Facts successor: {index}")
        if observed[M3_STRING_FACTS_PATH] != step["predecessor_git_blob"]:
            raise ValueError(f"discontinuous Facts successor: {index}")
        observed[M3_STRING_FACTS_PATH] = step["successor_git_blob"]
    return observed


def pool_source_observation(current: dict[str, str], rows: list[dict[str, str]]) -> dict[str, str]:
    """Validate a frozen Pool source DAG only; canonical history remains partial."""
    if len(rows) != len(EXPECTED_POOL_SOURCE_OBSERVATION):
        raise ValueError("Pool source observation inventory drift")
    for index, (step, values) in enumerate(zip(rows, EXPECTED_POOL_SOURCE_OBSERVATION), start=1):
        expected = dict(zip(POOL_SOURCE_OBSERVATION_HEADER, values))
        if step != expected:
            raise ValueError(f"unreviewed Pool source observation: {index}")
    if current[M3_STRING_POOL_PATH] != rows[0]["git_blob"]:
        raise ValueError("discontinuous Pool historical anchor")
    observed = dict(current)
    observed[M3_STRING_POOL_PATH] = rows[-1]["git_blob"]
    return observed


def current_successor_blobs(mapping_rows: list[dict[str, str]]) -> dict[str, str]:
    """Validate V1/String/Facts receipts and a separate frozen Pool source DAG.

    Pool observations retain PARTIAL canonical history and its unresolved old gap.
    Neither merge observation is a proved serial transform. Wrong source, order,
    provenance, missing or extra rows fail closed. No runtime, whole-image, JNI,
    JIT/GC or performance acceptance follows from these source coordinates.
    """
    historical = approved_successors(mapping_rows)
    current = {target: row["successor_git_blob"] for target, row in historical.items()}
    chain = load_tsv(SUCCESSOR_CHAIN, SUCCESSOR_CHAIN_HEADER)
    if len(chain) != len(EXPECTED_STRING_CHAIN):
        raise ValueError("String successor chain length drift")
    for index, (step, expected) in enumerate(zip(chain, EXPECTED_STRING_CHAIN), start=1):
        before, after, synexia_pr, m3jdk_pr = expected
        if any(not step[field] for field in SUCCESSOR_CHAIN_HEADER):
            raise ValueError(f"blank String successor chain field: {index}")
        if (step["schema"] != "M3JDK21_STRING_TARGET_EVOLUTION_V1"
                or step["target_path"] != M3_STRING_PATH
                or step["predecessor_git_blob"] != before
                or step["successor_git_blob"] != after
                or step["synexia_pr"] != synexia_pr
                or step["m3jdk_pr"] != m3jdk_pr
                or step["state"] != "SOURCE_CANDIDATE_NOT_RUNTIME_ACCEPTED"):
            raise ValueError(f"unreviewed String successor chain step: {index}")
        if current[M3_STRING_PATH] != before:
            raise ValueError(f"discontinuous String successor chain: {index}")
        current[M3_STRING_PATH] = after
    observed = merged_source_observation(
        current, load_tsv(MERGED_SOURCE_OBSERVATION, MERGED_SOURCE_OBSERVATION_HEADER))
    facts = facts_successor_observation(
        observed, load_tsv(FACTS_SUCCESSOR_CHAIN, FACTS_SUCCESSOR_CHAIN_HEADER))
    return pool_source_observation(
        facts, load_tsv(POOL_SOURCE_OBSERVATION, POOL_SOURCE_OBSERVATION_HEADER))



def additional_owner_successors(mapping_rows: list[dict[str, str]]) -> dict[str, str]:
    """Return six exact source coordinates without promoting source custody to runtime acceptance.

    This is separate from the four historical successor owners. Each declared
    base-to-current edge retains its own canonical binding or explicit unknown
    provenance. Atom's final canonical edge is not reconstructed by this table.
    """
    current: dict[str, str] = {}
    for path, (before, _) in ADDITIONAL_OWNER_BASES.items():
        matches = [row for row in mapping_rows if row.get("target_path") == path]
        if len(matches) != 1 or matches[0].get("target_git_blob") != before:
            raise ValueError("additional owner historical anchor drift: " + path)
        current[path] = before
    rows = load_tsv(ADDITIONAL_OWNER_SUCCESSORS, ADDITIONAL_OWNER_HEADER)
    if len(rows) != len(EXPECTED_ADDITIONAL_OWNER_SUCCESSORS):
        raise ValueError("additional owner successor inventory drift")
    for index, (row, values) in enumerate(zip(rows, EXPECTED_ADDITIONAL_OWNER_SUCCESSORS), start=1):
        if row != dict(zip(ADDITIONAL_OWNER_HEADER, values)):
            raise ValueError(f"unreviewed additional owner successor: {index}")
        path = row["target_path"]
        if current.get(path) != row["predecessor_git_blob"]:
            raise ValueError(f"discontinuous additional owner successor: {index}")
        current[path] = row["successor_git_blob"]
    expected = {path: values[1] for path, values in ADDITIONAL_OWNER_BASES.items()}
    if current != expected:
        raise ValueError("additional owner final identity drift")
    return current


def load_precompute_observation() -> dict[str, str]:
    """Read the exact V2 lineage receipt; candidate facts never imply acceptance."""
    rows = load_tsv(PRECOMPUTE_OBSERVATION, PRECOMPUTE_OBSERVATION_HEADER)
    if len(rows) != 1:
        raise ValueError("M3String precompute observation cardinality drift")
    row = rows[0]
    if any(not row[field] for field in PRECOMPUTE_OBSERVATION_HEADER):
        raise ValueError("blank M3String precompute observation field")
    if row != EXPECTED_PRECOMPUTE_OBSERVATION:
        raise ValueError("unreviewed M3String precompute observation drift")
    for field in (
        "source_git_blob", "cache_git_blob", "checkpoint_m3jdk_merge_commit",
        "checkpoint_target_git_blob", "observation_m3jdk_head",
        "observation_m3jdk_merge_commit", "observation_target_git_blob",
        "current_m3jdk_merge_commit", "current_m3jdk_master_commit",
        "current_target_git_blob", "synexia_source_recipe_head",
    ):
        if not HEX40.fullmatch(row[field]):
            raise ValueError("unsealed M3String precompute observation: " + field)
    for field in (
        "checkpoint_m3jdk_pr", "observation_m3jdk_pr", "current_m3jdk_pr",
        "synexia_source_recipe_pr", "synexia_donor_decision_pr",
    ):
        if not row[field].isdigit():
            raise ValueError("invalid M3String precompute PR: " + field)
    target = ROOT / row["target_path"]
    if target.is_file() and git_blob(target) != row["current_target_git_blob"]:
        raise ValueError("live M3JDK precompute target blob drift")
    return row


def validate_precompute_mapping(mapping_rows: list[dict[str, str]],
                                observation: dict[str, str]) -> None:
    matches = [
        row for row in mapping_rows
        if row["relation"] == "PRECOMPUTE_CACHE"
        and row["source_path"] == observation["cache_path"]
    ]
    if len(matches) != 1:
        raise ValueError("MIndexString precompute source mapping missing or duplicated")
    row = matches[0]
    if (row["source_git_blob"] != observation["cache_git_blob"]
            or row["target_path"] != observation["target_path"]
            or row["relationship"] != "CONSUMER_SPLIT_PRECOMPUTE"
            or row["license_class"] != observation["source_license"]
            or row["state"] != "PINNED_MAPPING"):
        raise ValueError("MIndexString precompute mapping contract drift")


def validate_successor_source(path: str, source: str) -> None:
    for fragment in SUCCESSOR_MARKERS[path]:
        if fragment not in source:
            raise ValueError(f"history-preserving successor responsibility lost: {path} {fragment}")
    if path.endswith("M3StringPositionPrecompute.java"):
        block = source.split("private static final class ExactBlock {", 1)
        if len(block) != 2:
            raise ValueError("position-mask ExactBlock missing")
        exact = block[1].split("private static final class Entry {", 1)[0]
        if re.search(r"(?m)^\s*(?:final\s+)?char\s*\[\s*\]\s+\w+\s*;", exact):
            raise ValueError("position-mask successor reintroduced retained char[] spelling")

    if path.endswith("M3StringPool.java"):
        ordered = (
            "if (left.length() == 0) return right;",
            "if (right.length() == 0) return left;",
            "if ((long) left.length() + right.length() > Integer.MAX_VALUE) {",
            "throw new OutOfMemoryError(\"Required length exceeds implementation limit\");",
            "if (left.owner() == right.owner() && left.end() == right.start()) {",
            "return concatBalanced(left, right);",
        )
        positions = [source.find(fragment) for fragment in ordered]
        if any(position < 0 for position in positions) or positions != sorted(positions):
            raise ValueError("M3StringPool concat length successor guard order drift")


def estate_snapshot() -> tuple[str, str]:
    with ESTATE.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    matches = [row for row in rows if row["family"] == "MINDEX_STRING_RUNTIME"]
    if len(matches) != 1:
        raise ValueError("MINDEX_STRING_RUNTIME estate row missing or duplicated")
    row = matches[0]
    if row["source_path"] != (
        "synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexString.java"
    ):
        raise ValueError("MIndexString estate source-path drift")
    if row["source_tree_or_blob"] != "1840f12a3ea64750d829f6dd2bda7916fc25d9f5":
        raise ValueError("MIndexString estate source identity drift")
    return row["snapshot_revision"], row["license_class"]


def validate_name_mapping() -> None:
    data = json.loads(NAME_MAP.read_text(encoding="utf-8"))
    port = data.get("porting_invariant", {})
    if port.get("naming_analogy") != (
        "MIndexString -> java.lang.M3String while public java.lang.String remains String"
    ):
        raise ValueError("canonical MIndexString/M3String naming analogy drift")
    sequence = port.get("receiving_sequence", {})
    if sequence.get("active_phase") != "STRING":
        raise ValueError("STRING is not the active receiving phase")
    if sequence.get("first_party_license") != "Apache-2.0":
        raise ValueError("STRING first-party license policy drift")
    if sequence.get("copyright_notice") != (
        "Copyright 2026 Hitesh Soliwal and contributors"
    ):
        raise ValueError("STRING copyright notice drift")
    if sequence.get("abstract_ideas") != "PROVENANCE_NOT_COPYRIGHTED_EXPRESSION":
        raise ValueError("abstract-idea copyright boundary drift")

    family = data.get("family_name_mapping", [])
    rows = [
        row
        for row in family
        if row.get("target_family") == "java.lang.M3String family"
    ]
    if len(rows) != 1:
        raise ValueError("canonical M3String family map missing or duplicated")
    row = rows[0]
    if row.get("public_api") != "java.lang.String remains java.lang.String":
        raise ValueError("public String naming contract drift")
    required = {
        "java.lang.M3String",
        "java.lang.M3StringOwner",
        "java.lang.M3StringAtom",
        "java.lang.M3StringPool",
        "java.lang.M3StringTuple",
    }
    if set(row.get("target_owners", [])) != required:
        raise ValueError("canonical M3String target owner set drift")

    search_maps = [
        item for item in data.get("mappings", [])
        if item.get("source") == PRECOMPUTE_SEARCH_MAPPING_SOURCE
        and item.get("target") == "java.lang.M3StringSearchPrecompute"
    ]
    if len(search_maps) != 1:
        raise ValueError("canonical MIndexString/M3StringSearchPrecompute map missing")
    search_map = search_maps[0]
    if (search_map.get("status") != "internal-only bounded operation-precompute adaptation"
            or search_map.get("mapping_rule") != PRECOMPUTE_SEARCH_MAPPING_RULE):
        raise ValueError("M3StringSearchPrecompute internal mapping contract drift")



WORK_HEADER = [
    "schema",
    "lane",
    "synexia_pr",
    "synexia_head",
    "m3jdk_pr",
    "m3jdk_head",
    "depends_on",
    "target_gate",
    "state",
]

EXPECTED_WORK_LANES = {
    "STRING_SEARCH",
    "REGEX_ORACLE_10K_X64",
    "REGEX_REQUIRED_LITERAL_GATE",
}


def validate_work_orders() -> None:
    rows = load_tsv(WORK, WORK_HEADER)
    lanes: set[str] = set()
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in WORK_HEADER):
            raise ValueError(f"blank STRING work-order field at row {physical}")
        if row["schema"] != "M3JDK21_STRING_WORK_V1":
            raise ValueError(f"invalid STRING work-order schema at row {physical}")
        if row["lane"] in lanes:
            raise ValueError(f"duplicate STRING work lane: {row['lane']}")
        lanes.add(row["lane"])
        if not row["synexia_pr"].isdigit() or not row["m3jdk_pr"].isdigit():
            raise ValueError(f"invalid PR number at row {physical}")
        if not HEX40.fullmatch(row["synexia_head"]):
            raise ValueError(f"invalid Synexia head at row {physical}")
        if not HEX40.fullmatch(row["m3jdk_head"]):
            raise ValueError(f"invalid M3JDK head at row {physical}")
        if row["state"] != "DRAFT_CANDIDATE":
            raise ValueError(
                f"STRING work order claims acceptance without successor receipt at row {physical}"
            )
    if lanes != EXPECTED_WORK_LANES:
        raise ValueError(
            f"STRING work lane drift missing={sorted(EXPECTED_WORK_LANES-lanes)} "
            f"extra={sorted(lanes-EXPECTED_WORK_LANES)}"
        )

def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: check_string_phase.py", file=sys.stderr)
        return 2

    revision, estate_license = estate_snapshot()
    if revision != "296323958b1019edd59b60b9c05cb148d024cfe5":
        raise ValueError("STRING map is not bound to the pinned full-borrow snapshot")
    if estate_license != "Apache-2.0":
        raise ValueError("MIndexString estate lost Apache-2.0 classification")

    rows = load_tsv(MAP, HEADER)
    successors = approved_successors(rows)
    current_blobs = current_successor_blobs(rows)
    additional_blobs = additional_owner_successors(rows)
    if set(additional_blobs) & set(current_blobs):
        raise ValueError("additional owner collides with historical successor owner")
    current_blobs.update(additional_blobs)
    precompute_observation = load_precompute_observation()
    validate_precompute_mapping(rows, precompute_observation)
    if precompute_observation["target_path"] in current_blobs:
        raise ValueError("M3String precompute owner collides with V1 successor chain")
    current_blobs[precompute_observation["target_path"]] = (
        precompute_observation["current_target_git_blob"]
    )
    relations: set[str] = set()
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in HEADER):
            raise ValueError(f"blank STRING mapping field at row {physical}")
        if row["schema"] != "M3JDK21_STRING_PHASE_V1":
            raise ValueError(f"invalid STRING mapping schema at row {physical}")
        if row["phase"] != "STRING":
            raise ValueError(f"non-STRING row at {physical}")
        if row["relation"] in relations:
            raise ValueError(f"duplicate STRING relation: {row['relation']}")
        relations.add(row["relation"])
        if not row["source_path"].startswith(
            "synexia-mindex/runtime/src/main/java/com/synexia/mindex/"
        ):
            raise ValueError(f"source escaped pinned MIndex runtime at row {physical}")
        if not HEX40.fullmatch(row["source_git_blob"]):
            raise ValueError(f"invalid source Git blob at row {physical}")
        if not row["target_path"].startswith("src/java.base/"):
            raise ValueError(f"target escaped java.base at row {physical}")
        if not HEX40.fullmatch(row["target_git_blob"]):
            raise ValueError(f"invalid target Git blob at row {physical}")
        if row["license_class"] != "Apache-2.0":
            raise ValueError(f"source fast lane lost Apache-2.0 at row {physical}")
        if row["copyright_class"] != "FIRST_PARTY_COPYRIGHTABLE_EXPRESSION":
            raise ValueError(f"copyright classification drift at row {physical}")
        if row["state"] != "PINNED_MAPPING":
            raise ValueError(f"STRING mapping claims promotion at row {physical}")

        target = ROOT / row["target_path"]
        if not target.is_file():
            raise ValueError(f"target owner missing: {row['target_path']}")
        actual = git_blob(target)
        successor = successors.get(row["target_path"])
        pinned = current_blobs.get(row["target_path"], row["target_git_blob"])
        if actual != pinned:
            raise ValueError(
                f"target owner drift: {row['target_path']} "
                f"expected={pinned} actual={actual}"
            )
        if successor is not None:
            validate_successor_source(row["target_path"], target.read_text(encoding="utf-8"))

    if relations != EXPECTED_RELATIONS:
        raise ValueError(
            f"STRING relation set drift missing={sorted(EXPECTED_RELATIONS-relations)} "
            f"extra={sorted(relations-EXPECTED_RELATIONS)}"
        )

    validate_name_mapping()
    validate_work_orders()
    print(
        "M3JDK21_STRING_PHASE_PIN_PASS "
        f"rows={len(rows)} source_revision={revision} successors={len(successors)} "
        "promotion=false public_api=java.lang.String"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
