<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 AST: 100 MB real-JSON acceptance lane

**This is a test, not a performance result.** The Java 21 Jackson JMOD
receiver from M3JDK21 PR #1289 is the prerequisite. The stress test is
opt-in because it creates and reads a 100 MiB file; it is not run by the
ordinary staged contract lane.

## What constitutes passing

- Read a full JSON file of **at least 100,000,000 bytes**. The bundled
  deterministic fixture is **exactly 104,857,600 bytes (100 MiB)**.
- Jackson remains responsible for parsing, escaping and Unicode decoding.
  M3 AST must capture all its tokens, field occurrences and ordered child
  references in the sealed primitive-array/offset representation.
- The parser may allocate transient token strings and one frame per
  nesting level. It must not construct Java AST nodes or Map.Entry objects
  for each JSON occurrence. M3ASTAtom is constructed only when explicitly
  requested by its public API.
- Stream the entire AST back through Jackson without a whole-file String,
  then compare the **independent original and output parser token streams**,
  including duplicate field names in original order, unescaped scalar
  values and original numeric lexemes. Do not compare only final JsonNode
  maps: they discard duplicate-field occurrences.
- Check indexed random access without creating M3ASTAtom wrappers. Record
  real byte counts, atom/child count, time, throughput, heap and GC. There
  is no PASS if quota refusal, an OOM or a mismatch occurs.

## Run

Complete the original pack's build.py **lint -> compile -> test -> runtime**
process in a fresh OUT directory according to its README; use the same
checkout and source epoch for all four stages.

Generate fixture outside the repository; its SHA-256 is printed:

~~~sh
python3 m3/tooling/module-packs/jackson/bench/generate_json_100m.py /scratch/m3-100m.json
~~~

From the repository root on a POSIX Java 21 host with an already qualified
pack run (change OUT to the exact existing staged output directory):

~~~sh
OUT=/scratch/jackson-qualified
JDK=/absolute/path/to/jdk-21
"$JDK/bin/java" -Xms1g -Xmx4g \
  --module-path "$OUT/donor-jars:$OUT/modules" \
  --add-modules jdk.m3.jackson \
  -cp "$OUT/test-classes" \
  jdk.m3.jackson.tests.M3Json100MiBAcceptance \
  /scratch/m3-100m.json /scratch/m3-100m-roundtrip.json
~~~

PowerShell uses a semicolon, rather than a colon, to separate module-path
entries. Replace input with an independently obtained **real 100 MB JSON**
for the second corpus; do not reuse a fixture round-trip as the original.

Capture console output, process exit status, JVM build/version, command
line, environment, source commit, input SHA-256, input byte count and heap
settings in immutable evidence. M3_JSON_100M_PASS is emitted only after
complete independent token-stream validation. The pool peak metric is
a **sum of per-pool peak values (upper bound)**, not an exact single-instant
process peak. Use JFR / OS process monitoring to measure allocated bytes and
resident high-water memory; the program does not pretend a cumulative GC
count is an allocation measurement.

This explicit 100 MiB profile is a budget **for this workload**, not a
promise that every possible 100 MiB JSON shape fits a 4 GiB heap. Compare
normal, unique-string-heavy, high-fan-out, and deeply nested corpora and
run at several -Xmx limits before claiming bounded memory.

## Why this implementation is structured this way

- M3ObjectMapper.readASTStreaming(Path, Charset, long) decodes malformed
  UTF-8 strictly and enforces a streaming byte cap. It does **not** retain
  a M3File whole-source snapshot or invent an unrecorded byte hash.
- M3ASTPool uses flat primitive atom and ordered-child arenas, and its
  construction-only String index is primitive/open-addressed instead of
  a boxed HashMap. Hashes are candidates; equality is exact.
- M3AST.kindAt, nameAt, valueAt, childCountAt and childAtomIndexAt expose
  primitive-indexed access to callers.
- M3JsonCodec uses primitive traversal stacks, not an OutputFrame instance
  per emitted token. Numeric lexemes use the RFC 8259 decimal grammar
  without a second JsonParser instance per number.
- The JSON AST remains an optional receiving projection, not the javac
  compiler AST and not a claim of unified source-coordinate owners.
  Canonical shared algorithms/recipes belong in Synexia; M3JDK21 is the
  target runtime owner.

## Qualification still required

Run the original strict Java 21 compiler and staged tests after this source
change, rerun the source-sealed recipe on the new preimage to a zero-diff
fixed point, record provenance and source hashes, and execute the 100 MiB
acceptance class on a matched M3JDK21 runtime. Then use several genuine
JSON datasets, malformed/truncated input, unique scalars, high fan-out,
UTF-8 edge cases and deep-but-bounded nesting for differential tests.
No throughput, memory or runtime integration claim is established without
those receipts.
