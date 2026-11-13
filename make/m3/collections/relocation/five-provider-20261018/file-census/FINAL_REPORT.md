<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 five-provider pass-0 verification

**Actual change:** five pinned per-file Git-tree inventories and M3Scale output-contract documents only. **No upstream source classes or M3 runtime classes changed.**

- Inventory: 3214 Java file identities across 12 source/test/generator roots; 5 families; Git API reported no truncated trees.
- Guava 450, Eclipse Collections 1516, Apache Commons Collections 678, Spring 192 (read-only), JDK 378 (java.base protected).
- Source classification: candidate based on filenames and root categories; not a behavioral proof.
- Exact compiler/runtime/JNI/file-wide tests: **NOT RUN**. Local Maven not present. No PR merge status counts as test evidence.
- Next action: use existing Synexia OpenRewrite recipes, full-file reads, donor tests and serial proof gates before M3 runtime edits or product handoff.
- M3Scale canonical attachment SHA-256 is pinned in RUN_CONTEXT.tsv.
