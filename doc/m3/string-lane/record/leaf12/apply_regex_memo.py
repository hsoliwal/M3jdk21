# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""apply_regex_memo.py <String.java in> <regex_memo.txt> <String.java out>

REGEX_MEMO_RECIPE v1 (2026-11-16). The same three edits for the fork String.java and for the stock one, so the proof
can run the edited stock class:
  1. `Pattern.matches(regex, this)`  ->  `compiledRegex(regex).asMatchPredicate().test(this)`
  2. every `Pattern.compile(regex)` in code  ->  `compiledRegex(regex)`
  3. the memo text is inserted before the javadoc of matches(String)
It prints the counts and stops when an edit finds nothing, or when the result is not a fixed point of the recipe."""
import sys

source, memo, target = sys.argv[1:4]
text = open(source, encoding="utf-8", newline="").read().replace("\r", "")
piece = open(memo, encoding="utf-8", newline="").read().replace("\r", "")
if "RegexMemo" in text:
    sys.exit("ALREADY_APPLIED")
matches = text.count("Pattern.matches(regex, this)")
lines = text.split("\n")
compiles = sum(1 for line in lines if "Pattern.compile(regex)" in line and not line.lstrip().startswith(("*", "//")))
if matches != 1 or compiles < 1:
    sys.exit(f"UNEXPECTED matches={matches} compiles={compiles}")
lines = [line if line.lstrip().startswith(("*", "//")) else line.replace("Pattern.compile(regex)", "compiledRegex(regex)")
         for line in lines]
text = "\n".join(lines).replace("Pattern.matches(regex, this)", "compiledRegex(regex).asMatchPredicate().test(this)")
lines = text.split("\n")
anchor = [i for i, line in enumerate(lines) if line == "    public boolean matches(String regex) {"]
if len(anchor) != 1:
    sys.exit(f"ANCHOR {len(anchor)}")
start = max(i for i in range(anchor[0]) if lines[i] == "    /**")
lines[start:start] = piece.rstrip("\n").split("\n") + [""]
open(target, "w", encoding="utf-8", newline="").write("\n".join(lines))
print(f"applied\tmatches_sites={matches}\tcompile_sites={compiles}\tinserted_lines={len(piece.rstrip(chr(10)).split(chr(10))) + 1}")
