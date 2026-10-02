# MIndex-backed ordinary String integration

This candidate replaces #5's provisional String leaf directory with #4's canonical
`java.lang.MIndexString` owner and retains #5's tested VM boundary protections. It
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

After VM bootstrap, trusted/array/charset/builder/copy constructors admit content
to MIndex storage before publishing final String fields. A shared hit retains its
mapped scalar and no temporary constructor payload array. Equal live local misses
share the canonical immutable Compact-String array. Local and tuple tables hold
weak references, with queued metadata cleanup during admission. This is not a
fixed total-memory budget: metadata scales with live atoms/tuples and pending
cleanup. The finite mapped lexicon remains owned for the VM's lifetime.

Two-reference `+`, `concat`, eligible general invokedynamic concat recipes,
`substring`, bounded `repeat` and bounded `String.join` compose scalar/range tuples.
Equal live geometry shares one tuple body. Full and partial slices retain the same
backing, including small slices of large atoms; that retention is deliberate.
Full-atom hashes combine precomputed Java hashes with powers of 31. Java hash and
VM StringTable hashing avoid rescanning full joined atoms. Original String identity
and content-based `intern` semantics are separate from descriptor identity.

Pre-bootstrap wrappers retain their original final byte arrays and may attach
canonical metadata later. They cannot discard those arrays retroactively.
Empty Strings may keep the ordinary empty representation. General recipes linked
before activation and large join/repeat paths retain explicit contiguous fallbacks.
There is no claim that every concat shape or every consumer remains unmaterialized.
Other byte-array consumers use a separate descriptor cache. `toCharArray` traverses
atoms directly into its required result, without allocating an extra byte cache.

VM readers acquire the volatile descriptor. Native UTF-8 conversions retain #5's
no-scratch-allocation buffer paths. JNI critical acquisition always copies while
the flag is enabled, so later lazy descriptor attachment cannot change release
ownership. JNI/JVMTI tests inspect the Java cache before and after native traversal.
SA's decoder is adapted and compiled; live debugger attachment is not tested.

## Boundary with the application MIndexString

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
