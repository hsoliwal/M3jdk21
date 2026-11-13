# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""csv_capabilities.py <CSV_API_SUPERSET.tsv> <DONOR_FEATURES.tsv> <DONOR_EXTRA.tsv> <CSV_CAPABILITY_SYNONYMS.tsv>
<out CSV_CAPABILITIES.tsv> : CSV-CAPABILITY v1 projection and gate (CPU, read-only on inputs).

Universe of donor keys, each a (donor, key) pair:
  v1        every non-empty donor cell of CSV_API_SUPERSET.tsv (242 keys from the configuration classes)
  feature   enum-valued options, feature:<Enum>.<CONSTANT> (csv_donor_docs.py)
  gap:      univocity CommonParserSettings / CommonWriterSettings keys the v1 file regex missed
  op:       record-level operation keys (univocity AbstractParser / AbstractWriter; commons CSVParser /
            CSVPrinter / CSVRecord)
Gate: the reviewed synonym table assigns every pair to exactly one canonical capability, names no pair outside the
universe, and leaves no capability empty; otherwise exit 1 with the offending pairs. Output: one row per canonical
capability with donor coverage and the donor methods behind it. Also prints how the review resolved the v1 keys
that matched across donors by name only (TRUE_SYNONYM or FALSE_FRIEND). Byte-identical on re-run."""
import collections
import sys

DONORS = ("univocity-parsers", "commons-csv", "jackson-dataformat-csv")
EMPTY = "-"
SEP = ";"
EXTRA_PREFIX = {"gap-config": "gap:", "operation": "op:"}
HEADER = ("capability\tgroup\ttier\tdonors\tunivocity_covered\tcommons_covered\tjackson_covered\tkeys\t"
          "univocity_methods\tcommons_methods\tjackson_methods\n")


def rows(path):
    with open(path, encoding="utf-8") as f:
        head = next(f).rstrip("\n").split("\t")
        return head, [line.rstrip("\n").split("\t") for line in f if line.strip()]


def universe(superset, features, extra):
    pairs = {}
    head, body = rows(superset)
    for r in body:
        for d in DONORS:
            cell = r[head.index(d)]
            if cell != EMPTY:
                pairs[(d, r[0])] = cell
    _, body = rows(features)
    for donor, feature, _ in body:
        pairs[(donor, feature)] = feature[len("feature:"):]
    _, body = rows(extra)
    for source, donor, key, methods, _ in body:
        pairs[(donor, EXTRA_PREFIX[source] + key)] = methods
    return pairs


def assignments(synonyms):
    head, body = rows(synonyms)
    table, meta, problems = {}, {}, []
    for r in body:
        cap = r[0]
        meta[cap] = (r[head.index("group")], r[head.index("tier")])
        keys = 0
        for d in DONORS:
            cell = r[head.index(d)]
            for k in ([] if cell == EMPTY else cell.split(SEP)):
                keys += 1
                if (d, k) in table:
                    problems.append("DUPLICATE %s %s in %s and %s" % (d, k, table[(d, k)], cap))
                table[(d, k)] = cap
        if keys == 0:
            problems.append("EMPTY capability %s" % cap)
    return table, meta, problems


def name_matches(superset, table):
    head, body = rows(superset)
    out = []
    for r in body:
        donors = [d for d in DONORS if r[head.index(d)] != EMPTY]
        if len(donors) > 1:
            caps = sorted({table.get((d, r[0]), "?") for d in donors})
            out.append((r[0], ",".join(donors), ",".join(caps), "TRUE_SYNONYM" if len(caps) == 1 else "FALSE_FRIEND"))
    return out


def main(argv):
    superset, features, extra, synonyms, out = argv[1:6]
    pairs = universe(superset, features, extra)
    table, meta, problems = assignments(synonyms)
    problems += ["UNKNOWN %s %s -> %s" % (d, k, c) for (d, k), c in sorted(table.items()) if (d, k) not in pairs]
    problems += ["MISSING %s %s" % (d, k) for (d, k) in sorted(pairs) if (d, k) not in table]
    by_cap = collections.defaultdict(lambda: collections.defaultdict(list))
    for (d, k), cap in table.items():
        if (d, k) in pairs:
            by_cap[cap][d].append(pairs[(d, k)])
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write(HEADER)
        for cap in sorted(meta):
            cov = [d for d in DONORS if by_cap[cap].get(d)]
            methods = [SEP.join(sorted(set(SEP.join(by_cap[cap].get(d, [])).split(SEP)) - {""})) or EMPTY
                       for d in DONORS]
            keys = sum(len(v) for v in by_cap[cap].values())
            f.write("\t".join([cap, meta[cap][0], meta[cap][1], str(len(cov))]
                              + ["Y" if d in cov else EMPTY for d in DONORS] + [str(keys)] + methods) + "\n")
    tiers = collections.Counter(t for _, t in meta.values())
    spread = collections.Counter(sum(1 for d in DONORS if by_cap[c].get(d)) for c in meta)
    print("CSV-CAPABILITY v1 capabilities=%d pairs=%d mapped=%d tiers=%s donors_per_capability=%s problems=%d"
          % (len(meta), len(pairs), sum(1 for p in pairs if p in table), dict(sorted(tiers.items())),
             dict(sorted(spread.items())), len(problems)))
    for m in name_matches(superset, table):
        print("NAME_MATCH %s donors=%s capability=%s %s" % m)
    for p in problems:
        print(p)
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main(sys.argv)
