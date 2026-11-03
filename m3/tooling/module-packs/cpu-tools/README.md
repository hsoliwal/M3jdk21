<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3 CPU tools: optional JMOD integration

This pack composes the existing M3 text DAG, file/text owners and javac frontend
with sixteen selected Synexia CPU tool modules. A successful build links
**exactly 20 optional modules**: three existing modules, sixteen adapted donor
modules and the `com.m3.tools.cpu` aggregate. Standard JDK modules are additional
dependencies and are not part of that twenty-module count.

The current `CpuTools` registry contains **22 executable operations**. The
modules also export direct Java APIs for literal search, regex, edit distance,
indexing, intervals, priority queues, file snapshots and compiler projections.
The 110 rows in the existing CPU category matrix represent **91 category rows
and 19 Unix command rows**. This pack does not complete that catalogue or imply
that every listed solver, format, distributed protocol or media engine exists.

This is an optional Apache-2.0 SDK module integration. It does not advance the
repository's String -> arrays -> collections -> AST/compiler -> remaining
families product qualification sequence. Exact candidate qualification belongs
in [STATUS.tsv](STATUS.tsv), the canonical recipe receipts and each build's
`BUILD_RECEIPT.json`. Source presence and this README do not assert a passing
image, recipe or full JDK product gate.

## Ownership and source contracts

`jdk.m3.io.M3File` admits one bounded, strictly decoded UTF-8 file snapshot. It
retains the public `String` reference, exact raw-byte SHA-256, separate UTF-16
SHA-256, source URI and observed file attributes. It follows the inspected
`MIndexFileStreamIndexer` single-stream admission shape. It is **not the full
MIndexFile token, occurrence, postings, persistence or precompute plane**.
It retains no second raw-byte snapshot or string pool. The current per-file
admission ceiling is 16 MiB. A before/after metadata check detects observed
changes; it is not an atomic filesystem snapshot or protection against all
concurrent ancestor replacement and metadata-restoring writes.

Java callers continue to use `String` and `CharSequence`. The M3 runtime owns
the internal `java.lang.M3String` representation when M3 string storage is
enabled. The CPU tools neither rename the public JDK class nor add a competing
payload owner. The qualification driver checks the real M3 implementation and
runs source and pipeline tests with M3 storage both at the runtime default and
explicitly enabled. A stock Java 21 image is not sufficient for this pack's
M3 compiler/progress APIs.

`M3AstTools` captures facts from actual `com.sun.source.util.M3AST` callbacks.
`consumer(...)` and `capture(...)` reuse a caller's existing compilation;
`parse(List<M3File>)` explicitly starts a syntax-only javac task with
`-proc:none` over the admitted immutable strings. It does not reopen the files,
perform attribution, run annotation processors or generate classes. Detached
snapshots retain primitive structural coordinates and declared-name metadata;
public fact records are projected on access. No live compiler tree, `Unit`,
compiler task or type/element object escapes in a snapshot. Symbol and method
facts are syntax facts, not resolved call graphs or portable semantic caches.
Coordinates are half-open UTF-16 ranges; missing compiler positions are counted
explicitly. Raw-byte and UTF-16 digests remain distinct. Admission allows at most
1,024 files, 16 Mi UTF-16 units per source, 64 Mi UTF-16 units cumulatively,
100,000 visited nodes and depth 256. The CLI checks the cumulative source
admission before invoking the compiler.

## The twenty modules

All names below are actual source module names expected by the build. The
sixteen `com.m3.tools.*` donor modules retain target-owned package names; the
pack has no runtime dependency on `com.synexia`.

