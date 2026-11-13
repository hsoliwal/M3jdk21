#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""FOLDAUDIT_COLLECTIONS v1: restore the m3/collections leaves of the fold-close audit (lane 0e) whose PRs were closed
as landed although their landing merge was ancestry-only (tree == first parent) or a retain merge kept master's side.

Inputs (S:/m3-scratch/eb-conv/foldaudit: LEAVES.tsv, HUNK_VERDICTS.tsv, CLOSED_PRS.tsv, CITED_MERGES.tsv):
  PR #1084 head 6b16a20dcedf, cited landing b42ab7605b86 = ANCESTRY_ONLY (3 parents, tree 692d0cc47d21 == first parent):
    L0014 M3GuavaLongMultiset, L0050 M3LibraryBags (interop), L0025 M3CountTotals, L0028 M3CountLanes, L0054
    M3CountedCatalogue, L0029 m3_count_lanes.c, L0032 m3_count_totals.c, and the seven hash-pinned receiver texts passed
    from lane 22 (L0308 L0311 L0341 L0346 L0373 L0377 L0493); qualified by the origin tests L0107 L0151 L0166 L0173
    (MAPPED into the real module test trees: the origin counts/ module has no pom on master).
  PR #1791 head f452419fdcee, landed through #2070 (2028b1fb5f4) whose retain merge kept master's pre-identity owner:
    L0048 M3IndexPathRank identity path, qualified by the origin tests L0129 and L0180 (retained variant blobs).
  Record-only (DECISION.tsv): L0044, L0053, L0066, L0067.
