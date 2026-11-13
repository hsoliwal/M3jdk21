"""errdiff.py <live.log> <rest.log> : javac error sets (file basename + message + following source line) present only in
the restored log. Line numbers are dropped so moved lines compare equal."""
import re
import sys
from collections import Counter


def errors(path):
    lines = open(path, encoding="utf-8", errors="replace").read().splitlines()
    out = Counter()
    for i, l in enumerate(lines):
        m = re.match(r"^(.*?[\\/])?([^\\/:]+\.java):\d+: error: (.*)$", l)
        if m:
            src = lines[i + 1].strip() if i + 1 < len(lines) else ""
            out[(m.group(2), m.group(3), src)] += 1
    return out


live, rest = errors(sys.argv[1]), errors(sys.argv[2])
for k, v in (rest - live).items():
    print(f"NEW x{v}\t{k[0]}\t{k[1]}\t{k[2][:120]}")
for k, v in (live - rest).items():
    print(f"GONE x{v}\t{k[0]}\t{k[1]}\t{k[2][:120]}")
