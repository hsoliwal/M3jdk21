<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Current-stack qualification for the existing stable index-order owner

Canonical source: Synexia PR #12569, commit `2dd776f268b83efa328246e08cda49bc67754fbe`. This exact-source mirror maps to its `synexia-openrewrite-recipes` subtree. The three unchanged engine/scope files, dataset, test, POM, script, original probe and catalogue retain their canonical Git identities. This is build-time proof tooling, not another mutation authority or runtime dependency.

M3JDK21 is the integrated product. The existing `M3LongIndexProjection` keeps its single immutable long[] payload and composes int[] indexes and half-open offsets. This continuation reuses the already-reviewed equal-value-run reversal from PR #1451 rather than reinventing sorting or adding a new owner.

## Planned composition and initial state

PR #1451's original branch predates newer filtered endpoint and common-JNI work. The narrow algorithm remains applicable to stack #1554 at `987f743658055252bdaf056610a3a903eba0bfee` (on #1553), with identical projection preimage `317136e66200db1d8fc72d47ff9f8cf52b4566b5`. The separate interior-null correction already exists there. Do not replace its filtered-view class with the older #1451 postimage.

This initial proof-only commit changes no production Java or C. The workflow executes the exact canonical Maven/OpenRewrite recipe, consumes actual Result output, and compiles against the pinned current stack with its real JNI helpers. A final comparison requires the receiving branch to retain the newer filtered/native owners plus the generated projection. It is expected to remain red until the subsequent forward-only source reconciliation; earlier successful steps must be reported separately from the overall conclusion.

Generated output SHA256 must be `a29b0bc589565924611538c6fabe15a70d3629bc4eb3db0977d19f411ef4eb4e`, Git blob `4191858c5aa6e0c1961376e2724fccef0a1d6bcb`. No template-copy or manifest-kernel pass substitutes for actual SDK execution.

## Reproduce

Run `mvn -B -ntp -f m3/collections/qualification/view-order-sdk-20261020/verification/view-order-sdk-20261020/pom.xml clean test`, then its adjacent `verify-current.sh /path/to/exact-987f7436-checkout`.

The parser context uses the original source-sealed fixtures for unchanged referenced APIs; actual current dependency compilation is independently mandatory. The unchanged original probe checks sorting/slicing/filtering, stable physical ties, single-payload identity, mask/gather Java/JNI behavior, exceptional prefixes, bounds, export isolation and missing libraries. Three-process measurements are scoped diagnostics, not universal speedup or UI evidence.

Local POM/shell syntax checks passed. Maven/OpenRewrite and native results for this continuation are pending actual runner execution at authorship. Full collection-module, installed M3JDK/HotSpot/jtreg, all five provider families, non-Linux platforms and SWT/Nebula visible-screen/event/resource acceptance remain separate. No release phase advances from this mirror or a bounded passing job. Existing Apache/OpenJDK/third-party notices remain authoritative.
