export const meta = {
  name: 'm3-string-utils-scout',
  description: 'Scout and adversarially verify: which StringUtils-family operations (Commons Lang, Guava, Spring, Synexia utils) M3String can answer from measures or composite kernels, and how library code reaches those kernels without new public API',
  phases: [
    { title: 'Scout', detail: 'two read-only readers: operation catalogue against M3 measures, and the bridge from libraries to java.base kernels' },
    { title: 'Verify', detail: 'one skeptic per scout tries to refute every candidate against the source' },
  ],
}

const ROOT = 'I:/m3-scratch/string-converge-20261116'
const COMMON = `
You are working on the string lane of M3JDK (a JDK 21 fork where java.lang.String can be backed by an M3String: a composite/rope of atoms and tuples with precomputed monoid measures in M3StringFacts). The owner's standing orders: "M3 string is extremely crucial", "also enhance the pre compute", "keep enhancing with leet code and other algorithms", "all of the string operations", and now: "consider all the enhancements that string utils normally have".

READ-ONLY TASK. Do not create, edit or delete any file. Do not run git status, builds, javac or tests. Do not fetch from the network. Use Glob, Grep and Read (read large files in line ranges). Your final answer is the structured output only.

Sources:
- CURRENT String.java and M3StringFacts.java (M3jdk21 master): ${ROOT}/leaf1/head/src/java.base/share/classes/java/lang/
- The other java.lang string owners (M3String.java, M3StringPrecomputeBridge.java, M3StringSearchPrecompute.java, M3StringPositionPrecompute.java, M3StringCodePointPrecompute.java, AbstractStringBuilder.java, ...): ${ROOT}/master-java-lang/  (IGNORE String.java and M3StringFacts.java in this folder: one commit older)
- jdk.internal.mindex, jdk.internal.m3.*, java.util.regex at master: ${ROOT}/owner/src/src/java.base/share/classes/
- A full checkout of M3jdk21 from 2026-11-14 (two days older than master; say so when you cite it): K:/m3-work/wt-fix3/ . It vendors libraries under m3/vendor (for example m3/vendor/third_party and m3/vendor/third_party-m3/relocated with packages jdk.m3.lib.*): find the string utility classes with Glob, for example **/StringUtils.java, **/CharSequenceUtils.java, **/CharUtils.java, **/WordUtils.java, **/Strings.java, **/CharMatcher.java, **/Splitter.java, **/Joiner.java, **/Ascii.java under K:/m3-work/wt-fix3/m3 and K:/m3-work/wt-fix3/src. Also jdk/internal/access/JavaLangAccess.java and SharedSecrets under K:/m3-work/wt-fix3/src/java.base/share/classes.
- Synexia's own string utilities (a working checkout; names only matter as a catalogue of operations): E:/ws2/com.synexia/synexia-common (Grep for class names containing String, Csv, Text under src/main/java).
- Invariants and state of the lane (read sections 2 and 3 first): ${ROOT}/record/docs/m3/string-lane/HANDOVER.md

Measures an M3-backed String already carries (M3StringFacts): utf16Length, utf8Length, modifiedUtf8Length, codePointCount, unpairedSurrogateCount, javaHash and 31^length, first and last unit, a 64-bit unit presence signal, ascii and latin1 flags, class masks over all units and over any unit for 14 classes (DIGIT, WORD, SPACE, LOWER, UPPER, ALPHA, ALNUM, HEX, ASCII, LOWER_HEX, UPPER_HEX, DOT, HORIZONTAL, VERTICAL: these are the exact zero-flag java.util.regex classes, ASCII-defined), ASCII upper, lower and title hashes, first and last four units, bigram and trigram signals, trim and strip edges, and the line terminator count. facts() prepares them with one scan and caches; factsIfPrepared() never prepares.

Hard rules for any proposal:
- I7 (owner's rule, 2026-11-08): no new public or protected class or member in M3JDK without the owner's admission. So separate two kinds of candidates and label each title with one of these prefixes: "INTERNAL:" a faster path behind an operation that already exists (in java.lang.String, or in a vendored utility class that M3JDK already ships) using an existing kernel or measure; "ADMISSION:" a new public operation that would need the owner's decision (list these, do not design them in detail).
- The owner's rules also say: libraries keep their upstream packages and API; the JDK owns the fast implementation once, in java.base, and libraries call it; no duplicate fast paths.
- Results of existing operations must stay identical, including Unicode definitions: Character.isWhitespace vs the ASCII SPACE class, Character.isDigit vs [0-9], Character.isLetter vs [A-Za-z] differ outside ASCII, so a class mask may only answer when the ascii fact is true or the definitions provably agree.
- I2 / I3: no whole-value flatten of an M3-backed String where a measure or composite kernel answers.

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
    key: 'utils-catalogue',
    prompt: `${COMMON}

