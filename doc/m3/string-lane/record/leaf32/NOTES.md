# ESCAPE_CODE family draft (2026-11-17)

Draft only: nothing committed, pushed or opened as a PR. `escape_code_family.txt` (784 lines) is
inserted in `String.head.java` right after the case family text (leaf31 head + 784 lines; tool
`tools/make_head.py`). No name of the leaf31 head is redefined (REGEX_SPEC R08/R09 greps; the
eleven texts compile together under `javac -Xlint:all,-this-escape -Werror`).

## Methods (17 public members + 1 public nested enum)

| Member | Donor (oracle, run in-JVM) |
|---|---|
| `escapeJava()` / `unescapeJava()` | commons-text 1.15.0 `StringEscapeUtils.escapeJava` / `unescapeJava` |
| `escapeEcmaScript()` / `unescapeEcmaScript()` | `StringEscapeUtils.escapeEcmaScript` / `unescapeEcmaScript` (= UNESCAPE_JAVA) |
| `escapeJson()` / `unescapeJson()` | `StringEscapeUtils.escapeJson` / `unescapeJson` (= UNESCAPE_JAVA) |
| `escapeCsv()` / `unescapeCsv()` | `StringEscapeUtils.escapeCsv` / `unescapeCsv` (one excluded divergence, below) |
| `escapeXSI()` / `unescapeXSI()` | `StringEscapeUtils.escapeXSI` / `unescapeXSI` |
| `escapeCssString()` | unbescape 1.1.8 `CssEscape.escapeCssString(text)` = compact hex, LEVEL_2 |
| `escapeCssIdentifier()` | unbescape `CssEscape.escapeCssIdentifier(text)` = compact hex, LEVEL_2 |
| `unescapeCss()` | unbescape `CssEscape.unescapeCss` |
| `escapePropertiesValue()` | unbescape `PropertiesEscape.escapePropertiesValue(text)` = LEVEL_2 |
| `escapePropertiesKey()` | unbescape `PropertiesEscape.escapePropertiesKey(text)` = LEVEL_2 |
| `unescapeProperties()` | unbescape `PropertiesEscape.unescapeProperties` |
| `translateEscapes(EscapeSyntax)` + `enum EscapeSyntax {FTL, ST}` | FTL: FreeMarker 2.3.35 `StringUtil.FTLStringLiteralDec`; ST: StringTemplate 4.3.4 `STLexer` (STRING token of `<"body">`, antlr-runtime 3.5.3) |

Default levels were read from unbescape's own javadoc and code (`CssEscape.java:252,978`,
`PropertiesEscape.java:274,912`): every zero-argument unbescape escaper is LEVEL_2 (basic escape
set + all non-ASCII), CSS with BACKSLASH_ESCAPES_DEFAULT_TO_COMPACT_HEXA.

Private (all new names): `escapedUnits` (one window engine for the nine escape rules),
`escapesAt`, `appendEscape`, `upperHexDigit`, `isAsciiHexDigit`, `asciiHexRunEnd`,
`wordUnitsOnly`, `firstBackslash`, `appendJavaEscape`, `appendCssHexEscape`, `cssWhitespaceAt`,
`appendFtlEscape`, constants `ESC_*`, holder `EscapeRules` (per rule: flags, escaped ASCII set,
backslashed ASCII set, built once into two mask words each by the head's `asciiMask`). Reused
head helpers: `SCAN_WINDOW`, `asciiMask`, `inMask`, `indexOfAny(char...)`, `m3()`,
`M3StringFacts.REGEX_WORD`, `mayContainCodeUnit`. Product code uses no regex and never touches
`value` directly (only `getChars`, `indexOf`, `charAt` at escape sites, `substring`, `length`).

## Semantic choices

- **One rule per name; the task names win over the catalogue.** The catalogue
  (TEMPLATE_ENGINES_NOTES) gives the zero-argument `escapePropertiesKey/Value()` names to the
  `java.util.Properties.store` rule (VelocityTools EscapeTool); the task gives them to unbescape's
  default level. They are unbescape's here; the store rule (every key space, leading value space,
  `# ! = :`, U+0020..U+007E) is not landed and needs its own name. Likewise the catalogue's
  "JSON unescape" row is unbescape's (`\/` decoded, exactly 4 hex digits, unknown kept); the task's
  `unescapeJson()` is commons-text's (= `unescapeJava`). No level/type enum was added: the task's
  parenthesised list names zero-argument members only, and the catalogue's level enums
  (CssStringEscapeLevel, PropertiesKeyEscapeLevel, JavaEscapeLevel, JsonEscapeType/Level, ...) are
  new public types that need owner admission (no-public-sprawl).
