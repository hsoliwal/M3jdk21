# ESCAPE_MARKUP family draft (2026-11-17)

Draft only: nothing committed, pushed or opened as a PR. `escape_markup_family.txt` is inserted in
`String.head.java` right after the case family text (leaf31 head + 1477 lines; `tools/make_head.py`).
The family text is generated: `tools/gen_markup_tables.py` fills the two table literals of
`tools/escape_markup_family.template.txt`.

## Methods (16 public methods, 5 public nested enums)

| Member | Donor (oracle) |
|---|---|
| `escapeHtml3()` / `unescapeHtml3()` | commons-text 1.15.0 `StringEscapeUtils.escapeHtml3` / `unescapeHtml3` |
| `escapeHtml4()` / `unescapeHtml4()` | commons-text `StringEscapeUtils.escapeHtml4` / `unescapeHtml4` |
| `escapeXml10()` / `escapeXml11()` | commons-text `StringEscapeUtils.escapeXml10` / `escapeXml11` |
| `unescapeXml()` | commons-text `StringEscapeUtils.unescapeXml` |
| `escapeHtml4Xml()`, `escapeHtml5()`, `escapeHtml5Xml()` | unbescape 1.1.8 `HtmlEscape` methods of the same names |
| `escapeHtml(HtmlEscapeType, HtmlEscapeLevel)` | unbescape `HtmlEscape.escapeHtml(text, type, level)` (all 30 pairs) |
| `unescapeHtml()` | unbescape `HtmlEscape.unescapeHtml` (full WHATWG table, 2231 names) |
| `escapeXml(XmlVersion, XmlEscapeType, XmlEscapeLevel, boolean)` | unbescape `XmlEscape.escapeXml10/11[Attribute](text, type, level)` (all 64) and its 8 fixed escapers |
| `unescapeXmlStrict()` | unbescape `XmlEscape.unescapeXml` |
| `escapeHtml(boolean quote)` | CPython 3.12.6 `html.escape(s, quote)` |
| `unescapeHtml5Python()` | CPython 3.12.6 `html.unescape(s)` |
| `String.HtmlEscapeType`, `String.HtmlEscapeLevel`, `String.XmlEscapeType`, `String.XmlEscapeLevel` | unbescape enums of the same names (same constants, same order) |
| `String.XmlVersion` (`XML_1_0`, `XML_1_1`) | the catalogue's `escapeXml(XmlVersion, ...)` proposal |

Private: one escape engine `escapeMarkup(rule)` (rule = kind + level + flags; `markupKeeps`,
`appendMarkup`, `markupUntouched` facts gate), one unescape engine `unescapeMarkup(rule)` with four
reference readers (`markupCommons`/`markupCommonsNumber`, `markupLenient`, `markupStrict`,
`markupPython`), the table class `MarkupNames` and its two holders `Html4Names` (252 names) and
`Html5Names` (2231 names). No name of the leaf31 head is redefined (grep checked: no member named
`escape*`, `unescape*`, `markup*`, `*Html*` or `*Xml*` existed; `translateEscapes` is unrelated); the
head's `mayContainUnit`, `SCAN_WINDOW` and `m3()` are reused.

### Catalogue choices

- The catalogue (TEMPLATE_ENGINES_OPERATIONS.tsv, family ESCAPE) proposes the parameterised forms
  `escapeHtml(HtmlEscapeType, HtmlEscapeLevel)` and `escapeXml(XmlVersion version, XmlEscapeType type,
  XmlEscapeLevel level, boolean attributeValue)`, so this draft has ONE method each with small public
  nested enums, not unbescape's 12 XML and 5 HTML String escape method names. The fixed unbescape names that the
  catalogue rows name as methods (`escapeHtml4Xml`, `escapeHtml5Xml`) and `escapeHtml5` (task) are
  kept as one-line shortcuts. unbescape's `escapeHtml4` (level 2) collides with commons
  `escapeHtml4`; as the catalogue says it is reached through
  `escapeHtml(HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL, LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT)`.
