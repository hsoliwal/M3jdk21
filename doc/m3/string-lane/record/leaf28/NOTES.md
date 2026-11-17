# BUILD family draft (2026-11-17)

Text: `build_family.txt` (25 public methods, 6 private helpers), inserted in `String.head.java` right after
the slice family text (head = leaf26 head + this text, nothing else changed).

## Methods and donors

| Method | Donor (oracle) |
|---|---|
| `leftPad(int)`, `leftPad(int, char)`, `leftPad(int, String)` | commons-lang3 3.22.0 `StringUtils.leftPad` |
| `rightPad(int)`, `rightPad(int, char)`, `rightPad(int, String)` | commons-lang3 3.22.0 `StringUtils.rightPad` |
| `center(int)`, `center(int, char)`, `center(int, String)` | commons-lang3 3.22.0 `StringUtils.center` |
| `padStart(int, String)`, `padEnd(int, String)` | JavaScript `String.prototype.padStart/padEnd` (Node.js v25.9.0) |
| `zeroFill(int)` | CPython 3.12 `str.zfill` |
| `repeat(String separator, int count)` | commons-lang3 3.22.0 `StringUtils.repeat(String, String, int)` |
| `reverse()` | commons-lang3 3.22.0 `StringUtils.reverse` (= `StringBuilder.reverse`); Kotlin 2.2.20 `CharSequence.reversed` agrees |
| `reverseDelimited(char)` | commons-lang3 3.22.0 `StringUtils.reverseDelimited` |
| `rotate(int)` | commons-lang3 3.22.0 `StringUtils.rotate` |
| `appendIfMissing`, `appendIfMissingIgnoreCase` `(CharSequence, CharSequence...)` | commons-lang3 3.22.0 (`Strings.CS` / `Strings.CI`) |
| `prependIfMissing`, `prependIfMissingIgnoreCase` `(CharSequence, CharSequence...)` | commons-lang3 3.22.0 (`Strings.CS` / `Strings.CI`) |
| `wrap(char)`, `wrap(String)`, `wrapIfMissing(char)`, `wrapIfMissing(String)` | commons-lang3 3.22.0 |
| `overlay(String, int, int)` | commons-lang3 3.22.0 `StringUtils.overlay` |

Private helpers: `keepPairOrder`, `hasAnyAffix`, `hasAffix`, `padUnits`, `padded`, `tiled` (names grepped free in
the head). Reused head helpers: `splitsPair`, `SCAN_WINDOW`, `startsWithIgnoreCase`, `endsWithIgnoreCase`.
`build.provenance.tsv` holds the member -> donor rows for the atom-ledger step (same format as
`tools/PROVENANCE.tsv`).

## Semantic choices (each read from the donor source, then proven by the oracle)

- Null rule (house rule of the five merged families): a `null` argument throws `NullPointerException` where commons
  would return the input (`leftPad(5, null)`, `wrap(null)`, `overlay(null, ..)`, `repeat(null, 2)`, `appendIfMissing(null)`,
  a `null` suffixes/prefixes array). `null` ELEMENTS of the varargs arrays are skipped, as commons does (and as
  `startsWithAny` does in the head). The null check runs before every early return, so the throw does not depend on
  the other arguments.
- `size <= length()` is tested before `size - length()`, so `Integer.MIN_VALUE` sizes cannot overflow (proved).
- `leftPad/rightPad/center(int, String)`: commons 3.22.0 rule: an empty pad pads with spaces; when cutting the tiled
  pad would split a surrogate pair OF THE PAD, the padding is one unit shorter (`splitsSurrogatePair(padStr, pads %
  padLen)`; `"a".leftPad(2, PAIR) == "a"`). `center` = leftPad to `length + pads/2` then rightPad to `size`, each with
  that rule (left side gets the floor half). Char overloads have no pair rule.
- `padStart/padEnd` vs `leftPad/rightPad(int, String)`: the rules DIFFER (Node oracle), so both are landed:
  1. empty pad: JS returns the string unchanged, commons pads with spaces (`"abc".padStart(5, "")` = `"abc"`,
     `"abc".leftPad(5, "")` = `"  abc"`);
  2. surrogate pair of the pad: JS cuts anywhere (`"a".padStart(2, PAIR)` = HIGH + `"a"`), commons shortens.
  For a non-empty pad that is not cut inside a pair the two agree. `padStart(int)`/`padEnd(int)` (default pad
  `" "`) are identical to `leftPad(int)`/`rightPad(int)` and were NOT added. JS pads with `"null"` for a `null` pad
  (ToString); here `null` throws (house null rule).
