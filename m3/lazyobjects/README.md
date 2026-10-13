# Parent-owned transient objects: experimental runtime adapter

This additive Java 21 slice supplies stable *logical handles*, per-construction bit flags,
lazy factory creation, LRU eviction, optional codec-based spill to disk, recreation,
and explicit pin leases. Source: `m3/lazyobjects/src/main/java/org/m3/runtime/lazy/LazyObjectParent.java`.

## Contract

* `LAZY | RECREATE`: cold values are discarded and the factory reruns on demand.
* `LAZY | SWAP`: cold values are serialized by the caller-provided codec and restored on demand.
* `pin()`: protects the resident object until the lease closes; a raw `get()` value is
  **not** protected from subsequent eviction. A caller retaining references must pin.
* `evictUpTo(n)` is the host's **pressure callback**; no fictitious automatic
  JVM low-memory hook is claimed here.
* The parent serializes lifecycle operations. It does not preserve raw Java object
  identity through eviction, and it does not intercept ordinary field/array accesses.
* Values implementing `AutoCloseable` are closed on eviction. Do not use this for
  SWT widgets, Swing UI objects, JNI handles, or live resources without a
  thread-affine lifecycle adapter and explicit semantic tests.
* Stored pages are process-local scratch data, not a durable store. Treat the
  scratch directory as trusted and delete it on teardown.

## Further implementation gates

1. Compiler annotation/metadata pass: capture proven escape, ownership, lifetime,
   native attachment, construction flags, and independently modeled retention counts.
   Static escape analysis cannot establish universal runtime reference counts.
2. SWT/JFace TreeViewer virtualization: visible window + configurable overscan;
   parent-owned row descriptors and bit flags; UI-thread-only widget materialization.
3. Swing adapter: EDT-only attach/detach and model/view identity constraints.
4. JNI backend: owned native buffers, explicit pinning, bounded serialization and
   resource reclamation. No swapping arbitrary Java object graphs.
5. Memory policy: budgeted resident bytes, access counters, dirty tracking, page
   integrity, per-parent pressure sampling, failure injection, telemetry, and
   reference oracle / regression testing.
6. Recipes: JDT/OpenRewrite opt-in conversion only after inventory, API/ABI comparison,
   escape analysis, deterministic tests, and measured convergence.

**Status:** proof-of-concept runtime core; not an integrated JVM or SWT optimization.
