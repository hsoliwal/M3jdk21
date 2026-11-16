export const meta = {
  name: 'm3-string-enhance-scout',
  description: 'Scout and adversarially verify the next M3String leaves: flatten sites, LeetCode-style algorithm sites, measure readers, compile-time image consumer',
  phases: [
    { title: 'Scout', detail: 'four read-only readers over the string owners, one angle each' },
    { title: 'Verify', detail: 'one skeptic per scout tries to refute every candidate against the source' },
  ],
}

const ROOT = 'I:/m3-scratch/string-converge-20261116'
const COMMON = `
You are working on the string lane of M3JDK (a JDK 21 fork where java.lang.String can be backed by an M3String: a composite/rope of atoms and tuples with precomputed monoid measures in M3StringFacts). The owner's standing orders: "M3 string is extremely crucial", "also enhance the pre compute", "keep enhancing with leet code and other algorithms".

READ-ONLY TASK. Do not create, edit or delete any file. Do not run git status, builds, javac or tests. Do not fetch from the network. Use Grep and Read (read large files in line ranges). Your final answer is the structured output only.

Sources (exact content of M3jdk21 master for these owners):
- CURRENT String.java and M3StringFacts.java: ${ROOT}/leaf1/head/src/java.base/share/classes/java/lang/
- The other 26 java.lang string owners (M3String.java, M3StringPool.java, M3StringTuple.java, M3StringAtom.java, M3StringOwner.java, M3StringSearchPrecompute.java, M3StringPositionPrecompute.java, M3StringCodePointPrecompute.java, M3StringAffixPrecompute.java, M3StringRegexRowPrecompute.java, M3StringPrecomputeBridge.java, M3StringRegexDp.java, M3StringLeafCursor.java, M3StringLeafRanges.java, M3StringBuilderLane.java, AbstractStringBuilder.java, StringLatin1.java, StringUTF16.java, ...): ${ROOT}/master-java-lang/  (IGNORE the String.java and M3StringFacts.java in this folder: they are one commit older)
- jdk.internal.mindex, jdk.internal.m3.*, java.util.regex: ${ROOT}/owner/src/src/java.base/share/classes/
- Invariants and state of the lane (read sections 2 and 3 first): ${ROOT}/record/docs/m3/string-lane/HANDOVER.md

Hard invariants for any proposal:
- I2: never flatten a whole M3-backed value (materialize(), compatibilityValue(), charShadow(), String.value() on an M3 owner) where a composite path can answer.
- I3: mechanical gates (facts, signals, precompute) before any materialization.
- I4: a new measure is a monoid: scan(a+b) == compose(scan(a), scan(b)) at every cut (surrogate pairs, CR LF).
- I7: no new public or protected class or member; package-private in java.lang or jdk.internal only.
- Behaviour of the public String API must stay identical: results, exception types and order, null handling.
- A per-unit charAt on a composite M3String is an owner dispatch (not an array read): loops of charAt over a composite are expensive; bulk getChars windows, leaf cursors and facts are the cheap paths.

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
    key: 'flatten-sites',
    prompt: `${COMMON}

ANGLE: flatten sites reachable with an M3-backed String.
A lexical pass classified every value() call site of String.java: ${ROOT}/STRING_VALUE_SITE_GUARDS.tsv (columns string_method, line, receiver, class, guard). Line numbers refer to the CURRENT String.java. Rows of class OTHER_RECEIVER, NO_GUARD and MIXED_GUARD are unresolved (20 rows: compareTo, regionMatches, startsWith, indexOf(String), lastIndexOf(String), replace, contentEquals, nonSyncContentEquals, lines, indexOfNonWhitespace, lastIndexOfNonWhitespace, getBytes*NoRepl).
String.value() is: M3String storage = m3(); return storage == null ? value : storage.materialize();  so any value() call that executes while the receiver is M3-backed flattens the WHOLE value.
For each unresolved site, read the method and decide: can the call execute with an M3-backed receiver (this or the other String)? Typical case: a guard handles only "both sides M3" and a mixed pair (one M3, one flat) falls through to value() on the M3 side.
Each candidate = one method (group its sites) where an M3-backed String is still flattened, with the cheapest composite path that avoids it (an existing M3String / M3StringPrecomputeBridge method, a facts gate, a bounded getChars window, or a leaf cursor). Rank by how common the call is in real programs. Also list in notes the sites you proved NOT reachable with an M3 owner and why.`,
  },
  {
    key: 'leetcode-algorithms',
    prompt: `${COMMON}

