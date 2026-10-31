<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Qualification of program escape precomputation

**Final v4 source qualification: PASS with the explicit historical custody HOLD.**
The named Maven/OpenRewrite recipe executed all five SDK tests, then satisfied
all 15 contract-command outcomes against its actual emitted Results. The
complete candidate port suite ran **30 tests, zero failures, zero errors and
zero skips**. No product target or remote repository was mutated by this gate.

The result is bound to public target revision
`d47a4b4fdb8e3794ec56768b06aa9e23730c6b8e` and canonical Synexia owner/donor revision
`81c4f19a5ce5a68da316ff6f2802f2041a684f31`. Receiving application and a combined
current-tree/product build are separate target-owner gates. The name-map
fragment remains `PROVING_GROUND_CANDIDATE` with no phase advancement.

## Executed scope

| Gate | Executed outcome |
| --- | --- |
| Actual SDK, named YAML descriptor and four current owners | 5 tests, no failure/error/skip: deterministic input order and fixed point, mixed pre/post states, admission refusals, post-scan drift refusal, and retained source-bound Results. |
| Frozen parent admission | All 141 parent files and all five target pre/postimage states matched their exact seals. |
| ASM source compilation | All 84 unchanged vendored core/tree/analysis Java files compiled with Java 21 `--release 8`. |
| Original full-port compile | Reproduced the exact duplicate `destination` declaration failure. |
| Available original port | Compiled all available baseline sources and ran its 18 unchanged tests successfully. The two GVAS tests are excluded only from this failing-baseline diagnostic. |
| Generated full-port compile | All 57 Java source files from the complete compile-time port plus precompute main-source dependencies compiled with `--release 21 -Xlint:all -Werror`. No candidate owner or test was excluded. |
| Generated full-port tests | All 30 tests passed, including both original GVAS tests and ten prepared-escape tests. |
| Original SWAP witness | Reproduced `SWAP_REFERENCE_ESCAPE_LOST expected=1 actual=0` against the frozen original owner. |
| Corrected SWAP witness | The generated owner returns `FIELD`; the JUnit corpus also loads and executes four legal bytecode witnesses showing the actual field store. |
| Source/API contracts | Original forward source differs only by the admitted SWAP case; GVAS differs only at the three input-binding occurrences; `javap -public` output is equal. |
| CPU/allocation/retained result | The bounded actual-VM probe completed; raw measurements are below and retained in the receipt. |
| Read-only receiving check | Existing receiver reports `targets=5 changes=5` on the pristine pinned target, without writing. |
| Target integrity | All admitted source inputs remained unchanged; the pinned worktree was still Git-clean after qualification. |

The final JUnit counts are:

| Test class | Tests |
| --- | ---: |
| `M3EscapePreparedTest` | 10 |
| `M3ItemReferenceScanTest` | 7 |
| `M3GvasLiteralHarvesterTest` | 2 |
| `M3RegexRequirementTest` | 5 |
| `M3FormatPlanTest` | 2 |
| `M3ConstantRegexImageTest` | 4 |

Prepared-escape coverage includes comparison with the corrected forward
algorithm at every instruction; all 13 legal DUP/SWAP forms; 60 loop/branch
methods; exact overloaded callee policies and `requireNonNull` descriptors;
wide argument coordinates; complete escape-bit unions; null/dead frames;
precise budget boundaries and fallback; VM analysis failures; and independent
parallel/serial preparation. The four single-site SWAP scanner fixtures
exercise the final caller's forward admission; direct comparisons still
exercise the prepared path on those same witnesses.

## CPU, allocation and retained result

The Java runtime was `21.0.12.1+1-LTS`. SDK and source-gate JVMs used
`JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2`, recorded in `toolchain.json`,
with the same setting for baseline, candidate and probe. Source-test/probe
heaps were bounded at `-Xmx512m`. Maven 3.9.11, OpenRewrite 8.17.1,
Surefire 3.2.5 and JUnit 5.10.2 were resolved from the approved offline cache.

These are medians of seven rounds over fixed, already-computed ASM frames.
One query batch visits every obtaining site once. Forward and cold graph
measurements use 30 repetitions per round; prepared-array lookup uses 3,000.
CPU and allocated bytes come from the current thread's VM counters. The
last column is actual `Instrumentation.getObjectSize` of the sole returned
primitive array, rather than an inferred whole-process heap value.

| Sites | Instructions | Forward CPU (us/batch) | Raw graph preparation + queries CPU (us/batch) | Forward allocation (B/batch) | Raw graph allocation (B/batch) | Prepared lookup CPU (ns/batch) | Raw graph retained array (B) |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 23 | 3.136 | 12.846 | 304 | 864 | 73 | 112 |
| 8 | 121 | 82.817 | 18.552 | 31,424 | 6,224 | 39 | 504 |
| 32 | 457 | 449.902 | 35.948 | 106,304 | 20,008 | 32 | 1,848 |