- Left out (catalogue rows of other rules, not in this task): the `HtmlEscapeProfile` tables (JSTL,
  mustache.java, Handlebars 3/4, JTE content/attribute, ST4 numeric), `escapeHtmlText/Attribute`
  (WHATWG serialization), `unescapeHtml(boolean inAttribute)` (WHATWG spec incl. the attribute rule;
  not CPython's rule, see below), `escapeXmlNonAscii`/`encode|decodeNumericEntities` (PHP mb_*),
  `nl2br`, `decodeMarkdownReferences`, FreeMarker's output-format dispatch.

## Semantic choices

- **commons escapeXml10/escapeXml11 are points of unbescape's XML space**: `escapeXml(XML_1_0 | XML_1_1,
  CHARACTER_ENTITY_REFERENCES_DEFAULT_TO_DECIMAL, LEVEL_1_ONLY_MARKUP_SIGNIFICANT, false)`; the proof
  checks both against commons-text on every text, so they share the engine but keep their own names.
- **unescapeXmlStrict()**: unbescape's `XmlEscape.unescapeXml` is not commons' `unescapeXml` (the task
  gives that name to commons): it reads only a lower-case `x` and throws `IllegalArgumentException`
  for a reference to a character XML 1.1 does not allow (0, a surrogate, U+FFFE, U+FFFF, above
  U+10FFFF), where commons decodes those up to U+10FFFF, throws only for values in
  (U+10FFFF, `Integer.MAX_VALUE`] and leaves larger values as text. Own name per the family rule; the
  name is this draft's choice (the catalogue row calls the unbescape rule `unescapeXml()`).
- **unbescape's lenient name search is reproduced, quirk included.** `unescapeHtml()` decodes a legacy
  name without `;` at the start of a longer run only where unbescape's binary search over its sorted
  table meets it (`HtmlEscapeSymbols.binarySearch` keeps the best partial among the probed entries).
  6,892 of 414,028 legacy-name probes differ from the "longest legacy prefix" rule (`&centes`, `&notit`
  stay as they are; CPython decodes both). The table is in unbescape's order (';' below end of name,
  end below every other character; ORDER_KEY in the generator reproduces SORTED_NCRS exactly) and the
  search is ported; the Javadoc says so.
- **HTML5 escape names** are unbescape's choice among a code point's names (first in its initializer
  order below U+2FFF, last in sorted order above, from its overflow map), dumped from the jar by
  `tools/OracleDump.java` and carried as markers in the table text: 1446 code points.
- **Null arguments**: `escapeHtml`/`escapeXml` with a null enum throw `NullPointerException`
  (family rule); unbescape throws `IllegalArgumentException`.
- **Exceptions of the donors are kept**: commons `unescapeHtml3/4`/`unescapeXml` throw
  `IllegalArgumentException` ("Not a valid Unicode code point: 0x...", as `Character.toChars`) for a
  value in (U+10FFFF, `Integer.MAX_VALUE`]; `unescapeXmlStrict` throws unbescape's
  "Not a valid XML char in character reference: 0x...". The proof compares exception classes.
- **unbescape HTML escaping escapes U+0000 and every unpaired surrogate at every level** (`&#0;`,
  `&#55349;`), as the donor does; documented.
- **CPython rules differ from every other method, so both were added** (proof `distinct` lines):
  `html.escape(s, True)` writes the apostrophe as `&#x27;` (unbescape `&#39;`/`&apos;`, commons keeps
  it); `html.escape(s, False)` escapes only `& < >`. `html.unescape` reads runs of 1..32 code points of
  any character but TAB LF FF space `<` `&` `#` `;`, takes the longest name prefix, and removes
  references to U+0001..U+0008, U+000B, U+000E..U+001F, U+007F, U+FDD0..U+FDEF and the noncharacters
  U+xFFFE/U+xFFFF. The name `unescapeHtml5Python()` is the task's.
- **Table sources**: `donor/entities.json` (WHATWG, sha256
  d741d877ac77c4194c4ad526b5b4a19aef8dfe411ab840a466891cdbb9f362e6, 2231 names, 106 without `;`, 93 of
  two code points). The generator checks it equals CPython's `html.entities.html5` and unbescape's
  HTML5 table, and that unbescape's HTML4 escape map equals commons-text's `EntityArrays` (252 names).
  Table text: 22,514 chars (HTML5, front-coded names in lookup order) and 2,376 chars (HTML4), built
  once into `char[]` + `int[]` arrays on first use of a holder.
