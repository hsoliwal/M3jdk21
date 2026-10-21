#!/usr/bin/env python3
"""Exercise the unchanged receiver verifier on exact isolated source fixtures."""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import shutil

from run_text_owner_gate import WORK, digest, write_json


def main() -> None:
    base = WORK / "source-lock-correction"
    candidate = base / "candidate-receiver"
    fixture = base / "source-lock-refusal-fixture"
    fixture.mkdir(exist_ok=False)
    driver_relative = Path("m3/tooling/module-packs/nlp-search/current_forks.py")
    lock_relative = driver_relative.with_name("current-source-lock.json")
    lock = json.loads((candidate / lock_relative).read_text())
    paths = {path for entry in lock["modules"].values() for path in entry["files"]}
    paths.update([str(driver_relative), str(lock_relative)])
    for relative in sorted(paths):
        target = fixture / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(candidate / relative, target)
    spec = importlib.util.spec_from_file_location("nlp_exact_source_lock_guard", fixture / driver_relative)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    if module.verify_receiver(fixture) != lock:
        raise AssertionError("Exact candidate was not accepted")
    cases = [{"case": "exact_candidate", "expected": "ACCEPT", "observed": "ACCEPT"}]
    owner = "m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java"
    path = fixture / owner
    original = path.read_bytes()
    def refused(name, expected):
        try:
            module.verify_receiver(fixture)
        except ValueError as error:
            if str(error) != expected:
                raise AssertionError("Wrong refusal: " + str(error)) from error
            cases.append({"case": name, "expected": expected, "observed": str(error)})
        else:
            raise AssertionError("Mutation was admitted: " + name)
    path.write_bytes(original + b"\n")
    refused("known_owner_byte_drift", "receiver source drift: " + owner)
    path.write_bytes(original)
    path.unlink()
    refused("known_owner_missing", "receiver source drift: " + owner)
    path.write_bytes(original)
    unknown = fixture / "m3/core/src/com/m3/text/UnlistedOwner.java"
    unknown.write_text("package com.m3.text; final class UnlistedOwner { }\n")
    refused("unlisted_java_owner", "receiver source coverage drift: com.m3.text")
    unknown.unlink()
    if module.verify_receiver(fixture) != lock:
        raise AssertionError("Fixture was not restored exactly")
    for relative in sorted(paths):
        if (fixture / relative).read_bytes() != (candidate / relative).read_bytes():
            raise AssertionError("Fixture drift after restoration: " + relative)
    result = {"schema": "M3_NLP_SOURCE_LOCK_GUARD_PROOF_1", "state": "PASS", "cases": cases,
              "locked_file_count": sum(len(entry["files"]) for entry in lock["modules"].values()),
              "source_lock_sha256": digest((candidate / lock_relative).read_bytes()),
              "unchanged_production_verifier_sha256": digest((candidate / driver_relative).read_bytes()),
              "fixture_restored_exactly": True, "source_coverage_guard_removed": False,
              "helper_sha256": digest(Path(__file__).read_bytes())}
    write_json(base / "SOURCE_LOCK_GUARDS.json", result)
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
