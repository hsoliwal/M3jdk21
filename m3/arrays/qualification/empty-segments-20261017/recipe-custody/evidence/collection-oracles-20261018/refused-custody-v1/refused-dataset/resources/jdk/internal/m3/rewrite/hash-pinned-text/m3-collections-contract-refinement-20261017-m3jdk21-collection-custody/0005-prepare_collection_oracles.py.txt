#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Prepare test-only inputs for the existing qualified Maven transformation host.

This writes a separate candidate tree. It never receives or edits a target checkout.
The original Maven recipe must qualify these bytes before its receiver applies them.
"""
from pathlib import Path
import argparse
import hashlib
import json
import shutil


REVISION = "0efc1719ca5766aead09f4f7c49f5067975bd946"
TESTS = {
    "m3/collections/src/test/java/com/m3/collections/M3LongTreeSetTest.java":
        "57289895f6bf2307cf3890faa803ff8b81ec128a56e11f84df613fe65068c4cf",
    "m3/collections/src/test/java/com/m3/collections/M3StoredRepresentativeContract.java":
        "9686f4d820c5872665d6273faa0de0b327b9689b12696af7827269cb8774e016",
}
OWNERS = {
    "m3/collections/src/main/java/com/m3/collections/M3LongTreeSet.java":
        "141b90d104cf9d8715e97b6a805314fd2a8fcc9fe10d3c81b1894cf744141d42",
    "m3/collections/src/main/java/com/m3/collections/M3LongSortedViews.java":
        "7a42630fbffd2c5875d8a7e4258540454879117a52bd6fe52c3cde4ce07b4d99",
    "m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java":
        "4a109baf88457d625fa8fc259ee8e7908340f5436cfea77dc5ac554924045b8d",
}
QUALIFICATION = "m3/collections/qualification/oracle-repair-20261018"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def replace_once(text, before, after):
    if text.count(before) != 1:
        raise ValueError("expected exactly one reviewed behavioral atom: " + before[:100])
    return text.replace(before, after, 1)


def refine_tree_test(text):
    # Build the independent JDK oracle from the same admitted members, then use
    # its ordinary removal contract. Keep every original M3 growth/merge check.
    text = replace_once(text,
        """    void growthSplitsPagesAndRemovalMergesAndTrimsThem() {
        M3LongTreeSet set = new M3LongTreeSet();
""",
        """    void growthSplitsPagesAndRemovalMergesAndTrimsThem() {
        M3LongTreeSet set = new M3LongTreeSet();
        TreeSet<Long> expected = new TreeSet<>();
""")
    for expression in ("i * 7", "-i * 7"):
        text = replace_once(text,
            "            assertTrue(set.add(" + expression + "));\n",
            "            assertTrue(set.add(" + expression + "));\n"
            "            assertTrue(expected.add(" + expression + "));\n")
        text = replace_once(text,
            "                assertTrue(set.remove(" + expression + "));\n",
            "                assertTrue(set.remove(" + expression + "));\n"
            "                assertTrue(expected.remove(" + expression + "));\n")
    text = replace_once(text,
        """        for (int i = 1; i < survivors.length; i++) {
            assertEquals(survivors[i - 1] + 21L, survivors[i]);
        }
""",
        """        // Zero was never inserted: the central ordered pair is -21, 21.
        assertArrayEquals(order(expected), survivors);
        assertFalse(set.contains(0L));
""")
    text = replace_once(text,
        """        assertEquals(3, set.pageCount());
        RuntimeException boom = new RuntimeException("boom");
""",
        """        // Insert-only half splits keep these pages between half full and full.
        assertTrue((long) set.pageCount() * set.pageCapacity() >= set.size());
        assertTrue((long) set.pageCount() * (set.pageCapacity() / 2) <= set.size());
        RuntimeException boom = new RuntimeException("boom");
""")
    text = replace_once(text,
        """        assertTrue(sub.add(7));
        assertTrue(jdk.add(7L));
""",
        """        assertFalse(sub.add(7));
        assertFalse(jdk.add(7L));
        assertTrue(sub.add(8));
        assertTrue(jdk.add(8L));
""")
    return text


def refine_representative_test(text):
    return replace_once(text,
        """        duplicates.remove(visible);
        no(projected.contains(new Key("same", true)), "last visible equals member removed");
""",
        """        // remove(Object) would remove the earlier hidden, equals-equal member.
        // Select the visible reference explicitly while keeping the Collection overload.
        var cursor = duplicates.iterator();
        while (cursor.hasNext()) {
            if (cursor.next() == visible) {
                cursor.remove();
                break;
            }
        }
        eq(1, duplicates.size(), "only the selected visible reference is removed");
        yes(duplicates.iterator().next() == hidden, "hidden equals-equal reference remains");
        no(projected.contains(new Key("same", true)), "last visible equals member removed");
""")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--witness", type=Path, required=True)
    parser.add_argument("--witness-receipt", type=Path, required=True)
    parser.add_argument("--baseline-receipt", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    baseline = args.baseline.resolve()
    output = args.output.resolve()
    if output.exists():
        raise ValueError("fresh, separate candidate tree required")
    for path, digest in {**TESTS, **OWNERS}.items():
        if sha(baseline / path) != digest:
            raise ValueError("baseline source drift: " + path)
    witness = json.loads(args.witness_receipt.read_text())
    if witness["status"] != "PASS" or witness["source_sha256"] != sha(args.witness):
        raise ValueError("matching executable independent witness required")
    original_run = json.loads(args.baseline_receipt.read_text())
    if original_run["sourceCommit"] != REVISION or original_run["tests"] != {
            "tests": 225, "failures": 4, "errors": 0, "skipped": 0}:
        raise ValueError("retain and investigate the exact original baseline failures")
    output.mkdir(parents=True)
    targets = {}
    for index, (path, digest) in enumerate(TESTS.items()):
        original = (baseline / path).read_text()
        changed = refine_tree_test(original) if index == 0 else refine_representative_test(original)
        target = output / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(changed)
        targets[path] = {"before_sha256": digest, "after_sha256": sha(target)}
    qualification = output / QUALIFICATION
    source = qualification / "src/test/java/CollectionOracleWitness.java"
    source.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(args.witness, source)
    cases = [
        {
            "id": "M3-ORACLE-SIGNED-SURVIVORS", "category": "SORTED_MEMBERSHIP_AND_ORDER",
            "operation": "M3LongTreeSetTest.growthSplitsPagesAndRemovalMergesAndTrimsThem",
            "baseline_failure": "expected 0 but was 21 at the -21-to-21 transition",
            "rejected_oracle": "a single arithmetic progression through zero, which was never inserted",
            "selected_oracle": "TreeSet receives and removes the same members; compare the full ordered array and explicitly exclude zero",
            "coverage_preserved": "200000 insertions, 133334 removals, 66666 survivors, ends, page merges, trim, navigation, clear and reuse",
        },
        {
            "id": "M3-ORACLE-PAGE-OCCUPANCY", "category": "DYNAMIC_ARRAY_PAGE_SPLIT_INVARIANT",
            "operation": "M3LongTreeSetTest.filterInPlaceCommitsPriorRejectionsAndKeepsTheThrowingSuffix",
            "baseline_failure": "expected three pages but had four before the predicate ran",
            "rejected_oracle": "ceil(size / capacity) assumes packed pages after documented half splits",
            "selected_oracle": "pageCount * capacity >= size and pageCount * floor(capacity/2) <= size for this insertion-only fixture",
            "coverage_preserved": "partial commit before predicate throw, exact throwing suffix, exception identity, removed counts, fail-fast and reuse",
        },
        {
            "id": "M3-ORACLE-BOUNDED-ADD", "category": "SORTED_VIEW_SET_MEMBERSHIP",
            "operation": "M3LongTreeSetTest.sortedViewsAndUnionsWorkOverTheOwnerAsBackingSet",
            "baseline_failure": "add(7) correctly returned false because -200 + 3*69 equals 7",
            "rejected_oracle": "treating an existing bounded member as absent",
            "selected_oracle": "assert duplicate add(7) false in both views; assert new bounded add(8) true in both views",
            "coverage_preserved": "both duplicate and successful admission, range rejection, removals, clear, navigation, unmodifiable and union behavior",
        },
        {
            "id": "M3-ORACLE-STORED-REFERENCE", "category": "FILTERED_STORED_REPRESENTATIVE_IDENTITY",
            "operation": "M3StoredRepresentativeContract.identityAndDuplicateMembers",
            "baseline_failure": "fixture removed the first equals member (hidden), leaving the visible reference present",
            "rejected_oracle": "Collection.remove(Object) as an identity removal operation",
            "selected_oracle": "iterator removal selected with reference equality; assert exactly the hidden reference remains before last-visible lookup",
            "coverage_preserved": "Collection overload, distinct equals-equal hidden/visible members, identity-owned sets, foreign identity exclusion and last-visible removal semantics",
        },
    ]
    catalogue = {
        "schema": "M3_COLLECTION_TEST_ORACLE_REFINEMENT_V1",
        "baseline_repository": "hsoliwal/M3jdk21", "baseline_revision": REVISION,
        "baseline_failures": original_run["tests"],
        "baseline_receipt_sha256": sha(args.baseline_receipt),
        "source_targets": targets, "unchanged_production_owners": OWNERS,
        "production_changes": 0, "public_interface_changes": 0, "jni_changes": 0,
        "algorithm_selection": "reuse the existing first-party M3 owners and JDK collection contracts; repair the four test premises demonstrated independently",
        "cases": cases,
        "existing_catalogues_consulted": [
            "m3/collections/algorithm-catalog/full-m3-family-inventory-20261017/COLLECTION_SOURCE_INVENTORY.tsv",
            "m3/collections/algorithm-catalog/collection-catalogue-input-provenance-20261017.tsv",
            "m3/backports/ALGORITHM_CATALOGUE.tsv",
        ],
        "primary_contract_sources": [
            "src/java.base/share/classes/java/util/ArrayList.java: remove(Object) and iterator removal",
            "src/java.base/share/classes/java/util/TreeSet.java: add and subSet",
            "m3/collections/src/main/java/com/m3/collections/M3LongTreeSet.java: documented half split and page capacity",
        ],
        "donor_evaluation": [
            {"candidate": "existing M3LongTreeSet / M3LongSortedViews / M3FilteredViews", "license": "Apache-2.0", "choice": "retain exact production bytes; no defect established by these failures"},
            {"candidate": "JDK TreeSet / ArrayList", "license": "GPL-2.0-with-Classpath-exception", "choice": "call public APIs as independent test oracles; no implementation source copied"},
            {"candidate": "existing Synexia PackedLongSortedSet / PackedLongPagedSortedSet lineage", "license": "Apache-2.0", "choice": "reuse current documented half-split contract; no new production donor adaptation"},
            {"candidate": "existing GitHub / LeetCode / HackerRank / GeeksforGeeks catalogue", "license": "reference-only; individual donor licenses remain applicable", "choice": "reuse prior category discovery; no new algorithm replacement is justified by a mistaken test fixture"},
        ],
        "complexity": {"production": "unchanged", "retained_production_state": "unchanged", "test_tree_oracle": "O(n log n) time and O(n) test-only space", "test_reference_removal": "O(n) scan and O(1) auxiliary space"},
        "independent_witness": {"path": QUALIFICATION + "/src/test/java/CollectionOracleWitness.java", "source_sha256": sha(args.witness), "receipt_sha256": sha(args.witness_receipt), "status": "PASS", "production_dependencies": []},
        "required_qualification": ["original Maven SDK five tests, eight sealing tests and nine receiving cases", "entire original collections Maven suite after receiving; no disabled tests", "unchanged production owner fingerprints"],
        "claim_limit": "These repairs qualify the test premises. They do not establish complete SDK parity, application performance, native equivalence, or a new production algorithm improvement.",
    }
    (qualification / "CATALOGUE.json").write_text(json.dumps(catalogue, indent=2) + "\n")
    index = output / "m3/collections/algorithm-catalog/collection-oracle-repair-20261018.tsv"
    index.parent.mkdir(parents=True, exist_ok=True)
    columns = ["id", "category", "operation", "selection", "evidence", "production_changes"]
    lines = ["\t".join(columns)]
    for case in cases:
        lines.append("\t".join([case["id"], case["category"], case["operation"], case["selected_oracle"], QUALIFICATION + "/CATALOGUE.json", "0"]))
    index.write_text("\n".join(lines) + "\n")
    (qualification / "README.md").write_text("""<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Collection oracle repairs

The original collections Maven run at `0efc1719ca5766aead09f4f7c49f5067975bd946`
ran 225 tests, with four failures and no errors or skipped tests. Independent
Java 21 JDK and arithmetic witnesses established four mistaken test premises.
This recipe corrects those premises and preserves the three production owners.

The signed growth fixture never inserts zero. Its complete survivor order is now
compared to `TreeSet`, with an explicit zero-absence assertion. The insertion-only
page fixture checks half-full/full occupancy bounds while retaining every later
predicate-exception and partial-commit assertion. Bounded view admission checks
both an existing member (7) and an absent in-range member (8). The equal-member
fixture selects the visible reference through its iterator and verifies that the
hidden reference remains before asserting that the last visible member is gone.

`CATALOGUE.json` records the four operation categories, rejected and selected
oracles, original source hashes, unchanged production hashes, donor evaluation,
coverage, and evidence boundaries. The searchable module catalogue index is
`m3/collections/algorithm-catalog/collection-oracle-repair-20261018.tsv`.

The independent witness needs only Java 21 and no M3 classes. From the repository
root, use an isolated output directory:

```sh
mkdir -p /tmp/m3-collection-oracle-classes
javac -Xlint:all -Werror -d /tmp/m3-collection-oracle-classes m3/collections/qualification/oracle-repair-20261018/src/test/java/CollectionOracleWitness.java
java -Xmx128m -cp /tmp/m3-collection-oracle-classes CollectionOracleWitness
```

The canonical original Maven transformation host seals and qualifies the
`m3jdk21-collection-oracles` dataset with its unchanged five SDK tests, eight
sealing tests and nine receiver cases. The original `m3/collections/pom.xml`
suite must run after receiving. No production behavior, public interface,
predicate callback order, range contract, equality rule, or JNI implementation
is changed by this test-only recipe. Original failing sources and reports are
retained separately in recipe custody; no historical report is a fresh pass.
""")
    print(json.dumps({"kind": "RECIPE_INPUT_ONLY", "production_targets": 0,
        "targets": sorted(path.relative_to(output).as_posix() for path in output.rglob("*") if path.is_file())}))


if __name__ == "__main__":
    main()
