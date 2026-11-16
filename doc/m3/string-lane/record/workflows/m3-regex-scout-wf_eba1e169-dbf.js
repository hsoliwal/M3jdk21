export const meta = {
  name: 'm3-regex-scout',
  description: 'Scout and adversarially verify regex optimizations in M3JDK: gate order and materialization per regex entry point, algorithm upgrades in the regex owners, and how Matcher reads an M3-backed input',
  phases: [
    { title: 'Scout', detail: 'three read-only readers over java.util.regex and the M3 regex owners' },
    { title: 'Verify', detail: 'one skeptic per scout tries to refute every candidate against the source' },
  ],
}

const ROOT = 'I:/m3-scratch/string-converge-20261116'
const COMMON = `
You are working on the string and regex lane of M3JDK (a JDK 21 fork where java.lang.String can be backed by an M3String: a composite/rope of atoms and tuples with precomputed monoid measures in M3StringFacts). The owner's standing orders: "M3 string is extremely crucial", "also enhance the pre compute", "keep enhancing with leet code and other algorithms", "we must optimize regex operations", and from 2026-10-17: "regex must achieve the maximum mechanical match before using char arrays from JNI" and "regex has to get to the level of precompute".

READ-ONLY TASK. Do not create, edit or delete any file. Do not run git status, builds, javac or tests. Do not fetch from the network. Use Grep and Read (read large files in line ranges). Your final answer is the structured output only.

Sources (exact content of M3jdk21 master e3c8bb1d0e3 unless noted):
- java.util.regex (Pattern.java, Matcher.java and the M3 helpers beside them), jdk.internal.mindex (M3PatternQuery, M3TQ, M3RX7, M3RegexDpPlan, M3AutomatonOps, M3Automaton, M3RegExpCompiler, M3RunTable, M3ChunkCursor, M3TokenIndex, M3TokenPattern, M3TokenPredicateCache, M3CI, M3Boundary, ...), jdk.internal.m3.*: ${ROOT}/owner/src/src/java.base/share/classes/
- CURRENT String.java and M3StringFacts.java: ${ROOT}/leaf1/head/src/java.base/share/classes/java/lang/
- The other java.lang string owners (M3String.java, M3StringRegexDp.java, M3StringRegexRowPrecompute.java, M3StringTokenRegex.java, M3StringSearchPrecompute.java, M3StringPrecomputeBridge.java, M3StringLeafCursor.java, ...): ${ROOT}/master-java-lang/  (IGNORE the String.java and M3StringFacts.java in this folder: one commit older)
- A full checkout of M3jdk21 from 2026-11-14 (two days older; use it for tests and for anything not in the folders above, and say so when you cite it): K:/m3-work/wt-fix3/  (test/jdk/java/util/regex, test/jdk/java/lang/String/M3*Regex*, m3/ports/regex-cursor, m3/ports/compiletime)
- Invariants and state of the lane (read sections 2, 3 and 7 first): ${ROOT}/record/docs/m3/string-lane/HANDOVER.md
- A mechanical pass listed loop shapes in these owners (nested loops, manual binary searches, array shifts; columns sourcePath, line, shape, enclosingMethod, categories, snippet): ${ROOT}/owner/leet/com.synexia.rewrite.mop.leetcodify.LeetCodifySites.tsv  (M3AutomatonOps 26 sites, M3RX7 17, M3PatternQuery 12, M3CI 12, M3TQ 6)

The intended ladder for a regex operation on an M3-backed input, cheapest first: literal, required literal, trigram facts (M3TQ), bit-parallel transfer, NFA rows, DFA (run table), oracle corpus; the JNI char or byte shadow of the whole input is the last resort. A transfer element is keyed by (pattern, atom) so a match over a tuple composes per-atom results instead of scanning text.

Hard invariants for any proposal:
- Results must equal stock java.util.regex exactly: match bounds, group captures, hitEnd / requireEnd, exceptions (PatternSyntaxException text and index), flags (CASE_INSENSITIVE, UNICODE_CASE, MULTILINE, DOTALL, COMMENTS, UNICODE_CHARACTER_CLASS, CANON_EQ, LITERAL), region, anchoring and transparent bounds, and behaviour for a CharSequence that is not a String.
- I2 / I3: do not flatten a whole M3-backed input where a gate or a cursor can answer; mechanical gates first.
- I7: no new public or protected class or member.
- A per-unit charAt on a composite M3String is an owner dispatch, not an array read.

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
    key: 'regex-gates',
    prompt: `${COMMON}

