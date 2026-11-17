# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""consolidate_superset.py <donors-dir> <out-dir>

STRING_SUPERSET_CONSOLIDATE v1 (2026-11-17; owner: "consolidate superset additively and optimize for m3string",
"inline them in atomized way"). CPU only, read-only on its inputs. Consolidates the atoms of every donor under
<donors-dir>/out/<donor>/ (atomize kit output: planes/ATOM_HASHES.tsv and the semantic MethodTable) into ONE additive
superset against the M3JDK string baseline <donors-dir>/out/baseline/:

  1. every TYPE-level and METHOD-level atom of every donor gets exactly one row (additive: nothing is dropped);
  2. its baseline plane: EXACT (atom_sha256 in the baseline), LOGIC (same normal-form logic hash), STRUCTURE (same
     structure hash), else ABSENT;
  3. cross-donor clusters by atom_sha256, then by logic_sha256: a cluster with members from two or more places names
     one owner (the smallest donor/path/symbol) and the rest MERGE into it;
  4. string relevance from the MethodTable (a String, CharSequence, char, char[], StringBuilder or StringBuffer in
     the return or parameter types of any overload of that name in that file);
  5. status, as a PROPOSAL (NEC: equivalence is decided by oracle or diff, never by a hash alone):
       REUSE    the baseline holds the same atom or logic: M3JDK's owner is used, nothing is inlined;
       MERGE    another donor atom owns the same atom or logic: one owner in the superset;
       VARIANT  same structure as a baseline atom but other logic: inlined as a named variant after the oracle run;
       INLINE   new: inlined as an atom;
     and the M3 class of string-relevant atoms: M3_PATH (facts or bulk windows on M3 storage) and, when the return
     type is String or CharSequence, COMPOSITE (the result is built from slices, no copy).
