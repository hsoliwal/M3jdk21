# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""absorb_rename_check.py : self-check of absorb_rename.py JAVADOC_REF v1 (CPU only, deterministic, no input files).

Runs rename_doc_refs over fixed sources with the type map {Canvas: M3Canvas, Path: M3Path} and compares the output and
the counts with the expected ones: class, member parameter types and constructor names of {@link}, {@linkplain},
{@value}, @see, @throws references are renamed (also across a continued parameter list); labels, prose, {@code},
quoted and HTML @see, block comments, line comments and string literals are kept; a name javac scope does not make ours
is counted as shadowed and kept; a fully qualified donor reference takes the new package; a donor package left in a
reference stops the run. Prints one line per case and exits 1 when a case fails. absorb_leaf1.sh runs it before S2."""
import importlib.util
import os
import sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
SIMPLE = {"Canvas": "M3Canvas", "Path": "M3Path"}
OURS = frozenset(SIMPLE)
NOTHING_VISIBLE = frozenset()
STOPS = "SystemExit"
CASES = (
    ("link_class_member", "/** Draws on {@link Canvas#drawPath(Path, int)} into {@link Path}. */",
     "/** Draws on {@link M3Canvas#drawPath(M3Path, int)} into {@link M3Path}. */", OURS,
     {"javadoc_type_token": 3}),
    ("label_and_prose_kept", "/** A Path: {@linkplain Path the Path} and {@code Path}. */",
     "/** A Path: {@linkplain M3Path the Path} and {@code Path}. */", OURS, {"javadoc_type_token": 1}),
    ("block_tags", "/**\n * Text.\n * @see Canvas#save()\n * @throws IllegalStateException if Path is bad\n */",
     "/**\n * Text.\n * @see M3Canvas#save()\n * @throws IllegalStateException if Path is bad\n */", OURS,
     {"javadoc_type_token": 1}),
    ("constructor_reference", "/** {@link Path()} and {@link Path#Path(Canvas)}. */",
     "/** {@link M3Path()} and {@link M3Path#M3Path(M3Canvas)}. */", OURS, {"javadoc_type_token": 4}),
    ("continued_parameter_list", "/**\n * {@link Canvas#clipPath(Path,\n *  Path)} x\n */",
     "/**\n * {@link M3Canvas#clipPath(M3Path,\n *  M3Path)} x\n */", OURS, {"javadoc_type_token": 3}),
    ("qualified_donor", "/** @see io.github.humbleui.skija.Canvas */",
     "/** @see jdk.m3.graphics.M3Canvas */", OURS, {"javadoc_pkg": 1, "javadoc_type_token": 1}),
    ("quoted_and_html_see", '/**\n * @see "Path"\n * @see <a href="Path.html">Path</a>\n */',
     '/**\n * @see "Path"\n * @see <a href="Path.html">Path</a>\n */', OURS, {}),
    ("value_tag", "/** {@value Canvas#MAX} */", "/** {@value M3Canvas#MAX} */", OURS, {"javadoc_type_token": 1}),
    ("not_doc_comments", '/* {@link Path} */ // {@link Path}\nString s = "/** {@link Path} */";',
     '/* {@link Path} */ // {@link Path}\nString s = "/** {@link Path} */";', OURS, {}),
    ("shadowed_kept", "/** {@link Path} */", "/** {@link Path} */", NOTHING_VISIBLE,
     {"javadoc_type_token_shadowed": 1}),
    ("jdk_qualified_kept", "/** {@link java.nio.file.Path} */", "/** {@link java.nio.file.Path} */", OURS, {}),
    ("donor_package_left_stops", "/** {@link io.github.humbleui.Other} */", STOPS, OURS, {}),
)


def load():
    spec = importlib.util.spec_from_file_location("absorb_rename", os.path.join(HERE, "absorb_rename.py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def run(module, case):
    name, source, expected, vis, counts = case
    got = Counter()
    try:
        out = module.rename_doc_refs(source, SIMPLE, got, set(vis))
    except SystemExit:
        out = STOPS
    ok = out == expected and {k: v for k, v in got.items() if v} == counts
    print("%s\t%s%s" % ("PASS" if ok else "FAIL", name, "" if ok else "\tgot %r %s" % (out, dict(got))))
    return ok


# FLLDCIM: TOP - JAVADOC_REF v1 self-check, 12 fixed cases
def main():
    module = load()
    passed = sum(run(module, c) for c in CASES)
    print("javadoc_ref_cases=%d passed=%d" % (len(CASES), passed))
    return 0 if passed == len(CASES) else 1


if __name__ == "__main__":
    sys.exit(main())
