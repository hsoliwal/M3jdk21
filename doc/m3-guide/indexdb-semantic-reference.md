<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# IndexDB semantic graph and fingerprint reference

[Guide](README.md) · [Persistence and V1 codec](indexdb-persistence-reference.md) · [Coverage](coverage.md)

## Scope and the four identities

This is complete source-body review of the six graph/fingerprint owners in the public `m3/indexdb` module, together with the three persistence owners in the [companion reference](indexdb-persistence-reference.md). Source pin: `1dce82bfb7f1058d74f7503c4cd54fa7d431c032`, inspected 14 October 2026. The nine blobs are unchanged from inventory pin `278b221e8365ebeb6095a3a5643b38a0564258b5`. It is bounded source/API documentation, not analyzer correctness, all-caller coverage, a fresh test pass or runtime promotion.

Keep four different identities separate:

1. A semantic node ID hashes kind plus semantic key. It identifies a graph row independently of that row's current fingerprint
2. Exact/structural/logic fingerprints describe supplied source/features or the composition of children; the algebra does not parse or type-check source
3. A persisted artifact content SHA hashes the complete encoded byte array, including metadata and wire representation
4. An artifact name selects a filesystem ref that later puts may replace

Matching candidate signals do not establish program equivalence. Even an `exactSha256` lookup compares a stored hash string, not retained exact source bytes. The [public-target authority and qualification boundary](indexdb-persistence-reference.md#scope-and-authority) remains unchanged. Builds, examples, tests, recipe/policy checks and measurements are **NOT_RUN**.

## Nine-owner coverage map

Paths in the first eight rows are under `m3/indexdb/src/main/java/com/m3/indexdb/`; the ninth is the distinct public compatibility path `m3/indexdb/src/main/java/com/synexia/mindex/db/`. P13 comprises seven public top-level types and one package-private codec, not eight public API types. P14 contains one public record. No private donor source was used.

| Complete source owner | Blob SHA | Contract location and member boundary |
| --- | --- | --- |
| [M3IndexDB][store] | `61e913815a3dc8d94280a765cc050f2e75c9c040` | [Persistence](indexdb-persistence-reference.md#store-lifecycle-and-every-public-operation): all nine public methods, private path/publication/size/hash helpers, closed state and `SizeFailure` |
| [M3IndexDbArtifact][artifact] | `7b152b2ed51a559c40b466cc7183e94c3b06a342` | [Artifact values](indexdb-persistence-reference.md#artifact-values-and-validation): canonical constructor, `create`, explicit payload/length accessors, generated projections and array equality boundary |
| [M3IndexDbSemanticCodec][codec] | `da78ddcd3dbce6777a7434b7917434c4a2074cf6` | [Wire format](indexdb-persistence-reference.md#v1-semantic-wire-layout): package-private encode/decode; private constructor, framing, budget, text, SHA, ordinal/count, string-table and edge-emission helpers |
| [M3IndexDbSemanticEdge][edge] | `81b2c71d69b99a081629b1c39db38664af1476e6` | [Identity records](#identity-records-and-kinds): canonical constructor and all four projections; endpoint/role/ordinal validation |
| [M3IndexDbSemanticFingerprint][fingerprint] | `df44495f5e5b48ea1bf4a863df9ca188bd787948` | [Fingerprint algebra](#fingerprint-values-and-every-entry-point): canonical constructor, seven projections, nested `Component`, leaf/compose/distance/UTF-16/hash-hex APIs and private framing/hash/vote helpers |
| [M3IndexDbSemanticIndex][index] | `a7245a61c9ccf353be202790f8058f84a0016205` | [Construction](#graph-construction-order-and-recomposition) and [query map](#queries-traversal-and-storage-adapters): constants, all 20 top-level public methods, nested `Similarity`, private indexes, composition/order/validation and `HashKind`/`VisitState` |
| [M3IndexDbSemanticKind][kind] | `d024ecc2a6c39ae8e232a02493995f505c9a82e3` | [Kinds](#identity-records-and-kinds): all twelve constants; generated `values`/`valueOf` and persisted enum-order boundary |
| [M3IndexDbSemanticNode][node] | `1c995542cd6d1a9c09126e9cadb4b2d6b2f11a7b` | [Identity records](#identity-records-and-kinds): both constructors, `withFingerprint`, all seven projections and identity/token validation |
| [com.synexia.mindex.db.M3IndexDbFingerprintV1][compatibility] | `a51449e205016cfc01fb0ccbdb70e0038539b276` | [Compatibility](#historical-compatibility-owner): same declared fingerprint/component surface, its own record type, algorithm/framing and bounded parity tests |

All nine complete bodies are covered here, including internal helpers. Generated record/enum behavior is identified rather than counted as extra source declarations; inherited `Object`/`Enum` APIs are not a new method-by-method JDK audit. There is no native package documentation in these two physical production roots. Inline documentation, complete inbound callers, exact-head execution and whole-package/runtime qualification remain open.

## Identity records and kinds

Sources: [node][node], [edge][edge], [kind][kind], [index][index].

### Node identity

`M3IndexDbSemanticIndex.nodeId(kind, semanticKey)` rejects null inputs and hashes the UTF-16 code units of:

`M3_NODE_V1|` + kind name + `|` + `semanticKey.toString()`

The helper does **not** strip or validate the semantic key. The node constructor strips `semanticKey`, `symbol` and `patternRole`, rejects empty or NUL-containing results, then verifies that its supplied lowercase 64-hex `nodeId` equals the ID computed from its kind and stripped key. Compute IDs from the same normalized key you intend to store; an ID computed from a padded key will generally not match the constructor's stripped key.

Node fields/projections are `nodeId()`, `kind()`, `semanticKey()`, `sourcePath()`, `symbol()`, `patternRole()` and `fingerprint()`. Kind and fingerprint are non-null. Source path is only non-null: empty strings and NUL are accepted, and no path normalization, file existence or repository containment is checked. Token lengths have no explicit maximum here.

The six-argument constructor omitting pattern role supplies `"M3:" + kind.name()`. `withFingerprint(replacement)` creates a new validated node retaining all other fields; it does not mutate a graph that already contains the old node. Null replacement fails. Node ID ignores source path, symbol, role and fingerprint, but generated record equality includes all seven components. Two rows with the same ID and different metadata/fingerprint therefore conflict in `Index.of`.

### Edges and kinds

The edge record projects `parentId()`, `childId()`, `role()` and `ordinal()`. Both IDs must be non-null lowercase 64-hex strings, role is stripped/nonempty/NUL-free, and ordinal must be nonnegative. A self-edge is immediately rejected. Existence of endpoints, multi-node cycles and ordering conflicts are graph-level checks, not properties proven by constructing an edge alone.

The V1 enum declaration order is:

`ATOM, FIELD, METHOD, FILE, INTERFACE, IMPLEMENTATION, DOCUMENTATION, PACKAGE, MODULE, LIBRARY, PROJECT, REPOSITORY`

`values()` and `valueOf(String)` are the generated enum entry points. The binary codec persists the ordinal, not the name. Reordering/adding kinds requires explicit format/consumer compatibility analysis; the label “stable hierarchy” does not define a migration mechanism.

No constructor enforces a particular parent-kind/child-kind hierarchy, a single root, connectedness, a tree shape, or that a role spelling matches the child's kind. Shared children, multiple parents, disconnected components and an empty graph are allowed if all other checks pass.

## Graph construction, order and recomposition

`M3IndexDbSemanticIndex.of(Collection<Node>, Collection<Edge>)` takes non-null collections with non-null members. The result holds immutable lists/maps of immutable records and strings; it does not retain mutable caller collections. Construction is not an atomic snapshot of concurrently mutating input collections.

Construction proceeds as follows:

1. Insert nodes into a `TreeMap` by node ID. Equal duplicate records collapse; unequal records with the same ID throw `IllegalArgumentException`
2. Sort edge rows by parent ID, role, child ID and input ordinal. Collapse only fully equal records
3. Reject edges whose parent or child is absent
4. Normalize/order edges under the rules below
5. Recursively compose children before parents, tracking VISITING/DONE state and rejecting cycles
6. Freeze node-ID-sorted rows and the ordered edges; build ID, exact/structural/logic SHA, pattern-role and parent/child indexes

There is no mutation method: updates require creating another graph. The index class does not override `equals`/`hashCode`; separate snapshots do not acquire value equality merely because their rows or bytes agree. Construction failure returns no partial graph and does not write to storage. Null failures are `NullPointerException`; semantic conflicts, dangling endpoints, cycles and ordered-ordinal conflicts are `IllegalArgumentException`. Recursive traversal has no separate maximum depth or stack-overflow translation.

### Ordered and unordered roles

All edge comparisons begin with parent ID, then role text. Within each parent/role group:

| Role | Composition order and retained ordinal |
| --- | --- |
| Exactly `ATOM` or `FIELD`, or starts with `ORDERED:` | Sort by producer ordinal, then child semantic key, then child ID. Preserve numeric ordinals. Two distinct rows in the group with the same ordinal are rejected after equal-row deduplication |
| Any other role | Sort by child semantic key then child ID, disregarding producer ordinal for composition. Rewrite ordinals to 0, 1, … within that parent/role group |

Matching is case-sensitive. `METHOD` and `FILE` membership are unordered under these rules; a role's name, rather than the node kind, selects the policy.

Exact duplicate edges are removed before normalization. Two otherwise identical rows with different input ordinals are not exact duplicates: an unordered membership can retain both and give them consecutive canonical ordinals. A child can therefore appear more than once in traversal/composition. Ordered roles allow ordinal gaps; contiguous numbering is not required.

An ordered edge's numeric ordinal affects ordering and encoded evidence. Fingerprint composition itself uses the resulting child's **list position across all outgoing roles**, numbered from zero, rather than the original numeric ordinal. Changing gaps without changing order can change encoded bytes while leaving the parent's fingerprint unchanged. “All producer ordinals are merely tie-breakers” is not an accurate summary for ordered roles.

### Parent fingerprints

A node without outgoing edges retains its supplied fingerprint. Every node with outgoing edges, regardless of kind, is replaced by `withFingerprint(compose(...))`, using domain `kind + ":" + semanticKey` and each canonical child fingerprint paired with the edge role.

Consequently:

- A parent's input/placeholder fingerprint is ignored after its children have been composed
- Parent queries return recomposed nodes, not necessarily record-equal input nodes
- Child structural/logic changes propagate through the corresponding composition channels
- A shared child is computed once and reused by its parents during construction
- Parent sourcePath, symbol and patternRole metadata are retained but do not enter this composition domain or child list
- Changing source metadata can change encoded artifacts without changing node identity or parent fingerprint

The internal maps only accelerate/select the stored records. There is no background analyzer, incremental invalidation, compiler attribution or exact source verifier in this owner.

## Fingerprint values and every entry point

Sources: [M3IndexDbSemanticFingerprint][fingerprint] and [compatibility owner][compatibility].

The main record has these seven projections:

- `exactSha256()`, `structuralSha256()`, `logicSha256()`
- `structuralHash64()`, `logicHash64()`, `simHash64()`
- `normalizedComposition()`

The direct constructor validates three lowercase 64-hex SHA strings and a non-null normalized string. It accepts all long values. It does not verify that the SHA channels, 64-bit channels and normalized text correspond to one another or to any source.

Nested `Component(String role, Fingerprint fingerprint)` strips the role and rejects empty/NUL-containing roles or null fingerprint; `role()` and `fingerprint()` are generated projections. The compatibility owner has its own Component type referencing its own fingerprint type.

| Entry point | Contract |
| --- | --- |
| `leaf(domain, structuralFeatures, logicFeatures, exactSource)` | Strips and validates a nonempty NUL-free domain; copies both non-null feature lists and rejects null elements; captures non-null exact source using `toString()`; computes the channels below |
| `compose(domain, orderedChildren)` | Same domain validation; copies a non-null list of non-null Components; consumes the list in caller-supplied order without sorting or graph checks |
| `utf16Sha256(CharSequence)` | Hashes each UTF-16 code unit high byte then low byte, no BOM; accepts empty text, NUL and unpaired surrogate code units. Reads the supplied sequence directly; caller must keep it stable |
| `hammingDistance(left, right)` | Returns `Long.bitCount(left ^ right)`, in 0…64 |
| `structuralHash64Hex()`, `logicHash64Hex()`, `simHash64Hex()` | Fixed-width 16-character lowercase hexadecimal representation of the full 64-bit pattern, including negative long values |

Nulls produce `NullPointerException`; invalid SHA/domain/role tokens produce `IllegalArgumentException`. SHA-256 provider absence becomes `AssertionError` in both fingerprint implementations. Feature strings are non-null but are not stripped, sorted, deduplicated, bounded, or restricted against NUL/separators. A mutable input sequence/list must remain stable while being captured.

### Leaf channel framing

`leaf` is a fingerprint algebra, not an analyzer. The caller supplies ordered structure and logic features; variable renaming, type normalization and any claimed semantic meaning are upstream responsibilities.

- Exact SHA hashes the captured exact source's UTF-16 code units, independent of domain/features
- Structural SHA hashes the domain followed by each structural feature, each framed with its UTF-8 byte length as four big-endian bytes and then its UTF-8 bytes
- Logic SHA applies the same framing to domain and logic features
- The two 64-bit hashes use distinct `STRUCTURE:`/`LOGIC:` domain seeds, feature labels `S`/`L`, UTF-8 FNV-style accumulation, the source-defined avalanche/combine constants, ordered feature folding and final feature-count mixing
- SimHash casts one equal-weight 64-bit vote per logic feature; an empty logic list uses a domain-derived vote. Zero vote ties select the 1 bit
- Normalized text is simply domain + `|` + logic features joined with `|`

Length framing prevents simple feature-boundary concatenation ambiguity in the SHA input. Structural and logic SHA do **not** add distinct channel tags: identical domain/feature lists can produce identical SHA strings in those channels. Their 64-bit channels use different seeds/labels. The normalized display string is not a reversible escaped serialization; different feature boundaries containing `|` can produce the same displayed text. Use the framed channel or original features for the relation you need.

The UTF-8 feature path uses JDK replacement behavior for malformed UTF-16 strings. It is not the same byte identity as `utf16Sha256`, which processes raw code units. No input normalization, collision-proof equivalence theorem or performance bound follows from these names.

### Parent composition framing

For the child at list position `i`, compose builds `role + "#" + i`:

- Structural and logic feature entries are that role-position text + `:` + the corresponding child SHA, then length-framed with the parent domain
- Exact input begins with the parent domain and appends `|`, role-position, `:` and child exact SHA for each child; its UTF-16 SHA is the parent exact channel. This is rendered composition text, not concatenated original source and not the same framing as structural/logic SHA
- Structural64 and logic64 fold the role-position hash and corresponding child64 in order
- SimHash votes on the role-position hash and a mixed child SimHash combined with it; an empty child list has an EMPTY/domain fallback vote
- Normalized text records the domain and role-position/child-logic-SHA entries

Direct `compose` does not canonicalize input order. `Index.of` supplies the separately defined graph order before calling it. Empty `compose` and empty `leaf` are distinct factory paths; do not substitute one based only on the absence of children/features.

## Queries, traversal and storage adapters

Source: [M3IndexDbSemanticIndex][index]. Constants are `ARTIFACT_KIND = "m3.semantic-index"` and `FORMAT_VERSION = 1`. Construction and `nodeId` are covered above; the remaining public methods are mapped here.

| Operation | Validation, result and ordering |
| --- | --- |
| `nodes()`, `edges()` | Return immutable stored lists. Nodes are sorted by node ID; edges follow parent/role/composition order |
| `find(nodeId)` | Requires non-null only, with no SHA syntax check. Returns Optional.empty for any absent key, including malformed non-null text |
| `require(nodeId)` | Uses find; absent key throws `IllegalArgumentException` |
| `nodesOfKind(kind)` | Requires non-null; scans/filter nodes, preserving their node-ID order |
| `exactSha256(sha)`, `structuralSha256(sha)`, `logicSha256(sha)` | Require non-null, lowercase 64-hex input; return the matching hash bucket or an empty immutable list. Buckets are sorted by semantic key, retaining node-ID order for ties |
| `patternRole(role)` | Strips non-null input and rejects empty text; returns that bucket or empty list, sorted by semantic key with node-ID ties. Unlike node construction, this query does not explicitly reject NUL |
| `structuralHash64(hash)`, `logicHash64(hash)` | Accept any long, scan every node and return equal-bit-pattern matches in node-ID order |
| `nearSimHash(simHash64, maximumHammingDistance)` | Threshold must be 0…64. Scans every node, materializes all matches and sorts by distance, semantic key, then node ID; returns an immutable list |
| `children(parentId)` | Requires non-null only; unknown parent returns empty. Maps outgoing edges to current child records in role/composition order; repeated membership can repeat a node |
| `parents(childId)` | Requires non-null only; unknown child returns empty. Orders by parent semantic key, edge role, ordinal, then parent ID; repeated memberships can repeat a parent |
| `encode()` | Materializes new V1 bytes through the package-private codec |
| `decode(payload)` | Static codec entry point; admission and normalization are described in the [wire reference](indexdb-persistence-reference.md#decoder-admission-and-corruption-boundaries) |
| `store(database, artifactName)` | Requires non-null caller-owned store; delegates to putSemanticIndex and returns its artifact; does not close the database |
| `load(database, artifactName)` | Static adapter requiring non-null store; delegates to requireSemanticIndex; does not close it |

Nested `Similarity(node, hammingDistance)` requires non-null node and distance in 0…64; `node()`/`hammingDistance()` expose those values. Construction does not verify the supplied distance against a query, since no query value is part of the record.

A threshold of zero means identical SimHash bits, not exact source or program equivalence. Threshold 64 accepts every node. There is no top-K parameter, paging, timeout, cancellation or caller-provided result capacity. The 64-bit and near-SimHash queries are scans, not a persisted locality-sensitive search structure. Matching SHA/64-bit buckets and pattern roles select candidate records; callers must perform the exact, domain-appropriate verification their task requires.

Immutable snapshots have no query mutation or close method. The implementation uses method-local working state for fingerprint functions and graph construction, but this guide does not assert blanket thread safety for caller-owned mutable inputs, filesystem handles or analyzer integrations.

## Historical compatibility owner

The actual public type is `com.synexia.mindex.db.M3IndexDbFingerprintV1`. Do not rewrite it to `com.m3.indexdb` or infer that it is an external runtime service. It is a separate source file/type in this public module.

Its record components, canonical validation, nested Component, `leaf`, `compose`, `utf16Sha256`, `hammingDistance`, three hex projections and all private hashing/framing/vote mechanics match the corresponding main implementation's algorithm at the inspected pin. The public record types are still distinct: the graph's node/component APIs accept `M3IndexDbSemanticFingerprint`, with no declared compatibility conversion overload. Cross-type record equality is not field-parity verification.

“V1” names the historical compatibility algebra; it is not an independent serialized fingerprint frame or a migration dispatcher. The graph wire format has its own V1 header, and the artifact envelope has its own kind/version. There is no runtime version negotiation between these record classes.

## Semantic test evidence

The [persistence test map](indexdb-persistence-reference.md#test-source-evidence-and-remaining-gates) accounts for all fourteen test-source files. The five graph/fingerprint JUnit classes are:

| Read class | Actual source assertions and limits |
| --- | --- |
| [M3IndexDbSemanticIndexTest][test-index] | Eleven tests: exact/shape/logic separation for supplied feature fixtures; role/order-sensitive composition; hierarchy propagation and hash queries; input-order convergence; unordered METHOD versus ordered ATOM behavior; conflicting FIELD ordinals; canonical codec round trip; store/reopen/dedup; conflicting/dangling/cyclic/invalid inputs |
| [M3IndexDbSemanticFailClosedTest][test-fail] | Seven tests: fingerprint/record validation, equal-node and exact-edge deduplication, cycle/order checks, recomposed-parent oracle, query thresholds/absence, wire corruption and empty storage. Test names do not prove every possible invalid form |
| [M3IndexDbSemanticBoundaryTest][test-boundary] | Four tests: empty fingerprints/queries, token positions, absent/malformed query inputs, ORDERED roles and shared parents |
| [M3IndexDbFingerprintV1ParityTest][test-parity] | Four tests: one explicit historical known-answer leaf; 128 generated leaf inputs; ordered composition widths 0…16; 64 Hamming comparisons and seven UTF-16 strings. Compares all seven fields and the three hex projections between implementations |
| [M3IndexDbFingerprintV1BoundaryTest][test-compat-boundary] | Four tests: malformed SHA/role/domain, null lists/elements/source, empty/populated paths and composition-order sensitivity |

The retained known-answer leaf has domain METHOD, structural features private/static/int, logic features a+b/return and exact source `return a+b;`. The test fixes exact SHA `bc38ecd30e18d4aea0dcf50c0eb39c6f713375363378d2c4cb66b287dfeb6267`, structural64 `f7540c3cb5f5cd1d`, logic64 `705f301e930d6b49` and SimHash `b7fff9efbfdfffb3`, alongside the other channel assertions in the source. These are checked-in expected values, not outputs produced by this documentation pass.

The hierarchy fixtures are finite examples. They do not establish analyzer completeness, language/type equivalence, cryptographic collision absence, exhaustive graph orders, concurrency safety or resource behavior. Decoder normalization versus strict canonical-byte rejection, direct-constructor channel incoherence, repeated memberships with distinct ordinals and very deep graphs retain explicit caller/qualification obligations. Whole-package inline documentation, full inbound-consumer mapping, exact-head builds and all target receiving/runtime gates remain open.

[store]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDB.java
[artifact]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbArtifact.java
[codec]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticCodec.java
[edge]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticEdge.java
[fingerprint]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticFingerprint.java
[index]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticIndex.java
[kind]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticKind.java
[node]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticNode.java
[compatibility]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/synexia/mindex/db/M3IndexDbFingerprintV1.java
[test-index]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDbSemanticIndexTest.java
[test-fail]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDbSemanticFailClosedTest.java
[test-boundary]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDbSemanticBoundaryTest.java
[test-parity]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDbFingerprintV1ParityTest.java
[test-compat-boundary]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDbFingerprintV1BoundaryTest.java
