# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""junit_summary.py <TEST-junit-jupiter.xml> <out JUNIT_SUMMARY.tsv> : JUNIT XML PROJECTION v1 (CPU).
One row per test class: dynamic tests, failures, errors, skipped, seconds; exit 1 when any test failed."""
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict


def main():
    xml_path, out = sys.argv[1:3]
    classes = defaultdict(lambda: [0, 0, 0, 0, 0.0])
    for case in ET.parse(xml_path).getroot().iter("testcase"):
        row = classes[case.get("classname")]
        row[0] += 1
        row[1] += len(case.findall("failure"))
        row[2] += len(case.findall("error"))
        row[3] += len(case.findall("skipped"))
        row[4] += float(case.get("time", "0"))
    with open(out, "w", encoding="utf-8", newline="\n") as w:
        w.write("class\ttests\tfailures\terrors\tskipped\tseconds\n")
        for name in sorted(classes):
            t, f, e, s, sec = classes[name]
            w.write("%s\t%d\t%d\t%d\t%d\t%.3f\n" % (name, t, f, e, s, sec))
        total = [sum(r[i] for r in classes.values()) for i in range(4)]
        w.write("TOTAL\t%d\t%d\t%d\t%d\t%.3f\n" % (*total, sum(r[4] for r in classes.values())))
    print("junit_classes=%d tests=%d failures=%d errors=%d skipped=%d" % (len(classes), *total))
    sys.exit(1 if total[1] or total[2] else 0)


main()
