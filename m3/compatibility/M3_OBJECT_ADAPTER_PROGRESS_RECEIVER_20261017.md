# M3JDK21 Object adapter / progress default-method candidate

Status **DRAFT / UNQUALIFIED**. No existing JDK public surface, String/arrays/collections stage, or HotSpot/GC/JNI phase is declared complete.

## Exact source handoff
- Canonical Synexia Maven/OpenRewrite recipe: [Synexia #12062](https://github.com/hsoliwal/com.synexia/pull/12062), exact authoring commit `338a7360ed9fde255374a7ba569db03f04238bfe`.
- Named recipe: `com.synexia.m3.ObjectAdapterProgress20261017` / existing class `M3HashPinnedJavaSnapshotRecipe`; manifest and full reviewed target Java templates reside in `synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-object-adapter-progress-20261017/`.
- M3JDK receiver base: `master@a918e588dd11d5ff7e0a6d4efaa89bb1679a9798`, pinned original preimage and candidate postimages in `m3-object-adapter-progress-20261017.tsv`.
- All 11 proposed code/test blobs are reproduced verbatim from the donor's reviewed templates. No target-side master recipe, source-generating hand patch, or Synexia runtime dependency.

## Target-specific compatibility / naming
- `java.lang.Object#toAdapter(Class)` is a *new public JDK API candidate*, separate from existing Synexia `IAdaptable#adapt(Class)` which returns Optional. Runtime implementation defaults to identity-compatible cast, null on mismatch and NPE for null Class.
- Concrete context types must explicitly bridge `toAdapter` to `adapt` where desired; interface defaults lose to the `Object` method. Do not assume an inherited `IAdaptable.toAdapter` default can override it.
- `Runnable`, `Callable`, `Executor`, `ExecutorService`, `Consumer`, `Function`, `Supplier` acquire *new default overloads*; `progressMonitor()` is on Runnable only (not Callable) after an executable Java21 unrelated-defaults collision was reproduced and repaired in the Synexia recipe; preexisting abstract SAM and ordinary method contracts do not change. Nonetheless adding a new Object method or default overload is source/reflective-ABI-sensitive and remains a deliberate compatibility gate.
- `ProgressMonitor` and `ProgressMonitors` live in java.base with a process default, weighted child, lexical scope, bounded completion and explicit handoff. Zero Object resident fields. Not an Eclipse JobManager replacement; jobs, families, scheduling-rule locks and UI services require separate design.
- This API does not replace M3Index collections or filtered/sorted wrappers. Follow already active #1061-#1070, respecting array/index/offset/mask storage ownership and serial recipe gates.
- No JNI implementation is proposed for these Java-only progress operations; if a native consumer becomes necessary it needs independent JNI ABI and lifetime tests.
- A target `m3/docs/name-mapping.json` addition is held for qualification, not silently asserted by a draft that has not run JVM product gates.

## Actual evidence / unexecuted gates
- Standalone Java 21 compiler and runtime proof for progress classes executed locally with `--patch-module java.base`: PASS for scoped lookup, child weights, large integer scaling, cancellation and idempotent completion. This is **not** a modified M3JDK21 build.
- Receiver verifier `check_object_adapter_progress_receiver_20261017.py` tests exact handoff identity and refusals (none absent); the standalone proof script exercises the progress-only subset.
- **Not run:** Maven/OpenRewrite actual SDK replay and refusal/fixed point, full M3JDK `java.base` build, jtreg `M3AdapterProgress`, JVM bootstrap/CDS, GC/JIT, reflection/source override collision corpus, native runtime and performance.
- Do not mark this PR ready for merge until required product gates execute. Keep recipe fixed and replay; do not manually patch generated target sources.

## Commands
```sh
python3 m3/compatibility/check_object_adapter_progress_receiver_20261017.py
bash m3/compatibility/verify-object-progress-standalone.sh
```
