# JEP 454 Foreign Function & Memory API — Java 21 compatibility inventory

Status: dependency/high-risk inventory only. No product source mutation or compatibility promotion.

## Authority

- JEP: 454 — Foreign Function & Memory API
- JDK 21 third-preview implementation: `cbccc4c8172797ea2f1b7c301d00add3f517546d`
  - JDK-8304265 — Implementation of Foreign Function and Memory API (Third Preview)
- JDK 22 final implementation: `32ac72c3d35138f5253e4defc948304ac3ea1b53`
  - JDK-8312522 — Implementation of Foreign Function & Memory API
  - parent: `9728e21db1b35e487c562690de659aac386aa99d`
- finalization denominator: 261 touched paths.

## M3JDK21 compatibility classification

This is **not** a mechanical preview-marker removal.

The final JDK22 API contains source, binary and behavioral compatibility changes relative to the
JDK21 preview API. M3JDK21 therefore classifies JEP 454 as:

- default Java21: NO;
- classification: OPT_IN_SE_API_EXTENSION;
- edit scope: LIBRARY_API;
- risk: HIGH;
- source materialized: false;
- mutation authority: false;
- promotion: NOT_AUTHORIZED.

The stock Java 21 preview-era FFM contract must not be silently replaced.

## Required planes

1. PUBLIC_FOREIGN_API
2. INTERNAL_FOREIGN_RUNTIME
3. ABI_LINKERS
4. NATIVE_FALLBACK_LINKER
5. METHOD_HANDLE_VAR_HANDLE_INTEGRATION
6. MODULE_PREVIEW_NATIVE_ACCESS_POLICY
7. VECTOR_AND_OTHER_CONSUMERS
8. FOREIGN_JTREG
9. NATIVE_ACCESS_MANIFEST_TESTS
10. MICROBENCH_EVIDENCE

Each plane must close its Java21 dependency and compatibility split before a source-sealed recipe
may be generated.

## Finalization deltas that require explicit review

The final API work includes, among other changes:

- broader native-string charset support;
- removal of one-argument `MemoryLayout.sequenceLayout`;
- `Linker.canonicalLayouts`;
- layout-derived handles gain a base-offset coordinate;
- removal of `MemorySegment.segmentOffset`;
- allocator initializer methods renamed to `allocateFrom`;
- variadic documentation changes;
- linker availability becomes required;
- final API is no longer preview;
- `Linker.Option.isTrivial` becomes `critical`;
- `critical` is incompatible with `captureCallState`;
- `Arena.allocate` becomes abstract;
- unsupported access modes explicitly throw;
- arbitrary segment `allocateFrom` support;
- `Enable-Native-Access` manifest attribute support.

These deltas are contract changes, not FILE-local refactors.

## Admission order

```text
inventory
 -> exact JDK21-preview/JDK22-final path split
 -> API delta ledger
 -> current-tree preimage inventory
 -> dependency closure by plane
 -> FILE atoms only where behavior/contracts are local
 -> PACKAGE/MODULE reconciliation
 -> LIBRARY_API opt-in contract review
 -> full JDK build/jtreg/native-access proof
 -> fixed point
```

Until that sequence is green, JEP454 remains research/inventory evidence only.
