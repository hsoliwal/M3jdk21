# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""skipped_reasons.py <surefire-reports dir> <out SKIPPED.tsv>

SUREFIRE_SKIPPED_REASONS v1 (2026-11-09) reads every TEST-*.xml of a surefire run and writes one row per skipped test
case: class, test, skip message (first line, tabs flattened). It prints the count per message, so a native-profile run
shows which native gates still skip and why (an unset property, a missing library, a host-only proof)."""
import glob
import os
import sys
import xml.etree.ElementTree as ET
from collections import Counter

reports, out = sys.argv[1:3]
rows = []
for path in sorted(glob.glob(os.path.join(reports, "TEST-*.xml"))):
    for case in ET.parse(path).getroot().iter("testcase"):
        skipped = case.find("skipped")
        if skipped is not None:
            message = (skipped.get("message") or "").splitlines()[0:1]
            rows.append((case.get("classname", ""), case.get("name", ""),
                         (message[0] if message else "").replace("\t", " ")))
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("class\ttest\tmessage\n")
    for row in rows:
        w.write("\t".join(row) + "\n")
print("SUREFIRE_SKIPPED", len(rows))
for message, count in Counter(r[2] for r in rows).most_common():
    print(f"{count}\t{message[:160]}")
