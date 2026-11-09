# M3 JDK internal bulk input binding ownership

This target change is source-sealed by **Synexia PR #11196**, using the
existing M3JDK `M3Jdk21HashPinnedSnapshotRecipe` receiver.

## Observed issue

`BulkScheduler.execute` validated the original mutable input map, called
`BulkExecutorProvider.supports`, and passed that same map into
`BulkExecutorProvider.execute`. During selection a callback holding the
original caller map could replace a validated binding.

An isolated Java 21 regression produced an `AssertionError` against the
original code. The candidate passes with `-Xlint:all -Werror`, verifying
the original object identity is handed to the provider even when its
`supports` callback changes the caller's map.

## Scope

Only the map of **names to values** is copied once at submission.
Large primitive arrays and device-resident backing objects remain shared by
reference; their contents are not defensively deep-copied. The mapping given to
the provider is unmodifiable. Additional null-valued bindings remain tolerated,
as required by the existing JDK internal contract.

This is not cancellation, concurrent mutation of payload contents, or general
task isolation. Callers remain responsible for synchronizing mutable payloads.

## Ownership and gates

- Canonical reusable recipe: Synexia `M3Jdk21BulkInputBindingRecipe`.
- Target installer: `M3BulkInputBindingReceiverRecipe`.
- Runtime owner: `jdk.internal.vm.parallel.BulkScheduler` in `java.base`.
- TornadoVM provider: unchanged; optional backend-specific proof remains required.
- Public Java SE API: unchanged.
- JDK member names/signatures: unchanged.

The two exact target postimages equal their receiver resource Git blobs.
The new workflow runs the receiver, an isolated java.base patch, and
normal/-Xint tests. The existing `M3 Bulk Executor Port` workflow covers
full JDK `make images` and focused jtreg for the same target files.

A recipe dry-run or local patch proof is not full-image promotion. Do not
claim a GPU speedup without real hardware and setup-inclusive measurements.
