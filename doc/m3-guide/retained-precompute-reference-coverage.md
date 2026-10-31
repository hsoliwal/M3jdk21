<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

# Retained typed precompute: source, member and evidence coverage

[Guide](README.md) · [Contract reference](retained-precompute-reference.md) · [Custody inventory](packages/custody-01.md)

## Scope and counting

Exactly eight primary Java files, 696 lines, are routed here at public source pin
`363c02caa2e5af1df9460b927ecd78f6a9dbcefb`. Every assigned source body, explicit
member and nested implementation was read. This is bounded documentation coverage,
not executed code coverage, full dependency closure or product qualification.

The public-type surface has 39 explicitly declared callables: 1 Artifact,
6 Context, 9 Key, 1 Limits, 6 Pass, 4 Passes, 7 Plan plus 2 Builder, and 3 Scope.
This count includes compact constructors and interface declarations. Four public
overrides inside non-public nested types (`Work.close` and three `FunctionalPass`
methods) are separately routed as support. Generated record accessors/object
methods and the implicit `Builder()` constructor are listed separately. Nested
types do not add physical primary files. There is one explicit public constant,
`Limits.DEFAULT`.

All eight physical file hashes match the historical v7 export. No regex, filesystem,
AST, shared-array, compatibility-fixture or port source is counted as an additional
primary owner. These bodies are retained source, not a complete module or a newly
exported JDK API. Builds, tests, examples, scripts, native/library loading,
benchmarks and runtime/policy qualification are **NOT_RUN in this documentation
authoring/review pass**. Hosted exact-commit CI is separate evidence and is not
reported by that status.

## 1. Exact primary source and hash manifest

The full path prefix for every row below is
`m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/`.
Git blob SHA-1 identifies the Git object including its blob header; SHA-256 below
is over raw file bytes. Neither label is a semantic proof. Each file maps to its
named contract section and the per-member routes below.

