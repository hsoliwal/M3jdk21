# CASE family draft (2026-11-17)

Draft only: nothing committed, pushed or opened as a PR. `case_family.txt` is inserted in
`String.head.java` right after the slice family text (leaf26 head + 654 lines).

## Methods (16 public)

| Method | Donor (oracle) |
|---|---|
| `capitalize()` | commons-lang3 3.22.0 `StringUtils.capitalize` |
| `uncapitalize()` | commons-lang3 `StringUtils.uncapitalize` |
| `swapCase()` | commons-lang3 `StringUtils.swapCase` |
| `swapCaseWords()` | commons-text 1.15.0 `WordUtils.swapCase` (differs from StringUtils, see below) |
| `capitalizeWords()` / `capitalizeWords(char...)` | `WordUtils.capitalize(str)` / `capitalize(str, char...)` |
| `capitalizeFully()` / `capitalizeFully(char...)` | `WordUtils.capitalizeFully` |
| `uncapitalizeWords()` / `uncapitalizeWords(char...)` | `WordUtils.uncapitalize` |
| `initials()` / `initials(char...)` | `WordUtils.initials` |
| `toCamelCase(boolean, char...)` | commons-text `CaseUtils.toCamelCase` |
| `caseFold()` | CPython 3.12 `str.casefold` (Unicode 15.0.0 CaseFolding.txt status C + F) |
| `equalsFoldCase(String)` | `a.casefold() == b.casefold()` (CPython 3.12) |
| `equalsSimpleFold(String)` | Go 1.26.2 `strings.EqualFold` (Unicode 15.0.0, status C + S) |

Private: `withFirst`, `mapCodePoints` (one windowed code point engine, modes `MAP_*`),
`swappedCase`, `isDelimiter`, `asciiWithout`, `bothAscii`, holder `CaseFolding` (generated
from `CaseFolding.txt` by `tools/gen_case_fold.py`: 227 runs of status C/S folds + 104 status F
folds; 1530 keys, built once into sorted `KEYS/SIMPLE/FULL`, `EXPANSIONS` and a Latin-1 table).
No name of the leaf26 head is redefined (grep checked).

## Semantic choices

- **WordUtils.swapCase is not StringUtils.swapCase.** WordUtils title-cases a lower-case letter
  at the start or after whitespace: `"a ǆ"` gives `"A Ǆ"` (StringUtils) and
  `"A ǅ"` (WordUtils). Two owners: `swapCase()` and `swapCaseWords()`.
- **Null delimiters.** The donors read `null` as "whitespace" (WordUtils) or "space only"
  (CaseUtils). Our rule is NPE for a null argument, so the whitespace rule is the no-argument
  overload (`capitalizeWords()` = `WordUtils.capitalize(str)`); an empty array keeps the donor
  meaning: no delimiter (`initials(new char[0])` = `""`; `toCamelCase(b)` = space only).
- **Delimiters are code points** read by `Character.codePointAt(delimiters, i)` at each index,
  exactly as WordUtils/CaseUtils build their sets (a `{HIGH, LOW}` array also adds the lone LOW).
- `capitalizeFully` and `toCamelCase` lower-case with `toLowerCase()` (default locale), as the
  donors do; delimiters are matched against the lower-cased text (CaseUtils).
- The donors advance by the width of the *mapped* code point; we advance by the input's. The two
  are the same on JDK 21: no `Character` case map changes a code point's width (checked over all
  0x110000 code points in every proof run, 0 found).
- `swapCaseWords` sets its "after whitespace" state from `Character.isWhitespace` for every code
  point; the donor sets it false for cased ones. Same because no whitespace code point is cased
  (checked in every run, 0 found).
- `caseFold()` is status C + F of CaseFolding.txt 15.0.0 (Turkic T left out; not normalized);
  CPython agrees on all 1530 folding code points and on every other code point (identity).
  The catalogue row names it `foldCase()`; the task named it `caseFold()`.
