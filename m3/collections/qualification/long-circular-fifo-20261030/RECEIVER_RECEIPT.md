<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3LongCircularFifoQueue — independent primitive donor convergence (draft)

## Source and name

- Target repository: `hsoliwal/M3jdk21`; exact base master: `5860598b515885ebc2275f6552d9a5a1348fdb15`.
- Product: `m3/collections/src/main/java/jdk/m3/collections/M3LongCircularFifoQueue.java`, Git blob `7d19d615e71db271fcc362a4e335de984bca35de`, SHA-256 `8b5d5e8b6799074e502e0ccec22eba2f648707955d732c648d60c0910f579062`.
- JUnit: `m3/collections/src/test/java/jdk/m3/collections/M3LongCircularFifoQueueTest.java`, Git blob `4ce37efa670a9adb597eeb09cc18ae25d4daea9d`, SHA-256 `eecae48e3c8d4007488f4e6adc4ef7759872534c59be4490e4e6e495cee3abc6`.
- Canonical authored Maven/OpenRewrite dataset: [Synexia PR #14165](https://github.com/hsoliwal/com.synexia/pull/14165), `synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/datasets/long-circular-fifo-20261030`.
- New API name: `com.m3.collections.M3LongCircularFifoQueue`; implements existing `M3LongQueue`, not `java.util.Queue`, not a replacement for Guava/Apache API/serialized identities. Both receiving paths were absent at frozen master. No changed public methods or JNI symbols in existing owners.

## Donors and behavioral atoms

- Category donors: `hsoliwal/commons-collections` `com.m3.collections.apache.queue.CircularFifoQueue` (blob `4647485b0b0787981579f22693d2f6b65ffe42ab`), `hsoliwal/guava` `EvictingQueue` (blob `a75f0997dbb873c925263fe7404253cfa663fa39`).
- **No donor Java code copied**; both object-based upstream classes remain in their separate namespaces with their own license notices. New code is Apache-2.0 first-party, no new runtime/downstream dependencies.
- Primitive `long[]`, head/size offsets; bit mask is not needed because a bounded ring need not be power of two. `offer` overwrites oldest when full; `peek/poll` throw on empty per `M3LongQueue`; signed extrema/zero valid payloads. No Java callback or reference crosses JNI.
- `offerBatch` bounds preflight before mutation; when input length >= capacity, one `System.arraycopy` retains the source window suffix in O(capacity), otherwise repeated O(length) primitive `offer`. Iterator fail-fast across structural updates; arrays detached by `toArray`.

## Evidence and gate

- Local stock Java 21: exact source compiled with `--release 21 -Xlint:all -Werror` against byte-for-byte target M3LongIterator, M3LongCollection and M3LongQueue sources (Git blobs `44778a63`, `d0b38968`, `8fcb61bd`); still not a full JDK module build. Canonical Synexia packet now contains `verify_java21_source.sh`, which rejects changed input blobs and reproduces the 99,000-transition proof.
- Independently authored ArrayDeque<Long> oracle (test-only boxing): 99,000 seeded state transitions / 3,483,902 checked assertions **PASS**. JUnit 5 method content compiled/executed against an annotation stub; actual project JUnit **NOT RUN**.
- This draft contains **authored source templates**, not recorded SDK-emitted output. Canonical Maven/OpenRewrite 5 SDK cases, actual receiver fixed-point/refusal, source-built M3JDK21 Maven reactor/JUnit, relevant jtreg/JNI, multi-platform and JMH gates **NOT RUN**.
- Keep **draft** until the full recipe, name-contract policy and product gates pass. No default branch writes, no rebase, no forced update, no phase-promotion claim.

## Required qualification

```sh
cd synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017
mvn -B -ntp -Pcontract -Dm3.dataset=long-circular-fifo-20261030 verify
python3 receive.py --dataset long-circular-fifo-20261030 --target /absolute/isolated/M3jdk21 --check
python3 receive.py --dataset long-circular-fifo-20261030 --target /absolute/isolated/M3jdk21
python3 receive.py --dataset long-circular-fifo-20261030 --target /absolute/isolated/M3jdk21
# Last invocation zero changes, then exact M3 module/JDK/JNI platform tests and JMH.
```
