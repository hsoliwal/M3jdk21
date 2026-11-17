# Absorbing string-utility algorithms into java.base

Written 2026-11-16. Companion to [HANDOVER.md](HANDOVER.md). Data: `record/absorb/` beside this file.

## The decision

The owner, 2026-11-16, in two question dialogs:

1. Fork commons-lang, commons-text, commons-io, univocity-parsers, antlr4 and javaparser ("all"). Done; the forks
   are untouched.
2. "inline all as per M3String" means: **absorb the algorithms into java.base.** The string algorithms of the
   utility libraries get one implementation in the JDK, on the M3 String owners, and the libraries call the JDK.

This follows the owner's earlier rule: the JDK owns each fast implementation once, in java.base; no duplicate
fast paths.

## What binds the work

- **Licence.** java.base is GPLv2 with the Classpath exception; the donors are Apache-2.0 (commons, Guava),
  BSD-3-Clause (antlr4) or to be checked (univocity-parsers, javaparser). A rule recorded on 2026-10-07 (carried
  from notes, not re-read today) says no Apache-derived code goes into `src/**`. So "absorb" here means a
  clean-room implementation of the algorithm, written from its specification and the donor's documented
  behaviour. The donor code is the oracle: it may enter the tree only as a test fixture that a differential test
  runs against the JDK owner. If the owner wants donor code copied instead, that needs an explicit ruling.
- **Public surface.** No new public or protected class or member without the owner's admission (rule of
  2026-11-08). A kernel can land package-private at once; its public entry point is a separate decision (below).
- **Behaviour.** A library method that is redirected to the JDK must return what it returned before, for every
  input, null and empty arguments included. The donor's own test suite is the gate, on a stock JDK and on M3JDK.
- **Measures.** Class tests answered from the M3 class masks are exact only for ASCII text; the Unicode
  definitions (`Character.isLetter`, `isDigit`, `isWhitespace`) decide everything else.
- **Proof.** As in the handover: kernel text copied into a harness on a stock JVM, or the class patched into the
  stock JDK when it compiles there; discriminators; a jtreg test; in-VM runs stated as NOT_RUN until they happen.

## Catalogue

`absorb/ABSORB_CATALOGUE.tsv`, built by `tools/absorb_catalogue.py` from the copies M3jdk21 carries at
`83d1ceca0e07`: every public static operation name of commons-lang3 3.17.0 `StringUtils` and
`CharSequenceUtils`, Guava `Strings` and `Ascii`, and commons-text `WordUtils`, with overload count, source lines,
`toCharArray` and `charAt` counts, and whether `java.lang.String` already has a public method of that name.

172 operation names. 28 have a same-name `String` method (route DELEGATE: the library can call it, nothing to
absorb). 144 do not (route ABSORB). The route and the family come from the name alone, so each row is a proposal.

| Family | To absorb | Examples | Classic algorithm, and the M3 structure it can use |
|---|---|---|---|
| SLICE | 25 | `substringBefore`, `substringBetween`, `left`, `mid`, `abbreviate`, `truncate`, `remove`, `overlay` | One search, then slices of the composite: no copy |
| SEARCH | 20 | `containsAny`, `containsNone`, `containsOnly`, `indexOfAny`, `indexOfAnyBut`, `countMatches`, `ordinalIndexOf` | Character-set bitmask (two words for ASCII), counted scans over bulk windows; the unit presence signal rejects early |
| EMPTY_BLANK | 16 | `isNotBlank`, `isAnyBlank`, `defaultIfBlank`, `firstNonBlank` | The edge scan of leaf 6 |
| BUILD | 13 | `leftPad`, `rightPad`, `center`, `rotate`, `reverse`, `wrap`, `appendIfMissing` | Tuples of the argument and a repeated pad: no copy |
| EDGE | 12 | `stripStart`, `stripEnd`, `chomp`, `chop`, `deleteWhitespace`, `normalizeSpace` | Edge scans; run compaction in one pass |
| CLASS_TEST | 11 | `isNumeric`, `isAlpha`, `isAlphanumeric`, `isWhitespace`, `isAllLowerCase`, `isAsciiPrintable` | Class masks when the text is ASCII, else the Unicode test per code point |
| CASE | 11 | `capitalize`, `uncapitalize`, `swapCase`, `lowerCase` | First-change scan; the class masks prove "nothing to change" |
| REPLACE | 7 | `replaceEach`, `replaceChars`, `replaceOnce`, `replaceIgnoreCase` | Aho-Corasick for many patterns; a translation table for chars |
| SPLIT_JOIN | 6 | `splitByWholeSeparator`, `splitPreserveAllTokens`, `splitByCharacterTypeCamelCase` | Delimiter walk over bulk windows (leaf 7); boundary masks for camelCase |
| COMPARE | 6 | `indexOfDifference`, `getCommonPrefix`, `compareIgnoreCase`, `equalsAny` | Longest common prefix by word-at-a-time mismatch over leaf runs |
| DISTANCE | 3 | `getLevenshteinDistance`, `getJaroWinklerDistance`, `getFuzzyDistance` | Bit-parallel edit distance (Myers), banded when a threshold is given |
| OTHER | 14 | `getDigits`, Guava `commonPrefix`, `commonSuffix`, `padStart`, `lenientFormat`, `Ascii.truncate` | Per operation |

