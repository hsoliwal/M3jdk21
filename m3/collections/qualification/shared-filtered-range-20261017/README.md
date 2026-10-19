<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 collection catalogue and shared live filtered navigation receiver

Canonical Synexia source-first recipe and tests:
https://github.com/hsoliwal/com.synexia/pull/12150
Synexia exact recipe commit: `18a6e542fa4414d13c9f4e20eef40a257ea32a0b`.
Named Java/text OpenRewrite composition:
`com.synexia.m3.Jdk21SharedFilteredNavigation20261017`.
Target baseline: `hsoliwal/M3jdk21@e913ab89056657eca4009678cfe9a657c020e79b`.
This receiver has **no canonical transformation engine or
Synexia runtime dependency**. All six generated Java/catalogue files
are byte-for-byte the Git blob IDs from the canonical Synexia crate.

## Existing Java FILE owner, not obsolete facade
`m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java`
old SHA256:
`d67a632f63647f033e71360858f8f1be1f93418880178a96437bb0605998ad5b`;
new SHA256:
`87a79889852d1890e6c9483089fa9f0869ca8f26b9cf1a82201e81a789f8e249`.
Two additive JDK differential JUnit/self-test source files are ABSENT
before. The existing `M3FilteredNavigableSet` class is already a
forwarding compatibility facade and remains **unmodified**.

One contained behavioral atom is replaced: `lower/floor/ceiling/higher`
for the existing read-only filtered navigable set. The backing
JDK `NavigableSet` is asked for an in-range candidate first;
a strictly in-range successor/predecessor view is then used to skip
rejected elements. A comparator-admitted null stored member is
distinguished from an absent candidate by comparator and actual
range endpoint evidence. Read-only mutation refusal, comparator
order, returning the stored object, nested bounds and descending
semantics remain original contracts. No iterator/node/data-entry
cache, no copied backing and no new native ABI.

## Collection family scope
The accompanying frozen inventory indexes **114** Java source
entries under 13 problem categories; exact-name tests were found for
34 sources by a deliberately narrow name-match heuristic. Source
presence does not imply behavior proof, upstream interface parity,
JDK installation, JNI qualification or performance superiority.
The separate category decisions document which of JDK, Guava,
Eclipse Collections, Apache Commons Collections, Spring Framework
and challenge taxonomy patterns are reused, compared, refused or
still open. No third-party code copied; preserve GPL/CPE,
EPL/EDL and applicable Apache notices.

## Distinct execution gates
- Existing analogous **old standalone facade** refinement had local
  strict-Java21/110325 differential assertions, but that is NOT
  evidence that the current shared-owner replacement passed.
- Exact **target** workflow compiles and executes this new owner and
  its real facade with `javac --release 21 -Xlint:all -Werror`.
- JUnit Maven whole `m3/collections` source and prior filtered
  wrapper regression tests must pass on exact PR head.
- Canonical Synexia Maven/OpenRewrite fixed-point/refusal and
  text inventory source receipts must pass on Synexia PR #12150.
- Full installed M3JDK21, JCK/jtreg, native/C17 JNI,
  JMM/linearization, GC and resource/benchmark gates remain
  **PENDING**; no new JNI symbol is required or claimed.
- No String → arrays → collections phase advancement is claimed.
  Keep receiver a draft until true head jobs pass; no rebase or
  force push and do not modify product master.
