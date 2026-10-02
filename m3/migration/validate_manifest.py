#!/usr/bin/env python3
"""Dependency-free fail-closed validator for the MIndex -> M3 migration manifest."""
from __future__ import annotations
import json
import re
import sys
from pathlib import Path

HEX40 = re.compile(r"^[0-9a-f]{40}$")
HEX64 = re.compile(r"^[0-9a-f]{64}$")
STATUS = {"implemented-tested","implemented-unverified","partial","proposed","blocked","excluded"}
KINDS = {"direct-port","rename","adapter","consolidation","specialization","dependency-reuse","pending","excluded"}
REQUIRED = {
    "id","capability","sources","destinations","mappingKind","canonicalOwner",
    "identityRules","contractDifferences","compatibilityObligations","formats",
    "bootstrapConstraints","dependencies","downstreamConsumers","materializationBoundaries",
    "cachedFactOwnership","recipe","tests","licenseProvenance","lastSynchronizedSourceRevision",
    "lastSynchronizedTargetRevision","targetAdaptations","conflicts","rationale","portDirection","status"
}
ENDPOINT = {"repository","commit","module","path","symbol","signature","contentHash"}

def add(errors, where, message):
    errors.append(f"{where}: {message}")

def check_hash(errors, where, value):
    if value is None:
        return
    if not isinstance(value, dict) or not {"algorithm","value"} <= value.keys():
        add(errors, where, "contentHash requires algorithm and value")
        return
    algorithm, digest = value["algorithm"], value["value"]
    if algorithm == "git-blob-sha1":
        if not isinstance(digest, str) or not HEX40.fullmatch(digest):
            add(errors, where, "git-blob-sha1 must be 40 lowercase hex characters")
    elif algorithm == "sha256":
        if not isinstance(digest, str) or not HEX64.fullmatch(digest):
            add(errors, where, "sha256 must be 64 lowercase hex characters")
    else:
        add(errors, where, f"unsupported hash algorithm {algorithm!r}")

def check_endpoint(errors, where, endpoint, source):
    if not isinstance(endpoint, dict):
        add(errors, where, "endpoint must be an object")
        return
    missing = ENDPOINT - endpoint.keys()
    if missing:
        add(errors, where, f"missing fields {sorted(missing)}")
        return
    commit = endpoint["commit"]
    if commit is not None and (not isinstance(commit, str) or not HEX40.fullmatch(commit)):
        add(errors, where + ".commit", "must be null or 40 lowercase hex characters")
    if source and commit is None:
        add(errors, where + ".commit", "source commit may not be null")
    if source and endpoint["contentHash"] is None:
        add(errors, where + ".contentHash", "source content hash is mandatory")
    check_hash(errors, where + ".contentHash", endpoint["contentHash"])

def validate_document(document):
    errors = []
    if not isinstance(document, dict):
        return ["root: manifest must be an object"]
    if document.get("schemaVersion") != 1:
        add(errors, "root", "schemaVersion must be 1")
    baselines = document.get("baselines")
    if not isinstance(baselines, dict):
        add(errors, "baselines", "must be an object")
    else:
        for name in ("source","targetMaster","targetRuntimeCandidate"):
            item = baselines.get(name)
            if not isinstance(item, dict):
                add(errors, f"baselines.{name}", "missing baseline")
            elif not HEX40.fullmatch(str(item.get("commit",""))):
                add(errors, f"baselines.{name}.commit", "must be 40 lowercase hex characters")
    mappings = document.get("mappings")
    if not isinstance(mappings, list):
        add(errors, "mappings", "must be an array")
        return errors
    ids = set()
    for i, mapping in enumerate(mappings):
        where = f"mappings[{i}]"
        if not isinstance(mapping, dict):
            add(errors, where, "must be an object")
            continue
        missing = REQUIRED - mapping.keys()
        if missing:
            add(errors, where, f"missing fields {sorted(missing)}")
            continue
        mapping_id = mapping["id"]
        if not isinstance(mapping_id, str) or not mapping_id.startswith("M3-"):
            add(errors, where + ".id", "must start with M3-")
        elif mapping_id in ids:
            add(errors, where + ".id", f"duplicate mapping ID {mapping_id}")
        else:
            ids.add(mapping_id)
        if mapping["status"] not in STATUS:
            add(errors, where + ".status", "unknown status")
        if mapping["mappingKind"] not in KINDS:
            add(errors, where + ".mappingKind", "unknown mapping kind")
        sources = mapping["sources"]
        destinations = mapping["destinations"]
        if not isinstance(sources, list) or not sources:
            add(errors, where + ".sources", "must contain at least one source")
        else:
            for j, endpoint in enumerate(sources):
                check_endpoint(errors, f"{where}.sources[{j}]", endpoint, True)
        if not isinstance(destinations, list):
            add(errors, where + ".destinations", "must be an array")
            destinations = []
        else:
            for j, endpoint in enumerate(destinations):
                check_endpoint(errors, f"{where}.destinations[{j}]", endpoint, False)
        if mapping["status"] in {"implemented-tested","implemented-unverified","partial"} and not destinations:
            add(errors, where, f"status {mapping['status']} requires at least one destination")
        source_revision = mapping["lastSynchronizedSourceRevision"]
        if not isinstance(source_revision, str) or not HEX40.fullmatch(source_revision):
            add(errors, where + ".lastSynchronizedSourceRevision", "must be 40 lowercase hex characters")
        target_revision = mapping["lastSynchronizedTargetRevision"]
        if target_revision is not None and (not isinstance(target_revision, str) or not HEX40.fullmatch(target_revision)):
            add(errors, where + ".lastSynchronizedTargetRevision", "must be null or 40 lowercase hex characters")
        tests = mapping["tests"]
        if mapping["status"] == "implemented-tested":
            if not any(isinstance(t,dict) and t.get("status") == "passed" and t.get("candidate") and t.get("evidence") for t in tests):
                add(errors, where, "implemented-tested requires passing exact-candidate evidence")
        recipe = mapping["recipe"]
        if not isinstance(recipe, dict) or not {"id","version","preconditions","beforeHash","afterHash","rollback"} <= recipe.keys():
            add(errors, where + ".recipe", "recipe record is incomplete")
        if (mapping["mappingKind"] == "excluded" or mapping["status"] == "excluded") and not mapping["rationale"]:
            add(errors, where, "excluded mapping requires rationale")
    return errors

def main(argv):
    path = Path(argv[1]) if len(argv) > 1 else Path(__file__).with_name("manifest.json")
    document = json.loads(path.read_text(encoding="utf-8"))
    errors = validate_document(document)
    if errors:
        for error in errors:
            print("ERROR", error, file=sys.stderr)
        return 1
    print(f"M3_MIGRATION_MANIFEST_VALID mappings={len(document['mappings'])}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
