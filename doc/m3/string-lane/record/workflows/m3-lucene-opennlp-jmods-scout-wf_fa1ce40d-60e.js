export const meta = {
  name: 'm3-lucene-opennlp-jmods-scout',
  description: 'Scout and adversarially verify how the Lucene and OpenNLP forks can ship as jmods inside M3JDK and use the M3 string structure: the module recipe, Lucene text carriers, OpenNLP text carriers',
  phases: [
    { title: 'Scout', detail: 'three read-only readers: jmod recipe, Lucene string surfaces, OpenNLP string surfaces' },
    { title: 'Verify', detail: 'one skeptic per scout tries to refute every candidate against the source' },
  ],
}

const ROOT = 'I:/m3-scratch/string-converge-20261116'
const COMMON = `
You are working on M3JDK (hsoliwal/M3jdk21: a JDK 21 fork where java.lang.String can be backed by an M3String, a composite/rope of atoms and tuples with precomputed monoid measures in M3StringFacts). The owner's order of 2026-11-16, repeated four times: "forks: lucene and opennlp using M3 string structure as jmods in M3JDK". The owner's forks are hsoliwal/lucene and hsoliwal/opennlp (default branch main); M3jdk21 already vendors their sources.

READ-ONLY TASK. Do not create, edit or delete any file. Do not run git status, builds, javac or tests. Do not fetch from the network. Use Glob, Grep and Read (read large files in line ranges). Your final answer is the structured output only.

Sources:
- A full checkout of M3jdk21 from 2026-11-14 (two days older than master; say so when you cite it): K:/m3-work/wt-fix3/ . Vendored libraries: m3/vendor/third_party/lucene-core (1,097 Java files), lucene-analysis-common (552), lucene-queryparser (250), lucene-queries (165), lucene-sandbox (53), lucene-analysis-opennlp (20), opennlp-tools (729); m3/vendor/third_party-m3/nlp-search (13); the pack m3/tooling/module-packs/nlp-search (14 files) and its sibling packs (parser, jackson, text-tools). JDK modules of M3: src/jdk.m3.annotations, jdk.m3.collections, jdk.m3.graphics (Skija was absorbed here), jdk.m3.io, jdk.m3.llm, jdk.m3.logging, jdk.m3.paradigm, jdk.m3.tornado, each with share/classes/module-info.java; the JDK build glue under make/ (make/modules, make/conf, make/common/Modules.gmk) and src/java.base/share/classes/module-info.java.
- M3 string owners at master: CURRENT String.java and M3StringFacts.java in ${ROOT}/leaf1/head/src/java.base/share/classes/java/lang/ ; the other java.lang owners in ${ROOT}/master-java-lang/ (ignore the String.java and M3StringFacts.java there); jdk.internal.mindex (it holds M3Automaton, M3AutomatonOps, M3RegExpCompiler, M3RunTable: an automaton core that descends from Lucene util.automaton), jdk.internal.m3.* and java.util.regex in ${ROOT}/owner/src/src/java.base/share/classes/
- Invariants and state of the string lane (read sections 2, 3 and 7): ${ROOT}/record/docs/m3/string-lane/HANDOVER.md

Owner's rules that bind any proposal:
- Libraries keep their upstream packages and public API; the JDK owns each fast implementation once, in java.base, and libraries call it; no duplicate fast paths.
- No new public or protected class or member in the JDK's own modules without the owner's admission; internals reach a library module through qualified exports, never through new public API in java.lang.
- One integrated source tree in the OpenJDK layout (src, make, test, doc) is the target; the m3/ folder is being migrated away. A new module root is an owner decision: the order above admits Lucene and OpenNLP modules in principle, but names and scope are still decisions to list.
- M3JDK must build, test and run from what it carries (no external artifacts at build time).
- Licences: java.base is GPLv2 with the Classpath exception; Lucene and OpenNLP are Apache-2.0. Apache-2.0 code may ship as its own module with its legal notices; it may not be copied into GPLv2 java.base sources without the owner's ruling.
- Behaviour of Lucene and OpenNLP public API must stay identical (index bytes, token streams, model outputs).
- A per-unit charAt on a composite M3String is an owner dispatch, not an array read; toCharArray and getBytes of a whole M3-backed value flatten it.

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
    key: 'jmod-recipe',
    prompt: `${COMMON}

