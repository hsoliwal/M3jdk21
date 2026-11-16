export const meta = {
  name: 'm3-univec-scout',
  description: 'Scout and adversarially verify "univec" for M3String: vectorized Unicode kernels (SWAR, intrinsics, native), UnicodeX rank-coded words as an M3 owner profile, and additive vector measures for similarity',
  phases: [
    { title: 'Scout', detail: 'three read-only readers: vectorized Unicode kernels, UnicodeX profile, vector measures' },
    { title: 'Verify', detail: 'one skeptic per scout tries to refute every candidate against the source' },
  ],
}

const ROOT = 'I:/m3-scratch/string-converge-20261116'
const COMMON = `
You are working on M3JDK (hsoliwal/M3jdk21: a JDK 21 fork where java.lang.String can be backed by an M3String, a composite/rope of atoms and tuples with precomputed monoid measures in M3StringFacts). The owner wrote "univec" and, asked what it means, answered: "ALL of the above, whatever is best and works for m3string and m3jdk", where the options were (a) vectorized Unicode: UTF-8/UTF-16 validation, transcoding and scans word-at-a-time or with vector instructions (the simdutf approach) in the M3 string encoders, decoders and scan kernels; (b) UnicodeX (the owner's synexia-unicodex module): its frozen word image and rank-coded words as an M3String owner or immediate profile; (c) universal vectors: one vector representation per string or token carried as an M3 measure for similarity and NLP.

READ-ONLY TASK. Do not create, edit or delete any file. Do not run git status, builds, javac or tests. Do not fetch from the network. Use Glob, Grep and Read (read large files in line ranges). Your final answer is the structured output only.

Sources:
- M3 string owners at M3jdk21 master: CURRENT String.java and M3StringFacts.java in ${ROOT}/leaf1/head/src/java.base/share/classes/java/lang/ ; the other java.lang owners in ${ROOT}/master-java-lang/ (M3String.java with encodeUtf8 / getChars / getBytes / CodeUnitCursor, M3StringAtom.java: units live in native memory allocated with Unsafe, M3StringImmediate.java: two packed profiles, Latin-1 up to 7 units and a 6-bit identifier alphabet up to 10 units, M3StringPool.java, M3StringPositionPrecompute.java, M3StringSearchPrecompute.java, StringCoding.java, StringLatin1.java, StringUTF16.java; ignore the String.java and M3StringFacts.java in that folder, they are older); jdk.internal.mindex, jdk.internal.m3.* (hash, intern, compression) and java.util.regex in ${ROOT}/owner/src/src/java.base/share/classes/
- A full checkout of M3jdk21 from 2026-11-14 (two days older than master; say so when you cite it): K:/m3-work/wt-fix3/ with src/java.base/share/classes (jdk/internal/util/ArraysSupport.java, jdk/internal/misc/Unsafe.java, sun/nio/cs/UTF_8.java, java/lang/StringCoding.java), src/java.base/share/native/libjava (Grep for M3 C kernels: M3Line*.c, m3_jni_util.h and other M3*.c), src/hotspot (only to check which String intrinsics exist: Grep vmIntrinsics.hpp for _countPositives, _compressStringC, _inflateStringC, _encodeISOArray, _vectorizedMismatch, _indexOfL), src/jdk.incubator.vector if present, m3/lexicon, m3/nlp or any module named nlp.
- The owner's UnicodeX module in a working checkout: E:/ws2/com.synexia/synexia-unicodex (many design documents at its top level, for example PRECOMPILED_WORD_FAST_PATH.md, BYTE_LEVEL_ENCODING.md, DIMENSIONAL_ADDRESSING.md, LAYER_0_ALLOCATION.md, MASTER_SPECIFICATION_SUMMARY.md, and Java sources under src/main/java).
- Invariants and state of the string lane (read sections 2 and 3): ${ROOT}/record/docs/m3/string-lane/HANDOVER.md

Owner's rules that bind any proposal:
- M3JDK is the JDK: java.lang, java.base and HotSpot are changed in place; JNI and HotSpot work is allowed for M3JDK. java.base cannot depend on an incubator module.
- A precompute measure is a monoid with a seam rule: scan(a + b) == compose(scan(a), scan(b)) at every cut. A measure needs a concrete reader to be worth its scan cost and its bytes per record.
- No new public or protected class or member without the owner's admission.
- Never change observable String behaviour: results, exceptions, malformed-input handling (unpaired surrogates encode as '?' or the replacement, exactly as stock).
- Absorb mechanisms, never corpora: a word list or model may not be copied into the JDK; only the mechanism, with data supplied at run time or built from the program's own strings.
- Donors first: name existing owners in the tree before proposing new code; for outside donors give the licence (simdutf is Apache-2.0 OR MIT; state what you know and mark what you could not verify, since you cannot fetch).

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
    key: 'vectorized-unicode',
    prompt: `${COMMON}