ANGLE: LeetCode-style algorithm upgrades inside the string owners.
Find code in the java.lang M3String* owners, AbstractStringBuilder's M3 paths, the M3 paths of String.java, and jdk.internal.mindex that does more work than a known algorithm needs. Look for: nested loops giving O(n*m); per-unit charAt loops over a composite; repeated linear walks over leaves where prefix sums + binary search would do; rescans after a mismatch (no failure function); substring equality done unit by unit where the composable hash (javaHash with hash31Power, which is a rolling polynomial hash monoid) gives an O(1) reject; repeated concatenation in a loop; manual linear search in sorted arrays; array shifts in loops; recomputation of something a facts field already holds.
Algorithm menu to match against (name the one that fits): two-way / KMP / Z-function for needles across chunk seams; Rabin-Karp with the existing hash monoid; Boyer-Moore-Horspool and Sunday for long needles; Shift-Or / Bitap for needles up to 64 units; SWAR word-at-a-time scans; binary search over cumulative leaf offsets; longest common prefix by hash + binary search (compareTo, regionMatches, startsWith on composites); two pointers; prefix sums; monotonic stack or deque; Aho-Corasick for multi-pattern replace; prefix function for period detection (repeat); Fenwick tree for edit offsets; Manacher only if a palindrome scan exists.
First check what already exists (the notes say KMP/BMH, a trigram lane, a reverse lane and a flat-needle gate exist in M3StringSearchPrecompute and related owners): do not propose what is already there; propose where an existing kernel is not used.
Each candidate: the exact method and lines, the current complexity, the algorithm, the new complexity, and a differential proof plan against the stock JDK String result.`,
  },
  {
    key: 'measure-readers',
    prompt: `${COMMON}

ANGLE: precompute measures that exist but are not read, and scans a measure could answer.
M3StringFacts (CURRENT file) holds: utf16Length, utf8Length, modifiedUtf8Length, codePointCount, unpairedSurrogateCount, javaHash, hash31Power, firstUtf16Unit, lastUtf16Unit, bitSignal64 (unit presence signal), ascii, latin1, regexAllClasses / regexAnyClasses (14 class masks: DIGIT, WORD, SPACE, LOWER, UPPER, ALPHA, ALNUM, HEX, ASCII, LOWER_HEX, UPPER_HEX, DOT, HORIZONTAL, VERTICAL), asciiUpperHash, asciiLowerHash, asciiTitleHash, prefix4, suffix4, bigramSignal64, trigramSignal64, trimStart, trimEnd, stripStart, stripEnd, and the new lineTerminatorCount (LF, CR, CR LF as one; so far read only by String.lines()).
Accessors: M3String.facts() prepares facts (a full scan with bulk getChars windows, then cached); M3String.factsIfPrepared() never prepares.
Find String / M3String / AbstractStringBuilder methods on the M3 path that still scan units for something a facts field answers exactly or can reject early. Examples to check, not to assume: isBlank, isEmpty-like checks, strip/trim family, indent and stripIndent (M3String.indent counts lines in a pre-pass), lines, split on a single char or on a line terminator, chars().count, codePointCount, equalsIgnoreCase (ASCII fold hashes as a reject), toUpperCase / toLowerCase when regexAnyClasses proves there is no lower or upper unit, matches for simple class patterns, contains(char) via bitSignal64, hashCode, getBytes length sizing via utf8Length, repeat, endsWith / startsWith via prefix4 / suffix4.
For each candidate say whether it needs facts() (forces a scan: only acceptable if the method scans anyway and the result is cached for later) or factsIfPrepared() (free). Also propose at most two NEW monoid measures only if a concrete reader exists; give the per-unit fold and the seam rule.`,
  },
  {
    key: 'compile-time-consumer',
    prompt: `${COMMON}

