# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""mr_check_selftest.py <repo> <work_dir> <oracle_jars> : self-check of the Multi-Release cross-check of
gen_atom_tests.py v3 (CPU only, deterministic).

The generator takes the JEP 238 choice of the oracle jars from mr_select.jar_versions and compares it with
leaf1/MR_SELECT.tsv in both directions (gen_atom_tests.mr_cross_check). Cases: the real jars against the real
MR_SELECT.tsv (every row MATCH); in-memory rows where MR_SELECT misses a versioned class, selects another version, names
another jar entry, or selects a version above the release (each MISMATCH); and two jars written into <work_dir>: a
Multi-Release jar with versions/9 and versions/11 of one class (Java 21 loads versions/11; a last-name-wins reading of
the sorted entries gives versions/9, the v2 generator's answer) and a jar with a versions/9 entry but no
"Multi-Release: true" manifest (Java 21 loads the root class; v2 reported the versioned entry). Prints one line per case
and exits 1 when a case fails."""
import importlib.util
import os
import sys
import tempfile
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
LEAF = "m3/tooling/skija-absorb/leaf1"
NAME = "io.github.humbleui.skija.impl.Cleanable"
ENTRY9 = "META-INF/versions/9/io/github/humbleui/skija/impl/Cleanable.class"
ENTRY11 = "META-INF/versions/11/io/github/humbleui/skija/impl/Cleanable.class"
ROOT_ENTRY = "io/github/humbleui/skija/impl/Cleanable.class"
MANIFEST = "META-INF/MANIFEST.MF"
MR_MANIFEST = "Manifest-Version: 1.0\r\nMulti-Release: true\r\n"
PLAIN_MANIFEST = "Manifest-Version: 1.0\r\n"
CLASS_BYTES = b"\xca\xfe\xba\xbe"
ROW9 = {"selected": "9", "versions": "base,9", "jar_entry": ENTRY9, "source": "shared/java9/impl/Cleanable.java"}


def load(name):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, name + ".py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def jar(path, manifest, entries):
    with zipfile.ZipFile(path, "w") as z:
        z.writestr(MANIFEST, manifest)
        for e in entries:
            z.writestr(e, CLASS_BYTES)
    return path


def statuses(gen, chosen, selected):
    return [r[4] for r in gen.mr_cross_check(chosen, selected)]


def cases(gen, repo, work, oracle):
    mr = gen.MR
    real = {r["name"]: r for r in gen.rows(os.path.join(repo, LEAF, "MR_SELECT.tsv")) if r["kind"] == "CLASS"}
    real_chosen = mr.jar_versions(oracle.split(os.pathsep), int(gen.MR_RELEASE))[0]
    real_rows = statuses(gen, real_chosen, real)
    chosen9 = {NAME: (9, ENTRY9)}
    two = jar(os.path.join(work, "mr_9_11.jar"), MR_MANIFEST, [ROOT_ENTRY, ENTRY9, ENTRY11])
    plain = jar(os.path.join(work, "not_mr.jar"), PLAIN_MANIFEST, [ROOT_ENTRY, ENTRY9])
    last_name_wins = [n for n in sorted(zipfile.ZipFile(two).namelist()) if n.startswith("META-INF/versions/")][-1]
    return (
        ("real_jars_real_ledger", bool(real_rows) and set(real_rows) == {"MATCH"}, "%d rows %s" % (
            len(real_rows), ",".join(real_rows))),
        ("agree", statuses(gen, chosen9, {NAME: ROW9}) == ["MATCH"], "jar 9, MR_SELECT 9"),
        ("ledger_misses_class", statuses(gen, chosen9, {}) == ["MISMATCH"], "jar 9, no MR_SELECT row"),
        ("ledger_other_version", statuses(gen, chosen9, {NAME: dict(ROW9, selected="11", versions="base,9,11")})
         == ["MISMATCH"], "jar 9, MR_SELECT 11"),
        ("ledger_other_entry", statuses(gen, chosen9, {NAME: dict(ROW9, jar_entry=ROOT_ENTRY)}) == ["MISMATCH"],
         "jar entry differs"),
        ("ledger_above_release", statuses(gen, {}, {NAME: dict(ROW9, selected="25", versions="base,25")})
         == ["MISMATCH"], "MR_SELECT 25 > release 21, jar root"),
        ("jep238_highest_version", mr.jar_versions([two], int(gen.MR_RELEASE))[0].get(NAME, (0,))[0] == 11
         and last_name_wins == ENTRY9, "jar_versions 11; last name of the sorted entries %s" % last_name_wins),
        ("jep238_needs_manifest", mr.jar_versions([plain], int(gen.MR_RELEASE))[0] == {},
         "no Multi-Release manifest: root class"),
    )


def main(argv):
    if len(argv) != 4:
        sys.stderr.write(__doc__)
        return 2
    repo, work, oracle = argv[1:]
    os.makedirs(work, exist_ok=True)
    gen = load("gen_atom_tests")
    with tempfile.TemporaryDirectory(dir=work) as tmp:
        results = cases(gen, repo, tmp, oracle)
    for name, ok, detail in results:
        print("%s\t%s\t%s" % ("PASS" if ok else "FAIL", name, detail))
    passed = sum(ok for _, ok, _ in results)
    print("mr_check_cases=%d passed=%d" % (len(results), passed))
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
