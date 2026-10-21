<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 compound-signal array/mask/order receiver

## Exact authority

Product: `hsoliwal/M3jdk21`, branch `m3/compound-signals-receiver-20261018`, base master `07ba6e383343d32c83cfdaf641387bbe72bf325d`.
Canonical Maven/OpenRewrite source-recipe branch: `hsoliwal/com.synexia@m3/m3jdk21-compound-signals-20261018`, draft PR [#12243](https://github.com/hsoliwal/com.synexia/pull/12243).
First-party donor design: Synexia [#11656](https://github.com/hsoliwal/com.synexia/pull/11656) at `a63848ba1875d53a0b675ff66d0c65e698e53f83`.

Named recipe `com.synexia.m3.M3Jdk21CompoundSignals20261018` reuses existing Maven/OpenRewrite Java LST source-seal implementation. Four target source and test files were `ABSENT` on pinned master, generated from the four sealed Synexia templates. All source Git blobs were read back from GitHub and match their source-template Git blobs exactly. The recipe's real SDK first-apply/fixed-point/refusal JUnit is provided in Synexia, not duplicated into the target runtime.

## Existing owners unchanged (Git SHA-1 and SHA-256)

- `M3SignalOrder.java`: Git blob `f7f6e1f4ea9763b4b4238998ffb7377f4f871e6f`; SHA-256 `da761a6b4f10d9a3c9a61979a4ee238fd6d26492d1ba0e6daacdf371f6561058`.
- `M3SignalOrderNative.java`: Git blob `7e5bc5fb1c1a25e00452f2dc0f58d4c90900bffe`; SHA-256 `eaa37fee096172a200088d80b2b48955d60d602061fba71d715e25f6d395acc8`.
- `m3_signal_order_jni.c`: Git blob `46b578b94710d804dd4eb198a8c235fb3db92547`; SHA-256 `e7a5991f5be2e0bad99400ff477a1c03ec9d1d8e1236f9faf366485f94966ed5`.

## Four exact generated new files

| Target file | Source Git blob | SHA-256 |
| --- | --- | --- |
| M3SignalDimensions.java | `dbda28df5bb16211e03516e8142d10bbc9047bad` | `c9bc4dc17e68606be0321e42d1c88c404a4dd7f68d7b90c266e446e19d4fa9ba` |
| M3SignalDimensionsContract.java | `85dcf68f15ffd864cd14031849e7a3a65f43f8a3` | `372a5be9ccd7bba8aae5083e63aaca59805f4b8ed17aca5854f6ea387aa49190` |
| M3SignalDimensionsNativeContract.java | `3735a5c02c44db6d3b7e693972318351101e6ac0` | `c0812f809f09c7ae389c5a9bbf7f6b3904619cb102eae41815f78be34adc58f3` |
| M3SignalDimensionsTest.java | `93f78fc322a20d6aa77ea4cc5bd9c2a1d9c85fc1` | `32036f5d67de908b70e63ed4294b0ca00dfbbf22bdd5a4b4aaaaef79154375ef` |

## API and correctness

`M3SignalDimensions.order` sorts independently descending/ascending `double[d][n]` with stable original indices; `filtered` applies unsigned-byte state masks first; `project` converts each selected object/dimension exactly once into primitive sidecar signals; `compare` preserves sign, first decisive dimension and every finite scalar separation in a defensive immutable receipt. NaN/nonfinite/overflow numeric gaps are marked unavailable; they are not semantic graph or edit distances. Java `Double.compare` (including signed zero and canonical NaNs) is oracle authority. No global O(n²) pair cache or separate object payload; no new JNI symbol, no runtime Synexia dependency, no public java.util interface changes. The existing `M3SignalOrder` Java/JNI sorters are reused.

## Executed focused proof

The `verify.sh` gate reproduces the following against the target's exact existing Git-source blobs:
- OpenJDK 21.0.12.1: `javac --release 21 -Xlint:all -Werror` PASS (actual target owner classes, not synthetic sorter).
- Java oracle `M3SignalDimensionsContract` **1,474 checks PASS** at each of `-Xmx32m`, `-Xmx128m`.
- GCC C17 `-Wall -Wextra -Werror -fsanitize=undefined` JNI library compiled.
- Actual target JNI `-Xcheck:jni` **400 Java/native parity comparisons PASS**, including arbitrary IEEE values, dimensions, masks and stable ties.
- The seven source SHA-256 checked equal the pinned baseline, and all four generated Git blobs were read back from GitHub.

## Pending — no whole-repository completion claim

The source recipe JUnit `Recipe.run` on the actual SDK; Maven module Surefire/reactor and other suite coverage; full HotSpot image/jtreg/JCK, Windows/MSVC, GC/concurrent, serialization, source-time `@Sortable` processor product promotion, memory pressure allocation/cold-warm benchmark, and multi-provider contract equivalence are **not certified**. Target `m3/collections` is still a separately-built proving module and the product's STRING phase remains active; this PR does not promote java.util internals. Full filtered/ordered/writable collection families remain their existing owners and require independent proof. Preserve this candidate as draft until all necessary gates genuinely pass.

Run from a Linux checkout: `bash m3/collections/qualification/compound-signals-20261018/verify.sh`.
