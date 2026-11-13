<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 sorted-SDK Maven recipe compiler recovery

This is a **receiver of one canonical Synexia source-sealed recipe output**,
not a handwritten change to Collections production source. It is created from
the exact output fixture owned by private
[hsoliwal/com.synexia#12265](https://github.com/hsoliwal/com.synexia/pull/12265).

- Current original source Git blob: `0ea9e8bca2a3522d037e675a1ae298f35e59a440`.
- Canonical materialized postimage Git blob: `19dc55f249614aa5aa50717354c42e5a30fc244d`.
- Canonical SHA-256 source preimage: `4871d83f3c0da5d1bbf0f5157c1a73d5335a8508027d2a69b5e5096ce62673ff`.
- Canonical SHA-256 source postimage: `0b1c74509f8e36cbdfbe55a69fd59019315ece6a9bcd6319c985f7f872b12807`.
- Source-sealed rule: `com.synexia.m3.M3SdkSortedSelftestScopeRepair20261018`
  reusing `M3HashPinnedJavaSnapshotRecipe`.
- Original error: `selfTest` declares local `file` inside `try`, then
  accesses it in `finally` after the scope closes.
- Generated repair: replace the exact singular `file.getParent()`
  occurrence with `root.resolve(MAIN + "SelfTestFixture.java").getParent()`.

The two expressions represent the **same temporary directory**, and only
the test/cleanup atom in the recipe implementation changes. M3 Collection
runtime, native C/JNI, public API/ABI and all target production files are
unchanged. The revised `M3SdkSortedContractRecipe` must compile under
Java 21 and execute its `--selftest` as an exact target receipt.

The generated Git blob is a **kernel/materialized candidate**. OpenRewrite SDK
execution must be independently proven by canonical hosted JUnit at the exact
Synexia PR head. The target pull request remains draft until that proof and
target compile are green; do not use this change to justify automatic
application of unrelated recipes or replacing newer outputs.
