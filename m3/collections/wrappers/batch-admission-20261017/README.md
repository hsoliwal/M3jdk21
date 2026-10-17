# Filtered/sorted batch admission receiver

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

Canonical recipe/tests/source images: Synexia PR12028, commit
b095e3c944d66a70d21c267e78ed0db197526a62, task
`synexia-openrewrite-recipes/recipe-crates/collection-view-range-20261016/batch-admission-20261017`.
Exact26-file task tree:f84ca9c65a60be620b9fb1cc5ae251d1a91264d4.

Parent: M3JDK21 PR1036 cb501d5447c659606f4a5d0910379abaa0c36739.
Only the existing M3CollectionViews.addAll atom is changed; one standalone regression
is added. The original hierarchy, deep predicates, range/navigation and metadata work
remain. Use the canonical README and ten-category ledger for contracts and donor choices.
No java.base/HotSpot change, runtime Synexia dependency or production JNI export.

The first receiving commit publishes this exact handoff and nonmutating gate. The
second receives the two exact real-SDK-generated blobs after the canonical source
recipe was committed and locally executed. The target keeps no duplicate canonical
mutation engine; the workflow checks out the exact source commit/tree and reruns it.

Old code validated an input traversal and then let the backing owner read the caller
again. The new code reuses selectedCopy to stage only the validated references.
Stable-input contracts remain tested. JDK addAll behavior under mutation of its input
is unspecified; the five changing-input fixtures establish a stronger M3 guarantee,
not an old JDK contract violation. Custom-owner argument identity, source callback
count and allocation/OOME timing may differ. Staging costs O(batch) temporary references.
No full transactional mutation, concurrent stability or deep element immutability is claimed.

Locally executed source recipe: five genuine Maven/OpenRewrite tests, isolated8.89.0
compatibility profile;34281 generated Java checks each in tiered and interpreter modes;
34261 stable predecessor checks; five original changing-input cases retained;4506 real
JNI checks through the unchanged first-party C17 iterator consumer; public API descriptors
and an original-compiled client retained. Nine unaffected classes match only with -g:none.
The new full-checkout receiving shell/workflow is a separate requested hosted gate, not
a locally executed Git-ancestry/CI result. It is syntax-checked, and its SDK/runtime payloads
were executed locally. Do not infer hosted success from workflow creation.

```sh
bash m3/collections/wrappers/batch-admission-20261017/verify.sh \
  /path/to/target /path/to/exact/canonical-checkout /absolute/new/evidence
```
For an approved offline Maven repository set M3_OFFLINE=true,
M3_MAVEN_REPOSITORY=/approved/repository and MAVEN=/path/to/mvn.

Historical source manifests and predecessor workflows retain their old hashes; they
are not weakened to accept this successor. Full installed-source reconciliation remains
required. Full module/provider/reactor, rebuilt JDK/jtreg/JCK, platforms, serialization,
concurrency linearizability, runtime/native memory benchmarking and SWT/Nebula/Skija/
RWT/GWT/React programme closure remain HOLD. Keep draft; no merge or phase advancement.
