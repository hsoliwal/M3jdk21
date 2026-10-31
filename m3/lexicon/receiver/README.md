<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3Lexicons receiver (scaffold, 2026-10-18)

Thin M3JDK21 receiver for the Synexia lexicon lane ("m3jdk ... lexicons and all"). Package `com.m3.lexicon`,
Java 21, no runtime dependencies. Canonical source: Synexia crate
`synexia-openrewrite-recipes/crates/m3-lexicons-receiver-v1` (hash-pinned postimages of these files).
Ledger: `m3/lexicon/LEXICON_RECEIVER_LEDGER.tsv`; data rows: `m3/lexicon/synexia-lexicon-data-manifest-20261018.tsv`;
design: `m3/docs/m3-lexicons-separator-wordid-20261018.md`.

| class | role |
| --- | --- |
| `M3LexiconFormat` | every known image magic (M3LEX001, SYNARR01, MIWROWS1, SYNDICT1, SYNTULP1, SYNSEMG1, SNXSTR01, SNXLEX01, M3PAIR01, M3LIMG01, M3GEO001 + proposed M3FOLD01, M3RXW001, M3SEPB01), header gate |
| `M3Lex001Image` | Java writer + verifier for the `-Djdk.mindex.lexicon` image (same admission rules as `java.lang.M3StringPool`), `synexia.shards.tsv` row for `verify-synexia-lexicon.py` |
| `M3SharedArenaVerifier` | SYNARR01 snapshot verifier (CRC32C, COMMIT01 trailer, tuples, Java hashes) |
| `M3LexiconWordIds` | canonical word id (CWID = row + 1) inside `M3StringAtom.canonicalId`; SYNDICT1 and MIWROWS1 id decoders |
| `M3LexiconFoldIds` | folded-variant ids (ASCII lower, Unicode simple fold = `equalsIgnoreCase`) |
| `M3WindowBoundarySummary` | CAMEL / PASCAL boundary monoid (window 3, seam rule) |
| `M3CsvBoundarySummary` | RFC 4180 separator monoid with quote state across atom seams |
| `M3PunctBoundarySummary` | Unicode P* punctuation separator monoid (surrogate pairs across seams) |
| `M3SeparatorLexicon`, `M3AtomCursor` | composition over atoms and token spans; never joins atoms |
| `M3WordIdSequence` | token spans -> CWID lanes (SoA) |
| `M3RegexWordTransferRows` | (pattern, CWID) DFA transfer rows, M3RXW001 writer/reader |

Gate: `mvn -o -B -ntp -f m3/lexicon/receiver/pom.xml verify` compiles with `-Xlint:all -Werror` and runs
`M3LexiconReceiverProof` (differential vs Commons Lang `splitByCharacterTypeCamelCase`, Commons CSV RFC 4180,
`Character.getType`, `String.equalsIgnoreCase`, direct DFA runs; corruption refusals for every verifier).
Commons Lang/CSV are test-scope oracles only; their sources are vendored under `m3/vendor/third_party`.

Not claimed: java.base binding (M3StringAtom/M3StringTuple), jtreg, data images (manifest rows only), body
readers for SYNDICT1/SNXSTR01/SNXLEX01/M3PAIR01/M3LIMG01/M3GEO001, R6 DFA serialization (pattern key is
caller-supplied), Jackson CSV differential.
