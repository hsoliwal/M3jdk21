<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/lexicon: interned-language token substrate (leaf interned-language-20261106)

Five types added to the receiver module `com.m3:m3-lexicon-receiver:0.1.0-SNAPSHOT` (package `jdk.m3.lexicon`,
automatic module name `org.m3jdk.lexicon`), Java 21, no dependency outside `java.base`. Fourth leaf of the m3scale
convergence plan (`C:/m3-superset/conv/plans/m3scale.tsv`, rank 4 `m3-interned-language`, decision ABSORB).

## What it is

| Type | Role |
|---|---|
| `M3TokenId` | 64-bit packed token id: `pack(languageId 0..0xffff, wordId >= 0) = language << 32 \| unsigned word`; `languageId`, `wordId`, `isSymbol` (`SYMBOL_LANGUAGE_ID` 0xfffe), `isOverflow` (`OVERFLOW_LANGUAGE_ID` 0xffff) |
| `M3PackedText` | immutable `long[]` paragraph leaf of token ids: defensive clones, cached `Arrays.hashCode`, `token(i)`, `toArray()`, `forEach(LongConsumer)`, content equality |
| `M3TokenRope` | sealed rope over packed texts: `record Leaf(text)`, `record Concat(left, right, tokenCount, fingerprint)`; `concat` is O(1) (`Math.addExact` count, mixed FNV fingerprint), `forEach` is iterative |
| `M3OverflowInterner` | process-local escape hatch for strings absent from immutable dictionaries: `intern(sourceLanguageId, raw)` NFC-normalizes and returns an OVERFLOW token with a dense id, `entry(id)`, `size()`; synchronized |
| `M3SharedSymbols` | 22 process-independent symbol tokens (`SPACE`, `NEWLINE`, `TAB` first; append-only ids): `find(String)`, `resolve(long)`, `size()` |

The mapped-lexicon part of the donor module (`MappedLexicon`, `Utf8LexiconCompiler`, `MappedConceptIndex`,
`NativeLexicon` and its JNI) is the lexicon-index lane's (`C:/m3-superset/conv/plans/lexicon-index.tsv`) and is a
`SKIP` row here; `m3/lexicon/native` is untouched.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/synexia-m3scale-apache2` develop `3385d79a2968c770aedf1d3398b2a62f05ebc092` (2026-11-06); the substrate was written 2026-09-23 (28fcd46a `feat(intern): add packed multilingual token runtime`, caa93bb2 / bb1c7099 `separate shared symbols from overflow ids`, f0e43961 `keep common separators out of overflow strings`), frozen since, byte-restored by 99f80871 (2026-10-27) |
| donor files | `m3-interned-language/src/main/java/com/synexia/m3scale/intern/{TokenId,PackedText,TokenRope,OverflowInterner,SharedSymbols}.java` (blobs `a0739662f208`, `b13b076cb382`, `a9846f166598`, `6497b08b21d5`, `7788fc08cd7a`); the module has no pom and is in no donor reactor |
| recipe | `relocate.py` v1 (`RELOCATE_INTERNED_LANGUAGE`): `python -I m3/lexicon/recipe/interned-language-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--check]` |
| map | `RELOCATION_MAP.tsv`: 1 `REPO`, 5 `FILE`, 1 `PACKAGE` (`com.synexia.m3scale.intern -> jdk.m3.lexicon`), 5 `TYPE` (M3 prefix; nested `Leaf`, `Concat`, `Entry`, `Key` keep their names), 6 `SKIP`, 4 `NEC` |
| provenance | `PROVENANCE.tsv`: donor commit, blob, sha256, URL, target sha256, counts of renamed / mapped / dropped lines, `body_match` per file |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/lexicon/`: 0 moves, 0 rewrites, 0 refusals |

