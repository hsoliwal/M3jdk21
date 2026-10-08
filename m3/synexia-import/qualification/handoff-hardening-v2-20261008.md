# M3JDK21 Synexia import hardening evidence — 2026-10-08

Scope: focused importer/check-out hardening. This does not qualify a full JDK image, HotSpot/JIT/GC/CDS/JNI/JVMTI, jtreg, cross-platform or performance gate.

Observed branch before this receipt:
- branch: `m3/synexia-handoff-v2-hardening-20261008`
- head: `fc658ad0a5e355c5bbea1ab70e9e2ea5eb3d75ae`
- target base: current `master` successor state.

## Implemented

- direct import still only adds missing exact files; it does not become a replacement engine;
- a failed multi-file materialization rolls back only files created by that invocation;
- rollback deletes a created file only if it remains a regular non-symlink file with the exact expected SHA-256;
- rollback failures are suppressed onto the primary failure rather than hiding it;
- invocation-created empty directory chains are removed in reverse order when safe; directory ownership is recorded only after this invocation successfully creates each directory;
- source and destination resolution reject symlink/non-directory ancestors;
- `verify-strict`, `materialize-strict` and `stage-strict` bind the manifest's source revision to an exact clean Synexia Git checkout;
- the strict receiver accepts the import manifest's existing 40- or 64-hex Git object-id grammar;
- the existing non-strict APIs remain available for synthetic/replay fixtures.

## Fresh executed focused evidence

Exact current importer Git blob:
`6b6d902d13d4ad75aff551a065f03a8f7a73a7c4`

The locally executed `SynexiaImporter.java` was checked with `git hash-object` and matched that blob exactly before compilation.

Exact current Git-checkout helper blob:
`7f1302fccfe5575ce54538ec4796b98db0bb48ef`

Java 21 compile used:

```sh
javac --release 21 -Xlint:all -Werror ...
```

Focused importer output:

```text
M3_IMPORT_EXACT_PASS checks=8
```

That proof exercised:
- deterministic failure before the second move;
- rollback of the first created file;
- rollback of the invocation-created empty directory chain;
- successful two-file materialization;
- exact target snapshot verification;
- rejection of a source path traversing an intermediate symlink where symlinks are supported.

Strict checkout output:

```text
M3JDK21_TARGET_GIT_SEAL_V2_PASS checks=5
```

That proof exercised:
1. exact committed HEAD plus a tracked manifest source admitted;
2. a SHA-correct but untracked manifest source refused;
3. an untracked source containing Git pathspec metacharacters was refused literally rather than matching another tracked file;
4. a different 64-hex manifest revision refused as a revision mismatch (not a grammar error);
5. tracked worktree drift refused.

## Authored repository tests

The module tests additionally cover:
- rollback of invocation-created empty directories;
- exact-clean operational CLI admission;
- `stage-strict` refusal before staging on tracked drift;
- existing target/source drift behavior and manifest/family policy.

The full module Maven/JUnit suite was not executed from the complete repository in this environment. No hosted CI PASS or whole-JDK acceptance is claimed.
