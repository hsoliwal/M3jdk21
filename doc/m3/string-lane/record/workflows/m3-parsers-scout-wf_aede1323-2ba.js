export const meta = {
  name: 'm3-parsers-scout',
  description: 'Scout and adversarially verify how parsers in M3JDK can use M3String and M3AST: javac lexer/parser and the M3AST owners, JDK text parsers that consume Strings, and library parsers (Jackson, the M3 parser pack)',
  phases: [
    { title: 'Scout', detail: 'three read-only readers: javac parser and M3AST, JDK text parsers, library parsers' },
    { title: 'Verify', detail: 'one skeptic per scout tries to refute every candidate against the source' },
  ],
}

const ROOT = 'I:/m3-scratch/string-converge-20261116'
const COMMON = `
You are working on M3JDK (hsoliwal/M3jdk21: a JDK 21 fork where java.lang.String can be backed by an M3String, a composite/rope of atoms and tuples with precomputed monoid measures in M3StringFacts; javac carries an M3AST owner). The owner's orders of 2026-11-16: "M3 string is extremely crucial", "is m3ast also connected?", "all of the string operations and even M3JDK compiler optimizations", and now: "additionally parsers using m3string and m3ast".

READ-ONLY TASK. Do not create, edit or delete any file. Do not run git status, builds, javac or tests. Do not fetch from the network. Use Glob, Grep and Read (read large files in line ranges). Your final answer is the structured output only.

Sources:
- A full checkout of M3jdk21 from 2026-11-14 (two days older than master; say so when you cite it): K:/m3-work/wt-fix3/ with src/jdk.compiler/share/classes/com/sun/tools/javac (parser/: JavaTokenizer, UnicodeReader, JavacParser, Tokens, Lexer, ScannerFactory; util/: Names, Name, Utf8NameTable or SharedNameTable; tree/: JCTree, TreeInfo; m3/: M3AST, M3ASTPC, M3CompilerPatternHints, M3PrimitiveImageCompiler), src/java.base/share/classes (java/lang Integer, Long, Double, FloatingDecimal in jdk/internal/math, java/util Scanner, StringTokenizer, Properties, Formatter, UUID, java/io StreamTokenizer, java/net URI, java/time/format), src/java.xml, the M3 parser pack m3/tooling/module-packs/parser (it holds ten M3AST* files in jdk/m3/parser), m3/ports/compiletime (its own ast package with six M3AST* files), m3/vendor/m3/m3-mindex/compiler (four M3AST* files), m3/tooling/module-packs/jackson and the vendored Jackson under m3/vendor, m3/ports/regex-cursor.
- M3 string owners at master: CURRENT String.java and M3StringFacts.java in ${ROOT}/leaf1/head/src/java.base/share/classes/java/lang/ ; the other java.lang owners in ${ROOT}/master-java-lang/ (ignore the String.java and M3StringFacts.java there); jdk.internal.mindex (M3TokenIndex: lanes for kind, lexeme id, start/end, flags, binding to an M3AST node; M3TokenPattern; M3Boundary), jdk.internal.m3.* (intern) and java.util.regex in ${ROOT}/owner/src/src/java.base/share/classes/
- Invariants and state of the string lane (read sections 2, 3 and 6): ${ROOT}/record/docs/m3/string-lane/HANDOVER.md

What an M3-backed String offers a parser: bulk getChars windows and leaf-run cursors (no per-unit dispatch), substrings and concatenations that share units (no copy), an intern pool with canonical ids, measures (ascii and latin1 flags, class masks over all units and any unit for DIGIT, WORD, SPACE, LOWER, UPPER, ALPHA, ALNUM, HEX and others, first and last units, trim and strip edges, line terminator count, hash), and for token streams the M3TokenIndex lanes.

Owner's rules that bind any proposal:
- One AST in M3JDK: the compiler's own AST is the single source; marks and censuses run as javac plugins; no second parser or second AST for the same language. M3AST* types exist in four places today, which this rule does not allow to grow.
- No new public or protected class or member without the owner's admission; internals through qualified exports.
- The JDK owns each fast implementation once, in java.base; libraries keep their upstream packages and API.
- Behaviour must stay identical: parse results, error messages and positions, exceptions and their order, accepted and rejected inputs (including non-ASCII digits and white space where the stock code accepts them).
- A per-unit charAt on a composite M3String is an owner dispatch, not an array read; toCharArray flattens the whole value.

Report at most 6 candidates, best first. Each candidate must cite a file and line range you actually read and quote up to 6 lines of the code as evidence. If you are not sure a claim holds, lower the confidence and say what is unverified. Do not invent line numbers.`