ANGLE: gate order and first materialization point per regex entry point.
Entry points: Pattern.compile(String) and (String, int); Pattern.matcher(CharSequence); Pattern.matches; Pattern.asPredicate / asMatchPredicate; Pattern.split and splitAsStream; Matcher.matches, find, find(int), lookingAt, replaceAll, replaceFirst, results, reset(CharSequence); String.matches, replaceAll, replaceFirst, split(String) and split(String, int), contains-like regex helpers.
For each, trace what happens when the input is an M3-backed String: which M3 gates run and in what order (M3PatternQuery, M3TQ trigram facts, literal and required-literal gates, M3StringRegexDp, M3RegexDpPlan, run tables, row precompute), and where the whole input is first materialized or read unit by unit (Matcher's text field and charAt, getSubSequence, toString, value(), a JNI shadow). Put the table in "notes" as lines "entry | gates in order | first materialization (file:line) | condition that skips the gates".
Known from notes of 2026-10-18 (re-check each on this source, do not assume): CASE_INSENSITIVE disabled the trigram query (m3Tq = null); the compile-time harvest was unwired; a general DFA step was missing; String.split and replace fast paths for single-char or literal regexes may not have an M3 path.
Candidates = the gate gaps to close, ordered by how common the pattern shape and entry point are in real programs (literal and simple class patterns, split on one char or a short literal, replaceAll with a literal, anchored prefixes, case-insensitive ASCII), each with the smallest change that adds the missing gate and a differential proof plan against stock java.util.regex on a pattern corpus.`,
  },
  {
    key: 'regex-algorithms',
    prompt: `${COMMON}

ANGLE: algorithm upgrades inside the regex owners.
Read the M3 regex owners in jdk.internal.mindex and java.lang (M3AutomatonOps, M3Automaton, M3RegExpCompiler, M3RunTable, M3RX7, M3RegexDpPlan, M3PatternQuery, M3TQ, M3CI, M3TokenPattern, M3TokenPredicateCache, M3StringRegexDp, M3StringRegexRowPrecompute, M3StringTokenRegex) and the M3 additions inside Pattern.java and Matcher.java. Use the mechanical site list as a map, then judge each hot spot with the code open.
Look for: subset construction and epsilon closure done with per-state rescans where bitset closures and a worklist are linear; determinization without state-set hashing; minimization that is quadratic where Hopcroft's partition refinement is O(n log n); transition lookup by linear scan over sorted ranges where binary search or a 256-entry ASCII table fits; repeated sorting or repeated array growth inside loops; per-unit state stepping where a byte-class-compressed transition table or a precomputed transfer function per atom applies; small NFAs (up to 64 states) not using bit-parallel Shift-And / Glushkov stepping; literal patterns not using the existing KMP / two-way / BMH kernels of M3StringSearchPrecompute; alternations of literals not using a trie or Aho-Corasick; a required-literal prefilter computed but not used as a skip loop; backtracking paths with no memoization for patterns the DFA cannot take; composition of transfer functions recomputed where it could be cached per (pattern, atom id).
First establish what is already implemented, so nothing is proposed twice. Each candidate: the method and lines, the current complexity, the algorithm, the new complexity, the behaviour risks, and a differential proof plan (the frozen 10,000-pattern x 64-input corpus style: every pattern stock regex accepts, compare match results with stock).`,
  },
  {
    key: 'matcher-input',
    prompt: `${COMMON}

ANGLE: how Matcher and Pattern read the input when it is an M3-backed String or a StringBuilder, and the cheapest correct way to stop paying per unit or flattening.
Establish from Matcher.java and Pattern.java: the type and use of the text field; every place a node reads input (seq.charAt, Character.codePointAt(seq, i), seq.subSequence, toString); whether an M3-backed String input gets a window, a cursor (see M3ChunkCursor in jdk.internal.mindex and M3String.CodeUnitCursor / M3StringLeafCursor in java.lang, and the regex-cursor port in K:/m3-work/wt-fix3/m3/ports/regex-cursor) or is read through String.charAt, which for an M3 owner is a dispatch into the composite per unit; where group(), replaceAll and appendReplacement build results (substring on the composite, or a flat copy); what Pattern's literal nodes (Slice, BnM) do against a composite.
Quantify per operation the reads per input unit and the allocations for an M3-backed input of n units with k leaves.
Candidates, smallest and safest first: for example a request-local sequential cursor behind the CharSequence the engine reads, so forward scans cost O(1) amortized per unit; bulk windows for literal nodes; routing whole-literal patterns to the existing composite search kernels; results built as composite slices. Each must keep the stock engine semantics for every flag, region and bound setting, and must not change behaviour for non-String CharSequences. Give a differential proof plan (stock engine on the flat String vs the candidate on a composite of the same content, random cut points, all flags).`,
  },
]

const verifyPrompt = (scout, result) => `${COMMON}

YOU ARE THE SKEPTIC for the scout "${scout.key}". Another agent produced the candidates below. Your job is to REFUTE them against the source. Default to refuted=true when you cannot confirm a claim from the code.
For every candidate: (1) open the cited file and lines: does the quoted evidence exist there? (2) is the claimed gap, cost or missing gate real: trace the guards in front of it, and check whether an existing gate or kernel already handles the case; (3) would the proposed change keep java.util.regex results identical: match bounds, groups, hitEnd and requireEnd, exceptions, every flag, regions and bounds, non-String CharSequences; (4) does it add any public or protected member (forbidden); (5) is the stated gain plausible, or does the change add a scan or allocation on the common path?
Give for each candidate: refuted true/false, whether the evidence exists as quoted, the reason with file and line, a corrected claim when the idea survives in a narrower form, and the hazards an implementer must handle. In "missed", name better candidates of the same angle that the scout overlooked, with file and lines.

SCOUT ANGLE:
${scout.prompt.split('ANGLE:')[1]}

CANDIDATES (JSON):
${JSON.stringify(result.candidates.slice(0, 6), null, 1)}

SCOUT NOTES (spot-check at least eight rows of any table in here against the source and report wrong rows in "missed"):
${(result.notes || '(none)').slice(0, 24000)}`

const results = await pipeline(
  SCOUTS,
  scout => agent(scout.prompt, { label: `scout:${scout.key}`, phase: 'Scout', schema: CANDIDATES }),
  (found, scout) => {
    if (!found || !found.candidates || !found.candidates.length) return { scout: scout.key, candidates: [], verdicts: [], notes: found && found.notes, missed: '' }
    if (found.candidates.length > 6) log(`${scout.key}: ${found.candidates.length} candidates, verifying the first 6 only`)
    if ((found.notes || '').length > 24000) log(`${scout.key}: notes are ${found.notes.length} characters, the skeptic sees the first 24000`)
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
