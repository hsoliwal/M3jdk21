<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Synexia Machine Oriented Programming
## Recipe Owned Memory for Self Improving Coding Agents

**Hitesh Soliwal · Synexia**  
Technical design paper · Version 1.1 · 22 October 2026

Documentation destination: M3JDK21 · Recipe execution and memory workspace: Synexia

Companion invariant: SYNEXIA-MOP-RECIPE-MEMORY-1

Copyright 2026 Hitesh Soliwal and contributors.  
Synexia — Machine-Oriented Programming (MOP).  
This original first-party paper is licensed under the Apache License, Version 2.0. Upstream and third-party rights remain unchanged.

## Abstract

A coding agent can repair a transformation and still lose the knowledge needed to avoid repeating its mistake. The patch persists, but the explanation, failed alternatives, applicability conditions and verification boundaries often remain in a transient conversation. This paper specifies recipe-owned improvement memory as an invariant of Synexia Machine-Oriented Programming (MOP): every recipe correction must create a durable, versioned record that a later developer, LLM or coding agent can retrieve, inspect and validate. The proposed workflow combines four mandatory facets, Atomize, Patternize, Annotate and Document, with an evidence-gated correction lifecycle. A recipe becomes the durable unit of reusable engineering knowledge; the LLM remains a replaceable planner rather than the owner of state. Immutable events preserve positive and negative results, while rebuildable indexes expose current lessons. Admission is bounded by exact source identity, contracts, environments and tests, and is distinct from permission to execute or merge. We describe a deterministic record model, failure handling, canonical Synexia ownership and M3JDK21 target qualification, then propose an evaluation programme. This is a design and workflow specification, not a report of completed automatic-memory infrastructure or measured performance gains.

## 1 Motivation and contribution

Recipe-first development changes the object of repair. Instead of repeatedly editing each generated file, a developer corrects the reusable transformation that produces those files. This improves the possibility of reproduction, but it does not by itself preserve learning. A future agent may encounter the same compiler error, select the same rejected transformation, or apply a valid repair outside its original scope. Repository history reveals what changed; it may not explain the assumptions required to reuse that change safely.

We propose that a correction is incomplete until its owning recipe retains the lesson. The lesson is not a conversational assertion that the agent has learned. It is an inspectable record connecting a failure, a bounded change, a regression fixture and the evidence actually obtained. Failed and unverified attempts matter because avoiding a known-invalid path can be as useful as recalling a successful one. However, preserving an attempt does not qualify it for reuse.

The central invariant is: **each time an LLM, coding agent or developer corrects a recipe, the recipe records that correction as persistent improvement memory for subsequent work.** This paper expands that invariant into an engineering model. Its contribution is a project-specific contract joining recipe ownership, structured correction history, four-stage knowledge preparation and evidence-bounded admission. It does not claim that version control, regression testing, provenance, retrieval or reusable transformations were invented here, nor establish priority over all related systems.

## 2 Scope and project authority

MOP is the name used here for the Synexia Machine-Oriented Programming approach. Its durable state comprises explicit source identities, transformations, contracts and evidence, rather than dependence on a particular conversation or model instance. “Self-improving” means that the engineering system accumulates qualified improvements and makes them available to later agents. It does not mean that a model retrains its weights, that every generated correction improves the system, or that the agent gains authority from its own records.

The existing repository ownership split remains controlling. Synexia is the exclusive convergence workspace for authoring, storing and executing reusable Maven/OpenRewrite recipes, their execution machinery, recipe fixtures, source seals, provenance and correction memory. M3JDK21 owns the delivered integrated Java, HotSpot and JNI product and its target acceptance. M3JDK21 and other public product forks receive only executed recipe results and data-only provenance/target acceptance evidence. They must not contain recipe sources, recipe execution machinery, correction-memory events, lesson stores, indexes or mirrors. Target-specific acceptance observations may be stored with the target and reference exact Synexia recipe/event identities without copying their payloads. Conceptual documentation may explain the design; it is not an executable recipe or a deployed memory store. This placement rule supersedes older target-copy or proof-tooling-mirror allowances in the historical references, without changing their copyright or license grants. No Synexia runtime service is introduced into the JDK by this proposal.

The companion operational specification defines mandatory requirements and the illustrative MOP-RCM/1.0 record profile. This paper explains the rationale and evaluation model. Neither document advances the repository's staged receiving order or substitutes documentation for implementation. Existing public API, licensing, source-custody and target-runtime requirements remain in force [1–4].

## 3 Definitions and state model

