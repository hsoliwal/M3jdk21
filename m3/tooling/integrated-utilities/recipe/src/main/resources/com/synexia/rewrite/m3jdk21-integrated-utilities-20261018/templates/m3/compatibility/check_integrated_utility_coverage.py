#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Verify the existing naming-map utility coverage against pinned source evidence.

This is a source-inventory check. It neither runs product code nor grants runtime,
conformance, resource-performance, source-license or receiving-phase acceptance.
"""
from __future__ import annotations
import argparse
import csv
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

MAPPING = "m3/docs/name-mapping.json"
FRAGMENT = "m3/docs/name-mapping.d/m3-integrated-utility-families-20261018.json"
REQUIRED_FAMILIES = frozenset("objects strings arrays collections iterators spliterators streams collectors functions reflection classes handles io files paths buffers channels resources charsets regex numbers math bits time concurrency processes network archives digests jdbc modules".split())
CATALOGUE_HEADER = ["category", "candidate", "source", "version_or_revision", "rights", "contract_decision", "cost", "validation", "selection"]
CANDIDATE_STATUS = "CANDIDATE_REQUIRES_SOURCE_AND_RUNTIME_QUALIFICATION"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def relative(value: str) -> str:
    require(isinstance(value, str) and value and not value.startswith("/"), "invalid source path")
    require("\\" not in value and ":" not in value, "noncanonical source path")
    require(all(part not in ("", ".", "..", ".git") for part in value.split("/")), "unsafe source path")
    return value


def load_candidate(repository: Path, overlay: Path | None, path: str) -> bytes:
    relative(path)
    candidate = overlay / path if overlay else None
    return (candidate if candidate is not None and candidate.is_file() else repository / path).read_bytes()


def git_sources(repository: Path, revision: str, paths: list[str]) -> dict[str, tuple[str, bytes]]:
    require(bool(re.fullmatch(r"[0-9a-f]{40}", revision)), "baseline must be an immutable commit")
    ordered = sorted(set(relative(path) for path in paths))
    queries = "".join(revision + ":" + path + "\n" for path in ordered).encode("utf-8")
    result = subprocess.run(["git", "cat-file", "--batch"], cwd=repository, input=queries,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
    answer: dict[str, tuple[str, bytes]] = {}
    offset = 0
    for path in ordered:
        end = result.stdout.index(b"\n", offset)
        header = result.stdout[offset:end].decode("ascii").split()
        require(len(header) == 3 and header[1] == "blob", "missing pinned source: " + path)
        size = int(header[2]); offset = end + 1
        body = result.stdout[offset:offset + size]; offset += size + 1
        require(len(body) == size, "truncated Git source: " + path)
        answer[path] = (header[0], body)
    require(offset == len(result.stdout), "unexpected Git source output")
    return answer


def public_declaration(text: str, api: str) -> bool:
    simple = api.rsplit(".", 1)[1]
    pattern = r"(?m)^\s*(?:(?:abstract|final|sealed|non-sealed|static|strictfp)\s+)*public\s+(?:(?:abstract|final|sealed|non-sealed|static|strictfp)\s+)*(?:class|interface|enum|record|@interface)\s+" + re.escape(simple) + r"(?:\s|<|\{|\()"
    return re.search(pattern, text) is not None


def candidate_source(repository: Path, overlay: Path | None, candidate: dict) -> dict:
    path = relative(candidate["target_path"])
    staged = overlay / path if overlay else None
    source = staged if staged is not None and staged.is_file() else repository / path
    if not source.is_file():
        return {"api": candidate["public_api"], "source_observed": False,
                "reason": "candidate source is not available in this source view"}
    text = source.read_text(encoding="utf-8")
    package = candidate["public_api"].rsplit(".", 1)[0]
    declaration = public_declaration(text, candidate["public_api"])
    package_ok = re.search(r"(?m)^\s*package\s+" + re.escape(package) + r"\s*;", text) is not None
    methods = candidate.get("proposed_methods", [])
    # This is conservative declaration-token evidence, not Java type attribution or API conformance.
    missing = [method for method in methods if not re.search(
        r"(?m)^\s*public\s+(?:(?:static|final|synchronized|native|abstract)\s+)*[^;{}]*\b"
        + re.escape(method) + r"\s*\(", text)]
    actual_sha256 = hashlib.sha256(source.read_bytes()).hexdigest()
    expected_sha256 = candidate.get("candidate_source_sha256")
    source_hash_matches = expected_sha256 is None or actual_sha256 == expected_sha256
    missing_declarations = [token for token in candidate.get("declaration_tokens", []) if token not in text]
    return {"api": candidate["public_api"],
            "source_observed": declaration and package_ok and not missing and source_hash_matches and not missing_declarations,
            "source_path": path, "sha256": actual_sha256,
            "expected_sha256": expected_sha256, "source_hash_matches": source_hash_matches,
            "public_declaration": declaration, "package_declaration": package_ok,
            "missing_candidate_method_tokens": missing,
            "missing_candidate_declaration_tokens": missing_declarations,
            "evidence": "SOURCE_HASH_AND_DECLARATION_TOKENS_ONLY_NOT_SIGNATURE_OR_RUNTIME_CONFORMANCE"}


def internal_source(repository: Path, overlay: Path | None, candidate: dict) -> dict:
    path = relative(candidate["target_path"])
    staged = overlay / path if overlay else None
    source = staged if staged is not None and staged.is_file() else repository / path
    if not source.is_file():
        return {"owner": candidate["owner"], "source_observed": False,
                "reason": "candidate source is not available in this source view"}
    body = source.read_bytes()
    text = body.decode("utf-8")
    if candidate["visibility"] == "PACKAGE_PRIVATE":
        owner = candidate["owner"]
        simple = owner.rsplit(".", 1)[1]
        declaration = re.search(r"(?m)^\s*(?:final\s+)?class\s+" + re.escape(simple) + r"(?:\s|\{)", text) is not None
        package = re.search(r"(?m)^\s*package\s+java\.util\s*;", text) is not None
        observed = declaration and package and not public_declaration(text, owner)
    else:
        observed = re.search(r"\b" + re.escape(candidate["jni_symbol"]) + r"\s*\(", text) is not None
    return {"owner": candidate["owner"], "source_path": path, "source_observed": observed,
            "sha256": hashlib.sha256(body).hexdigest(),
            "evidence": "DECLARATION_TOKENS_ONLY_NOT_SIGNATURE_OR_RUNTIME_CONFORMANCE"}


def module_metadata(text: str) -> dict:
    text = re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.S)
    declaration = re.search(r"\bmodule\s+([\w.]+)\s*\{", text)
    require(declaration is not None, "module declaration not found")
    return {
        "name": declaration.group(1),
        "unqualified_exports": re.findall(r"\bexports\s+([\w.]+)\s*;", text),
        "qualified_exports": [{"package": package, "to": re.findall(r"[\w.]+", recipients)}
                              for package, recipients in re.findall(r"\bexports\s+([\w.]+)\s+to\s+([^;]+);", text)],
        "requires": [{"module": name, "modifiers": modifiers.split()}
                     for modifiers, name in re.findall(r"\brequires\s+((?:(?:transitive|static)\s+)*)([\w.]+)\s*;", text)],
        "uses": re.findall(r"\buses\s+([\w.]+)\s*;", text),
        "provides": [{"service": service, "implementations": re.findall(r"[\w.]+", implementations)}
                     for service, implementations in re.findall(r"\bprovides\s+([\w.]+)\s+with\s+([^;]+);", text)],
    }


def owner_disposition(module: str, meta: dict) -> str:
    services = {row["service"] for row in meta["provides"]}
    if module == "java.se":
        return "AGGREGATOR_MODULE_EXISTING_REQUIRES_NO_NEW_UTILITY"
    if module.startswith("jdk.unsupported"):
        return "UNSUPPORTED_EXPORTED_API_KEEP_EXISTING_OWNER_NO_NEW_FACADE"
    if module.startswith("jdk.incubator."):
        return "INCUBATING_EXPORTED_API_KEEP_EXISTING_OWNER_NO_STABILITY_CLAIM"
    if meta["unqualified_exports"]:
        return "EXPORTED_API_MODULE_KEEP_EXISTING_PUBLIC_OWNERS"
    if "java.util.spi.ToolProvider" in services:
        return "TOOL_MODULE_USE_STANDARD_TOOL_PROVIDER_NO_PUBLIC_PACKAGE_FACADE"
    if "java.security.Provider" in services:
        return "SECURITY_PROVIDER_MODULE_USE_STANDARD_JCA_JCE_OWNERS"
    if module.startswith("jdk.internal.") or module == "jdk.hotspot.agent":
        return "INTERNAL_OR_QUALIFIED_EXPORT_MODULE_NO_GENERAL_PUBLIC_UTILITY"
    if services:
        return "SERVICE_PROVIDER_MODULE_RETAIN_EXISTING_SERVICE_OWNERS"
    return "CLI_RUNTIME_OR_INTERNAL_MODULE_NO_PUBLIC_PACKAGE_EXPORT"


def verify_module_index(repository: Path, overlay: Path | None, fragment: dict, core_apis: dict) -> dict:
    index_path = fragment["module_wide_index"]
    require(index_path == "m3/docs/name-mapping.d/m3-jdk-module-owners-20261018.json", "module index authority drift")
    index = json.loads(load_candidate(repository, overlay, index_path))
    require(index["schema"] == 1 and index["mapping_authority"] == MAPPING
            and index["core_family_fragment"] == FRAGMENT, "module index naming-map authority drift")
    require(index["target_repository"] == fragment["target_repository"]
            and index["target_baseline"] == fragment["target_baseline"], "module index baseline drift")
    for flag in ("runtime_qualified", "performance_qualified", "repository_completion", "receiving_sequence_advanced"):
        require(index[flag] is False, "module source inventory cannot claim " + flag)
    require(index["new_public_utility_candidates"] == [], "module inventory must not invent utility facades")
    require(index["module_set_coverage"] == "EXACT_PINNED_SOURCE_TREE_ONLY", "module evidence scope overstatement")
    tree = subprocess.run(["git", "ls-tree", "-r", "--name-only", index["target_baseline"], "src"],
                          cwd=repository, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, text=True).stdout.splitlines()
    observed_paths = sorted(path for path in tree if re.fullmatch(r"src/[^/]+/[^/]+/classes/module-info\.java", path))
    require(observed_paths == sorted(path for path in tree if path.endswith("/module-info.java")), "unclassified source module descriptor layout")
    observed_names = {path.split("/")[1] for path in observed_paths}
    observed_roots = {path.split("/")[1] for path in tree}
    rows = index["modules"]
    require(index["source_module_count"] == len(observed_paths)
            and index["module_source_paths"] == observed_paths
            and sorted(row["path"] for row in rows) == observed_paths,
            "module index does not cover every pinned source module descriptor")
    require(len({row["module"] for row in rows}) == len(rows), "duplicate module ownership row")
    nonmodules = index["source_roots_without_module_declarations"]
    require(len(nonmodules) == len(observed_roots - observed_names)
            and {row["path"] for row in nonmodules} == {"src/" + name for name in observed_roots - observed_names}
            and all(row["disposition"] for row in nonmodules), "unclassified non-module source root")
    extra = index["additional_public_api_sources"]
    require(len({row["public_api"] for row in extra}) == len(extra)
            and not ({row["public_api"] for row in extra} & set(core_apis)), "duplicate core/extended public owner identity")
    apis = {**core_apis, **{row["public_api"]: row for row in extra}}
    evidence = rows + extra
    sources = git_sources(repository, index["target_baseline"], [row["path"] for row in evidence])
    for row in evidence:
        blob, body = sources[row["path"]]
        require(row["git_blob"] == blob and row["sha256"] == hashlib.sha256(body).hexdigest(),
                "module index source seal drift: " + row["path"])
    modules = {row["module"]: row for row in rows}
    for module, row in modules.items():
        metadata = module_metadata(sources[row["path"]][1].decode("utf-8"))
        require(metadata.pop("name") == module and row["path"].split("/")[1] == module,
                "module source owner mismatch")
        require(row["source_platform"] == row["path"].split("/")[2], "module platform ownership drift")
        require(all(row[key] == value for key, value in metadata.items()), "module declaration/export/provider drift: " + module)
        require(row["module_completion"] is False and row["receiving_sequence_advanced"] is False
                and bool(row["remaining_scope"]), "module row falsely claims completion")
        require(row["evidence_level"] == "BASELINE_MODULE_DECLARATION_AND_REPRESENTATIVE_SOURCE_ONLY",
                "module row evidence overstatement")
        require(row["owner_disposition"] == owner_disposition(module, metadata), "module owner disposition drift: " + module)
        expected_refs = sorted(api for api, source in apis.items() if source["module"] == module)
        require(row["public_owner_source_refs"] == expected_refs, "public module owner references drift: " + module)
        require(not row["unqualified_exports"] or bool(expected_refs), "public module lacks representative source owner: " + module)
        require(set(row["standard_entry_point_refs"]).issubset(apis), "unknown shared module entry point")
        declared_public_services = {entry["service"] for entry in row["provides"] if entry["service"] in apis and apis[entry["service"]]["module"] != module}
        require(declared_public_services.issubset(row["standard_entry_point_refs"]), "declared public provider service owner is missing")
        require(all(apis[api]["module"] != module for api in row["standard_entry_point_refs"]),
                "shared entry point is mislabeled as an external module owner")
    for row in extra:
        api = row["public_api"]
        require(row["visibility"] == "PUBLIC" and row["source_kind"] == "JAVA_SOURCE"
                and row["evidence_level"] == "BASELINE_SOURCE_DECLARATION_AND_MODULE_EXPORT", "extended API evidence overstatement")
        require(row["template_substitutions"] == {} and row["generation_owner"] is None, "unexpected extended template metadata")
        require(row["module"] in modules and row["path"].startswith("src/" + row["module"] + "/"), "extended owner/module mismatch")
        require(row["package"] == api.rsplit(".", 1)[0]
                and row["package"] in modules[row["module"]]["unqualified_exports"], "extended API not publicly exported by owning module")
        text = sources[row["path"]][1].decode("utf-8")
        require(public_declaration(text, api), "extended API public declaration absent: " + api)
        require(re.search(r"(?m)^\s*package\s+" + re.escape(row["package"]) + r"\s*;", text) is not None, "extended API package declaration absent")
        module = row["module"]
        expected_support = ("UNSUPPORTED_JDK_MODULE_EXISTING_PUBLIC_DECLARATION" if module.startswith("jdk.unsupported")
                            else "INCUBATING_JDK_MODULE_EXISTING_PUBLIC_DECLARATION" if module.startswith("jdk.incubator.")
                            else "PREVIEW_API_EXISTING_PUBLIC_DECLARATION" if row["package"] == "java.lang.foreign"
                            else "EXISTING_EXPORTED_API_OWNER")
        require(row["support_disposition"] == expected_support, "preview/incubator/unsupported API status drift")
        require(row["role"] == "EXISTING_FACTORY_UTILITY_PROTOCOL_OR_DATA_OWNER_NO_NEW_FACADE", "extended source invents a new utility owner")
    aspects = index["additional_aspect_ownership"]
    require(bool(aspects) and len({row["aspect"] for row in aspects}) == len(aspects), "missing or duplicated additional aspect ownership")
    for aspect in aspects:
        require(bool(aspect["owner_source_refs"]) and set(aspect["owner_source_refs"]).issubset(apis), "unknown additional aspect public owner")
        require(aspect["owning_modules"] == sorted({apis[api]["module"] for api in aspect["owner_source_refs"]}), "additional aspect module ownership drift")
        require(aspect["disposition"] == "EXISTING_SOURCE_OWNERS_ONLY_NO_NEW_PUBLIC_FACADE"
                and aspect["family_completion"] is False and aspect["receiving_sequence_advanced"] is False
                and bool(aspect["remaining_scope"]), "additional aspect overstates implementation or qualification")
    for family in fragment["families"]:
        require(set(family["related_module_owner_refs"]).issubset(apis), "unknown core-family related module owner")
        slots = family.get("pending_extension_slots", [])
        require(not slots or family["id"] == "regex", "unexpected pending extension family")
        for slot in slots:
            require(slot["aspect"] == "TOKEN_SEQUENCE_MATCHING" and slot["target_module"] == "java.base"
                    and slot["target_package"] == "java.util.regex", "token candidate receiving ownership drift")
            require(slot["public_owner"] is None and slot["public_signatures"] is None and slot["proof"] is None
                    and slot["status"] == "PENDING_DONOR_RIGHTS_OWNER_AND_CONTRACT_SELECTION"
                    and slot["receiving_sequence_advanced"] is False, "pending token slot fabricates an API or qualification")
    regex = next(family for family in fragment["families"] if family["id"] == "regex")
    token = regex.get("token_sequence_extension")
    require(isinstance(token, dict), "missing token sequence contract extension")
    expected_token_owners = ["java.util.regex.TokenPattern", "java.util.regex.TokenMatcher"]
    require(token["public_owners"] == expected_token_owners
            and [row["public_api"] for row in regex["new_candidates"]] == expected_token_owners,
            "token sequence public owner drift")
    require(token["aspect"] == "TOKEN_SEQUENCE_MATCHING"
            and token["target_module"] == "java.base" and token["target_package"] == "java.util.regex",
            "token sequence receiving ownership drift")
    require(token["status"] == CANDIDATE_STATUS and token["proof"] is None
            and token["runtime_qualified"] is False and token["performance_qualified"] is False
            and token["receiving_sequence_advanced"] is False,
            "token sequence inventory cannot claim runtime or performance qualification")
    require(token["engine_owner_sources"] == ["java.util.regex.Pattern", "java.util.regex.Matcher"]
            and token["existing_character_owners_unchanged"] is True
            and token["named_character_groups_are_token_aware"] is False,
            "character regex must remain distinct from token sequence matching")
    require(token["unit"] == "ONE_LIST_ELEMENT_NO_STRING_SERIALIZATION"
            and token["span_positions"] == "ABSOLUTE_HALF_OPEN_TOKEN_INDICES",
            "token sequence unit or span contract drift")
    require(not regex.get("pending_extension_slots"), "selected token owners cannot remain a fabricated pending slot")
    for candidate in regex["new_candidates"]:
        require(re.fullmatch(r"[0-9a-f]{64}", candidate.get("candidate_source_sha256", "")) is not None
                and bool(candidate.get("declaration_tokens")),
                "token candidate lacks exact source and declaration seals")
    exports = sum(len(row["unqualified_exports"]) for row in rows)
    require(index["exported_package_count"] == exports, "module exported-package count drift")
    return {"source_modules": len(rows), "unqualified_exported_packages": exports,
            "additional_representative_public_types": len(extra), "total_representative_public_types": len(apis),
            "additional_aspect_groups": len(aspects),
            "platform_specific_modules": [row["module"] for row in rows if row["source_platform"] != "share"],
            "non_module_source_roots": [row["path"] for row in nonmodules],
            "scope": "ALL_PINNED_SOURCE_MODULES_WITH_REPRESENTATIVE_API_OWNERS_NOT_ALL_TYPES_OR_METHODS",
            "runtime_qualified": False, "receiving_sequence_advanced": False}


def verify(repository: Path, overlay: Path | None, require_candidates: bool = False) -> dict:
    mapping = json.loads(load_candidate(repository, overlay, MAPPING))
    fragment = json.loads(load_candidate(repository, overlay, FRAGMENT))
    require(fragment["schema"] == 1 and fragment["mapping_authority"] == MAPPING, "unexpected mapping authority")
    require(fragment["canonical_policy"] == "m3/docs/m3jdk21-porting-invariant.md", "policy authority drift")
    require(fragment["target_repository"] == "hsoliwal/M3jdk21", "target repository drift")
    for flag in ("receiving_sequence_advanced", "family_completion", "repository_completion"):
        require(fragment[flag] is False, "source inventory cannot claim " + flag)
    require(fragment["no_companion_runtime"] is True, "utility receiver must use owning JDK modules")
    require(fragment["public_existing_jdk_names_unchanged"] is True, "existing JDK names must be preserved")
    families = fragment["families"]
    identifiers = [family["id"] for family in families]
    require(len(identifiers) == len(set(identifiers)) and set(identifiers) == REQUIRED_FAMILIES,
            "requested utility family coverage is missing, duplicated or unreviewedly extended")
    require(fragment["required_family_ids"] == identifiers, "family coverage list/order drift")
    api_rows = fragment["public_api_sources"]
    apis = {row["public_api"]: row for row in api_rows}
    require(len(apis) == len(api_rows), "duplicate public API identity")
    module_rows = {row["module"]: row for row in fragment["module_sources"]}
    require(len(module_rows) == len(fragment["module_sources"]), "duplicate module identity")
    evidence = api_rows + fragment["module_sources"] + fragment["reuse_sources"]
    baseline = git_sources(repository, fragment["target_baseline"], [row["path"] for row in evidence] + [MAPPING])
    for row in evidence:
        blob, body = baseline[row["path"]]
        require(row["git_blob"] == blob, "Git blob drift: " + row["path"])
        require(row["sha256"] == hashlib.sha256(body).hexdigest(), "SHA-256 drift: " + row["path"])
    previous_map = json.loads(baseline[MAPPING][1])
    require({k: v for k, v in mapping.items() if k != "family_name_mapping"}
            == {k: v for k, v in previous_map.items() if k != "family_name_mapping"},
            "existing naming-map values or receiving-plan states changed")
    prior_rows = previous_map["family_name_mapping"]
    require(mapping["family_name_mapping"][:len(prior_rows)] == prior_rows, "existing family mappings changed")
    links = [row for row in mapping["family_name_mapping"][len(prior_rows):] if row.get("mapping") == FRAGMENT]
    require(len(links) == 1, "coverage fragment must be linked exactly once by the existing naming map")
    require(links[0]["module_wide_mapping"] == fragment["module_wide_index"], "module source index is not linked from the existing naming map")
    require(links[0]["receiving_sequence_advanced"] is False and links[0]["family_completion"] is False,
            "coverage mapping must not promote a family")
    used_apis: set[str] = set()
    candidates: list[dict] = []
    for family in families:
        require(family["evidence_level"] == "BASELINE_SOURCE_INVENTORY_ONLY", "family evidence overstatement")
        require(family["family_completion"] is False and bool(family["remaining_scope"]), "missing honest family gap")
        require(bool(family["source_evidence"]), "family lacks source evidence")
        require(set(family["source_evidence"]).issubset(apis), "unknown family source reference")
        used_apis.update(family["source_evidence"])
        require(family["owning_modules"] == sorted({apis[api]["module"] for api in family["source_evidence"]}),
                "family/module mapping drift: " + family["id"])
        for candidate in family["new_candidates"]:
            require(candidate["status"] == CANDIDATE_STATUS and candidate["proof"] is None,
                    "candidate status falsely claims source/runtime admission")
            require(candidate["receiving_sequence_advanced"] is False, "candidate cannot promote a phase")
            require(candidate["target_module"] == "java.base", "unexpected receiving module")
            require(candidate["target_path"] == "src/java.base/share/classes/" + candidate["public_api"].replace(".", "/") + ".java",
                    "candidate owner/path naming mismatch")
            observed = candidate_source(repository, overlay, candidate)
            if require_candidates:
                require(observed["source_observed"], "candidate declaration not observed: " + candidate["public_api"])
            candidates.append(observed)
    internal_candidates = fragment["internal_receiving_candidates"]
    expected_internal = {
        "java.util.M3CollectionViews": "src/java.base/share/classes/java/util/M3CollectionViews.java",
        "java.util.M3CollectionMasks": "src/java.base/share/classes/java/util/M3CollectionMasks.java",
        "java.util.M3CollectionMasks.selected0": "src/java.base/share/native/libjava/M3CollectionMasks.c",
    }
    require(len(internal_candidates) == len(expected_internal)
            and {row["owner"]: row["target_path"] for row in internal_candidates} == expected_internal,
            "internal receiving owner coverage drift")
    internal_observations = []
    for candidate in internal_candidates:
        require(candidate["status"] == CANDIDATE_STATUS and candidate["proof"] is None
                and candidate["receiving_sequence_advanced"] is False,
                "internal candidate falsely claims source/runtime admission")
        require(candidate["target_module"] == "java.base", "internal receiving module drift")
        java_source = candidate["target_path"].endswith(".java")
        require(candidate["visibility"] == ("PACKAGE_PRIVATE" if java_source else "JNI_IMPLEMENTATION_ONLY"),
                "internal candidate became a public companion API")
        if not java_source:
            require(candidate["jni_symbol"] == "Java_java_util_M3CollectionMasks_selected0", "JNI owner drift")
        observed = internal_source(repository, overlay, candidate)
        if require_candidates:
            require(observed["source_observed"], "internal candidate declaration not observed: " + candidate["owner"])
        internal_observations.append(observed)
    require(used_apis == set(apis), "unused or unbound public API evidence")
    generated = 0
    for api, row in apis.items():
        require(row["visibility"] == "PUBLIC", "nonpublic source mislabeled as public API")
        require(row["module"] in module_rows, "missing module source")
        require(row["path"].startswith("src/" + row["module"] + "/share/classes/"), "API placed in wrong module")
        require(row["package"] == api.rsplit(".", 1)[0], "API package mismatch")
        text = baseline[row["path"]][1].decode("utf-8")
        if row["source_kind"] == "GENERATED_TYPE_TEMPLATE":
            generated += 1
            require(row["path"].endswith(".java.template") and row["generation_owner"] in baseline,
                    "generated API lacks template/generator source")
            require(row["evidence_level"] == "BASELINE_TEMPLATE_AND_MODULE_EXPORT", "template evidence overstatement")
            for key, value in row["template_substitutions"].items():
                require(key in text, "unbound template substitution")
                text = text.replace(key, value)
        else:
            require(row["source_kind"] == "JAVA_SOURCE" and row["evidence_level"] == "BASELINE_SOURCE_DECLARATION_AND_MODULE_EXPORT",
                    "unexpected source evidence kind")
            require(row["template_substitutions"] == {} and row["generation_owner"] is None, "non-template source has generator metadata")
        require(public_declaration(text, api), "public source declaration not found: " + api)
        require(re.search(r"(?m)^\s*package\s+" + re.escape(row["package"]) + r"\s*;", text) is not None,
                "package declaration not found: " + api)
        module_text = baseline[module_rows[row["module"]]["path"]][1].decode("utf-8")
        require(re.search(r"\bexports\s+" + re.escape(row["package"]) + r"\s*;", module_text) is not None,
                "API package is not publicly exported by its owning module: " + api)
    module_coverage = verify_module_index(repository, overlay, fragment, apis)
    module_index = json.loads(load_candidate(repository, overlay, fragment["module_wide_index"]))
    all_evidence_paths = set(baseline) | {row["path"] for row in module_index["modules"] + module_index["additional_public_api_sources"]}
    catalogue = load_candidate(repository, overlay, fragment["existing_catalogue"]).decode("utf-8")
    reader = csv.DictReader(catalogue.splitlines(), delimiter="\t")
    require(reader.fieldnames == CATALOGUE_HEADER, "existing catalogue schema changed")
    decisions = list(reader)
    require(bool(decisions) and all(all(row.get(key) for key in CATALOGUE_HEADER) for row in decisions), "incomplete donor decision")
    require(all(re.fullmatch(r"https://api\.github\.com/repos/[^/]+/[^/]+/git/blobs/[0-9a-f]{40}", row["source"]) for row in decisions),
            "donor catalogue source is not pinned to a Git blob")
    require(all("no runtime or benchmark qualification" in row["validation"] for row in decisions), "catalogue row overstates validation")
    return {"result": "BASELINE_SOURCE_COVERAGE_VERIFIED", "baseline": fragment["target_baseline"],
            "families": len(families), "public_api_sources": len(apis), "generated_api_templates": generated,
            "core_owning_modules": sorted(module_rows), "core_pinned_unique_source_paths": len(baseline),
            "owning_modules": sorted(row["module"] for row in module_index["modules"]),
            "pinned_unique_source_paths": len(all_evidence_paths),
            "catalogue_decisions": len(decisions), "candidate_source_observations": candidates,
            "internal_candidate_observations": internal_observations,
            "module_wide_coverage": module_coverage,
            "runtime_qualified": False, "performance_qualified": False, "receiving_sequence_advanced": False}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=Path("."))
    parser.add_argument("--candidate-root", type=Path)
    parser.add_argument("--require-candidate-sources", action="store_true",
                        help="also require candidate public/internal declaration tokens in the current source view; not runtime proof")
    args = parser.parse_args()
    try:
        result = verify(args.repository.resolve(), args.candidate_root.resolve() if args.candidate_root else None,
                        args.require_candidate_sources)
        print(json.dumps(result, indent=2, sort_keys=True))
        return 0
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as failure:
        print("utility source coverage refused: " + str(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