Not catalogued yet: commons-io, Guava `CharMatcher` / `Splitter` / `Joiner`, Spring `StringUtils`, the parsers
(univocity scanning, Jackson number and string decoding), commons-text similarity and `StringSubstitutor`.

## Order of work

1. The 28 DELEGATE rows cost nothing in the JDK: they are fork changes (the library calls the `String` method
   for a `String` argument).
2. Kernels by family, most used first: SEARCH (character sets and counting), EDGE and EMPTY_BLANK (already half
   there through leaf 6), CLASS_TEST, COMPARE, then SLICE and BUILD (composition, no algorithm), then REPLACE,
   SPLIT_JOIN and DISTANCE.
3. Each kernel: package-private in `java.lang` next to the M3 String owners, working on a flat String and on a
   composite; a differential test against the donor method in the test tree; then its entry point.

## Open decision: where a library calls the kernel

A kernel in `java.lang` cannot be called by a library until it has an entry point. The choices:

- **A facade in an M3 module** (`jdk.m3.text` exists with 26 source files): public static methods there, the
  kernels reached through a qualified export of java.base. Matches the owner's earlier rule "libraries keep
  upstream packages; jdk.m3 facades on M3 structures". A fork then needs M3JDK at run time, or a fallback.
- **New public methods on `java.lang.String`.** Shortest call path; changes the Java SE surface of the fork.
- **A new public class in `java.lang`.** Same remark.
- **JDK-internal only** (through `JavaLangAccess`): JDK modules and library copies built as M3JDK modules can
  call it; a fork on the class path cannot.

**Taken by the owner on 2026-11-16 (question dialog): new methods on `java.lang.String`.** This admits new
public `String` methods for absorbed operations; each pull request still lists the methods it adds.

## How the forks call the JDK

Taken by the owner on 2026-11-16 (question dialog): **M3JDK-only forks.** A fork is compiled against M3JDK and
calls every `String` method directly, the new ones included; it then builds and runs only on M3JDK. No method
handles and no fallbacks. A fork that upstream compiles for Java 8 (commons-lang does) is raised to the M3JDK
level in the same change. Proof for a fork change without an M3JDK image: compile the fork's main sources
against the stock JDK with the edited `String` patched in, and compare the changed methods with the released
library jar on the same inputs.

## Landed

