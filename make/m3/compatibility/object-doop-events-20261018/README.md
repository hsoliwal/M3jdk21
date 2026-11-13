<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# M3JDK21 lazy Object observation candidate

The existing `Object.toAdapter` name is retained. `object.toAdapter(M3Object.class)` returns a lazy identity-bound capability. `M3Object` is an optional facade over an existing owner, not a superclass requirement. No fields are added to `Object`.

```java
var owner = object.toAdapter(M3Object.class);
var field = owner.field(MethodHandles.lookup(), MyType.class, "child", Child.class);
var subscription = field.listen(ObjectEvent.REFERENCE | ObjectEvent.CONTENT, event -> refresh());
field.set(nextChild);
// Cooperating child mutation producers call M3Object.of(child).contentChanged().
// Keep the owner/subscription reachable for the observation lifetime.
subscription.close();
```

Field descriptors retain Lookup authority and resolve their VarHandle only when used. The exact declaring class and field type identify a slot. Final/inaccessible/static fields do not gain write access. Root listeners expose no other subscriber's field capability. `get` is a volatile read; `set` is an atomic exchange; `compareAndSet` preserves the field's CAS semantics. Reference identity and primitive raw bits determine change.

## Asynchronous, explicit, bounded by observed state

Enable with the startup property `-Djdk.m3.objectEvents.enabled=true`; the default is false. The policy is read lazily from VM-saved startup properties. Disabled registration returns a shared closed subscription without resolving fields or allocating observer lanes, edges or a worker. Enabled authorized registration lazily starts one existing JDK InnocuousThread dispatcher. Callbacks run outside producer locks and never inline. One slow callback delays other deliveries on this shared worker; producers do not execute it.

Pending event kinds and sequences are coalesced in growable arrays, with numeric field/subscription slots and binding generations. At most one queue entry exists per dirty owner. Event envelopes materialize at delivery; `currentValue()` reads the latest value, not historical content. A weak identity registry avoids globally rooting source/callback graphs. Its current lookup is linear in registry capacity; no throughput or zero-overhead claim is made.

CONTENT follows only explicitly observed reference edges. A covered reference replacement rebinds that edge; generations discard pending invalidations from the detached child. Origin wave deduplication terminates graph cycles while distinct alias slots remain distinct. Changes through arbitrary Java fields, arbitrary VarHandles, Unsafe, arrays or unmodified JNI setters are not automatically intercepted.

## Native producer ABI

`src/java.base/share/native/libjava/m3_object.h` exports `M3Object_NotifyContentChanged` and `M3Object_NotifyFieldChanged`. Producers call after a successful unequal committed mutation. The bridge preserves a pending JNI exception, retains no global references, and uses the same Java observation queue. There is no second native event owner. `JNI_TRUE` includes disabled/unobserved calls and does not promise delivery. Notification failure cannot roll back an already committed native store.

The JNI fixture includes the actual product C source. Source discovery uses the existing libjava build rules. Installed product symbols, platform builds, complete jtreg and automatic VM-wide store capture require separate qualification.

## Reproducible bounded qualification

Use a matching JDK 21 GA compiler and runtime with a Linux GCC/G++ toolchain. For the pinned 21.0.0 HotSpot, the qualified configuration uses a pristine Temurin 21+35 compiler JDK and a separate complete Temurin 21+35 runtime overlay with only `lib/server/libjvm.so` replaced by the built VM. A HotSpot-only build directory lacks its boot class libraries; a newer update runtime may require different native VM exports. A newer compiler platform can also select standard-library methods absent from GA, even for unchanged source. Keep the pristine compiler separate because the M3 VM requires the complete Object and M3 String patch to boot. The script downloads nothing and refuses source drift against `sources.tsv`:

```sh
python3 m3/compatibility/object-doop-events-20261018/verify.py \
  --java-home /path/to/pristine-jdk-21+35 \
  --output /path/to/fresh-evidence \
  --hotspot-jvm /path/to/matching-runtime/lib/server/libjvm.so
```

The ordered stages are source admission/lint, patched product Java compilation, Java tests plus JNI header compilation, C17 product fixture and C++20 header compilation, ABI declarations, Object adapter dispatch in default/interpreted/forced-C2 modes, Java enabled/disabled tests, interpreted Java, and enabled/disabled/interpreted JNI with `-Xcheck:jni`. Logs/commands/hash receipts remain in the requested fresh output. The existing non-final Object.toAdapter requires six virtual slots. The native bootstrap atom updates ClassLoader::compute_Object_vtable to six while preserving the compatibility guard. A stock five-slot VM rejects this Object class; provide a compatible rebuilt M3JDK or the actual built libjvm.so via --hotspot-jvm. This is a bounded rebuilt-VM/patch-module gate, not a full rebuilt M3JDK image.

Canonical recipe: `hsoliwal/com.synexia/synexia-openrewrite-recipes`, named `com.synexia.m3.ObjectDoopEvents20261018`, with separate existing hash-pinned Java and native/text engines. Canonical contracts, category/donor comparison, exact pre/post manifests, fixtures and SDK fixed-point/refusal proof stay there. The historical adapter/progress recipe remains immutable and has drifted inputs; it is not the authority for this successor.

The source manifest includes the fourteen existing M3 String bootstrap dependencies required by this fork's VM. Thirteen are unchanged at the pinned base; `M3TQ.java` receives only its missing final LF through the canonical recipe. These are compiled with the Object candidate. The existing M3 String storage feature retains its default disabled state; its separate native storage and concatenation paths are outside this object qualification.

The additive role mapping is `m3/docs/name-mapping.d/m3-object-doop-events-20261018.json`. Existing MIndex snapshot/sidecar owners are conceptual donors, not renamed or replaced by this live capability.