A **recipe** is a versioned transformation or execution plan with declared inputs, preconditions, intended outputs and verification gates. It may operate on one atom, a file, a package, a module or an explicitly bounded dependency graph. A **correction attempt** changes its logic, configuration, template, applicability or validation to address a defect or limitation. An **improvement record** describes the attempt and its evidence. **Admission** is the decision that a particular corrected recipe is suitable within a stated scope at pinned revisions.

For reasoning purposes, represent a recipe state as the tuple R = (I, V, P, C, D, G), where I is stable identity, V is version and exact source digest, P is applicability and refusal conditions, C is the preserved contract, D is the dependency manifest, and G is the required gate set. This tuple is a conceptual model, not a newly implemented Java API. It separates the identity of a transformation from the context in which it is safe to use.

Represent a correction event as E = (Rbefore, Rafter, F, Δ, L, A, X, Q, Parents). F identifies the triggering failure; Δ describes the change; L is the reusable lesson; A is its applicability; X captures exclusions and rejected alternatives; Q links observed verification evidence; and Parents links prior events. A record can be useful with incomplete evidence, but it cannot be admitted merely because its JSON is valid or its digest matches.

The applicable memory for a task is a derived view of valid events whose recipe identities, contracts and preconditions match the task. That view must also account for revocations, contradictions and supersession. Retrieval is therefore not just nearest-text matching. Similar wording can nominate a lesson, but exact conditions and evidence determine whether it applies. A performance improvement admitted for one workload does not become a universal property of the recipe.

## 4 Atomize Patternize Annotate Document

**Atomize** identifies the smallest meaningful contract-bearing unit affected by the correction. The unit needs a stable identity, owner, dependencies and source pins. Atomization must preserve the surrounding composition: splitting a file does not permit changing its package API or a module's observable behavior. Useful atoms include a bounds predicate, a compiler rewrite, a native ownership rule or a test oracle. Fragmentation without a reusable contract increases bookkeeping rather than knowledge.

**Patternize** expresses the correction as a reusable transformation pattern. The pattern states what matches, what must be true before applying it, and when it must refuse. A fix for a single boundary case remains single-case evidence until broader inputs are qualified. Patternization should connect to existing recipe and donor catalogues, but a catalogue entry supplies a research lead, not automatic permission to copy a solution or a proof of performance.

**Annotate** attaches machine-readable facts: input and output identities, recipe revisions, applicability, fixture references, provenance, execution status, limitations and predecessor links. An annotation may say a cause is hypothesized or a test is NOT_RUN. These are useful explicit states. They must not be converted into confidence by summarization. Machine-readable annotations make the correction searchable and checkable without requiring the next model to infer structure from prose.

**Document** preserves a concise explanation of the failure, diagnosis, change, reusable lesson and remaining uncertainty. Documentation should help another engineer decide what to inspect and what to run. It is not a transcript dump, hidden reasoning trace or replacement for evidence. The four facets share the same atom, pattern, event and evidence identifiers so a reader can move between explanation and proof. A correction lacking any facet remains incomplete as a MOP correction record, even if an isolated patch appears to work.

## 5 Architecture and correction lifecycle

The proposed architecture has five logical responsibilities: a canonical recipe owner; an immutable event store associated with that owner; retained fixtures and evidence; rebuildable machine and human indexes; and an admission evaluator. These are responsibilities, not a requirement for five new services. Existing Synexia repository, catalogue and verification infrastructure should implement them where possible; these recipe and improvement-memory responsibilities remain inside Synexia. The target receiver records which exact canonical recipe and event produced a target change and which target gates were executed.

The lifecycle begins with **recall**. Before changing a recipe, the agent retrieves applicable lessons, unsuccessful approaches, exclusions and revoked conclusions. It compares current source and dependency identities with the recorded preconditions. A stale result remains a lead, not an instruction. The agent then identifies the failure, atomizes the affected behavior, patternizes a proposed repair and changes the owning recipe rather than only its generated output.

Next, the agent records the attempt, regenerates the bounded output and runs the appropriate gates. Annotation and documentation capture actual outcomes, including blocked commands and absent artifacts. Verification may produce further events. Admission occurs only after required evidence supports the declared scope. Later use repeats the applicability check. A new contradiction appends a rejection, narrowing, revocation or supersession event rather than silently rewriting history.

Candidate recipe changes and their initial records should travel together. Where a multi-commit workflow is necessary, the change references its pending record and cannot be represented as admitted until the record and required evidence are present. This is an engineering admission rule, not a permission grant. Repository publication, external execution, credentials and merge decisions continue to require the authorization applicable to those actions.

## 6 Memory representation and integrity

