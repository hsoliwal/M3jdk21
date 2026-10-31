<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 Object bootstrap vtable prerequisite

The shipped `java.lang.Object` has six non-final instance methods: `hashCode`, `equals`, `clone`, `toString`, `finalize` and the M3 `toAdapter` method. The pinned HotSpot `ClassLoader::compute_Object_vtable()` still reserved five slots before Object loading. Array metadata is allocated during this early phase. The later size check correctly rejected the inconsistent definition.

This named, one-owner recipe updates the existing bootstrap count to six and explains the exact method census in its local comment. It preserves the Object API, adapter override behavior, native bindings, object headers and all mismatch guards. The correction is a prerequisite for running the already integrated M3JDK21 baseline.

## Canonical source and admission

- Baseline: public `hsoliwal/M3JDK21` commit `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.
- Owner: `src/hotspot/share/classfile/classLoader.cpp`.
- Recipe: `com.synexia.m3jdk.ObjectBaseVtable20261018`.
- Existing engine: `com.synexia.rewrite.M3HashPinnedTextSnapshotRecipe`.
- Maven coordinate: `com.synexia.verification:m3-object-base-vtable-recipe-proof:1`.
- Before SHA-256: `665bb788428c8d3837d052ac4f69a2756bb1823918726b41a7a7008eadfafd8d`.
- Generated after SHA-256: `c465bc54b703617bad3ab70c25041c3182363afb95b82ea9640f20e29f032b0c`.

The actual OpenRewrite SDK Results are the only candidate input to the native proof. Admission refuses missing, duplicate, drifted or late-changed inputs and corrupt resource payloads. Replaying the exact admitted output is a fixed point. The generated source was applied only after the six SDK groups passed.

## Fresh qualification on 19 October 2026

All evidence below was rerun after the transient workspace reset. Earlier unavailable logs are not used as current receipts.

| Gate | Fresh result |
| --- | --- |
| Maven/OpenRewrite SDK | 6 tests, zero failures, errors or skips |
| Source context | 10 unchanged owners sealed by SHA-256 |
| Actual built Object.class | Six virtual methods; existing native wait0(J)V binding matches |
| Whole C++ owner | Original and generated owners compile with the actual recorded strict HotSpot command and GCC 13.3 |
| Native symbols and addresses | Identical |
| Whole native object comparison | Exactly one byte differs: offset 14149, immediate value 5 becomes 6 |
| Java fixture compilation | Verified Temurin 21.0.12.1+1; --release 21, --patch-module java.base, -Xlint:all, -Werror |
| Actual exploded M3JDK21 image | Normal and -Xint executions both pass with -Xshare:off and -Xverify:all; no runtime patch |

The actual-image fixture exercises default and overridden adapters, calls through Object references, all primitive array kinds, reference and multidimensional arrays, hash/equality/text/clone behavior, explicit finalizer dispatch and monitor ownership, waits, notification and interruption on platform and virtual threads. Its receipt identifies the executable, libjvm and fixture hashes.

The public Java `wait(long)` wrapper and private native `wait0(long)` already match the value of `vmSymbols::wait_name()`: the symbol expands to `"wait0"`. That owner is retained. Identifier spelling alone did not justify a binding change.

Full image packaging and CDS are a separate parent integration gate. The normal/interpreted fixture result proves this bounded bootstrap correction in the actual exploded image; it is not a general M3 runtime phase promotion.

## Reproduce from the canonical checkout

Use Java 21 and Maven, retaining the normal strict toolchain configuration:

```sh
mvn -f recipe-crates/m3-object-base-vtable-20261018/pom.xml test
mvn -f recipe-crates/m3-object-base-vtable-20261018/pom.xml \
  -Pnative-owner-proof -Dm3.target.root=/absolute/path/to/M3JDK21 verify
python3 recipe-crates/m3-object-base-vtable-20261018/contract/verify_runtime_image.py \
  --jdk /absolute/path/to/M3JDK21/build/m3-p0/jdk
```

The native proof requires the configured build's actual `classLoader.o.cmdline` and its compiled `Object.class`. Runtime verification requires the rebuilt image and the fixture classes emitted by that native/context proof. It does not modify the running image. See `CONTRACT.md`, `CATALOGUE.tsv`, `PROVENANCE.md` and `proof/recovery-20261019` for the exact gates and receipts.
