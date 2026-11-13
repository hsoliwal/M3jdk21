<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# absorb-common-lazy-b27-20261109

This batch begins convergence of `com.synexia.common.lazy` (synexia-common, develop `5ab05fd05653`) into M3JDK, using
RELOCATE_M3_OWNER v6.3. Only the `lazy.event` subpackage had been received before, as aliases of
`jdk.m3.collections.LazyEvent*`.

## Selection (SEL_b27_v6.tsv)

select_multi over the package found 24 main classes: 5 ELIGIBLE and 19 HOLD. The 5 eligible classes land in
`jdk.internal.m3.collections` (v6_place: root family):
- AlertException
- BatchProcessor
- ICoalescingBuffer
- ISequenceBarrier
- IWaitStrategy

BatchProcessorTest lands with them.

The 19 HOLD classes fall into two groups:
- **lombok `@Slf4j` / slf4j logging.** Lazy, SoftLazy, WeakLazy, SoftLazyIndex, LazyIndex, LazyOps, MappedLazy,
  WaitStrategies, SequenceBarrier, ScatterGatherIO, CoalescingRingBuffer and others. These need a declared recipe
  (delombok, then slf4j -> System.Logger); M3JDK carries neither lombok nor slf4j.
- **third-party imports.** Caffeine, Chronicle/net.openhft, Agrona, Apache commons-pool2, commons-net and Synexia
  FOP. Under the no-third-party-absorb law they stay out unless an M3 owner replaces the dependency.

## Proof (PROOF.tsv): 8 of 8 PASS

- javap -c and javap -v are identical for all 6 class files.
- The mutant is caught.
- JUnit runs 5 of 5 on both sides.

## Gates

- jdk_naming: fixed point (0 moves, 0 rewrites).
- Sprawl gate: new=5, refused=0.
- Custody: the tip is resealed, and --check reports changed=0.
- Default module suite: 2790 tests, 0 failures, 0 errors, 151 skipped.