- **unescapeJava (= Ecma, Json) keeps the donor's exact unicode-escape reading**: one or more
  `u`, an optional `+` (the donor skips one), then exactly 4 characters parsed by
  `Integer.parseInt(..., 16)`: a leading sign is accepted (`\u-EDD` gives U+F123) and non-ASCII
  digits count (`Character.digit`); fewer than 4 characters or a non-number throws
  IllegalArgumentException, as the donor does. Proven on every 4-character body over 14 tricky
  units (signs, U+FF11, U+0661, `\`, `u`, a high surrogate).
- **unescapeCsv**: commons-text 1.15.0 strips the enclosing quotes even when the inside has no
  comma/quote/CR/LF (the catalogue's "keeps the quotes" describes older commons-lang; the oracle
  decides). **Excluded divergence**: the donor throws StringIndexOutOfBoundsException on the
  one-character text `"` (it is both the opening and the closing quote); the method returns it
  unchanged (it is not a quoted field). The proof checks the donor really throws there and that
  this is the only special case (its oracle wrapper maps exactly that input).
- **escapeXSI** removes LF and CR LF, keeps a lone CR (donor lookup table: `\r\n` key, no `\r`).
- **CSS**: a code point is escaped as a whole (a pair is one `\1D51E`), a lone surrogate as its
  unit; the trailing space after a hex escape looks at the unit after the code point (string: hex
  digit or space; identifier: hex digit only). `& < >` (and `:` in identifiers) use hex, not a
  backslash, because unbescape's backslash table has them commented out.
- **unescapeCss**: zero, surrogate values and values above U+10FFFF give U+FFFD; a high-surrogate
  escape followed by a low-surrogate escape gives the pair; a trailing backslash is kept.
- **translateEscapes(EscapeSyntax)** reads a literal *body* (no quotes). Only quote-independent
  syntaxes fit a one-argument method; both landed ones are. A trailing lone backslash throws
  IllegalArgumentException (FTL: "last character is backslash"; ST: the lexer's "EOF in string").
  FTL ParseException answers as IllegalArgumentException (Java's translateEscapes precedent).
  ST bodies with an unescaped `"` or U+FFFF have no engine answer (no ST literal holds the first;
  STLexer reads U+FFFF as its EOF char, an engine bug not replicated) and are not in the corpus.
- **M3 paths**: every escaper runs `escapedUnits`: a prepared-facts gate (ASCII word units only:
  nothing to escape; identifier also needs a letter first) returns this in O(1); otherwise bulk
  windows of SCAN_WINDOW via `getChars`, each keeping 2 units of lookahead for the next window, so
  a surrogate pair and the unit after it (CSS trailing-space rule, XSI CR LF, identifier `-`) are
  never split from the deciding unit; the clean prefix and every clean run are appended as one
  range; nothing to escape returns this. Unescapers: facts gate (word units, or
  `mayContainCodeUnit('\\')` false) then `indexOf('\\')` jumps; text between escapes copied as
  ranges; `charAt` only at escape sites. `unescapeCsv` returns an M3 slice (`substring`).

## Proof (PROOF.txt)

Six runs (flat / m3-facts / m3-scan x CompactStrings on/off), two harnesses each; every run
`RESULT PASS failures=0`. EscapeCodeFamilyProof: 33 checks (16 donor equalities + 16 "returns
this when unchanged" + the excluded-divergence check) over 3,378,801 escaper texts and 4,477,106
unescaper texts: every string of 0..3 units over 61 escape units (all specials of all nine rules,
C0/C1 edges, ASCII hex digits, U+FFFF and D835/DD1E/DD1F), every 4-unit string over 17 units,
every string of 0..4 units over 28 unescape units and of 5 units over 11, unicode-escape and CSS
hex/surrogate-pair templates, every BMP unit in 8 contexts, every supplementary code point, the
donors' escape outputs as unescape inputs, and window-seam texts (deciding unit at 505..520 and
1018..1030). EscapeSyntaxProof: 5 checks over 582,982 FTL and 475,985 ST bodies (engine code run
in-JVM). javac `-Xlint:all,-this-escape -Werror` clean. doclint `all,-missing/private`: 0
findings inside the family lines; the `missing` group adds 52, all on private helpers and `ESC_*`
constants (no @param/@return or no comment), the style of the merged families' private members.

Mutation check (tools/mutants.py, MUTANTS.txt, compact strings on): 8 mutants, 8 killed:
window lookahead 2 -> 1 (escapeCssString seam), JSON without DEL (escapeJson), identifier `--`
rule (escapeCssIdentifier), properties `\u` with 3 digits (unescapeProperties), octal third
digit after 4..7 (unescapeJava), facts gate ignoring the identifier's first unit (killed only in
the m3-facts mode, so the gate is exercised), FTL `\x` with 5 digits, ST without `\t`.

## Tests

- `StringEscapeCodeFamilyTest.java`: 30,941 parameterized tests (every storage form = flat, all
  18 answers, on every string of 0..4 units over 13 units) + 9 rule tests (per method, template
  syntaxes, window seams).
- `StringEscapeCodeFamilyDonorTest.java` (port_donor_tests.py via `tools/port_escape_donor.py`,
  `escape_code.spec.tsv`, commons-text 1.15.0 StringEscapeUtilsTest): 80 assertions ported, 15
  skipped (`escape_code.skipped.tsv`: 11 NULL_RECEIVER, 4 LOCAL_STATE).
- JUNIT.txt: 30,964 tests (both classes), 0 failures in all six runs.
- Reproduce: `bash proof/run_proof.sh <draft> <work> "$(cat proof/donor_classpath.txt)"`, then
  `bash proof/run_junit.sh <draft> <work>` (about 20 min + 3 min); `tools/mutants.py` for MUTANTS.txt.

## Not landed (left out, with reason)

- EscapeSyntax VELOCITY, PEBBLE, JEXL, EL: quote-dependent; they need the delimiter, which a
  one-argument `translateEscapes(EscapeSyntax)` cannot take. EL probe (expressly 6.0.0 via
  ELProcessor): in `'...'` only `\'` and `\\` are accepted, in `"..."` only `\"` and `\\`; every
  other escape (including `\#`, `\$`, `\n`) is an ELException, so the catalogue row ("# and $
  escapes, other backslashes kept") does not hold at the engine level. JEXL `buildString` keeps
  `\'` inside `"..."` (separator rule) and with no separator stops at the first U+0000.
- EscapeSyntax MVEL: `ParseTools.handleStringEscapes` marks removed units with U+0000, so every
  NUL in a body is dropped; its unicode check reads one unit past the end
  (ArrayIndexOutOfBounds) and accepts only upper-case hex. Engine bugs; not replicated, not proven.
- EscapeSyntax OGNL: no lexer-level oracle run (catalogue: backtick escape yields '0', a bug).
- `escapeLiteral(EscapeSyntax.JEXL, char)`: JEXL is not in the enum.
- Catalogue level/profile overloads (unbescape Java/JavaScript/Json/Csv levels, JTE/Jinjava/FTL
  profiles) and the Properties.store rule: need new public types / names (admission).

## Open issues

1. The `@run ... -XX:+UseM3StringStorage` lines and the real M3StringFacts were not exercised:
   the proof uses the stock JVM and the stub facts, as for the earlier families.
2. Porter gap (port_donor_tests.py, not edited): a donor statement that declares a local from a
   spec call (`final String value = StringEscapeUtils.escapeJava(x)`) is skipped as LOCAL_STATE.
3. Notices on landing: commons-text tests (Apache-2.0; `donor-src/META-INF/` holds its LICENSE
   and NOTICE; `donor-src/.../StringEscapeUtilsTest.java` is the port input), unbescape (Apache-2.0),
   FreeMarker (Apache-2.0), ST4 (BSD-3-Clause) as oracles/rule sources.
4. Not done here: timing tables and the atom/pattern ledger for this family.
5. The relayed user request (byte[] value / char[] chValue as interned, internally protected
   arrays, a JDK-wide invariant) is not part of this family's task; this text reads neither field.
   The leaf31 head has no `chValue` field (`private final byte[] value;` only, line 170).