- **M3 path**: escapers: prepared `M3StringFacts` answer in O(1) when the text is ASCII and no ASCII
  unit the rule changes may be present (`mayContainCodeUnit`); otherwise bulk windows of `SCAN_WINDOW`
  units via `getChars`, a surrogate pair never split, the first change found before anything is copied,
  clean runs appended as ranges, `this` when nothing changes. Unescapers: `mayContainUnit('&')` then
  `indexOf('&', from)` between references; each reference is read on its own units only. A name run
  is read up to `MARKUP_NAME_MAX` = 40 units (every name is shorter, so a longer run is decided by
  its first units; unbescape's search included, since its partial rank shifts by a constant); a
  numeric reference consumes its whole digit run, as the donors do.
- No regular expression in product code. `REGEX_SPEC.tsv` (28 rows) lists the CPython `_charref` rule
  (implemented here without a regex) and the regexes of the donor-test porter/pruner run by this draft.

## Proof (PROOF.txt)

Six runs (flat / m3-facts / m3-scan x CompactStrings on/off): `RESULT PASS failures=0` in each, 127
checks each. Corpus: 239,292 escape texts (every string of 0..3 units over a 27-unit alphabet with & < > " ', the controls and C1 units the
XML rules treat specially, U+00A0, U+00E9, U+0152, U+2329, U+27E8, U+FB00, U+FFFE, the halves
0xD835/0xDD1E/0xDD1F; 4 units over 12; every BMP unit alone and in two contexts; window seams at
505..520 and 1018..1030), plus all 1,048,576 supplementary code points for the fixed-rule methods and
672 sampled ones (all mathematical alphanumerics) for the 94 parameterised configurations; 875,253
unescape texts (all 0..4-token strings over 18 tokens, each BMP unit after `&`/`&#`, each WHATWG name
in six contexts, each legacy name + one or two letters/digits, numeric references of boundary values
in five forms, overflowing/padded digit runs, runs of 26..42 and 100 units around the 32-code-point
and 40-unit run limits, seams). The `distinct` lines (informational) count the texts that tell each
added rule from its nearest other one, e.g. `html.escape(s, True)` vs `escapeHtml4Xml()` 33,730 texts,
`html.unescape` vs `unescapeHtml()` 7,849 (`&ltx`), `unescapeXmlStrict()` vs `unescapeXml()` 18,770.
Every method must also return the receiver when its answer equals it. javac
`-Xlint:all,-this-escape -Werror` clean; `-Xdoclint:all/protected` 0 findings (whole file).
`-Xdoclint:all/private` reports missing @param/@return on private helpers, as for the case and replace
families.

Mutation check (`tools/mutate.py`, m3-facts, compact on; MUTATION.txt; run 1 predates the name-run
cap, whose edit touched none of its mutated lines): 13 mutants, 10 killed. The
3 survivors are equivalent: (1) the partial-rank comparison in `findLenient` never decides, since no
legacy name is a prefix of another (checked: 0 of 106 x 105 pairs), so at most one partial exists;
(2) CPython's 32-code-point cap changes only how far an unmatched run is consumed (no `&` inside it;
the longest name has 31 letters, and prefixes can only be the 6-letter-or-shorter legacy names);
(3) `unescapeHtml3`'s limit 0xFF vs 0x100: HTML 4.01 has no name for U+0100. Stronger mutants of the
same three spots (drop the partial result, cap 30, limit 0x2FF) are all killed. Killed: Windows-1252
index, commons int overflow, facts gate, window seam, XML level of U+0085, strict upper-case X, HTML5
escape name, and the three stronger ones.

## Tests

- `StringEscapeMarkupFamilyTest.java`: 10,448 tests (every storage form = flat, receiver when
  unchanged, rules, long texts).
- `StringEscapeMarkupFamilyDonorTest.java` (port_donor_tests.py via `tools/port_markup_donor.py`,
  `escape_markup.spec.tsv`, commons-text 1.15.0 `StringEscapeUtilsTest`; the HTML_ESCAPES loops are
  unrolled into one method per row first): 77 assertions ported, 10 skipped
  (`escape_markup.skipped.tsv`: 5 NULL_RECEIVER, 5 LOCAL_STATE).
- `StringEscapeMarkupFamilyPythonDonorTest.java` (`tools/port_python_donor.py`, CPython 3.12.6
  `Lib/test/test_html.py` run with recording wrappers): 298 assertions ported, 0 skipped.
- JUNIT.txt: 10,501 tests, 0 failures in all six runs.

## Open issues

1. Admission: 16 public methods and 5 public nested enums in `java.lang.String`
   (no-public-sprawl rule); the enums follow the catalogue's parameterised proposal.
2. Names to confirm: `unescapeXmlStrict()` (this draft) and `unescapeHtml5Python()` (task).
3. unbescape's own tests were not ported: its test sources are not in the sources jar and the
   porter reads only `StringUtils.op(` statements; the CPython tests were ported by a separate
   recording tool, not by port_donor_tests.py.
4. Porter gaps (port_donor_tests.py not edited, outside this folder): `String[][]` constants and loop
   variables are not read (worked around by unrolling in `tools/port_markup_donor.py`).
5. As for the earlier families, the real M3 storage (`-XX:+UseM3StringStorage` @run lines) and the
   real `M3StringFacts` were not exercised: the proof uses the stub.
6. Not done here: timing tables and the atom/pattern ledger for this family.
7. Notices on landing: WHATWG entities.json (CC BY 4.0), unbescape 1.1.8 (Apache-2.0; ported search
   and fix-up logic), commons-text 1.15.0 tests (Apache-2.0; `donor/commons-text-LICENSE.txt`,
   `donor/commons-text-NOTICE.txt`), CPython 3.12 test_html.py (PSF-2.0).
8. The downloaded commons-text 1.15.0 sources/test-sources jars were deleted after porting (sha256
   6fa2f1db12dc4147094be928a5b54d567cea287df54e4c76b7dfb6096c05439d and
   126208c1faac3b71149af1cb14dc4bf035af6df6f39bf8e1e3c0dc6b28546fd6); re-download to re-run the port.
9. Size: the two table literals add about 300 lines (24.9K chars) to `String.java`; they could move to
   a java.base resource if the JDK prefers, at the cost of a resource read on first use.
10. Reproduction: `work/` was deleted to save disk (J: shared). `tools/gen_markup_tables.py` needs the
    oracle dumps of `tools/OracleDump.java` in its oracle directory; `proof/run_proof.sh . work <jars>`
    then `proof/run_junit.sh . work` rebuild everything else (about 9 minutes on this machine).
