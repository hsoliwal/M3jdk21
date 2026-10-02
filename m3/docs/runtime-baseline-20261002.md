# Runtime baseline and merge reconciliation, 2026-10-02

Read this with `shared-atom-concatenation.md` before interpreting a merged PR as a runtime implementation. The migration starts from M3jdk21 master **8bb6215372e07712f1fdf5a0cb912af495007b19**, tree **26554d002cf158c97dd4ffbb06b3885b647c0a17**. The authenticated source default branch is com.synexia develop **785dba96acef7588ba8920f8e455a0d0ddcfaff1**. Source and target remain separate repositories and acceptance domains.

## Tree authority

[Documentation PR #7](https://github.com/hsoliwal/M3jdk21/pull/7) merged head `1210d76ce80263003981c8c6ba5bb4175524358a` as `3029cff40e927aefcca444a6ef759a562204579c`. Its design remains proposed and its acceptance checklist remains open.

[Runtime PR #6](https://github.com/hsoliwal/M3jdk21/pull/6), head `3776d6e674d6c9b04539ca24aca2504aa88d4a57`, merged at 01:27:56 UTC into the feature branch `m3/segmented-string-experiment`, merge `305dc277139b0e7cff1f4e284e7019a381480153`. [Master integration PR #10](https://github.com/hsoliwal/M3jdk21/pull/10) subsequently reports merged at 01:34:12 UTC as `67876a03d823f5ff5ca92ca1545a026d757c4b12`.

Executed Git object inspection proves that #6 is an ancestor of the pinned master, but **its runtime changes are absent from the master tree**. The #10 merge and master both retain tree `26554d002cf158c97dd4ffbb06b3885b647c0a17`, also the #7 merge tree. `m3/runtime-stage1`, `m3/runtime-segments`, `m3/runtime-integration`, `java.lang.MIndexString` and `java.lang.MIndexStringPool` exist at #6 and are absent at master. Master `java.lang.String` has no M3 runtime integration. An ancestry-preserving merge therefore does not prove content integration.

The independent foundation at master has a sealed `com.m3.text.M3StringPiece` CharSequence API, immutable owned local ranges and descriptor joins. `SharedLexiconImage` copies records into local immutable pieces; it does not publish directly mapped String backing. `LocalM3Arena` is an owner allocator, not yet an interner. `m3/docs/name-mapping.json` is a provisional four-entry map, not an exhaustive family migration.

## Existing recipe chain

Reuse #6's source-pinned mechanisms before adding an alternative runtime architecture:

1. `m3/runtime-stage1/apply.py`: stock String hash `4689d77e...` to singleton-join hash `53cf6d06...`, with unchanged VM fences.
2. `m3/runtime-segments/recipe/apply.py`: 17-file coordinated segmented String/VM patch, String preimage `53cf6d06...`, postimage `d1c20848...`.
3. `m3/runtime-integration/recipe/apply.py`: 25-file canonical MIndex storage/VM patch, String preimage `d1c20848...`, postimage `068cc68a...`, creating both `java.lang.MIndexString` owners.

The integration recipe alone cannot apply to stock master: it requires the previous stage's exact hashes. Each existing recipe refuses drift/mixed state, records hashes and supports reverse replay. The final recipe's own tests exercise replay, idempotence, rollback, per-file drift refusal, symlinks, missing files and prior-stage gates. Runtime recovery must preserve that serial dependency chain and retain current master documentation and target adaptations. Restoring Git ancestry alone has already failed this content requirement.

## Historical acceptance, not current proof

#6's published receipt reports a complete Linux x86-64 fastdebug image, 86 flag-off tests passing, and enabled interpreter-mode upstream tests at 16 pass / 2 fail. The two StringJoiner suites contain six failing OutOfMemoryError expectations. Enabled compiled/JIT mode, CDS, JFR, JVMCI, deduplication and full jtreg/JCK remain open or disabled. A reduced allocation result is not a throughput result. Those receipts are tied to the historical candidate and cannot certify an eventual migration branch.

The mapped formats proven by that candidate are M3LEX001 UTF-16LE and SYNARR01 committed language-0 UTF-16BE scalar records. Byte/tuple/multilingual sharing, refresh and application-resolver sharing remain separate gaps. The historical shared-owner source pin `c6cb340d33323348fab94e8157842455e9b72bc2` differs from live #7498 head `a01cbe66fa53a0fe782573ac66bfc090f02415d9`; do not substitute the newer source without renewed format and behavior evidence.

## Available execution lanes and custody

The host is Windows x64; stock Oracle Java/Javac 21.0.9 is available at `C:/Program Files/Java/jdk-21`. WSL Ubuntu 24.04 provides Linux x86-64, OpenJDK 21.0.12.1, GCC 13.3 and Autoconf 2.71. Header/configuration checks and a new complete native image build remain execution gates. No installed JDK is replaced.

The initial full-history filtered clone on E failed with an actual no-space error. Measured E free space was 9,768,960 bytes and C free space 36,933,156,864 bytes. The migration checkout resides on C and the requested E path is an owned junction. WSL scratch has approximately 935 GiB available. These measurements do not revive the earlier erroneous historical claim that C was full.

Authenticated repository metadata confirms com.synexia is PRIVATE and M3jdk21 is PUBLIC. This user authorizes the relevant migration; unrelated private material and data are outside scope. Source root LICENSE is Apache-2.0 at the inspected existing source checkout; exact source pins and per-file notices still govern individual ports. OpenJDK root LICENSE remains GPLv2 with its existing exceptions/notices; the independent m3 subtree retains Apache-2.0. Historical unauthenticated private-source HTTP 404 and missing compiler packages remain infrastructure failures, not passed acceptance.

Live PR JSON snapshots are retained locally under `E:/tmp/m3-migration-audit-target`; they contain no tokens. REST requests reported an exhausted API rate limit. Git HTTPS/SSH and authenticated GraphQL PR reads succeeded without bypassing the limit.

## Execution update

The exact #6 candidate was restored in a separate checkout at `C:/worktrees/M3jdk21-runtime-migration-20261002`, commit `3776d6e674d6c9b04539ca24aca2504aa88d4a57`, tree `1a1d13646d4255231ae2e18aafb73684e3c88b23`. Its integration `--check` and four recipe suites (P0, P1, segments and integration: 16 tests total) executed successfully. A separate preflight seeded all 37 runtime/fence paths from exact master, executed the three existing recipes serially, compared every result with #6 and reversed all three to byte-identical master preimages. The root migration runtime remains untouched.

Native staging encountered a WSL restart that cleared `/tmp`; durable scratch now lives under `/var/tmp/m3-runtime-migration-20261002`. A first configure detected the upstream WSL default Windows target and missing zip. The documented Linux flags in OpenJDK `doc/building.md:247-251` are `--build=x86_64-unknown-linux-gnu --openjdk-target=x86_64-unknown-linux-gnu`. With those flags and 39 Ubuntu development/runtime packages extracted into private scratch, configure exited 0 and a complete fastdebug `make images JOBS=4` was started. No system package or installed JDK was replaced. A successful configure is not a successful image build or acceptance run.

Receipts remain at the separate checkout's `m3/runtime-integration/evidence/20261002-rerun`: `recipe-check.log`, `master-replay-preflight.json`, `configure-linux-private-deps.log`, dependency hashes and the live native build log. Next executable work: finish the matched image build, then rerun flag-off and opt-in tests while keeping the two known failing StringJoiner gates visible. A current success claim requires the resulting candidate/tree/image identities and commands in `m3/evidence/current-baseline.json` or its exact-candidate successor.

## Matched restoration candidate

The new master-based restoration is code commit
`69667e3f1ce673aaded2ad93e4ae2ecf6c59668b`, tree
`406f4bd973d40c5c8ce1782e01745307aab008ff`. Its complete incremental fastdebug
image build exited 0; focused/API/JNI/JVMTI/GC/mapping/rejection/allocation gates
have independent receipts under `evidence/20261002-candidate-696`. The unchanged
selected upstream scope now passes 86 flag-off tests and 16 enabled tests.

Both original 4 GiB StringJoiner cases remain pending on this candidate because
the host had insufficient virtual-memory headroom; the donor's two historical
failures remain open. Cancellation/interruption and resumed subcases are recorded
separately. Enabled execution remains interpreter-only. Full jtreg/JCK, enabled
JIT and the remaining VM/platform gates have not passed.

New master `4a81f3b3050fa5572ec1ff9368e2c383231db2e6` preserves the relevant
String/HotSpot production paths; the clean merge result was checked without
relabeling the 696 image as that merge tree's build. The task-owned private source
clone has moved to S: with its C: logical path preserved. Current disk and memory
failures are measured new observations; the historical C: claim remains corrected.