The recipe rewrites only the license header (the receiver's two `//` lines), the `package` line, mapped `import`
lines (none here: the five files import `java.*` only), the declared type names (whole words, on every line) and
inserts one `//FLLDCIM: RELOCATE` anchor (donor blob and commit; repository, path and URL are in `PROVENANCE.tsv`)
before each top-level type. `body_match=TRUE` for all 5 files; 0 dropped lines; idempotent (`--check` after the run
and after `jdk_naming.py`: `changed=0`). Blobs are read with `git cat-file` (LF), never from the autocrlf checkout.

The donor's own `LexiconSelfTest` (blob `0efa44a53b9d`) does not compile against the donor tree: it imports
`LanguageRegistry`, `InternedTextRuntime` and `ConceptIndex`, deleted on 2026-09-23 (28bf3c9c, d0fe2f6d, a2622d70)
and never restored. Its seven substrate checks are carried by `M3InternedLanguageSubstrateTest`; the lexicon checks
belong to the lexicon-index lane. Donor follow-up: restore or delete the test.

## NEC rows (a human decides equivalence, nothing overwritten)

| Donor | M3JDK / com.synexia owner | Note |
|---|---|---|
| `TokenId` (blob `a0739662f208`) -> `M3TokenId` | `jdk.m3.lexicon.M3LexiconWordIds` (blob `12b648c00a63`) | three language+word packings in one package: `language << 32 \| word` with SYMBOL/OVERFLOW ids; SYNDICT1 `((language + 1) << 25) \| (word + 1)` (int, language < 100); MIWROWS1 `indexId >>> 48` with overlay bit 47 and a 47-bit local coordinate. Image formats pin the layouts; one owner is a decision |
| `SharedSymbols` (blob `7788fc08cd7a`) -> `M3SharedSymbols` | `jdk.m3.lexicon.M3SeparatorLexicon` (blob `121339158936`) with `M3SeparatorKind` | fixed symbol tokens with persistent ids vs boundary monoids over atoms with no id table |
| `PackedText` (blob `b13b076cb382`) -> `M3PackedText` | com.synexia develop `ac5feec00a6`: `synexia-dictlang/.../com/dictlang/packed/PackedText.java` (blob `e1080db508b3`); `synexia-interned-language` (33 types under `com.synexia.language`, none of the five) | same simple name, different class (FixedLexicon snapshot with sha256); com.synexia's interned-language module is a different lineage, m3scale is the only live copy |
| `PackedText` (blob `b13b076cb382`) -> `M3PackedText` | `jdk.m3.lexicon.M3WordIdSequence` (blob `fa374db5e7fc`) | token spans -> int CWID lanes vs packed 64-bit token ids without spans; a bridge is a later leaf |

## Build

| Step | Command |
|---|---|
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/lexicon/receiver/pom.xml package` (`-Xlint:all -Werror`, `-proc:none`; surefire 3.5.4 runs `M3InternedLanguageSubstrateTest`; the receiver's `*Proof` mains stay on `exec:java` in `verify`) |
| from the control reactor | `mvn -f m3/pom.xml -pl lexicon/receiver package` (`lexicon/receiver` is now a default module of `m3/pom.xml`) |
| differential | `bash m3/lexicon/recipe/interned-language-20261106/differential.sh <donor clone> <rev> <scratch> [cases]` after `package`: compiles the five donor sources from the donor tree, replays the same seeded operations through both sides with `M3InternedLanguageDump` (LF output, nothing identity-dependent printed), normalizes the donor dump by the map, compares byte for byte and compares `javap -p` of every class |
| all gates | `bash m3/lexicon/recipe/interned-language-20261106/verify.sh <donor clone> <rev> <scratch> [cases]` |

Consumers depend on the module with

```xml
<dependency>
  <groupId>com.m3</groupId>
  <artifactId>m3-lexicon-receiver</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

and `requires org.m3jdk.lexicon;` on the module path (automatic module; a `module-info.java` for the receiver is a
later decision, see `RECIPE_MEMORY.tsv`).

## Gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 1 `mvn package`, JDK 21.0.9, `-Xlint:all -Werror` | BUILD SUCCESS; 22 main + 6 test sources; jar with 34 class entries (9 new) |
| 2 substrate test in the test phase | 5 tests, 0 failures; figures: 7 donor self-test checks, 196,608 token round trips (65,536 languages x 3 word boundaries) + 3 refusals, 10,000 rope leaves in 26 trees (depth <= 12, 34,910 tokens), 100,000 overflow strings across 3 languages (88,397 entries, 11,606 duplicates), 22 shared symbols round-tripped |
| 3 differential donor vs relocated | 2,227,522 cases (5 labels x 100,000 seeded operations + boundary probes), 323,909 identical exceptions, largest rope 4,096 tokens; dump 2,227,523 lines / 125,017,160 bytes; sha256 donor raw = donor normalized = relocated `5465badcc21a26794f6ab8d605423e0db3445f05aa3e007ebed5a92884f93b55` |
| 3b `javap -p` parity | 9 of 9 class files identical |
| 4 `jar --describe-module` | `org.m3jdk.lexicon@0.1.0-SNAPSHOT automatic`, contains `jdk.m3.lexicon` |
| 5 `jdk_naming.py apply --only m3/lexicon/` | 0 moves, 0 rewrites, 0 refusals |
| 6 `CONVERGE_COLLECTIONS.tsv`, `LEXICON_RECEIVER_LEDGER.tsv` | OverflowInterner GAP -> PRESENT_IN_M3JDK, 4 rows added, MappedConceptIndex stays GAP; ledger row IL01 |
| 7 `m3/pom.xml` default module | `lexicon/receiver` after `algorithms` |
| 8 scope | `m3/lexicon/**`, one `m3/pom.xml` line, the CONVERGE rows |

Deviation from the brief, recorded: the brief expected an orphan `m3/lexicon` without a pom; master already carries
`m3/lexicon/receiver/pom.xml` (c9a1d41baaf), so no second pom was created, gate 1 runs on the receiver pom, and the
receiver is registered in `m3/pom.xml`. Hard-limit notes for a later pass, recorded not fixed: donor bodies use `for`
loops (`M3PackedText.forEach`, `M3TokenRope.Leaf.fingerprint`, `M3SharedSymbols.find` linear scan over 22 entries);
`M3OverflowInterner` boxes through `HashMap<Key, Integer>` and `ArrayList<Entry>` (collections culprit, HP-LAW review
later). Lessons of the run are in `RECIPE_MEMORY.tsv`.

## Files

| File | Purpose |
|---|---|
| `../../receiver/pom.xml` | the existing Maven module; this leaf adds the JUnit test dependency, surefire and the `Automatic-Module-Name` manifest entry |
| `../../receiver/src/main/java/jdk/m3/lexicon/M3{TokenId,PackedText,TokenRope,OverflowInterner,SharedSymbols}.java` | the five relocated types (recipe output) |
| `../../receiver/src/test/java/jdk/m3/lexicon/M3InternedLanguageSubstrateTest.java` | gate 2 (JUnit 5): donor self-test substrate checks + plan inputs |
| `../../receiver/src/test/java/jdk/m3/lexicon/M3InternedLanguageDump.java` | reflective dump driver of the differential gate (not a test) |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its map and its pins |
| `differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
| `../../LEXICON_RECEIVER_LEDGER.tsv` | receiver ledger, row IL01 |
