<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 reference counting, DOOP lifetime, and tracing-GC contract

Candidate, 2026-10-16. Synexia canonical recipe: https://github.com/hsoliwal/com.synexia/pull/11912

## Objective

The M3JDK fork can improve source-derived object lifetime, event handling and deferred-object
residency, but ordinary Java heap object reachability remains governed by HotSpot tracing GC
(Serial, Parallel, G1, ZGC and Shenandoah as built/configured). Do not equate a cooperative
owner-side count of zero to JVM unreachability. JVM stack roots, JNI globals, native/VM
roots, reflection, weak/soft/phantom references, monitor semantics and cyclic graphs
cannot be ignored or mechanically replaced by a simple integer counter.

## Implemented bounded receipt

Existing owner `com.m3.collections.M3ObjectResidency` now maintains an `int[] borrowers`
with one primitive count per logical child slot and allocates a generation-safe
`M3SlotAllocator28` plus `M3IntLane28` owner/callback lanes lazily, only when the first
cooperative borrow occurs. `retainProjection(slot, bytes)` returns a single `long` token;
`useProjection(token, callback)` respects the original synchronized residency/escape
semantics; `releaseProjection(token)` balances exactly once and rejects stale/double
release and reentrant release while active. LRU and physical eviction refuse borrowed
slots. No per-event or per-slot Lease wrappers, no Map.Entry allocation, no new JNI symbol.
Logical state remains in parent-owned `parentBits`; ordinary Java object reference identity
is not changed.

`retireUnborrowedProjection(slot)` releases the owner's reference only when all of:
- RECREATE and STRICT_NO_ESCAPE are configured by the caller and are valid for the owner,
- no active callback or outstanding cooperative borrower exists,
- the slot was not externally installed or pinned,
- SWAP_SECONDARY is not configured and current state is RESIDENT.

This makes a projection *eligible* for ordinary JVM collection if nothing else refers to it.
It is not synchronous memory free and does not call `System.gc()`. If the supposed
non-escape contract is unsound, retirement can violate logical reuse expectations;
it must therefore remain opt-in until compiler/contract evidence rules out all
escape paths. `STRICT_NO_ESCAPE` currently detects only direct callback return,
not indirect aliasing or escape through closures, JNI, reflection or fields.

## Compile-time stages: from evidence to admissible optimization

1. **Classify** source and JDK owner: parent-owned transient projection; JNI/native
   resource; ordinary Java heap object; immutable canonical M3 String/primitive data.
2. **Compute conservative facts** from existing Synexia compiler ownership and borrow-use
   analyses. Unknown paths, active callbacks, reflection, unsafe, cloning, serialization,
   JNI and native roots remain UNKNOWN; annotations alone cannot prove nonescape.
3. **Prove** callback lifetime and generated `retain / try-finally release` use
   with attributed AST, bytecode/original behavior, JMM, monitor identity,
   exception paths and multithreaded differential oracles.
4. **Specialize** eligible internal event/DOOP API consumers. Move object-independent
   state to parent primitive lanes; reuse resettable projections only through
   declared handles where Java reference identity is not publicly observed.
5. **Measure** allocations per operation, TLAB refill, p95/p99 latency, compiled
   code size, resident heap, native memory, GC pause/CPU, false sharing and
   counter contention across G1, Parallel, Serial, ZGC and Shenandoah.

## VM-wide reference diagnostics (NOT implemented)

A separate experimental HotSpot service could compute reference *incoming-edge*
summaries at controlled safepoints/GC observation epochs using exact object graph
and root traversal. A snapshot count is not an instantaneous `refCount(Object)`.
A continuously maintained count for all references would need compiler
store/replace barriers, interpreter/C1/C2, stack/root metadata, compressed
oops, moving GC remaps, VarHandles/Unsafe, JNI handles, weak references,
concurrency ordering, cyclic graphs, finalization/resurrection and memory-cost
qualification. Even a correct count cannot replace tracing GC for cycles.
Never embed obligatory per-object counters in normal headers without an
A/B collector, allocation, throughput and footprint proof. Side metadata
may be preferable for a reversible diagnostic experiment.

## JNI boundary

Synexia already implements `M3NativeLeaseCounter` with an actual optional JNI
backend. Native handles count explicit resource leases; JNI GlobalRef creates a
strong GC root and `DeleteGlobalRef` removes that particular root. Java GC
must retain its authoritative heap-reachability semantics. Borrowed Java object
addresses must not be persisted across native calls, compacting GC or
concurrent safepoints.

## Acceptance and limitations

Local isolated test of the borrow-ledger algorithm with JDK21 and test-only
primitive-lane shims: 60,010 checks passed. The real receiver was imported
from the source-sealed Synexia recipe, but **full Maven/OpenRewrite SDK,
M3 collection module JUnit, source-built modified JDK, jtreg, GC collectors,
JIT/interpreter, JMM/JNI, throughput and memory proof are NOT RUN** here.
The current ordered String→arrays→collections receiving discipline is unchanged.

Never report this candidate as "all Java objects now refcounted", "GC replaced",
"fast GC proven", or "arbitrary event reuse safe".
