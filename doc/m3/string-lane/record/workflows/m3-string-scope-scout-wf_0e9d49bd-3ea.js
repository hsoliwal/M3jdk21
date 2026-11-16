export const meta = {
  name: 'm3-string-scope-scout',
  description: 'Scout and adversarially verify: StringBuilder/StringBuffer composite paths, a per-operation matrix of all String operations on M3-backed values, and M3JDK javac string optimizations',
  phases: [
    { title: 'Scout', detail: 'three read-only readers: builders, operation matrix, compiler optimizations' },
    { title: 'Verify', detail: 'one skeptic per scout tries to refute every candidate against the source' },
  ],
}

const ROOT = 'I:/m3-scratch/string-converge-20261116'
const COMMON = `
You are working on the string lane of M3JDK (a JDK 21 fork where java.lang.String can be backed by an M3String: a composite/rope of atoms and tuples with precomputed monoid measures in M3StringFacts). The owner's standing orders: "M3 string is extremely crucial", "also enhance the pre compute", "keep enhancing with leet code and other algorithms", and "StringBuilder, String, all of the string operations and even M3JDK compiler optimizations".

READ-ONLY TASK. Do not create, edit or delete any file. Do not run git status, builds, javac or tests. Do not fetch from the network. Use Grep and Read (read large files in line ranges). Your final answer is the structured output only.

Sources (exact content of M3jdk21 master for these owners):
- CURRENT String.java and M3StringFacts.java: ${ROOT}/leaf1/head/src/java.base/share/classes/java/lang/
- The other 26 java.lang string owners (M3String.java, M3StringPool.java, M3StringTuple.java, M3StringAtom.java, M3StringOwner.java, M3StringSearchPrecompute.java, M3StringPositionPrecompute.java, M3StringCodePointPrecompute.java, M3StringAffixPrecompute.java, M3StringRegexRowPrecompute.java, M3StringPrecomputeBridge.java, M3StringRegexDp.java, M3StringLeafCursor.java, M3StringLeafRanges.java, M3StringBuilderLane.java, AbstractStringBuilder.java, StringBuilder.java, StringBuffer.java, StringConcatHelper.java, StringLatin1.java, StringUTF16.java, ...): ${ROOT}/master-java-lang/  (IGNORE the String.java and M3StringFacts.java in this folder: they are one commit older)
- jdk.internal.mindex, jdk.internal.m3.*, java.util.regex: ${ROOT}/owner/src/src/java.base/share/classes/
- A full checkout of M3jdk21 from 2026-11-14 (two days older than master; fine for everything outside the files above, but say so when you cite it): K:/m3-work/wt-fix3/  (src/java.base/share/classes, src/jdk.compiler/share/classes, m3/ports/compiletime, test/jdk/java/lang/String)
- Invariants and state of the lane (read sections 2 and 3 first): ${ROOT}/record/docs/m3/string-lane/HANDOVER.md
- A mechanical pass already listed loop shapes in these owners (nested loops, manual binary searches, array shifts; columns sourcePath, line, shape, enclosingMethod, categories, snippet): ${ROOT}/owner/leet/com.synexia.rewrite.mop.leetcodify.LeetCodifySites.tsv

Hard invariants for any proposal:
- I2: never flatten a whole M3-backed value (materialize(), compatibilityValue(), charShadow(), String.value() on an M3 owner) where a composite path can answer.
- I3: mechanical gates (facts, signals, precompute) before any materialization.
- I4: a new measure is a monoid: scan(a+b) == compose(scan(a), scan(b)) at every cut (surrogate pairs, CR LF).
- I7: no new public or protected class or member; package-private in java.lang or jdk.internal only.
- I8: StringBuilder and StringBuffer are composite too: appends accumulate atoms and tuples; toString must not flatten.
- Behaviour of the public API must stay identical: results, exception types and order, null handling, and for StringBuffer its synchronization contract.
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
    key: 'builders',
    prompt: `${COMMON}

ANGLE: StringBuilder, StringBuffer, AbstractStringBuilder, M3StringBuilderLane and StringConcatHelper.
Establish from the code how the builders use M3 today: what M3StringBuilderLane holds (chunks, atoms, a rope builder?), which AbstractStringBuilder operations go through the lane and which fall back to the flat byte[] value, and what toString() returns (a composite without flattening, or a one-atom re-admission of a flat copy).
Then find: (1) operations that flatten the builder or an M3-backed argument: append(String) of an M3-backed String, append(CharSequence, start, end), insert, delete, deleteCharAt, replace, reverse, setLength, setCharAt, indexOf/lastIndexOf, compareTo, chars, getChars, substring, toString; (2) loops that do more work than needed: per-unit copies where a bulk range copy exists, linear chunk walks where the lane already keeps cumulative offsets (it has a binary search in charAt), repeated shifting of the tail on insert/delete (a gap buffer or a piece table edit log would make it O(log n)), re-search from scratch after each replacement; (3) places where an append of an M3-backed String could share the argument's atoms instead of copying units.
Each candidate names the method, the lines, the current cost, the replacement (algorithm or existing kernel), and a differential proof plan against the stock java.lang.StringBuilder for random edit scripts.`,
  },
  {
    key: 'operation-matrix',
    prompt: `${COMMON}