Rules: ADD = a target absent on master is generated from the pinned donor git blob by exact substitutions (each old text
must occur exactly `count` times) plus CNPCDA anchors; MODIFY = the target must equal the pinned preimage blob and is
rewritten through the recorded hunk table (rules/*.hunks.json: every old line verified); NEW = a template of this
directory copied byte for byte. A target equal to its postimage is a no-op; anything else is drift and the recipe
refuses (exit 2) without writing. EDITS.tsv (every target: before/after sha256, donor blob, origin) is regenerated on
each run and must be a fixed point. --check writes nothing and exits 1 when a run would change a file. CPU only.
usage: python -I apply.py [--root REPO] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from pathlib import Path

RECIPE, VERSION = "apply.py", "v1"
HERE = Path(__file__).resolve().parent
RECIPE_REL = "make/m3/collections/qualification/foldaudit-collections-20261107"
EDITS_COLUMNS = ("leaf", "kind", "file", "before_sha256", "after_sha256", "donor_blob", "origin", "dropped_by", "rule")
CORE = "m3/collections/src/main/java/jdk/m3/collections/"
CTEST = "m3/collections/src/test/java/jdk/m3/collections/"
CRES = "m3/collections/src/test/resources/jdk/m3/collections/counts/"
NATIVE = "m3/collections/src/main/native/collections/"
IMAIN = "m3/collections/interop/src/main/java/jdk/m3/collections/interop/"
ITEST = "m3/collections/interop/src/test/java/jdk/m3/collections/interop/"
RECRES = "make/m3/collections/counts/recipe/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-counted-receiver/"
# The donors spell the legacy package; it is assembled at run time so the JDK naming recipe leaves this file alone.
LEG = "com" + ".m3.collections"
JNI_LEG = "Java_" + "com_" + "m3_" + "collections_"
P1084 = "PR #1084 head 6b16a20dcedf"
D1084 = "ancestry-only landing b42ab7605b86 (tree == first parent)"
P1791 = "PR #1791 head f452419fdcee"
D1791 = "retain merge of #2070 (2028b1fb5f4) kept master's side"
REC = "recipe " + RECIPE_REL + "/apply.py " + VERSION


def anchor(leaf: str, text: str) -> str:
    return "//FLLDCIM: MRL - fold-close audit leaf " + leaf + ": " + text + "; " + REC + "\n"


API = ("    //FLLDCIM: MRL - the PR-head owner spelled size()/distinctSize()/add(long); master's M3LongBag (e6722c63e9b,"
       " blob d00efc879547) spells totalCount()/distinctCount()/add(long, long): calls renamed, contract unchanged"
       " (HANDOFF.tsv)\n")
MAPPED = "    //FLLDCIM: MRL - MAPPED: the origin counts/ module has no pom on master; the file joins the real module tree\n"
GATE = ("    //FLLDCIM: MRL - the origin refused to run without the count library; on master the Java path runs always, the"
        " native path when the property names the library, and -Dm3.test.native.required=true restores the refusal\n")
CANCHOR = ("/* FLLDCIM: MRL - fold-close audit leaf %s: ADD from " + P1084 + " blob %s, " + D1084
           + "; JNI symbol renamed with the owner package (NAMING_RULES JNI_COM_M3); " + REC + " */\n")
LANES_BEFORE = ("    @BeforeAll static void loadActualNativeLibrary() {\n"
                "        String path = System.getProperty(\"m3.counted.native\"); assertNotNull(path, \"native proof must not silently skip\");\n"
                "        System.load(Path.of(path).toAbsolutePath().toString());\n    }\n")
LANES_AFTER = (GATE + "    private static boolean nativeLoaded;\n"
               "    @BeforeAll static void loadActualNativeLibrary() {\n"
               "        String path = System.getProperty(\"m3.counted.native\");\n"
               "        if (path == null && Boolean.getBoolean(\"m3.test.native.required\")) fail(\"native proof must not silently skip\");\n"
               "        nativeLoaded = path != null;\n"
               "        if (nativeLoaded) System.load(Path.of(path).toAbsolutePath().toString());\n    }\n")
TOTALS_BEFORE = ("    @BeforeAll static void loadRealNativeLibrary() {\n"
                 "        String library = System.getProperty(\"m3.totals.native\");\n"
                 "        assertNotNull(library, \"native verification is required, never skipped\");\n"
                 "        System.load(library);\n    }\n")
TOTALS_AFTER = (GATE + "    private static boolean nativeLoaded;\n"
                "    @BeforeAll static void loadRealNativeLibrary() {\n"
                "        String library = System.getProperty(\"m3.totals.native\");\n"
                "        if (library == null && Boolean.getBoolean(\"m3.test.native.required\")) fail(\"native verification is required, never skipped\");\n"
                "        nativeLoaded = library != null;\n"
                "        if (nativeLoaded) System.load(library);\n    }\n")
TOTALS_ALG_BEFORE = ("        assertEquals(a.intersection(b).size(), M3CountTotals.intersection(a, b));\n"
                     "        assertEquals(a.union(b).size(), M3CountTotals.union(a, b));\n"
                     "        assertEquals(a.sum(b).size(), M3CountTotals.sum(a, b));\n"
                     "        assertEquals(a.difference(b).size(), M3CountTotals.difference(a, b));\n"
                     "        assertEquals(ax, a.size()); assertEquals(by, b.size());\n")
TOTALS_ALG_AFTER = TOTALS_ALG_BEFORE.replace(".size()", ".totalCount()")
PKG = ("package " + LEG + ";\n", "package jdk.m3.collections;\n", 1)
PKG_INTEROP = ("package " + LEG + ".interop;\n", "package jdk.m3.collections.interop;\n", 1)
IMPORT_BAG = ("import " + LEG + ".M3LongBag;\n", "import jdk.m3.collections.M3LongBag;\n", 1)


def cls(line: str, *anchors: str) -> tuple[str, str, int]:
    return (line, "".join(anchors) + line, 1)


# leaf, kind, target, donor blob (ADD) / preimage blob (MODIFY) / template (NEW), origin, dropped_by, rule, subs
RULES = (
    ("L0028", "ADD", CORE + "M3CountLanes.java", "1d260fdbfe5a961e96ecf76f0882d93ec8f598d0", P1084, D1084,
     "ADD: origin blob, JDK naming of the package line, one anchor",
     [PKG, cls("public final class M3CountLanes {\n", "    " + anchor("L0028", "ADD from " + P1084 + " blob 1d260fdbfe5a, " + D1084))]),
    ("L0025", "ADD", CORE + "M3CountTotals.java", "31ce540046a34db05d65a950ffd6fae48f5a2d81", P1084, D1084,
     "ADD: origin blob, JDK naming, M3LongBag calls renamed to master's owner API (size->totalCount, distinctSize->distinctCount) in code and the four equivalence Javadoc lines",
     [PKG,
      ("        return Math.addExact(left.size(), right.size());\n", "        return Math.addExact(left.totalCount(), right.totalCount());\n", 1),
      ("        if (left == right) return left.size();\n", "        if (left == right) return left.totalCount();\n", 1),
      ("        M3LongBag scan = left.distinctSize() <= right.distinctSize() ? left : right;\n",
       "        M3LongBag scan = left.distinctCount() <= right.distinctCount() ? left : right;\n", 1),
      ("        return Math.addExact(left.size() - overlap, right.size());\n", "        return Math.addExact(left.totalCount() - overlap, right.totalCount());\n", 1),
      ("        return Objects.requireNonNull(left, \"left\").size() - intersection(left, right);\n",
       "        return Objects.requireNonNull(left, \"left\").totalCount() - intersection(left, right);\n", 1),
      ("equivalent to left.sum(right).size().", "equivalent to left.sum(right).totalCount().", 1),
      ("equivalent to left.intersection(right).size().", "equivalent to left.intersection(right).totalCount().", 1),
      ("equivalent to left.union(right).size().", "equivalent to left.union(right).totalCount().", 1),
      ("equivalent to left.difference(right).size().", "equivalent to left.difference(right).totalCount().", 1),
      cls("public final class M3CountTotals {\n", "    " + anchor("L0025", "ADD from " + P1084 + " blob 31ce540046a3, " + D1084), API)]),
    ("L0054", "ADD", CORE + "tooling/M3CountedCatalogue.java", "ffc026d13caa254e8eabed974eca365669c3e7a2", P1084, D1084,
     "ADD MAPPED: origin counts/src/main tooling class into the core module (no counts pom on master); JDK naming; its ledger is the test resource counts/catalogue.tsv",
     [("package " + LEG + ".tooling;\n", "package jdk.m3.collections.tooling;\n", 1),
      cls("public final class M3CountedCatalogue {\n", "    " + anchor("L0054", "ADD from " + P1084 + " blob ffc026d13caa, " + D1084), MAPPED)]),
    ("L0029", "ADD", NATIVE + "m3_count_lanes.c", "fcb85a55c9ff3d19faa1de991a109e3b3f3c3582", P1084, D1084,
     "ADD: origin blob, JNI symbol renamed with the package (javac -h header match in GATES.tsv), one anchor comment",
     [(JNI_LEG + "M3CountLanes_combineNative(", "Java_jdk_m3_collections_M3CountLanes_combineNative(", 1),
      ("#include <stdint.h>\n", "#include <stdint.h>\n" + CANCHOR % ("L0029", "fcb85a55c9ff"), 1)]),
    ("L0032", "ADD", NATIVE + "m3_count_totals.c", "d987d5860dec30d042cacbe833b244c4e54d79ca", P1084, D1084,
     "ADD: origin blob, JNI symbol renamed with the package, one anchor comment",
     [(JNI_LEG + "M3CountTotals_combineNative(", "Java_jdk_m3_collections_M3CountTotals_combineNative(", 1),
      ("#include <stdint.h>\n", "#include <stdint.h>\n" + CANCHOR % ("L0032", "d987d5860dec"), 1)]),
    ("L0014", "ADD", IMAIN + "M3GuavaLongMultiset.java", "4bef22568f1d2f87a6b6758109d04a8194f43805", P1084, D1084,
     "ADD: origin blob, JDK naming, M3LongBag calls renamed to master's owner API (size->totalCount, distinctSize->distinctCount); NEC against master's M3GuavaMultiset (boxed E over M3CountedBag; other contract, both kept)",
     [PKG_INTEROP, IMPORT_BAG,
      ("    @Override public int size() { return (int) Math.min(Integer.MAX_VALUE, counts.size()); }\n",
       "    @Override public int size() { return (int) Math.min(Integer.MAX_VALUE, counts.totalCount()); }\n", 1),
      ("            @Override public int size() { return counts.distinctSize(); }\n", "            @Override public int size() { return counts.distinctCount(); }\n", 2),
      ("other.entrySet().size() != counts.distinctSize()) return false;\n", "other.entrySet().size() != counts.distinctCount()) return false;\n", 1),
      cls("public final class M3GuavaLongMultiset extends AbstractCollection<Long> implements Multiset<Long> {\n",
          anchor("L0014", "ADD from " + P1084 + " blob 4bef22568f1d, " + D1084), API.lstrip())]),
    ("L0050", "ADD", IMAIN + "M3LibraryBags.java", "f22e5603a10d1efd57cb9b48ffe122ad4d775ac3", P1084, D1084,
     "ADD: origin blob, JDK naming, add(long) -> add(long, 1L) of master's owner API; NEC against M3LibraryCollections factories (object bags; this owner bridges primitive long counts)",
     [PKG_INTEROP, IMPORT_BAG,
      ("        for (Long key : source) result.add(Objects.requireNonNull(key, \"key\"));\n",
       "        for (Long key : source) result.add(Objects.requireNonNull(key, \"key\"), 1L);\n", 1),
      cls("public final class M3LibraryBags {\n", anchor("L0050", "ADD from " + P1084 + " blob f22e5603a10d, " + D1084), API.lstrip())]),
    ("L0166", "ADD", CTEST + "M3CountLanesTest.java", "6d1e3be27b430f633d7594c3a312aba6beb8c05a", P1084, D1084,
     "ADD MAPPED test: origin counts/src/test blob, JDK naming, native assertions gated on the library property (Java path always)",
     [PKG, (LANES_BEFORE, LANES_AFTER, 1),
      ("            assertArrayEquals(expected, M3CountLanes.combineNative(a, ao, b, bo, n, op));\n",
       "            if (nativeLoaded) assertArrayEquals(expected, M3CountLanes.combineNative(a, ao, b, bo, n, op));\n", 1),
      ("            assertThrows(failure, () -> M3CountLanes.combineNative(a, ao, b, bo, n, op));\n",
       "            if (nativeLoaded) assertThrows(failure, () -> M3CountLanes.combineNative(a, ao, b, bo, n, op));\n", 1),
      cls("final class M3CountLanesTest {\n", anchor("L0166", "ADD from " + P1084 + " blob 6d1e3be27b43, " + D1084), MAPPED.lstrip())]),
    ("L0107", "ADD", CTEST + "M3CountTotalsTest.java", "67d23af6cc9c743c4a82a673d6ff4ea73fedc5b6", P1084, D1084,
     "ADD MAPPED test: origin counts/totals/src/test blob, JDK naming, M3LongBag API renames (size->totalCount, add(long)->add(long, 1L)), native assertions gated on the library property",
     [PKG, (TOTALS_BEFORE, TOTALS_AFTER, 1), (TOTALS_ALG_BEFORE, TOTALS_ALG_AFTER, 1),
      ("        b.clear(); b.add(Long.MAX_VALUE);\n", "        b.clear(); b.add(Long.MAX_VALUE, 1L);\n", 1),
      ("        assertEquals(Long.MAX_VALUE, a.size()); assertEquals(1, b.size());\n",
       "        assertEquals(Long.MAX_VALUE, a.totalCount()); assertEquals(1, b.totalCount());\n", 1),
      ("        assertEquals(expected, M3CountTotals.combineNative(a, ao, b, bo, length, op));\n",
       "        if (nativeLoaded) assertEquals(expected, M3CountTotals.combineNative(a, ao, b, bo, length, op));\n", 1),
      ("        assertEquals(expected, Arrays.stream(M3CountLanes.combineNative(a, ao, b, bo, length, op)).sum());\n",
       "        if (nativeLoaded) assertEquals(expected, Arrays.stream(M3CountLanes.combineNative(a, ao, b, bo, length, op)).sum());\n", 1),
      ("        assertThrows(type, () -> M3CountLanes.combineNative(a,ao,b,bo,len,op));\n",
       "        if (nativeLoaded) assertThrows(type, () -> M3CountLanes.combineNative(a,ao,b,bo,len,op));\n", 1),
      ("        assertThrows(type, () -> M3CountTotals.combineNative(a,ao,b,bo,len,op));\n",
       "        if (nativeLoaded) assertThrows(type, () -> M3CountTotals.combineNative(a,ao,b,bo,len,op));\n", 1),
      cls("final class M3CountTotalsTest {\n", anchor("L0107", "ADD from " + P1084 + " blob 67d23af6cc9c, " + D1084), MAPPED.lstrip())]),
    ("L0173", "ADD", CTEST + "M3CountTotalsAllocationProbe.java", "7684db3d4224dcf211db6e9156cbbb2d62f26f67", P1084, D1084,
     "ADD MAPPED probe (main, not a surefire test): origin blob, JDK naming, size->totalCount",
     [PKG, ("escapedTotal=result.size();", "escapedTotal=result.totalCount();", 1),
      cls("public final class M3CountTotalsAllocationProbe {\n", anchor("L0173", "ADD from " + P1084 + " blob 7684db3d4224, " + D1084), MAPPED.lstrip())]),
    ("L0151", "ADD", ITEST + "M3BagInteropTest.java", "a15150ccdb812ec544bdc99946c7617535dedba9", P1084, D1084,
     "ADD MAPPED test: origin counts/src/test/.../interop blob, JDK naming, size->totalCount of master's owner API",
     [PKG_INTEROP, IMPORT_BAG,
      ("        assertEquals(20000, imported.size()); for (long key = 0; key < 113; key++) {\n",
       "        assertEquals(20000, imported.totalCount()); for (long key = 0; key < 113; key++) {\n", 1),
      ("        eclipse.clear(); assertEquals(20000, imported.size());\n", "        eclipse.clear(); assertEquals(20000, imported.totalCount());\n", 1),
      cls("final class M3BagInteropTest {\n", anchor("L0151", "ADD from " + P1084 + " blob a15150ccdb81, " + D1084), MAPPED.lstrip())]),
    ("L0054", "ADD", CRES + "catalogue.tsv", "651cb7b16228427a46b831a9ec15574d6756a98f", P1084, D1084,
     "ADD MAPPED ledger byte for byte (historical receiver sub-ledger; the TSV takes no anchor): test resource of M3CountedCatalogueTest", []),
    ("L0054", "NEW", CTEST + "M3CountedCatalogueTest.java", "templates/M3CountedCatalogueTest.java", "this recipe", "-",
     "NEW test (no origin test ran the main): queries the restored ledger, refuses malformed rows and bad arguments", []),
    ("L0129", "ADD", CTEST + "M3IndexPathIdentitySelfTest.java", "f5edbff66ec6aed6f9a533a90c7f9bca44f919ff", P1791, D1791,
     "ADD: retained variant blob (JDK-named by fe35ea98841) byte for byte plus one anchor",
     [cls("public final class M3IndexPathIdentitySelfTest {\n", anchor("L0129", "ADD from " + P1791 + ", retained variant blob f5edbff66ec6, " + D1791))]),
    ("L0180", "ADD", CTEST + "M3IndexPathIdentityTest.java", "d9bf058b77b450efae867a3e38790c28e7af13f9", P1791, D1791,
     "ADD: retained variant blob byte for byte plus one anchor",
     [cls("final class M3IndexPathIdentityTest {\n", anchor("L0180", "ADD from " + P1791 + ", retained variant blob d9bf058b77b4, " + D1791))]),
    ("L0048", "MODIFY", CORE + "M3IndexPathRank.java", "647d2dc0452da9b217b4b262aa6355361d067287", P1791, D1791,
     "MODIFY: 3-way merge (base pr1791 parent blob 4a60fd1fab76, ours master blob 647d2dc0452d, theirs 905797a33244) clean except one token in Window.copySelectedRowIds (rowIdAt); hunk table rules/M3IndexPathRank.hunks.json; three anchors",
     "rules/M3IndexPathRank.hunks.json"),
)
TXT = (("L0493", "M3LongBag.java.txt", "9483047e48b4ba926093c827bef92ff40fb6420d"),
       ("L0377", "M3LongHashSet.java.txt", "d2fca03ba49ef568a900320f99bac690bc3eaea5"),
       ("L0308", "M3LongLinkedHashSet.java.txt", "99a41e8bdfdf92941ceb979e3d7354c9b291df90"),
       ("L0346", "M3LongLongHashMap.java.txt", "ad7cd2dca56f42d8b39175dc25a65409f050097d"),
       ("L0373", "pre/M3LongHashSet.java.txt", "1fe9ac1bfa11cf6a626a749c37d64ccec0ed09df"),
       ("L0311", "pre/M3LongLinkedHashSet.java.txt", "28c864a69a384dbfe6fbc499eff7da4015264a3f"),
       ("L0341", "pre/M3LongLongHashMap.java.txt", "b1d85622c2e19db069d0b2a64fb8011b5c01be34"))
RULES += tuple((leaf, "ADD", RECRES + name, blob, P1084, D1084,
                "ADD hash-pinned receiver text (passed from lane 22): origin blob with the JDK naming of its package line (master's recipe texts are JDK-named); no anchor in a pinned text",
                [PKG]) for leaf, name, blob in TXT)


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git_blob(root: Path, oid: str) -> bytes:
    data = subprocess.run(["git", "-C", str(root), "cat-file", "-p", oid], check=True, capture_output=True).stdout
    got = subprocess.run(["git", "-C", str(root), "hash-object", "--stdin"], input=data, check=True, capture_output=True).stdout.decode().strip()
    if got != oid:
        raise Refused(f"blob id mismatch {oid} -> {got}")
    return data


def substitute(text: str, subs, path: str) -> str:
    for old, new, count in subs:
        if text.count(old) != count:
            raise Refused(f"{path}: anchor occurs {text.count(old)} times, expected {count}: {old[:60]!r}")
        text = text.replace(old, new)
    return text


def apply_hunks(pre: bytes, hunks_path: Path, path: str) -> bytes:
    lines = pre.decode("utf-8").split("\n")
    hunks = json.loads(hunks_path.read_text(encoding="utf-8"))
    for hunk in sorted(hunks, key=lambda h: h["pre_line"], reverse=True):
        start = hunk["pre_line"] - 1
        if lines[start:start + len(hunk["old"])] != hunk["old"]:
            raise Refused(f"{path}: hunk at pre line {hunk['pre_line']} does not match the preimage")
        lines[start:start + len(hunk["old"])] = hunk["new"]
    return "\n".join(lines).encode("utf-8")


def postimage(root: Path, rule) -> tuple[bytes | None, bytes, str]:
    leaf, kind, path, donor, *_rest, subs = rule
    if kind == "NEW":
        return None, (HERE / donor).read_bytes(), "template " + donor
    if kind == "MODIFY":
        pre = git_blob(root, donor)
        return pre, apply_hunks(pre, HERE / subs, path), donor
    source = git_blob(root, donor)
    if not subs:
        return None, source, donor
    return None, substitute(source.decode("utf-8"), subs, path).encode("utf-8"), donor


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=str(HERE.parents[4]))
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(args.root).resolve()
    plans, rows = [], []
    try:
        for rule in RULES:
            leaf, kind, path, donor, origin, dropped, note, _subs = rule
            pre, post, donor_note = postimage(root, rule)
            target = root / path
            current = target.read_bytes() if target.exists() else None
            if current == post:
                state = "UNCHANGED"
            elif current is None and kind != "MODIFY":
                state = "WRITE"
            elif kind == "MODIFY" and current == pre:
                state = "WRITE"
            else:
                raise Refused(f"{path}: drifted ({'absent' if current is None else sha256(current)[:16]}); neither preimage nor postimage")
            plans.append((target, post, state, path))
            rows.append((leaf, kind, path, sha256(pre) if pre is not None else "ABSENT", sha256(post), donor_note[:40], origin, dropped, kind + " " + note))
    except Refused as problem:
        print("REFUSED", problem)
        return 2
    edits = "\t".join(EDITS_COLUMNS) + "\n" + "".join("\t".join(row) + "\n" for row in rows)
    edits_path = HERE / "EDITS.tsv"
    edits_state = "UNCHANGED" if edits_path.exists() and edits_path.read_text(encoding="utf-8") == edits else "CHANGED"
    changed = sum(state == "WRITE" for _t, _p, state, _path in plans)
    for target, post, state, path in plans:
        print(f"{state:9} {path}")
    print(f"FOLDAUDIT_COLLECTIONS {VERSION} rules={len(plans)} changed={changed} edits={edits_state}")
    if args.check:
        return 1 if changed or edits_state == "CHANGED" else 0
    for target, post, state, _path in plans:
        if state == "WRITE":
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(post)
    if edits_state == "CHANGED":
        edits_path.write_text(edits, encoding="utf-8", newline="\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
