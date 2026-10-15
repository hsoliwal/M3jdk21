<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Retained AST language specifications, production plans and dispatch

This reference describes eighteen physically retained Java source files in
`com.synexia.indexstring.ast` at public M3JDK21 source snapshot
`363c02caa2e5af1df9460b927ecd78f6a9dbcefb`. Their bodies are unchanged at documentation
base `fb1e2f78ae4000f34adbed4a7fb91a69dd5ef8e0`. The
[source/member coverage](retained-ast-specification-reference-coverage.md) records
exact identities and the [37-file routing map](retained-ast-routing-map.md) assigns
every retained AST production file once.

**Source-body documentation is not runtime qualification.** The retained subtree
has no `pom.xml` or `module-info.java`; grammar/token, heap/mapped image and native
bridge dependencies needed here are absent from that subtree. These are retained
source contracts, not an exported module, full Java parser, admitted JDK AST
runtime, or standalone compilable library. Builds, tests, examples, verifier
scripts, JNI/library loads and benchmarks are **NOT_RUN** in this documentation authoring/review pass. Hosted CI is separate exact-commit
evidence and does not qualify these source contracts.

## 1. Ownership and interpretation

There are three different coordinate systems:

- Java source metadata: `Tree.Kind.ordinal()` identifies a `MIndexLanguageRule`
- Formal grammar metadata: grammar symbol IDs and production IDs address a
  `MIndexASTSpecPrecompute`, construction plans and predictive dispatch
- One formal parse tree: node IDs and preorder ranks address a
  `MIndexASTSpecTreePrecompute`; the supplied tree and token stream remain separate

The Java AST atom/document/pool lifetime is described in
[ownership and lifetime](retained-ast-ownership-reference.md). Java document
occurrence navigation, queries and codecs are described in
[navigation and queries](retained-ast-navigation-reference.md). Their handles,
occurrences and `Tree.Kind` IDs must not be substituted for formal-tree node IDs,
production IDs or terminal sign-bit codes.

The public [AST intake](../../m3/synexia-import/intakes/ast-navigation-20261007/README.md)
attributes these files to donor revision `7d2133f1412a9e7295296c3f86baae577bb3251c`.
Its exported SHA-256 values match the retained bodies. This is a statement about
public receiver records and bytes, not independent inspection of donor source.
Preserve the existing [LICENSE](../../m3/vendor/synexia/LICENSE),
[NOTICE](../../m3/vendor/synexia/NOTICE) and scoped third-party notices.

Repository [delivery policy](../../m3/docs/synexia-delivery.md) keeps reusable
mechanics canonical in Synexia and JDK integration target-owned. The
[porting invariant](../../m3/docs/m3jdk21-porting-invariant.md) and
[naming map](../../m3/docs/name-mapping.json) retain String → arrays → collections →
AST/compiler → remaining families, with String qualification active. The newer
[current-full-borrow plan](../../m3/synexia-import/current-full-borrow/README.md)
also leaves AST and grammar at source-pin-only and promotion phases planned.
This documentation changes none of those states or license boundaries. The
isolated frozen-text fixture, `ports/indexstring` bridge and canonical
`java.lang.String`/`M3String` owners remain distinct.

## 2. Java 21 metadata and value rows