const CANDIDATES = {
  type: 'object',
  properties: {
    candidates: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          id: { type: 'string' },
          title: { type: 'string' },
          file: { type: 'string' },
          lines: { type: 'string' },
          evidence: { type: 'string' },
          current_cost: { type: 'string' },
          proposed_change: { type: 'string' },
          algorithm: { type: 'string' },
          expected_gain: { type: 'string' },
          behavior_risk: { type: 'string' },
          public_api_change: { type: 'boolean' },
          proof_plan: { type: 'string' },
          confidence: { type: 'number' },
        },
        required: ['id', 'title', 'file', 'lines', 'evidence', 'current_cost', 'proposed_change', 'algorithm', 'expected_gain', 'behavior_risk', 'public_api_change', 'proof_plan', 'confidence'],
      },
    },
    notes: { type: 'string' },
  },
  required: ['candidates'],
}

const VERDICTS = {
  type: 'object',
  properties: {
    verdicts: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          id: { type: 'string' },
          refuted: { type: 'boolean' },
          evidence_exists_as_quoted: { type: 'boolean' },
          reason: { type: 'string' },
          corrected_claim: { type: 'string' },
          implementation_hazards: { type: 'string' },
        },
        required: ['id', 'refuted', 'evidence_exists_as_quoted', 'reason'],
      },
    },
    missed: { type: 'string' },
  },
  required: ['verdicts'],
}

const SCOUTS = [
  {
    key: 'javac-parser-m3ast',
    prompt: `${COMMON}

ANGLE: the Java parser in javac, and M3AST.
Establish from the code: (1) how javac turns source text into tokens and names today: UnicodeReader's buffer, JavaTokenizer's handling of identifiers, literals and comments, how a Name is created (name table over bytes), where java.lang.String objects are created for literals and identifiers (stringVal, new String(...), StringBuilder), and where javac compares or hashes them; (2) what com.sun.tools.javac.m3.M3AST, M3ASTPC, M3CompilerPatternHints and M3PrimitiveImageCompiler do: are they built from JCTree by a tree scanner, a TaskListener or plugin, or by a second parse; what they record (kinds, spans, facts, images) and who reads the result; (3) how the other three M3AST families relate to the javac one (jdk.m3.parser pack, jdk.m3.compiletime.ast, the vendored mindex compiler ast): duplicated declarations, different shapes, or adapters; which of them parse Java source themselves; (4) whether any of them fills M3TokenIndex lanes (kind, lexeme id, binding to an AST node) or carries M3String atoms for lexemes.
Candidates, smallest first, that make the parser side use M3String and M3AST under the one-AST rule: for example lexeme interning through the M3 pool so identical identifiers and literals share one atom and carry facts; string literal values handed to the constant pool as M3-admitted values with facts; a token lane (M3TokenIndex) filled from the javac scanner so token-aware regex and recipes do not re-lex; removing a duplicate M3AST family by pointing its readers at the javac owner; the compile-time harvest reading the javac M3AST instead of its own ast package. For each: where it goes, what it must not change (javac diagnostics, positions, class file bytes), and the proof plan (compile a corpus with and without, compare class files and diagnostics).`,
  },
  {
    key: 'jdk-text-parsers',
    prompt: `${COMMON}

ANGLE: text parsers inside the JDK that take a String or CharSequence.
Read how each reads its input and catalogue it in "notes" as one line per parser: "API | how the input is read (charAt loop, toCharArray, substring per token, regex, getChars window) | per-unit owner dispatches or flatten for an M3-backed input | what an M3 facility could answer". Cover: Integer.parseInt / parseUnsignedInt / decode and the Long equivalents (they call Character.digit per unit); Double.parseDouble and Float (jdk.internal.math.FloatingDecimal.readJavaFormatString: trim, then charAt); Boolean.parseBoolean (equalsIgnoreCase); UUID.fromString; HexFormat; java.util.StringTokenizer; java.io.StreamTokenizer; java.util.Scanner (regex based); java.util.Properties.load (LineReader); java.util.Formatter's format-string parsing (it runs a regex over every format string unless a fast path exists); java.text.MessageFormat.applyPattern; java.net.URI's Parser and URLDecoder / URLEncoder; java.time.format.DateTimeFormatter parsing of CharSequence (subSequence and charAt); String.split with a single-character separator (fast path) and with a regex; Pattern.quote; Base64 decoding from a String; java.xml scanners only at the level of how they take String input.
Candidates: the six with the best gain for M3-backed inputs, each with an exact gate or a cheaper read: for example a class-mask gate (all units ASCII digits, from regexAllClasses with the ascii fact) that lets parseInt run over one bulk window with no Character.digit table walk and no per-unit dispatch; trim and strip edges for number parsers that trim first; a bulk window instead of charAt for short inputs; the line terminator measure for line-based loaders; a constant format string's parse cached per String identity; a split fast path that walks leaf runs. State for each which inputs keep the stock path (non-ASCII digits, signs, radix other than 10, overflow) and the differential proof plan against stock results and exceptions (same NumberFormatException messages).`,
  },
  {
    key: 'library-parsers',
    prompt: `${COMMON}

ANGLE: parsers that M3JDK ships as libraries or packs: Jackson (m3/tooling/module-packs/jackson and the vendored jackson-core under m3/vendor), the M3 parser pack (m3/tooling/module-packs/parser, package jdk.m3.parser), the regex-cursor port, the text tools pack, and any JSON, CSV or properties parser under m3/ (Grep for class names ending in Parser, Lexer, Tokenizer, Scanner under K:/m3-work/wt-fix3/m3 and list what exists).
Establish from the code: (1) for jackson-core: how field names and string values are produced (TextBuffer, the symbol tables CharsToNameCanonicalizer and ByteQuadsCanonicalizer, String.intern use, new String(char[], off, len)), how a String input is read (ReaderBasedJsonParser over a char[] buffer: where the String is copied in), and how output escapes strings (WriterBasedJsonGenerator / UTF8JsonGenerator: getChars windows or charAt loops); (2) for the M3 parser pack: what it parses (Java? a grammar set?), whether it builds its own AST (the M3AST* files) or adapts the compiler's, how lexemes are held; (3) which of these already call into java.base M3 internals and through which export.
Candidates, smallest first: for example symbol tables that hand out M3-interned names (one canonical atom per field name across parsers, with facts), string values built as composite slices of the input when the input is an M3-backed String (no copy until escapes appear), generator escaping driven by the ascii flag and class masks (no per-unit test when a value needs no escape: the ascii fact is not enough by itself, quotes, backslashes and control units still need a test, so say which measure would be exact), the parser pack reading the javac M3AST instead of holding a second AST family. For each: the exactness argument (same tokens, same values, same error positions), which rule it touches (upstream API kept, one AST, one fast implementation in java.base, qualified exports), and a differential proof plan against the unmodified library on its own test inputs.`,
  },
]

