<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss leaf 70: mask-window native probe, one-compare mask test and bounded count

Origin 685675e2969 (2026-10-24) was dropped by a retain merge. It brings back three things to
`jdk.internal.m3.M3IndexMaskWindow` and its libjava kernel:

- **Native availability probe.** Native availability is admitted only when both existing entries resolve, using
  zero-work, valid windows. A libjava that predates the receiver therefore never takes `NATIVE_REQUIRED` calls.
- **One-compare mask test.** `(flags & (required|excluded)) == expected`, with expected = -1 for contradictory
  masks. This is equivalent to the two-compare test (proved by the differential).
- **`countBounded` and `nativeCountAvailable`.** A bounded match count with no row or result arrays; its native
  entry is additive. There are no callers yet; it is a capability.

Gates: GATES.tsv. Not run: `make images` or jtreg. libjava on the stock VM lacks these entries, so the Java
backend is probed there, and the C kernels run under a fake JNIEnv.
