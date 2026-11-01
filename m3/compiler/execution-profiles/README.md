<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK execution-profile artifact contract

**Invariant ID:** `M3JDK-EXECUTION-PROFILE-ARTIFACT-1`  
**Format:** `M3-EXECUTION-PROFILE/1`  
**Status:** tooling-only specification and deterministic bundle implementation; runtime capture and profile consumption remain separate gated leaves.

## 1. Ownership boundary

This module owns **dynamic execution-profile artifacts**: immutable observations produced by a named workload against an exact runtime revision, plus deterministic validation and set-union merging. It is a sibling of `m3/compiler/source-signals`, not an extension of that owner. Source signals remain attributed source-time facts; execution profiles remain runtime observations. Neither is silently promoted into Java/JDK behavior.

Existing HotSpot owners remain unchanged:

- `src/hotspot/share/cds/**` owns CDS archive construction and loading;
- `src/hotspot/share/compiler/**` owns runtime compilation policy, queues and compiler services;
- `m3/compiler/source-signals/**` owns static javac source facts;
- this module owns only the portable profile **artifact contract** under `m3/compiler/execution-profiles/**`.

No file under `src/**`, no Java SE API, no class-file contract, no JVM behavior and no JNI ABI is modified by this leaf.

## 2. Donor boundary

Excelsior JET publicly documented Test Run discovery, startup/reflection profiles, profile-guided optimization, cross-target profiling images and persistent project databases. Those ideas nominate architecture candidates through the parent donor catalogue. This module is an independent M3 specification and implementation; it contains no Excelsior source, binary format, decompilation result or mechanical translation.

BiSheng's JIT profile-cache implementation remains a separate compatible-lineage `PATCH` candidate. This artifact format does not claim to be BiSheng's cache format and does not import that implementation.

## 3. Bundle contents

A bundle is one directory containing exactly the two normative tables:

```text
BUNDLE.tsv
OBSERVATIONS.tsv
```

`BUNDLE.tsv` contains one metadata row:

```text
bundle_id
format_version
runtime_commit
workload_sha256
capture_target
deployment_target
producer
producer_version
```

`OBSERVATIONS.tsv` contains zero or more sorted observations:

```text
observation_id
kind
subject
context
phase
count
evidence_ref
evidence_sha256
```

No wall-clock timestamp participates in either identity. A runtime capture tool may retain time in a separate evidence artifact, but the portable semantic bundle is deterministic.

## 4. Identity and evidence

Every observation ID is derived from its complete canonical row excluding the ID:

```text
m3o1:SHA-256("M3-EXECUTION-OBSERVATION/1\n" + canonical-fields + "\n")
```

Every bundle ID is derived from its complete metadata excluding the ID plus every exact sorted observation row:

```text
m3p1:SHA-256("M3-EXECUTION-PROFILE/1\n" + canonical-metadata + observations)
```

Consequences:

- source mutation, count mutation, phase mutation or evidence mutation changes identity;
- duplicate IDs are impossible in a valid table;
- a supplied ID that does not match its content is refused;
- an observation is evidence-bound by a normalized relative `evidence_ref` and lowercase SHA-256;
- the validator does not claim the raw evidence file is locally present unless a later custody gate proves it.

## 5. Closed observation vocabulary

The initial artifact contract represents only portable facts:

- `CLASS_LOAD`
- `CLASS_INITIALIZE`
- `REFLECTIVE_MEMBER`
- `DYNAMIC_PROXY_INTERFACE`
- `SERVICE_PROVIDER`
- `RESOURCE_ACCESS`
- `NATIVE_LIBRARY_LOAD`
- `COMPILED_METHOD`
- `CALL_EDGE`
- `STARTUP_MARKER`

A `subject` is a canonical ASCII descriptor owned by the producer, for example:

```text
jvm:class:java/lang/String
jvm:method:java/lang/String#indexOf(I)I
jvm:field:example/Config#value:Ljava/lang/String;
resource:META-INF/services/example.Provider
native:library:zip
```

`context` is another canonical descriptor or `-`. Phases are `STARTUP`, `STEADY_STATE`, `SHUTDOWN` or `UNSCOPED`. Counts are positive signed 64-bit values and are not silently combined during merge.

## 6. Merge law

A merge is permitted only when all inputs agree on:

- format version;
- exact runtime commit;
- workload SHA-256;
- capture target;
- deployment target.

Producer identities may differ. The merged producer is fixed to `m3-execution-profile-merger` version `1`.

Merge is a sorted set union by content-derived `observation_id`:

```text
merge(A, A) = A'                 idempotent content
merge(A, B) = merge(B, A)       commutative content
merge(merge(A, B), C)
  = merge(A, merge(B, C))        associative content
```

`A'` may have merger metadata rather than capture-producer metadata, but re-merging it reaches a byte-identical fixed point. Identical observations collapse; different evidence or counts remain different observations. No count is summed, no hotness threshold is inferred, and no profile overrides runtime correctness.

## 7. Safety invariant

A profile is optimization and packaging evidence, never sole correctness authority. In particular:

- absence from a profile does not prove code, reflection, services, resources, JNI or dynamic loading are unreachable;
- a consumer must preserve a fallback or separately prove a closed world before omission;
- runtime, workload, capture target or deployment target mismatch invalidates direct reuse;
- stale or malformed identities fail closed;
- profiles cannot unlock public contracts or authorize an incompatible donor import.

## 8. CLI

```sh
# Validate one bundle
java -cp <classes> jdk.m3.compiler.profiles.M3ExecutionProfileCli \
  validate /path/to/bundle

# Deterministically merge two or more compatible bundles
java -cp <classes> jdk.m3.compiler.profiles.M3ExecutionProfileCli \
  merge /path/to/output /path/to/input-a /path/to/input-b
```

The CLI writes no file outside the explicit output directory. It refuses a nonempty output directory unless its two normative files already describe the exact output bundle.

## 9. Verification

```sh
bash m3/compiler/execution-profiles/verify.sh
```

The ordered verifier performs:

1. Java 21 compilation with `-Xlint:all -Werror`;
2. strict Javadoc;
3. dependency-free positive and refusal tests;
4. independent merge executions;
5. byte-level fixed-point comparison;
6. deterministic validation and receipt emission.

The tests cover malformed IDs and hashes, unsorted and duplicate rows, traversal, incompatible runtime/workload/targets, zero and overflow counts, unknown vocabulary, evidence mutation, commutativity, associativity, idempotence and nonempty-output refusal.

## 10. Explicit holds

This leaf does **not** implement:

- HotSpot/JFR/runtime event capture;
- CDS/AppCDS class-list generation or archive consumption;
- JIT profile-cache import/export;
- reachability closure;
- AOT/native-image compilation;
- profile-guided compiler policy;
- runtime trimming or packaging omission;
- incremental native compiler state.

Those are later leaves that may consume this format only after their own contract, implementation and product evidence are admitted.