[`MIndexLanguageSpec`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageSpec.java#L17-L87) declares identity,
language, release, `SourceVersion`, ordinal-addressed rules and five token-table
families. Its defaults obtain the programming-expression formal spec, compose a
source `MIndexLang`, obtain the shared AST spec, and delegate plans/dispatch.
The formal grammar implementation is not retained here. These delegates do not
establish full Java grammar acceptance or replace javac.

[`MIndexLanguageSpecs`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageSpecs.java#L5-L16) exposes the singleton
`java21()`. `forLanguage(language, release)` accepts case-insensitive `java` and
exact release 21; other values, including null language, throw
`IllegalArgumentException`. There is no public registration or replacement method.

The package-private [`MIndexJava21LanguageSpec`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L21-L261)
builds one rule per host `Tree.Kind` value at class initialization. It returns
`java-21`, `java`, 21 and `SourceVersion.RELEASE_21`; the rule count is the host
`Tree.Kind.values().length`, not a separately fixed numeric constant. A kind must
be non-null; indexed rule/table access uses `Objects.checkIndex`. No backing
array is returned. Returned strings and rule records are immutable values.

Its sorted vocabulary contains:

| Table | Retained entries and lookup boundary |
| --- | --- |
| Keywords | 51 entries including `_`, `const` and `goto`; exact case-sensitive UTF-16 comparison |
| Contextual keywords | `exports`, `module`, `non-sealed`, `open`, `opens`, `permits`, `provides`, `record`, `requires`, `sealed`, `to`, `transitive`, `uses`, `var`, `when`, `with`, `yield` |
| Literal tokens | `false`, `null`, `true` |
| Primitive types | `boolean`, `byte`, `char`, `double`, `float`, `int`, `long`, `short`; `void` is a keyword, not an entry here |
| Modifier tokens | `abstract`, `default`, `final`, `native`, `non-sealed`, `private`, `protected`, `public`, `sealed`, `static`, `strictfp`, `synchronized`, `transient`, `volatile` |

`sorted` clones input before sorting; `contains` uses binary search and reads the
caller's `CharSequence` directly without converting it to a `String`. Null tokens
fail; concurrent changes to a mutable token are not synchronized. `isReservedToken`
combines keywords and literal tokens only, not contextual keywords.
The [complete tables and comparison](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L24-L120)
and [helpers](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L477-L527) are authoritative.

### Categories, labels, flags and release metadata

[`MIndexSyntaxCategory`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexSyntaxCategory.java#L5-L25) has fifteen ordinal
bit categories: compilation, declaration, expression, statement, type, pattern,
directive, literal, operator, control flow, name, annotation, module, error and
other. `bit()` is `1L << ordinal()`. Categories overlap: the Java image derives
expression/statement/pattern/directive bits from `Tree.Kind.asInterface()` and adds
explicit kind-name groups; operators add the operator bit, and only an otherwise
empty mask becomes `OTHER`.

The [kind-name tables](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L263-L427) are metadata,
not a validator. Literal names ending `_LITERAL` use `LITERAL_VALUE`; name kinds
use `IDENTIFIER`; class/interface/enum/annotation/record/method/variable/type-parameter
kinds use `DECLARATION_NAME`; primitive types and labeled-statement/break/continue
have their dedicated label policies; others use `NONE`. `MIndexLabelPolicy.isLabelled`
is false only for `NONE`.

`MIndexFlagPolicy` names `NONE`, `MODIFIER_BITS`, `BOOLEAN` and
`ENUM_ORDINAL_PLUS_ONE`. The table assigns modifier bits to `MODIFIERS`, boolean
to `IMPORT`, and ordinal-plus-one to member reference, case, lambda and module.
The enum comment describes a compact 56-bit AST flag lane; the language interface's
modifier helpers independently support ordinals below 64. `modifierMask` includes
all host `Modifier` enum values and class initialization fails if an ordinal reaches
64. `modifierBit` rejects that overflow; null fails when reading its ordinal.
Neither helper independently enforces a 56-bit pool lane or legal modifier combinations.

Minimum-release rows are exactly the source table: assert 4; annotation/type-annotation,
annotation type, enum, enhanced-for, type parameter, parameterized type and wildcard
kinds 5; union type 7; lambda/member-reference/annotated/intersection types 8;
module/directive kinds 9; switch-expression/yield 14; record/binding-pattern 16;
the listed template/pattern/case-label kinds 21; otherwise 1. Preview is true only
for `TEMPLATE` and `ANY_PATTERN`. These retained classifications are not an independent
JLS correctness or preview-support claim.

### Rule and operator record validation

[`MIndexLanguageRule`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexLanguageRule.java#L13-L42) retains rule index,
kind, category mask, label policy, flag policy, operator, minimum release and
preview flag. Its compact constructor requires non-null object components,
`ruleIndex == kind.ordinal()` and positive minimum release. It does not validate
category-mask bits, release availability or combinations. `is(category)` null-checks
then tests the mask; `isOperator()` delegates to the operator row.

[`MIndexOperatorRule`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexOperatorRule.java#L12-L36) retains token,
precedence, associativity, arity, short-circuit and assignment flags. Token and
associativity are non-null; precedence is nonnegative; arity is 0–3; arity zero
requires precedence zero. It does not reject positive arity with zero precedence,
empty tokens or inconsistent flags. `NONE` is empty token, precedence/arity zero,
`NONE` associativity and false flags; `isOperator` is simply arity nonzero.

The [Java operator table](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexJava21LanguageSpec.java#L429-L475) uses higher
precedence for tighter binding: postfix ++/-- 16 (left); prefix ++/--, unary +/-/~/!
15 (right); cast 14 (right); multiplicative 13; additive 12; shifts 11; relational
and instanceof 10; equality 9; bitwise &/xor/or 8/7/6; conditional &&/|| 5/4;
conditional expression 3 (right, arity 3); assignment and compound assignments 2
(right). Ordinary binary levels are left-associative. Only &&, || and ?: have the
short-circuit flag; assignments have the assignment flag. Multiple kinds can share
a token, so token identity alone does not select unary, binary or postfix semantics.

## 3. Canonical spec registry and cache lifetime

[`MIndexASTSpecs`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecs.java#L12-L94) owns two static
`ConcurrentHashMap<String, …>` caches. `forLang` null-checks the language and uses
its identity as the `compute` key. The first successful request constructs the
spec. A later same-identity request returns the existing object only when language
fingerprints match; a mismatch throws `IllegalStateException` and does not install
the candidate. Equality of a 64-bit fingerprint is the actual comparison, not a
full structural comparison or collision-proof assertion.

`heapImage(lang)` is a separate identity cache and compares both language and
grammar fingerprints when an image exists. Its new-image branch wraps `forLang`.
The heap image implementation is absent from the retained directory. String-based
lookups resolve through absent `MIndexLangs.byIdentity`; null identity is rejected.
`precomputeBuiltIns` follows the external registry's returned identity order and
returns a fresh array of shared spec references; neither the built-in list nor its
resolution failures can be established from these bodies alone.

`cachedSpecCount` and `cachedHeapImageCount` report current map sizes, not a stable
multi-cache snapshot. There is no eviction, close, reset or weak ownership in this
registry. A constructor/resolver exception propagates; prior successful entries
remain. Initialization is lazy at registry lookup, while a newly constructed spec
eagerly builds its plans and dispatch. Immutable arrays are allocated internally;
referenced grammar/language objects rely on their external frozen-object contract.

`writeImage` delegates to the writer after obtaining the heap image; `openMappedImage`
null-checks input and language then delegates to absent `MappedMIndexASTSpecImage.open`.
Its comment promises read-only fingerprint validation, but the validating reader,
unmapping/close behavior and malformed-input refusal are not implemented in these
retained files. No mmap lifecycle guarantee is inferred from the factory name.

## 4. Formal spec precompute

[`MIndexASTSpecPrecompute`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L24-L272) retains the
language and grammar, optional source rule image, primitive symbol/production lanes,
compressed postings, operator mappings, plans, dispatch and a 64-bit fingerprint.
Its package-private constructor is reached by the registry.

Construction copies the external grammar's symbol kind, token class (byte `-1`
for absent), literal case, minimum token count and FIRST sets. It copies FOLLOW,
left-recursive and can-end-after facts only for nonterminals. Nullable, productive
and reachable facts also come from the grammar. This class does not calculate or
prove the grammar's original FIRST/FOLLOW fixed points, lexical rules, nullability,
productivity, reachability or left recursion.

For each production it flattens RHS symbols in ordinal order, records each used
symbol only once in reverse production-use postings, and sums minimum tokens with
`Math.addExact`. Any unproductive RHS minimum makes the production minimum `-1`.
Prediction is the distinct union of FIRST for the nullable prefix through the first
non-nullable symbol. If the entire RHS is nullable, including an empty RHS, FOLLOW
of its LHS is added, and `canEndAfter(lhs)` determines the separate EOF flag.
Prediction symbols are sorted by numeric symbol ID; reverse uses are filled in
production order. EOF is not an invented terminal ID.

[Operator mapping](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L245-L267) scans expression
operators in their external order. Literal grammar symbols whose name exactly
equals the token receive the first matching operator index; duplicates do not
replace it. Source rule operators likewise map by first exact token match, ignoring
arity, precedence and associativity. No match gives `-1`/empty `Optional`. Thus a
shared token such as `+` can map several Java rule kinds to one expression-operator
index; the mapping is not overload resolution.

[Queries](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L274-L510) bounds-check symbol,
production and RHS ordinals. FIRST, FOLLOW, prediction and use postings return
copies. `predictsTerminal` returns false for a valid nonterminal argument.
`sourceRule` throws `IllegalStateException` when no source rule image exists;
source-rule index methods instead check the zero-length lane. `decisionIndex`
delegates to the external grammar; plans and dispatch return the stored shared
objects. `precompute(tree)` first requires the same grammar fingerprint and then
builds a new per-tree object.

Symbol hashes mix grammar fingerprint, symbol ID, kind, literal case, minimum,
optional token class and name units. Production hashes mix grammar fingerprint,
production ID, LHS and ordered RHS. The spec fingerprint mixes the language
fingerprint, symbol/production hashes, source operator mapping and plan/dispatch
fingerprints. These hashes are candidate/identity aids, not cryptographic
attestation or semantic equality. [Hash helpers](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecPrecompute.java#L531-L585)
and checked offset arithmetic define the exact algorithm.

## 5. Production construction plans

[`MIndexASTProductionPlans`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L126-L364)
precomputes each RHS slot's symbol and role: nonterminal, token class, operator
literal, or ordinary literal. It records nullable self, nullable prefix strictly
before the slot, nullable suffix strictly after it, direct operator flag/index,
and minimum token counts strictly before/after. Empty prefixes/suffixes have
minimum zero and are nullable; a nonproductive prefix/suffix uses minimum `-1`.
It counts terminal/nonterminal children and direct operator slots, and returns
operator ordinals in RHS order. These are grammar construction facts, not a builder
that creates or type-checks Java AST atoms.

Reverse LHS and prediction postings are filled in production order. Queries return
new arrays; a valid terminal used for `productionsForLhs` and a valid nonterminal
used for `productionsPredicting` return empty arrays. Other invalid numeric
coordinates fail bounds checks.

Terminal ordinals follow ascending symbol IDs. Derivable-terminal/operator bitsets
start at terminal/operator symbols and propagate by a work queue through reverse
production uses to LHS symbols. A production row unions all its RHS symbol rows.
The implementation propagates graph facts through every production without
filtering on productive/reachable flags. A hit therefore must not be interpreted
as proof that a complete accepted token sequence exists. `derivesTerminal` returns
false when the requested target is a valid nonterminal; operator indices are
bounds-checked against the expression table. [Closure helpers](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L624-L757)
keep these mechanics explicit.

`shapeHash64` deliberately omits symbol names/IDs and particular operator tokens;
it mixes child roles, token-class ordinals and operator presence. `planHash64`
combines the production hash, shape and production minimum. Neither proves
semantic equivalence. The plan fingerprint includes language fingerprint, plan
hashes and before/after minima.

[Syntax-kind encoding](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionPlans.java#L543-L573) uses nonnegative
production IDs and `Integer.MIN_VALUE | terminalSymbol` for terminals. Encoding
checks the relevant production/symbol and rejects nonterminal symbols.
`terminalSyntaxKindCode` tests sign only; decoding a terminal masks the low 31 bits
without rechecking the decoded symbol or its kind. Production decoding rejects
negative values and bounds-checks. Do not accept arbitrary negative codes as
validated terminal coordinates.

## 6. Predictive dispatch, order and ambiguity

[`MIndexASTProductionDispatch`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L61-L222)
builds from either the precompute or the image view through a private `Source`
adapter. It requires each LHS to have `NON_TERMINAL` kind and rejects predicted
symbols whose kind is `NON_TERMINAL`, with both coordinates range-checked. It does
not independently validate all source-view metadata: a malformed image view
returning null kind is not rejected by the predicted-symbol check. It packs `(lhs, terminal)` into high/low unsigned 32-bit halves,
sorts by unsigned packed key then production ID, and rejects duplicate identical
key/production pairs. Distinct productions for one key are retained, not resolved
by priority. Per-cell candidates and EOF candidates are in production-ID order.

Sorted keys have compressed candidate offsets plus a per-symbol row index. The
fixed open-addressed directory stores `packedKey + 1` and `entry + 1`, reserving
zero for empty slots. Linear probing handles hash collisions without equating
unequal keys. Capacity is a power of two at least `max(2, entries + entries/2 + 1)`;
checked arithmetic and the explicit int-capacity guard can fail construction.
EOF candidates have their own per-symbol compressed postings.

[Lookup](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTProductionDispatch.java#L224-L326) has these results:

| Cell | Candidate count | Unique production | Ambiguous | Batch flags |
| --- | --- | --- | --- | --- |
| Absent | 0 | -1 | false | 0 |
| One production | 1 | that ID | false | 1 (`DISPATCH_PRESENT`) |
| Multiple productions | all retained | -1 | true | 3 (present + ambiguous) |

`predictedTerminals` returns sorted terminal IDs for a symbol; candidate and EOF
arrays are copies. Unique/ambiguous entry totals count ordinary dispatch cells,
not EOF cells. Public query methods validate numeric symbol range only, not the
argument's grammar kind: valid but wrong-kind pairs normally have no indexed
cell. EOF is queried through dedicated methods, not terminal `-1`.

The private arrays are not returned by public scalar APIs. Package-private
`*ForNative` accessors expose the actual arrays to the trusted native adapter;
the immutability boundary is therefore not defensive against arbitrary same-package
mutation. The dispatch object has no public mutation or close method.

## 7. Batch outputs and the optional native boundary

`batchDispatch` and `batchEofDispatch` write caller-owned count, unique-ID and byte
flag lanes of exactly the input length. Null lanes fail; unequal lengths throw
`IllegalArgumentException`. The concrete dispatch helper validates every input
symbol before executing either provider. The image-view defaults check lengths
but validate coordinates only through per-row virtual scalar calls, so an invalid
later row can follow earlier output writes. Empty equal-length batches do no work.

There is no alias rejection or input snapshot. Use distinct output arrays and do
not mutate inputs concurrently. Overlapping output lanes cannot preserve both
results. In the image-view default, count is written before the unique-production
lookup, so aliasing a count lane with an input can affect that lookup. The concrete
helper computes the arguments before `writeResult`, but this does not establish
provider-independent alias safety. No transactional rollback is promised for
provider/runtime exceptions. The EOF scalar loop requests a candidate array per
row; the interface's allocation-free comment is not a measured or universal
zero-allocation guarantee. [Defaults](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImageView.java#L101-L188)
and [concrete scalar loops](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L235-L378) show the distinction.

The package-private [`MIndexASTDispatchNative`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L14-L233)
uses ABI version 1 and a class-initialized threshold from
`synexia.indexstring.ast.dispatch.native.threshold`, default 256 and clamped to
at least 1. Its volatile boxed availability flag is initialized under class
synchronization. It first asks absent `IndexStringNative.available()`, then probes
`nativeAbiVersion`. False availability/ABI mismatch is cached; there is no retry
or reset API. `UnsatisfiedLinkError` during probing or batch invocation disables
future native use. The outer `IndexStringNative.available()` call is outside the
ABI probe's catch; unrelated failures are not generally swallowed.

Native dispatch is attempted only at/above threshold and, for mapped input, only
for a direct buffer. Native status zero means success; `-1` falls back to scalar
Java without disabling availability; other status codes throw `IllegalStateException`.
No provider was loaded or run here.

[Mapped helper validation](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDispatchNative.java#L380-L427) checks
nonnegative section bases, entry/candidate counts and EOF symbol count,
power-of-two slot count at least two, and section ranges against buffer **capacity**,
not limit. The ordinary mapped helper does not explicitly reject a negative
symbol count when an empty batch makes per-symbol checks vacuous. It does not itself enforce section
nonoverlap, alignment, byte order, checksum, correspondence with the fallback view,
or read-only ownership. Its `entryCount + 1` and `symbolCount + 1` expressions are
int additions before widening. Do not treat these helpers as a complete hostile
image decoder. The five native declarations have no implementation in the retained
AST source set; the retained sibling shared-array CMake target is not evidence of
this dispatch ABI. The comment that addresses never survive a call is the Java
source's stated contract, not a native lifetime proof available in this slice.

## 8. Per-formal-tree geometry and snapshot transfer

[`MIndexASTSpecTreePrecompute`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L505-L696)
builds fresh primitive geometry while retaining the supplied spec and tree.
The ordinary entry is `spec.precompute(tree)`, which checks grammar fingerprint.
Build rejects an empty tree, repeated child parentage, a parented root, disconnected
nodes and traversal that does not cover all nodes. It iteratively computes DFS
preorder/inverse, parent/child ordinal/depth, subtree size, leaf count, sign-bit
kind codes, binary-lifting ancestors, hashes and postings. Nonterminal production
LHS must match the node symbol; terminal nodes must lack a production. These
checks do not constitute independent full grammar/token acceptance.

Exact hashes include terminal token UTF-16 content; structural hashes omit that
content; logic hashes omit content for `IDENTIFIER`, `QUOTED_IDENTIFIER`,
`NAME_PLACEHOLDER` and `VALUE_PLACEHOLDER`, retaining it for other token classes
and literal symbols. All fold ordered child hashes. The logic description is
identifier/name-insensitive, but the actual exclusions include value placeholders.
No hash proves equality. Two hashed bits per symbol form a Bloom-like subtree
signal; `mayContainSymbol` can be a false positive, whereas `containsSymbol`
uses exact posting counts. [Hash helpers](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L966-L1023)
define the UTF-16 and token-class details.

Node postings are filled in ascending node-ID order; separate preorder postings
support lower-bound counts inside `[preorder(node), preorder(node)+subtreeSize)`.
Parent and child ordinal of the root are `-1`; ancestor containment includes self;
zero-distance ancestor is self; a negative distance fails and beyond-root returns
`-1`. LCA uses the binary-lifting table. Node, preorder, symbol and production
queries check bounds. `childRole` rejects terminals and checks child-count/RHS-length
agreement; `childGrammarSymbol` rejects terminals but does not repeat that count
check. `productionShapeHash64` rejects terminals.
[Queries](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L698-L954) return copied posting
arrays and shared spec/tree references. Static kind decoders only enforce sign;
unlike the plan's production decoder they have no grammar-specific bounds check.

### Snapshot validation is a bounded transfer contract

[`snapshot`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L104-L271) clones every primitive
array. The public nested `Snapshot` exposes no public constructor or public lane
accessors; its package-private constructor stores supplied arrays directly and
package-private accessors return them. Publicly obtained snapshots are detached
from the original precompute, not serializations of the grammar or token stream.

`restore(spec, tree, payload)` null-checks, validates, then clones all lanes into a
new precompute retaining the supplied objects. [Validation](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecTreePrecompute.java#L273-L503)
limits tree nodes, symbol count and production count to 16,000,000; checks node
count/root/levels and all array lengths; checks parentage, preorder permutation
and inverse/root, depths, kind codes, subtree/leaf/hash/signal derivations,
ancestor recurrence and exact symbol/production posting arrays and offsets.
Failures are chiefly `IllegalArgumentException`, with bounds/checked-arithmetic
failures possible for malformed supplied structures.

Do not strengthen that list into a proven arbitrary-input restore guarantee.
Unlike `spec.precompute`, public `restore` has no explicit grammar-fingerprint
comparison. It checks the supplied preorder permutation but does not independently
rebuild DFS order or verify every subtree's preorder contiguity. It also does not
independently validate each child's grammar symbol against the production RHS.
Validation and subsequent cloning do not synchronize against same-package mutation
of a snapshot. No direct spec-tree hostile-restore test is present in the retained
test directory. These source-visible limits remain qualification work; this guide
neither repairs nor executes them.

## 9. Compiled spec image and serialization effects

[`MIndexASTSpecImageView`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImageView.java#L15-L197) is a read-only
query interface for identifiers, fingerprints, symbol/production facts, construction
plans, derivation, source-rule mapping, dispatch, syntax codes and payload sizing.
It provides only the batch defaults and private length validators. An interface
contract does not prove copying, mapping, close semantics or safe publication for
missing implementations.

[`MIndexASTSpecImage.compile`](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L23-L127) wraps a spec in
the absent heap implementation and calls `write`. `write` accepts a non-null view
and path, refuses a mapped legacy-v1 image with a request to rebind through the
canonical language, reconstructs dispatch from prediction sets, computes counts
and an integer-addressed layout, and compares a recomputed content fingerprint
with the view's declared fingerprint before opening the output.

The writer emits big-endian `DataOutputStream` fields, raw UTF-8 identity/language/
version bytes, then primitive lanes. Its 128-byte header is:

| Offset | Field(s) |
| --- | --- |
| 0 | 8-byte magic `0x4d49445841535031` (`MIDXASP1`) |
| 8, 12 | format 2, header length 128 (4 bytes each) |
| 16, 20, 24 | UTF-8 identity, language and version byte lengths |
| 28–76 | thirteen 4-byte counts in order: symbols, productions, operators, source rules, RHS total, FIRST total, FOLLOW total, prediction total, use total, direct operators, terminals, terminal words, operator words |
| 80, 88, 96, 104, 112 | language, grammar, AST-spec, production-plan and content fingerprints (8 bytes each) |
| 120, 124 | arrays start; dispatch entry count |

The [ordered layout](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L602-L798) allocates without padding:

1. Symbol kind/token-class/literal-case/flags byte lanes, minimum-token ints and
   symbol-hash longs; FIRST, FOLLOW and use offset/value pairs; operator-index and
   source-rule-operator int lanes
2. Production LHS, RHS starts/symbols, minimum-token ints, EOF bytes and hash longs;
   prediction offsets/values
3. RHS role/flag bytes, operator/before/after ints; per-production child/operator
   counts and shape/plan hashes; direct-operator, LHS and reverse-prediction postings;
   terminal ordinal maps; symbol/production terminal and operator bit matrices
4. Format 2 dispatch keys, candidate offsets/values, symbol row offsets, hash slot
   keys/entries, EOF offsets and an EOF candidate lane padded to production count
   with `-1`

`Counts` carries the fourteen construction counts including dispatch entries.
Matrices use at least one 64-bit word per row. `Layout` uses checked int addition/
multiplication; legacy layout omits dispatch sections (bases `-1`). Merely having
that branch is not evidence that a retained reader accepts either version.
`counts` requires dispatch candidate count to equal prediction total; dispatch
serialization rechecks its entry/candidate/hash-capacity geometry. Terminal matrix
writing rejects a returned nonterminal and operator matrix writing checks indices.

The [content fingerprint](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSpecImage.java#L129-L201) mixes identifiers,
reported fingerprints/counts, symbol facts/postings, production/plan geometry,
derivation arrays, source-rule operator mappings and optionally dispatch fingerprint/
summary counts. It is a 64-bit custom hash; the distinct 32-byte SHA-256 trailer
covers all header/string/payload bytes and excludes itself. Neither is a signature
or proof of grammar semantics.

The output is normalized to an absolute path, missing parent directories are
created, and `Files.newOutputStream` directly creates/truncates that path. There
is no temp-file/atomic rename, rollback, fsync or exclusive lock. A failure after
opening can leave partial output or replace prior bytes. Size is checked after
payload flush and after the digest trailer; the intended limit is no more than
`Integer.MAX_VALUE` bytes including trailer. The payload-plus-trailer expression
`Math.addExact(layout.payloadEnd(), SHA256_BYTES)` selects the int overload before
assignment to long, so it can throw `ArithmeticException` before the subsequent
`> Integer.MAX_VALUE` check; checked layout arithmetic can fail still earlier.
View methods are called repeatedly: the pre-write fingerprint
comparison is not synchronization or a general mutation-proof transaction.

## 10. Usage, errors and evidence limits

A conceptual sequence is: resolve a known language → obtain its shared spec →
query plans/dispatch → separately obtain a compatible formal parse tree → build
per-tree geometry. A dispatch result of `-1` requires inspecting candidate count;
it means either absent or ambiguous. A positive hash or Bloom-like signal requires
an exact authority before reuse. Serialization is an explicit filesystem effect.
This sequence was inspected, not executed; absent grammar/image dependencies prevent
a standalone recipe or compilation promise here.

Common failure families are null validation, index/range validation, unsupported
language selection, identity collisions, malformed production geometry, checked
arithmetic overflow, image inconsistency and I/O failure. No cancellation callback,
resource budget framework, registry eviction or universal exception recovery is
implemented by these owners. `primitivePayloadBytes` counts selected primitive
arrays (and, in the spec, plan/dispatch arrays); it excludes object headers,
references, external grammar/tree/token storage, temporary copies and native/mapped
resources. Several byte/int/long array-length groups are added as int before
widening, so extreme aggregate lengths can overflow before the long multiplication
or total. These counters are not guaranteed wide-size bounds or retained-heap/
process-memory measurements; no overflow case was executed here.

The [historical qualification receipt](../../m3/synexia-import/intakes/ast-navigation-20261007/evidence/QUALIFICATION.json)
reports four original AST-precompute tests and isolated importer cases, while
explicitly excluding source-complete build, cold-image valid-checksum/wrong-geometry
refusal, cache/edit/resident lifecycle, native providers, matched JDK/jtreg,
performance and full-family completion. It does not qualify these spec/dispatch
or image/native paths. The only retained test caller read here that directly
requests `astSpec().fingerprint()` uses it as an LCA-codec key; its missing codec
implementation prevents interpreting that test as spec-image coverage.

For exact test assertions, all declared members, provenance rows, build boundaries
and remaining dependency gaps, use the [coverage companion](retained-ast-specification-reference-coverage.md).
