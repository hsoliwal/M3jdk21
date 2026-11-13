#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_01_15 v1: restore the interop factories/collectors dropped by the ancestry merge f4fbe3333946.

Merge-loss audit (S:/m3-scratch/eb-conv/mergeloss, leaves 01 and 15) on m3/collections/interop:
  case 01 RESTORE: ancestry merge f4fbe3333946 (2026-10-17) dropped origin 58a72d0b2b8 (PR #1174, shown MERGED);
  case 15 NEC: ancestry merge 0efc1719ca57 (#1214 #1218 #1221) dropped origin 50779ade2a3 (PR #1218); its
  guavaMultiset/apacheMultiSet are the case-01 members (same bodies); its jdkArrayList, eclipseListMultimap and
  eclipseSetMultimap were never in the SDK binding inventory (DECISION.tsv, HUNKS.tsv): no rule here.
Derived from make/m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored "sub" edits, every
anchor exactly once, LF payloads re-joined with the preimage's line ending), extended with one CREATE rule kind: a
target absent on master is generated from a pinned donor blob by the same anchored edits. Every EDIT rule names the
git blob sha of its preimage on master b28b7c938e2; a target must equal the preimage (the rule is applied) or the
postimage (already applied, no-op); anything else is drift and the recipe refuses (exit 2) without writing. A CREATE
target must be absent or equal the postimage. Idempotent: a second run reports changed=0 and EDITS.tsv is a fixed
point. --check writes nothing and exits 1 when a run would change a file. Every lost member's disposition
(RESTORED, MAPPED, PRESENT, NEC, SUPERSEDED) is declared in HUNKS.tsv; DECISION.tsv records the case decisions.
v1.1 (hand-off only; RULES and the edit logic are unchanged): a file this recipe wrote may be handed to a successor
recipe in SUCCESSORS, pinned by this recipe's postimage sha256 and the successor's postimage sha256. When the target
equals the successor's postimage it is reported FILE SUPERSEDED (no write, no refusal); its original EDITS.tsv row is
kept as history and a SUPERSEDED_BY row names the successor owner.

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1.1"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "b28b7c938e2"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "case", "origin", "dropping_merge", "donor_blobs", "rule")
MAIN = "m3/collections/interop/src/main/java/jdk/m3/collections/interop/"
TEST = "m3/collections/interop/src/test/java/jdk/m3/collections/interop/"
O01, M01 = "58a72d0b2b8", "f4fbe3333946"
LEGACY = "com" + ".m3.collections"  # pre-JDK-naming package of the donor blob and the master TSV owners (split so
#                                      the JDK naming recipe leaves these anchors alone)
A01 = ("    //FLLDCIM: MRL - mergeloss case 01: restored from origin 58a72d0b2b8 (PR #1174), dropped by ancestry merge"
       " f4fbe3333946; recipe qualification/mergeloss-01-15-20261106\n")


class Refused(Exception):
    pass


# case, kind, path, preimage blob (EDIT) or donor blob (CREATE), origin, merge, donor blobs, rule note, edits
RULES = (
    ("01", "EDIT", MAIN + "M3LibraryCollections.java", "03d974d46ebd2e3dda96551767747e0ac914680c", O01, M01,
     "post=f65660191bfb pre=8ca3906e4ca1",
     "MAPPED by member name (owner moved on: M3ArrayList list multimap, Spring array map, guavaBiMap/apacheBidiMap/"
     "springCaseInsensitiveMap added since); restores no-arg guavaMultiset, eclipseOrderedMap, eclipseBag,"
     " apacheMultiSet, apacheBag with the origin text; M3ObjectCounts owner overloads NEC (HUNKS.tsv)", [
         ("import com.google.common.collect.Multimaps;\n",
          "import com.google.common.collect.Multimaps;\nimport com.google.common.collect.Multiset;\n"),
         ("import java.util.Set;\nimport org.eclipse.collections.api.list.MutableList;\n",
          "import java.util.Set;\nimport org.apache.commons.collections4.Bag;\n"
          "import org.apache.commons.collections4.MultiSet;\nimport org.eclipse.collections.api.bag.MutableBag;\n"
          "import org.eclipse.collections.api.list.MutableList;\n"),
         ("import org.eclipse.collections.api.map.MutableMap;\n",
          "import org.eclipse.collections.api.map.MutableMap;\nimport org.eclipse.collections.api.map.MutableOrderedMap;\n"),
         ("import org.eclipse.collections.impl.map.mutable.MapAdapter;\n",
          "import org.eclipse.collections.impl.map.mutable.MapAdapter;\n"
          "import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;\n"),
         ("    public static <K, V> org.apache.commons.collections4.SetValuedMap<K, V> apacheSetValuedMap() {\n"
          "        return new M3ApacheSetValuedMap<>();\n    }\n",
          "    public static <K, V> org.apache.commons.collections4.SetValuedMap<K, V> apacheSetValuedMap() {\n"
          "        return new M3ApacheSetValuedMap<>();\n    }\n\n" + A01 +
          "    /** Guava count contract, with insertion-ordered distinct elements and primitive counts. */\n"
          "    public static <E> Multiset<E> guavaMultiset() { return new M3GuavaMultiset<>(); }\n\n"
          "    /** Eclipse ordered-map operations over an insertion-ordered M3 map. */\n"
          "    public static <K, V> MutableOrderedMap<K, V> eclipseOrderedMap() {\n"
          "        return OrderedMapAdapter.adapt(new M3LinkedHashMap<>());\n    }\n\n"
          "    /** Eclipse occurrence contract over the shared M3 primitive count owner. */\n"
          "    public static <E> MutableBag<E> eclipseBag() { return new M3EclipseBag<>(); }\n\n"
          "    /** Apache MultiSet old-count returns and per-element saturation over primitive counts. */\n"
          "    public static <E> MultiSet<E> apacheMultiSet() { return new M3ApacheMultiSet<>(); }\n\n"
          "    /** Legacy Apache Bag cardinality rules, including remove(element) removing all copies. */\n"
          "    @SuppressWarnings(\"deprecation\")\n"
          "    public static <E> Bag<E> apacheBag() { return new M3ApacheBag<>(); }\n"),
     ]),
    ("01", "EDIT", MAIN + "M3LibraryCollectors.java", "eaf5277e8b94678157ed52111b8e05da1129b112", O01, M01,
     "post=770be6bca11c pre=1c358f17cf8b",
     "APPLICABLE: the owner equals the origin preimage after JDK naming; the origin hunk verbatim (imports, doc line,"
     " nine collectors, uniqueBidi/putUniqueBidi) plus one anchor", [
         ("package jdk.m3.collections.interop;\n\nimport com.google.common.collect.ListMultimap;\n",
          "package jdk.m3.collections.interop;\n\nimport com.google.common.collect.BiMap;\n"
          "import com.google.common.collect.Multiset;\nimport com.google.common.collect.ListMultimap;\n"),
         ("import java.util.stream.Collector;\nimport org.eclipse.collections.api.list.MutableList;\n",
          "import java.util.stream.Collector;\nimport org.apache.commons.collections4.Bag;\n"
          "import org.apache.commons.collections4.BidiMap;\nimport org.apache.commons.collections4.ListValuedMap;\n"
          "import org.apache.commons.collections4.MultiSet;\nimport org.apache.commons.collections4.SetValuedMap;\n"
          "import org.eclipse.collections.api.bag.MutableBag;\nimport org.eclipse.collections.api.map.MutableOrderedMap;\n"
          "import org.eclipse.collections.api.list.MutableList;\n"),
         (" * Stream result-owner closure for the supported M3/JDK/Guava/Eclipse/Spring contracts.\n",
          " * Stream result-owner closure for the supported M3/JDK/Guava/Eclipse/Spring/Apache contracts.\n"),
         ("                (left, right) -> { left.addAll(right); return left; });\n    }\n}\n",
          "                (left, right) -> { left.addAll(right); return left; });\n    }\n\n" + A01 +
          "    /** Counts stream elements, retaining nulls and refusing per-element count overflow. */\n"
          "    public static <E> Collector<E, ?, Multiset<E>> toGuavaMultiset() {\n"
          "        return Collector.of(M3LibraryCollections::<E>guavaMultiset, Multiset<E>::add,\n"
          "                (left, right) -> { left.addAll(right); return left; });\n    }\n\n"
          "    /** Counts stream elements using Apache MultiSet saturation and old-count semantics. */\n"
          "    public static <E> Collector<E, ?, MultiSet<E>> toApacheMultiSet() {\n"
          "        return Collector.of(M3LibraryCollections::<E>apacheMultiSet, MultiSet<E>::add,\n"
          "                (left, right) -> {\n"
          "                    for (MultiSet.Entry<E> entry : right.entrySet()) {\n"
          "                        left.add(entry.getElement(), entry.getCount());\n"
          "                    }\n                    return left;\n                });\n    }\n\n"
          "    /** Legacy Bag collector; duplicate add returns do not affect stream accumulation. */\n"
          "    @SuppressWarnings(\"deprecation\")\n"
          "    public static <E> Collector<E, ?, Bag<E>> toApacheBag() {\n"
          "        return Collector.of(M3LibraryCollections::<E>apacheBag, Bag<E>::add,\n"
          "                (left, right) -> {\n"
          "                    for (E element : right.uniqueSet()) left.add(element, right.getCount(element));\n"
          "                    return left;\n                });\n    }\n\n"
          "    /** Counts stream elements into an M3-owned Eclipse MutableBag. */\n"
          "    public static <E> Collector<E, ?, MutableBag<E>> toEclipseBag() {\n"
          "        return Collector.of(M3LibraryCollections::<E>eclipseBag, MutableBag<E>::add,\n"
          "                (left, right) -> { right.forEachWithOccurrences(left::addOccurrences); return left; });\n"
          "    }\n\n"
          "    /** Retains value order and duplicate pairs under Apache ListValuedMap semantics. */\n"
          "    public static <T, K, V> Collector<T, ?, ListValuedMap<K, V>> toApacheListValuedMap(\n"
          "            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {\n"
          "        Objects.requireNonNull(keys, \"keys\"); Objects.requireNonNull(values, \"values\");\n"
          "        Supplier<ListValuedMap<K, V>> owner = M3LibraryCollections::apacheListValuedMap;\n"
          "        return Collector.of(owner, (map, item) -> map.put(keys.apply(item), values.apply(item)),\n"
          "                (left, right) -> { left.putAll(right); return left; });\n    }\n\n"
          "    /** Suppresses duplicate pairs under Apache SetValuedMap semantics. */\n"
          "    public static <T, K, V> Collector<T, ?, SetValuedMap<K, V>> toApacheSetValuedMap(\n"
          "            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {\n"
          "        Objects.requireNonNull(keys, \"keys\"); Objects.requireNonNull(values, \"values\");\n"
          "        Supplier<SetValuedMap<K, V>> owner = M3LibraryCollections::apacheSetValuedMap;\n"
          "        return Collector.of(owner, (map, item) -> map.put(keys.apply(item), values.apply(item)),\n"
          "                (left, right) -> { left.putAll(right); return left; });\n    }\n\n"
          "    /** Ordered Eclipse map collector; duplicate keys are refused. */\n"
          "    public static <T, K, V> Collector<T, ?, MutableOrderedMap<K, V>> toEclipseOrderedMap(\n"
          "            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {\n"
          "        return uniqueMap(keys, values, M3LibraryCollections::<K, V>eclipseOrderedMap);\n    }\n\n"
          "    /** Guava BiMap collector; duplicate keys or values are refused in every partition. */\n"
          "    public static <T, K, V> Collector<T, ?, BiMap<K, V>> toGuavaBiMap(\n"
          "            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {\n"
          "        return uniqueBidi(keys, values, M3LibraryCollections::<K, V>guavaBiMap);\n    }\n\n"
          "    /**\n"
          "     * Apache BidiMap result collector. Duplicate keys and values are refused during\n"
          "     * collection, so parallel merging cannot silently discard input mappings.\n"
          "     * Subsequent result.put calls retain Apache's ordinary displacement semantics.\n"
          "     */\n"
          "    public static <T, K, V> Collector<T, ?, BidiMap<K, V>> toApacheBidiMap(\n"
          "            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values) {\n"
          "        return uniqueBidi(keys, values, M3LibraryCollections::<K, V>apacheBidiMap);\n    }\n\n"
          "    private static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> uniqueBidi(\n"
          "            Function<? super T, ? extends K> keys, Function<? super T, ? extends V> values,\n"
          "            Supplier<M> owner) {\n"
          "        Objects.requireNonNull(keys, \"keys\"); Objects.requireNonNull(values, \"values\");\n"
          "        return Collector.of(owner,\n"
          "                (map, item) -> putUniqueBidi(map, keys.apply(item), values.apply(item)),\n"
          "                (left, right) -> { right.forEach((k, v) -> putUniqueBidi(left, k, v)); return left; });\n"
          "    }\n\n"
          "    private static <K, V> void putUniqueBidi(Map<K, V> map, K key, V value) {\n"
          "        Objects.requireNonNull(value, \"value\");\n"
          "        if (map.containsValue(value)) throw new IllegalStateException(\"duplicate value: \" + value);\n"
          "        putUnique(map, key, value);\n    }\n}\n"),
     ]),
    ("01", "EDIT", "m3/collections/interop/sdk-family-bindings.tsv", "9d3f8f15cc73ab8e7df87bf2068bcc023df027c8", O01,
     M01, "post=821ed89f52b pre=603b0a40b85",
     "MAPPED: the origin's five inventory rows of the restored no-arg factories, appended in origin order (master"
     " appended guavaBiMap/apacheBidiMap/springCaseInsensitiveMap rows since); JDK naming; m3_owners follow the master"
     " adapters' count owner M3CountedBag (c92266bb605) instead of M3ObjectCounts; Apache version follows master's"
     " pinned 4.5.0; M3ObjectCounts overload rows NEC; the TSV takes no comment line (anchor is in EDITS/HUNKS)", [
         ("\tjava.util.Map\t" + LEGACY + ".interop.M3SpringCaseInsensitiveMap\t" + LEGACY + ".M3LinkedHashMap"
          "\tM3_CREATED\t6.2.0\n",
          "\tjava.util.Map\t" + LEGACY + ".interop.M3SpringCaseInsensitiveMap\t" + LEGACY + ".M3LinkedHashMap"
          "\tM3_CREATED\t6.2.0\n"
          "GUAVA\tguavaMultiset\t\tcom.google.common.collect.Multiset\tjdk.m3.collections.interop.M3GuavaMultiset"
          "\tjdk.m3.collections.M3CountedBag;jdk.m3.collections.M3LinkedHashMap\tM3_CREATED\t33.4.8-jre\n"
          "ECLIPSE_COLLECTIONS\teclipseOrderedMap\t\torg.eclipse.collections.api.map.MutableOrderedMap"
          "\torg.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter\tjdk.m3.collections.M3LinkedHashMap"
          "\tM3_CREATED\t11.1.0\n"
          "ECLIPSE_COLLECTIONS\teclipseBag\t\torg.eclipse.collections.api.bag.MutableBag"
          "\tjdk.m3.collections.interop.M3EclipseBag\tjdk.m3.collections.M3CountedBag;jdk.m3.collections.M3LinkedHashMap"
          "\tM3_CREATED\t11.1.0\n"
          "APACHE_COMMONS_COLLECTIONS\tapacheMultiSet\t\torg.apache.commons.collections4.MultiSet"
          "\tjdk.m3.collections.interop.M3ApacheMultiSet\tjdk.m3.collections.M3CountedBag;jdk.m3.collections.M3LinkedHashMap"
          "\tM3_CREATED\t4.5.0\n"
          "APACHE_COMMONS_COLLECTIONS\tapacheBag\t\torg.apache.commons.collections4.Bag"
          "\tjdk.m3.collections.interop.M3ApacheBag\tjdk.m3.collections.M3CountedBag;jdk.m3.collections.M3LinkedHashMap"
          "\tM3_CREATED\t4.5.0\n"),
     ]),
    ("01", "EDIT", TEST + "M3SdkFamiliesTest.java", "4cfdcb18cb8417c607bb582069bfdc07975d9998", O01, M01,
     "post=c44b462d278 pre=69d9ca54a84",
     "MAPPED: the origin raised the pinned SDK factory count 22 -> 33 (+11); on master 25 -> 30 (+5 restored no-arg"
     " factories; guavaBiMap/apacheBidiMap already counted; four M3ObjectCounts overloads NEC)", [
         ("        // The canonical SDK table now declares 25 actual first-party factories.\n"
          "        assertEquals(25, M3LibraryBindings.all().size());\n"
          "        assertEquals(25, M3LibraryBindings.verifyRuntime(getClass().getClassLoader(), null));\n",
          A01.replace("    //", "        //") +
          "        // The canonical SDK table now declares 30 actual first-party factories.\n"
          "        assertEquals(30, M3LibraryBindings.all().size());\n"
          "        assertEquals(30, M3LibraryBindings.verifyRuntime(getClass().getClassLoader(), null));\n"),
     ]),
    ("01", "CREATE", TEST + "M3FiveProviderIntegrationTest.java", "904bfc73e3b3b7f04b5a5fb02ead4b7e5d614338", O01, M01,
     "post=904bfc73e3b3",
     "MAPPED from the origin's own test (absent on master): JDK naming of package/imports; the M3ObjectCounts owner"
     " test oneOwnerExposesProviderSpecificMutationReturnsAndFlags and the four null-owner asserts are NEC with the"
     " owner overloads (HUNKS.tsv), with the five imports only they used; every other line verbatim", [
         ("package " + LEGACY + ".interop;\n",
          "//FLLDCIM: MRL - mergeloss case 01: origin test of 58a72d0b2b8 (PR #1174), dropped by ancestry merge"
          " f4fbe3333946; recipe qualification/mergeloss-01-15-20261106\npackage jdk.m3.collections.interop;\n"),
         ("import com.google.common.collect.Multiset;\nimport " + LEGACY + ".M3IndexPathMasks;\n"
          "import " + LEGACY + ".M3ObjectCounts;\n", ""),
         ("import org.apache.commons.collections4.MultiSet;\nimport org.eclipse.collections.api.bag.MutableBag;\n", ""),
         ("    @Test\n    @SuppressWarnings(\"deprecation\")\n"
          "    void oneOwnerExposesProviderSpecificMutationReturnsAndFlags() {\n"
          "        M3ObjectCounts<String> owner = new M3ObjectCounts<>();\n"
          "        Multiset<String> guava = M3LibraryCollections.guavaMultiset(owner);\n"
          "        MultiSet<String> apache = M3LibraryCollections.apacheMultiSet(owner);\n"
          "        var legacy = M3LibraryCollections.apacheBag(owner);\n"
          "        MutableBag<String> eclipse = M3LibraryCollections.eclipseBag(owner);\n"
          "        assertEquals(0, guava.add(\"shared\", 3));\n"
          "        owner.setFlags(\"shared\", Integer.MIN_VALUE | 4);\n"
          "        assertEquals(3, apache.add(\"shared\", 2));\n"
          "        assertEquals(5, eclipse.occurrencesOf(\"shared\"));\n"
          "        assertFalse(legacy.add(\"shared\"));\n"
          "        assertEquals(6, guava.count(\"shared\"));\n"
          "        assertEquals(List.of(\"shared\"), owner.flaggedElements(4, 2, M3IndexPathMasks.Backend.JAVA));\n"
          "        assertTrue(guava.remove(\"shared\"));\n"
          "        assertEquals(5, owner.count(\"shared\"));\n"
          "        assertTrue(legacy.remove(\"shared\"));\n"
          "        assertTrue(guava.isEmpty());\n"
          "        assertEquals(0, owner.flags(\"shared\"));\n"
          "        eclipse.addOccurrences(null, 2);\n"
          "        assertEquals(2, apache.getCount(null));\n"
          "        owner.verifyInvariants();\n    }\n\n", ""),
         ("        assertTrue(second.isEmpty());\n"
          "        assertThrows(NullPointerException.class, () -> M3LibraryCollections.guavaMultiset(null));\n"
          "        assertThrows(NullPointerException.class, () -> M3LibraryCollections.apacheMultiSet(null));\n"
          "        assertThrows(NullPointerException.class, () -> M3LibraryCollections.apacheBag(null));\n"
          "        assertThrows(NullPointerException.class, () -> M3LibraryCollections.eclipseBag(null));\n",
          "        assertTrue(second.isEmpty());\n"),
     ]),
)


# file, this recipe's postimage sha256, successor postimage sha256, successor recipe dir, successor commit/merge
SUCCESSORS = (
    ("m3/collections/interop/sdk-family-bindings.tsv",
     "f12946941000d97a14f68b1f762b52ff03dc06427485c9c938009e8b7a6887f5",
     "97654ada7e6d7e386f8e42927ccba0c97ae6b17f7071f61cd99cf08d2f3fbaad",
     "m3/collections/interop/qualification/bindings-gating-20261107", "#2648 cbc58194a44 merged 33541216b2cd"),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def eol_of(data: bytes) -> str:
    return "\r\n" if b"\r\n" in data else "\n"


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("pinned blob %s is not in this repository (%s)" % (blob, failure)) from failure


def postimage(pre: bytes, edits, path: str) -> bytes:
    eol = eol_of(pre)
    text = pre.decode("utf-8")
    for old, new in edits:
        old, new = old.replace("\n", eol), new.replace("\n", eol)
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def chain(root: Path):
    """Pre/post image per rule; rules on one path chain (the second starts from the first's postimage)."""
    staged, plan = {}, []
    for case, kind, rel, blob, origin, merge, donors, note, edits in RULES:
        if kind == "CREATE":
            pre = git_blob(root, blob)
            if blob_sha(pre) != blob:
                raise Refused("donor blob %s does not hash to itself" % blob)
            before = b""
        elif rel in staged:
            pre = before = staged[rel]
        else:
            pre = before = git_blob(root, blob)
            if blob_sha(pre) != blob:
                raise Refused("preimage blob %s does not hash to itself" % blob)
        post = postimage(pre, edits, rel)
        staged[rel] = post
        plan.append((case, kind, rel, before, post, origin, merge, donors, note))
    return plan, staged


def run(root: Path, check: bool) -> int:
    plan, finals = chain(root)
    firsts = {}
    for case, kind, rel, before, post, *_ in plan:
        firsts.setdefault(rel, (kind, before))
    rows, writes, changed = [], [], 0
    handed = {rel: (before, after, owner, cite) for rel, before, after, owner, cite in SUCCESSORS}
    for rel, final in finals.items():
        kind, first = firsts[rel]
        target = root / rel
        on_disk = target.read_bytes() if target.exists() else None
        if rel in handed and sha256(final) != handed[rel][0]:
            raise Refused("hand-off pin of %s does not match this recipe's postimage" % rel)
        if rel in handed and on_disk is not None and sha256(on_disk) == handed[rel][1]:
            print("FILE SUPERSEDED %s by %s" % (rel, handed[rel][2]))
            continue
        if kind == "CREATE":
            if on_disk not in (None, final):
                raise Refused("CREATE target %s exists with other content (blob %s)" % (rel, blob_sha(on_disk)))
        elif on_disk is None:
            raise Refused("missing preimage %s" % rel)
        elif on_disk != final and on_disk != first:
            raise Refused("drifted preimage %s: on disk blob %s" % (rel, blob_sha(on_disk)))
        want = final
        state = "UNCHANGED" if on_disk == want else "CHANGED"
        if state == "CHANGED":
            changed += 1
            writes.append((target, want))
        print("FILE %s %s" % (state, rel))
    for case, kind, rel, before, post, origin, merge, donors, note in plan:
        rows.append((rel, sha256(before) if before else "ABSENT", sha256(post), case, origin, merge, donors,
                     kind + " " + note))
    for rel, before, after, owner, cite in SUCCESSORS:
        rows.append((rel, before, after, "01", "-", "-", "successor=" + owner.rsplit("/", 1)[-1],
                     "SUPERSEDED_BY %s (%s): successor owner of this file; this recipe's postimage is its pinned"
                     " preimage, its postimage is accepted here without a write" % (owner, cite)))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, data in writes:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("MERGELOSS_01_15 recipe=%s %s base=%s rules=%d files=%d changed=%d superseded_by=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, len(plan), len(finals), changed, len(SUCCESSORS),
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
