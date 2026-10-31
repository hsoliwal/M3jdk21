# M3JDK21 packed filter refinement — 2026-10-18

M3JDK21 is the product. No M3-over-JDK runtime service, second collection owner,
per-entry structural object, Java comparator/predicate JNI callback, or payload copy
is introduced. Source remains within the existing m3/collections owners; java.base
promotion and the String-first receiving phase are unchanged.

## Frozen behavior
M3IndexedListSnapshot is an immutable occurrence projection over its one Object[]
payload and ordered int[] directory, with from/to offsets. Sorted ordering remains
stable including duplicate equal keys; filter callbacks run once per logical
occurrence in encounter order. Nulls, identities, exception object propagation,
nested slices, physical indexes and lazy membership remain unchanged. It is NOT
a live filter; existing live wrappers remain their own owners and are not replaced.

Only the JAVA filter path may use a packed long[] transient selection bitmap and
an exact int[] result. NATIVE_REQUIRED keeps the existing byte-state ABI, loading,
validation order and failure behavior. No new public/private native signature.
Fresh result identity even for all/none and empty input remains unchanged.

Native compactor second-pass row reads may skip rejected prefixes/chunks, borrowing
the existing mask-window first-match technique. All state checks, count/emit drift
checks, no-callback/no-retention rules, validation and exact detached results stay.
Inputs must remain stable during a call as already required. Native calls still
refuse missing symbols/libraries; no silent Java fallback is added.

## Recipe and qualification
Use unchanged M3GpuToolJoinManifest/M3GpuToolJoinRecipe source-sealed family,
a Maven task crate and complete pre/postimages. Template correction and recipe
replay only; never patch emitted receiver files. Per-file serial validation:
patch -> source lint -> compile -> tests -> actual runtime. Record command logs,
input/output hashes, fixed point, drift/refusal and negative controls. Exact
compiled whole owners, not API stand-ins, supply the finite product proof.
Resource decision must include Java allocation/cpu and native timing/RSS observations.
Retain rejected candidates; do not promote a performance regression silently.

The broader SWT/Nebula history remains pinned reference, NOT copied wholesale into
collections. Existing #219/#245 fixes and unreceived #220/#221/#243/#247 source
work stay independently owned and unmodified. This leaf cannot qualify every
widget, all APIs, jmod packaging, HotSpot, jtreg/JCK, or installed M3JDK21 images.
