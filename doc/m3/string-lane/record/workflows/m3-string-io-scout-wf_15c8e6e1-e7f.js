export const meta = {
  name: 'm3-string-io-scout',
  description: 'Scout and adversarially verify: how java.io readers and writers, charset encoders and decoders, and file read/write helpers consume and produce Strings in M3JDK, and where they can use the M3 composite and its measures',
  phases: [
    { title: 'Scout', detail: 'two read-only readers: the write/encode side and the read/decode side' },
    { title: 'Verify', detail: 'one skeptic per scout tries to refute every candidate against the source' },
  ],
}

const ROOT = 'I:/m3-scratch/string-converge-20261116'
const COMMON = `
You are working on the string lane of M3JDK (a JDK 21 fork where java.lang.String can be backed by an M3String: a composite/rope of atoms and tuples with precomputed monoid measures in M3StringFacts). The owner's standing orders: "M3 string is extremely crucial", "also enhance the pre compute", "all of the string operations", and now: "also the M3 JDK reader and writer can leverage this".

READ-ONLY TASK. Do not create, edit or delete any file. Do not run git status, builds, javac or tests. Do not fetch from the network. Use Glob, Grep and Read (read large files in line ranges). Your final answer is the structured output only.

Sources:
- CURRENT String.java and M3StringFacts.java (M3jdk21 master): ${ROOT}/leaf1/head/src/java.base/share/classes/java/lang/
- The other java.lang string owners (M3String.java with getChars / getBytes / encode / CodeUnitCursor, M3StringLeafCursor.java, M3StringLeafRanges.java, AbstractStringBuilder.java, M3StringBuilderLane.java, StringCoding.java, ...): ${ROOT}/master-java-lang/  (IGNORE String.java and M3StringFacts.java in this folder: one commit older)
- jdk.internal.mindex and jdk.internal.m3.* (including jdk/internal/m3/io) at master: ${ROOT}/owner/src/src/java.base/share/classes/
- A full checkout of M3jdk21 from 2026-11-14 (two days older than master; say so when you cite it): K:/m3-work/wt-fix3/src/java.base/share/classes/ with java/io (Reader, Writer, StringReader, StringWriter, BufferedReader, BufferedWriter, PrintWriter, PrintStream, InputStreamReader, OutputStreamWriter, CharArrayWriter, DataOutputStream, DataInputStream, ObjectOutputStream, LineNumberReader), sun/nio/cs (StreamEncoder, StreamDecoder, UTF_8, ISO_8859_1, US_ASCII), java/nio/file/Files.java, java/nio/charset, java/util/Scanner.java, java/lang/StringCoding.java; and K:/m3-work/wt-fix3/m3 for any M3 IO module (Grep for M3IO).
- Invariants and state of the lane (read sections 2 and 3 first): ${ROOT}/record/docs/m3/string-lane/HANDOVER.md

Measures an M3-backed String already carries (M3StringFacts): utf16Length, utf8Length (exact encoded size, -1 when it overflows), modifiedUtf8Length (the DataOutput.writeUTF size), codePointCount, unpairedSurrogateCount, ascii and latin1 flags, class masks, first and last units, the line terminator count (LF, CR, CR LF as one), trim and strip edges. facts() prepares them with one scan and caches; factsIfPrepared() never prepares. Bulk reads: M3String.getChars(begin, end, char[], offset) copies a range without flattening the whole value; leaf cursors walk runs.

Hard rules for any proposal:
- I2 / I3: no whole-value flatten of an M3-backed String (toCharArray, value(), materialize, getBytes of the whole value into a temporary) where bulk range reads, a leaf-run walk or a measure can answer.
- I7: no new public or protected class or member. Internal paths behind existing java.io / java.nio API only.
- Behaviour must stay identical: bytes written, malformed-input and unmappable-character handling (replacement vs exception), flush and autoflush points, line separator handling, exceptions and their order, synchronization on the stream lock, and results for CharSequences that are not Strings.
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
    key: 'write-encode',
    prompt: `${COMMON}

