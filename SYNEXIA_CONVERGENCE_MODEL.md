# Synexia Convergence / Delivery Model

**Repository role:** DELIVERY_TARGET  
**Convergence workspace:** `hsoliwal/com.synexia`

This repository is a downstream delivery target. Synexia is the working/convergence project where
architecture, donor comparison, M3/OpenRewrite recipes, JNI/Java implementations, precompute
layers, MIndex/M3 mappings, collection/storage mechanics, tests, benchmarks, and proof receipts are
allowed to evolve until they converge.

This repository receives the **polished, verified export** of that converged work. It is not the
canonical experimentation workspace.

## Authority direction

```text
Synexia
 inventory -> atomize -> compare -> recipe -> implement -> verify -> converge -> seal
                                      |
                                      v
                 recipe + source revision + hashes + receipts
                                      |
                                      v
                             this target repository
                                      |
                                      v
                         target-specific verification
```

Target-specific adaptations may exist, but they do not silently redefine Synexia architecture.
A useful target discovery flows back as explicit evidence/proposal to Synexia and becomes canonical
only after the Synexia convergence/verification process.

## Delivery admission

A Synexia-derived change should identify:

- Synexia source revision;
- Maven/OpenRewrite recipe or task-crate identity;
- exact source/postimage hashes;
- verification receipts;
- deliberate target-specific adaptations;
- unresolved gaps.

Do not claim that this target contains the polished Synexia result unless those artifacts exist and
this repository's own build/tests/runtime verification pass.

## M3 String / precompute / JNI rule

For M3 String-derived delivery, preserve the converged layering:

```text
M3String / M3Text / M3CompositeString
        |
        v
MIndex/M3 mapping + String shadow ABI
        |
        v
precompute / metadata / search facts
        |
        v
canonical logical IDs and zero-copy views
        |
        v
JNI/native storage and execution substrate
```

Precompute is semantic memory above storage; it must not be bypassed by wiring String directly to
JNI. Java `byte[]`, `char[]`, and similar primitive-array surfaces are compatibility/ingress/export
projections when the converged Synexia owner is native-backed. Joined arrays/strings are
descriptor/ID composition where supported; flattening is an explicit boundary.

## M3 recipe-first rule

For Synexia-derived changes, prefer the exported Maven/OpenRewrite recipe and its sealed postimages
over hand-editing target files. Preserve public APIs, signatures, contracts, serialization and
observable behavior unless a target-specific change explicitly unlocks them.

Verification remains ordered:

```text
diff -> lint/static analysis -> compile -> recipe replay/refusal/fixed point
     -> tests -> JNI/native runtime -> target runtime
```

## Local contract

This policy does not make Synexia a runtime dependency and does not weaken this repository's own
public API, compatibility, build, security, or release requirements. It defines where converged
implementation authority originates and how it is admitted here.
