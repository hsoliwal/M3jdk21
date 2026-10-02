# Synexia canonical intake into M3jdk21

Status: documentation-only downstream gate. No Synexia production source is ported by this document.

## Ordering rule

M3jdk21 consumes **accepted Synexia exports**, not arbitrary Synexia branches, PRs, files or classes.

The required order is:

```text
Synexia full-tree inventory
 -> atomization
 -> patternization
 -> recipe-first consolidation inside Synexia
 -> repeated re-inventory / convergence passes
 -> verified fixed point
 -> Synexia repository completion complete=true
 -> per-capability promotion + atom/pattern coverage
 -> M3SynexiaCanonicalExport
 -> M3jdk21 source-to-target mapping
 -> route-specific JDK adaptation recipe
 -> JDK compatibility / build / runtime acceptance
```

The upstream export is evidence that Synexia accepted a capability. It is not evidence that the same representation is valid inside the JDK.

## Required upstream receipt

The intake must bind an exact Synexia source repository, commit and Git tree plus a content-addressed canonical export root. Each exported capability must carry:

- stable capability ID;
- source path and fully qualified symbol;
- contract SHA-256;
- implementation SHA-256;
- source atom root;
- pattern root;
- atom/pattern coverage root;
- promotable recipe receipt;
- provenance/license evidence;
- repository-completion root.

Reject exports whose repository completion is incomplete, whose capability promotion failed, whose atom/pattern evidence is absent, whose source commit/tree moved, or whose capability identity conflicts with an existing mapping owner.

## M3jdk21 intake procedure

For every exported capability:

1. Resolve or create the stable entry in the existing M3jdk21 mapping authority. Do not create a second registry.
2. Identify the exact JDK subsystem, public/internal contracts, VM/native consumers, build modes and platform variants affected.
3. Select Route A, B or C independently. An accepted Synexia Route-A implementation is not automatic evidence for compiler lowering or a custom JDK backend.
4. Preserve JDK semantics first. Adapt storage, lifetime, bootstrap dependencies, identity and exceptions before reusing the upstream implementation.
5. Create or improve the reusable transformation recipe/work packet and bind it to the Synexia export root plus JDK source preimages.
6. Run differential API tests, compiler/static-analysis gates, serialization/ABI/JMM/native gates where applicable, and exact-image runtime acceptance for Route C.
7. Record target adaptations and update the existing mapping alongside the port.
8. Only after JDK acceptance may the capability be marked retained/verified for this target revision.

## Atomization and patternization in the JDK

Synexia atom/pattern identities are source provenance and reuse evidence. JDK target atoms are separately inventoried because package, module, bootstrap, visibility, VM and native contracts differ.

A matching pattern can choose a likely adapter or recipe family, but cannot prove equivalence. Pattern transfer must preserve:

- public/protected API and binary linkage;
- Java object identity and equality domains;
- evaluation order, exceptions and synchronization;
- serialization and native formats;
- JMM guarantees;
- bootstrap dependency closure;
- VM/GC/JIT/JNI/JVMTI/serviceability assumptions where touched.

## Current state

The Synexia canonical-export gate is being established in Synexia draft PR #7730. The checked-in Synexia repository-completion evidence still reports `LEAF_VERIFIED_DRAFT`, with local Maven/full-reactor/final hosted scheduler evidence incomplete.

Therefore the M3jdk21 intake is currently **blocked before source porting**. This is intentional. No branch-local Synexia implementation should be described as canonically contributed to M3jdk21 until the upstream export gate actually opens.