The final scanner **uses the corrected forward query for exactly one site**.
The one-site raw graph row is an explicit cost comparison, not the selected
production path. Repeated sites share one graph; no-site methods are already
skipped by the existing scanner. The preceding v3 trial also showed a
one-site preparation cost (13.649 us CPU/864 B versus 8.660 us/304 B for
forward), which motivated this exact admission choice. Its entire successful
source proof and measurements are retained alongside the final proof.

All three prepared lookup rows allocated zero measured bytes. Their array
payload arithmetic is 92, 484 and 1,828 bytes; the VM reported 112, 504 and
1,848 bytes including its array representation and alignment. This measures
only the returned primitive result. It does not measure total peak JVM heap,
ASM-frame memory, native/process memory, or the complete compiler invocation.

The raw costs vary with JIT state and scheduling, as the retained v3 and v4
trials show. The prepared-lookup inner loop is not an independently measured
public API latency. The receiving scanner reuses the result only within one
method analysis; no cache survives across scanner invocations. Finite
synthetic measurements support this bounded admission choice and do not
establish a universal graph or application speedup.

## Exact final target hashes

| Target | Final SDK postimage SHA-256 |
| --- | --- |
| `m3/ports/compiletime/src/main/java/com/m3/compiletime/M3EscapeFlow.java` | `180488506ddfd0709530a7f164217d684389b4d5e77ce56b204bb0df4ad153de` |
| `m3/ports/compiletime/src/main/java/com/m3/compiletime/M3GvasLiteralHarvester.java` | `31dd73ae6b8621eb326af013560fb1fca09650248fa67dba7daf526f7c385dd6` |
| `m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ItemReferenceScan.java` | `eadaa78df40c19062ac56119fb123e31faf2dcc8e9b302e29a415bc7798e35f0` |
| `m3/ports/compiletime/src/test/java/com/m3/compiletime/M3EscapePreparedTest.java` | `ea8bed1a238df1964d11b49e3a5742f44f27ff262c0fe1741bbaa0bcf5a23dcb` |
| `m3/docs/name-mapping.d/m3-program-escape-precompute.json` | `d293b891ed743279037f4cf4417bf8efaae9fab186dee4a3cc6bb69dc78a0e8f` |

The Java manifest hash is `13e556fb54e01c3aac8ae328b6e75363ff626ae797652209f5e50ddc4c077c4a`;
the text manifest hash is `029f8135054b08b5b05b7e7b8ca62a4ed84614ac2a4ad04620e3a23848f8d293`.
The parent inventory hash is `f131fbdae5e460c89a93521873f540e93921b16729ceebb0b23eb10542eceee6`.

| Retained binding | SHA-256 |
| --- | --- |
| `evidence/qualified/SDK_INPUTS.tsv` | `e3c442a0973fe61a69c1768682023d1f1970b27bf24c2f945a4a5ba8aba0c4e9` |
| `evidence/qualified/SDK_RESULTS.tsv` | `69b360a1288ec8ecf5fdb9d67867e1b6f941e9e12f47cd538a1cf37343ab3c9d` |
| `evidence/qualified/SDK-five-tests.xml` | `065c2b1dcf7ad3400dcb5b07f9969b20d202efb7e446a1f9f0dd6b63baa2c6d9` |
| `evidence/qualified/receipt.json` | `4daa23b7a980ad7d0d500bd053f8a8a0ba52f6498aeac5a83c9afc29a2e32ec6` |
| `contract/input-seals.json` | `1deb105709e42e049ebb9245ab411c98aed97b7282e21816770e98e878e2c96c` |
| `payload-manifest.json` | `60a169a42c311294ddeb569cc649d16544752df19f5eacb20693811871050db2` |

## Retained failures and open gates

`evidence/refinements/20261017-v2-process-deadline` retains the 90-second
candidate Maven startup timeout, including its original contract/POM and
zero-output result. It is a failed attempt, not a passed suite. The final
replay changes only the documented process deadlines and isolated JVM
thread cap; it does not reduce any semantic workload or assertion.

The original whole-port custody checker still refuses its stale source
inventory with `crate targets differ from port sources (re-run seal_crate.py)`.
Its old inventory and checker were not weakened or resealed. The successful
successor gate explicitly binds this task's actual source set and records
that historical HOLD. Toolchain-selection failure/retry logs and the
original compile/SWAP failures are preserved separately.

This is full compile-time-port **source compilation and JUnit qualification**,
not a complete product Maven reactor, rebuilt JDK image, native/JNI run,
general escape/lifetime/GC proof, repository-wide source reconciliation or
receiving-family promotion. The receiving owner must record those applicable
current-tree/platform gates separately. The independent source/SDK review
is retained at `review/independent-graph-review.md`.

The compact published evidence consists only of text receipts, source argument
lists, XML test reports and logs. `evidence/qualified/COPY_INVENTORY.tsv`
binds each copied artifact. Compiled classes, jars, caches, settings and
transient baseline/candidate build trees are excluded.
