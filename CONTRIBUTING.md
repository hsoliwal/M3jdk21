# Contributing to M3JDK21

M3JDK21 retains the upstream JDK contributor guidance below. For M3-specific changes,
start with [AGENTS.md](AGENTS.md), the [porting invariant](m3/docs/m3jdk21-porting-invariant.md),
and [existing naming map](m3/docs/name-mapping.json). This checklist explains how to
prepare a review; it does not change those authorities or any license.

## Before editing

1. Record the target base SHA, branch and relevant source/test paths. Inspect path history
   backward to the last qualified baseline and forward through successors. A merge or a
   historical passing result is not evidence for a changed tree.
2. Identify the existing owner, public API/ABI and observables: exceptions, Unicode,
   equality/identity, ordering, concurrency, serialization, ownership and release.
   Reuse an existing guide and mapping row rather than creating a second authority.
3. Classify the change: standalone module, reusable donor recipe, exact target adaptation,
   runtime integration, documentation, or evidence. Review the
   [convergence model](m3/docs/SYNEXIA_CONVERGENCE_MODEL.md).
4. For reusable semantics, improve and qualify the canonical Synexia recipe first; receive
   exact pinned output here. Record donor revision/path/blob, recipe identity, target
   preimage/postimage, notices and target-only adaptations. Do not independently edit
   a sealed mirror or add a Synexia runtime dependency to `java.base`.
5. Preserve the ordered receiving phases. A standalone collection proof does not promote
   the JDK collection backend. Missing provenance or qualification is an explicit pending
   disposition, never a reason to weaken a gate.

## Documentation and source comments

Document inputs, units, return/refusal behavior, exceptions, owner lifetime, thread-safety
and compatibility boundaries where the implementation establishes them. Link the symbol
or executable test rather than treating a design aspiration as an implementation fact.
Keep historical receipts dated and immutable in meaning; add a current-status explanation
instead of silently relabeling the old result.

Javadoc and native comments inside hash-sealed source are source changes too. Reconcile
such edits through the owning recipe and refresh affected receipts; a comment-only change
can invalidate exact blob checks. This is why a guide correction need not rewrite sealed
Java/C++ files. Preserve per-file copyright, license and change notices.

## Review and regression evidence

Use the [build/test routes](m3/docs/native-build.md#current-checkout-verification-routes)
for the affected artifact. In a PR, include:

- exact base and candidate SHAs, changed owners and recipe/provenance links;
- the command, toolchain, OS/architecture, runtime flags and selected image;
- separate PASS, FAIL, BLOCKED and NOT_RUN results, with logs and test selection;
- fixed-point, changed-input refusal and rollback evidence for recipe changes;
- relevant String/Unicode/regex, collection, JNI/GC or native regression cases;
- CPU, retained heap and native/process memory evidence for resource claims;
- excluded surfaces, known failures and remaining promotion gates.

A documentation-only PR should check links, shell examples and source claims, and state
that native builds, SDK tests, jtreg and performance measurements were not run unless
actually executed. A parsed example or source invariant is not runtime qualification.
A production PR must additionally run the applicable exact-head build and behavior gates.
Do not alter an oracle, delete a failing test or claim the entire JDK from a focused pass.

For a publication candidate, recheck the base and final diff, then open a draft PR with
bounded claims. Keep publication, merge and release decisions distinct.


# Contributing to the JDK

Please see <https://openjdk.org/contribute> for how to contribute.
