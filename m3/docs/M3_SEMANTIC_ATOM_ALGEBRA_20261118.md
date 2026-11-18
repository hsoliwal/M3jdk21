<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: documented semantic-atom algebra

**Normative invariant:** `M3-MOP-SEMANTIC-ALGEBRA/1`. **Compatibility floor:** OpenJDK 21.
**Recipe, donor evaluation and correction-memory owner:** `hsoliwal/com.synexia`.
**Qualified Java, HotSpot and JNI implementation owner:** `hsoliwal/M3jdk21`.

## Downward source algebra

Atomize and freeze source in the order:

`REPOSITORY -> PROJECT -> MODULE -> PACKAGE -> FILE -> TYPE/MEMBER -> SEMANTIC ATOM`.

Each semantic atom has a stable source identity, purpose, effects, dependencies, owner,
contract, declared pattern or `UNKNOWN_PATTERN`, documented applicability and evidence.
A file is the fundamental independently schedulable **canonical mutation boundary**.
Extract private/static atoms only where their composition preserves identity, state,
aliasing, callback and exception order, synchronization, visibility, allocation and
resource lifetimes, and public/JVM/JNI contracts. Fragmentation without replaceability,
reuse or proof value is not an improvement.

## Upward semantic composition

Patternize and revalidate in the opposite direction:

`ATOM -> FILE COMPOSITION -> PACKAGE/INTERFACE -> MODULE -> PROJECT -> PRODUCT`.

A pattern record names its member atoms, ordering, dependency/effect envelope,
algorithm category, owner, contract and proof. A leaf which cannot be recognized
receives `UNKNOWN_PATTERN` or `RESIDUE`; it does not receive a guessed pattern.
Atom-level proof does not certify the owning file, and a file proof does not
certify a module, its native ABI or an assembled JDK image.

## Persistent machine memory

Documentation, signatures, atom/evidence ledgers, typed pattern records, dependency
records, donor source identities and recipe-correction memory are part of the
software architecture. Coding agents retrieve them before changes; unsupported
novelty stays a candidate. If a recurring transformation fails, improve the
**canonical reusable Maven/OpenRewrite recipe** and its regression guard, then
regenerate the target. Never silently repair only generated output.

Each accepted recipe is source-pinned, file-local where possible, serially
applied per file, and demonstrated at a deterministic fixed point.
Independent candidates can be analyzed in parallel only when their source
identities and dependency boundaries are frozen. Parent contracts are rechecked
on the exact affected closure; unchanged and qualified parts remain stable.

## Best demonstrated implementation

Compare alternatives by problem category, not by whichever solution an LLM
happened to synthesize. Assess correctness, JDK/source/binary/JNI compatibility,
asymptotic complexity, allocation, memory pressure, native and thread effects,
predictability, benchmark workloads, license, provenance and regression risk.
LeetCode, HackerRank and GeeksforGeeks provide **problem categories and adversarial
test ideas**, not automatically licensed implementation source. GitHub donor
expression requires immutable revision pins plus applicable license/NOTICE.

An LLM may propose; it cannot certify itself. Acceptance follows:
**inventory -> source seal -> recipe -> diff -> lint -> compiler -> tests ->
runtime/JIT/JNI/ABI as applicable -> measured performance -> fixed point**.

No Maven, OpenRewrite, donor catalogue, or Synexia correction-memory dependency
belongs in the shipped JDK runtime merely because it authored a JDK change.

## Existing capability owners: do not reinvent

- Runnable/Callable scheduling and Eclipse Jobs: caller-owned thread-pool and
  progress adapters; a new default method on standard Java interfaces needs
  separate public API/binary-compatibility qualification.
- Eclipse IResource/EFS: existing M3FS and Eclipse EFS provider; native owner
  retains stream, mount, generation and inode contracts.
- JDT/Java analysis: existing M3AST and Java compiler/tooling, with Maven as
  authoring/build plane and exact dependency invalidation.
- Swing/SwingX and SWT/Nebula: existing UI/viewport owners; respect UI-thread,
  event, focus, disposal, accessibility and native rendering contracts.
- Filtered/sorted/navigable collection wrappers: existing M3 collection views,
  preserving live backing, comparator/callback order and standard iterator/API
  contracts; prefer array/offset/mask internals when proved.
- File marks: reuse the existing Eclipse mark/AST lineage before proposing a new
  persistent annotation or marker namespace.

## First incremental frontier

`M3ImpactFrontier` reuses the authoritative validated atom/evidence ledger and
a supplied source-bound dependency relation. It computes conservative downstream
atom, file and source-owner revalidation closure with primitive reverse-graph
and file offset planes. It carries all affected admission blockers and never
grants mutation, semantic-equivalence or promotion authority. Java native
declarations and JNI export atoms can be linked; this does **not** invoke JNI
or claim native ABI proof. Higher-level owners must be established by explicit
M3AST/M3File mapping rather than inferred from a path.

See `m3/tooling/atom-census/contract/M3_IMPACT_FRONTIER_V1.md` and the separate
catalogue-donor decision ledger.

**Governing law:** descend to atoms, preserve contracts, recognize and document
patterns, compose upward, verify every affected parent boundary, and admit only
what the evidence actually establishes.
