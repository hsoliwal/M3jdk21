#!/usr/bin/env python3
"""Recipe SUREFIRE_FAILING_CLASSES v1 (CPU only): parse surefire XML reports into FAILING_CLASSES.tsv and EXCLUDED.tsv.

Usage: failing_classes.py <surefire-reports dir> <FAILING_CLASSES.tsv> [<EXCLUDED.tsv>]
Kinds: JNI = UnsatisfiedLinkError or a 'JNI library' message (m3_arrays native library absent);
       OTHER = anything else (never excluded; listed as an open item with its first message).
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET


def kind_of(messages, types):
    text = " ".join(messages + types)
    if "UnsatisfiedLinkError" in text or "JNI library" in text:
        return "JNI"
    return "OTHER"


def head(text):
    return (text or "").replace("\t", " ").replace("\r", " ").replace("\n", " ")[:160]


def rows_of(reports):
    rows = []
    for path in sorted(glob.glob(os.path.join(reports, "TEST-*.xml"))):
        root = ET.parse(path).getroot()
        tests, fails, errs, skip = (int(root.get(k)) for k in ("tests", "failures", "errors", "skipped"))
        if fails + errs == 0:
            continue
        msgs, types = [], []
        for case in root.iter("testcase"):
            for tag in ("failure", "error"):
                for node in case.findall(tag):
                    text = node.get("message") or (node.text or "").strip().splitlines()[0] if (node.get("message") or node.text) else ""
                    msgs.append(head(text))
                    types.append(node.get("type") or "")
        rows.append((root.get("name"), tests, fails, errs, skip, kind_of(msgs, types), types[0] if types else "", msgs[0] if msgs else ""))
    return rows


def main(reports, failing_out, excluded_out=None):
    rows = rows_of(reports)
    with open(failing_out, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("class\ttests\tfailures\terrors\tskipped\tkind\tfirst_type\tfirst_message_head\n")
        fh.writelines("\t".join(str(x) for x in r) + "\n" for r in rows)
    if excluded_out:
        with open(excluded_out, "w", encoding="utf-8", newline="\n") as fh:
            fh.write("class\treason\tfailures\terrors\tfirst_message_head\n")
            fh.writelines(f"{r[0]}\tJNI: m3_arrays native library absent (m3.arrays.native.path unset); re-included by -Pm3-collections-native\t{r[2]}\t{r[3]}\t{r[7]}\n" for r in rows if r[5] == "JNI")
    kinds = {k: sum(1 for r in rows if r[5] == k) for k in ("JNI", "OTHER")}
    print(f"failing classes: {len(rows)}; {kinds}; tests={sum(r[1] for r in rows)} failures={sum(r[2] for r in rows)} errors={sum(r[3] for r in rows)}")
    for r in rows:
        if r[5] == "OTHER":
            print(f"OTHER\t{r[0]}\t{r[7]}")


if __name__ == "__main__":
    main(*sys.argv[1:4])
