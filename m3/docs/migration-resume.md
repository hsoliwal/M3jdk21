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

The private inventory lives at
`synexia-mindex/repository-inventory/docs/migration/`: `source-snapshot.json`
records pinned Git discovery, and `mindex-to-m3.json` records every disposition.
Do not publish the entire private catalogue in the public JDK. Selected authorized
text source hashes and adaptations are public under `m3/ports/text/provenance.json`.

Implemented and tested here: the seven-file immutable Frozen/Joined closure,
three additive explicit-view types, exact UTF16 admission, retained join/ranges,
canonical-owner projection, deliberate uncached compaction, owner-local UTF16
facts, literal cursor search, stock regex/whole-input encoding, source-bound
replay and reviewed reverse replay. Four stock modes each pass 64,450 checks.
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

Current diagnostic measurements show smaller allocation for a retained long
two-atom join and worse time/allocation for tiny slices than stock String. Warm
admission still scans input for exact hash/content lookup. The three-fork benchmark
does not provide latency percentiles, complete retained heap headers, cache-pressure
distributions or precomputation amortization. Those are next performance gates.

Infrastructure notes: anonymous raw GitHub fetches of private source return 404.
Authorized exact Git fixtures can be used in ignored private scratch; do not copy
them into the public tree to make hosted CI pass. E: space was measured tight;
current task checkouts/scratch use C: and native builds use WSL `/var/tmp`.
JVM native-allocation failures were recorded separately; bounded test heaps do
not turn a failed invocation into evidence. Installed JDKs remain unchanged.
