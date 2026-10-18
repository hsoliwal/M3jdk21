# M3JDK21 utility families — coverage-first naming

Status: **reviewed family-name inventory; one initial internal implementation**. The table
[`m3-utility-families.tsv`](m3-utility-families.tsv) lists candidate entry points, exact
JDK API areas, implementation owners, and unclosed contract gates. A name in the table
is not a Java class until the recorded source path exists and its contract is qualified.

## The rule

Every meaningful JDK capability/API family gets one discoverable M3-prefixed utility
entry point **where it adds real M3 operations**, e.g. `M3Objects`, `M3Strings`,
`M3Arrays`, `M3Collections`, `M3Streams`, `M3Reflection`, `M3IO`, `M3Files`.
Use **`M3` + established JDK noun/family name** instead of proliferating
`*Manager`, `*Helper`, `*Facade`, `*Service`, `*Impl` and `*Utils` synonyms.
Do not create a dummy utility merely to claim naming coverage.

This rule covers the JDK product, **not a parallel "M3 + JDK" library**:

- Public `java.*`/`javax.*` APIs retain their contract, names, identity, serialized forms,
  module descriptors and VM integration. M3 internal kernels/backends can be installed
  behind those APIs only after JDK-specific acceptance.
- Reuse existing target owners before adding a utility. `M3Arrays` and
  `M3Collections` already exist in independently qualified modules; this work does not
  create aliases or claim they have been installed as the ordinary JDK backing.
- Distinguish static/entry-point vocabulary from storage identity: `M3Strings` is a
  proposed collection of composite-aware text operations; `java.lang.M3String` is the
  canonical internal String representation. The plural utility cannot own another
  string payload or flatten compositions by default.
- Locate a cross-cutting utility beside its **semantic owner**. Source under `src/**`
  must honor the OpenJDK module/license boundary; opt-in proving-ground modules under
  `m3/**` retain their independent custody until explicitly accepted.
- Represent mutable object state in indexed flags/masks, with logical-to-physical
  offsets/indices and contiguous primitive arrays. Row/entry objects are materialized
  **only where an API requests them**, and owner/generation/GC-lifetime rules still apply.
- Precomputation, native kernels, spill and SIMD are **private strategies** chosen by
  exact contract and benchmark, not new public APIs or universal speedup claims.
- Algorithm donor discovery is by problem category. Preserve provenance and licensing;
  catalogue entries from LeetCode, HackerRank and GeeksforGeeks are problem evidence
  unless source-copy rights are independently established.

## Initial concrete M3IO implementation

`src/java.base/share/classes/jdk/internal/m3/io/M3IO.java` provides:

1. `transfer(in, out, borrowedScratch)` for bounded stream copying with no
   internally allocated byte buffer and explicit zero-progress fallback.
2. `readExactly(in, destination, offset, length)` for checked range filling and
   precise EOF handling.
3. `writeMasked(out, pages, offsets, lengths, activeWords)` for a primitive-array
   state-mask page walk without per-page entry objects.
4. `writeProjected(out, pages, offsets, lengths, logicalToPhysical, from, to,
   activeWords)` for logical ordering over physical slots with a bitmask selection.

All offsets and selected active page ranges are validated before writing anything.
Page and metadata arrays are *borrowed* and must not change concurrently. Ordinary
I/O failure can produce partial output. Streams remain caller-owned. This is an
**internal java.base utility**, not a public Java I/O contract replacement, a
precomputed filesystem, an `M3File` implementation or a JNI optimization.
`M3Files`, NIO channels and M3File integration have separate catalogue entries.

## Admission and ongoing coverage

`python3 m3/tooling/verify_m3_utility_families.py` rejects duplicate/unsorted
families, invalid modules/states, fake implemented statuses, wrong paths, or an
unresolved initial seed family. With `--missing-modules`, it also shows JDK modules
not yet represented by any utility row; those are a *gap report*, not spurious
success. A module may require several utilities, and some internal modules may
not justify a public-named utility at all.

Real implementation promotion requires per-API contract inventory, JDK tests,
compiler/HotSpot/native consumers where applicable, Java fallback, memory/JNI safety,
CPU/heap measurements, and the existing Synexia-owned recipe-first replay:
**inventory → dated additive convergence → donor problem catalogue → Inline →
Atomize → Patternize → Comment → compile/test gates → reviewable PR**.
The target-only mapping does not fork reusable Synexia recipes. This initial PR
is not proof of whole-JDK equivalence, a full OpenJDK image build, jtreg execution,
JNI equivalence, or source-sealed OpenRewrite admission.

The authoritative migration name registry remains `m3/docs/name-mapping.json` and its
reviewed fragments; the TSV is an implementer checklist, not a competing ABI registry.