ANGLE: every public String operation, one row each: what happens when the receiver is M3-backed.
Work through the public instance and static methods of the CURRENT String.java in source order (about 130 of them; overloads count separately only when their M3 path differs). For each, find the M3 branch (a local such as "M3String storage = m3();" or "m3() != null") and classify the path taken for an M3-backed receiver:
  COMPOSITE   answered on the composite by an M3String / M3StringPrecomputeBridge / precompute owner method without flattening
  FACTS       answered or rejected from M3StringFacts alone
  UNIT_LOOP   a loop of charAt / codePointAt over the composite (per-unit owner dispatch)
  FLATTEN     reaches value(), materialize(), compatibilityValue() or charShadow() for the M3 owner
  NO_M3_PATH  the method has no M3 branch at all (then say what it calls and whether that callee handles M3)
Follow one level into M3String when the String method only delegates, to see whether the M3String method itself loops per unit or flattens (for example M3String.indexOf near line 1516 and lastIndexOf near line 1541 are nested loops according to the mechanical pass: confirm what they do and when they are reached).
Put the full matrix in "notes" as compact lines "method(signature) | class | callee | line". The candidates are the six worst rows by expected frequency in real programs times cost, each with the replacement and a differential proof plan against stock String.`,
  },
  {
    key: 'compiler-optimizations',
    prompt: `${COMMON}

ANGLE: M3JDK javac and runtime-linkage optimizations for strings.
Sources: K:/m3-work/wt-fix3/src/jdk.compiler/share/classes/com/sun/tools/javac/ (jvm/StringConcat.java, jvm/Gen.java, comp/Lower.java, comp/ConstFold.java, comp/Attr.java, and the M3 owners in m3/: M3AST, M3ASTPC, M3CompilerPatternHints, M3PrimitiveImageCompiler), K:/m3-work/wt-fix3/src/java.base/share/classes/java/lang/invoke/StringConcatFactory.java, ${ROOT}/master-java-lang/StringConcatHelper.java, the m3Join* entry points in the CURRENT String.java, K:/m3-work/wt-fix3/m3/ports/compiletime/src/main/java, and java/lang/runtime (SwitchBootstraps, ObjectMethods) in the 2026-11-14 checkout.
Establish from the code: (1) how string concatenation is compiled and linked in M3JDK today: does javac's indy concat (StringConcatFactory) or the inline StringBuilder strategy produce an M3 composite (tuple of the operand Strings, no unit copy) or a flat copy; where is that decided (look for m3Join, M3String.concat, joinCharRanges, m3JoinedStringsEnabled); (2) what M3-specific compiler work exists (M3CompilerPatternHints, M3PrimitiveImageCompiler, the compile-time harvest) and what consumes it; (3) how switch on String is lowered (hashCode + equals chain in Lower) and whether a precomputed perfect hash or the M3 intern identity is used.
Then list compiler-side optimizations that are missing and concrete, smallest first, for example: constant folding of pure String methods on compile-time constants (length, isEmpty, charAt, indexOf, substring, trim/strip, toUpperCase/toLowerCase for ASCII, repeat, concat, equals, hashCode); a concat recipe that hands M3 the operand Strings so the result is a tuple; StringBuilder chains with a known final length lowered to one sized concat; String.format / formatted with a constant format string lowered to concat; switch on String through a compile-time perfect hash; constant regex patterns compiled once (static holder) or their image consumed; string constants carrying precomputed facts into the class file or the M3 image.
For each candidate: the javac phase and file where it belongs, what is already there, the semantic risks (side effects and evaluation order, null operands, locale sensitivity, class file compatibility, javac -source/-target behaviour for user code, and the rule that the public language semantics may not change), and the proof plan (compile small programs with and without, compare outputs and javap).`,
  },
]

const verifyPrompt = (scout, result) => `${COMMON}

YOU ARE THE SKEPTIC for the scout "${scout.key}". Another agent produced the candidates below. Your job is to REFUTE them against the source. Default to refuted=true when you cannot confirm a claim from the code.
For every candidate: (1) open the cited file and lines: does the quoted evidence exist there? (2) is the claimed cost, path class or missing optimization real: trace the guards in front of it, and check whether an existing owner method or javac phase already handles the case; (3) would the proposed change keep the public behaviour identical: results, exception type and order, null handling, evaluation order and side effects, surrogate and CR LF seams, empty values, StringBuffer synchronization, concurrency of facts caching, Java language semantics for compiler changes; (4) does it add any public or protected member (forbidden); (5) is the stated gain plausible, or does the change add a scan or allocation on the common path?
Give for each candidate: refuted true/false, whether the evidence exists as quoted, the reason with file and line, a corrected claim when the idea survives in a narrower form, and the hazards an implementer must handle. In "missed", name better candidates of the same angle that the scout overlooked, with file and lines.

SCOUT ANGLE:
${scout.prompt.split('ANGLE:')[1]}

CANDIDATES (JSON):
${JSON.stringify(result.candidates.slice(0, 6), null, 1)}

SCOUT NOTES (may be long; the operation matrix is here when the angle is operation-matrix: spot-check at least ten of its rows against the source and report wrong rows in "missed"):
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
