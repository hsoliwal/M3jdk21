# String lane handover: M3String, java.lang.String and the precompute

Written 2026-11-16. Read this before touching any string code in M3JDK. This file lives in M3jdk21 by the owner's decision of that day (records and tools converge into M3JDK too); the run record is in `record/` beside it. It holds the invariants the owner has
set, the measured state, what landed, and the next leaves. Run record with data, tools and proofs:
`doc/m3/string-lane/record/`.

Every statement below is marked by how it is known:

- **[V]** verified on 2026-11-16 by a command whose output is in the run record.
- **[N]** carried from session notes of the date given; not re-verified on the current master. Re-check before
  relying on it.
- **[U]** the owner's words (quoted) or a rule the owner stated on that date.

## 1. What the owner asked for

| Date | Words | Meaning for this lane |
|---|---|---|
| 2026-11-16 | "M3 string is extremely crucial" (said five times in one session) | The string lane outranks the collection lanes when they compete. |
| 2026-11-16 | "everything must converge to M3jdk M3string and String related" | Every string-related source in com.synexia, the private repositories and the forks ends in the M3JDK string owners. |
| 2026-11-16 | "also enhance the pre compute" | Precompute growth is a standing part of the lane, leaf after leaf. |
| 2026-11-16 | "is m3ast also connected?" | See section 6: it is not. Connecting it is an open leaf. |
| 2026-11-16 | "commit all push all merge all prs" | Finished leaves are pushed, opened as PRs and merged the same session. A PR merged before green checks is fixed forward. |
| 2026-11-16 | "keep enhancing with leet code and other algorithms" (twice) | Known algorithms replace quadratic or per-unit code in the string owners, each with a differential test. |
| 2026-11-16 | "String build, string, all of string operations and even m3jdk compiler optimizations" | Scope: StringBuilder, StringBuffer, every String operation, javac string optimizations. |
| 2026-11-16 | "we must optimize and regex operations" (three times), "regex" | Regex on M3-backed input is part of the lane (invariants I3 and I9). |
| 2026-11-16 | "consider all the enhancements that string utils normally have" | StringUtils-family operations: internal fast paths behind existing operations are leaves; new public operations need the owner's admission (I7). |
| 2026-11-16 | "also m3 jdk reader and writer can leverage this" | java.io readers and writers, encoders and decoders use the composite and its measures. |
| 2026-11-16 | "com.synexia and all the private repos also need to converge to m3jdk eventually" | Standing convergence goal (ledger row PRIVATE-CONVERGE). |
| 2026-11-16 | "forks lucene and opennlp using m3 string structure as jmods in m3JDK" (four times) | The forks hsoliwal/lucene and hsoliwal/opennlp use M3 string structures and ship as modules of the M3JDK image (ledger row LUCENE-OPENNLP-JMODS). |
| 2026-11-16 | "additionally parsers using m3string and m3ast" (twice) | Parsers (javac, JDK text parsers, library parsers) use M3String and the one M3AST. |
| 2026-11-16 | "maven can benefit hugely, there is lp4j as well" (sent three times) | Maven (fork hsoliwal/maven) is a named consumer: its String use ranks the leaves (section 5b). "lp4j" is not a name in any of the owner's repositories. Readings covered: SLF4J (used by Maven in 114 files and vendored in M3JDK: the most likely) and Eclipse LSP4J. Log4j is used nowhere in the Maven fork or M3JDK. The owner has not said which. |
| 2026-11-16 | "univec"; asked what it means: "ALL of the above, whatever is best and works for m3string and m3jdk" | No repository holds that name. The options offered were vectorized Unicode kernels, UnicodeX rank-coded words as an owner profile, and vector measures for similarity. All three are in scope. |
| 2026-10-17 | "composite processing of java.lang.String in M3jdk21 is the most crucial function" | Rank each leaf by what it adds to composite String processing. |
| 2026-10-17 | "String becomes a composite of M3Strings, and regex must achieve the maximum mechanical match before using char arrays from JNI" | Invariants I2 and I3. |
| 2026-10-19 | "this is M3Jdk21 not m3 + jdk .. the integration is not some layer" | Invariant I1. |

## 1b. What converges into M3jdk21 (owner, 2026-11-16, question dialog: all four)

The owner said "this is supposed to converge to m3jdk" repeatedly that evening and then chose every option
offered:

1. **Library source trees.** The whole source of the forked libraries (commons-lang, commons-text, commons-io,
   Jackson, univocity-parsers, antlr4, javaparser) comes into M3jdk21 as modules built with the JDK and rewritten
   onto M3 String, the way Guava is being inlined. Absorbing single operations into `java.lang.String` (leaves 11
   and 15) continues, but it is not the whole of it.
2. **Everything under `m3/`.** The parser pack, the compile-time port, the vendored mindex, the M3AST copies and
   the vendored third-party copies move into real JDK modules under `src/`, one owner each.
3. **The com.synexia string code.** `synexia-indexstring` and the related modules (1,813 types: 25 already in
   M3JDK, 495 partly) are brought into M3jdk21.
4. **Records and tools.** Done for this lane on the same day: this folder.

State: 4 is done. 1, 2 and 3 are not started as moves. Each needs the JDK build to prove it (module descriptors,
make glue, the importers of every moved type), which no host of that day could run, and each deletes or replaces
tracked files, which the owner confirms per batch. Evidence gathered so far: section 5 (routes for 3), section 5c
(how a library becomes a module), section 5d (the forks and the pinned vendored copies), section 5f (the M3AST
families).

**Landed after that (2026-11-17): item 2 started with `m3/ports/precompute`.** Owner's choice in a question
dialog: java.base with the package name kept. Its 68 classes are now `src/java.base/share/classes/jdk/m3/precompute`
(not exported) and its 50 JUnit tests `test/jdk/jdk/m3/precompute`; `m3/ports/precompute/pom.xml`
builds the same artifact from there, and the natives, verify scripts and records stay in that folder for
now. The two current targets of `synexia.precompute.ncm-codebook` in `m3/docs/name-mapping.json` follow
the move (its lineage keeps the old paths; pin in `m3/tooling/name-contract/name-plan.json` updated).
Next in item 2: the natives into
`src/java.base/share/native/libm3precompute`, then `m3/ports/indexstring` (16 of its 20 classes compile in
java.base as they are; 4 take `jdk.m3.collections.ProgressMonitor`, which java.base cannot see, and the
public-names check lists `jdk.internal.m3.indexstring` as a donor namespace).

**Then `m3/ports/indexstring` (same day).** Owner's choices in question dialogs: package name kept
(`jdk.internal.m3.indexstring` + `.type`, 19 classes into java.base by renames); `check_public_names.py` no
longer lists it as a donor namespace; the four classes that took `jdk.m3.collections.ProgressMonitor` take
java.base's `java.util.concurrent.ProgressMonitor`, with a never-cancelling default when the caller passes
none; the port's `pom.xml` is kept and documented as no longer building. The smoke tests moved to
`test/jdk/jdk/internal/m3/indexstring` and run in `make/m3/indexstring/test-java-base.sh`
(workflow `m3-java-base-internals`). `jdk.m3.text.compat.M3Text` stays in the port (a new java.base
package was not admitted).