A correction record contains stable recipe identity and schema version; before and after exact-byte source digests; dependency and template pins; the failure and reproducer; root-cause status; the change and lesson; applicability and exclusions; rejected alternatives; actual commands and environment identities; evidence and regression references; replay instructions; and parent or supersession links. Unknown values remain unknown. Missing tests are NOT_RUN or BLOCKED, not implicitly passed. An absent log is not replaced by an invented count or hash.

The authoritative history is append-only. Human-readable lessons and search indexes are materialized views that can be rebuilt from retained events. An implementation may use one event per file under the existing recipe owner, provided it preserves equivalent guarantees. The operational profile specifies deterministic UTF-8 JSON serialization, ordered object keys, schema-defined ordering for set-like lists, and restrictions on numeric values. Exact-byte hashes for source artifacts remain distinct from hashes of canonical event payloads.

The event identifier is the SHA-256 digest of its canonical payload. The payload excludes its own digest, avoiding a circular definition; the identifier is carried in a filename or envelope. Parent identifiers form a history graph. Integrity validates that bytes match an identity, not that the content is true or the author is authorized. Verification must therefore check both artifact consistency and the actual claim supported by the evidence.

Concurrent writers publish separate events and retain parent relationships. A later reconciliation event can join valid branches without overwriting either attempt. Readers must not mistake partially written files for admitted records. Atomic publication is useful where supported, but an ordinary replacement fallback is not unconditionally atomic. A writer must fail closed or use a documented recoverable protocol that keeps partial state out of admission. Missing parents, unknown schema versions and inconsistent pins block admission rather than trigger an optimistic guess.

## 7 Worked example of a boundary correction

Consider a synthetic recipe that rewrites a half-open string-slice bounds check. The declared contract permits an empty slice at the end of the string. An illustrative old predicate rejects the start position when it equals the length, even though an empty end-boundary slice is valid. This example describes how the proposed memory would work; it reports no executed test, existing bug or measured result in M3JDK21.

The agent first recalls earlier bounds lessons and checks that the current API really uses half-open ranges. It atomizes the predicate and identifies the generated source owner. It patternizes a correction that admits the valid empty boundary while retaining negative-index, reversed-range and beyond-end refusals. It rejects a hand-edit to the generated file because regeneration would discard the repair. The event records this alternative and its reason, rather than leaving that knowledge only in chat.

The first record is RECORDED with a hypothesized diagnosis and pending source pins. Compile, regression, replay and JNI checks are NOT_RUN. Once real artifacts exist, subsequent evidence records would identify their exact hashes and results. The minimum fixture set includes empty input, an empty slice at the start and end, the full range, reversed indices, negative positions and an end beyond the length. If the same transformation can reach native code, native lifetime and bounds behavior require separate qualification.

Admission would require evidence that the old behavior fails the relevant oracle, the corrected recipe generates the expected output, public behavior remains compatible and repeat application reaches the required fixed point. A later agent working on an inclusive-end API must refuse to reuse the lesson directly. The remembered improvement is not “allow equality everywhere”; it is a precisely scoped rule with an attached contract and evidence boundary.

## 8 Failure and trust boundaries

Negative knowledge must remain retrievable. A failed recipe variant, disproved diagnosis or rejected donor prevents repeated exploration only when its applicability and reason survive. Failure records should distinguish a test assertion from missing dependencies, inaccessible logs or a workflow that never executed. Source inspection and tests against API-shaped stand-ins are not equivalent to compilation and execution against real owning dependencies. Merge ancestry is not proof that source content survived integration.

Contradictory lessons must not be resolved merely by choosing the newest timestamp. A reconciliation decision compares scope, versions and evidence, then appends the bounded outcome. If two results apply to different environments, both may remain valid. If the conflict cannot be resolved, admission is blocked for the disputed scope. Rollback and supersession preserve the earlier record and identify what changed; they do not retroactively claim that an old failed attempt succeeded.

Memory is evidence, not authority. Logs, donor comments, example prompts and retrieved records may contain instructions, but they cannot override trusted repository policy or user authorization. An ADMITTED event does not authorize a merge or credential use. Public memory should retain concise factual rationale rather than private conversations or hidden reasoning. Credentials, private prompts and unnecessary personal information must be excluded. If secrets are captured accidentally, authorized security removal takes precedence over a simplistic append-only rule.

## 9 Java and JNI integration boundaries

Inside Synexia, Java 21 can provide deterministic validation, serialization, hashing, indexing and retrieval through existing recipe and provenance owners. Java 21 identifies the implementation language; it does not place this subsystem in the JDK or another public product fork. This is a proposed implementation direction, not a statement that such a complete memory subsystem has shipped. Array-backed representations, offsets and masks may support internal storage, but choosing them does not relax public contracts or eliminate the need to measure retained heap and native memory.

