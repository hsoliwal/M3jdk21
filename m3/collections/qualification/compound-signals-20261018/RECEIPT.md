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
- `M3SignalOrderNative.java`: Git blob `ca5ec4b6e0fce7350ab411ffe180732d932c0502`; SHA-256 `2b8b802a75e2a0268db41e732da6ed3530df6a128ea25fe3e3cbe981fe419172`.
- `m3_signal_order_jni.c`: Git blob `46b578b94710d804dd4eb198a8c235fb3db92547`; SHA-256 `e7a5991f5be2e0bad99400ff477a1c03ec9d1d8e1236f9faf366485f94966ed5`.

## Four exact generated new files

| Target file | Source Git blob | SHA-256 |
| --- | --- | --- |
| M3SignalDimensions.java | `dbda28df5bb16211e03516e8142d10bbc9047bad` | `c9bc4dc17e68606be0321e42d1c88c404a4dd7f68d7b90c266e446e19d4fa9ba` |
| M3SignalDimensionsContract.java | `895150e091db04a0be96454a3883d0b48459ec0a` | `4d7a8a18db0ea2c548cf1a0117295cb89b9bd7e8cda56f03ef57060258ab728f` |
| M3SignalDimensionsNativeContract.java | `9a9ac756ee5d9fbe52777515026be9a80b44a003` | `60e7c8850d214b485359634fc57f0d5896d362da2eed613bfa0c2a0f901d4d8a` |
| M3SignalDimensionsTest.java | `69f432cb49e28de257e1f2955b5f9a5df9d0c455` | `06bd4926667ec5d37797502694bf92f114a486b2307f595d0c9cb3d48832d85a` |

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