| Module | Main responsibility |
| --- | --- |
| `jdk.m3.text` | Existing `TextFrame`, `TextTools`, `Registry`, `Plan`, `M3UnixRuntime`, limits and receipts |
| `jdk.m3.io` | Existing file tools and JNI; additive `M3File` snapshot adapter |
| `jdk.m3.structured` | Existing CSV/TSV, JSON Lines, INI/env and structured CLI |
| `com.m3.tools.text.normalize` | JDK Unicode normalization |
| `com.m3.tools.text.tokenize` | JDK word-break tokenization |
| `com.m3.tools.text.regex` | Direct JDK regex match API with a match-count cap |
| `com.m3.tools.text.search` | Exact overlapping UTF-16 literal positions and one-plan ordered-corpus search |
| `com.m3.tools.text.ngram` | Overlapping UTF-16 n-gram counts |
| `com.m3.tools.text.frequency` | Exact term counts |
| `com.m3.tools.text.similarity` | Bounded UTF-16 Levenshtein distance |
| `com.m3.tools.text.diff` | Common-prefix/suffix delta, bounded UTF-16 scripts, conservative character/line three-way merges and lossless indexed-line diff |
| `com.m3.tools.text.index` | Exact immutable term/document postings in primitive lanes |
| `com.m3.tools.graph.topology` | Deterministic integer topological order |
| `com.m3.tools.graph.paths` | Nonnegative weighted shortest-path distances |
| `com.m3.tools.stats.streaming` | Welford streaming moments |
| `com.m3.tools.constraint.interval` | Half-open interval overlap and conflict positions |
| `com.m3.tools.codec.varint` | Canonical LEB128 for `[0, Long.MAX_VALUE]` |
| `com.m3.tools.verify.sha256` | SHA-256 of bytes or UTF-8 text |
| `com.m3.tools.plan.priority` | Stable score priority queue with parallel storage lanes |
| `com.m3.tools.cpu` | Registry bindings, existing-DAG composition, source receipts, literal-search completeness and M3AST projections |

[DONOR_MODULES.tsv](DONOR_MODULES.tsv) binds the sixteen selected modules to
Synexia revision `50f48cbbe11f17fb8dc209bac3812f8940a1e267`, original paths,
Git blobs and SHA-256 hashes. The donor adaptations, source metadata and
per-file change notices describe target refinements. Lucene, OpenNLP, model
quantization/distillation and WebGPU are not part of this selected module set;
their separate existing receivers and qualification work retain their own
status.

## Build and run

Use Linux, an M3 Java 21 development image, a C compiler, Python 3.10 or newer,
PATH-selected `objcopy` (binutils), and Maven for the Maven entry point.
Select a clean source M3 JDK with none
of these twenty optional modules already installed. The driver checks the
source image's `release` `MODULES` inventory and its `jmods/*.jmod` names before
creating output, and refuses any collision. This matters because the existing
packager searches source-JDK JMODs before freshly built candidate JMODs. An
earlier text-tools or CPU-tools linked image is therefore not a suitable
source toolchain. The supplied JDK remains read-only.

Set a task-specific path variable; the build does not change a global
`JAVA_HOME`.

```bash
M3_CPU_JDK=/absolute/path/to/m3-java-21-image
mvn -B -ntp clean verify \
  -f m3/tooling/module-packs/cpu-tools/pom.xml \
  -Dm3.jdk="$M3_CPU_JDK"
```

Maven uses `${java.home}` by default; `-Dm3.jdk=...` selects an explicit M3
toolchain. `m3.python` overrides `python3`, and `m3.cpu.out` overrides the
default `target/cpu-tools-image` output below this pack. An existing output
directory is refused rather than replaced. The equivalent direct command is:

```bash
python3 -B m3/tooling/module-packs/cpu-tools/build.py \
  --jdk-home "$M3_CPU_JDK" \
  --out m3/tooling/module-packs/cpu-tools/target/cpu-tools-image
```

The driver compiles source with `-source 21 -target 21`, compiles the original
`text-tools/native/m3textio.c`, and calls the existing
`m3/tooling/jmod-recipes/tools/jmod_pack.py` for JMOD creation and image linking.
There is no second packager. On Linux its `--strip-debug` option applies
`objcopy -g` to native libraries. The driver requires exact original compiled
bytes in the unique JMOD native member, then compares the linked library with
a separate expected copy transformed by the same PATH-selected `objcopy -g`.
The build receipt records compiled/JMOD/expected/linked hashes and the resolved
compiler/objcopy executable hashes and versions; PATH resolution and tool
bytes must match before and after qualification. The original JNI source and
compiled input remain intact.

The staging legal directory preserves both
`text-tools/legal` and `cpu-tools/legal` under distinct names before the
existing packager includes them in the JMODs. It then reads every notice from
every JMOD and from each linked module's legal directory and requires exact
byte-hash equality; internal jlink deduplication links are accepted.
Every subprocess receives at most 115 seconds plus five seconds of forced
process-group cleanup. These build-process limits are separate from the
in-process tool execution contracts described below. The packager subprocess
receives one controlled, quoted `JAVA_TOOL_OPTIONS` property selecting
`java.io.tmpdir` under the fresh output's `java-tmp` directory. Its nested
JMOD/jlink tools use that staging space, including native debug stripping;
other commands retain their sanitized environment and original PATH. The
normalized temporary directory is recorded in the packager command receipt.