For JNI-related recipes, evidence must identify Java and native source revisions, compiler and flags, ABI, target and runtime conditions. Preconditions must include relevant array ownership, pinning or copying behavior, bounds, lifetimes, mutation visibility and exception handling. A Java-side pass cannot establish native safety, and one native platform's results cannot be propagated to other backends. Target qualification must retain its own exact receipts while linking the canonical recipe event.

M3JDK21 remains the integrated product rather than an “M3 plus JDK” overlay. Reusable recipe corrections return to Synexia's canonical convergence history; target runtime adaptation remains owned by the JDK receiver. This separation allows reusable learning without importing a mandatory runtime service or creating a second canonical memory authority [1–3].

## 10 Evaluation programme

The main hypothesis is that evidence-linked recipe memory reduces repeated correction effort while preserving correctness. A second hypothesis is that explicit negative lessons reduce recurrence of previously rejected approaches. A third is that scoped retrieval rejects stale lessons more reliably than unqualified conversational recall. These hypotheses require experiments; this paper supplies no measured speedup, success rate or comparative result.

An evaluation should compare the same pinned task corpus under three conditions: no persistent correction memory; a prose-only lesson summary; and structured recipe-owned memory with evidence and applicability checks. Use comparable models, prompts, tool access and budgets, repeat trials, and separate held-out correction tasks from the records used to prepare memory. Include boundary errors, dependency drift, donor incompatibility, JNI ownership changes and contradictory historical lessons.

Measure verified task completion, recurrence of known failures, invalid lesson reuse, human review effort, tool calls, elapsed time and resource cost. Also measure record completeness, retrieval coverage, stale-lesson refusal, replay agreement and recovery after interrupted publication. Report distributions and failures, not only selected successful examples. Correctness and evidence integrity are gates; a faster run that bypasses them is not an improvement. Full-JDK claims require the relevant product tests and installed-image evidence, beyond a finite recipe corpus.

## 11 Limitations and relationship to established practice

The proposal combines familiar engineering practices: version control preserves changes; regression tests detect recurrence; provenance binds artifacts to origins; event histories preserve transitions; and retrieval exposes prior knowledge. MOP does not replace these mechanisms. It requires that their relevant outputs travel with the owning recipe so that an agent can recover both the reusable correction and the limits under which it was qualified.

The design cannot make an incorrect oracle correct, infer missing permissions, guarantee exhaustive coverage or prove global improvement from finite tests. Memory may grow, become expensive to retrieve, or contain misleading explanations despite intact hashes. Summaries may omit decisive exclusions. The proposed countermeasures are bounded scope, rebuildable indexes, explicit revocation and negative records, independent checks where warranted, and evaluation of retrieval failures. Empirical implementation and qualification remain necessary.

## 12 Conclusion

A recipe should carry forward the engineering knowledge created when it is corrected. In Synexia MOP, Atomize, Patternize, Annotate and Document turn a transient repair into a reusable, evidence-linked work product. The persistent unit is the recipe and its correction history, not the model session. Later agents inherit inspectable learning, including failed attempts and restrictions, while current contracts and authorization remain authoritative. The next implementation task is to integrate this contract into the existing canonical recipe and verification owners and demonstrate it with reproducible, scoped evidence.

## Proposed Java interface contract

The following Java 21 boundary sketch makes the interaction explicit. Its implementation and execution owner is exclusively the Synexia convergence workspace, not M3JDK21 or another public product fork. This documentation-only sketch is a proposed interface, not an installed SDK API, executable recipe or storage implementation. `appendEvent` handles correction, verification, admission, rejection, supersession and revocation by appending events; no method edits an old event in place. An adapter must implement exact profile validation, expected-frontier comparison, durable publication and bounded retrieval through the existing canonical owner. Constructor null checks and immutable list snapshots are not substitutes for those semantic checks.

The expected revision is an optimistic-concurrency token, and the parent digests describe the event frontier observed by the caller. A stale token or incompatible parent set must be refused, allowing the caller to retrieve and reconcile without losing another writer's history. Errors, interruption, conflicts and successful durable appends must remain distinguishable in a concrete binding. The eventual owning API must define typed failure outcomes and retry/idempotency semantics; this sketch does not pretend that an unspecified exception contract already provides them.

`MemoryView` supplies evidence identifiers, not executable instructions or a blanket list of trusted fixes. An incomplete view cannot qualify a correction. The implementation must retain relevant rejected and revoked evidence and follow the normative MOP-RCM/1.0 serialization profile in the companion specification, rather than invent a second encoder. The Java sketch has not been compiler-qualified in this publication; it remains subject to owning-module compile and behavioral tests.

