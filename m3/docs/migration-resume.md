# Resume the MIndex migration and continual optimization

Read `name-mapping.json`, its schema, `migration-coverage.md`, this file and the
private inventory before changing a mapped owner. The complete user specification
remains authoritative; this milestone does not close the migration.

The active thread goal is `01a0fa3c-18d0-7940-b8c1-f90c2aecae42`: continually
implement, verify and optimize the entire MIndex/MatIndex/SubMIndex migration,
Routes A/B/C, performance and retention, recipes and durable enhancement mappings.
Keep it active across partial ports, passing tests and draft PRs. The durable
pick-up list is `C:/Users/hsoliwal/.codex/TODO.md`; append bounded entries because
the inherited file is unusually large.

Current pins: private source `6df9df8d8f42111239013ee941723ec37f97ba6e`, public
target baseline `8bb6215372e07712f1fdf5a0cb912af495007b19`. PR ancestry alone did
not integrate the runtime production tree into master. Exact PR #6 reproduction
and a new master-based runtime restoration candidate have separate receipts.

The current selected Java implementation is committed at
`ef33a8cceff4848551f32ec302f8940ec6609591`, tree
`01e5fef884fa06eb655ebe628acc8c26786ef6b2`. `shared-owner-code-binding.json`
binds twelve target bodies and seven actual receipts to that code. The historical
`selected-code-binding.json` remains bound to the earlier f07 implementation.
The integration incorporates master `45f546ff5bcb06a1b2604f14baf998785d98d9a1`,
preserves its schema-1 authority and all 25 incoming migration records, and adds
twelve selected records. The seven physical Frozen/Joined owners now reside only
in `m3/ports/indexstring/src/main/java`; both M3Text and M3String consume them.
The v2 installer, donor replay and baseline consolidation recipe reproduce this
closure. Historical v1 templates and receipts retain their original identities.

The private inventory lives at
`synexia-mindex/repository-inventory/docs/migration/`: `source-snapshot.json`
records pinned Git discovery, and `mindex-to-m3.json` records every disposition.
Do not publish the entire private catalogue in the public JDK. Selected authorized
text source hashes and current adaptations are public under
`m3/ports/text/provenance-consolidated.json`; the older provenance is historical.
The private artifact commit is `edc04489dc46d4ff810517e74bf9162f83a2b15c`,
distinct from its original source pin 6df. Integration commit
`1702b78729dc84c06aaebd9fff3ebb596479bfd6` is published in private draft #7696.
The hash-bound census covers 29,056 items: 29,037 pending and 19 visibly excluded;
16,250 Java files parsed and 93 original syntax failures remain blocked. It records
31,706 symbols and 153,692 public/protected contracts. Eighteen inventory
discriminators pass. Whole-source admission remains FAILED, and the private
wrapper's mechanical split is published at
`3a5e8d14856e77990c4036ff76a23451fc5dfe2a`: 18 modules, maximum 147 lines,
40-line compatibility shim, all 28 signatures preserved, 18 discriminators and
950 exact regenerated index/shard comparisons pass. Facade monkeypatching no longer
replaces a leaf's internal globals; that compatibility boundary is explicit.
The selected seven source IDs/raw blobs and 76 syntax contracts resolve to
the frozen census; their private pending dispositions remain unchanged.

Implemented and tested here: the seven-file immutable Frozen/Joined closure,
three additive explicit-view types, exact UTF16 admission, retained join/ranges,
canonical-owner projection, deliberate uncached compaction, owner-local UTF16
facts, literal cursor search, stock regex/whole-input encoding, source-bound
replay and reviewed reverse replay. Four stock modes each pass 64,450 checks.
The consolidated closure also passes M3Text's 1,014,911 checks in default and
interpreter modes, 30,000 concurrency iterations and the original 502,762-check
surface corpus. Public JVM descriptors are preserved for both pinned baselines;
four executions of unchanged baseline-compiled test bytecode pass on candidate
classes. This scoped binary evidence does not admit every source, constant,
serialization or native ABI contract.
The existing foundation passes four modes of 70,462 checks and its private
content/range oracle passes 786,432 checks. Linux two-process image mapping is
existing-foundation evidence, not lexicon admission for the new facade.

