# M3String-backed ordinary String integration

This candidate adapts the Synexia MIndex String donor model into the M3JDK target runtime.
Synexia's `com.synexia.indexstring.MIndexString` is the donor/reference type; the M3JDK
counterpart is `java.lang.M3String`. M3 target naming is authoritative inside the JDK.
The target preserves the donor owner+coordinate structure and retains the tested VM boundary
protections. It
requires a complete matched OpenJDK/HotSpot image. Do not transplant classes or
replace the system JDK. The flag is `-XX:+UnlockExperimentalVMOptions
-XX:+UseM3StringStorage`; it forces interpreter mode. Compiled mode, CDS, JFR,
JVMCI and dedup remain unsupported while enabled. Flag off remains the default.

## Source custody and existing owners

- Official upstream: jdk-21+35, `890adb6410dab4606a4f26a942aed02fb2f55387`.
- Tested safety ancestor: M3jdk21 #5, initially `c5c2a344baaa2db400c0e4b7a74e8641d25aefd1`.
- MIndex source input: M3jdk21 #4, `fbe45aec581d992af6192756f9e1358eec587582`.
- Existing shared owner: com.synexia #7498, `c6cb340d33323348fab94e8157842455e9b72bc2`.
- Replay base: #5 with its CI repairs, `3dbb233235d5e3d539ed2cce645e6e2b2952576c`.

`owner-pins.json` seals the existing Synexia shared-owner sources. `prepare-owner.py`
extracts them from a separately fetched Git object database; it does not change
Synexia's branch. The fixture is created by that exact `SharedArrayPool`, not by
an imitation writer. No new native arena or JNI storage implementation is added.

The runtime accepts both #4's `M3LEX001` UTF-16LE image and the existing owner's
`SYNARR01` UTF-16BE arena through `-Djdk.mindex.lexicon=/absolute/file`. For SYNARR01
it maps the original `arena.bin` read-only, validates committed records/CRC/hash,
and admits language-0 UTF-16 scalar records. Byte records, other languages and
persisted tuple records are not independent String lexicon atoms. Odd addresses
are supported. Misses never append to the owner file. The mapped extent is a
startup snapshot; later appended entries require a new VM to become lexicon hits.

The original owner's publication contract still applies: trusted directory,
immutable committed bytes, no external rewrite or truncation. Read-only mapping
alone does not enforce that contract against another writer. The file channel
closes after mapping; descriptors retain mapped ownership. The tests unlink the
file only after both child VMs have mapped it, then verify content after GC.

## Storage and Java behavior

After VM bootstrap, trusted/array/charset/builder/copy constructors may admit content
to M3String storage before publishing the logical String value. A shared hit retains its
mapped scalar owner. A VM-local miss moves the immutable spelling into canonical native
memory; M3String itself retains only one owner reference plus one packed coordinate.
For M3-backed Strings the legacy `String.value` array is a compatibility sentinel rather
than canonical text. Local and tuple lookup tables hold weak references, with queued
metadata/native cleanup during admission. This is not a fixed total-memory budget:
metadata scales with live atoms/tuples and pending cleanup. The finite mapped lexicon
remains owned for the VM's lifetime.

Two-reference `+`, `concat`, eligible general invokedynamic concat recipes,
`substring`, bounded `repeat` and bounded `String.join` compose scalar/range tuples.
Tuple geometry is maintained as a bounded-height persistent DAG. Canonical tuple reuse is
independent of binary-tree parenthesization: the same normalized ordered terminal M3 atom
owner/range sequence converges after exact verification. Candidate hashes route lookup only;
hash equality never proves canonical identity. Full and partial slices retain the same owner
and change only their packed coordinate, including small slices of large atoms; that retention
is deliberate. Java hash and VM StringTable hashing avoid unnecessary rescans. Original String
identity and content-based `intern` semantics remain separate from M3 coordinate identity.

Pre-bootstrap wrappers may retain their original flat byte arrays because VM bootstrap
cannot retroactively rewrite already-published final layout state. Post-activation M3-backed
Strings use M3String as semantic authority. Java `byte[]` / `char[]` values are projections
or compatibility shadows, not canonical M3 payload. Public copy APIs return fresh caller-owned
arrays; JNI/native String access similarly materializes/release-manages shadows. General recipes
linked before activation and explicitly unsupported boundaries may retain contiguous fallbacks.
There is no claim that every consumer is zero-materialization.

VM readers acquire the M3String coordinate and descend the M3 owner graph. Native UTF-8
and UTF-16 conversion paths produce compatibility shadows from canonical M3 text. JNI critical
acquisition copies while the flag is enabled, so release ownership remains explicit. Returned
shadows never become canonical M3String storage. JNI/JVMTI tests inspect the boundary before and
after native traversal. SA's decoder is adapted and compiled; live debugger attachment is not tested.