**Then `m3/core` (module `jdk.m3.text`, same day).** Owner's choice in a question dialog: java.base with the
package name kept (I had recommended its own module `src/jdk.m3.text`, which the licence-intake verifier
plans). Its 26 classes are now `src/java.base/share/classes/jdk/m3/text` (not exported), `module-info.java` is removed
(java.base owns the package), the MIT notice of tools4j shortstring is `src/java.base/share/legal/shortstring.md`.
The tests stay in `m3/core/test` for now and the workflows compile the moved sources from java.base;
record `synexia.lexicons.related-source-range` follows with a `REVIEW_REQUIRED` direct-copy row.

**Lucene and OpenNLP (2026-11-17).** Owner's choices in question dialogs: ONE module `jdk.m3.search` carries them
with their upstream packages in `src/jdk.m3.search/providers/classes` (the Guava-in-`jdk.m3.collections` layout),
landed unchanged first, then moved onto M3 String one text carrier per leaf. The module is not resolved by default
(`make/CreateJmods.gmk`), so it never shadows an application's own Lucene. First PR: Lucene 9.10.0 core, queries,
sandbox, queryparser through the provider pipeline of the Guava inline (bytecode identical to the vendored sources;
the Java 19-21 multi-release sources are not carried), #3213. Second PR: Lucene analysis-common (with the Snowball
stemmers) and analysis-opennlp, OpenNLP Tools 2.4.0, and the SLF4J API 2.0.12 OpenNLP needs, relocated into the
concealed package `jdk.internal.search.slf4j` (owner's choice); deprecation lint is off for the module because
its descriptor provides factories Lucene deprecates. Next: the M3 String leaves (Lucene CharTermAttribute and
analyzers, OpenNLP tokenizer and sentence spans).

## 2. Invariants

Break one of these and the leaf is wrong, whatever its tests say.

**I1. M3JDK is the JDK. [U 2026-10-19]** `java.lang.String`, `StringBuilder`, `StringBuffer`, `java.util.regex`,
java.base and HotSpot are changed in place. Never put an M3 representation beside `String` as a separate layer.
A change that needs VM cooperation (field layout, intrinsics, JNI shadows, CDS) carries its VM change in the same
leaf.

**I2. A String is a composite of M3Strings. [U 2026-10-17]** concat, join, repeat, substring and replace compose
tuple and atom nodes. A flat `byte[]` or `char[]` is a compatibility shadow, never the owner. Flattening a whole
value is a defect to be removed, not a fallback to add.

**I3. Mechanical gates come before any materialization. [U 2026-10-17]** Search and regex consult the precompute
first, in this order: literal, required literal, trigram facts, bit-parallel transfer, NFA rows, DFA, oracle
corpus. The JNI char or byte shadow is the last step. Each remaining materialization point is a gap; close gaps in
order of how often they are reached. Done means no whole materialization for `indexOf`, `lastIndexOf`,
`contains`, `matches`, `regionMatches` and regex.

**I4. A precompute measure is a monoid with a seam rule. [V]** Each field of `M3StringFacts` must satisfy
`scan(a + b) == compose(scan(a), scan(b))` for every cut, including cuts through a surrogate pair or a CR LF.
A new measure needs: the per-unit fold in `scan`, the seam rule in `compose`, and seam cases in
`test/jdk/java/lang/String/M3StringFactsCompositionTest.java` (that test compares every primitive facts field of
a composed String with the flat one by reflection, so a new field is covered once it exists). A measure without a
reader is not a leaf: land it with its first consumer.

**I5. Lineage stays explicit. [U 2026-10-17]** `java.lang.M3String*` and `jdk.internal.mindex.*` descend from
`com.synexia.indexstring.MIndexString*`. Name the donor file and blob in each leaf. Never infer the owner from a
name alone.

**I6. Convergence is decided by logic, not by names. [U 2026-11-16, V]** Routes come from the atomizer hash planes
(`converge_plan.py`: the logic hash decides; structure and SimHash planes are counted and never route). A name
match is a proposal. Equivalence needs a hash or a diff. Nothing is deleted or retired without the loss audit.

**I7. No sprawl. [U 2026-11-08]** No new public class, public member, package or module root in M3JDK without
the owner's admission. String internals stay package-private in `java.lang` or in `jdk.internal.*`.

**I8. Builders are composite too. [U 2026-10-18]** `StringBuilder` and `StringBuffer` accumulate atoms and tuples;
`toString` must not flatten.

**I9. Regex runs over the composite. [U 2026-10-18]** A cursor walks chunks (the regex-cursor pattern); transfer
elements are keyed by (pattern, atom) or (pattern, word id) and composed; text is not flattened and not
reconstructed. Token-aware regex runs over interned token ids.

**I10. Proof is executed, and its limits are stated. [U]** Each leaf reports PASS, FAIL, NOT_RUN or HOLD per
check, with the command. A stock JVM cannot boot a patched `java.lang`, so on a host without an M3JDK build image
the accepted proof is: (a) stock `javac --patch-module java.base=...` on base and head with no new diagnostics,
(b) a differential on the kernel text copied out of the head source, with discriminators that a plausible wrong
implementation fails, (c) the jtreg test for CI. Runtime inside M3JDK is then NOT_RUN and must be said so.

**I11. Clean output in M3jdk21. [U 2026-11-14]** The M3jdk21 PR carries source and jtreg tests only. Evidence,
tools, recipes and handovers lived in com.synexia until 2026-11-16; that day the owner decided that the
records and tools of this lane converge into M3jdk21 too (this folder). No M3 stamping of new names, Java 21 API only, lines within 100
columns, Javadoc on new members, no AI co-author or generated-by trailers in commits.

**I12. Donors first. [U]** Before a new mechanism, search existing owners and licensed donors and record the
decision. Rope and regex donors already classified: section 7.

**I13. One production owner at a time. [U]** Edit one owner file, prove it, then the next. Fix the recipe or tool
that produced a wrong output, not the output.

## 3. State of the owners on M3jdk21 master

Pin for all [V] rows: M3jdk21 `e3c8bb1d0e3` (2026-11-16, merge of #3150). Leaf 1 below landed afterwards.

- **[V]** `src/java.base/share/classes/java/lang` holds 28 string owners (22,981 lines): 20 `M3String*` types,
  `M3FrozenLexiconIndex`, `String`, `AbstractStringBuilder`, `StringBuilder`, `StringBuffer`, `StringLatin1`,
  `StringUTF16`, `StringConcatHelper`. Blob ids: `data/JAVA_LANG_STRING_BLOBS.tsv`.
- **[V]** Precompute owners: `M3StringFacts` (monoid measures), `M3StringCodePointPrecompute`,
  `M3StringPositionPrecompute`, `M3StringAffixPrecompute`, `M3StringRegexRowPrecompute`,
  `M3StringSearchPrecompute`, `M3StringPrecomputeBridge`, `M3StringRegexDp`.
- **[V]** Measures in `M3StringFacts`: UTF-16, UTF-8 and modified UTF-8 lengths; code point count; unpaired
  surrogate count; Java hash and 31^length; first and last unit; 64-bit unit presence signal; ASCII and Latin-1
  flags; 14 regex class masks (all units, any unit); ASCII upper, lower and title hashes; first and last four
  units; bigram and trigram signals; trim and strip edges; and, since leaf 1, the line terminator count.
- **[V]** Flatten accessors in `M3String`: `materialize()`, `compatibilityValue()`, `charShadow()`.
  `String.value()` calls `materialize()` when the String is M3-backed. `value()` call sites exist in 23 `String`
  methods (`data/STRING_VALUE_SITES.tsv`); the sites read so far sit after the M3 branch has returned, on the flat
  path. Which sites can still run with an M3 owner has not been analysed. `compatibilityValue()` is reached from
  nine `String` constructors and `m3AdmitNative`; `charShadow()` from `toCharArray`.
- **[N 2026-10-18]** Absent on master then: insert/delete edit log, line and column index, multi-pattern search,
  a pluggable per-node measure slot, SIMD per-atom runs, a general DFA step. Regex ladder then: literal gate and
  `M3PatternQuery.derive` wired; trigram facts without a per-atom cache; compile-time harvest present but unwired.

## 4. Leaf 1, landed 2026-11-16

M3jdk21 PR #3152, merged into master as `0c6107abd9be` (commit `906c2fceeaeb`; 3 files, +47 -4).

- `M3StringFacts.lineTerminatorCount`: terminators as `String.lines()` defines them (LF, CR, or CR LF as one).
  Fold: `lineTerminatorStep(previous, unit)`, called only when the unit has the vertical-whitespace class bit.
  Seam rule: `lineTerminatorSeam`, left + right, minus one when CR ends the left side and LF starts the right.
- First reader: `String.lines()` on an M3-backed String. When facts are already prepared and the count is zero,
  the spliterator emits the whole value without the per-unit `charAt` scan. It never prepares facts.

| Check | Result |
|---|---|
| Stock JDK 21 `javac --patch-module`, base and head | PASS: exit 0 both, 0 new diagnostics |
| Fold vs `java.util.regex`, 29,524 exhaustive + 200,000 random strings | PASS: 0 mismatches |
| Compose at every cut; associativity at every pair of cuts | PASS: 0 mismatches |
| Copied spliterator text vs `String.lines()` | PASS: 0 mismatches |
| Three discriminators (no seam rule, forced flag, no CR memory) | PASS: all detected |
| Inside an M3JDK VM: `scan` over a real `M3String`, the jtreg test, the reader in place | NOT_RUN (no build image on the host) |
| Timing | NOT_RUN |
| CI checks at merge time | pending; the merge is recorded UNVERIFIED in the merge ledger |

Proof files: `leaf1/` in the run record (`run_proof.sh`, `LineTerminatorProof.java`, `compile_gate.sh`,
`PROOF.tsv`, `COMPILE_GATE.txt`, `leaf1.patch`). First thing to do next: read the CI result of `0c6107abd9be`
and fix forward if a check is red.

## 4b. Leaves 2, 3 and 4, landed 2026-11-16

All three are exact gates: they remove forced work and change no result.

| Leaf | Change | PR and merge |
|---|---|---|
| 2 | `String.codePointCount(begin, end)` on an M3-backed value returns `end - begin` when the owner is Latin-1 coded or prepared facts show `codePointCount == length` (the gates `offsetByCodePoints` already had). | M3jdk21 #3154, merged `fed60d8a7400` |
| 3 | `AbstractStringBuilder.inflateIfNeededFor(String)` (under `append`, `insert`, `replace`): a Latin-1 coded M3 argument no longer calls `facts()` to learn its width. | same PR, second commit |
| 4 | `M3String.encodeSingleByte` and the ISO-8859-1 branch of `encodeNoRepl`: the bulk copy runs on the coder alone for a Latin-1 coded owner. | M3jdk21 #3155, merged `b7539452eb6c` |

Later the same day:

| Leaf | Change | PR and merge |
|---|---|---|
| 5 | `startsWith` / `endsWith`: a flat receiver compares an M3-backed prefix by content (correctness defect, see section 8 item 2). | M3jdk21 #3158, merged `82131e6ef9e9` |
| 6 | `strip`, `stripLeading`, `stripTrailing`, `trim`, `isBlank`: the prepared measure when there is one, else a bounded edge scan (64 units per edge), facts only for a longer blank run. Kernels `leadingEdge` / `trailingEdge` in `M3StringPrecomputeBridge`. Proof: 597,871 exhaustive strings against stock, 0 mismatches. | M3jdk21 #3160, merged `b1c5723683a8` |
| 7 | Linear single-unit search (`indexOf(char)`, `lastIndexOf(char)`, under `split(char)` and `replace(char, char)`) for sources under 256 or over 32,768 units: windows start at 32 units and double to 4,096. Proof: head = base = oracle on 20,000 random texts and a seam sweep; units copied in a search-again loop fall from 19,437,056 to 159,980 on 40,000 units. | M3jdk21 #3161, merged `0db78e2e9f58` |
| 9 | `StringBuilder` / `StringBuffer` range append of an M3-backed String that is not lifted as a piece: one bulk inflate in place of `putCharsSB` (a `charAt` dispatch per unit) for a UTF-16 builder and a Latin-1 coded source; prepared facts of the whole value read before slicing for a Latin-1 builder and a UTF-16 coded source. | M3jdk21 #3168, merged `83d1ceca0e07` |
| - | Fix forward of the 100-column gate on the `@run` lines of the tests of 5, 6 and 7. | M3jdk21 #3164, merged `07ffb167a378` |
| 10 | `StreamEncoder.write(String, int, int)` (under `OutputStreamWriter.write(String)`, so every JUL record and every `FileWriter.write(String)`): the fork wrapped the String in a `CharBuffer` without a backing array, so every encoder ran its per-char buffer loop, for flat Strings and with M3 storage off too. Now char windows of at most 8,192 through `String.getChars` and the array loops. | M3jdk21 #3169, merged `fa31ad3ed029` |
| 11 | `String.countMatches(int)`: the first string-utility operation absorbed into java.base, and a new public `String` method (admitted by the owner, see [ABSORB_PLAN.md](ABSORB_PLAN.md)). | M3jdk21 #3175, merged `74ba211ea9c2` |
| 12 | Regex arguments of `String.matches`, `replaceFirst`, `replaceAll` and `split` are compiled once and stored: a private table in `java.lang.String` (256 slots, softly held, keyed by the regex hash, verified by its text). This is the run-time half of the owner's regex rule; the compile-time half (the compiler writes regex metadata into the jar) is open. Proof: the edit recipe applied to the stock `String`, 30,048 cases identical on the stock VM with and without it; `matches` 313 ns to 84 ns per call, `replaceAll` and `split` within noise. | M3jdk21 #3178, merged `cbbabc227c21` |
| 13 | Rope as a segment list, first step: a tuple of height 3 or more computes the flat list of its scalar leaf ranges once (cumulative ends, owner and start per segment), stores it, and `charAt` finds a unit by binary search instead of a descent from the root. More than 1,024 segments, a non-scalar leaf or a length mismatch keep the descent. Proof with stubs of the three types the copied text calls: 3,003 trees, every index, 0 mismatches; owner reads per unit on a 600-leaf chain 301 before, 1 after. | M3jdk21 #3182, merged `9c0877f6f55b` |
| - | JUnit 5 tests for everything above: `M3StringLeavesTest` (10 parameterized tests x 12 texts x 4 storage forms = 432) and `StringCountMatchesTest` (20). Run on the stock JDK: all pass. The owner asked: "add junits for everything". | M3jdk21 #3184, merged `45983c130fa6` |
| 15 | `String.indexOfDifference(String)`: the second absorbed operation and new public `String` method; flat Strings of one coder use `ArraysSupport.mismatch`. Semantics equal to commons-lang3 on 21.9 million pairs (see [ABSORB_PLAN.md](ABSORB_PLAN.md)). JUnit test with it. | M3jdk21 #3186, merged `b7200d690615` |

**Convergence, restated by the owner on 2026-11-16 ("this supposed to converge to m3jdk", "all of this").**
Everything in this lane ends inside M3jdk21. The library algorithms are absorbed into `java.lang.String`; the
M3AST copies under `m3/` converge on the javac owner; the parsers follow. A change in a fork, such as
commons-lang #1, is the consumer side only. The commons-lang3 copy that M3jdk21 carries under
`m3/vendor/third_party` has a `SOURCES_MANIFEST.tsv` and `PROVENANCE.json` that pin it to the upstream release,
so it is not edited; it goes away when its operations are absorbed.

Leaf 9 waited for `M3JDK source naming` to pass on its pull request before the merge; do the same for every leaf.
Its test `M3StringBuilderRangeAppendTest` also passes on a stock JDK (420 checks).

Each of 5, 6 and 7 adds a jtreg test that compares M3-backed Strings (whole, range, concatenation) with a flat
peer. All three tests also pass on a stock JDK, where they only confirm their own expectations.

**[V] Leaf 10 is the first leaf proven on the real classes.** `StreamEncoder` compiles against a stock JDK, so
the base and the head class were each patched into `java.base` of stock JDK 21.0.9 and one public-API harness ran
on the unpatched JVM, the base and the head: 7,040 cases over 11 charsets gave identical bytes and identical
sequences of write lengths in all three. Time per 120-char write (one run, ASCII / Latin-1 mix / CJK mix):
stock 52 / 113 / 164 ns, base 277 / 318 / 458 ns, head 48 / 128 / 170 ns. The base was therefore a regression
against the stock JDK, independent of the M3 flag. The same check fits other fork changes outside `java.lang`:
`java/nio/StringCharBuffer.java`, `java/io/Writer.java` and `java/io/OutputStreamWriter.java` differ from stock by
82, 112 and 40 lines and have not been compared this way. Script: `leaf10/run_proof.sh`.

**[V] M3 storage is behind an experimental VM flag.** The jtreg tests run with
`-XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage`, and a review agent read the flag as off by default
(`globals.hpp`, EXPERIMENTAL, in a 2026-11-14 checkout). Every gain in this lane, for Maven or any other program,
exists only when the VM runs with that flag. Whether and when it becomes the default is the owner's decision.

**[V] What CI did say on 2026-11-16.** The fast pull-request checks completed on the leaf commits:
- Passed: `Public JDK naming contract`, `M3 repository file atom census`, and `M3JDK canon gate` where it ran.
- `M3JDK source naming` passed on leaves 1 to 4 and **refused leaves 5, 6 and 7**: the `@run` lines of the three
  new tests were 105 to 126 columns wide. This gate checks every added line against 100 columns, test tags
  included. Fixed forward by M3jdk21 #3164 (class name and options moved to a continuation line). When you check
  line widths before a commit, do not exempt `@run` lines.
- Already failing before this lane on commits of other work, and still failing: `M3JDK naming standard`,
  `M3 TQ counterpart` (last success 2026-10-21), `M3 Lane28 counterparts` (no success on record),
  `M3SDK JMOD Receiver Proof` (last success 2026-11-04). Their logs show Maven test failures, not findings in
  the string leaves; nobody in this lane has read them further.
- No JDK build ran: see the next paragraph.

**[V] CI gave no build or test verdict on 2026-11-16.** On every recent master commit, by this lane and by others, the workflow
runs were cancelled by a newer push or still queued, the OpenJDK sanity build included. The rollup shows FAILURE
without a failed check. Read the runs of the newest master commit before trusting any leaf above in a VM.

**[V] The invariant behind 3 and 4:** a Latin-1 coder implies every unit is at most U+00FF, for all three owner
kinds: atom factories reject a wider unit and store one byte per unit; immediates are Latin-1 only; a tuple's coder
is the OR of its sides. A range reports its owner's coder. The reverse does not hold: a UTF-16 coder proves nothing
about a range (a Latin-1-only slice of a UTF-16 owner reports UTF-16; mapped lexicon atoms take their coder from a
table). Never use `coder()` of an M3 side as a content gate in the UTF-16 direction.

Proof (stock JDK 21): compile gates exit 0 on base and head with 0 new diagnostics; for leaf 2, 97,656 exhaustive
strings, every range, 0 mismatches against stock `codePointCount`, two discriminators detected. A read-only review
agent confirmed leaves 2 and 3 against the source independently. Inside an M3JDK VM: NOT_RUN. CI was pending at
each merge. Files: `leaf2/`, `leaf4/` in the run record.

**The owner's rule, 2026-11-16: "metadata that computed once are stored for subsequent retrieval".** A measure
or a result that was computed is cached on its owner and read back. Consequences still to implement: the bounded
edge scan of leaf 6 does not store what it finds, so a repeated `strip` or `isBlank` rescans the edges (a memo of
two small counts per owner would do); and facts are prepared on demand and kept, which settles the question
below.

**The owner's rule, same day (twice): "static regex can compute the precompute at compile time in jar".**
Constant regex patterns get their precompute at compile time, stored in the jar, retrieved at run time. State
of the evidence: the harvest exists in `m3/ports/compiletime` (`M3ConstantRegexHarvester`, `M3ConstantRegexImage`)
and nothing reads it at run time. A review of this angle kept two candidates and rejected three: kept were a
memo of the compiled `Pattern` per regex text for `String.matches`, `replaceAll`, `replaceFirst` and `split`
(rows CT2 and CT3: today each call compiles the regex again); rejected were an image reader in `Pattern.compile`
that only skips a derivation, a producer inside javac, and a precomputed DFA lane, each with reasons in
`scout/SCOUT_CANDIDATES.tsv`. The owner's instruction stands above that review. Order: first the run-time memo
(computed once, stored), which needs no build; then the compile-time step, where javac gives every constant regex
a constant-pool entry that is resolved to its compiled `Pattern` once, and the harvest image travels in the jar as
a resource. The compile-time step needs a host that can build javac and run it. The owner added the same day: "compiler can generate metadata": javac is the producer of
stored metadata in general, not only for regexes. Leaf 12 is the run-time memo.

**Design decision these leaves raise (settled by the first rule above):** each removes a place where facts were prepared as a side
effect. Readers that use `factsIfPrepared()` (search gates, the regex trigram gate, `lines()`, the `codePointCount`
gate) then fire less often. Where facts are prepared on purpose has to be decided: at admission above a size, while
decoding (the decode loop already sees every unit), or on a second use.

## 5. Convergence of string sources

- **[V]** com.synexia develop `a7b7bb639802` (2026-11-07) has 5,426 main Java files with a string-related name
  (String, Rope, CharSeq, Utf8, Utf16, Latin1, Regex, Lexicon, Intern). 3,240 are copies under noise roots
  (`.superset`, `chat-artifacts`, `third_party`, `.merge-preserved`, `.m3`, `docs`, `donors`); 1,302 sit under
  recipe owners; 884 are in live modules. Of the 884, 749 have no type of the same concept name in M3JDK. This is a
  names-only scout: it scopes the work and decides nothing. Per module: `data/STRING_CONVERGE_MODULES.tsv`.
- **[V]** Largest live donors: `synexia-indexstring` 372 files by name (1,622 main Java files in all),
  `synexia-common` 79, `synexia-mindex` 71, `synexia-dictlang` 34, `synexia-index-precompute` (123 files),
  `synexia-indexstring-compiler` (111), `synexia-mindex-indexstring-bridge` (16).
- **[V]** Owner hash planes for the string owners are built: 132 files (java.lang string owners,
  `jdk.internal.mindex`, `jdk.internal.m3`, `java.util.regex`), 7,073 atoms. The atomizer reported 5 errors on the
  owner run; the atom tables were written.
- **[V]** Donor run over the indexstring family (1,872 files, 64,928 atoms, 1,813 types). Against the first owner
  side (132 files) no type was already in M3JDK. Against a wider owner side (552 files: the 132 plus
  `m3/ports/indexstring`, the vendored mindex, lexicon, precompute, compile-time, regex-cursor,
  runtime-integration, core, indexdb): 25 types `CONVERGE_TO_OWNER` (20 are `jdk.m3.precompute.*` in
  `m3/ports/precompute`, 5 in `m3/ports/indexstring`), 495 `SHARED_LOGIC`, 1,293 `ABSORB`. Atom join: 2,503 exact,
  2,829 logic, 31,322 near, 723 absent. The `ABSORB` count is an upper bound: the owner side is still not the whole
  of M3JDK. Both atomizer runs exited 1 with `INVALID_AST` signal-chain errors (6 and 8) and wrote their tables.
  Files: `routes/` in the run record (`RENAME_PLAN.tsv` has one row per donor type).
- Order after the indexstring family: `synexia-mindex`, `synexia-common` string utilities, `synexia-dictlang`
  and the lexicon modules, then the forks.

## 5b. Maven as a consumer that ranks the leaves

Maven needs no source change to benefit: it runs on the JDK, so every `java.lang` leaf reaches it when it runs on
M3JDK. What its sources tell us is which leaves matter most.

**[V]** Lexical census of the main Java sources of hsoliwal/maven at `3b1854f5` (1,696 files; tests and `its/`
left out): 4,365 String, StringBuilder and regex call sites in 402 files. Files: `maven/` in the run record.

| Group | Call sites | Operations | Leaf in this lane |
|---|---|---|---|
| Builder | 2,072 (47%) | `append` 1,869, `new StringBuilder` 203 | Leaf 3 landed (append without a forced facts scan); builder candidates ASB-APPEND-GATE parts 2 and 3, CONCAT-PAIRWISE-FOLD |
| Search and slice | 749 | `substring` 341, `indexOf` 186, `charAt` 181, `lastIndexOf` 41 | Range hash without full facts (facts-01), gallop search (S1), skip loops (S4) |
| Prefix and suffix | 503 | `startsWith` 371, `endsWith` 125, `regionMatches` 7 | The FLAT-01 defect sits on this path; then exact `prefix4` / `suffix4` answers and run-wise compare (S2) |
| Regex | 264 | `split` 107, `matches` 78, `Pattern.compile` 58, `replaceAll` 21 | Compiled-Pattern slots for constant regexes (CT2, CT3); the unverified regex leads |
| White space | 243 | `trim` 149, `isBlank` 83, `strip` 11 | Edge scan in place of full facts (facts-03) |
| Format | 78 | `String.format` 74, `formatted` 4 | Compiler lowering of constant format strings (scout to be run again) |

Other counts: `replace` 125, `equalsIgnoreCase` 50, `toLowerCase` 53, `concat` 46, `getBytes` 35, `intern` 1.
Densest modules: `impl/maven-impl` 569, `impl/maven-core` 533, `impl/maven-cli` 458.

Limits: a call-site count says how often the code names an operation, not how often it runs. A run-time profile
of a real build on M3JDK is the next measurement, and it needs a build image. Concatenation with `+` is not
counted (javac lowers it; see the concat candidates).

The same census runs on any repository: `maven/census.sh <clone> <commit> <out-dir>`.

**"lp4j" is JUL, `java.util.logging`.** The owner said so in a question dialog on 2026-11-16, after sending the
sentence four times; the SLF4J and LSP4J paragraphs below were wrong guesses and stay only as data. For Maven the
owner chose: keep landing JDK leaves.

JUL in M3jdk21 master (`src/java.logging`, 23 Java files; `jul/` in the run record): 9 files have String call
sites, 160 in all (`XMLFormatter` 70, `HttpLogFormatter` 30, `LogManager` 26). The path of one log record:
`Formatter.formatMessage` scans the message for `{digit` with `indexOf` and `charAt`; `SimpleFormatter.format` is
one `String.format` with six arguments; `StreamHandler.publish0` calls `writer.write(msg)` on an
`OutputStreamWriter`. That last call was the defect fixed by leaf 10. Next for JUL: `String.format` with a
format string that never changes (parse once per format String), and the `XMLFormatter` escape loop.

**SLF4J** (a wrong guess for "lp4j", kept as data: Maven's logging API, and already vendored in M3JDK as
`m3/vendor/third_party/slf4j-api-2.0.12`): the Maven fork imports `org.slf4j` in 114 main files and has 605
logging call sites, 119 of them with `{}` placeholders on the same line. SLF4J itself has 76 String call sites in
7 of 47 files, and `org.slf4j.helpers.MessageFormatter` holds 49: it finds each placeholder with
`messagePattern.indexOf("{}", i)`, copies the pieces with `sbuf.append(messagePattern, i, j)`, appends each
argument's `toString()` and ends with `sbuf.toString()`. The patterns are literals, and literals stay flat under
M3, so the M3 side of a log call is the arguments and the builder. Files: `slf4j/` in the run record.

**What the three have in common:** builder appends are 47% of Maven's call sites, 80% of SLF4J's and 71% of
LSP4J's. The builder is therefore the owner to work on for all of them: leaf 3 landed; the verified candidates
ASB-APPEND-GATE parts 2 and 3 (no dead flat buffer in `new StringBuilder(String)`, bulk windows in
`append(CharSequence, int, int)`, which is the exact call `MessageFormatter` makes), LANE-EDIT-SPLICE and
ASB-RANGED-READERS are next, in one batched pull request.

**LSP4J** (the other reading of "lp4j", not confirmed by the owner; upstream eclipse-lsp4j/lsp4j at `b2f54ce`,
EPL-2.0 OR EDL-1.0; no fork and no vendored copy exists): 148 call sites in 17 of 131 main Java files, 105 of
them builder appends, 96 in `org.eclipse.lsp4j.jsonrpc`. Its string work is the wire path: every outgoing message
calls `content.getBytes(encoding)` with the charset name "UTF-8" and `header.getBytes(US_ASCII)`
(`StreamMessageConsumer`), and incoming JSON is read through an `InputStreamReader` into Gson. Document text,
line and column positions and incremental edits are handled by the language servers built on LSP4J, not by
LSP4J itself. So the leaves that serve it are the UTF-8 encode path of `M3String` (unverified lead W1) and the
readers and writers; a line index measure would serve the servers. Files: `lsp4j/` in the run record.

## 5c. Lucene and OpenNLP as modules of the image

From the one scout of this angle that finished, with its skeptic (rows `MR-1` to `MR-6` in
`scout/SCOUT_CANDIDATES.tsv`; all read from a 2026-11-14 checkout, two days older than master):

- **[V]** A Python pack (`m3/tooling/module-packs/nlp-search`, `jmod_pack.py`) already turns jars into JMODs and
  links a second, derived image. Its CI lane runs on a stock Temurin 21 and checks the forks out from GitHub. So
  Lucene and OpenNLP are not modules of the image that `make images` builds, and no M3 string structure is
  exercised there.
- **[V]** `make` finds a module only at `src/<module>/share/classes/module-info.java`. The vendored descriptors
  sit in a Maven layout under `m3/vendor/third_party`, so make never sees them. `opennlp-tools` has no descriptor.
- **[V]** Three version lanes exist: what M3jdk21 carries (Lucene 9.10.0, OpenNLP Tools 2.4.0, release sources);
  the fork heads compiled for Java 21 by a staging recipe (sources and Maven dependencies not carried); the fork's
  default Java 25 lane, whose classes cannot load on a 21 VM. Only the first builds from what the tree carries.
- **[V]** java.base exports no M3 internal to any module. Libraries get the benefit through the public String
  API when the JDK side has an M3 path. The proposed qualified export was refuted: it needs a named consumer and a
  narrow forwarding package the owner admits; a fork source that imports `jdk.internal.*` no longer builds on a
  stock JDK.
- Verified order of leaves: decisions (below), `org.apache.lucene.core` as a make-built pilot, resources and
  service files and legal notices, an image smoke test, then `org.slf4j`, `org.apache.opennlp.tools` and
  `org.apache.lucene.analysis.opennlp`. Hazard: service binding resolves every image module that provides a
  service, so shipping the set changes what a modular application resolves; an image SLF4J shadows a class-path one.
- Not established: the two scouts for Lucene's and OpenNLP's text carriers died on the usage limit.

**Owner decisions needed before the pilot (none taken):** module names (the upstream descriptor names are what the
rule "libraries keep upstream packages and API" implies); which version lane counts as "the forks"; sources moved
into `src/<module>` or staged through the stock import hook (`--with-import-modules`); the class loader; whether
"jdk.m3.search = search + indexdb + nlp" folds the libraries or only the M3 bridge. The pilot is a build-system
leaf and needs a host that can build the JDK image.

## 5d. Forks of parsers, Jackson and utility libraries

Owner, 2026-11-16 (three times): "parsers and the jackson and famous utilities can be forked and we can use
this instead".

- **[V]** The owner has 81 forks (`forks/FORKS.tsv`). Present: `jackson`, `jackson-core`, `jackson-databind`,
  `jackson-annotations`, `jackson-datatypes-collections`, `rewrite` (the OpenRewrite parsers) with its Maven and
  Gradle plugins, `guava`, `spring-framework`, `commons-collections`, `commons-compress`, `eclipse-collections`,
  `vavr`, `fastutil`, `hppc`, `JCTools`, `RoaringBitmap`.
- **[V]** Not forked, but vendored in M3JDK under `m3/vendor/third_party`: `commons-lang3` 3.17.0, `commons-text`,
  `commons-io`, `univocity-parsers`, `antlr4-runtime`.
- **[V]** Where these libraries flatten a String or read it per unit (main sources of the vendored copies,
  `forks/LIBRARY_STRING_SITES.tsv`): commons-lang3 has 30 `toCharArray` and 210 `charAt` call sites
  (`StringUtils` alone holds 104 flatten or per-unit sites); commons-text 21 and 95; guava 20 and 124
  (`CharMatcher` 30 sites); jackson-core 7 and 86, plus 29 `getChars` into its own buffers (`NumberInput`,
  `JsonStringEncoder`, the two generators); jackson-databind 6 and 68; univocity-parsers 6 and 46.
- How a fork can use M3 strings under the owner's rules (upstream packages and API kept; one fast implementation
  in java.base; java.base exports no M3 internal): replace `toCharArray` and `charAt` loops by String operations
  that have an M3 path (`indexOf`, `regionMatches`, `getChars` ranges, `strip`, `chars`), so the library gains on
  M3JDK and behaves the same on a stock JDK. Each such change needs the library's own tests on both.
- **Decided by the owner on 2026-11-16 ("all", in a question dialog) and done:** six new forks, untouched so far:
  `hsoliwal/commons-lang`, `commons-text`, `commons-io` (Apache-2.0), `univocity-parsers` (GitHub reports no
  licence; check the repository's licence file before any change), `antlr4` (BSD-3-Clause) and `javaparser`
  (GitHub reports no single licence; check before any change). Heads at fork time:
  `forks/FORKS_CREATED.tsv`.
- **"inline all as per M3String" = absorb the algorithms into java.base** (owner, same day, question dialog).
  The plan, the licence rule (clean-room, donors as test oracles), a catalogue of 172 operations (28 already in
  `java.lang.String`, 144 to absorb) and the open decision on the entry point are in
  [ABSORB_PLAN.md](ABSORB_PLAN.md).
- First fork leaf: `StringUtils` in commons-lang. Each fork leaf needs the library's own test suite on a stock JDK
  and on M3JDK, so it needs a host with room for a Maven or Gradle build. `javaparser` is a second Java parser:
  inside M3JDK the one-AST rule still holds, so it is a library fork, not a parser for the JDK.

## 5e. M3String on the M3 collections

Owner, 2026-11-16 (five times): "m3string can use collection too internally since every string is a segment".
An M3String is already a segment: an owner plus a packed start and length. The instruction is to build its
internals on the M3 collection owners, one model for collections and strings.

**[V] What the string owners use today** (the 20 `M3String*` files and `M3FrozenLexiconIndex` at M3jdk21
`83d1ceca0e07`): `java.util.ArrayList` at 39 places (32 in `M3String`: the piece lists of join, replace,
indent, translateEscapes; 7 in `M3StringPool`), `ConcurrentHashMap` at 5 (the pool buckets), `HashMap` at 2,
`AtomicReferenceArray` at 18 and `WeakReference` at 26 (the caches of plans, geometry and facts), and plain
`int[]`, `long[]`, `byte[]` arrays everywhere else (leaf ranges, builder lane offsets, search plans, the frozen
lexicon). No M3 collection owner is used.

**[V] What `java.lang` can reach.** java.base cannot depend on another module, so only owners inside java.base
count: `jdk.internal.m3.IndexedArray` and `IndexedView`, `jdk.internal.m3.intern.InternSpace`, and in
`jdk.internal.mindex` the lanes `M3IntLane28`, `M3LongLane28`, `M3BitLane28`, `M3PrimitiveFixedArena` and
`M3CollectionMaskCompactor`. The segmented storages (their tests are `SegmentedPrimitiveStorageTest`,
`SegmentedLaneBulkContractTest`, `LazySegmentedStorageTest`, `WeightedSegmentedLRUCacheTest`) live in the module
`jdk.m3.collections` and are out of reach until they are moved into java.base.

**What the owner means (question dialog, 2026-11-16): two things.** (a) *Rope as a segment list*: a
concatenated String is a flat, indexable list of segments, searched by offset, not a binary tree walked from the
root. (b) *Text stored in segmented lanes*: the units live as segments of the M3 segmented storage, one shared
arena of lanes, not one native allocation per atom; a String is then a lane, a start and a length.

State: (a) has its first step in leaf 13: the list is derived from the tree and stored, and only `charAt` reads
it. Next for (a): `CodeUnitCursor.seek`, `getChars` / `getBytes` and the search kernels read the list; then the
list becomes the owner and the binary node the derived form. (b) is not started. It changes where atom text
lives, so it touches `M3StringAtom` (native memory through `Unsafe`), the JNI shadows and the natives in libjava
that read atom addresses, and it needs the segmented storage inside java.base first. It needs a design note and
a VM to test in before any code.

Not done, and each a leaf of its own: (1) decide which collection owners move into java.base; (2) the piece
lists (`ArrayList<M3String>`) on a segment list; (3) the leaf range and offset arrays on the long and int
lanes; (4) the caches on one bounded cache owner in place of five hand-written tables. Each needs the same
proof as any other leaf, and a measurement in a real VM before it replaces a plain array on a hot path.

## 5f. Parsers on M3String and M3AST

Owner, 2026-11-16 ("additionally parsers using m3string and m3ast", five times; then in a question dialog
"javac lexer and parser, consolidated", then "all"): all four groups are in scope, javac first, and the M3AST
copies are consolidated into one. The groups: the javac lexer and parser; the text parsers inside the JDK
(`Integer.parseInt`, `Double.parseDouble`, `Scanner`, `Properties`, `Formatter`, `URI`, date and time); the
library parsers in the forks (Jackson, univocity, the ANTLR runtime, javaparser, OpenRewrite); one M3 parser
kernel (`jdk.m3.parser`). Nothing is implemented. The three scouts for this angle died on the usage limit.

**[V] The M3AST families** (a 2026-11-14 checkout; `parsers/M3AST_DUPLICATES.tsv` in the run record):

| Family | Files | Lines | Java files elsewhere that import it |
|---|---|---|---|
| `com.sun.tools.javac.m3` (`M3ASTPC`, `M3CompilerPatternHints`, `M3PrimitiveImageCompiler`), with `com.sun.source.util.M3AST` beside it | 3 + 1 | 2,497 | 14 |
| `jdk.m3.parser` (the parser pack: `M3JavacParser`, `M3JavacTree`, `M3ASTFacts`, `M3ASTIndex` and 17 more) | 21 | 4,851 | 1 |
| `jdk.m3.compiletime.ast` | 10 | 1,947 | 4 |
| `jdk.internal.m3.mindex.compiler.ast` (vendored) | 13 | 2,059 | 9 |

Eight type names exist in both of the last two families. Compared with the package names normalised:
`M3ASTWorkspaceCache` is the same text; `M3AST`, `M3ASTCodec`, `M3ASTPlugin` and `M3CompilationContext` differ
by 4 lines each; `M3File` (53 lines), `M3FileSourceFacts` (66) and `M3JavacASTIndex` (43) have diverged. So they
are not plain copies: consolidation needs a merge per type, the importers moved, and the builds of the modules
that import them. Deleting a family needs the owner's go-ahead for the deletion itself.

**[V] The javac lexer and parser are stock code.** `JavaTokenizer`, `UnicodeReader`, `JavacParser`, `Tokens`,
`Scanner`, `Names`, `Name`, `StringConcat` and `Lower` differ from the stock JDK 21.0.9 sources by the licence
header only (40 lines each). Of the javac files outside `javac/m3`, only `main/JavaCompiler.java` mentions an M3
type (27 changed lines). Nothing in javac's lexer, parser or name table uses M3String or fills an M3 token
index today.

**A constraint to design around.** The JDK build compiles and runs javac with the boot JDK first (the interim
compiler). javac's own sources therefore cannot call a method that exists only in M3JDK, such as
`String.countMatches`; M3-only behaviour inside javac has to be reached in a way that still loads on the boot
JDK. (General knowledge of the OpenJDK build, not re-read in this tree.)

Order proposed: (1) merge the two near-duplicate AST families into one and point the importers at it;
(2) fill `jdk.internal.mindex.M3TokenIndex` from javac's scanner through the existing `JavaCompiler` hook, so
tools stop lexing again; (3) the JDK text parsers, which can be proven with the patched stock JDK like the
String leaves; (4) the fork parsers, which are M3JDK-only by the owner's decision; (5) the parser kernel.

## 6. M3AST and the string owners

**[V]** They are not connected at runtime.

- javac has `com.sun.tools.javac.m3.M3AST`, `M3ASTPC`, `M3CompilerPatternHints` and `M3PrimitiveImageCompiler`.
  None of the M3AST files names `M3String`, a precompute owner, `M3TQ` or `M3PatternQuery`.
- The compile-time port (`m3/ports/compiletime`, 35 files) harvests constants: `M3CompileTimeStringInventory`,
  `M3ConstantRegexHarvester`, `M3ConstantRegexImage`, `M3ConstantRegexImageWriter`, `M3ConstantRegexGate`. It names
  `M3TQ` twice and `M3PatternQuery` once.
- No string owner in `java.lang` (master blobs) and no file in `java.util.regex` (a 2026-11-14 checkout) reads a
  compile-time image.
- `jdk.internal.mindex.M3TokenIndex` documents lanes for M3AST kind ids and bindings. That is a layout contract
  only.
- **[V]** `M3AST*` types exist in four places (javac, `jdk.m3.parser`, `jdk.m3.compiletime.ast`, the vendored
  mindex compiler). The owner's rule is one AST in M3JDK, so the consumer must read the javac owner's output.

The missing piece is the consumer: `Pattern.compile` and the string constant paths reading the image the
compiler writes. That is leaf 4 below.

## 7. Donors already classified [N 2026-10-18]

- Ropes: Apple swift-collections `BigString` and `_RopeModule` (Apache-2.0, pattern and test scenarios only);
  functionaljava finger trees (BSD-3) and ReactFX `FingerTree` (BSD-2) for the measured-node contract; Netty
  `CompositeByteBuf`, `AsciiString`, `ByteProcessor` (Apache-2.0) for run scans across chunks; naotsugu/piecetable
  (Apache-2.0) for the edit log; IntelliJ `LineSet` for the line index. Ropes-for-Java is GPL-3.0: pattern only.
- Regex: helix-editor regex-cursor (MIT OR Apache-2.0, pattern); Lucene `util.automaton` (Apache-2.0); dk.brics
  (BSD-2); Aho-Corasick by robert-bor (Apache-2.0).
- Open licence decision: a Lucene-derived automaton core inside GPLv2+CPE java.base. The owner has to rule
  before such code merges into `src/`.

## 8. Next leaves, in order

Scout results with skeptic verdicts are in `scout/SCOUT_CANDIDATES.tsv` (columns: run, id, verdict, title, file,
lines, algorithm, corrected claim, hazards). Read the corrected claim and the hazards of a candidate before
implementing it; several scout claims survived only in a narrower form.

1. Read CI for the merges `0c6107abd9be`, `fed60d8a7400`, `b7539452eb6c`; fix forward if a check is red.
2. **Landed as leaf 5 (M3jdk21 #3158, merged `82131e6ef9e9`): the FLAT-01 correctness defect.**
   `String.startsWith(String, int)` (also behind `startsWith(String)` and `endsWith`) with a flat receiver and an
   M3-backed prefix flattened the whole prefix and returned false when a Latin-1 flat receiver met a
   Latin-1-content range of a UTF-16 owner. The prefix is now compared by content (`compareFlatPrefix` at offset 0,
   `regionMatches` otherwise). Evidence: compile gate 0 new diagnostics; the new jtreg test
   `M3StringMixedStartsWithTest` passes on a stock JDK (819 checks, oracle sanity only). The defect and the fix
   rest on source reading: **first thing to check in CI is that this test fails on the parent commit and passes on
   the merge.** The same one-sided guard pattern may exist in other mixed methods; `data/STRING_VALUE_SITE_GUARDS.tsv`
   lists the `OTHER_RECEIVER` sites to read next (`compareTo`, `regionMatches`, `indexOf(String)`, `replace`).
3. Exact readers that need no new measure (measure-readers skeptic). Landed: `strip`, `trim`, `isBlank`
   (leaf 6); the growing-window unit search S1 step 1 (leaf 7). Open: the per-line `line.facts()` reads in
   `M3String.indent` and `stripIndent` can call the bridge's `edgeStart` / `edgeEnd`; `translateEscapes` hops with
   `indexOf('\\')`; short prefixes and suffixes answered exactly from `prefix4` / `suffix4`; `lines()` uses the
   terminator count as a countdown; the free gates of facts-05 in `mayContainCodeUnit` and `caseMap`.
4. Algorithms (S1 to S5, SM-01 to SM-06): single-unit search that gallops over bulk windows for `split(char)` and
   `replace(char, char)`; run-wise compare with `ArraysSupport.vectorizedMismatch` for mixed flat and M3 `equals`,
   `compareTo`, `regionMatches`; skip loops for flat-needle and reverse searches; a range hash from stored owner
   hashes (facts-01: a wrong hash is fatal later, so prove it exhaustively).
5. Builders (ASB-APPEND-GATE parts 2 and 3, LANE-EDIT-SPLICE, ASB-RANGED-READERS, LANE-SEARCH-WINDOW,
   CONCAT-PAIRWISE-FOLD): no dead flat buffer in `new StringBuilder(String)`, bulk windows in range appends, tail
   and splice edits without rebuilding the lane, read-only operations that do not flatten.
6. Compile-time consumer (CT2, CT3 survived; see the compile-time rows): compiled-Pattern slots for constant
   regexes in `String.matches`, `replaceAll`, `replaceFirst`, `split`.
7. Unverified leads (rows marked UNVERIFIED: the scout returned candidates, its skeptic died on the usage limit,
   so nothing in them has been checked against the source a second time; the Lucene and OpenNLP module rows were verified later and are in section 5c): regex (18 candidates: gates such as a
   literal rung in `Matcher.search` and `X+` in the boolean full-match plan, algorithms, Matcher input), string
   utilities (12), readers and writers (12, for example bulk loops in `M3String.encodeUtf8` and a char window in
   `StreamEncoder.write(String)`), Lucene and OpenNLP as jmods (6, one scout of three). Run a skeptic over each
   before implementing: resume the run with its script in `workflows/` and the run id, and finished scouts replay
   from the journal.
   Nothing returned (scouts died on the usage limit, run again): parsers and M3AST (3 scouts), "univec"
   (3 scouts), javac string optimizations (1 scout), Lucene text carriers and OpenNLP text carriers if their
   rows are absent from the table.
8. Convergence: turn the 25 `CONVERGE_TO_OWNER` rows into reference migrations with the loss audit, then one leaf
   per `SHARED_LOGIC` group; widen the owner side to the whole tree before trusting an `ABSORB` row.
9. More readers of `lineTerminatorCount` in `M3String.indent` and `stripIndent` (facts-06, narrowed by its
   skeptic: no new measure is needed).

## 9. How to work here

- Sources without a build: blobless clones. List names with `git ls-tree -r --name-only <sha>` (no blobs
  needed). Fetch blobs in one batch: pipe the blob ids to `git fetch --no-tags --filter=blob:none origin --stdin`.
  Then read them with `git cat-file blob <id>`. A batch fetch overwrites `FETCH_HEAD`: use explicit commit ids
  afterwards.
- Convergence run: `tools/run_string_apl.sh` (atomize, planes, join, converge over the atomize kit).
- Leaf proof: `leaf1/run_proof.sh <head java/lang dir> <work dir>` and
  `leaf1/compile_gate.sh <leaf dir> <pin overlay> <full tree>`.
- Commit without a clone: the GraphQL commit tool (`gql_commit.py <owner/repo> <base> <new-branch> <message
  file> <specs file>`), then `gh pr create`, then the merge-all recipe.
- Before committing over a base, compare the blob ids of the edited files on the branch head with the base you
  edited. They must be equal.
- Task ledger rows: `STRING-CONVERGE` and `STRING-PRECOMPUTE` (priority P0).

Proof inside a patched stock JVM (leaf 7): when a kernel calls package-private `java.lang` helpers
(`StringLatin1`, `StringUTF16`, `String.LATIN1`), copy its text into a generated class in package `java.lang`,
add a stub for the `M3String` members it calls, compile with `javac --patch-module java.base=<gen dir>` and run
with `java --patch-module java.base=<classes> java.lang.<Proof>`. The copied text then links against the stock
helpers, and the base and head kernels can be compared with each other and with an oracle
(`leaf7/run_proof.sh`). Test idioms for jtreg: a guaranteed-flat String under M3 is built with the
`M3String.ADMITTING` thread-local set around `new String(char[])`; `String.m3()` never admits lazily, so
literals stay flat; make the reflection optional so the same test runs on a stock JDK as an oracle check.

Scouting with agents (used on 2026-11-16): one workflow per angle, each scout followed by a skeptic that must
refute every candidate against the source. Scripts: `workflows/` in the run record. Results: one result line per
agent in the run's `journal.jsonl`; `scout/extract.py` turns the journals into `SCOUT_CANDIDATES.tsv`. Eight such
workflows in one afternoon used up the weekly usage limit (one scout died with "weekly limit"); run fewer at a time.

Traps met on 2026-11-16:

- Run stock `javac` for a proof harness with `-sourcepath <proof dir>`. With the shell in a folder that holds
  `String.java`, javac compiled that file and printed hundreds of errors.
- `merge_all.py` can miss a pull request created seconds earlier; run it again.

- Stock `javac --patch-module` needs `-implicit:none`. Without it one compile wrote 5,209 class files.
- The scratch volume used that day allocates about 1 MB per file: 8,142 class files took 8 GB and filled it. A
  full disk makes javac exit 1 with an empty log.
- `apl.sh` of the atomize kit fails under Git Bash: its `here=` line yields two lines, and its `step` function
  reports status 0 for a failed step. `run_string_apl.sh` runs the same steps directly.
- `rg` prints backslash paths on Windows, and Git Bash converts a bare `/` argument into a path. Use
  `git ls-tree` listings, or translate separators with `tr`.
- The Write and Edit tools can turn `\uXXXX` escapes into literal characters. In Java sources use numeric casts,
  and check the diff bytes after an edit near such escapes.
- `git status` on the large com.synexia checkout can run for minutes. Do not use it for inventory.