Compiler lowering is an opt-in typed explicit-admission optimization outside
`java.base`. Its own Maven and compiled before/after fixtures cover operand and
qualifier order, nulls and explicit exceptions. General `invokedynamic`, constants,
public/escaping boundaries, identity-sensitive observations and OOME parity remain
open; ordinary Java remains the fallback.

The full-family structural owners, bridges, conflicting substring adapters,
shared/local mixed lexicon lifetime, remaining actual String surface and all
unaccepted native/VM gates remain work. The complete modified JDK image must match
its claimed source commit/tree. Preserve historical StringJoiner failures and
interpreter-only restrictions until evidence justifies a compatibility change.

For an enhancement:

1. Diff the exact new source pin from each mapping's last synchronized pin;
   discover changed/deleted/moved items with the existing private scanner and
   dependency graph. Preserve stable mapping IDs through reviewed lineage.
2. Classify API, semantics, serialization/native ABI, bootstrap and performance
   effects. Run the borrow-first ledger gate before a substantial new capability.
3. Compare baseline/current source/target. Refuse target divergence; propose an
   explicit adapter or reviewed merge. Never automatically reverse-port changes.
4. Replay the exact selected donor patch with `text-port.py`; replay the public
   package with `apply.py --baseline`. Runtime recipes have their own native fences.
5. Run `test_mapping.py`, `test_text_port.py`, `test_recipe.py`, the four-mode text
   test and independent compiler/native gates. Bind receipts to candidate file
   hashes and the exact accepted code commit; do not relabel prior builds.
6. Update dispositions, dependencies, lineage, notices, evidence and generated
   coverage in the same reviewable change. Refresh recipe hashes after review,
   then verify replay again. Keep unresolved conflicts and exclusions visible.

Fresh ef33 raw-Git source/class-bound diagnostic measurements are in
`shared-owner-benchmark-20261002.json`. Three new forks report about 160 bytes
per retained long two-atom join versus 16,424 for stock concat; tiny retained slices
allocate 248 bytes versus stock 96 and run slower in this fixture. A one-unit slice
retains 8,192 owner payload bytes; explicit compaction leaves two. Warm admission
still scans input for exact hash/content lookup. These are load-dependent local
diagnostics, without JMH, latency percentiles, complete retained heap headers,
cache-pressure distributions, native/mapped bounds or precomputation amortization.
Those remain performance gates; no general speedup is admitted.

Infrastructure notes: anonymous raw GitHub fetches of private source return 404.
Authorized exact Git fixtures can be used in ignored private scratch; do not copy
them into the public tree to make hosted CI pass. E: space was measured tight;
current public checkouts use C:, the private source checkout is an S: junction,
private parser scratch uses S:, and native builds use WSL `/var/tmp`.
JVM native-allocation failures were recorded separately; bounded test heaps do
not turn a failed invocation into evidence. Installed JDKs remain unchanged.

Published review surfaces: [draft #24](https://github.com/hsoliwal/M3jdk21/pull/24)
for selected text/compiler/recipes, [draft #23](https://github.com/hsoliwal/M3jdk21/pull/23)
for the separate runtime restoration, and private
[draft #7696](https://github.com/hsoliwal/com.synexia/pull/7696) for the source census.
PRs #19 and #20 have merged into master. The exact combined-runtime code at
`06c2ee74dfd6d243e305e4c12e67f44862b3904e` failed compilation because its
AutoCloseable backing interface inherited a checked close contract and triggered
the JDK's warning-as-error gate. Candidate
`ebdafc4876717eb7e43e21827b260e50f63b6a00` explicitly declares unchecked close;
its own complete image build and acceptance remain pending. The older matched
696 image's 86 default/16 enabled scoped passes do not qualify either new commit.
The two original 4 GiB StringJoiner gates remain pending; historical failures are
preserved. The fixed-image launch encountered WSL startup I/O errors before make; no new
image pass exists. Task-owned runtime checkout relocation is in progress with
a saved raw inventory and resume state at
`S:/m3-runtime-transfer-20261002/runtime-relocation-state.json`. Resume the
existing worker; do not restart its walk or infer the startup error cause.
Continue with the matched image and canonical source-obligation reconciliation,
full family/VM/compiler gates, and fresh
performance and lifetime measurements. The continual goal remains active.