const verifyPrompt = (scout, result) => `${COMMON}

YOU ARE THE SKEPTIC for the scout "${scout.key}". Another agent produced the candidates below. Your job is to REFUTE them against the source. Default to refuted=true when you cannot confirm a claim from the code.
For every candidate: (1) open the cited file and lines: does the quoted evidence exist there? (2) is the claim about how the parser reads or builds text real, and is the claimed cost real for an M3-backed input; (3) exactness: same parse results, same error messages and positions, same accepted and rejected inputs (non-ASCII digits, signs, white space definitions, surrogates), same class file bytes for compiler-side changes; (4) rules: one AST (no new AST family or second parser), no new public or protected member, upstream library API kept, internals only through qualified exports; (5) is the gain plausible, or does the gate cost more than the loop it replaces for the short inputs these parsers usually see?
Give for each candidate: refuted true/false, whether the evidence exists as quoted, the reason with file and line, a corrected claim when the idea survives in a narrower form, and the hazards an implementer must handle. In "missed", name better candidates of the same angle that the scout overlooked, and report wrong rows you find when you spot-check at least eight rows of any table in the notes.

SCOUT ANGLE:
${scout.prompt.split('ANGLE:')[1]}

CANDIDATES (JSON):
${JSON.stringify(result.candidates.slice(0, 6), null, 1)}

SCOUT NOTES:
${(result.notes || '(none)').slice(0, 26000)}`

const results = await pipeline(
  SCOUTS,
  scout => agent(scout.prompt, { label: `scout:${scout.key}`, phase: 'Scout', schema: CANDIDATES }),
  (found, scout) => {
    if (!found || !found.candidates || !found.candidates.length) return { scout: scout.key, candidates: [], verdicts: [], notes: found && found.notes, missed: '' }
    if (found.candidates.length > 6) log(`${scout.key}: ${found.candidates.length} candidates, verifying the first 6 only`)
    if ((found.notes || '').length > 26000) log(`${scout.key}: notes are ${found.notes.length} characters, the skeptic sees the first 26000`)
    return agent(verifyPrompt(scout, found), { label: `verify:${scout.key}`, phase: 'Verify', schema: VERDICTS })
      .then(v => ({ scout: scout.key, candidates: found.candidates.slice(0, 6), notes: found.notes, verdicts: v ? v.verdicts : [], missed: v ? v.missed : 'VERIFIER_DIED' }))
  },
)

const rows = results.filter(Boolean).flatMap(r => r.candidates.map(c => {
  const v = r.verdicts.find(x => x.id === c.id)
  return { scout: r.scout, ...c, verdict: v ? (v.refuted ? 'REFUTED' : 'SURVIVED') : 'UNVERIFIED', verdict_reason: v && v.reason, corrected_claim: v && v.corrected_claim, hazards: v && v.implementation_hazards, evidence_ok: v && v.evidence_exists_as_quoted }
}))
log(`candidates ${rows.length}: survived ${rows.filter(r => r.verdict === 'SURVIVED').length}, refuted ${rows.filter(r => r.verdict === 'REFUTED').length}, unverified ${rows.filter(r => r.verdict === 'UNVERIFIED').length}`)
return {
  rows,
  notes: results.filter(Boolean).map(r => ({ scout: r.scout, notes: r.notes, missed: r.missed })),
  dropped_scouts: SCOUTS.length - results.filter(Boolean).length,
}
