# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""mutants.py <draft-dir> <mutant-root> <run_proof.sh> <work-dir> <donor-classpath>

ESCAPE_CODE_MUTANTS v1 (2026-11-17). Mutation check of the proof: each mutant is one literal edit of
escape_code_family.txt (no regular expressions); for each, a leaf folder <mutant-root>/<id> gets the ten base pieces,
the mutated family text and a head with it, and run_proof.sh runs in the mode that can see the edit (PROOF_MODES,
PROOF_MAINS). A mutant is KILLED when that run prints RESULT and fails; ERROR when it prints no RESULT. MUTANTS_BASH
names the bash to run (Git Bash; plain 'bash' on Windows may be WSL's). Prints one line per mutant; the folders are removed after.
"""
import os
import shutil
import subprocess
import sys

draft, root, runner, work, donor = sys.argv[1:6]
BASH = os.environ.get("MUTANTS_BASH", "bash")
PIECES = ("difference_base.txt", "search_family.txt", "edge_family.txt", "class_family.txt", "compare_family.txt",
          "slice_family.txt", "build_family.txt", "split_family.txt", "replace_family.txt", "case_family.txt")
MUTANTS = [
    ("lookahead-1", "flat", "EscapeCodeFamilyProof.class",
     "private static final int ESC_LOOKAHEAD = 2;", "private static final int ESC_LOOKAHEAD = 1;"),
    ("json-no-delete", "flat", "EscapeCodeFamilyProof.class",
     "            CONTROLS | DELETE | NON_ASCII | SHORT_B | SHORT_TNFR,\n            0,",
     "            CONTROLS | NON_ASCII | SHORT_B | SHORT_TNFR,\n            0,"),
    ("ident-dash-dash", "flat", "EscapeCodeFamilyProof.class",
     "return next < 0 || next == '-' || next >= '0'", "return next < 0 || next >= '0'"),
    ("properties-three-digits", "flat", "EscapeCodeFamilyProof.class",
     "if (asciiHexRunEnd(at + 2, Math.min(length, at + 6)) < at + 6) {",
     "if (asciiHexRunEnd(at + 2, Math.min(length, at + 5)) < at + 5) {"),
    ("octal-any-first", "flat", "EscapeCodeFamilyProof.class",
     "if (end < length && next <= '3' && charAt(end)", "if (end < length && next <= '7' && charAt(end)"),
    ("facts-gate-ident", "m3-facts", "EscapeCodeFamilyProof.class",
     "if (wordUnitsOnly() && (rule != ESC_CSS_IDENTIFIER || isEmpty()",
     "if (wordUnitsOnly() && (rule == ESC_CSS_IDENTIFIER || isEmpty()"),
    ("ftl-x-five-digits", "flat", "EscapeSyntaxProof.class",
     "int end = asciiHexRunEnd(at + 2, Math.min(length, at + 6));\n        if (end == at + 2) {",
     "int end = asciiHexRunEnd(at + 2, Math.min(length, at + 7));\n        if (end == at + 2) {"),
    ("st-no-tab", "flat", "EscapeSyntaxProof.class",
     ": next == 't' ? '\\t' : next", ": next"),
]
read = lambda p: open(p, encoding="utf-8", newline="").read()
family = read(draft + "/escape_code_family.txt")
head = read(draft + "/String.head.java")
env = dict(os.environ)
for name, mode, main, old, new in MUTANTS:
    assert family.count(old) == 1, name
    leaf = os.path.join(root, name)
    os.makedirs(leaf, exist_ok=True)
    for piece in PIECES:
        shutil.copy(os.path.join(draft, piece), leaf)
    mutated = family.replace(old, new)
    open(os.path.join(leaf, "escape_code_family.txt"), "w", encoding="utf-8", newline="").write(mutated)
    open(os.path.join(leaf, "String.head.java"), "w", encoding="utf-8", newline="").write(head.replace(family, mutated))
    env["PROOF_MODES"], env["PROOF_MAINS"], env["PROOF_COMPACT"] = mode, main, "-XX:+CompactStrings"
    run = subprocess.run([BASH, runner, leaf, work, donor], env=env, capture_output=True, text=True)
    failed = [l for l in run.stdout.split("\n") if l and not l.endswith("\t0\t-") and "\t" in l
              and not l.startswith(("checks", "RESULT", "method_texts", "lint", "excluded", "translateEscapes(null)"))]
    if "\nRESULT\t" not in "\n" + run.stdout:
        verdict, failed = "ERROR", [(run.stdout + run.stderr).strip()[-150:].replace("\n", " | ")]
    else:
        verdict = "KILLED" if run.returncode != 0 else "SURVIVED"
    print("%s\t%s\t%s\t%s" % (name, mode, verdict, failed[0][:150] if failed else run.stdout.strip()[-150:]))
    sys.stdout.flush()
    shutil.rmtree(leaf)