ANGLE: connect the compile-time harvest (M3AST / javac side) to the runtime string and regex owners. Today nothing in java.lang or java.util.regex reads a compile-time image.
Extra sources (a 2026-11-14 checkout, two days older than master; say so in your notes):
- compile-time port, 35 files: K:/m3-work/wt-fix3/m3/ports/compiletime/src/main/java  (M3CompileTimeStringInventory, M3ConstantRegexHarvester, M3ConstantRegexImage, M3ConstantRegexImageWriter, M3ConstantRegexGate, M3ConstantRegexDifferential, M3RegexRequirement, M3PatternNodes, M3GvasLiteralHarvester, its own ast package)
- javac owners: K:/m3-work/wt-fix3/src/jdk.compiler/share/classes/com/sun/tools/javac/m3  (M3AST, M3ASTPC, M3CompilerPatternHints, M3PrimitiveImageCompiler)
- runtime side: java/util/regex/Pattern.java and Matcher.java under ${ROOT}/owner/src/src/java.base/share/classes/ , plus M3PatternQuery / M3TQ wherever Grep finds them, and M3StringRegexRowPrecompute / M3StringRegexDp in ${ROOT}/master-java-lang/
Establish from the code: (1) what exactly the harvest writes (image format, magic, fields, which facts per constant regex or string constant), where it is written, and who is supposed to load it; (2) what Pattern.compile computes at run time for a constant pattern that the image already holds (literal prefix, required literal, trigram query, class masks, anchors); (3) the smallest runtime consumer that is correct: where the lookup goes in Pattern.compile, what the key is (pattern text + flags), how a stale or missing image falls back, and how it is proven (differential of compiled Pattern behaviour with and without the image).
Candidates = ordered leaves to make that connection, smallest first. State plainly which parts you could not establish from the sources.`,
  },
]

const verifyPrompt = (scout, result) => `${COMMON}

YOU ARE THE SKEPTIC for the scout "${scout.key}". Another agent produced the candidates below. Your job is to REFUTE them against the source. Default to refuted=true when you cannot confirm a claim from the code.
For every candidate: (1) open the cited file and lines: does the quoted evidence exist there? (2) is the claimed cost or reachability real: trace the guards in front of it, and check whether an existing owner method already handles the case; (3) would the proposed change keep the public String behaviour identical: results, exception type and order, null handling, surrogate and CR LF seams, empty values, concurrency of facts caching; (4) does it add any public or protected member (forbidden); (5) is the stated gain plausible, or does the change add a scan or allocation on the common path?
Give for each candidate: refuted true/false, whether the evidence exists as quoted, the reason with file and line, a corrected claim when the idea survives in a narrower form, and the hazards an implementer must handle. In "missed", name better candidates of the same angle that the scout overlooked, with file and lines.

SCOUT ANGLE:
${scout.prompt.split('ANGLE:')[1]}

CANDIDATES (JSON):
${JSON.stringify(result.candidates.slice(0, 6), null, 1)}

SCOUT NOTES:
${result.notes || '(none)'}`

const results = await pipeline(
  SCOUTS,
  scout => agent(scout.prompt, { label: `scout:${scout.key}`, phase: 'Scout', schema: CANDIDATES }),
  (found, scout) => {
    if (!found || !found.candidates || !found.candidates.length) return { scout: scout.key, candidates: [], verdicts: [], notes: found && found.notes, missed: '' }
    if (found.candidates.length > 6) log(`${scout.key}: ${found.candidates.length} candidates, verifying the first 6 only`)
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
