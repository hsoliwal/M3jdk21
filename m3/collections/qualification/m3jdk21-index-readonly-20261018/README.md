<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source-sealed M3 index snapshot read-only receiving

Canonical [Synexia #12249](https://github.com/hsoliwal/com.synexia/pull/12249), commit `84a937606b1fe1ba7823b12c9ad9ff10a9dd35a9`.
Pinned M3JDK21 product base `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`. First commit mirrors the canonical two full
Java preimages, four exact recipe postimage templates, original source hashes,
algorithm/problem-category ledger, and actual Java21/JNI/JUnit workflow.
Second commit receives only the four generated Java source/test postimages,
each Git-blob-identical to Synexia and local mirror.

No direct target source patching. Original M3ObjectIndexSnapshot payload, slot
arrays, state bytes, selection order, JNI optional boundary and sorted/filtered
semantics remain unchanged. The strict read-only facade now refuses empty/no-op
mutation paths consistently, including Java21 reversed views. M3IndexedListSnapshot
still delegates JNI position selection to original M3IndexMaskCompactor, and
M3ObjectIndexSnapshot retains M3IndexMaskWindow.

The original Maven/JUnit module deliberately depends on source-vendored
`org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`. Reuse its
`m3/vendor/third_party` reactor before JUnit; never silently change POM
coordinates or upstream licenses. Missing exact-head test evidence blocks promotion.
