#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
from pathlib import Path

PRIMARY = "b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e"
FOLLOWUP = "3a109f49feb19f313632be6a2aa24ba7d9b7269b"

def rows(path: Path):
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))

def main() -> int:
    root = Path(__file__).resolve().parents[4]
    adaptation = rows(root / "m3/backports/recipes/jdk-8359706/adaptation.tsv")
    if len(adaptation) != 8:
        raise SystemExit(f"expected 8 adaptation rows, found {len(adaptation)}")
    for row in adaptation:
        if row["upstream_commit"] != PRIMARY:
            raise SystemExit(f"donor drift: {row['target_path']}")
        actual = hashlib.sha256((root / row["target_path"]).read_bytes()).hexdigest()
        if actual != row["target_sha256"]:
            raise SystemExit(f"target drift: {row['target_path']}: {actual}")

    seed = next(
        (r for r in rows(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
         if r["jbs"] == "JDK-8359706"), None)
    if seed is None or seed["upstream_commit"] != PRIMARY:
        raise SystemExit("JDK-8359706 seed lineage missing")
    if seed["disposition"] != "candidate-adapted":
        raise SystemExit("JDK-8359706 must remain candidate-adapted")

    linux = (root / "src/hotspot/os/linux/os_linux.cpp").read_text(encoding="utf-8")
    bsd = (root / "src/hotspot/os/bsd/os_bsd.cpp").read_text(encoding="utf-8")
    os_hpp = (root / "src/hotspot/share/runtime/os.hpp").read_text(encoding="utf-8")
    vm_error = (root / "src/hotspot/share/utilities/vmError.cpp").read_text(encoding="utf-8")
    test = (root / "test/jdk/sun/tools/jcmd/TestJcmdSanity.java").read_text(encoding="utf-8")

    if 'opendir("/proc/self/fd")' not in linux:
        raise SystemExit("Linux descriptor counter missing")
    if "#include <libproc.h>" not in bsd or "precond(buflen >= sizeof(struct proc_fdinfo))" not in bsd:
        raise SystemExit(f"macOS donor/follow-up contract missing: {FOLLOWUP}")
    if "static void print_open_file_descriptors(outputStream* st);" not in os_hpp:
        raise SystemExit("shared descriptor diagnostic contract missing")
    if vm_error.count("print_open_file_descriptors") < 2:
        raise SystemExit("VM.info/hs_err descriptor output join missing")
    if "8359706 8380236" not in test or "Open File Descriptors:" not in test:
        raise SystemExit("focused jcmd regression proof missing")
    print("PASS: JDK-8359706 + JDK-8380236 current-tree recovery verifier")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