After successful qualification, invoke the linked image directly:

```bash
M3_CPU_IMAGE=m3/tooling/module-packs/cpu-tools/target/cpu-tools-image/pack/image
"$M3_CPU_IMAGE/bin/java" -m com.m3.tools.cpu/com.m3.tools.cpu.CpuToolsCli --list
"$M3_CPU_IMAGE/bin/java" -m com.m3.tools.cpu/com.m3.tools.cpu.CpuToolsCli --self-test
"$M3_CPU_IMAGE/bin/java" -m com.m3.tools.cpu/com.m3.tools.cpu.CpuToolsCli \
  --run text.normalize.nfc,text.words,text.frequency input.txt
"$M3_CPU_IMAGE/bin/java" -m com.m3.tools.cpu/com.m3.tools.cpu.CpuToolsCli \
  --find needle input.txt 100
"$M3_CPU_IMAGE/bin/java" -m com.m3.tools.cpu/com.m3.tools.cpu.CpuToolsCli \
  --ast Example.java Other.java
```

`--list` writes the exact 22 binding IDs and escaped contract descriptions.
`--self-test` checks composition, graph order and replay; success starts with
`M3_CPU_TOOLS_PASS modules=20 bindings=22`, followed by implementation and
receipt hashes. Those hashes depend on the actual linked implementation and
runtime, so no sample hash is an expected universal value.

`--run OP[,OP...] FILE` validates the binding names before admitting the file.
It writes the final text to standard output and source/DAG/file receipts to
standard error. `--find NEEDLE FILE [MAX_HITS]` writes zero-based UTF-16
positions, including overlaps; standard error records hit count, result hash
and explicit completeness. The default is 1,000 hits and the accepted cap is
0 through 1,000,000. A capped prefix is never reported as a complete search.
`--ast FILE...` writes source, node, symbol, import and method records, with
AST identity/phase/count receipts on standard error. CLI syntax errors return
2; refused execution returns 1; successful execution returns 0.

The current registry IDs are:

```text
codec.varint.decode          codec.varint.encode
graph.shortest-paths        graph.topology          graph.tsort
hash.sha256.utf8            statistics.moments
structured.csv.first-column structured.tsv.first-column structured.env
text.identity              text.sort               text.sort.reverse
text.uniq                  text.normalize.nfc      text.normalize.nfkc
text.words                 text.frequency          text.ngram.2
text.ngram.3               text.fold.80            text.number
```

[COVERAGE.tsv](COVERAGE.tsv) gives the operation-level contracts and separates
registry bindings from direct APIs and deferred families. A Java caller can
create `CpuTools.session(limits, monitor)`, build a pipeline or reuse the
existing `Plan` DAG, execute an admitted `M3File`, and explicitly replay the
result. `M3AstTools.consumer(...)` is the integration point for an existing
compiler task.

## Limits and verification boundaries

The default text limits are 256 plan nodes, 2,048 graph/plan edges, 100,000
lines and 16 MiB per checked frame. The existing `Limits`, `PlanValidator`,
`FrameCollector` and `M3UnixRuntime` own admission and execution. These are
frame/graph bounds, not a total-JVM heap, RSS or arbitrary-instruction sandbox.
Borrowed progress monitors are checked cooperatively at operation, selected
loop and emission boundaries; a long library call may complete before the
next check. Direct donor APIs retain their individual limits, and are not
automatically governed by a `CpuTools.Session`.

The existing `MultiLiteralSearch.Prepared` uses one primitive-lane compiled
Aho-Corasick automaton for exact overlapping UTF-16 literal matching. Its new
`findCorpus` scans up to 4,096 ordered `String` owners (64 Mi UTF-16 units
combined, 16 Mi units per owner) without concatenating them; search state
resets at each source boundary so cross-file phantom matches cannot appear.
An exact global cap of 1,000,000 hits retains the file/end-offset/longer-first/
duplicate-pattern-ID prefix and checks one additional hit before reporting
`complete=false`. A zero cap can still be complete when no source contains a
hit. Source and pattern limits are validated before scanning, even if an
earlier file would exhaust the result cap. Results retain four primitive
int-array hit lanes, not a second input text collection; immutable API hit
records are projected on demand. `CpuTools.corpusLiteral` shares that prepared
plan over immutable `M3File` snapshots and seals **every ordered** URI,
raw-byte SHA-256, UTF-16 SHA-256, source length, the plan identity, global cap,
scanned-file count and exact emitted coordinates. `verifyCorpusLiteral`
recomputes. These APIs do not interpret regex syntax, which remains owned by
`Pattern`/M3Regex, and do not claim universal full-text indexing or query
engine performance.