| Operation | New public method | Pull request |
|---|---|---|
| `countMatches` | `public int countMatches(int ch)` | M3jdk21 #3175, merged `74ba211ea9c2` |
| `indexOfDifference` | `public int indexOfDifference(String other)` | M3jdk21 #3186, merged `b7200d690615` |
| SEARCH family: `containsAny` (3 overloads), `containsAnyIgnoreCase`, `containsIgnoreCase`, `containsNone` (2), `containsOnly` (2), `containsWhitespace`, `indexOfAny` (3), `indexOfAnyBut` (2), `lastIndexOfAny`, `indexOfIgnoreCase` (2), `lastIndexOfIgnoreCase` (2), `ordinalIndexOf`, `lastOrdinalIndexOf`, `startsWithAny`, `endsWithAny`, `startsWithIgnoreCase`, `endsWithIgnoreCase`, WordUtils `containsAllWords` | 27 public methods, same names and overloads as the donors minus the first parameter; null arguments throw | M3jdk21 #3215, merged `851d1b31dbe7` (`leaf20/`) |
| EDGE + EMPTY_BLANK family: instance `chomp`, `chop`, `deleteWhitespace`, `normalizeSpace`, `stripAccents`, `strip(String)`, `stripStart(String)`, `stripEnd(String)`, `isNotBlank`, `isNotEmpty`; static null-safe `isNullOrEmpty`, `isAnyBlank`, `isAllBlank`, `isNoneBlank`, `isAnyEmpty`, `isAllEmpty`, `isNoneEmpty`, `defaultIfBlank`, `defaultIfEmpty`, `defaultString` (2), `firstNonBlank`, `firstNonEmpty`, `getIfBlank`, `getIfEmpty`, `stripAll` (2), `stripToEmpty`, `stripToNull`, `trimToEmpty`, `trimToNull` | 32 public methods: instance where the operation is about the text, static where null handling is the point | M3jdk21 #3217, merged `ce74f5b4005c` (`leaf21/`) |
| Speed of the SEARCH and EDGE families: `indexOfAny(char...)`, `indexOfAnyBut(char...)`, `containsWhitespace` scan the bytes of a flat Latin-1 string; `indexOfIgnoreCase`/`lastIndexOfIgnoreCase` (and the `contains...IgnoreCase` methods on them) compare a region only where the first unit can match; `normalizeSpace` returns the same string when nothing changes; `stripAccents` maps Latin-1 text through a 256-entry table built from the full algorithm | no API change; timings in `leaf22/BENCH_TABLE.tsv` | M3jdk21 #3218, merged `94953bc9f7dc` (`leaf22/`) |
| CLASS_TEST family: `isAlpha`, `isAlphaSpace`, `isAlphanumeric`, `isAlphanumericSpace`, `isAsciiPrintable`, `isNumeric`, `isNumericSpace`, `isAllLowerCase`, `isAllUpperCase`, `isMixedCase`; commons-lang `isWhitespace` is the existing `isBlank()` (same answers on every input of the proof) | 10 public instance methods, read as code points like commons-lang3 3.22; one 256-entry class table for Latin-1 units; M3 storage answered from prepared facts or read in bulk windows | this PR (`leaf23/`) |

**Lesson from `indexOfDifference`: the library's behaviour is found by testing, not assumed.** A plain
first-differing-char rule disagreed with commons-lang3 3.22.0 on 84,574 of 21.9 million pairs, and a pure
code-point rule on 418. The library moves the result back onto a high surrogate only when the difference is
found inside the shorter string. The JDK method documents and implements exactly that, so the library can
delegate. Run the donor jar as an oracle before writing the Javadoc of any absorbed operation.

**Lesson from the SEARCH family: commons-lang3 3.22 changed the surrogate rules of the character-set operations.** `containsAny`/`indexOfAny`/`containsNone` keep the older rule (a high surrogate in the set matches only before the character that follows it there, or alone at the end of the set) with one change: the low surrogate of a pair in the set no longer matches alone. `indexOfAnyBut`/`containsOnly` now read the text and the set as code points. `indexOfIgnoreCase` with an empty search returns `fromIndex` only up to the length. A probe table (`leaf20/DONOR_PROBE.txt`) found all three in one run; the first proof run had 11 failing checks, the second none.

**Lesson from the EDGE family: `stripAccents` is NFKD, removal of U+0300..U+036F, then NFC, plus twelve stroke letters.** A first NFKD-only reading failed on 34,266 of 201,289 inputs (Hangul and Indic text came back decomposed); a probe over every BMP character (`leaf21/ACCENT_PROBE.txt`) gave the composition step and the stroke-letter table. `normalizeSpace` turns NBSP into a space without collapsing it and trims the result.

