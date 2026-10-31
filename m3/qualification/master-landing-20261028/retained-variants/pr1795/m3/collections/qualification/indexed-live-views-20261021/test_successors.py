#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only regression of the original M3JDK21 historical source-successor gate.

Only temporary copies are mutated. Historical preimages/templates are still
checked before any exact, source-qualified successor can be admitted.
"""
from contextlib import redirect_stdout
import hashlib
import importlib.util
import io
from pathlib import Path
import shutil
import tempfile

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
VERIFY = HERE / "verify_recipe.py"


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def inventory(root: Path) -> dict[str, str]:
    return {str(path.relative_to(root)): digest(path.read_bytes())
            for path in sorted(root.rglob("*")) if path.is_file()}


def copied_gate(root: Path):
    spec = importlib.util.spec_from_file_location("m3_receiver_verifier", VERIFY)
    if spec is None or spec.loader is None:
        raise RuntimeError("cannot inspect original M3JDK21 verifier")
    gate = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(gate)
    origin_crate, origin_pre, origin_ledger = gate.CRATE, gate.PRE, gate.SUCCESSIONS
    crate = root / origin_crate.relative_to(REPO)
    pre = root / origin_pre.relative_to(REPO)
    ledger = root / origin_ledger.relative_to(REPO)
    shutil.copytree(origin_crate, crate)
    shutil.copytree(origin_pre, pre)
    ledger.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(origin_ledger, ledger)
    for line in (crate / "manifest.tsv").read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        path = line.split("\t")[0]
        source = REPO / path
        target = root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    gate.ROOT, gate.CRATE, gate.PRE, gate.SUCCESSIONS = root, crate, pre, ledger
    return gate


def run(gate) -> None:
    with redirect_stdout(io.StringIO()):
        gate.main()


def refuses(gate, label: str, path: Path, changed: bytes) -> None:
    original = path.read_bytes()
    if original == changed:
        raise AssertionError("fault did not change " + label)
    path.write_bytes(changed)
    try:
        try:
            run(gate)
        except (RuntimeError, OSError, ValueError):
            pass
        else:
            raise AssertionError("unqualified input accepted: " + label)
    finally:
        path.write_bytes(original)


def main() -> None:
    with tempfile.TemporaryDirectory(prefix="m3-successor-gate-") as work:
        root = Path(work)
        gate = copied_gate(root)
        original_inventory = inventory(root)
        run(gate)
        if inventory(root) != original_inventory:
            raise AssertionError("read-only positive gate wrote a source file")
        ledger = gate.SUCCESSIONS
        rows = [line for line in ledger.read_text(encoding="utf-8").splitlines()
                if line and not line.startswith("#")]
        if len(rows) != 3:
            raise AssertionError("expected three qualified source successors")
        refusals = 0
        current = root / "m3/collections/src/main/java/com/m3/collections/M3IndexedLiveView.java"
        refuses(gate, "source drift", current, current.read_bytes() + b"\n// source drift\n")
        refusals += 1

        prior = gate.PRE / "M3IndexedLiveView.java.txt"
        refuses(gate, "original preimage drift", prior, prior.read_bytes() + b"\n")
        refusals += 1

        templates = (gate.CRATE / "manifest.tsv").read_text(encoding="utf-8")
        template = gate.CRATE / next(row.split("\t")[3] for row in templates.splitlines()
                                     if "M3IndexedLiveView.java" in row)
        refuses(gate, "historical template drift", template, template.read_bytes() + b"\n")
        refusals += 1

        ledger_text = ledger.read_text(encoding="utf-8")
        refuses(gate, "duplicate successor", ledger,
                (ledger_text + rows[0] + "\n").encode("utf-8"))
        refusals += 1

        refuses(gate, "unknown successor target", ledger,
                ledger_text.replace("M3IndexedLiveView.java",
                                    "M3UnreviewedLiveView.java", 1).encode("utf-8"))
        refusals += 1

        rejects_original = rows[0].split("\t")[1]
        refuses(gate, "wrong original recipe digest", ledger,
                ledger_text.replace(rejects_original, "0" * 64, 1).encode("utf-8"))
        refusals += 1
        if inventory(root) != original_inventory:
            raise AssertionError("fault injection modified original fixture state")
        print(f"M3_INDEXED_LIVE_SUCCESSORS_PASS positive=1 refusal={refusals}")


if __name__ == "__main__":
    main()
