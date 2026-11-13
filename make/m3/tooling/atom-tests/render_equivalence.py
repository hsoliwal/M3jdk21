# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""render_equivalence.py <old_spec_dir> <new_spec_dir> <template_dir> <out_tsv> : RENDER EQUIVALENCE v1 (CPU).

Proof of a spec refactoring (pattern extraction, argument moves): every atom row of the old spec set and of the new one
is rendered through the generator (gen_atom_tests.render_body + render_row, no gates), and the generated test methods
are compared byte for byte after the pattern name in the method label is masked. Writes one row per atom:
atom, old pattern, new pattern, status (IDENTICAL, CHANGED, ONLY_OLD, ONLY_NEW); exit 1 unless every row is IDENTICAL."""
import os
import re
import sys

import gen_atom_tests as gen

LABEL_PATTERN = re.compile(r"^(    /\*\* \S+#\d+ )\w+(; oracle: )", re.M)


def rendered_methods(spec_dir, templates):
    out = {}
    for spec in sorted(os.listdir(spec_dir)):
        if not spec.endswith(".atoms"):
            continue
        header, rows = gen.parse_spec(os.path.join(spec_dir, spec))
        gen.assign_method_names(rows)
        for row in rows:
            body = gen.render_body(row, templates, spec)
            name, label, method = gen.render_row(row, body, header.get("returns", "void"))
            key = "%s %s.%s#%d" % (spec, row["owner"], row["atom"], row["n"])
            out[key] = (row["pattern"], LABEL_PATTERN.sub(r"\1<pattern>\2", method))
    return out


def main():
    old_dir, new_dir, template_dir, out_tsv = sys.argv[1:5]
    templates = gen.load_templates(template_dir)
    old, new = rendered_methods(old_dir, templates), rendered_methods(new_dir, templates)
    rows, counts = [], {}
    for key in sorted(set(old) | set(new)):
        if key not in new:
            status = "ONLY_OLD"
        elif key not in old:
            status = "ONLY_NEW"
        else:
            status = "IDENTICAL" if old[key][1] == new[key][1] else "CHANGED"
        counts[status] = counts.get(status, 0) + 1
        rows.append((key, old.get(key, ("-",))[0], new.get(key, ("-",))[0], status))
    with open(out_tsv, "w", encoding="utf-8", newline="\n") as w:
        w.write("atom\told_pattern\tnew_pattern\tstatus\n")
        w.writelines("\t".join(r) + "\n" for r in rows)
    moved = sum(1 for r in rows if r[1] != r[2] and r[3] == "IDENTICAL")
    print("render_equivalence rows=%d %s pattern_changed_identical=%d" % (
        len(rows), " ".join("%s=%d" % kv for kv in sorted(counts.items())), moved))
    sys.exit(0 if counts.get("IDENTICAL", 0) == len(rows) else 1)


if __name__ == "__main__":
    main()
