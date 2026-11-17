# SPLIT_JOIN family draft (draft-split, 2026-11-17)

Status: draft only. Nothing was committed, pushed or opened as a PR. Nothing outside this folder was edited.

## Methods in split_family.txt (17 public methods, 13 private helpers)

| Method | Donor and rule |
|---|---|
| `String[] split()` | commons-lang3 3.22.0 `split(str)`: runs of whitespace (`Character.isWhitespace(char)`, unit by unit); no empty parts |
| `String[] split(char)` | commons `split(str, char)` |
| `String[] splitPreserveAllTokens()` | commons `splitPreserveAllTokens(str)` |
| `String[] splitPreserveAllTokens(char)` | commons `splitPreserveAllTokens(str, char)` |
| `String[] splitPreserveAllTokens(String separatorChars)` | commons `splitPreserveAllTokens(str, String)` |
| `String[] splitPreserveAllTokens(String separatorChars, int max)` | commons `splitPreserveAllTokens(str, String, int)` |
| `String[] splitByWholeSeparator(String)` | commons `splitByWholeSeparator(str, sep)`; `""` means whitespace |
| `String[] splitByWholeSeparator(String, int max)` | commons `splitByWholeSeparator(str, sep, max)` |
| `String[] splitByWholeSeparatorPreserveAllTokens(String)` | commons, same name |
| `String[] splitByWholeSeparatorPreserveAllTokens(String, int max)` | commons, same name |
| `String[] splitByCharacterType()` | commons `splitByCharacterType`: code points by `Character.getType` |
| `String[] splitByCharacterTypeCamelCase()` | commons `splitByCharacterTypeCamelCase` |
| `String[] partition(String)` | CPython 3.12 `str.partition` |
| `String[] rpartition(String)` | CPython 3.12 `str.rpartition` |
| `String[] rsplit(String separator, int maxSplit)` | CPython 3.12 `str.rsplit(sep, maxsplit)` |
| `String[] splitLines(boolean keepEnds)` | CPython 3.12 `str.splitlines(keepends)` |
| `String[] chunked(int size)` | Kotlin 2.2.20 `CharSequence.chunked(size)` |

Every element of every result is a `substring` range of the receiver (`partition`'s middle element is the matched range,
not the argument). `String.split(String regex[, int])` and `lines()` are unchanged. The new `split(char)` cannot
change how existing code resolves, because a `char` argument was never applicable to `split(String)`.

## Semantic choices

- **Null arguments.** A null argument throws NullPointerException, following the family rule. The commons rule
  "null separator means whitespace" is therefore not available through `splitByWholeSeparator(null)` or
  `splitPreserveAllTokens((String) null)`. Use `""`, `split()` or `splitPreserveAllTokens()` instead.
- **Python works on code points.** In `partition`, `rpartition` and `rsplit`, a match counts only when it neither
  starts nor ends between the two halves of a surrogate pair. This is how CPython sees a string: a pair is one code
  point, and a lone surrogate is a code point of its own. For example, `"\uD835\uDC00".partition("\uD835")` gives
  `[pair, "", ""]`. Python's ValueError("empty separator") becomes
  `IllegalArgumentException("empty separator")`.
- **`rsplit` searches right to left.** Matches do not overlap, so `"aaa".rsplit("aa", -1)` gives `["a", ""]`. A
  negative `maxSplit` means no limit. Python's `rsplit(None, n)` (whitespace) has no overload, because `null` throws
  NullPointerException.
- **`splitLines` boundaries.** It uses Python's boundaries: `\n \r \r\n \x0b \x0c \x1c \x1d \x1e \x85 \u2028 \u2029`.
  It returns `String[]`, like the rest of the family, where the catalogue proposed `List<String>`. Every boundary is
  one BMP unit, so the unit scan matches Python exactly.
- **`chunked` counts chars.** It works on `char` values, as Kotlin does, so a chunk boundary can split a surrogate
  pair. It throws `IllegalArgumentException("size N must be greater than zero.")`, Kotlin's message.
- **Whitespace for the commons methods.** These follow `Character.isWhitespace(char)` per unit, as commons does.
  NEL (U+0085) is therefore not whitespace, but it is a `splitLines` boundary. The proof corpus covers both cases.

## M3 paths

- **Separator-unit scans.** `split()`, `splitPreserveAllTokens()`, the separator-chars forms and the whitespace
  form of whole separators all go through `splitOnUnits`. Prepared M3 facts can rule out every separator unit, and
  then the answer is the receiver alone with no scan:
  - Whitespace is ruled out when the string has none of the regex classes `SPACE|HORIZONTAL|VERTICAL` and
    `mayContainCodeUnit` is false for U+001C..U+001F.
  - Separator chars are ruled out by `noUnitMayOccur`.

  Otherwise the text is read in bulk windows (`getChars` into a `char[min(length, SCAN_WINDOW)]`).
- **`splitLines`.** The facts gate is `REGEX_VERTICAL` plus U+001C..U+001E. The scan uses bulk windows. When the
  CR of a CRLF ends a window, the following unit is read with one `charAt` at the seam.
- **`splitByCharacterType[CamelCase]`.** Bulk windows read the text, and a surrogate pair is never split between two
  windows (the class-family rule).
- **Single-char and whole separators.** `split(char)`, the one-char forms, `splitByWholeSeparator*`, `partition`,
  `rpartition` and `rsplit` use `indexOf` and `lastIndexOf`, the JDK's own M3-aware searches. No duplicate fast path
  was added. The Python alignment check is `splitsPair`, which is O(1) per candidate.

## Proof (PROOF.txt)