ANGLE (a): vectorized Unicode kernels for M3 strings.
Catalogue in "notes" every kernel on the M3 path that walks units or bytes one at a time, one line each: "kernel (file:lines) | what it computes | unit source (native atom memory, byte[] window, char[] window, per-unit owner dispatch) | scalar loop shape | which existing vectorized facility could do it". Cover: M3StringFacts.scan (a 26-measure fold per unit over 1,024-unit char windows); M3String.encodeUtf8 and encodeUtf8NoRepl and the utf8Length measure; the decode and admission paths in String.java (maybeAdmit*, decodeForM3Construction) and M3StringPool intern (hash + compare under a bucket monitor); M3StringAtom comparisons (compareRange, contentEquals*); getChars / getBytes copies with inflate or compress; M3StringPositionPrecompute linear scans; caseMap; the trim and strip edge scans; regex class tests.
Facilities to match against, all usable from java.base: the HotSpot intrinsic candidates StringCoding.countPositives / hasNegatives, StringLatin1.inflate, StringUTF16.compress, implEncodeISOArray / implEncodeAsciiArray, ArraysSupport.vectorizedMismatch (it works on raw addresses through Unsafe base + offset, so it can compare native atom memory), StringLatin1 / StringUTF16 indexOf intrinsics, Unsafe.getLong for SWAR word-at-a-time tests on native memory (has-byte, has-high-bit, has-zero), and M3 C kernels in libjava (JNI is allowed for M3JDK; the simdutf algorithms for UTF-8 validation and UTF-16 to UTF-8 transcoding are the pattern donor). State for each candidate which facility is exact for the job and what the scalar fallback must keep (unpaired surrogates, the modified UTF-8 rules, endianness of native memory).
Candidates: the six with the largest gain, each naming the kernel, the facility, the exactness argument and a differential proof plan against the scalar kernel on random text including surrogates at window and atom seams. Prefer changes that need no new native code first.`,
  },
  {
    key: 'unicodex-profile',
    prompt: `${COMMON}

