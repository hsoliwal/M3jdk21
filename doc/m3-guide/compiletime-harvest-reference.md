<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Constant-regex harvest and differential receipt contracts

This reference covers the complete bodies of `M3ConstantRegexHarvester` and
`M3ConstantRegexDifferential` at [public source pin
`8758991645037126985d56f16a208fec8a1eef38`](https://github.com/hsoliwal/M3jdk21/tree/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime).
The [inventory reference](compiletime-inventory-reference.md) owns the scanner,
constant-operand and TSV contracts; the [analysis/image/gate reference](compiletime-regex-reference.md)
owns regex semantics, layout, admission, retained buffers and gate scratch.
The [19-owner map](compiletime-source-coverage.md) keeps their boundaries separate.

**Verification: NOT_RUN.** These command-line tools, examples, tests, benchmarks,
recipe and custody scripts were not executed for this documentation. The
[design's 2026-10-13 results](../../m3/docs/m3-compiletime-precompute.md#10-first-vertical-leaf-constant-regex-proven)
are historical finite-corpus results, not results of this source review or a
new guarantee about another JDK, input set or deployment.

## Standalone build and invocation boundary

Both public final classes have private constructors and public
`main(String[] args) throws IOException`. They belong to the standalone
`com.m3:m3-compiletime-precompute:0.1.0-SNAPSHOT` Java-21 classpath project,
which depends on ASM/ASM tree/ASM analysis and the separate `m3-precompute`
artifact. The [POM](../../m3/ports/compiletime/pom.xml) has no parent/modules
and Surefire uses `useModulePath=false`. No `module-info.java` occurs in the
complete project tree, and the project is not part of the
[root control reactor's module/profile module lists](../../m3/pom.xml).

The [existing README](../../m3/ports/compiletime/README.md) gives the explicit
precompute-install, compiletime-test and dependency-classpath build order.
The reflective analyzer needs
`--add-opens java.base/java.util.regex=ALL-UNNAMED` for useful node-tree
classification. This applies to harvest and also to the differential driver's
fresh oracle-image analysis, even though the latter's short source-header
invocation omits it. If the view cannot initialize, analyses refuse and the
gates can become passthrough; finite stock equality with no useful hits would
not prove optimization coverage. Inspect hit/refusal/pruning metrics too.

Neither main is a strict CLI parser: the shared `arg` helper takes the first
exact option followed by a value, otherwise the fallback; unknown options and
trailing valueless options are not rejected as a complete grammar. Numeric
parsing, paths, resource allocation and unchecked failures can propagate in
addition to the declared IOException. These tools assume controlled build
inputs, not hostile CLI or image/TSV streams.

## Harvester inputs and exact versus sampled work

[`M3ConstantRegexHarvester`](../../m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexHarvester.java)
accepts:

| Option | Default | Meaning |
| --- | --- | --- |
| `--out DIR` | `target/constant-regex` | Create directory and write fixed output names |
| `--jdk` | Absent | Include executing JVM's `jrt:/` module classes |
| `--jars LIST` | No list | Read one jar path per nonblank trimmed line; the scanner sorts paths; comment lines are not a separate syntax |
| `--sample-mod N` | `128` | Keep sampled distinct ldc strings when floorMod of their h2 content hash by N is zero |
| `--rounds N` | `101` | Number of timing repetitions per retained key |

The tool creates the output directory before later argument and input
validation. It instantiates `M3InventoryState` and
`M3CompileTimeStringInventory(state, null, true)`: no per-unit inventory TSV
writer, regex-only call-site recording. The scanner still collects ldc
strings needed for the sample. It scans JDK first when requested, then jars;
jar lists are not deduplicated. Module descriptors and multi-release jar
entries have the scanner's documented exclusions. Runtime class-parse
failures are counted/logged by the scanner rather than necessarily stopping
the entire run; harvest does not automatically copy all those failure counters
into its own receipt. Consult the inventory contract/logs before interpreting
an input-set manifest as complete successful parsing.

The distinct regex-key table is a TreeMap keyed by pattern text, NUL and
flags. It holds exact retained discoveries under the scanner's constant-proof
rules, not every runtime regex in the application. Split fastpath cases and
nonconstant flags have their own handling in `M3SiteRecorder`; they need not
become image entries. Keys are harvested without probabilistic sampling.

The **input strings** are a separate deterministic sample of distinct ldc
spellings, restricted to at most 256 UTF-16 units. `inputSample` applies
`Math.floorMod(M3ContentKey.of(s, 0).h2(), mod) == 0`, sorts the retained
Strings and writes escaped lines. This is not every Nth item of a sorted list,
not random sampling of observed runtime inputs and not a representative-load
guarantee. Choose a positive modulus and positive rounds; no explicit range
validation enforces that choice. Zero modulus can throw when evaluated;
nonpositive rounds can fail allocation/median indexing or produce degenerate
empty-key behavior.

## Harvester outputs and self-check

Harvester writes **five** files. The differential driver later writes the
sixth receipt file. There is no transactional staging, atomic directory swap,
rollback or cleanup of stale outputs. Existing fixed-name files can be
truncated/replaced; a later error can leave a mixture of new, partial and old
files. Output order is manifest, image, image self-check, key table, input
sample, then final harvest receipt. An image file is already written before
the self-check succeeds.

| File | Exact schema / content |
| --- | --- |
| `INPUT_SET_MANIFEST.tsv` | First line `M3CTRX01_INPUT_SET`; optional `jrt<TAB>java.runtime.version`; sorted nonblank jar lines `jar<TAB>file_sha256_hex<TAB>basename` |
| `constant-regex.m3ctrx` | Image of the ordered distinct keys with their stored analyses; input-set SHA is computed from the manifest String |
| `CONSTANT_REGEX_IMAGE_KEYS.tsv` | Ten-column header described below |
| `INPUT_SAMPLE.tsv` | No header; one `M3Tsv.escape`d sampled String per line, sorted before escaping |
| `HARVEST_RECEIPT.tsv` | `metric<TAB>value` header; sorted numeric metric rows followed by four metadata rows |

The key table's complete ordered header is:

`pattern`, `flags`, `kind`, `matcher_state_admitted`, `atoms`, `jdk21_sites`,
`vendor_sites`, `stock_compile_median_ns`, `compile_plus_walk_median_ns`, `apis`.

Pattern cells use reversible `M3Tsv.escape`; flags/counts/timings are decimal,
kind is the enum name, admission is boolean text, and APIs are the key's sorted
set joined by commas. `atoms` counts formula literal occurrences, not global
unique atoms. This is an output schema, not a validated arbitrary TSV import
format. The differential reader consumes only the cells needed for its work.

The manifest's JDK line is the runtime-version text; it is not a hash of all
JDK module bytes. Each jar is hashed in full after scanning, with a 64-KiB
read buffer, and recorded by basename rather than full path. Duplicate input
paths remain duplicate manifest rows. The file hashing and scan are separate
reads, so stable inputs throughout the run are a caller requirement. Tabs or
newlines in filenames are not separately escaped in the manifest.

`selfCheck` verifies the produced byte array through the reader and looks up
every retained key. It counts a success only when kind and decoded atom-array
length equal the stored analysis kind and requirement atomCount. A count
mismatch throws `IllegalStateException`; otherwise
`image_self_check_keys` is recorded. It does **not** compare every opcode,
atom spelling, trigram, admission bit or matching result. The image reader's
admission limitations still apply.

### Build-time cost and receipt fields

Private `measure/compileQuietly/median` execute separate stock-compile and
compile-plus-analysis passes per round, timing each key with nanoTime.
Invalid keys' stock IllegalArgumentException is swallowed so the failure cost
can be measured. Each per-key column is the sorted upper-middle sample at
index `rounds / 2`; it is wall time, not per-key thread CPU. There is no
separate explicit warm-up stage in this method.

The entire stock pass and analysis pass are bracketed by ThreadMXBean CPU
reads. The receipt's means divide aggregate deltas by keys × rounds; zero
observations produce mean zero. Platform CPU-clock support/enablement and
clock granularity are not validated by this method. These counters should
not be presented as universally available precise per-operation measurements.
Analysis timing includes stock compilation again; subtracting medians is not
an independently measured walk-only distribution.

The sorted numeric metric keys are:

- `harvest_scan_thread_cpu_ns`, `harvest_scan_wall_ns`, `harvest_classes`
- `measure_rounds`, `measure_total_thread_cpu_ns_stock_compile`,
  `measure_total_thread_cpu_ns_compile_plus_walk`,
  `measure_mean_thread_cpu_ns_per_key_stock_compile`,
  `measure_mean_thread_cpu_ns_per_key_compile_plus_walk`
- `image_self_check_keys`, `image_bytes`, `input_sample_strings`, `input_sample_mod`
- `keys_PURE_LITERAL`, `keys_REQUIRED`, `keys_UNCONSTRAINED`, `keys_REFUSED`,
  `keys_INVALID`, `keys_total`, `keys_matcher_state_admitted`

The final metadata rows, in source order, are `image_sha256` (whole image
including its trailer), `input_set_sha256`, `producer_pattern_class_sha256`,
and `producer_java_runtime_version`. The image's internal trailer hashes only
the bytes before the trailer. These are distinct digest scopes. Package-private
`sha256Hex(Path)` streams a file and `hexInts(int[])` renders eight-hex-digit
words; private `sha256(byte[])` hashes the image. Hashes identify content,
not successful parsing, authenticity or runtime installation.

## Differential inputs and comparison domains

[`M3ConstantRegexDifferential`](../../m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexDifferential.java)
accepts `--harvest DIR` (default `target/constant-regex`), `--rounds N` (5)
and `--reps N` (8). Reps is mutable static state assigned by main. This CLI
is intended as one serial run, not concurrent invocations inside one JVM.
Static initialization casts the platform bean to
`com.sun.management.ThreadMXBean`; there is no portability fallback or check
that allocation/CPU counters are supported/enabled.

It opens the image, reads/unescapes every INPUT_SAMPLE line, reads the key
TSV, drops its first line, skips rows whose third cell is exactly `INVALID`,
parses flags and compiles each remaining pattern into a gate. It does not
validate the complete header, column count, kind vocabulary, row uniqueness,
expected manifest digest or table/sample binding to the image/harvest receipt.
The key-table implementation reads the file twice while constructing its
sublist; keep files stable. Header-only/empty/malformed files and stale kind
claims can fail or produce incomplete gate sets. It is not a general-purpose
hostile TSV importer.

| Comparison domain | Operations and scope |
| --- | --- |
| Harvested valid keys × sampled Strings | Compare stock first `find`, `matches`, replaceAll with the fixed replacement U+0001, and default-limit split; record per-operation mismatch counts |
| Harvested hit pairs | Count pairs whose gate hit; separately count hit pairs where holds is false as pruned |
| Fresh image built from distinct valid frozen-oracle expressions, flags zero | Harness candidate callback says CANNOT_MATCH only for a hit with false holds; otherwise MAY_MATCH; checks finite pruning contradictions |
| Same oracle expressions × harness inputs | Additional pairwise gate find/matches comparisons with cached harness values; no replaceAll or split comparison in this oracle loop |

The two domains use **different images**. The oracle image is built in memory
from its own unique valid expressions and an input digest derived from
`harness.oracleRoot()`, not the harvested jar manifest. The candidate interface
is a rejection check: MAY_MATCH is not proof of a match. See the separate
[oracle corpus/harness contracts](regex-precompute-reference.md#candidate-checks-and-reports)
for finite roots, sharding, invalid expressions and report limits.

The harvested pass counts one `diff_pairs` per gate/input pair while making
four result comparisons. It sums all four mismatch counts, so this sum is not
necessarily the number of distinct failing pairs. `diff_pruned_pairs` measures
false necessary conditions among hits, not all cases that avoid a Matcher:
pure-literal find/matches can complete directly even when their literal is
present. The default sample omits long and arbitrary future inputs, and fixed
replacement/default split do not qualify all replacement syntax or split-limit
semantics. No captures, regions, bounds, repeated-find state, exception-parity
or concurrent-gate test is added by these loops.

## Differential measurement and receipt schema

`bench` measures four named workloads: `find_hit_keys`, `matches_hit_keys`,
`replace_all_hit_keys`, and `find_all_keys`. Split has a semantic comparison
but no benchmark workload here. Compilation and image decoding occur before
measurement. Each workload warms up with two stock and two gated whole passes,
then records stock followed by gated for every requested round; the order is
not randomized.

Private `pass` repeats the complete gates × inputs sweep `reps` times and
returns the package-private record
`Pass(cpuNs, wallNs, allocatedBytes, calls, trueResults)`. Its fields are deltas
of current-thread counters and nanoTime plus loop counters. The two side
arrays are summarized independently using sorted upper-middle medians. Before
writing workload metrics, `bench` checks only the first measured stock and
gated passes' trueResults equality and throws if it differs. The separate
semantic differential pass is the actual per-pair content comparison.

For replaceAll benchmarking, the boolean is `result != input` by reference
identity, not returned-text equality. That counter is an observable allocation/
identity proxy for this harness; it is not a complete semantic assertion.
There is no independent randomized order, process isolation, GC normalization
or all-round truth-count assertion in bench. Use positive rounds and reps;
rounds zero accesses `a[0]`, and nonpositive reps can give empty work without
establishing useful coverage.

`DIFFERENTIAL_RECEIPT.tsv` has `metric<TAB>value` and a lexically sorted
TreeMap of string-valued rows:

- `image_entries`, `inputs`, `gates_valid_keys`, `gates_hit`
- `diff_pairs`, `diff_mismatch_find`, `diff_mismatch_matches`,
  `diff_mismatch_replace_all`, `diff_mismatch_split`, `diff_hit_pairs`,
  `diff_pruned_pairs`
- For each workload name above and each side `stock` / `gated`:
  `{name}_{side}_cpu_ns_median`, `{name}_{side}_wall_ns_median`,
  `{name}_{side}_alloc_bytes_median`, `{name}_{side}_true_results`
- For each workload: `{name}_calls_per_pass`, `{name}_reps_per_pass`
- `oracle_root`, `oracle_pairs`, `oracle_find_true`, `oracle_pruned`,
  `oracle_unsound`, `oracle_report_root`, `oracle_distinct_valid_keys`,
  `oracle_keys_hit`, `oracle_find_matches_mismatches`
- `total_mismatches`

The total is harvested-operation mismatches plus oracle find/matches
mismatches plus harness unsound-pruning count. Main writes/logs the receipt
and then throws IllegalStateException if this total is nonzero, so the mere
presence of the file is not success. Earlier I/O, analysis, benchmark or
counter failures can stop before a new receipt is written, leaving an older
receipt untouched. The receipt does not itself contain the harvest image
SHA, runtime version, full options or benchmark-round count; associate it with
the correct harvest files and invocation externally.

## Complete declared-member routing

These are command-line owners, not reusable public service interfaces. Their
internal methods are included to make output and measurement behavior traceable.

| Owner | Declared member group | Contract section |
| --- | --- | --- |
| Harvester | Private constructor; constants `DEFAULT_ROUNDS`, `MAX_INPUT_UNITS`; public `main` | [Inputs and work](#harvester-inputs-and-exact-versus-sampled-work) |
| Harvester | `measure`, `compileQuietly`, `median` | [Build-time cost](#build-time-cost-and-receipt-fields) |
| Harvester | `manifest`, package-private `sha256Hex`, `selfCheck`, `keyTable`, `inputSample` | [Outputs/self-check](#harvester-outputs-and-self-check) |
| Harvester | `writeReceipt`, `sha256`, package-private `hexInts` | [Build-time cost/receipt fields](#build-time-cost-and-receipt-fields) |
| Differential | Private constructor; `REPLACEMENT`, `MX`, mutable `reps`; public `main`; package-private `Pass` record/components | [Inputs](#differential-inputs-and-comparison-domains), [measurement](#differential-measurement-and-receipt-schema) |
| Differential | `gates`, `differential`, `oracle` | [Comparison domains](#differential-inputs-and-comparison-domains) |
| Differential | `bench`, `put`, `median`, `pass` | [Measurement/receipt](#differential-measurement-and-receipt-schema) |

## Historical evidence and custody boundaries

The existing [design](../../m3/docs/m3-compiletime-precompute.md#10-first-vertical-leaf-constant-regex-proven)
records a 2026-10-13 Windows 11 / Oracle JDK 21.0.9+7-LTS-338 run on a shared
host, including 556 harvested keys, 2,965 sampled inputs and a separate frozen
10,000 × 64 oracle corpus. Its zero finite mismatch counts, image digest and
measured timings retain that date and scope. They do not qualify all regexes,
future inputs/JDK updates, every Matcher operation, concurrency or a bootstrap
receiver. Nothing on this page reruns or promotes those historical receipts.

The project [custody verifier](../../m3/ports/compiletime/verify_compiletime_precompute.py)
reads recorded receipt cells, checks selected mismatch values equal text `0`,
checks harvest self-check count equality, source/crate/manifest identity,
selected reuse-ledger decisions and provenance tokens. It does not execute
the Java differential pipeline. The [seal script](../../m3/ports/compiletime/seal_crate.py)
first deletes existing `*.java.txt` files in its crate directory, then validates
and copies the source targets and rewrites `manifest.tsv`. The intended copies
are byte-identical to the project Java sources under src. This is neither an
append-only nor an atomic filesystem update: a later validation or I/O failure
can leave a partial crate. The manifest's additive ABSENT rows describe recipe
metadata, not the seal script's filesystem effects. It does not analyze regex
correctness. Both scripts were inspected as source only and remain **NOT_RUN**.

The pinned [crate manifest](../../m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/m3-compiletime-regex-precompute/manifest.tsv)
contains 29 Java targets: 19 compiletime production owners, three residency
annotations and seven test/fixture files. Those copies are not extra source
owners in the 19-row package denominator. The [recipe wrapper](../../m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3CompileTimeRegexPrecomputeRecipe.java)
delegates to the hash-pinned snapshot recipe and explicitly returns false for
both bootstrapPromotionAuthority and semanticAuthority.

Its [two test methods](../../m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/M3CompileTimeRegexPrecomputeRecipeTest.java)
assert materialization/fixed point and the wrapper's no-authority flags; they
were read, **NOT_RUN**, and do not execute harvest/gate semantics. The complete
compiletime project test tree contains four JUnit classes and three fixtures;
there is no dedicated Harvester/Differential test class there. The four
[image tests](../../m3/ports/compiletime/src/test/java/com/m3/compiletime/M3ConstantRegexImageTest.java)
are bounded dependency tests, not end-to-end CLI or receipt-integrity proof.

Public Apache port sources, image data, custody resources and a future GPL+CPE
JDK receiver remain distinct surfaces. This documentation changes no source,
license/provenance input, build profile, recipe authority or runtime admission.

## Pinned source index

| Source owner | Git blob |
| --- | --- |
| [M3ConstantRegexHarvester](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexHarvester.java) | `d2d957060725e06a330fe7097b3b0a477c4e159f` |
| [M3ConstantRegexDifferential](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexDifferential.java) | `60c2a55cf818c809bb5f1dd4a61df2f4e3ffb215` |
