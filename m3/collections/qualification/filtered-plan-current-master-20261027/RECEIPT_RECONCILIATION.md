<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 filtered-collection historical receipts vs current product (2026-10-27)

## Principle

A source-sealed historical recipe **must refuse** a changed Java owner. A
refusal does not prove that the new behavior is wrong, and passing a new
behavior suite does not retroactively qualify the old recipe on new inputs.
Keep historical manifests, original commit hashes, SDK outputs, and failure
logs immutable. A current product must have a new sealed source receiver and
independent tests. Never broaden hash allowlists to accept unknown code.

Current-master source owner:
`m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java`,
Git blob `45ad6df3a51e83c67dc70de7c7acd82bec2305d3`.
Its canonical Synexia recipe is under review in
[hsoliwal/com.synexia#13672](https://github.com/hsoliwal/com.synexia/pull/13672).
This target is the **M3JDK21 repository's own source**; the recipe is not
linked to its runtime.

## Observed CI classifications

| Old workflow / selected run | Actual failure observed | Classification | Independent current-master gate |
|---|---|---|---|
| [Filtered cursor-mask 40491152050](https://github.com/hsoliwal/M3jdk21/actions/runs/40491152050) | `FAIL SHA-256 product drift` for `M3FilteredViews.java` | Historical 2026-10-21 postimage no longer current | New Git-object lineage check and current Java21 test |
| [Strict filtered/sorted sequenced 40491151824](https://github.com/hsoliwal/M3jdk21/actions/runs/40491151824) | `refuse: output/template drift` for `M3FilteredViews.java` | Older recipe correctly declines the new owner | Current sorted/nav/read-only JUnit selection, not proof for older receipt |
| [Stored representative 40491152068](https://github.com/hsoliwal/M3jdk21/actions/runs/40491152068) | Exact owner hash mismatch | Historical source refusal | Current filtered-view tests; stored-representative full corpus still requires comparison |
| [Read-only Map defaults 40491152192](https://github.com/hsoliwal/M3jdk21/actions/runs/40491152192) | Old template mismatches owner; separate owning Maven job lacked installed `com.m3:m3-arrays` | Historical source refusal **and** independent build prerequisite | Current gate installs arrays before collections and reruns read-only Map default JUnit |
| [Direct cursor JNI 40491152050](https://github.com/hsoliwal/M3jdk21/actions/runs/40491152050) | Cursor-manifest SHA mismatch | Historical source refusal | Current Java/JUnit gate; original JNI native parity **not** thereby covered |
| [Bounded navigation 40491151923](https://github.com/hsoliwal/M3jdk21/actions/runs/40491151923) | Five real SDK JUnit tests passed, but receiver failed `target source drift` on historical `runtime/pom.xml` | Correct multi-file receiving refusal | Current Maven compile and JUnit separately; do not receive an older POM over current source |
| [Current successor 40491152076](https://github.com/hsoliwal/M3jdk21/actions/runs/40491152076) | No error: historical Git-object lineage and real selected current Java 21 tests passed | **Verified on prior current-master commit** | Expanded current gate has new tests and must pass again at its own head |

One row intentionally appears in two contexts: the direct cursor and cursor-mask
workflow share a historical receipt refusal. This is **not a count of unique
behavioral failures**. Other failing collection-family workflows have not been
classified here and remain open; do not infer every red check is harmless.

## New current-head qualification scope

Current verification script:
`m3/collections/qualification/filtered-plan-current-master-20261027/verify.sh`.

It pins the M3JDK21 target source and test-source Git blobs, installs the
real first-party `m3-arrays` module, compiles the actual `m3-collections`
module, executes existing sorted and bounded-tree JUnit tests, adds the
new four-case predicate-plan lifecycle JUnit class, and replays existing
read-only Map, filtered collections, and navigable-set JUnit suites.

The new lifecycle test covers multi-threaded first use of the immutable
predicate-coordinate plan (without concurrent writes to the backing list),
fresh live traversals after owner mutation, nested Set/Collection identity and
order, repeated `hasNext` callback demand, and strict read-only refusal.

**New selected test execution remains pending at this document revision.**
Previous current-master passing runs do not automatically cover new tests.
Exact source or Java/JUnit failure is a refusal requiring its own investigation.

## Remaining promotions

Full M3JDK21 source-built runtime image and jtreg, matched JNI/native ABI
and region/callback parity, broader behavioral atoms, upstream contract
compatibility, JUnit coverage audit per atom, and measured performance
remain separate. No JNI speedup or whole-JDK success is claimed.
