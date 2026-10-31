# CB: Class Bridge

This packet ports the reflection-backed class snapshot and caller-owned VM bridge into
`java.base`. It follows `m3/docs/name-mapping.json`; CB means Class Bridge, CI means Class
Index and PC means Precompute Context. It adds fourteen internal owners and reuses the
already inlined `M3VI` and `M3Release`. Maven/OpenRewrite is authoring tooling only.

## Source and adaptations

`pins.json` pins the source repository, commit, exact paths, Git blobs and SHA-256 hashes.
`donor` contains the exact source originals and scoped Apache-2.0 LICENSE/NOTICE snapshots.
The reflection class-index closure is copied in full for the twelve named owners. The two
VM candidate templates become `M3CB` and `M3PC`. Only the int-lane `lowerBound` and
`upperBound` method bodies from `MIndexAlgorithm` are copied; its whole original is retained
for attribution. This is not a claim that the other algorithms have been migrated.

The source class/member/CSR lanes and weak mirror/defining-loader boundary are retained.
Text outputs use existing immutable JDK Strings. Snapshot-local integer symbol IDs index
references to those Strings; a sorted primitive lane supports exact UTF-16 lookup. The
temporary map used during construction is discarded on freeze. There is no second text
payload store, global interner or retained collection Entry/Node per structural row.
The fingerprint incorporates length-prefixed UTF-16 spellings as well as the primitive
metadata lanes. It is a structural candidate key, not authenticated class bytes or a
portable persistent format. Progress/cancellation uses the existing `M3VI.Progress`.

Class/member wrappers are on-demand owner/row views. Reflection resolution is weak and
can become unavailable after unloading. Preparation discovers metadata without initializing
the fixture classes. It still performs reflection, allocates temporary builder state and
observes the runtime's access checks; this packet makes no allocation-free or speed claim.

## Runtime contract

`M3CB.over` reuses an existing snapshot. `prepare` builds a bounded one. OFF, OBSERVE and
ENABLED apply to the bridge's name/assignability operations; unknown or compact-only
classes fall back to the original Class operation. OBSERVE compares the original result
and retires the bridge on a mismatch. These are caller-selected modes, not new VM flags.

Declarations bind the actual defining Class's counterpart row to an immutable release and
precompute context. Same-named classes from different defining loaders remain distinct;
delegation resolves to the defining owner. A global content identifier does not become a
global live-Class registry. `effectiveVersion(Class<?>)` reads the release on that counterpart;
`effective` additionally returns its context and epoch. A declaration is supplied provenance,
not authentication of the final transformed definition and not permission to install code.

Declaration is synchronized, idempotent for the same values, and rejects conflicting values
or stale epochs. Invalidation retires the snapshot and clears declarations. The lifecycle
owner **must invalidate before** redefinition, retransformation or relevant access changes.
There is no automatic ClassLoader/JVMTI lifecycle hook in this packet. Existing attachments
must be checked with `isCurrent`; holding an attachment does not keep its weak Class live.

`M3PC` clones the ordered dependency-reference array and compares exact String values and
context fields. Its identity is VM-local. Array order, implementation revision, ABI, flags,
definition input and dependency changes prevent reuse. A matching key alone cannot bypass
verification, linking, initialization, native lifetime rules or JIT deoptimization.

## Recipe and proof

The retained `M3HashPinnedJavaSnapshotRecipe` generates all fourteen product owners and
both probe sources from a hash-pinned manifest. The retained bootstrap recipe inventories
the packet without direct mutation authority. `recipe-pin.tsv` guards those owners and the
existing VI/Release dependencies. Drift, wrong parser, tampered templates and unrelated
inputs are tested, followed by a generation fixed point and strict Java 21 compilation.

From the repository root with Java 21 and Maven available:

```sh
mvn -f m3/tooling/cb/pom.xml test
python m3/tooling/cb/test_install.py
python m3/migration/migration.py validate . --previous m3/tooling/cb/integration/name-mapping.json.before
```