## Internal precompute invariant

All String precompute is implementation-internal in both worlds. Synexia donor precompute maps to
M3JDK internal fact/search lanes; it is not public `java.lang.String` API. Fixed facts attach to
canonical owner/range identity, length-proportional plans live in separately bounded caches, and
absence/eviction of any fact changes performance only. Precompute never becomes a second spelling
store and never owns the canonical text.

## Code-point range/navigation lane

The Synexia donor history for prepared UTF-16 range metrics and code-point navigation is adapted
internally as `M3StringCodePointPrecompute`. This lane is deliberately separate from
`M3String`, `M3StringOwner`, and fixed `M3StringFacts`.

- cache key: exact canonical M3 owner identity + packed coordinate;
- cache bound: 64 direct-mapped weak-owner slots;
- source bound: 32,768 UTF-16 units;
- retained primitive payload: cumulative valid-surrogate-pair counts at 64-unit block boundaries;
- no String, M3String, byte[], char[], or spelling payload is retained;
- whole-value counts continue to reuse canonical fixed facts;
- small/one-shot ranges keep the existing exact range-fact path;
- large/repeated range/navigation work may admit the bounded block-prefix lane;
- small offsets use exact direct traversal rather than paying a whole-source preparation cost;
- cache miss, eviction, budget refusal, or OutOfMemoryError changes performance only.

`String.codePointCount(begin,end)` and `String.offsetByCodePoints` delegate through M3String when
M3-backed. Split surrogate boundaries and unpaired surrogates remain exact JDK UTF-16 semantics.

## Boundary with the Synexia donor MIndexString

The mapped payload owner is now interoperable with #7498: ordinary Strings and
its SharedArrayPool reader can use the same committed file bytes. This does **not**
make the application's resolver IDs, language coordinates, canonical tuple IDs or
native handles interchangeable with VM-local descriptor IDs. In exact #7498,
`ExactStringIndexResolver` still owns its separate growable primitive arena, and
`com.synexia.indexstring.MIndexString.asString()` still calls `materialize()` through
StringBuilder. That application boundary is not silently rewritten in this JDK PR.
Sharing those local arena ranges directly would require a frozen ownership API
and coordinated resolver/compiler changes; taking mutable arena pointers would
violate this experiment's immutable backing contract. JNI is optional throughout.

## Replay, build, tests, rollback

`recipe/runtime.patch` contains the complete production diff. `manifest.json` seals
every before/after file, including file creation. Replay refuses drift, mixed state
and symlinks before writing; reverse restores the precise prior runtime. Tests work
without Git history and execute all preceding recipe gates on reversed snapshots.

```
python3 m3/runtime-integration/recipe/apply.py --check
python3 m3/runtime-integration/recipe/test_recipe.py
source /workspace/m3-build-deps/env.sh
make images JOBS=5
python3 m3/runtime-integration/prepare-owner.py \
  --git-dir /path/to/synexia.git --output /path/to/exact-owner-sources
export M3_TEST_JDK=/path/to/matched/image
export M3_BOOT_JDK=/path/to/original/java21
export M3_OWNER_SRC=/path/to/exact-owner-sources
export M3_RESULTS=/path/to/results
bash m3/runtime-integration/run-focused.sh
bash m3/runtime-integration/run-api.sh
python3 m3/runtime-integration/test-gates.py
python3 m3/runtime-integration/protocol-tests.py
python3 m3/runtime-integration/two-process.py
bash m3/runtime-integration/run-allocation.sh
# Also set JTREG_JAR and optionally M3_NATIVE_DIR:
bash m3/runtime-integration/run-jtreg.sh
```

See evidence for actual passes and remaining failed gates. Allocation-dependent
upstream OOME expectations are not weakened or rewritten. Narrow smoke/API passes
are not full jtreg/JCK conformance, and no general performance claim is made.
Rollback selects the preserved original Java binary/JAVA_HOME. Reverse replay
requires rebuilding the complete image before execution.

## #5 hosted CI repairs

Job 110539018686 passed the source postimage check but failed all replay tests:
checkout depth was one, so `git show` could not retrieve the historical base.
Commit `9c04848c6b8bc82e4340e50b0aef90845fffd552` reconstructs exact preimages by
reversing the sealed patch; exported-tree tests pass. Hosted jobs 110552018106 and
110560751287 confirm that recipe and foundation gates then passed. Their subsequent
cross-repository source download returned HTTP 404. Updating to the exact reachable
#7498 source closure in `3dbb233235d5e3d539ed2cce645e6e2b2952576c` passed 786,432 local
content/range checks, but the hosted download still returned 404. Repository metadata confirms com.synexia is private and M3jdk21 is public. That external
access gate requires authorized cross-repository credentials and remains enabled/unresolved;
private reference implementations are not vendored into the public repository. No hosted
full pass is claimed.