- `equalsSimpleFold`: Go's SimpleFold orbit equals the C + S equivalence class (verified on all
  2878 orbit links). The length gate relies on no simple fold leaving its plane (0 cross-plane
  links). Go strings cannot carry lone surrogates; the Go oracle maps a lone unit u to the
  plane-15 private-use rune 0xF0000 + (u - 0xD800), which like a Java lone surrogate equals only
  itself (the corpus holds no plane-15 character; the oracle panics otherwise).
- `equalsFoldCase(null)` and `equalsSimpleFold(null)` throw NPE (precedent:
  `equalsIgnoreAsciiCase`), unlike `equalsIgnoreCase(null)`.
- M3 paths: prepared-facts gates (ASCII without ALPHA / LOWER / UPPER returns this string; two
  ASCII strings compare by `equalsIgnoreAsciiCase`, exact for both folds), bulk windows of
  `SCAN_WINDOW` via `getChars` with a pair never split, unchanged runs appended as ranges, an
  unchanged string returned as is. No Latin-1 byte loop: the window path serves every storage.

## Proof (PROOF.txt)

Six runs (flat / m3-facts / m3-scan x CompactStrings on/off): `RESULT PASS failures=0`, 68
checks each. In-JVM donors on 612,880 texts (every BMP unit alone and in 4 contexts, 87,217
supplementary letters/cased code points alone and in 2 contexts, 22,621 strings of 0..4 units
over a 12-unit case alphabet, 928 window-seam texts at 505..520 / 1018..1030), delimiter
variants on 8 delimiter arrays. CPython: casefold of every code point 0..10FFFF and 460,127
texts; equality on 3,553,225 ordered pairs + 6,152 explicit pairs. Go: EqualFold on the same
pairs, SimpleFold orbit of every rune. javac `-Xlint:all,-this-escape -Werror` clean; doclint
`all/private` gives 0 findings inside the case-family lines (the stock file has its own).

Mutation check (one compact mode): 6 mutants; 5 killed (swapCaseWords state, a table delta,
engine seam, a facts gate, the bothAscii gate). The survivor (`||` to `&&` in
equalsSimpleFold's seam test) is equivalent: when only one side ends a window in a high
surrogate, the code points there cannot fold equal either way.

## Tests

- `StringCaseFamilyTest.java`: 615 tests (every storage form = flat, rules, long texts).
- `StringCaseFamilyDonorTest.java` (port_donor_tests.py, `case.spec.tsv`, commons-lang3
  StringUtilsTest): 29 assertions ported, 4 skipped (`NULL_RECEIVER`, `case.skipped.tsv`).
- `StringCaseFamilyTextDonorTest.java` (commons-text 1.15.0 WordUtilsTest + CaseUtilsTest via
  `tools/port_text_donor.py` + the same porter, `case-text.spec.tsv`): 193 ported, 15 skipped
  (`NULL_RECEIVER`, `case-text.skipped.tsv`).
- JUNIT.txt: 615 tests, 0 failures in all six runs.

## Open issues

1. Porter gap (port_donor_tests.py, not edited: outside this folder): a local reassigned later
   in a donor test (`array = ...;`) keeps its first value, so `testInitials_String_charArray`
   would be ported with wrong expectations; `ArrayUtils.EMPTY_CHAR_ARRAY` passes the
   unknown-symbol check and would not compile. `tools/port_text_donor.py` works around both
   (splits at reassignments, inlines a null local, rewrites the constant).
2. Not in this family (catalogue CASE rows left for later): toSnakeCase/kebab/start/space cases,
   Python title(), Go ToTitle, simple foldCaseSimple(), upperFirst/lowerFirst(boolean),
   FreeMarker capitalizeTokens, codePointCaseMap, ASCII-only case maps.
3. The `@run ... -XX:+UseM3StringStorage` lines and the real M3StringFacts were not exercised:
   the proof uses the stock JVM and the stub facts, as for the earlier families.
4. Not done here: timing tables and the atom/pattern ledger for this family.
5. Notices: `CaseFolding.txt` (Unicode data, Unicode license; the JDK already carries
   legal/unicode.md) and the commons-lang3 / commons-text tests (Apache-2.0; `donor-src/`
   holds the commons-text LICENSE and NOTICE) must be cited on landing.
