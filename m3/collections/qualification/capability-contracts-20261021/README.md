# M3JDK21 capability qualification vocabulary

This records the user's ten proposed contract families and all 42 annotation names in [M3_CAPABILITY_QUALIFICATION.tsv](../../../docs/M3_CAPABILITY_QUALIFICATION.tsv). The four data cells of each family are preserved from the supplied table. The mechanism column is labelled `mechanism_candidate` and every row has `CONTRACT_PROPOSED_NOT_ADMITTED`: a name or candidate mechanism does not establish a qualified implementation. This does not revoke any narrower, separately proved capability already present in the repository.

M3JDK21 is the integrated implementation target. Refinements belong in its existing Java and JNI owners, preserving public interfaces, contracts and observable behavior. Synexia and other convergence workspaces supply reusable, validated recipes before materialization into the JDK or SWT owners. This packet adds contract data and an existing-recipe binding; it introduces no annotation classes or runtime indirection.

## Admission and refinement

For each operation, consult the problem-category algorithm catalogue before selecting a donor. Record the repository/revision/paths, licence, interfaces, lifecycle, dependencies, correctness evidence, complexity and measured performance where claimed; compare the incumbent and rejected candidates. GitHub implementations require suitable licensing and retained attribution. LeetCode, HackerRank and GeeksforGeeks can supply problem categories and approaches; a page or passing submission alone does not authorize copying code or prove compatibility.

Refine one production owner at a time. Bind each change in the canonical Maven/Java recipe, then materialize its receiving owner. Behavioral atoms may be replaced independently when their composition preserves the public contract. Admission requires the row's full qualification conditions, exact source and dependency identities, an executed discriminator and the relevant existing contract tests. A faster-looking algorithm or an annotation name supplies no admission.

Collection authority remains in the supplied custom JNI array owners and dynamic/segmented arrays, primitive offsets/index/order lanes and state masks under `M3-COLLECTION-JNI-ARRAY-STORAGE-1`. API projections use the existing `M3LFUCache` with bounded ownership and donor-compatible identity, liveness and escaped-reference behavior. Do not retain structural per-record Entry/Node objects. Existing char/byte concatenation and int/long selection JNI outputs are evidence for those exact operations; they do not by themselves prove custom JNI backing allocation for every collection.

For widgets, viewport behavior belongs inside the widget's existing contract. CPU owners control data, ordering, search, masks, paging, invalidation and memory-pressure swap. Rendering consumes visible damaged regions; hidden content is not a reason to allocate and paint a complete object tree.

## Existing recipe and provenance

The unchanged Apache-2.0 owner is [M3Jdk21HashPinnedTextSnapshotRecipe.java](https://github.com/hsoliwal/M3JDK21/blob/c831c4a77f0e3e10aac94178217efda882ba4be5/m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java), Git blob `4a38e39d0755462d40c2b22548b87abde938ea0b`. Its SPDX notice is retained in the existing owner. The crate-local `FOSS_REUSE_DECISION.tsv` records reuse before materialization. No external algorithm implementation is copied by this packet.

Classpath crate: `com/m3/rewrite/backport/jdk21-hash-pinned-text/capability-contracts-20261021/`.

Recipe binding: `com.m3.CapabilityContractQualifications20261021`, configured in `META-INF/rewrite/m3-capability-contracts-20261021.yml`. The one manifest row maps an explicit `ABSENT` preimage to `m3/docs/M3_CAPABILITY_QUALIFICATION.tsv`. The template and receiving TSV are byte-identical, SHA-256 `83ffd2096c22c83f8e4436ca9ed62025bd7a65cb6b3509a9d6b08e33f60b2253`. Replays must use real OpenRewrite PlainText sources and the existing engine's exact-preimage, duplicate, drift and fixed-point checks.

## Evidence boundary

The attachment was mechanically parsed with isolated Python: ten four-cell rows, 42 names, and exact retained data cells. All rows are proposed. The receiving TSV and template are identical, and the manifest hash is pinned. The extraction source attachment SHA-256 is `50162d339fe06bef048f3b0abd0d658b7471b571545053dae16baf1875d64cd1`; private transcript/UI footer debris is excluded.

No Java recipe execution, Maven test, JNI build, collection storage qualification, automatic optimization, memory-pressure guarantee or performance result is claimed by this documentation packet. The unchanged recipe owner's historical tests are not execution evidence for this new crate. Runtime implementation and proof remain separate work.