- **Run.** `proof/run_proof.sh` inserts the seven texts (`difference_base`, `search`, `edge`, `class`, `compare`,
  `slice`, `split`) into the stock JDK 21 String.java with the `m3()` shim. It compiles them into java.base with
  `-Xlint:all,-this-escape -Werror`, which gives no warnings. It then writes the corpus, has CPython 3.12.6 answer
  it (`proof/python_oracle.py`, gzip answers), and runs SplitFamilyProof in six runs: flat, m3-facts and m3-scan, each
  with CompactStrings on and off.
- **Corpus.** 14,012 texts:
  - every string of 0..4 units over `a B 1 space , CR LF NEL` plus the two halves of U+1D400 (an upper-case letter);
  - every string of 1..3 units over `TAB 1C 1F 0B 2028 3000 A0 z : D83D DE00 Z a`;
  - window-seam texts that put the deciding unit at 505..520 and 1018..1030. These include a CRLF and a surrogate
    pair across the seam at 511/512 and 1023/1024.
- **Checks.** There are 288 checks:
  - 210 against commons-lang3 3.22.0;
  - 10 against Kotlin `StringsKt.chunked`;
  - 68 against Python: partition and rpartition for 11 separators, rsplit for 11 separators x maxsplit -1/0/1/2, and
    splitlines with keepends false and true.

  Exceptions compare by class.
- **Result.** RESULT PASS, failures=0, in all six runs.
- **Mutation check.** One run on a scratch copy seeded three faults, and all three were caught:
  - dropping the Python alignment check: partition mismatches;
  - using index-1 instead of the previous code point's start in camelCase: caught by the single text `B` + pair + `a`;
  - dropping the U+001C..U+001F facts check: m3-facts mode only.

## JUnit (JUNIT.txt)

`proof/run_junit.sh` compiles StringSplitFamilyTest and StringSplitFamilyDonorTest against the patched java.base. It
runs them with junit-platform-console-standalone 1.11.4 in the same six runs. Each run found 599 tests: 599 successful,
0 failed.

- **StringSplitFamilyTest.** 585 parameterized texts, each checked as the flat string, as a range of a longer string
  and as a concatenation. It also has three rule tests: commons rules, Python and Kotlin rules, and long texts across
  windows.
- **StringSplitFamilyDonorTest.** 11 tests ported from commons-lang3 3.22.0 StringUtilsTest.

## Donor test port

`port_donor_tests.py` in this folder is a v1.1 copy of `J:/m3-scratch/donor-tests-20261117/port_donor_tests.py`. I
copied it at 14:25 and did not edit the shared original. The copy adds two backward-compatible features:

1. **Optional 5th spec column: argument shapes** (`-` none, `c` char literal, `*` any). With `split -|c`, the donor's
   `split(str, String[, int])` calls are skipped as `OVERLOAD_NOT_IN_FAMILY` instead of being ported onto the regex
   `String.split`, which would compile but test the wrong method.
2. **`--whole-methods` flag.** A donor @Test method can be ported whole, local state included, when:
   - every StringUtils statement in it translates;
   - the only exceptions are lone `assert*(StringUtils.op(null...))` statements, which are dropped and listed as
     NULL_RECEIVER;
   - it calls no donor helper and names only known constants.

Result: 152 donor assertions ported (6 whole methods and 6 single statements, in 11 @Test methods). 31 statements
skipped, all listed in `split.skipped.tsv`:

- 14 NULL_RECEIVER;
- 8 LOCAL_STATE, in methods that also pass a null separator, which has a different meaning here;
- 5 NPE_NOT_IN_ASSERT;
- 4 OVERLOAD_NOT_IN_FAMILY (`split(str, String[, int])`).

Without `--whole-methods`, the v1 behaviour ports 35 statements and skips 58.

## Not landed and open issues

1. **`split(String separatorChars)` and `split(String separatorChars, int max)` (commons).** They have exactly the
   signatures of `String.split(String regex)` and `split(String regex, int limit)`, so they would change existing
   behaviour. They were left out. TEMPLATE_ENGINES_OPERATIONS.tsv proposes `tokenize(String delimiterChars)` (the
   commons rule) as their clash-free name. The body would be one line (`splitOnUnits(chars, max, false)`), but the
   name is not in this task's method list, so the owner has to admit it.
2. **Donor whitespace via a null argument** (commons `null` separator, Python `rsplit(None)`) is not available,
   because of the NullPointerException rule (see above).
3. **The catalogue path in the task was wrong.** `string-converge-20261117/catalogue-20261117/` does not exist. The
   files are at `J:/m3-scratch/catalogue-20261117/` (both TSVs, rows read) and
   `record-pr/edit/doc/m3/string-lane/record/catalogue/` (CROSS_LANGUAGE only).
4. **The porter v1.1 changes stay in this folder.** Promoting them to the shared porter is a separate step. The
   shared porter was being changed by another lane while this draft ran: 14,576 bytes at first read, 17,875 bytes at
   14:25.
5. **The proof runs on the stub, not on the M3JDK build.** `M3StringFacts` field and class names (`regexAnyClasses`,
   `REGEX_SPACE`, `REGEX_HORIZONTAL`, `REGEX_VERTICAL`, `mayContainCodeUnit`) are taken as the stub copies them. The
   jtreg `@run` lines with `-XX:+UseM3StringStorage` have not been run on a real M3JDK build.
6. **Script-language interop was not checked.** Groovy's GDK `String.split()` (StringTokenizer whitespace) and
   Kotlin/Groovy extension names (`chunked`, `partition`) could be shadowed by the new Java members in those languages.
7. **No timing table and no atom ledger** were made for this family; neither was in this task's output list.
