#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exercise the exact product-owned donation verifier's replacement+additive overlay."""
import importlib.util
from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory

ROOT = Path(__file__).resolve().parents[4]
VERIFIER = ROOT / "m3/collections/verify_donation.py"
OLD = ROOT / "m3/collections/recipe/filtered-sorted-views-20261017/manifest.tsv"
NEW = ROOT / "m3/collections/recipe/stored-representative-20261017/manifest.tsv"

def refuse(call, why):
    try:
        call()
    except SystemExit:
        return
    raise AssertionError("M3_REP_RECEIPT_FAIL accepted " + why)

def main():
    spec = importlib.util.spec_from_file_location("m3_collections_donation", VERIFIER)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)

    if sha256(NEW.read_bytes()).hexdigest() != (
            "a62827ffafc18bd6d710d7898741d2114a0068b8ac9ee63aac02e55038e1f293"):
        raise AssertionError("M3_REP_RECEIPT_FAIL manifest sha")

    original = [row.split("\t") for row in OLD.read_text().splitlines() if row.strip()]
    expected_old = [(path, after) for path, before, after, template in original]
    rows = module.include_stored_representative_successor(expected_old)
    if len(rows) != 8:
        raise AssertionError("M3_REP_RECEIPT_FAIL expected 6 existing+2 tests")
    prior = {p: digest for p, digest in expected_old}
    current = {p: digest for p, digest in rows}
    if len(current) != len(rows):
        raise AssertionError("M3_REP_RECEIPT_FAIL duplicate source path")
    for path, before, after, template in [
            line.split("\t") for line in NEW.read_text().splitlines() if line.strip()]:
        if before != "ABSENT" and prior.get(path) != before:
            raise AssertionError("M3_REP_RECEIPT_FAIL no exact preimage")
        if current.get(path) != after:
            raise AssertionError("M3_REP_RECEIPT_FAIL wrong postimage " + path)
        source = ROOT / path
        if not source.is_file() or sha256(source.read_bytes()).hexdigest() != after:
            raise AssertionError("M3_REP_RECEIPT_FAIL installed source drift " + path)
    for path, before in expected_old:
        if path not in {p for p, old, new, template in (
                row.split("\t") for row in NEW.read_text().splitlines())}:
            if current.get(path) != before:
                raise AssertionError("M3_REP_RECEIPT_FAIL historical row modified " + path)

    with TemporaryDirectory(prefix="m3-collection-receipt-") as tmp:
        tampered = Path(tmp) / "tampered.tsv"
        tampered.write_bytes(NEW.read_bytes() + b"# hostile\n")
        refuse(lambda: module.include_stored_representative_successor(
                expected_old, receipt=tampered), "tamper")
        tampered.write_bytes(NEW.read_bytes())
        changed = list(expected_old)
        changed[0] = (changed[0][0], "0" * 64)
        refuse(lambda: module.include_stored_representative_successor(
                changed, receipt=tampered), "drift")
        new_line = next(line for line in NEW.read_text().splitlines()
                        if "\tABSENT\t" in line)
        occupied = [*expected_old, (new_line.split("\t")[0], "f"*64)]
        refuse(lambda: module.include_stored_representative_successor(
                occupied, receipt=tampered), "occupied test")
        tampered.unlink()
        tampered.symlink_to(NEW)
        refuse(lambda: module.include_stored_representative_successor(
                expected_old, receipt=tampered), "symlink")
    print("M3_STORED_REP_RECEIVER_RECEIPT_PASS original=6 changed=2 added=2"
          " unknown_source_refused=true historical_receipts_preserved=true")

if __name__ == "__main__":
    main()