```java
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.util.List;
import java.util.Objects;

/** Proposed boundary contract; no storage implementation is supplied. */
public interface RecipeImprovementMemory {
    enum EventKind {
        RECORDED, VALIDATED, ADMITTED, REJECTED, SUPERSEDED, REVOKED
    }

    record RecipeKey(String canonicalOwner, String recipeId) {
        public RecipeKey {
            Objects.requireNonNull(canonicalOwner);
            Objects.requireNonNull(recipeId);
        }
    }

    record ExpectedFrontier(String revision, List<String> parentDigests) {
        public ExpectedFrontier {
            Objects.requireNonNull(revision);
            parentDigests = List.copyOf(parentDigests);
        }
    }

    record EventDraft(EventKind kind, String canonicalPayload) {
        public EventDraft {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(canonicalPayload);
        }
    }

    record AppendReceipt(String eventDigest, String newRevision) {
        public AppendReceipt {
            Objects.requireNonNull(eventDigest);
            Objects.requireNonNull(newRevision);
        }
    }

    record LessonQuery(String recipeSourceDigest,
                       List<String> applicabilityTerms) {
        public LessonQuery {
            Objects.requireNonNull(recipeSourceDigest);
            applicabilityTerms = List.copyOf(applicabilityTerms);
        }
    }

    record MemoryView(String observedRevision, List<String> eventDigests,
                      boolean completeForQuery) {
        public MemoryView {
            Objects.requireNonNull(observedRevision);
            eventDigests = List.copyOf(eventDigests);
        }
    }

    /** Validates MOP-RCM/1.0 and appends without replacing prior events.
     *  Refuses a stale expected frontier; returns only after durable publication.
     *  kind must agree with the canonical payload's event_type. */
    AppendReceipt appendEvent(RecipeKey recipe, ExpectedFrontier expected,
                              EventDraft event);

    /** Includes applicable negative, rejected, revoked and superseding evidence.
     *  An incomplete view cannot authorize admission or unconditional reuse. */
    MemoryView retrieveApplicable(RecipeKey recipe, LessonQuery query);

    /** Returns a validated canonical payload for an exact retained digest.
     *  Missing or invalid evidence is an error, never an invented empty event. */
    String readEvent(RecipeKey recipe, String eventDigest);
}
```

## Copyright attribution and publication scope

Copyright 2026 Hitesh Soliwal and contributors. Synexia — Machine-Oriented Programming (MOP). This paper records the project's authored expression and proposed design. It does not establish legal registration, patent rights, exclusive invention of every component, or ownership of the abstract programming method. Copyright protection concerns qualifying original expression rather than ideas, procedures or methods of operation; jurisdiction and authorship can affect protection [5,6]. The Apache-2.0 grant for this original paper remains effective. OpenJDK, donors and other third-party material retain their own rights and licenses, and a recipe's license does not relicense its inputs or generated output [3,4,7].

## References

1. M3JDK21. *Agent instructions*. Repository baseline 73f59218dc4ef1a3ef79aad990e77273db2a2d60. https://github.com/hsoliwal/M3jdk21/blob/73f59218dc4ef1a3ef79aad990e77273db2a2d60/AGENTS.md
2. M3JDK21. *Porting and naming invariant*. Same baseline. https://github.com/hsoliwal/M3jdk21/blob/73f59218dc4ef1a3ef79aad990e77273db2a2d60/m3/docs/m3jdk21-porting-invariant.md
3. M3JDK21. *Recipe atom evidence and DAG contract*. Same baseline. https://github.com/hsoliwal/M3jdk21/blob/73f59218dc4ef1a3ef79aad990e77273db2a2d60/m3/docs/recipe-atom-evidence-and-dag.md
4. M3JDK21. *M3 and Synexia First-Party Rights and Provenance Notice*. Same baseline. https://github.com/hsoliwal/M3jdk21/blob/73f59218dc4ef1a3ef79aad990e77273db2a2d60/M3-SYNEXIA-RIGHTS.md
5. World Intellectual Property Organization. *What can I protect with copyright*. https://www.wipo.int/en/web/copyright/protection
6. World Intellectual Property Organization. *Frequently Asked Questions on Copyright*. https://www.wipo.int/en/web/copyright/faq-copyright
7. Apache Software Foundation. *Apache License Version 2.0*. https://www.apache.org/licenses/LICENSE-2.0

Companion operational specification: [Recipe owned correction memory invariant](../docs/mop-recipe-improvement-memory.md). The paper and specification describe the design and required workflow; publication or merge alone does not establish implemented runtime behavior.
