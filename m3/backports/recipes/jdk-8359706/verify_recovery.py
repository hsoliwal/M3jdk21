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
    seen = set()
    for row in adaptation:
        target = row["target_path"]
        if target in seen:
            raise SystemExit(f"duplicate target: {target}")
        seen.add(target)
        if row["primary_commit"] != PRIMARY:
            raise SystemExit(f"primary donor drift: {target}")
        path = root / target
        if not path.is_file():
            raise SystemExit(f"missing target: {target}")
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != row["target_sha256"]:
            raise SystemExit(f"target drift: {target}: {actual}")
    seeds = rows(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    primary = next((r for r in seeds if r["jbs"] == "JDK-8359706"), None)
    followup = next((r for r in seeds if r["jbs"] == "JDK-8380236"), None)
    if primary is None or primary["upstream_commit"] != PRIMARY:
        raise SystemExit("JDK-8359706 seed lineage missing")
    if followup is None or followup["upstream_commit"] != FOLLOWUP:
        raise SystemExit("JDK-8380236 dependency lineage missing")
    linux = (root / "src/hotspot/os/linux/os_linux.cpp").read_text(encoding="utf-8")
    bsd = (root / "src/hotspot/os/bsd/os_bsd.cpp").read_text(encoding="utf-8")
    vm_error = (root / "src/hotspot/share/utilities/vmError.cpp").read_text(encoding="utf-8")
    test = (root / "test/jdk/sun/tools/jcmd/TestJcmdSanity.java").read_text(encoding="utf-8")
    if 'opendir("/proc/self/fd")' not in linux:
        raise SystemExit("Linux descriptor enumeration missing")
    if "PROC_PIDLISTFDS" not in bsd or "precond(buflen >= sizeof(struct proc_fdinfo))" not in bsd:
        raise SystemExit("macOS descriptor implementation/follow-up missing")
    if "os::print_open_file_descriptors(st)" not in vm_error:
        raise SystemExit("VM.info/fatal-error integration missing")
    if "Open File Descriptors:" not in test:
        raise SystemExit("focused jcmd assertion missing")
    print("PASS: JDK-8359706 dependency-closed recovery verifier")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
