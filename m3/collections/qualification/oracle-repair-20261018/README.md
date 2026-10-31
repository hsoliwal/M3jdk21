<!--
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
