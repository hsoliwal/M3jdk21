# SYNEXIA M3 LLM recipe-first invariant

Every Java owner that invokes an LLM during a coding/build operation must bind the invocation to
a named M3MechanicalRecipe through M3LlmRecipeInvariant before any source candidate is admitted.

A source-producing binding is evidence only. The same owner must stage the candidate through
M3LlmCandidateStore; neither model output nor the binding grants write, apply, merge, or promotion
authority.

The Maven goal ai:m3-llm-recipe-invariant scans admitted Maven compile roots and fails closed when
an LLM invocation owner has no recipe binding or a source-fixture owner has no candidate staging
boundary. It emits a deterministic SHA-bound receipt under
target/m3-llm-recipe-invariant/receipt.tsv by default.

This gate is additive to existing M3 atom, contract, donor-provenance, replay, compile/test and
promotion gates. It does not authorize source mutation or weaken public API/ABI verification.

## Project atomization and pattern contract

The existing [file atom canonicalization invariant](SYNEXIA-FILE-ATOM-CANONICALIZATION-1.md#m3way-project-invariant-clarification)
records the additive M3Way requirements and coding-agent checklist: complete boundary
equivalence, one active scoped function composition, reviewed pattern/participant definitions,
recipe-first mutation, conflict-safe leaves, ordered verification, shared host veto and
provenance. Model invocation binding and candidate staging remain required; this cross-link
adds no source-copy, write or promotion authority. Recording the requirement does not prove
that every existing owner or launcher enforces it.

## Apply, retain and verify the recipe in Synexia

**NON-NEGOTIABLE / ERROR / missing evidence means HOLD.** This is the same recipe-first
invariant, not a new transformation authority. It applies to every LLM-assisted coding task,
including work requested in another conversation. Repository policy, not conversational memory,
is the durable record.

`hsoliwal/com.synexia` owns this conversation's reusable recipes, in-memory qualification support,
source templates, fixtures, provenance and application evidence. Keep them in the existing
`synexia-openrewrite-recipes` module and its verification crates. Do not introduce another
atomizer, patternizer, compiler or scheduler. M3JDK21 remains the separate product/runtime owner
when a task explicitly targets that repository; this rule does not move its accepted runtime back.

Create or improve the named Maven/OpenRewrite recipe BEFORE changing its production targets.
Execute the recipe on exact reviewed preimages; retain its actual outputs and execution record.
Publishing templates, a generated patch, or a PR is not equivalent to executing OpenRewrite.
A narrow Java/Maven replay through the existing canonical manifest kernel is allowed for exact
source-sealed work under the root AGENTS.md policy, but must be labelled KERNEL_REPLAY, never
SDK_EXECUTION. Neither execution lane grants promotion authority.

Inspect the delivered tree, not only PR state or ancestry. Classify each earlier output as
EXACT_PRESENT, EVOLVED_PRESERVED, MISSING, or BLOCKED. Preserve a newer implementation until
its contract/history is reconciled; do not force an old postimage onto a changed owner. Retain
or restore the recipe, source templates, fixtures and provenance with that owner. Historical
source-sealed installers keep their original preconditions: changed source must be rejected,
not admitted by rewriting an old receipt or weakening a drift gate.

Qualify source-changing atoms on bounded in-memory multi-file projects with protected expected
observations. Include code-looking strings, regexes, comments, text blocks, ambiguous overloads,
nulls, extrema and declared effects. Check every intermediate recipe application, not only
schedule endpoints. Compilation alone does not establish behavior, API/ABI, source-custody or
Java/JNI parity. A whole round is a fixed point only when EVERY stage is unchanged. Keep recipe
subsets/orders, counterexamples, cancellation, rejected cases and unvisited combinations explicit.

Review existing indexed categories and implementations first, then LeetCode, HackerRank and
GeeksforGeeks evidence and licensed GitHub donors. Pin revision, source identity, license/notice
and derivation before adaptation. Problem/editorial bodies remain reference-only unless separate
rights and policy admit reuse. Similarity and precomputed signals nominate work, never prove it.
Bind reusable facts to exact source, ranges, parameters, ordered recipes, provider/toolchain and
semantic context; invalidate on change. Do not manufacture an exhaustive-review or speedup claim.

Prepare independent file candidates within resource bounds; keep mutations and integration
serial under the existing scope ladder. Preserve interfaces, exception/null behavior, ownership,
effect order and JNI/ABI boundaries. Use the existing progress monitor or a no-op adapter.
Memory pressure changes optional hot/cold residency, not stable identity or canonical payload;
reuse the existing MIndex/SQLite/JNI owners and record actual allocation/lease limits.

Reapply the same recipe to establish a no-change replay and test refusal on drift. Preserve the
required diff/lint/compile/test/integration/contract/reactor gates and source-freshness checks.
Use approved offline dependencies. Record PASSED, FAILED, BLOCKED and NOT_RUN separately.
A stored hash, self-reported boolean, skipped test or merged PR is not execution evidence.
No rebase, reset, force-push or ancestry-only completion on develop; publication remains a
reviewable additive PR, not an automatic merge.

The machine-readable companion is [SYNEXIA-LLM-RECIPE-FIRST-2.json](SYNEXIA-LLM-RECIPE-FIRST-2.json).
The module's AGENTS.md points here. The source-sealed policy recipe is
`com.synexia.rewrite.M3RecipeCustody`; its focused Maven crate is
`synexia-openrewrite-recipes/verification/recipe-custody-20261006`.
The source-seal test checks policy custody, replay and refusal; it does not install a universal
runtime enforcer, authenticate evidence producers or alter branch protection.

## Serial owner refinement and the cumulative category catalogue

This is a standing Synexia M3 requirement for future coding tasks as well as the current task.
Reuse the validated recipe and the actual owning Java, native/JNI, build or contract file.
Refine its smallest useful atom while preserving the surrounding implementation and all
reviewed invariants. A candidate may improve storage, state encoding, traversal, filtering,
sorting or another algorithm without creating another public owner or transformation engine.

For each file, record its exact base, the atom under review, protected observations and the
proposed sequence of refinements before integration. Prepare independent candidates within
resource limits, then integrate one admitted file/refinement stage at a time. Each stage must
retain its recipe, exact before/after seals, actual execution output and applicable compiler,
behavior, API/ABI and Java/JNI evidence. Carry forward a passing owner into the next stage;
preserve a failed witness and reject or defer its candidate. A later successful endpoint does
not erase a failed intermediate stage. Replay every applicable leaf and the whole ordered
schedule to an unchanged result. Missing evidence remains HOLD under the existing gates.

Accumulate candidate and donor decisions in the existing searchable category catalogue,
`tools/short-flag-query/reviews/*.tsv`, using its current columns: `category`, `candidate`,
`source`, `revision_or_reference`, `rights`, `complexity`, `contract_comparison`,
`decision` and `qualification`. Reuse the existing search entry point:

```sh
python synexia-openrewrite-recipes/verification/materialization-leases-20261015/verify.py --category <category>
```

The catalogue is a cumulative review record. Append a new observation or explicit supersession
when a recipe, source pin, oracle or result changes; retain earlier accepted, rejected, failed,
deferred and unmeasured alternatives. Link the actual recipe and source owner, donor revision,
license/notice, assumptions and proof location. Keep EXACT_PRESENT and EVOLVED_PRESERVED
carry-forward decisions distinct from missing or blocked work. Search existing categories and
reuse prior evidence only within its recorded source and semantic bounds.

For each serious alternative, compare correctness and its assumptions; compatibility with
nulls, exceptions, effect order, ownership, public interfaces and JNI/ABI; time, space and
allocation costs; measured resource/performance evidence or an explicit NOT_MEASURED result;
and license, provenance and adaptation rights. State why the current owner, an adapted donor,
a simpler specialization or a deferred candidate wins that bounded comparison. Challenge
categories may guide discovery, but editorial code stays reference-only unless separately
admitted. Similarity, asymptotic analysis and finite tests do not establish universal correctness
or a speedup across workloads. These records support the existing admission and promotion
authorities; they do not create another catalogue engine or grant write/merge authority.

The additive source-sealed recipe for this clarification is
`com.synexia.m3.SerialReviewInvariant20261019`, qualified through
`synexia-openrewrite-recipes/verification/parser-collections-sdk-20261019/pom.xml`.
The historical custody recipe and its immutable preconditions remain intact.