ANGLE: the write and encode side. Every place a String (or StringBuilder) leaves the JVM as characters or bytes.
Trace, for an M3-backed String argument: Writer.write(String) and write(String, int, int), Writer.append(CharSequence), StringWriter, CharArrayWriter, BufferedWriter.write(String, int, int), PrintWriter.print / println / write, PrintStream.print / println (and its internal charOut / textOut), OutputStreamWriter and sun.nio.cs.StreamEncoder (how it gets chars from a String: getChars into a buffer, or CharBuffer.wrap), String.getBytes(...) and M3String.encode, Files.writeString and Files.write(Path, Iterable), DataOutputStream.writeUTF and ObjectOutputStream string writing (they compute the modified UTF-8 length with a loop over charAt: the modifiedUtf8Length measure answers it), StringCoding helpers, and any M3 IO owner.
For each path put one line in "notes": "API | how the String is read (bulk getChars window, toCharArray, charAt loop, value(), encode) | whole-value flatten yes/no | temporary allocations | a measure that could size or shortcut it".
Candidates: paths that flatten an M3-backed value or dispatch per unit, or that compute a size a measure already holds (utf8Length for exact buffer sizing, modifiedUtf8Length for writeUTF, ascii / latin1 flags to pick the single-byte encode loop without a scan), each with the smallest internal change and a differential proof plan (bytes written equal to stock for random strings including surrogates and unmappable characters, same flush points).`,
  },
  {
    key: 'read-decode',
    prompt: `${COMMON}

ANGLE: the read and decode side. Every place characters or bytes become Strings, and readers that read from a String.
Trace: StringReader (read(), read(char[], int, int), skip, how it reads its String: charAt per unit or getChars ranges); BufferedReader.readLine and lines() (how a line String is built: new String(char[], off, len) per line, StringBuilder for long lines; whether the result is admitted as an M3 atom; whether line splitting could reuse one decoded buffer as composite slices instead of copying each line); LineNumberReader; InputStreamReader and sun.nio.cs.StreamDecoder; Files.readString, Files.readAllLines, Files.lines; Scanner (how it wraps input and applies regex); new String(byte[], Charset) and the String constructors that admit into M3 (m3AdmitNative and friends in the CURRENT String.java); DataInputStream.readUTF; CharBuffer.wrap(CharSequence) / StringCharBuffer; String.chars() and codePoints() for an M3-backed value.
For each path put one line in "notes": "API | how the result String is built or how the source String is read | copies per character | M3 admission yes/no | what the composite or a measure could do".
Candidates: readers that dispatch per unit on a composite source (StringReader over an M3-backed String), line readers that copy every line where slices of one admitted buffer would do, decoders that could record facts while decoding (the decode loop already sees every unit: ascii / latin1 / line terminator count could be captured for free) so the first facts() call is not a second scan, each with the smallest internal change and a differential proof plan (same Strings, same line boundaries for LF, CR, CR LF and a final line without a terminator, same exceptions on malformed input).`,
  },
]

const verifyPrompt = (scout, result) => `${COMMON}

YOU ARE THE SKEPTIC for the scout "${scout.key}". Another agent produced the candidates below. Your job is to REFUTE them against the source. Default to refuted=true when you cannot confirm a claim from the code.
For every candidate: (1) open the cited file and lines: does the quoted evidence exist there? (2) is the claimed flatten, per-unit read or missing measure use real for an M3-backed String: trace what the String method it calls does on the M3 path; (3) would the change keep the bytes, the Strings, the line boundaries, the exceptions, the flush points and the locking identical; (4) does it add any public or protected member (forbidden); (5) is the gain plausible, or does it add a scan or an allocation on the common path, or retain a large buffer through slices (a memory hazard)?
Give for each candidate: refuted true/false, whether the evidence exists as quoted, the reason with file and line, a corrected claim when the idea survives in a narrower form, and the hazards an implementer must handle. In "missed", name better candidates of the same angle that the scout overlooked, and report wrong rows you find when you spot-check at least eight rows of the table in the notes.

SCOUT ANGLE:
${scout.prompt.split('ANGLE:')[1]}

CANDIDATES (JSON):
${JSON.stringify(result.candidates.slice(0, 6), null, 1)}

SCOUT NOTES:
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
