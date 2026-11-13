<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 deque zero-mask fast path — 2026-10-18

## Scope

This is a deliberately narrow continuation of the existing M3 primitive-array/mask owner. The Java list already validates the aligned state vector and returns immediately for `requiredMask == 0`; the circular deque validated the same input but still traversed its logical ring before returning the same no-op result.

The selected atom adds the same post-validation identity fast path to `M3LongArrayDeque.retainByStateMask`. It does not change signatures, fields, JNI symbols, ring layout, ordering, mask semantics, or mutation epochs.

## Contract

- Validate null, mask range, and exact logical-length alignment **before** the fast path.
- A zero mask admits every element, regardless of state bytes; return zero removed.
- Do not scan/rewrite the ring, clear slots, change size, or increment `modCount`.
- Preserve the deque's logical order even when the ring wraps or has grown.
- Nonzero masks retain the existing stable in-place compaction path.
- The mask remains caller-owned and must not be mutated concurrently.

## Problem catalogue

| Category | Candidate | Decision |
|---|---|---|
| Identity/empty-selection fast path | Existing `M3LongArrayList.retainByStateMask` | Reuse its established validate-then-return contract |
| Stable in-place compaction | Existing deque compaction loop | Retain for nonzero masks; no scratch array |
| Bit-mask predicate | Existing `M3IndexMaskCompactor.matches` | Keep canonical unsigned-byte semantics |
| Circular indexing | Existing `physicalIndex` / ring storage | Do not replace with linear-array assumptions |
| JNI state-mask selection | Existing C17 selector | No native ABI change; Java owner is the missing fast path |

The LeetCode/HackerRank/GeeksforGeeks comparison category is identity selection / stable compaction; no challenge-site implementation is copied. The selected algorithm is the already-qualified local list implementation because it is the closest contract-compatible donor. Expected work for a valid zero mask changes from O(n) traversal to O(1), with no allocation.

## Verification status

The source edit and regression fixture are additive on a branch from the reviewed `master` head. The regression checks zero-mask identity, iterator validity, extreme long values, and logical order. Full Maven/OpenRewrite execution, JNI parity rerun, and full module/reactor checks are **pending**; this packet does not claim they passed. Recipe tests are the promotion gate for this source transformation.
