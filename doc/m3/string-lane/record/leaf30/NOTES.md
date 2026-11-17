# REPLACE family: draft notes (2026-11-17)

Draft only: nothing committed, pushed or opened as a PR. `replace_family.txt` is inserted in
`String.head.java` right after the SLICE family text (`proof/make_head.py`, FAMILY_INSERT v1).

## Methods (21 public) and donors

| Method | Donor (rule copied) |
|---|---|
| `replace(String search, String replacement, int max)` | commons-lang3 3.22.0 `Strings.CS.replace(text, s, r, max)` |
| `replaceOnce(String, String)` | commons `Strings.CS.replaceOnce` (= max 1) |
| `replaceIgnoreCase(String, String)` | commons `Strings.CI.replace` (max -1) |
| `replaceIgnoreCase(String, String, int max)` | commons `Strings.CI.replace` |
| `replaceOnceIgnoreCase(String, String)` | commons `Strings.CI.replaceOnce` |
| `remove(String)` | commons `Strings.CS.remove` (= replace with "", max -1) |
| `removeIgnoreCase(String)` | commons `Strings.CI.remove` |
| `remove(char)` | commons `StringUtils.remove(String, char)` |
| `replaceEach(String[], String[])` | commons `StringUtils.replaceEach` (`replaceEachOnce`) |
| `replaceEachRepeatedly(String[], String[])` | commons `StringUtils.replaceEachRepeatedly` (DEFAULT_TTL 5 = 6 passes) |
| `replaceChars(String searchChars, String replaceChars)` | commons `StringUtils.replaceChars(String, String, String)` |
| `replaceFirstChar(IntUnaryOperator)` | Kotlin 2.2.20 `String.replaceFirstChar((Char) -> Char)` |
| `replaceBefore(String, String)` / `(String, String, String)` | Kotlin `replaceBefore(delimiter: String, ...)` |
| `replaceAfter(String, String)` / `(String, String, String)` | Kotlin `replaceAfter` |
| `replaceBeforeLast(String, String)` / `(String, String, String)` | Kotlin `replaceBeforeLast` |
| `replaceAfterLast(String, String)` / `(String, String, String)` | Kotlin `replaceAfterLast` |
| `translate(java.util.Map<Integer, String>)` | CPython 3.12 `str.translate` |

Not added: `replaceChars(char, char)`. commons-lang3 3.22.0 defines it as `str == null ? null :
str.replace(searchChar, replaceChar)`, so it is `String.replace(char, char)`; the donor's assertions
for it are ported onto `String.replace(char, char)` (5 statements).

Private helpers (new names, none in the head before): `replaceOccurrences`, `mayContainUnit`,
`EACH_PASSES`, `replacedEach`, `earliest`, `spliced`, nested `Splicer`. Reused from earlier
families: `indexOfIgnoreCase(CharSequence, int)`, `noUnitMayOccur`, `asciiMask`, `inMask`,
`hasWideUnit`, `SCAN_WINDOW`.

## Semantic choices

- Null arguments throw NullPointerException (`Objects.requireNonNull` before any other rule, so
  also on an empty string). Where commons answers the text for a null search, replacement,
  `searchChars`, `replaceChars` (commons reads it as "") or a null array, M3JDK throws; the proof
  checks all 56 such calls and the ported donor assertions became `assertThrows(NPE)` (55).
- Null ELEMENTS of the `replaceEach` arrays follow commons: a pair whose search is null or empty or
  whose replacement is null is left out. Order of checks as in commons: empty string, empty
  `searchList`, empty `replacementList` give this string before the length check; then
  IllegalArgumentException for differing lengths.
- `replaceEachRepeatedly`: exactly commons' budget, 6 passes; IllegalStateException when the 6th
  pass still replaces an occurrence. A pass that replaces an occurrence by an equal text counts as
  a replacement (commons builds a new String, so `"a"` with `{"a"} -> {"a"}` throws); the private
  pass signals it with `null`.
- An empty search occurs nowhere (commons), unlike JDK `replace(CharSequence, CharSequence)`; no
  two-argument `replace(String, String)` was added (it would change the JDK method's overload
  resolution for String arguments).
- Ignoring case = `regionMatches(true, ...)` at each start, which is what commons `Strings.CI`
  does for String arguments on the running JDK (3.22.0 no longer lower-cases the search).
