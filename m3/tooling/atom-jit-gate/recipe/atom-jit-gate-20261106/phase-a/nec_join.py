#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Phase A NEC probe join: donor pattern index atoms vs census semantic atoms on the same corpus (scratch evidence)."""
import collections
import hashlib
import sys
from pathlib import Path

out = Path(sys.argv[1])
donor = [l.split("\t") for l in (out / "donor-out/DONOR_ATOMS.tsv").read_text(encoding="utf-8").splitlines() if l]
census = [l.split("\t") for l in (out / "census-out/CENSUS_ROWS.tsv").read_text(encoding="utf-8").splitlines() if l]
refused = {l.split("\t")[0] for l in (out / "census-out/CENSUS_REFUSALS.tsv").read_text(encoding="utf-8").splitlines() if l}

by_span = {}
for row in census:
    by_span.setdefault((row[2], row[7], row[8]), []).append(row)

stats = collections.Counter()
matched = []
for d in donor:
    path, start, end, kind = d[0], d[1], d[2], d[3]
    stats["donor_atoms"] += 1
    if path in refused:
        stats["donor_atoms_in_census_refused_files"] += 1
        continue
    stats["donor_atoms_in_census_ok_files"] += 1
    rows = by_span.get((path, start, end), [])
    if not rows:
        stats["no_span_equal_census_atom_" + kind] += 1
        continue
    row = rows[0]
    stats["span_equal_" + kind + "_to_" + row[3]] += 1
    if row[9] == d[7]:
        stats["span_equal_and_exactRoot==source_sha256"] += 1
        matched.append((d, row))
    else:
        stats["span_equal_but_sha_differs"] += 1

# donor groups restricted to matched atoms vs groups the census can form from its own columns
def groups(items, key):
    g = collections.defaultdict(list)
    for d, row in items:
        g[key(d, row)].append(d[6])
    return {tuple(sorted(v)) for v in g.values()}

donor_projection = groups(matched, lambda d, r: d[8])            # donor patternId = projection key
donor_exact = groups(matched, lambda d, r: (d[8], d[7]))        # donor KNOWN_PATTERN scope
census_exact = groups(matched, lambda d, r: r[9])               # census: source_sha256 only
census_shape = groups(matched, lambda d, r: (r[3], r[6], r[10]))  # census: kind + signature + catalogue pattern
stats["matched_atoms"] = len(matched)
stats["donor_projection_groups"] = len(donor_projection)
stats["census_shape_groups(kind,signature,pattern)"] = len(census_shape)
stats["projection_groups_identical_to_census_shape_groups"] = len(donor_projection & census_shape)
stats["donor_exact_groups"] = len(donor_exact)
stats["census_exact_groups(source_sha256)"] = len(census_exact)
stats["exact_groups_identical"] = len(donor_exact & census_exact)
multi = lambda gs: {g for g in gs if len(g) > 1}
stats["donor_projection_groups_size>1"] = len(multi(donor_projection))
stats["census_shape_groups_size>1"] = len(multi(census_shape))
stats["identical_groups_size>1(projection_vs_shape)"] = len(multi(donor_projection) & multi(census_shape))
stats["donor_exact_groups_size>1"] = len(multi(donor_exact))
stats["census_exact_groups_size>1"] = len(multi(census_exact))
stats["identical_groups_size>1(exact)"] = len(multi(donor_exact) & multi(census_exact))

def digest(gs):
    return hashlib.sha256("\n".join(sorted(",".join(g) for g in gs)).encode()).hexdigest()

stats_lines = ["%s\t%s" % (k, stats[k]) for k in sorted(stats)]
stats_lines.append("sha256_donor_projection_groups\t" + digest(donor_projection))
stats_lines.append("sha256_census_shape_groups\t" + digest(census_shape))
stats_lines.append("sha256_donor_exact_groups\t" + digest(donor_exact))
stats_lines.append("sha256_census_exact_groups\t" + digest(census_exact))
(out / "NEC_JOIN.tsv").write_text("\n".join(stats_lines) + "\n", encoding="utf-8")
print("\n".join(stats_lines))
