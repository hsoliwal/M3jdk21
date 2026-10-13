# SPDX-License-Identifier: Apache-2.0
"""Seal RENAME_MANIFEST.tsv for the SWT residency engine -> m3/ports/swing-viewport package rename.

usage: python -I gen_rename_manifest.py SWT_REPO SWT_COMMIT OUT_TSV
CPU only: source bytes are git blobs at SWT_COMMIT; the post hash is the SHA-256 of the exact
transform ResidencyRename.java applies (ordered literal replacements over UTF-8 text).
"""
import hashlib
import subprocess
import sys

RULES = [
    ("org.eclipse.swt.internal.residency", "com.m3.swing.viewport.engine.residency"),
    ("org.eclipse.swt.internal.swap", "com.m3.swing.viewport.engine.swap"),
    ("org.eclipse.swt.internal.PackedHeapGrowth", "com.m3.swing.viewport.engine.PackedHeapGrowth"),
    ("import org.eclipse.swt.internal.*;", "import com.m3.swing.viewport.engine.*;"),
    ("package org.eclipse.swt.internal;", "package com.m3.swing.viewport.engine;"),
]
SRC = "bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/internal/"
DST = "m3/ports/swing-viewport/src/main/java/com/m3/swing/viewport/engine/"
APACHE_PORTS = {"SwapStore", "TieredSwapCache", "SwapBudgetPlane", "SwapPressureController", "SwapPageBinaryOps",
                "JavaSwapPageBinaryOps", "ParityCheckedSwapPageBinaryOps"}

repo, commit, out = sys.argv[1:4]


def git(*args):
    return subprocess.run(["git", "-C", repo] + list(args), check=True, capture_output=True).stdout


def transform(text):
    for a, b in RULES:
        text = text.replace(a, b)
    return text


paths = [p for p in git("ls-tree", "-r", "--name-only", commit, "--", SRC).decode("utf-8").splitlines()
         if p.endswith(".java") and (p.startswith(SRC + "residency/") or p.startswith(SRC + "swap/")
                                     or p == SRC + "PackedHeapGrowth.java")]
rows = []
for p in sorted(paths):
    blob = git("cat-file", "-p", commit + ":" + p)
    post = transform(blob.decode("utf-8")).encode("utf-8")
    rel = p[len(SRC):]
    package = "com.m3.swing.viewport.engine" + ("." + rel.rsplit("/", 1)[0] if "/" in rel else "")
    name = rel.rsplit("/", 1)[-1][:-5]
    licence = "Apache-2.0 (ported Synexia)" if name in APACHE_PORTS else "EPL-2.0 OR Apache-2.0"
    residual = transform(blob.decode("utf-8")).count("org.eclipse.swt")
    rows.append((p, hashlib.sha256(blob).hexdigest(), DST + rel, package, hashlib.sha256(post).hexdigest(), licence,
                 str(residual)))
with open(out, "w", encoding="utf-8", newline="\n") as f:
    f.write("# source_commit\t" + commit + "\n")
    f.write("source_path\tsource_sha256\ttarget_path\ttarget_package\tpost_sha256\tlicence\tresidual_org_eclipse_swt_mentions\n")
    for r in rows:
        f.write("\t".join(r) + "\n")
print("rows", len(rows))
