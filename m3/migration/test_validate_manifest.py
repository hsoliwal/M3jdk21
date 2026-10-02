#!/usr/bin/env python3
from __future__ import annotations
import copy
import json
from pathlib import Path
from validate_manifest import validate_document

BASE = json.loads(Path(__file__).with_name("manifest.json").read_text(encoding="utf-8"))

def require_failure(document, needle):
    errors = validate_document(document)
    if not any(needle in error for error in errors):
        raise AssertionError(f"expected {needle!r}; got {errors}")

errors = validate_document(BASE)
if errors:
    raise AssertionError(errors)

duplicate = copy.deepcopy(BASE)
duplicate["mappings"].append(copy.deepcopy(duplicate["mappings"][0]))
require_failure(duplicate, "duplicate mapping ID")

bad_hash = copy.deepcopy(BASE)
bad_hash["mappings"][0]["sources"][0]["contentHash"]["value"] = "bad"
require_failure(bad_hash, "git-blob-sha1")

bad_commit = copy.deepcopy(BASE)
bad_commit["baselines"]["source"]["commit"] = "HEAD"
require_failure(bad_commit, "40 lowercase hex")

false_complete = copy.deepcopy(BASE)
false_complete["mappings"][0]["status"] = "implemented-tested"
false_complete["mappings"][0]["tests"] = [{"id":"x","status":"not-run","candidate":None,"evidence":""}]
require_failure(false_complete, "passing exact-candidate evidence")

no_destination = copy.deepcopy(BASE)
no_destination["mappings"][0]["destinations"] = []
require_failure(no_destination, "requires at least one destination")

print("M3_MIGRATION_VALIDATOR_TEST_PASS cases=6")