**Lesson from the CLASS_TEST family (`leaf23/`): commons-lang3 3.22 reads the class tests as code points, and M3 storage needs its own path.** A probe (`leaf23/CLASS_PROBE.txt`) showed supplementary letters and digits passing and a lone surrogate failing, so the methods walk code points. The first version read UTF-16 and M3 storage through `charAt` and computed all six classes per code point; it was slower than the donor on UTF-16 text. Bulk windows (`getChars`, 512 units, a surrogate pair never split) and testing only the classes asked for fixed it. Prepared M3 facts carry exact ASCII regex classes, so on ASCII text `isAlpha`, `isNumeric`, `isAlphanumeric`, `isAllLowerCase`, `isAllUpperCase` and `isMixedCase` are answered without reading the text. The stock-JVM proof runs every method in three modes (flat; stub M3 storage with facts; stub M3 storage read in windows, `leaf23/M3StringStub.java.txt`), with compact strings on and off.

**Speed (owner, 2026-11-17: "that is exactly the reason why string needs to be so efficient").** From the third family on, every family PR carries measured kernels (no allocation when nothing changes, bulk scans over flat arrays and composite segments, timings against the donor); the first two families get a speed PR of their own.

**Lesson from the speed PR of families 1 and 2 (`leaf22/`): measure each kernel in its own JVM, and a prefilter must be cheaper than what it skips.** In one JVM the donor's own timings moved 2-10x between runs (one shared call site, machine load); one JVM per kernel and side, old and new interleaved, median of 3 forks, gave stable numbers. A first-unit prefilter with `Character.toUpperCase` on every unit made `indexOfIgnoreCase` slower than the plain loop (Latin-1 7.2 vs 5.9 microseconds, UTF-16 13.1 vs 9.4); ASCII case folding of ASCII units, the full one-unit rule only for other units, and the plain loop for a non-ASCII first unit made it 2.55x faster. Results on 4096-unit texts: 1.36x-2.91x faster than the merged code; commons-lang3 3.22.0 timings are bimodal across JVMs (`containsWhitespace` 3.7 or 31 microseconds), and against the donor's fastest fork the new code is 1.01x-16x faster (`leaf22/BENCH_TABLE.tsv`, re-recorded as `leaf22/VERIFY.txt` says); the donor proofs of both families stay at 0 mismatches with compact strings on and off, and the prefilter agrees with `regionMatches(true)` on all 2^32 pairs of BMP units.

**Scope set by the owner on 2026-11-17** (after "all of string functionality every developer dreamed of" and "every language, every parser, every text tool and format is strings"): besides commons-lang `StringUtils`, the catalogue takes commons-text, Guava strings (`CharMatcher`, `Splitter`/`Joiner` options, `CaseFormat`), Spring `StringUtils`, the string APIs of Python, Kotlin, C#, Swift, Rust, Go, JavaScript, Ruby and PHP, and popular string libraries on GitHub; and String-level kernels for CSV/TSV, JSON, XML/HTML, Markdown, source lexing and config formats, plus com.synexia's string code. Landing: one PR per family with an oracle proof and JUnit tests.

First fork change: hsoliwal/commons-lang #1, merged `db94e11f9ced`. `StringUtils.countMatches(CharSequence, char)`
calls `String.countMatches(int)` and `isBlank(CharSequence)` calls `String.isBlank()` for a `String` argument; the
compiler level in `pom.xml` goes from 1.8 to 21. Proof (`fork-commons-lang/` in the run record): the 264 main
sources compiled against the stock JDK with the edited `String` patched in; the fork's `StringUtils` and the
released 3.22.0 jar compared on 222,223 inputs for `isBlank` and 1,555,561 for `countMatches`, each text as a
`String` and as a `StringBuilder`, plus `null`: 0 mismatches. Not run: the fork's Maven build and tests (its
build plugins may reject non-Java-8 API until they are switched off), and anything inside an M3JDK VM.

How it was proven, as the pattern for the next ones (`leaf11/` in the run record): the method text is checked to
be verbatim in the head source, inserted into the stock `java/lang/String.java` from the JDK's `src.zip` together
with a null-returning `m3()` shim, compiled into `java.base` with `--patch-module`, and run inside the real
`java.lang.String` of the stock VM against a naive oracle and against the donor jar (commons-lang3 3.22.0). The
jtreg test has M3 storage runs for CI.