The `RegexMatcher.Prepared` direct API compiles one expression through the
authoritative JDK `Pattern`/M3Regex engine, then creates a fresh `Matcher` for
each ordered immutable `String` source. `findCorpus` keeps per-file UTF-16
offsets, including zero-width matches, in **three primitive hit lanes** and
uses a global one-extra-match probe for honest capped completeness. The
`CpuTools.regexCorpus`/`verifyRegexCorpus` entry points seal every ordered
`M3File` URI, raw-byte and UTF-16 digest, byte/character geometry, exact
expression code units, match cap, completeness, scanned file count and
coordinates. Limits are 4,096 files, 64 Mi UTF-16 units combined, 16 Mi per
file, 4,096 regex UTF-16 units and 1,000,000 reported hits. The existing
`RegexMatcher.find` contract remains unchanged and continues returning the
full matched group. No source concatenation, new regex compiler, JNI symbol,
copied regex group payload or filesystem reopening is introduced. These
caps limit input storage and result count, **not** worst-case regex CPU
time or stack consumption: a pathological `Matcher.find` invocation is not
preemptible by the cooperative progress monitor. Untrusted regexes require
external time/resource isolation; no ReDoS-safety claim is made.

For repeated retrieval against unchanged source files,
`CpuTools.prepareRanking(List<M3File>[, ProgressMonitor])` performs **one**
bounded source-admission/tokenization pass and retains the incumbent immutable
`Bm25Ranker.Prepared` primitive posting index. Each
`PreparedRanking.rank(query, maxResults[, monitor])` changes only query-time
work; it does not reopen any M3File, re-tokenize source files or rebuild the
BM25 index. Result scores, total match counts, ranking/tie order and cap
completeness equal the existing `CpuTools.rank` one-shot implementation for
the same ordered corpus/query. The prepared source SHA seals **all ordered**
file URI, raw/UTF-16 digests and byte/character lengths, the exact prepared
BM25 corpus SHA and default BM25 parameters. Each query gets a distinct
canonical source- and query-bound receipt over exact UTF-16 query units and
scored output lanes. `PreparedRanking.verify` replays one query against its
immutable plan; `CpuTools.verifyPreparedRank` additionally rebuilds the plan
from newly supplied source snapshots to test full source admission. The prior
`CpuTools.rank` API and its v1 receipt remain unchanged. The same 4 Mi
combined source UTF-16 bound, 10,000 document cap, 1,000,000 token cap,
1,024 query terms and 10,000 result cap apply. The source-bound prepared
index is read-only and owns no retained file handles or duplicate source text.
Scores are approximate BM25 relevance signals, **not** identity or semantic
proofs. Preparation cost must be included when comparing one-shot latency.
The optional `fixtures/M3PreparedRankBenchmark.java` measures separately
prepared-index admission, repeated query time and the original full one-shot
path, including a one-query control and parity checks. One synthetic JVM
measurement is not a production latency or allocation guarantee.

The graph distance sentinel
`Long.MAX_VALUE` covers unreachable, exact-maximum and overflowing distances, and must
not be interpreted as a proved unreachable state. Floating-point moments
retain normal double rounding/overflow behavior; finite input samples can
produce nonfinite moments. UTF-16 n-gram fields escape
record delimiters and surrogate code units; n-gram width is not code-point
width. Varints cover nonnegative signed-long values, not all unsigned 64-bit
bit patterns. The original direct `TextDelta` is one common-prefix/suffix
replacement span; it is not a general semantic diff. `BoundedDiff` separately
computes an exact shortest insertion/deletion script over at most 262,144
UTF-16 units per side and at most 512 edits (replacement counts as two),
including unmatched surrogate halves. `ThreeWayMerge` composes two such
complete scripts against one base, admitting independent or identical edits;
overlapping edits, divergent co-located inserts and insertion exactly at a
replacement boundary are conservative conflicts. It cannot resolve semantic
conflicts and never returns a partial merge. Conflict witnesses are capped at
1,024 while the full conflict count and truncation state remain available.