Outputs: SUPERSET_ATOMS.tsv, SUPERSET_CLUSTERS.tsv, SUPERSET_SUMMARY.tsv."""
import collections
import csv
import glob
import os
import sys

DONORS, OUT = sys.argv[1], sys.argv[2]
KINDS = ("METHOD", "IMPLEMENTATION", "INTERFACE", "TYPE")
STRINGY = ("java.lang.String", "java.lang.CharSequence", "java.lang.StringBuilder", "java.lang.StringBuffer")
csv.field_size_limit(1 << 30)


def rows(path):
    with open(path, encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f, delimiter="\t", quoting=csv.QUOTE_NONE))


def method_table(atomize_dir):
    found = glob.glob(os.path.join(atomize_dir, "*SemanticInventoryTables*MethodTable.tsv"))
    touch = {}
    for r in rows(found[0]) if found else []:
        types = (r.get("returnType") or "") + "," + (r.get("parameterTypes") or "")
        stringy = any(s in types for s in STRINGY) or ",char," in "," + types + "," or "char[]" in types
        returns = (r.get("returnType") or "") in ("java.lang.String", "java.lang.CharSequence")
        key = (r["sourcePath"], r["methodName"])
        old = touch.get(key, (False, False))
        touch[key] = (old[0] or stringy, old[1] or (stringy and returns))
    return touch


def main():
    base = [r for r in rows(os.path.join(DONORS, "out", "baseline", "planes", "ATOM_HASHES.tsv")) if r["kind"] in KINDS]
    b_atom = {r["atom_sha256"] for r in base}
    b_logic = {r["logic_sha256"] for r in base if r["logic_sha256"] not in ("", "-")}
    b_struct = {r["structure_sha256"] for r in base if r["structure_sha256"] not in ("", "-")}
    atoms = []
    for planes in sorted(glob.glob(os.path.join(DONORS, "out", "*", "planes", "ATOM_HASHES.tsv"))):
        donor = planes.replace("\\", "/").split("/out/")[1].split("/")[0]
        if donor == "baseline":
            continue
        touch = method_table(os.path.join(DONORS, "out", donor, "atomize"))
        for r in rows(planes):
            if r["kind"] not in KINDS:
                continue
            if r["atom_sha256"] in b_atom:
                plane = "EXACT"
            elif r["logic_sha256"] in b_logic:
                plane = "LOGIC"
            elif r["structure_sha256"] in b_struct:
                plane = "STRUCTURE"
            else:
                plane = "ABSENT"
            stringy, composite = touch.get((r["path"], r["symbol"]), (False, False))
            atoms.append(dict(donor=donor, path=r["path"], kind=r["kind"], symbol=r["symbol"],
                              atom=r["atom_sha256"], logic=r["logic_sha256"], structure=r["structure_sha256"],
                              plane=plane, stringy=stringy, composite=composite))
    # cross-donor clusters: exact atom first, then logic; owner = smallest (donor, path, symbol)
    owner_of = {}
    clusters = []
    for field in ("atom", "logic"):
        groups = collections.defaultdict(list)
        for a in atoms:
            if id(a) in owner_of or a[field] in ("", "-") or a["plane"] in ("EXACT", "LOGIC"):
                continue
            groups[a[field]].append(a)
        for key, members in sorted(groups.items()):
            places = {(m["donor"], m["path"], m["symbol"]) for m in members}
            if len(places) < 2:
                continue
            owner = min(members, key=lambda m: (m["donor"], m["path"], m["symbol"]))
            cid = f"{field.upper()}-{key[:12]}"
            clusters.append((cid, field, key, len(members), sorted({m["donor"] for m in members}),
                             f'{owner["donor"]}:{owner["path"]}#{owner["symbol"]}'))
            for m in members:
                owner_of[id(m)] = (cid, owner is m)
    for a in atoms:
        cid, is_owner = owner_of.get(id(a), ("-", False))
        if a["plane"] in ("EXACT", "LOGIC"):
            status = "REUSE"
        elif cid != "-" and not is_owner:
            status = "MERGE"
        elif a["plane"] == "STRUCTURE":
            status = "VARIANT"
        else:
            status = "INLINE"
        a["cluster"], a["status"] = cid, status
        m3 = []
        if a["stringy"] and status != "REUSE":
            m3.append("M3_PATH")
        if a["composite"] and status != "REUSE":
            m3.append("COMPOSITE")
        a["m3"] = "+".join(m3) or "-"
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "SUPERSET_ATOMS.tsv"), "w", encoding="utf-8", newline="") as w:
        w.write("donor\tpath\tkind\tsymbol\tplane\tstatus\tcluster\tstring_relevant\tm3\tatom_sha256\tlogic_sha256\n")
        for a in atoms:
            w.write("\t".join([a["donor"], a["path"], a["kind"], a["symbol"], a["plane"], a["status"], a["cluster"],
                               "yes" if a["stringy"] else "no", a["m3"], a["atom"], a["logic"]]) + "\n")
    with open(os.path.join(OUT, "SUPERSET_CLUSTERS.tsv"), "w", encoding="utf-8", newline="") as w:
        w.write("cluster\tplane\thash\tmembers\tdonors\towner\n")
        for cid, field, key, n, donors, owner in clusters:
            w.write(f"{cid}\t{field}\t{key}\t{n}\t{','.join(donors)}\t{owner}\n")
    summary = collections.defaultdict(collections.Counter)
    for a in atoms:
        s = summary[a["donor"]]
        s["atoms"] += 1
        s[a["status"]] += 1
        s["string_relevant"] += a["stringy"]
        s["M3_PATH"] += "M3_PATH" in a["m3"]
        s["COMPOSITE"] += "COMPOSITE" in a["m3"]
    cols = ["atoms", "REUSE", "MERGE", "VARIANT", "INLINE", "string_relevant", "M3_PATH", "COMPOSITE"]
    total = collections.Counter()
    with open(os.path.join(OUT, "SUPERSET_SUMMARY.tsv"), "w", encoding="utf-8", newline="") as w:
        w.write("donor\t" + "\t".join(cols) + "\n")
        for donor in sorted(summary):
            total.update(summary[donor])
            w.write(donor + "\t" + "\t".join(str(summary[donor][c]) for c in cols) + "\n")
        w.write("TOTAL\t" + "\t".join(str(total[c]) for c in cols) + "\n")
    print(open(os.path.join(OUT, "SUPERSET_SUMMARY.tsv"), encoding="utf-8").read())
    print("clusters", len(clusters))


main()
