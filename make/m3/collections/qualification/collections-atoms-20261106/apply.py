#!/usr/bin/env python3
"""Recipe COLLECTIONS_ATOMS_APPLY v1 (CPU only): hash-pinned EDIT rules for two com.synexia collection atoms.

EDITS.tsv rows: file, before_sha256, after_sha256, rule, donor. A rule is the pair rules/<rule>.before and
rules/<rule>.after: an exact snippet that must occur exactly once in the preimage. A file already at after_sha256 is
reported ALREADY (changed=0, the fixed point); any other digest is a drifted preimage and is REFUSED (exit 2); a
produced postimage whose digest is not after_sha256 is REFUSED (exit 3). An after_sha256 of "-" mints: the digest
the rule would produce is printed and nothing is written (exit 4).

Usage: python -I apply.py [--root <repository root>] [--check]
"""
import hashlib
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def read(path):
    with open(path, "rb") as fh:
        return fh.read()


def rows(path):
    with open(path, encoding="utf-8") as fh:
        head = fh.readline().rstrip("\n").split("\t")
        for line in fh:
            if line.strip():
                yield dict(zip(head, line.rstrip("\n").split("\t")))


def main(argv):
    root = os.getcwd()
    check = False
    args = list(argv)
    while args:
        arg = args.pop(0)
        if arg == "--root":
            root = args.pop(0)
        elif arg == "--check":
            check = True
        else:
            raise SystemExit(f"unknown argument {arg}")
    changed = 0
    status = 0
    for row in rows(os.path.join(HERE, "EDITS.tsv")):
        path = os.path.join(root, row["file"])
        data = read(path)
        digest = sha256(data)
        if digest == row["after_sha256"]:
            print(f"ALREADY\t{row['file']}\t{digest}")
            continue
        if digest != row["before_sha256"]:
            print(f"REFUSED_DRIFT\t{row['file']}\texpected {row['before_sha256']} or {row['after_sha256']}, found {digest}")
            status = max(status, 2)
            continue
        before = read(os.path.join(HERE, "rules", row["rule"] + ".before"))
        after = read(os.path.join(HERE, "rules", row["rule"] + ".after"))
        if data.count(before) != 1:
            print(f"REFUSED_SNIPPET\t{row['file']}\trule {row['rule']} matches {data.count(before)} times")
            status = max(status, 2)
            continue
        out = data.replace(before, after)
        produced = sha256(out)
        if row["after_sha256"] == "-":
            print(f"MINT\t{row['file']}\t{produced}")
            status = max(status, 4)
            continue
        if produced != row["after_sha256"]:
            print(f"REFUSED_POSTIMAGE\t{row['file']}\tproduced {produced}, pinned {row['after_sha256']}")
            status = max(status, 3)
            continue
        if not check:
            with open(path, "wb") as fh:
                fh.write(out)
        changed += 1
        print(f"{'WOULD_APPLY' if check else 'APPLIED'}\t{row['file']}\t{digest} -> {produced}\t{row['rule']}")
    print(f"changed={changed} status={status}")
    return status


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
