<!-- SPDX-License-Identifier: Apache-2.0 -->
# Required/excluded exact mask selector qualification — 2026-10-18

This qualification covers one contract-preserving atom:

`byte[] state + requiredMask + excludedMask -> stable exact logical positions`.

The existing required-only API and JNI ABI remain unchanged. The new overload is additive.
Explicit native required/excluded selection is capability-probed and refuses legacy native binaries
rather than silently discarding excluded-mask semantics. Existing `M3SignalDimensions` overloads
keep Java mask selection by default; native mask selection is opt-in through the new overload.

The implementation reuses first-party M3 owners. HackerRank/LeetCode/GeeksforGeeks are
problem-category references only; no challenge or editorial source is copied.

Run: `bash m3/collections/qualification/required-excluded-mask-selector-20261018/verify.sh`.