- `zeroFill(width)`: Python semantics exactly, so the width counts CODE POINTS (Python `len`): a surrogate pair
  counts once, a lone surrogate once (`PAIR.zeroFill(2)` = `"0" + PAIR`). This differs from `leftPad`, which counts
  `char` values; it is the donor's rule. A leading `'+'`/`'-'` stays in front of the zeros only when zeros are added.
  `"".zeroFill(3)` = `"000"`. Short-cut without a scan when `width <= ceil(length/2)`; otherwise
  `codePointCount(0, length)` (the head's own M3 facts/geometry gates apply).
- `repeat(separator, count)`: commons = `removeEnd(repeat(str + sep, count), sep)` = this (sep this)^(count-1);
  `""` for count <= 0, this for count 1. `IllegalArgumentException("The requested result is too large for a String.")`
  exactly when commons' `Math.multiplyExact(len + sepLen, count)` overflows (count >= 2); no allocation before.
- `reverse()`: commons reverses with `StringBuilder.reverse`: units reversed, then every (low, high) run swapped from
  the start. So a valid pair keeps its order, AND a misordered low-high run of the input becomes a valid pair in the
  output (`(LOW + HIGH).reverse()` = HIGH + LOW). This exact rule is followed (catalogue row "propose code point" and
  Jinjava's pair-breaking reverse were not).
- `reverseDelimited(c)`: commons `split` (empty tokens dropped: adjacent separators, leading/trailing) + `join` with
  single separators. A single surviving token is returned as a `substring` range; no separator -> this.
- `rotate(shift)`: unit rotation exactly as commons (`offset = -(shift % length)`); it may split a surrogate pair.
- `append/prependIfMissing[IgnoreCase]`: an empty suffix/prefix or an empty element always matches; case-insensitive
  matching is `regionMatches(true, ...)` (head `endsWithIgnoreCase/startsWithIgnoreCase`), as `Strings.CI` delegates
  for String arguments.
- `wrap(char)`/`wrapIfMissing(char)`: `U+0000` is a no-op (commons `CharUtils.NUL`).
- `overlay`: indexes clamped to `0..length`, swapped when start > end, then a cut inside a pair moves outward
  (3.22.0 rule); `overlay("", i, i)` returns this.

## M3 paths

- `reverse()`: flat Latin-1 strings reverse their value bytes into a new LATIN1 string; UTF-16 and M3 storage are
  read in bulk windows of `SCAN_WINDOW` units via `getChars` into the reversed `char[]` (pair order is repaired on
  the whole result array afterwards, so a pair split by a window seam is irrelevant); prepared facts with `latin1`
  skip the pair-repair pass.
- `reverseDelimited(c)`: prepared facts with `!mayContainCodeUnit(c)` answer `this` without reading; otherwise
  `lastIndexOf(int, int)` (the head's M3 path) finds the separators.
- `zeroFill`: `codePointCount` (head: LATIN1 coder / facts.codePointCount / precomputed geometry).
- Builders return `substring` ranges and `concat` joins (M3 slices and joined storage on M3JDK): pads are
  `pad.repeat(n).concat(pad.substring(0, r))` concatenated with this; `rotate` = two ranges joined; `overlay` = two
  ranges around the overlay; `wrap*`/`*IfMissing` = `concat`. Unchanged results return `this`.
- Not used: `M3StringFacts.firstUtf16Unit/lastUtf16Unit` (present in the stub) could gate `wrapIfMissing(char)`;
  the real M3StringFacts field set was not read in this session, so only `ascii/latin1/mayContainCodeUnit`
  (already read by the head) are used.

## Proof (`PROOF.txt`)

`proof/run_proof.sh <draft-dir> <work-dir> "<commons-lang3-3.22.0.jar>;<kotlin-stdlib-2.2.20.jar>"`: the seven texts
(difference_base, search, edge, class, compare, slice, build) are verbatim once in the head; inserted into stock JDK 21
`String.java` with the stub `m3()` shim; `javac -Xlint:all,-this-escape -Werror` clean; the harness writes the corpus,
`python_oracle.py` (CPython 3.12.6, `str.zfill`, UTF-16 surrogatepass) and `node_oracle.js` (Node v25.9.0, `padStart/
padEnd`, unit-exact) answer it once; `BuildFamilyProof` then runs in flat / m3-facts / m3-scan, compact on and off.
Corpus: all 2801 strings of 0..4 units over `a A , - + HIGH LOW` + 232 window-seam texts (505..520, 1018..1030 units;
tails pair, ",a", "aA", "") = 3033 texts. 1014 checks (911 commons/Kotlin + 13 python zfill rows + 90 node pad rows;
sizes incl. `Integer.MIN_VALUE` and length-relative sizes, pads with pairs, a misordered low-high pad, lone halves).
All six runs: `RESULT PASS failures=0`. The first run found one defect (`zeroFill` on the empty string read
`charAt(0)`); fixed before the final run. A doclint pass (`javac -Xdoclint:all/protected`) over the generated
String.java reported nothing.

## JUnit (`JUNIT.txt`)

`proof/run_junit.sh <draft-dir> <work-dir> <junit-platform-console-standalone-1.11.4.jar>` (after run_proof.sh):
`StringBuildFamilyTest` (259 storage-form cases + 6 rule tests incl. window seams) and `StringBuildFamilyDonorTest`
(27 tests): 292 tests, 292 successful, 0 failed in each of the six configurations.

## Donor tests

`build.spec.tsv` -> `StringBuildFamilyDonorTest.java` from commons-lang3 3.22.0 `StringUtilsTest.java` and
`StringUtilsAbbreviateTest.java`, then `prune_uncompilable.py`: 238 assertion statements (+6 local declarations) in
27 tests ported; 58 statements skipped (`build.skipped.tsv`): 40 NULL_RECEIVER, 13 OTHER_ARITY (2-argument
`repeat(str, n)` / `repeat(char, n)`: they belong to `String.repeat(int)`, whose negative-count and overflow rules
differ from commons and are not this family's), 3 LOCAL_STATE (loops), 2 COMPILE (`CharUtils.NUL`; the rule is in the
oracle proof).

Porter: the shared v1 (`J:/m3-scratch/donor-tests-20261117/port_donor_tests.py`) changed during this run (14:25,
NPE_ARRAY rule, `assertNpeOr` helper) and was not edited. `tools/make_porter_v11.py` derives
`tools/port_donor_tests.py` (DONOR_TEST_PORT v1.1) from the current v1 by exact once-only substitutions:
1. arity-qualified spec ops (`repeat/3`), other arities skipped as `OTHER_ARITY:<op>/<n>`;
2. null rule `NPE_ARG_ELEM` (used by append/prependIfMissing*): `(T) null` in a varargs position is a null ELEMENT
   (skipped, donor answer kept), a literal `null` or `(T[]) null` throws;
3. transitive locals: a kept local declaration comes after the locals its initializer reads (recovered 9 surrogate-pad
   assertions of `testPadMixedSurrogatePair`, and their `pad` declaration, that v1 lost to a compile prune);
4. header says v1.1; the `assertNpeOr` doc comment shortened to fit 100 columns.
Proposed for folding into the shared porter.

## Open issues

- The head is not compiled as M3JDK here (needs the M3jdk21 tree); like the merged families it is proven on stock JDK
  21 with the M3 stub. The real `-XX:+UseM3StringStorage` @run lines of the JUnit test were not executed.
- `zeroFill` counts code points (Python) while the pad methods count `char` values: intended (donor rule), flagged
  for owner review.
- Not landed (not in this step's list; catalogue rows only): Python `str.center` (odd split: `'ab'.center(5,'*')` =
  `'**ab*'`, commons gives `"*ab**"`, would need its own name), Python `ljust/rjust` (code-point widths),
  FreeMarker `?right_pad` tiled alignment (`rightPadTiled`), Kotlin `padStart/padEnd` (IAE on negative length), Guava
  `Strings.padStart/padEnd` (same as the char overloads).
- `CharUtils.NUL` -> `'\0'` mapping is missing from the porter (2 COMPILE skips).
- Timing tables for BUILD not run (owed like COMPARE/SLICE).
- Atom ledger not produced (not in this step's outputs); `build.provenance.tsv` is ready for it.
- `proof/BuildFamilyProof.java` (harness only, not landed) has lines over 100 columns, as the leaf26 harness does.
