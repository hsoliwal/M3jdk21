<!-- SPDX-License-Identifier: Apache-2.0 -->
# JDK-8309665 Arrays source custody successor

The original M3 Arrays-companion relation pins `java.util.Arrays` to Git object `2347e48700de1d26f630b348554ea9db3b4db25b` (SHA256 `e81e711d52fee23756fd0bf8e48c27ff0fe971419f4bb1c25ec1aa5ebc203a88`) at commit `669a949d192345acefcfc5e9bb0b3724174d270c`. The existing Java 21 target product now carries the exact JDK-8309665 [OpenJDK backport](https://github.com/hsoliwal/M3jdk21/commit/3a8e844c6866b8d8c28c08fae2a35bdd2ca1985f), whose immediate preimage has that same original Git blob. The new source blob is `c5412776d202bc6fb36a3a9e65ba35437a98cbef` (SHA256 `0ba39f0b1a0be0c1e8040f778253ea41b5342f34b39786be64988f8b31d12c73`). No `java.util.Arrays` or JNI source was edited by this successor.

The canonical mapping retains the old complete source row under `lineage.previous_sources`. Its current `sources` row is precisely pinned to the post-backport JDK source, and the authority plan is re-pinned to map blob `5aa9e6684ce069b40a6bf696e2e91f244a568ede`. The unmodified naming checker remains fail-closed. `verify.py` reconstructs the exact old source with a reversed immutable OpenJDK patch in a temporary folder, verifies both SHA256/Git blobs and the unchanged M3Arrays companion, and rejects mutant source and patch bytes.

LeetCode array-copy/interval categories (such as LC 88), HackerRank array indexing and GeeksforGeeks array-copy algorithms are **taxonomy references**; the actual authoritative donor is the existing licensed OpenJDK JDK-8309665 patch. No challenge solution body was copied. Full installed-image JDK/JNI/jtreg/JCK admission is not implied.

Original historical name mapping, source/ABI, and target/module contracts remain unchanged apart from the explicit additive source-succession entry. Other M3 collection custody gates remain HOLD.