| Physical source | Bytes / lines | Git blob SHA-1 | Raw SHA-256 | Contract |
|---|---:|---|---|---|
| [MIndexPrecomputeArtifact.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeArtifact.java#L1-L21) | 751 / 21 | `2951a0e0d8254d57fc4cede3e568f2e8282244c1` | `f28df39a3e247c22fe54fcfec9eaf749d490943b233a36148bf22611e3d18ba0` | [MIndexPrecomputeArtifact](retained-precompute-reference.md#artifact-validation-and-ownership) |
| [MIndexPrecomputeContext.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L1-L76) | 2509 / 76 | `8945a79b5d6f3e3b19c7fffe550092b99423f1cf` | `702fdd1c02a6ce17a1e63459c6392d1bd1d1a50367e2e5a0cd58042b3ebd1937` | [MIndexPrecomputeContext](retained-precompute-reference.md#5-context-exposes-only-available-direct-dependencies) |
| [MIndexPrecomputeKey.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L1-L74) | 2140 / 74 | `52ffe7b8895e4db3dffdde2d3fb7deeeecb779fb` | `fab40b3eee67ed7bc377befb73357d1358c0e52cfb0940d92fc8e988d274376d` | [MIndexPrecomputeKey](retained-precompute-reference.md#typed-keys-and-class-loader-identity) |
| [MIndexPrecomputeLimits.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L1-L54) | 1857 / 54 | `663ff20f394e4fb190341bb4e66b37df16369af4` | `51cc3fea9d768172564d4870c7c006a574a64a250218eda25bfb63cc0620c095` | [MIndexPrecomputeLimits](retained-precompute-reference.md#6-limits-primitive-accounting-and-work-lifecycle) |
| [MIndexPrecomputePass.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java#L1-L28) | 849 / 28 | `4c8176e37704f7d28faf24f043c6e5273e9635ec` | `4494b40289d89f17c0cc1dd6b8aff313090d63f88973cbe6c47282899ecb662d` | [MIndexPrecomputePass](retained-precompute-reference.md#the-interface-and-factory-wrappers) |
| [MIndexPrecomputePasses.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L1-L98) | 3341 / 98 | `8dfe2ab9cc99da76a4f3eb5b5d41b6b0b2c7189f` | `8973ba20e7c524b0cef2e10250a84f69297b340012d734e260b0459cc8cd90e0` | [MIndexPrecomputePasses](retained-precompute-reference.md#the-interface-and-factory-wrappers) |
| [MIndexPrecomputePlan.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L1-L272) | 9719 / 272 | `2d88332229d04df383e21db1f907b2a8f95d9beb` | `c1011fcc84663e1b93db824b7eb706d63a4b8d4ae6ab88bc81a31d68a363cd8a` | [MIndexPrecomputePlan](retained-precompute-reference.md#4-building-and-inspecting-a-graph) |
| [MIndexPrecomputeScope.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L1-L73) | 2684 / 73 | `712e219585561967dc3152e1323368c2d87dffb8` | `32e0508e5502ba50b9ea833cd7193f821122a935f72a7ae634c3a12733a0e3a9` | [MIndexPrecomputeScope](retained-precompute-reference.md#scope-fields-and-revisions) |

## 2. Every-member routing

Each range is bounded to the named declaration/body or the record/class declaration
that supplies generated behavior. “Support” means private/package-private or a
public override enclosed by a non-public implementation type; it is not an extra
public construction API. Record-generated methods have no separate handwritten
body in the source. Fields are named together where declared adjacently.

### MIndexPrecomputeArtifact

| Member or state | Kind | Exact source | Contract / limit |
|---|---|---|---|
| `record and components key/value/inputSha256/contentSha256/retainedBytes` | state or type | [L7–L12](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeArtifact.java#L7-L12) | Typed payload and supplied metadata; references retained |
| `MIndexPrecomputeArtifact<T>(...) compact constructor` | public explicit callable | [L14–L20](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeArtifact.java#L14-L20) | Null/type, normalized digest and nonnegative-weight checks, in source order |
| `key(), value(), inputSha256(), contentSha256(), retainedBytes(); equals(Object), hashCode(), toString()` | generated surface | [L7–L12](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeArtifact.java#L7-L12) | Five component accessors and generated record methods; no deep clone |

### MIndexPrecomputeContext

| Member or state | Kind | Exact source | Contract / limit |
|---|---|---|---|
| `scope, limits, plan, artifacts, passRow, monitor` | state or type | [L8–L14](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L8-L14) | Retained references and current row |
| `MIndexPrecomputeContext(...)` | support callable | [L16–L29](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L16-L29) | Package-private; row bounds and null checks; array retained; null-monitor normalization |
| `scope()` | public explicit callable | [L31–L33](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L31-L33) | Retained scope |
| `limits()` | public explicit callable | [L35–L37](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L35-L37) | Retained declared limits |
| `monitor()` | public explicit callable | [L39–L41](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L39-L41) | Retained or no-op monitor |
| `contains(key)` | public explicit callable | [L43–L46](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L43-L46) | Direct declared dependency plus non-null row; no payload validation |
| `require(key)` | public explicit callable | [L48–L50](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L48-L50) | Delegates to artifact and returns payload |
| `artifact(key)` | public explicit callable | [L52–L68](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L52-L68) | Undeclared/unavailable refusal; casts and rewraps available value |
| `allowed(row)` | support callable | [L70–L75](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L70-L75) | Linear scan of direct dependency interval |

### MIndexPrecomputeKey

| Member or state | Kind | Exact source | Contract / limit |
|---|---|---|---|
| `class and id/type/schemaVersion fields` | state or type | [L12–L15](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L12-L15) | Typed immutable key metadata |
| `MIndexPrecomputeKey(id,type,schemaVersion)` | support callable | [L17–L22](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L17-L22) | Private validation; nonblank ID, class non-null, positive schema |
| `of(id,type,schemaVersion)` | public explicit callable | [L24–L27](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L24-L27) | Construct validated key |
| `id()` | public explicit callable | [L29–L31](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L29-L31) | Original ID |
| `type()` | public explicit callable | [L33–L35](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L33-L35) | Actual Class object |
| `schemaVersion()` | public explicit callable | [L37–L39](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L37-L39) | Positive schema number |
| `canonicalName()` | public explicit callable | [L41–L43](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L41-L43) | NUL-separated ID/class name/decimal schema |
| `compareTo(other)` | public explicit callable | [L45–L52](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L45-L52) | ID, class name, numeric schema; null refusal |
| `equals(other)` | public explicit callable | [L54–L62](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L54-L62) | ID, actual Class equality and schema |
| `hashCode()` | public explicit callable | [L63–L68](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L63-L68) | ID, Class hash and schema |
| `toString()` | public explicit callable | [L70–L73](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L70-L73) | Human-readable ID/simple name/version |

### MIndexPrecomputeLimits

| Member or state | Kind | Exact source | Contract / limit |
|---|---|---|---|
| `record components and DEFAULT` | state or type | [L7–L10](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L7-L10) | Five maxima and exact default values |
| `MIndexPrecomputeLimits(...) compact constructor` | public explicit callable | [L12–L16](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L12-L16) | Positive node/cell/work/byte limits; nonnegative edges |
| `maxNodes(), maxEdges(), maxCells(), maxWork(), maxBytes(); equals(Object), hashCode(), toString()` | generated surface | [L7–L8](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L7-L8) | Five component accessors and generated record methods |
| `bytes(bytes)` | support callable | [L18–L20](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L18-L20) | Package-private nonnegative max-byte check; no allocation |
| `arrayLength(length)` | support callable | [L22–L27](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L22-L27) | Package-private 0..Integer.MAX_VALUE-8 guard |
| `Work; monitor, maximum, used` | state or type | [L29–L32](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L29-L32) | Package-private nested AutoCloseable; mutable counter; no closed flag |
| `Work(limits,monitor,name)` | support callable | [L34–L39](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L34-L39) | Normalize monitor, cancellation before beginTask UNKNOWN |
| `Work.charge(amount)` | support callable | [L41–L48](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L41-L48) | Cancellation, subtraction budget guard, increment, worked callback |
| `Work.close()` | support callable | [L50–L52](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L50-L52) | Public override on non-public type: cancellation then finally done |

### MIndexPrecomputePass

| Member or state | Kind | Exact source | Contract / limit |
|---|---|---|---|
| `MIndexPrecomputePass<T> interface` | state or type | [L6–L13](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java#L6-L13) | Deterministic/scope contract; enforcement belongs to implementation |
| `output()` | public explicit callable | [L14–L14](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java#L14-L14) | Abstract typed output key |
| `dependencies()` | public explicit callable | [L16–L18](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java#L16-L18) | Default empty immutable list |
| `implementationRevision()` | public explicit callable | [L20–L21](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java#L20-L21) | Abstract stable mechanics revision |
| `compute(context)` | public explicit callable | [L23–L23](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java#L23-L23) | Abstract computation |
| `retainedBytes(value)` | public explicit callable | [L25–L25](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java#L25-L25) | Abstract retained primitive-weight declaration |
| `contentSha256(value)` | public explicit callable | [L27–L27](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java#L27-L27) | Abstract content identity declaration |

### MIndexPrecomputePasses

| Member or state | Kind | Exact source | Contract / limit |
|---|---|---|---|
| `MIndexPrecomputePasses utility class` | state or type | [L10–L11](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L10-L11) | Factories and hash helpers |
| `MIndexPrecomputePasses()` | support callable | [L12–L12](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L12-L12) | Private utility constructor |
| `of(output,revision,dependencies,compute,retainedBytes,contentSha256)` | public explicit callable | [L14–L28](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L14-L28) | Construct private validating FunctionalPass wrapper |
| `fixed(output,revision,compute,retainedBytes,contentSha256)` | public explicit callable | [L30–L43](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L30-L43) | of with no dependencies; not memoization |
| `sha256(bytes)` | public explicit callable | [L45–L47](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L45-L47) | Null guard then Scope raw-byte SHA-256 |
| `sha256Utf16(text)` | public explicit callable | [L49–L59](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L49-L59) | Exact length-framed code units; direct input reads |
| `FunctionalPass<T>; output, implementationRevision, dependencies, computer, retainedBytesFunction, contentSha256Function` | state or type | [L61–L68](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L61-L68) | Private nested record, retained callback references |
| `FunctionalPass compact constructor` | support callable | [L70–L77](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L70-L77) | Validate output/revision/callbacks, copy dependency list |
| `FunctionalPass.compute(context)` | support callable | [L79–L83](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L79-L83) | Public override on private type; callback/null/type guard |
| `FunctionalPass.retainedBytes(value)` | support callable | [L85–L90](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L85-L90) | Public override on private type; cast/callback/nonnegative check |
| `FunctionalPass.contentSha256(value)` | support callable | [L92–L96](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L92-L96) | Public override on private type; cast/callback/digest validation |
| `FunctionalPass output()/implementationRevision()/dependencies(); computer()/retainedBytesFunction()/contentSha256Function(); equals(Object)/hashCode()/toString()` | generated surface | [L61–L68](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L61-L68) | Generated nested-record accessors; first three implement pass interface; non-public enclosing type |

### MIndexPrecomputePlan

| Member or state | Kind | Exact source | Contract / limit |
|---|---|---|---|
| `MIndexPrecomputePlan; keys, passes, dependencyStart, dependencyRows, executionOrder, fingerprint` | state or type | [L19–L25](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L19-L25) | Retained graph arrays, pass references and cached fingerprint |
| `MIndexPrecomputePlan(...)` | support callable | [L27–L39](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L27-L39) | Private constructor retains internal arrays and calculates fingerprint |
| `builder()` | public explicit callable | [L41–L43](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L41-L43) | New Builder |
| `size()` | public explicit callable | [L45–L47](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L45-L47) | Key count |
| `fingerprint()` | public explicit callable | [L49–L51](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L49-L51) | Stored graph digest |
| `keyAt(row)` | public explicit callable | [L53–L55](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L53-L55) | Bounds then retained key |
| `passAt(row)` | public explicit callable | [L57–L59](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L57-L59) | Bounds then original pass reference |
| `executionOrder()` | public explicit callable | [L61–L63](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L61-L63) | Clone of topological schedule |
| `dependencyRows(row)` | public explicit callable | [L65–L69](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L65-L69) | Bounds then copied CSR row |
| `rowOf(key)` | support callable | [L71–L83](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L71-L83) | Binary-search comparator; equality required at encountered tie |
| `dependencyStart(row)` | support callable | [L85–L87](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L85-L87) | Bounds then CSR start |
| `dependencyEnd(row)` | support callable | [L89–L92](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L89-L92) | Bounds then CSR end |
| `dependencyRowAt(index)` | support callable | [L94–L96](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L94-L96) | Bounds on flat lane |
| `Builder; passes list` | state or type | [L98–L99](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L98-L99) | Public nested static builder, mutable ArrayList |
| `Builder()` | generated surface | [L98–L99](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L98-L99) | Implicit public no-argument constructor |
| `Builder.add(pass)` | public explicit callable | [L101–L104](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L101-L104) | Retain non-null pass and return builder |
| `Builder.build()` | public explicit callable | [L106–L159](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L106-L159) | Sort/freeze graph, validate topology and identity, fingerprint; no execution or Limits |
| `Builder.topologicalOrder(size,starts,rows)` | support callable | [L161–L204](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L161-L204) | Reverse CSR and smallest-ready min-heap; cycle refusal |
| `Builder.offer(heap,size,value)` | support callable | [L206–L214](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L206-L214) | Private min-heap insertion |
| `Builder.sink(heap,size)` | support callable | [L216–L227](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L216-L227) | Private root repair |
| `Builder.binarySearch(keys,key)` | support callable | [L229–L241](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L229-L241) | Private dependency lookup with Class-sensitive equality |
| `fingerprint(keys,passes,starts,rows)` | support callable | [L244–L262](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L244-L262) | Private framed graph identity encoder |
| `update(digest,value)` | support callable | [L264–L271](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L264-L271) | Private length-framed raw UTF-16 encoder |

### MIndexPrecomputeScope

| Member or state | Kind | Exact source | Contract / limit |
|---|---|---|---|
| `record components specificationRevision/semanticProfile/lexiconSnapshot/compilerRevision/bindingSnapshot/sourceSha256` | state or type | [L9–L12](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L9-L12) | Explicit context identity, no discovery of actual state |
| `MIndexPrecomputeScope(...) compact constructor` | public explicit callable | [L13–L20](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L13-L20) | Require nonblank identity fields and normalize source digest |
| `fingerprint()` | public explicit callable | [L22–L35](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L22-L35) | Domain-separated framed six-field digest |
| `sha256(bytes)` | public explicit callable | [L37–L39](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L37-L39) | Raw byte digest with null check |
| `specificationRevision(), semanticProfile(), lexiconSnapshot(), compilerRevision(), bindingSnapshot(), sourceSha256(); equals(Object), hashCode(), toString()` | generated surface | [L10–L12](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L10-L12) | Six component accessors and generated record methods |
| `tokenDigest(tokens)` | support callable | [L41–L46](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L41-L46) | Package-private framed long vector |
| `checkedDigest(value)` | support callable | [L48–L53](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L48-L53) | Package-private exactly 64 hex normalization |
| `required(value)` | support callable | [L55–L60](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L55-L60) | Package-private null/blank rejection without normalization |
| `digest()` | support callable | [L62–L68](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L62-L68) | Package-private SHA-256 provider, missing-provider AssertionError |
| `putLong(digest,value)` | support callable | [L70–L72](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L70-L72) | Package-private eight-byte big-endian bit pattern |

## 3. Tests, caller and build boundary

| Evidence | Inspection result | What remains unproved |
|---|---|---|
| [Retained test tree](https://github.com/hsoliwal/M3jdk21/tree/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test) | Only five AST test/proof sources are physically present; no generic precompute package tests | No test execution or inherited coverage for these eight files |
| [Regex key][regex-key] | Entire 41-line caller read; two `sha256Utf16` calls bind expression and vendor/runtime string | No caller of a generic plan/executor established by those calls |
| [Regex persistence facade][regex-precompute] | Entire 111-line source read; uses separate filesystem cache, request revisions and codec | Does not implement generic plan execution or generic artifact serialization |
| [Filesystem key](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeKey.java#L1-L168) | Entire body read as the regex caller's separate canonical-key dependency | Not the typed `MIndexPrecomputeKey<T>` or a generic plan codec |
| [Filesystem cache](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L1-L280) | Entire body read as a persistence boundary; generic plan types absent | Complete fs family contracts/closure are outside this primary scope |
| [Filesystem result](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeResult.java#L1-L102) and [content ID](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ContentId.java#L1-L71) | Entire bodies read to distinguish persisted negative/value state and byte hashes | No adapter connecting their encoding to generic artifacts is supplied here |
| [Context import][context] and [Limits import][limits] | `com.synexia.job.IProgressMonitor` is outside the retained subtree | Full monitor implementation, cancellation exception type and receiving dependency resolution |
| [M3 reactor][reactor] | Retained subtree is not declared as a module | Not an executed effective build graph or proof about every manual/external harness |
| [Shell source set][core-build] | Script reads `core/src` and named core tests; read as text only | Does not qualify retained generic package |
| [Importer POM](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/pom.xml#L6-L25) | Separate `com.m3:m3-synexia-import` verifier and test dependencies | Importer tests are not pass/plan/Context behavioral tests |
| [Bridge POM](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/ports/indexstring/pom.xml#L4-L13) | Separate `com.m3.compat:indexstring-retained-view` project and named test mains | No inferred interchangeability or build admission for vendor types |

The retained subtree has no `pom.xml`, `module-info.java` or `package-info.java`.
The lexical retained-subtree caller search establishes one external class using
`MIndexPrecomputePasses`; it does not claim whole-repository caller exhaustion or
Java symbol resolution. The generic package contains no call constructing
`Limits.Work`, enforcing its budgets in `Plan`, or creating `Context` through a
public API. No private donor dependency was read or substituted to fill these gaps.

## 4. Provenance, successors and public authority

The original [v7 export][v7-export] contains the exact eight unchanged rows
18–25. Each records donor revision `80d288951148548ef6aa4c95df9cb31a438aac16`,
category `indexstring-regex-precompute`, license label `Apache-2.0` and mode
`APACHE_SOURCE`. The original donor-relative path is each primary path above
with the leading `m3/vendor/synexia/` removed. These are target-published metadata
claims verified against public target bytes, not a private-source review.

| Public record | Bounded use in this reference |
|---|---|
| [v7 README][v7-readme] | Historical exact custody, 22 nonlicense payloads plus preserved LICENSE/NOTICE, separate receiving gates |
| [v7 qualification][v7-proof] | Attribution of bounded importer/AST receipts; explicit unqualified receiving/build/runtime/performance gates |
| [Program successor authority][program-successor] | Preserved additive Program/Query lineage and still-open runtime gates; no claim its reported executions ran here |
| [Codec copy recovery][codec-recovery] | Original codec `09f4ef3d…` to `c7f17a7b…` receipt; source/receiver boundaries retained |
| [Codec nested-slice receipt][codec-slice] | Later codec `f7ad9444…` receipt; do not mislabel original key-owner literals as current body hashes |
| [Vendor scope][vendor] | Multiple independently pinned snapshots, original notices, source-custody/build/runtime distinction |
| [Borrowing contract][borrowing] | Project-owned receiving and licensing boundaries; no independent relicensing claim |

The eight precompute hashes are unchanged across these public regex successors.
Current Program/codec custody is separately documented by the retained regex
reference; it is not added to this eight-source count. The original v7 receipts
and successor history remain preserved. A public receipt cannot establish
current-head execution or private donor verification by this documentation.

## 5. Review and open evidence

Documentation checks verify source-byte identities, bounded line ranges, named
member routes, references and the additive navigation scope. They execute no
repository code. Independent review must assess the exact final Markdown bytes
before publication; this page itself is not a runtime or review receipt.

Remaining behavioral evidence includes mixed-loader key ties, mutation during
hash/build/callback use, artifact-array mispopulation, direct versus transitive
access, budget exhaustion/overflow, cancellation and monitor exceptions, repeated
work-helper lifecycle calls, concurrency discipline and any actual execution or
persistent-cache adapter. Source-complete receiving, native/JNI/provider parity,
installed-image/jtreg qualification and performance remain separate gates.
Historical PATH_ONLY inventory entries are preserved as historical coverage;
this bounded companion supplements them without rewriting their original claims.

[artifact]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeArtifact.java#L7-L20
[context]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L8-L76
[key]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L12-L74
[limits]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L6-L54
[pass]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java#L6-L28
[passes]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L14-L98
[plan]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L19-L272
[plan-fingerprint]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L244-L271
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml#L12-L37
[core-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh#L4-L20
[vendor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/README.md#L34-L79
[borrowing]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache2-borrowing-contract.md#L61-L110
[v7-export]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/export.tsv#L18-L25
[v7-readme]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/README.md#L4-L12
[v7-proof]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/evidence/QUALIFICATION.json#L1-L56
[program-successor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/CURRENT_ACTUAL_AUTHORITY.md#L4-L20
[codec-recovery]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/qualification/regex-copy-recovery-20261011.json
[codec-slice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/qualification/regex-nested-slice-20261011.json
[regex-key]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecomputeKey.java#L11-L39
[regex-precompute]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecompute.java#L18-L110
[scope]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L9-L73