ANGLE: the catalogue. What string utility libraries offer, and what M3String can already answer.
Build the operation catalogue from the sources you can read (Apache Commons Lang StringUtils and friends, Guava Strings / CharMatcher / Splitter / Joiner / Ascii, any Spring or other utility class vendored in the checkout, and Synexia's own utilities), grouped by family: emptiness and blank tests; character-class tests (isNumeric, isAlpha, isAlphanumeric, isWhitespace, isAsciiPrintable, isAllLowerCase, isAllUpperCase, isMixedCase); contains / containsAny / containsOnly / containsNone / containsIgnoreCase; counting (countMatches); index searches (indexOfAny, indexOfAnyBut, ordinalIndexOf, indexOfDifference, lastOrdinalIndexOf); prefix and suffix (startsWithAny, endsWithAny, removeStart, removeEnd, getCommonPrefix, appendIfMissing, prependIfMissing); substring families (substringBefore / After / Between, left / right / mid, truncate, abbreviate); trimming and stripping (strip, stripStart, stripEnd, stripAccents, chomp, chop, deleteWhitespace, normalizeSpace); padding, centering and repeat; case (capitalize, uncapitalize, swapCase, upperCase, lowerCase, equalsIgnoreCase, compareIgnoreCase); splitting and joining (split, splitByWholeSeparator, splitPreserveAllTokens, splitByCharacterTypeCamelCase, join); replacing (replace, replaceEach, replaceChars, replaceOnce, overlay, remove); reversing and rotating; distances and similarity (Levenshtein, Jaro-Winkler, fuzzy score, Hamming, longest common subsequence); wrapping and unwrapping; default-value helpers.
For each family give in "notes" one compact line per operation: "operation | library | how it is implemented there (unit loop over CharSequence.charAt, toCharArray, regex, delegates to String method X) | what an M3-backed String could answer it from (facts field, existing composite kernel with file:line, or nothing yet) | exactness condition (for example only when facts.ascii is true)".
Then the candidates: the six operations or families with the largest gain, each labelled INTERNAL: or ADMISSION: as defined above. Prefer INTERNAL candidates where a shipped utility method calls a java.lang.String method that has no M3 path, or loops charAt / calls toCharArray on a String argument (which flattens or dispatches per unit on a composite).`,
  },
  {
    key: 'utils-bridge',
    prompt: `${COMMON}

ANGLE: the bridge. How utility code outside java.lang reaches the M3 kernels today, and the smallest correct way to let shipped utility classes use measures and composite kernels without new public API.
Establish from the code: (1) what JavaLangAccess / SharedSecrets already exposes about M3 Strings (Grep for m3, M3, Facts, Trigram in JavaLangAccess.java and its implementation in System.java) and who calls it; (2) which packages of java.base are exported, and to which modules, for M3 internals (module-info.java of java.base: qualified exports of jdk.internal.mindex, jdk.internal.m3.*, jdk.internal.access); (3) which vendored or relocated library classes in the checkout are part of the M3JDK build as modules (so a qualified export could reach them) versus plain Maven artifacts on the class path (which can only use public API); (4) which public String methods already give a utility the benefit for free when the utility simply calls them (isBlank, strip, indexOf, contains, startsWith, regionMatches, chars, codePoints, equalsIgnoreCase, repeat, lines, split).
Candidates, smallest first, each labelled INTERNAL: or ADMISSION: as defined above. Examples to check, not to assume: character-class predicates answered from the class masks through one JavaLangAccess query for in-JDK callers; utility methods that call toCharArray or loop charAt on a String being routed to an existing String method with an M3 path; CharSequence-taking utilities that test "instanceof String" and could delegate; an existing String method whose M3 path is missing and which utilities call heavily (equalsIgnoreCase, regionMatches with ignoreCase, toCharArray, chars, getChars, compareToIgnoreCase, indexOf(String) family).
State plainly what you could not establish, and for every ADMISSION candidate what exactly the owner would have to decide.`,
  },
]

const verifyPrompt = (scout, result) => `${COMMON}

YOU ARE THE SKEPTIC for the scout "${scout.key}". Another agent produced the candidates below. Your job is to REFUTE them against the source. Default to refuted=true when you cannot confirm a claim from the code.
For every candidate: (1) open the cited file and lines: does the quoted evidence exist there? (2) is the claimed cost or missing path real: check whether the String method the utility calls already has an M3 path, and whether the cited utility class is really shipped in M3JDK; (3) exactness: would the measure-based answer equal the library's current answer for every input, including non-ASCII letters, digits and white space, surrogates, empty and null arguments; (4) is the INTERNAL / ADMISSION label right: does the change add any public or protected member, or a new package; (5) is the bridge legal: module exports, class path vs module path; (6) is the gain plausible?
Give for each candidate: refuted true/false, whether the evidence exists as quoted, the reason with file and line, a corrected claim when the idea survives in a narrower form, and the hazards an implementer must handle. In "missed", name better candidates of the same angle that the scout overlooked, and report wrong rows you find when you spot-check at least ten rows of the catalogue in the notes.

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
