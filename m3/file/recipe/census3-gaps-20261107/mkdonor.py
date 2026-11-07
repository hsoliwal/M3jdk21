#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""mkdonor.py <out-dir>: writes census3/DonorFile.java from oracle/census3/OwnerFile.java and oracle/SIDE_MAP.tsv (SUB rows
applied in order; every owner_text must occur). Donor package names live only in the lineage TSV."""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
text = (HERE / "oracle" / "census3" / "OwnerFile.java").read_text(encoding="utf-8")
for line in (HERE / "oracle" / "SIDE_MAP.tsv").read_text(encoding="utf-8").splitlines():
    if line.startswith("#") or line.startswith("kind\t") or not line.strip():
        continue
    kind, owner, donor = line.split("\t")[:3]
    if kind != "SUB" or owner not in text:
        raise SystemExit("SIDE_MAP row does not apply: " + line)
    text = text.replace(owner, donor)
out = Path(sys.argv[1]) / "census3"
out.mkdir(parents=True, exist_ok=True)
(out / "DonorFile.java").write_text(text, encoding="utf-8", newline="\n")
print("DonorFile.java written to", out)
