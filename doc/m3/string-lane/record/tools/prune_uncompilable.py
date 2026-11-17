# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""prune_uncompilable.py <test.java> <skipped.tsv> -- <javac command...>

DONOR_TEST_PRUNE v1 (2026-11-17): a ported donor statement may call an overload that M3JDK does not have (a varargs
static form, a deprecated variant). Compiles the file with the given javac command, reads the errors (file:line:
error: message, BLLSE style), removes each whole statement that holds an error line, appends it to skipped.tsv with
reason COMPILE:<message>, and repeats until the file compiles (at most 20 rounds). A test method left without
statements is removed too. Exit 0 when the file compiles."""
import re
import subprocess
import sys

split = sys.argv.index("--")
java, skipped = sys.argv[1], sys.argv[2]
command = sys.argv[split + 1:]


def statements(lines):
    """(first, last) line index ranges of the statements inside test methods (8-space indent, ending with ';')."""
    out, start = [], None
    for i, line in enumerate(lines):
        if start is None and line.startswith("        ") and not line.startswith("         ") and line.strip():
            start = i
        if start is not None and line.rstrip().endswith(";"):
            out.append((start, i))
            start = None
        elif start is not None and not line.strip():
            start = None
    return out


for round_ in range(20):
    result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace")
    errors = {}
    name = java.replace("\\", "/").split("/")[-1]
    for line in (result.stdout + result.stderr).split("\n"):
        m = re.match(r"^(.*?):(\d+): error: (.*)$", line)
        if m and m.group(1).replace("\\", "/").endswith(name):
            errors.setdefault(int(m.group(2)) - 1, m.group(3).strip())
    if result.returncode == 0:
        print(f"compiles after {round_} pruning round(s)")
        sys.exit(0)
    if not errors:
        print("javac failed without errors in this file:\n" + (result.stdout + result.stderr)[:2000])
        sys.exit(1)
    lines = open(java, encoding="utf-8").read().split("\n")
    drop, rows = set(), []
    for first, last in statements(lines):
        hit = next((errors[i] for i in range(first, last + 1) if i in errors), None)
        if hit:
            drop.update(range(first, last + 1))
            rows.append(f"{name}\t-\tCOMPILE:{hit}\t{' '.join(l.strip() for l in lines[first:last + 1])[:200]}")
    if not drop:
        print("errors outside statements:", list(errors.items())[:5])
        sys.exit(1)
    kept = [l for i, l in enumerate(lines) if i not in drop]
    text = re.sub(r"\n    @Test\n    void \w+\(\) \{\n    \}\n", "\n", "\n".join(kept))
    open(java, "w", encoding="utf-8", newline="").write(text)
    with open(skipped, "a", encoding="utf-8", newline="") as w:
        w.write("\n".join(rows) + "\n")
    print(f"round {round_ + 1}: removed {len(rows)} statement(s)")
sys.exit(1)
