<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3 CPU tools task report

## Delivered scope

The source candidate composes three existing optional text/file/structured
modules, sixteen selected donor CPU modules and one aggregate module. The
build inventory is exactly twenty optional modules; the current registry has
twenty-two executable operations plus separately exported direct APIs.
`README.md`, `COVERAGE.tsv`, `DONOR_CONTRACTS.md` and `DONOR_MODULES.tsv` describe
those contracts and pinned source identities.

`M3File` is a bounded UTF-8 snapshot adapter with distinct byte and UTF-16
identities. The full MIndexFile token/occurrence/postings plane is outside this
adapter. Public String retains the running M3JDK's storage ownership.
`M3AstTools` consumes actual compiler-owned M3AST callbacks and detaches syntax
facts with source-bound receipts. It does not provide attributed call graphs
or retain live compiler objects.

The canonical recipe source owner is
`synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018` in
`hsoliwal/com.synexia`. The declared target baseline is
`f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`, and the selected donor baseline is
`50f48cbbe11f17fb8dc209bac3812f8940a1e267`. Exact application, fixed-point and
source-drift-refusal results belong to the canonical recipe's own receipts.

## Qualification interpretation

The canonical task's `qualification/source-proof.json`, associated source
logs, donor proof files and `DONOR_QUALIFICATION.md` record observed isolated
source qualification, including execution on the actual M3 runtime and
explicit M3String storage. Those records identify their own tested inputs;
a source pass alone does not certify a generated JMOD/image.

For each generated candidate, `build.py` writes `BUILD_RECEIPT.json`,
`COMMANDS.json`, `SOURCE_INPUTS.tsv`, `TOOLCHAIN_INPUTS.tsv`,
`MODULE_ARTIFACTS.json` and detailed command logs under the explicit fresh
output directory. A missing receipt, failed command or non-PASS build status
leaves that candidate unqualified. This report is a stable description of the
required evidence; it does not substitute for that execution's receipt.

A passing image receipt requires all twenty JAR/JMOD descriptors, linked
module execution, three independent CPU/source/donor fixtures, the original
text/JNI smoke, two additional source/CPU tests with M3 storage enabled,
exact repeated CLI output and source/toolchain hash equality before and after
qualification. Both text-tools and CPU legal notices must survive with exact
bytes in every JMOD and linked module. Toolchain identity includes the actual
`lib/modules`, `lib/server/libjvm.so`, `release`, invoked executables and
source JMODs, beyond the version strings in the runtime implementation digest.

`STATUS.tsv` and `VERIFY_CONTRACT.tsv` list these obligations and evidence
locations. JAR dates are fixed; actual JMOD hashes identify a build without
claiming byte-for-byte JMOD reproducibility. Logical frame/graph limits,
cooperative progress and bounded build commands do not establish a hard
in-process CPU/RSS sandbox or any measured performance improvement.

## Remaining programme

The 110-row broad CPU catalogue is not complete. Task-local `COVERAGE.tsv`
maps all original rows to selected partial capabilities or explicit deferred
scope. Full SAT/SMT, LP/ILP, temporal logic, distributed consensus, durable
transaction, media and other specialized providers require their own
implementation and qualification. Lucene/OpenNLP, model quantization and
distillation, and WebGPU are outside this selected module set.

Full JDK product builds, jtreg, C1/C2/CDS, platform matrices and the ordered
String/arrays/collections/AST/remaining-family acceptance programme retain
their independent gate authorities. This optional JMOD add-on advances none
of those family stages by implication.