ANGLE (b): UnicodeX as an M3String owner or immediate profile.
Read the UnicodeX design documents and sources enough to state precisely: what the frozen word image is (how a word gets a rank or id, the byte-level encoding of an id, the layers and dimensional addressing), what the precompiled word fast path does, how large the image is, and what lookups cost (word to id, id to text). Then read M3StringImmediate.java fully: the tag bits, the two profiles (Latin-1 up to 7 units, 6-bit identifier alphabet up to 10 units), how pack refuses a value, how charAt, length and hash are computed from the packed word, how an immediate is interned (PackedPage slots), and where String admission tries the immediate before an atom (Grep String.java and M3StringPool.java for M3StringImmediate).
Evaluate, with the code open: (1) a third immediate profile that packs a word as a rank or id from a run-time-supplied frozen lexicon (the existing M3FrozenLexiconIndex in java.lang and the lexicon owners in jdk.internal.mindex or m3/lexicon): what charAt, length, hashCode, equals and compareTo would cost (an id needs a table read to produce units), what breaks if the lexicon is not loaded, how identity and interning behave across class loaders and CDS; (2) word-id sequences as a tuple of id atoms for text (token streams), and how that meets M3TokenIndex lexeme ids; (3) the measurement the ledger asks for first: the hit rate of real program strings (identifiers, literals, log text) against the two existing profiles and against a word-rank profile. Say what data an implementer must collect and how (a counting pass over class file constant pools and over a text corpus, CPU only).
Candidates, smallest first, each honest about cost and about the rule "absorb the mechanism, never the corpus": for example widening the identifier profile's alphabet or length if the packing has spare bits (show the bit arithmetic), a profile for short decimal numbers, a lexicon-backed id owner behind the existing frozen index, or nothing new if the two profiles already cover the measured share. For each give the exactness argument and the proof plan (round trip of pack and unpack over exhaustive short strings, hash equality with String.hashCode).`,
  },
  {
    key: 'vector-measures',
    prompt: `${COMMON}

ANGLE (c): vector measures on M3 strings for similarity and NLP.
First establish what exists: the bigram and trigram 64-bit signals and the unit presence signal in M3StringFacts (how they are folded and composed, including the seam n-grams built from the last and first units), M3TQ trigram facts, any MinHash, SimHash, sketch, Bloom or count-min owner in jdk.internal.m3.*, jdk.internal.mindex, src/jdk.m3.collections (it has a sketch package) and m3/ (Grep for MinHash, SimHash, Sketch, Bloom, CountMin, HyperLogLog, Embedding, Vector), and who reads them.
Then judge candidate vector measures strictly as monoids with a concrete reader: (1) an additive hashed n-gram count vector (feature hashing, for example 64 or 128 int lanes; compose = lane-wise add plus the seam n-grams; cosine or Jaccard estimates from two vectors) as the base for near-duplicate detection and fuzzy matching; (2) MinHash signatures (compose = lane-wise min plus seam n-grams: a monoid) for Jaccard similarity of shingle sets; (3) SimHash (not a monoid by itself; it is derivable from the additive count vector) and where its 64-bit form already exists in the owner's atomizer tooling; (4) a HyperLogLog of distinct n-grams (compose = lane-wise max); (5) token-level vectors for OpenNLP and Lucene: say plainly that learned embeddings are data, not a mechanism, and may not be carried in the JDK; only the slot and the algebra could be.
For each say the bytes per facts record, the scan cost per unit, the seam rule, the exact or approximate nature of what a reader gets, and name a concrete reader in the tree (for example the pool's near-duplicate detection if it exists, String similarity utilities, Lucene's or OpenNLP's n-gram code in m3/vendor) or state that there is none, in which case the candidate is an ADMISSION item for the owner (a new operation), not an internal leaf. Prefer a lazily prepared side record over growing every M3StringFacts record.
Candidates: at most six, best first, each labelled in its title INTERNAL: (a reader exists) or ADMISSION: (needs the owner's decision), with the algebra proof plan (compose equals scan at every cut on random text, including surrogates at the seam).`,
  },
]

const verifyPrompt = (scout, result) => `${COMMON}

YOU ARE THE SKEPTIC for the scout "${scout.key}". Another agent produced the candidates below. Your job is to REFUTE them against the source. Default to refuted=true when you cannot confirm a claim from the code.
For every candidate: (1) open the cited file and lines: does the quoted evidence exist there? (2) is the claimed scalar loop, spare bit, missing reader or existing facility real: check the intrinsic or helper signature and whether it can take the unit source in question (native address vs heap array, byte order, 1-byte vs 2-byte units); (3) exactness: identical results for unpaired surrogates, modified UTF-8, hash values, equals and compareTo order, and for a measure the monoid law at every cut including n-grams across the seam; (4) rules: no new public or protected member, java.base not depending on an incubator module, mechanism not corpus, a concrete reader for every measure; (5) is the gain plausible: window sizes, JNI call overhead for short strings, bytes added to every facts record, table reads behind an id owner's charAt?
Give for each candidate: refuted true/false, whether the evidence exists as quoted, the reason with file and line, a corrected claim when the idea survives in a narrower form, and the hazards an implementer must handle. In "missed", name better candidates of the same angle that the scout overlooked, and report wrong rows you find when you spot-check at least eight rows of any table in the notes.

SCOUT ANGLE:
${scout.prompt.split('ANGLE (')[1]}

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