ANGLE: the module recipe. How a library becomes a module and a jmod of the M3JDK image, and what is missing for Lucene and OpenNLP.
Establish from the code and build files: (1) what m3/tooling/module-packs/nlp-search and m3/vendor/third_party-m3/nlp-search are today: do they already build Lucene and OpenNLP as named modules, as jmods, or as jars; which Lucene and OpenNLP versions (look for version files, pom.xml, MANIFEST, module-info.java inside the vendored trees; Lucene 9 and later ships module-info.java per module); how the other packs (parser, jackson, text-tools) are built and linked; (2) how an M3 JDK module under src/ gets into the image: take src/jdk.m3.graphics (an absorbed third-party library) and src/jdk.m3.collections as worked examples and list every file that had to exist or change (module-info.java, make/modules/<module>/*.gmk, make/conf/module-loader-map.conf or docs, legal notices under src/<module>/share/legal, test roots, any list of modules for the boot or platform loader, jlink/jmod configuration); (3) how java.base exports internals to M3 modules today (qualified exports in src/java.base/share/classes/module-info.java for jdk.internal.mindex, jdk.internal.m3.*, jdk.internal.access) and which of them a Lucene or OpenNLP module would need to read M3 string measures and cursors; (4) the dependency closure: which vendored Lucene modules and OpenNLP parts depend on what (other libraries, slf4j, service loaders in META-INF/services, resources such as analyzers' stopword files and codecs' SPI files), because a jmod needs every dependency as a module.
Candidates = the ordered leaves that take the forks to jmods, smallest first, for example: module descriptors and names; make glue for one module (lucene core) as the pilot; legal notices; SPI and resources; the qualified exports; a smoke test that the image resolves the modules (java --list-modules, a tiny index-and-search program); then OpenNLP. For every leaf say what the owner must decide (module names, which Lucene modules, loader, whether sources move from m3/vendor into src/<module>) and what you could not establish.`,
  },
  {
    key: 'lucene-strings',
    prompt: `${COMMON}

ANGLE: Lucene's text carriers and where the M3 string structure fits.
Read the vendored Lucene sources (m3/vendor/third_party/lucene-core and lucene-analysis-common under K:/m3-work/wt-fix3). Catalogue in "notes", one line each, the classes through which text flows and how they hold or read it: BytesRef, BytesRefBuilder, BytesRefHash, CharsRef, CharsRefBuilder, UnicodeUtil (UTF16toUTF8, UTF8toUTF16, calcUTF16toUTF8Length), CharTermAttributeImpl (append(String), append(CharSequence), toString, the char[] term buffer), Term, Field.stringValue paths and StringField / TextField, analysis CharArraySet / CharArrayMap, CharacterUtils, Tokenizer buffering (CharacterBuffer), the util.automaton package (Automaton, Operations, RegExp, RunAutomaton, UTF32ToUTF8), FST Util string inputs, queryparser token images, similarity of string comparisons in sorting (BytesRef compare).
For each say: where a java.lang.String enters (charAt loop, getChars, toCharArray, getBytes(UTF_8), length()), what that costs for an M3-backed String (per-unit dispatch, whole flatten), and which M3 facility could answer instead without changing Lucene's public API: the utf8Length measure for exact buffer sizing, M3String bulk getChars windows and encode, ascii / latin1 flags to take a single-byte path, the hash monoid, the M3 intern pool, the frozen lexicon index for stop sets, and the M3 automaton core in jdk.internal.mindex (Lucene's automaton and the JDK's now exist twice: under the rule "the JDK owns the fast implementation once" say which direction a convergence could take and what blocks it: licence, API shape, UTF-8 vs UTF-16 alphabets).
Candidates = the six best first changes inside the Lucene fork, each a place where a String is consumed and an M3 facility removes a flatten, a per-unit loop or a second scan, with the exactness argument and a differential proof plan (same bytes in BytesRef, same token stream, same index for a small corpus). Mark clearly any candidate that needs a qualified export from java.base and name the package.`,
  },
  {
    key: 'opennlp-strings',
    prompt: `${COMMON}

ANGLE: OpenNLP's text carriers and where the M3 string structure fits.
Read the vendored OpenNLP sources (m3/vendor/third_party/opennlp-tools under K:/m3-work/wt-fix3, and lucene-analysis-opennlp for the bridge). Catalogue in "notes", one line each, how text flows: tokenizers (SimpleTokenizer, WhitespaceTokenizer, TokenizerME and its context generators), sentence detection (SentenceDetectorME, end-of-sentence scanners), Span and Span.getCoveredText / spansToStrings (substring per token), StringUtil (isWhitespace, toLowerCase, toUpperCase, isEmpty, Levenshtein distance if present), StringList and dictionaries (Dictionary, case-insensitive wrappers), n-gram models (NGramModel, NGramUtils, NGramCharModel), feature generation (string concatenation of feature names and values such as "w=" + token, prefix and suffix features, token class features via StringPattern / FeatureGeneratorUtil), the maxent / perceptron predicate maps keyed by feature strings (hash maps of String to index), normalizers (regex-based CharSequenceNormalizer implementations), language detection n-grams, and model IO of strings (readUTF / writeUTF).
For each say what it costs today (substring copies, concatenation copies, hashCode over fresh strings, regex per token, per-unit loops) and which M3 facility fits without changing OpenNLP's public API: composite substrings and concatenations that share units (no copy), cached hash through the hash monoid (a concatenation's hash from its parts), the intern pool for feature strings, class masks for token class tests (all digits, all letters, capitalised: exact only for ASCII unless the definitions agree with Character.isLetter / isDigit), trim and strip edges, the line terminator and white-space measures for sentence and token scanning, the frozen lexicon index for dictionaries, n-gram signals.
Candidates = the six best first changes inside the OpenNLP fork, each with the exactness argument (same tokens, same spans, same feature strings and therefore the same model outcomes and probabilities) and a differential proof plan against the unmodified library on a small corpus and the stock models' unit tests. Mark any candidate that needs a qualified export from java.base and name the package; note which ones work already through public String methods once the JDK side has an M3 path.`,
  },
]

const verifyPrompt = (scout, result) => `${COMMON}

YOU ARE THE SKEPTIC for the scout "${scout.key}". Another agent produced the candidates below. Your job is to REFUTE them against the source. Default to refuted=true when you cannot confirm a claim from the code.
For every candidate: (1) open the cited file and lines: does the quoted evidence exist there? (2) is the claim about the build, the module system or the text path real: check module-info files, make files, the vendored versions and what the cited method does; (3) exactness: would index bytes, token streams, spans, feature strings and model outputs stay identical, including non-ASCII text and surrogates; (4) legality: licence placement (Apache-2.0 code staying in its own module), qualified exports instead of new public API, split packages, automatic vs named modules, service loader files, resources, the loader the module would live in; (5) does the proposal respect "libraries keep upstream packages and API" and "one fast implementation in java.base"; (6) is the gain plausible?
Give for each candidate: refuted true/false, whether the evidence exists as quoted, the reason with file and line, a corrected claim when the idea survives in a narrower form, and the hazards an implementer must handle. In "missed", name what the scout overlooked (a blocking dependency, an SPI file, a version mismatch between the vendored copy and the fork, a better first leaf) and report wrong rows you find when you spot-check the notes.

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