- Unchanged gives this: every method returns this string when the result equals it, including the
  corner where `replaceEach`/`translate` replace text yet end equal (checked by length then
  `equals`). Kotlin's 3-argument forms return `missingDelimiterValue` itself when the delimiter is
  absent (it may be null, as `ifAbsent` of the SLICE family's `substringBefore(String, String)`).
- `replaceFirstChar`: Kotlin's `(Char) -> Char` as `IntUnaryOperator`, the result narrowed to a
  `char` as by a cast. Kotlin's `(Char) -> CharSequence` overload is not added.
- `replaceBeforeLast` / `replaceAfterLast` search with `lastIndexOf(delimiter, length() - 1)` as
  Kotlin does: an empty delimiter is found at the last character, never in an empty string.
- `translate`: code points (a pair is one, a lone surrogate is its own); key absent = kept, key
  mapped to null = removed (Python None), else the mapped string. Python's int values and custom
  `__getitem__`/LookupError mappings have no Java counterpart beyond `Map.get`/`containsKey`.

## M3 paths and range reuse

- Facts gates: `mayContainUnit` (first unit of each case-sensitive search, `remove(char)`, each
  `replaceEach` search) and `noUnitMayOccur` (`replaceChars`); ignoring case relies on
  `indexOfIgnoreCase`'s own Latin-1 gate.
- Bulk windows: `replaceChars` and `translate` read `getChars` windows of
  `min(length, SCAN_WINDOW)`; `translate` never splits a surrogate pair between windows. Searches
  go through `indexOf` / `lastIndexOf` / `indexOfIgnoreCase`; no `charAt` loop over the text.
- Builders: `Splicer` keeps the first edit pending; a single edit becomes `spliced`, which returns
  this, the replacement, a `substring` (empty replacement at either end: an M3 slice), or on M3
  storage `substring(..).concat(replacement).concat(substring(..))` (joined M3 ranges), else one
  exact-size StringBuilder. Two or more edits copy the ranges between them with
  `StringBuilder.append(this, from, to)`.

## Proof (PROOF.txt)

`proof/run_proof.sh <draft> <draft>/work "<commons-lang3 3.22.0 jar>;<kotlin-stdlib 2.2.20 jar>"`:
seven texts verbatim once in the head, inserted into stock JDK 21 String.java, javac
`-Xlint:all,-this-escape -Werror` clean (also `-Xdoclint:all/protected`: 0 warnings),
1505 checks, RESULT PASS in all six runs (flat, m3-facts, m3-scan x CompactStrings on/off).
Corpus A = 3033 texts (every 0..4-unit string over a b A , U+D83D U+DE00 x, plus seam texts with the
deciding unit at 505..520 and 1018..1030); corpus B = 3033 texts over a A k U+212A U+D801 U+DC00
U+DC28 (Kelvin sign, Deseret pair halves) for the ignoring-case rules. Oracles: commons-lang3
3.22.0, Kotlin stdlib 2.2.20 (replaceFirstChar through reflection, it is @InlineOnly), CPython
3.12.6 (`proof/python_oracle.py` writes TRANSLATE.bin). Every String answer equal to its text is
checked to be that text.

Mutation check of the proof (work/mut, flat run, each mutant on the generated String.java): 8 of 8
caught: EACH_PASSES 7; replaceBeforeLast with lastIndexOf(delimiter); translate keeping null-mapped
keys; replaceEach without the equal-content check; spliced without the unchanged check;
replaceChars with lastIndexOf; translate without the window pair guard (seam text at 511);
replaceEach position cache searched from the old index.

## JUnit (JUNIT.txt)

`proof/run_junit.sh <draft> <draft>/work` after run_proof.sh: StringReplaceFamilyTest (259 storage
forms + 4 rule tests) and StringReplaceFamilyDonorTest (14 tests) = 277 tests, 0 failed in all six
runs.

## Donor tests

`donor/donor_prepass.py` (REPLACE_DONOR_PREPASS v1) on lang3/StringUtilsTest.java (the only lang3
test calling these operations), then `port_donor_tests.py replace.spec.tsv`, then
`donor/wrap100.py` (WRAP_100 v1: 5 over-100-column lines fixed by 6 breaks outside literals, 1
javadoc re-flow and 1 message literal split as `"a " + "b"`; token stream asserted unchanged). Prepass rewrites, from the
donor's own definitions: 15 x `StringUtils.replace(t, s, r)` -> `replace(t, s, r, -1)` (the JDK
two-argument `replace` would be called otherwise) and 6 x `replaceChars(t, char, char)` ->
`replaceCharsChar` -> `String.replace(char, char)`. Result: 167 statements in 14 tests (164
assertions, 55 of them assertThrows(NullPointerException); 2 local declarations; 1 bare call
from LANG-1453), 44 skipped (all NULL_RECEIVER), see replace.skipped.tsv. The prepassed donor copy
is regenerable: `python -I donor/donor_prepass.py <lang3>/StringUtilsTest.java donor/StringUtilsTest.java`.

## Open issues

- Not proven on M3JDK itself: the stub's M3 path only routes through `m3() != null`, the facts
  gates and `getChars`; real M3 `concat`/`substring` range reuse and `StringBuilder.append` of M3
  strings run only in the M3JDK build (the @run lines with -XX:+UseM3StringStorage).
- No timing table yet for this family.
- `port_donor_tests.py` (shared tool, not edited): its own generated `assertNpeOr` javadoc line is
  104 columns and it cannot wrap statements without a comma that fits; WRAP_100 fixed both here.
  It also cannot tell overloads apart by arity or argument type (hence the prepass).
- Kotlin's Char-delimiter overloads (`replaceBefore(Char, ...)` etc.) and Python `str.maketrans`
  are not in this family.
- Catalogue rows of the REPLACE family not in this task's list (overlay, removeRange, filter,
  replaceLast, replacePairs, tr/squeeze/delete, replaceLineEndings, ...) remain open.
