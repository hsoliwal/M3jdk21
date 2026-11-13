# Proper-name UTF-8 comparator qualification (R234)

The existing M3InstanceIndexPrecompute prototype encoded both String operands on every TreeMap comparison. The qualified private comparator compares Unicode scalar prefixes directly and retains the entire encoded-byte comparator for encountered malformed UTF-16 or null operands. No public method, field, stored ID, normalization rule, collection owner or dependency changes.

The donor approach is Apache Lucene CharsRef at commit 17dee71932c683e345508113523e764c3e4c80fa. Original source, LICENSE and NOTICE are in donor/. The qualification-only adapter compiles its extracted comparator with Java21 Arrays.mismatch. It is an oracle for valid Unicode signs only. Actual String.getBytes(UTF_8) is the malformed-text oracle.

## Executed serial recipe flow

1. The four pinned incumbent recipe-engine sources apply m3-instance-utf8-owner-admission to the canonical engine itself. Only one exact M3InstanceIndexPrecompute.java path case is added. Eight JUnit checks cover replay, fixed point, missing/duplicate/wrong-AST/drift/post-scan mutation refusal, frozen atom and adjacent owner refusal.
2. The actual emitted engine afterimage applies m3-instance-utf8-scalar-order to the actual prototype source. The same eight recipe checks pass.
3. The actual emitted product source is compiled with Java21 --release21 -proc:none -g:none -Xlint:all -Werror and exercised before/after/after/before in four fresh JVMs. Fourteen JUnit methods per JVM include both unchanged incumbent main programs (25 +59 internal checks each invocation). Total: 16 recipe +56 product =72 JUnit executions, plus the internal main-program assertions. Counts are not combined as if main-program assertions were JUnit tests.

Every JVM covers all 65,536 UTF16 code units (786,432 anchor/prefix pairs), all 1,112,064 valid Unicode scalars, 100,000 seeded pairs, malformed queries, supplementary characters, prefix and sorting laws, null operand order, public raw map and metadata behavior, natural UTF16 reverse sets, ASCII normalization, ownership/immutability/concurrency and unchanged field/public/non-comparator method fingerprints. Instrumentation of the compiled method observes zero getBytes calls for the tested valid pairs after the change, two before; malformed fallback remains two; null operand call counts remain 1/1/2. Timings use uninstrumented class bytes.

## Measurements

Each process records 84 samples (336 total): three corpora, compare/raw lookup/normalized lookup (200,000 calls/sample) and construction (4,096 records/sample), seven samples each. -Xbatch and CICompilerCount=2 are fixed. Values below are medians across two processes per side. Allocation uses ThreadMXBean. These are local Java21 Windows measurements, not a cross-platform or universal performance guarantee.

| Operation/corpus | Before ns/call | After ns/call | Bytes/call |
| --- | ---: | ---: | ---: |
| compare/ascii | 177.9 | 126.9 | 64.00 → 0.00 |
| compare/multilingual | 838.9 | 268.7 | 296.09 → 0.00 |
| compare/supplementary | 593.9 | 163.0 | 207.44 → 0.00 |
| raw-lookup/ascii | 2495.0 | 2070.8 | 815.54 → 0.00 |
| raw-lookup/multilingual | 10449.2 | 3920.6 | 3775.72 → 0.00 |
| raw-lookup/supplementary | 7305.2 | 2534.9 | 2636.29 → 0.00 |
| normalized-lookup/ascii | 2899.9 | 2560.7 | 1039.54 → 224.00 |
| normalized-lookup/multilingual | 11322.8 | 4939.1 | 4103.22 → 327.51 |
| normalized-lookup/supplementary | 7889.7 | 3276.0 | 2924.25 → 287.96 |
| construct/ascii | 36681150.0 | 27273600.0 | 11468632.00 → 2327256.00 |
| construct/multilingual | 127906450.0 | 49544250.0 | 43440344.00 → 3171032.00 |
| construct/supplementary | 75783550.0 | 33432900.0 | 31285368.00 → 2851032.00 |

Construction time remains dominated by existing parsing, validation and maps as well as comparisons. Normalized lookups retain their existing normalization allocation. Lookup complexity and TreeMap storage are unchanged.

## Contract discoveries and failures retained

Public record and metadata maps use unsigned UTF8 ordering and expose raw malformed-query replacement aliases. Reverse-name snapshot sets use UTF16 natural ordering because the incumbent constructs TreeSet from a Set-typed argument. Both behaviors are preserved. The first QA receipt records an incorrect reverse-order test model; the second records comparison of VM-generated implicit NPE messages across different call sites. Generic public-view checks compare exception classes; direct details and exact operand encoding order remain recorded. Java21 permits implementation-specific or null implicit NPE detail. The production candidate and registry patch never changed during those test-oracle corrections. Prior failed receipts are retained under prior/ and not counted as successful executions.

## Reproduction and admission

qualification.py is the exact executed Maven/RAM runner; stdin-qualified.py pins the exact input blob b99e38e806895a7c968cc34c90a89bd6fcf9718a. qualification-input.references.json restores that input by resolving crate-resource-utf8 entries relative to this directory and git-blob-utf8 entries from the two unchanged incumbent-tests files with matching Git blob IDs. Serialize compact ASCII JSON, preserving insertion order, with no final LF; verify the Git blob identity before sending its hex lines to the loader. bootstrap/ contains the complete four-source incumbent engine, the existing RAM compiler and POM; recipe-test/, proof-src/ and donor-oracle/ contain all additional source. No private Synexia source fetch is required by the M3 mirror.

The executed deployment is deliberately host-bound: Java21, Maven3.9.14, 47 exact SHA256-pinned offline JARs and the installed read-only Snappy library are inputs. The POM's legacy default exec main is overridden by qualification.py to the RAM compiler. Reuse the provisioned offline cache rather than downloading or copying it. First verify the paths/capacity/native temp gates in the runner; an unprovisioned machine must stop rather than silently fall back. This is a source-complete qualification slice, not a claim of a portable one-command JDK build.

Canonical plugin resource copies of both manifests/snapshots are packaged with individual YAML recipes. Run the owner-admission recipe against the Synexia engine preimage; then use the qualified engine to run the scalar-order recipe against the M3 source preimage. The canonical Synexia file and M3 receiver file in this delivery are exactly the qualified emitted afterimages. Repeated application is a fixed point; drift refuses. To roll back, revert the scoped PR. Do not widen source admission or patch generated afterimages manually.

qualification-proof.json is the authoritative compressed executed receipt (SHA256 95f657eb01046545d670dcda2f12f973920e65af0c2b49ed3ec1d07842d892e6); qualification-summary.json and ADMISSION_DECISION.json describe selection. FOSS_REUSE_DECISION.tsv and ALGORITHM_CATALOGUE.tsv preserve donor/category decisions. New authored artifacts: Copyright 2026 Hitesh Soliwal, Synexia, Apache-2.0; upstream notices retained.

## Limits and storage

This is one private comparator in an existing target-side prototype, not full lexicon migration, new dataset admission, full JDK image/reactor validation or JNI/ARM/Android/macOS/ESP/Pico qualification. No native build was attempted; the separate ABI hold remains. No language-domain or Unicode metadata policy change is implied.

The approved S route and 545MiB total task cap remain in effect. Build/measurement artifacts stayed in RAM; S retained zero files before/after. Read-only F is charged 571469824 bytes from the sealed R230 baseline, not a fresh full audit (the earlier audit timed out). The 8GiB free-space reserve was enforced and all six child stages exited zero and were reaped. There were no new clones, snapshots, retained local build outputs or cache copies.
