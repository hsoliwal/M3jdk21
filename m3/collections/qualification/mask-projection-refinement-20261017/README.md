# M3JDK mask-driven index projections

SPDX-License-Identifier: Apache-2.0
Copyright 2026 Hitesh Soliwal and contributors

This target-specific refinement builds on master d47a4b4fdb8e3794ec56768b06aa9e23730c6b8e.
Read CONTRACT.md. The existing M3LongIndexProjection owns a private primitive payload, ordered
physical IDs, offsets and an immutable order enum. This change does not substitute snapshots
for live M3FilteredViews/M3FilteredLists, add another wrapper facade, or modify java.util.

The full-index filtering path lends immutable indices to the existing nonmutating compactor;
partial views still align indices through a detached range copy. Same-direction re-sorting
uses the existing order certificate: full extents share indices and partial ranges compact once. Opposite ordering
retains the stable index merge because reversing equal-value occurrences is incorrect.

The native emitter stages accepted offsets in its existing emission buffer for each state chunk.
It skips row-ID transfer only when the chunk has no accepted position. A fully accepted chunk
is emitted from the row buffer; mixed chunks use original ordinal order. State bytes remain
unsigned application flags, not object-reference/GC facts. Native selection is still explicit,
including empty inputs; Java callbacks do not enter JNI. Inputs must be stable during a call.

## Reproduce the bounded qualification

Requirements: JDK21, GCC with C17 and sanitizer support, nm, Python3, Maven3.9.9, and a populated
approved offline Maven mirror. The POM pins OpenRewrite8.17.1, compiler3.13.0, Surefire3.2.5,
JUnit5.10.2, ASM9.6 and SLF4J2.0.9. No network resolution or root installation is performed.
The recovered mirror archive SHA-256 is
443e5b400c584b8c29f73b8840bd65f4b7b7f0d92c92916af5be93af1bf84924.
It is not redistributed with this source/proof packet. The exact canonical recipe mirrors and
their Apache-2.0 source identities are recorded in SOURCE_LINEAGE.json; they are authoring
helpers only, with no dependency from runtime classes to com.synexia or OpenRewrite.

```sh
task=m3/collections/qualification/mask-projection-refinement-20261017
python3 "$task/qualify.py" run --root . --proof "$FRESH_PROOF" \
  --maven "$MAVEN" --cache "$OFFLINE_CACHE" --settings "$OFFLINE_SETTINGS"
```

Use a fresh absolute proof directory outside the source root. Source-policy/XML/Python and
strict native syntax lint precede compilation. The named YAML recipe is activated by the
actual OpenRewrite Environment; direct output string copying is not substituted for the SDK.
Recipe qualification precedes receiving. Maven verify requires Java tests and actual JNI.
The native test library contains only this compactor symbol: it is an isolated test binary,
not a replacement distribution of the complete collections native library.

The recipe seals two runtime replacements, two new catalogue/mapping resources, and two KEEP
inputs (the Java compactor and original target self-test). Runtime leaves are materialized
serially from SDK Results, then linted, compiled and tested before the next leaf. The generated
MAPPING.json records existing-name and phase dispositions; no central rename map is silently
edited. SDK application accepts exact before/after images and refuses drift. Source records,
commands, raw stdout/stderr, expected-negative exits and repeated class/header/native digests
are retained. A failed pass must not be overwritten or resumed as if it were successful.

The fixture tests partial before/after states, input order, source/template drift, missing and
duplicate owners, cancellation and code-looking unrelated text. Runtime tests include every
8-bit state/required-mask pair within the deterministic pattern corpus, unsigned-bit7, negative
and duplicate row IDs, chunk tails, signed-long stable ties, slices, repeated/opposite sorts,
identity isolation, explicit missing-native cases and input drift refusal. The unmodified
M3LongIndexProjectionSelfTest remains required. Test-only unsafe offset/order/offset-to-ID mutants
must fail; those source images are never admitted to runtime output. Parent-compiled clients,
public method/field descriptors and deterministic replay are compared separately.

## Evidence and promotion boundary

See evidence/RESULT.json and raw logs in the delivery packet for actual executed scope. Test
cases and assertion counts are not numbers of JUnit methods or unrestricted equivalence proof.
Native transfer counts describe the injected JNI table; real JVM -Xcheck:jni parity is a
separate gate. Compiler stack-byte reports are specific to the tested build. ThreadMXBean
allocation counts only the calling Java thread; local timing medians are not JMH, process RSS,
native heap or a universal speedup. Dense cases may incur extra mask work.

Full m3/collections module/reactor compilation, JDK image/jtreg, strict central inventory and
provenance admission, hosted CI, Windows/macOS, application stress and JMH remain independent
pending gates unless receipts say otherwise. The String-first product phase is not advanced.
Generic maps, mutable live-wrapper contracts and earlier collection experiments are not
claimed completed merely by this bounded primitive index improvement.

The retained-capacity probe rejected the first candidate's tiny sorted-slice index retention.
Read RESOURCE_ADMISSION.md. Final full-extent views reuse the index; partial same-order sorts
copy only their own range, without comparison/merge work. Plain slice() remains zero-copy.
The payload owner is unchanged. Retention and transient allocation are measured separately.

Read NATIVE_ADMISSION.md and SDK_SURFACE_ADMISSION.md. The bitmap candidate is retained as
comparison evidence; the selected ordinal candidate reuses existing native scratch. SDK
MAPPING.json distinguishes actual external API names and existing M3 backings from pending
Apache adapters. It is an inventory, not a claim of complete library compatibility.


The isolated Linux preview is built twice with fixed JAR entry timestamps and compared by
SHA-256. Each prefix runs the original target self-test, mask/native corpus and immutable
sharing probe using only the packaged JARs and library. It contains just the two target
runtime classes and the compactor JNI symbol, not the full collections module. Never replace
an installed full m3_collections_mask library with this isolated test binary.

SDK name custody is emitted in MAPPING.json: sixteen existing-source API/backing records and
one pending Apache Commons MultiValuedMap adapter. Metadata uniqueness and the pending status
are checked by the qualifier; no Guava/Eclipse/Spring/Apache interoperability execution is
claimed by this two-class runtime proof. The canonical task packet is retained in Synexia
under the existing keyed-api recipe family, with a commit/tree receiver receipt at delivery.