The runtime probes contain 4,515 assertions per interpreter/mixed run. They compare class
flags, names, hierarchy, fields, constructors, methods, records, sealed classes, casts and
assignability with reflection; cover bridge-method return selection, hidden/bootstrap/array/
primitive classes, defining-loader isolation/delegation/unloading, preparation budgets and
cancellation, concurrent declarations, invalidation, and precompute mutation/equality.
`jdeps` must report only `java.base`. `evidence/receipt.json` identifies exactly what ran.

`install/plan.json` replays the generated product sources with the existing sealed installer.
`integration/plan.json` requires those exact sources and installs the naming/legal metadata
and byte-identical generated tests under `test/jdk/jdk/internal/mindex/cb`. Apply source then
integration; rollback integration then source. Both plans reject drift before writing and
have a fixed point. The preceding VI plan retains its historical metadata preimages; it
correctly refuses to overwrite this later mapping. Its source kernels remain unchanged.

The JDK test entry point is:

```sh
make test TEST="jtreg:test/jdk/jdk/internal/mindex/cb"
```

The complete Linux x86_64 fastdebug image builds with warnings treated as errors.
On that image, three jtreg tests pass: Class bridge (4,525 assertions in each of
interpreter/mixed modes), mapped String backing, and native null ingress (1,000 calls
in each of default/enabled modes). The assertion count differs from stock Java because
reflection exposes the fork's extra String metadata. Separate C1-only and non-tiered C2
runs also pass 4,525 assertions each; compilation logs contain the new counterpart methods.
The existing String/JNI oracle passes 809,315 checks per default-mixed/enabled-interpreter
run with identical semantic digests. VI passes 3,061 checks in each image mode.

`run-image.sh` reproduces these checks with `M3_JDK`, `M3_BOOT_JDK` and `M3_JTREG`
pointing to the built image, boot JDK and jtreg.jar. It uses no java.base patching.
Enabled String storage remains interpreter-only; the C1/C2 proof covers default VM mode.

The original Synexia ClassIndex suite also references class search/hierarchy owners that
are outside this packet. It has not been represented as an executed upstream suite.

The strict image build also exposed an existing `-Xlint:try` warning in
`MIndexStringBacking`: inherited `AutoCloseable.close()` permits `InterruptedException`.
The existing mapped implementation already has a non-throwing close. The separate
`m3-close` recipe declares that narrower interface contract explicitly, preserving the
file's GPL-2.0-with-Classpath-exception header. Two additional tests prove the exact
rewrite, fixed point, drift/missing-source refusal and strict compilation. Its independent
sealed plan is `close/plan.json`; no compiler-warning suppression is added.

The next native build exposed invalid nested `CHECK_NULL` macros in the existing JNI
`NewString` and `NewStringUTF` paths. The `m3-jni` packet reuses the retained hash-pinned
OpenRewrite text recipe and returns Handles directly from `create_from_unicode` and
`create_from_str`. This retains GC rooting and checks exceptions before M3 admission.
The native hook now also skips a null Handle, preserving `NewStringUTF(null)` in enabled
mode; the traced result is initialized before any exception return. A generated native
jtreg regression checks 1,000 calls in each of default and enabled modes.
Its native file keeps its original OpenJDK license; `jni/plan.json` holds exact preimages
and rollback. Native compilation and JNI execution, not Java parsing, validate this change.

## Remaining integration

The internal package is not newly exported. Public Class/String names, ClassLoader linking,
module resolution, class-file formats and JVM version reporting retain their contracts.
Automatic counterpart attachment, authenticated definition capture, redefinition hooks,
CDS/GC/agent/platform matrices, Class-specific JNI/HotSpot routing, AST/compiler consumers and JIT/AOT
reuse remain separate gates. Actual image-build results belong in the evidence receipt;
patch-module success is not a substitute for a matching fork build. Benchmarks must precede
performance claims. Incompatible identity/linkage/class-file changes belong in the separate
experimental JVM project. The full MIndex-family census remains REVIEW.