The complementary `LineDiff` indexes whole lines as primitive 16-bit IDs and
reuses the same shortest-edit engine on those IDs. Each token includes its
exact CR, LF, CRLF or absent ending; the index neither normalizes line endings
nor retains a second set of line-text substrings. Its per-side limit is 16 Mi
UTF-16 units and 100,000 lines, with at most 65,536 **distinct lines across
both sources** and 512 insert/delete line operations. Over-budget distance is
incomplete with no usable patch; excessive source, line or unique-line
geometry is explicitly refused. This is not GNU `diff`/`diff3` output
compatibility or an unbounded large-file algorithm.

`LineThreeWayMerge` reuses two `LineDiff` scripts with a **shared base** to
compose independent full-line edits against original source line/UTF-16
coordinates. It incorporates identical changes only once and treats
intersecting replacements, divergent same-position insertions and insertions
at replacement boundaries as conflicts. A source exceeding either line
diff's explicit geometry limit is refused, and a distance over either side's
512-line budget yields `INCOMPLETE`; neither case exposes partial merged text.
The two source/side dictionaries are per diff, not a persistent global pool.
Conflict witnesses are capped at 1,024 and their full count is exposed.
It does not infer renames, semantic AST merges or automatic conflict resolution.

The existing `CpuTools.diff`, `merge`, `lineDiff` and new `lineMerge` Java entry points bind
their results to ordered immutable `M3File` URI, raw-byte and UTF-16 digests,
and algorithm/parameter/output facts in deterministic receipts. `verifyDiff`,
`verifyMerge`, `verifyLineDiff` and `verifyLineMerge` recompute; both
`FileLineDiff.apply` and `FileLineMerge.apply` refuse a substituted base/source
URI even when the text happens to be identical. These
algorithms use the canonical `String`/M3String text owner and do not alter
`String`, `Pattern`/M3Regex, JNI/HotSpot symbols or the existing DAG command
bindings. Their installed M3JDK21 image qualification is still pending.

Receipts bind identities, parameters, source coordinates, implementation
resources and outputs as appropriate to their API. `sourceReceipt` also checks
that a DAG's input frame matches the supplied `M3File`. Hashes are integrity
evidence, not signatures or formal correctness proofs. `replay` and
`verifyLiteral` perform explicit re-execution; retain the associated tests and
independent algorithm oracles. No recipe or image count proves the broad
catalogue's SAT/SMT, LP/ILP, consensus, temporal-logic or media contracts.

## Reproduction evidence and recipe ownership

Successful build output includes modular JARs, twenty JMODs, the linked image,
`SOURCE_INPUTS.tsv`, `TOOLCHAIN_INPUTS.tsv`, `COMMANDS.json`, all command logs,
`MODULE_ARTIFACTS.json` and `BUILD_RECEIPT.json`. JAR dates are fixed. Actual
JMOD hashes identify that execution; byte-for-byte JMOD reproducibility is
not claimed. The build runs the three independent CPU/source/donor fixtures and original
text/JNI smoke test, two additional M3String-storage-enabled fixture passes,
module readback and repeated CLI output checks. The exact generated candidate
must produce these receipts; missing or failed evidence leaves the gate pending. Toolchain seals
include `release`, `lib/modules`, `lib/server/libjvm.so`, invoked JDK executables
and the source JDK's JMODs; their hashes are checked again after qualification.
This identifies the actual runtime artifacts beyond the version strings in
the twenty-tool-module implementation digest.

Canonical recipe owner:
`hsoliwal/com.synexia/synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018`.
Recipe identity: `com.synexia.m3jdk.CpuToolsJmod20261018`. It uses the existing
`M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe` engines.
Correct the canonical payload, reseal inputs, rerun the recipe and qualify its
results; do not hand-edit generated target sources. The task starts from target
baseline `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`.

The task-local [TASK_PACKET.json](TASK_PACKET.json), [PROVENANCE.tsv](PROVENANCE.tsv),
[VERIFY_CONTRACT.tsv](VERIFY_CONTRACT.tsv) and [OUTPUT_CONTRACT.tsv](OUTPUT_CONTRACT.tsv)
describe scope and evidence obligations. The additive naming fragment is
`m3/docs/name-mapping.d/m3-cpu-tools-jmod-20261018.json`. Generated task evidence is published under
`m3/evidence/cpu-tools-jmod-20261018/`; it records actual command outcomes
without rewriting sealed source contracts. Existing global lineage,
historical recipes/receipts and per-file OpenJDK/third-party licensing remain
their own authorities.
