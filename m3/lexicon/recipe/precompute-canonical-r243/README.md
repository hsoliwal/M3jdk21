# M3Lexicons precompute canonical text, R243

The existing Python exporter emitted valid finite doubles that the existing Java 21 payload reader rendered differently. For example, `1e-5` became Python `1e-05` but Java `1.0E-5`; the two smallest subnormals became `4.9E-324` and `9.9E-324` in Java. DEL also differed. The catalogue requires exact canonical text equality, so these exports could not pass that check.

This slice changes one production owner: `m3/runtime-integration/export-m3-lexicon.py`. It preserves Python JSON validation, sorting, value kinds, finite doubles, signed zero, the payload budget, and existing acceptance/refusal behavior. It reuses `json.dumps`, `repr` and two-significant-digit `format`; a token pass applies Java 21 spelling while excluding quoted string contents. No JDK or JNI runtime implementation changes.

## Recipe and ownership

The production owner is replaced only by the unchanged, already-owned `jdk.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe` (Git blob `73779dee8c80a0913b88fff17637ed3f44b33ea3`). The four-column manifest pins the exact preimage and postimage. Source drift and a missing production owner are refused. The applied postimage is a fixed point.

The canonical convergence packet is `hsoliwal/com.synexia:synexia-openrewrite-recipes/crates/m3-precompute-canonical-r243/`. Its byte-identical receiver packet is `hsoliwal/M3JDK21:m3/lexicon/recipe/precompute-canonical-r243/`. The JDK also receives the generated exporter, normal Python regression test, and descriptor/resources in its existing migration-recipes module. No duplicate runtime owner is introduced in Synexia.

## Executed qualification

- Four JUnit recipe tests: exact replacement, metadata preservation, fixed point, drift/missing-owner refusal.
- Seven Python regression tests: numeric spelling and raw signed-zero preservation, literal escapes/DEL, value kinds, invalid input, fixed point, before/after size limits and existing donor-type acceptance.
- Three JUnit wire tests: 28,092 finite-double bit patterns exactly match the actual Java 21 `Double.toString`; 272 producer payloads exactly match unchanged `M3PrecomputePayload.canonicalJson()`; existing receiver refusals remain active.
- The corpus covers both signs, signed zeros, the first 1,024 subnormals, neighbors at every normal binary exponent, decimal-power neighbors and 5,000 seeded random magnitudes.
- Maven 3.9.14 executes the existing source launcher, compiling qualified Java sources entirely in RAM with `--release 21 -Xlint:all -Werror`. The Python test class body executes against the exact recipe-produced module; its filesystem import loader is replaced by that sealed RAM module.
- CPython 3.12.6 / Java 21.0.9 on Windows. Source, JAR, packet, output and receipt hashes are recorded.

`qualification/config.json`, the separate readable input fixtures, `qualification/run.py` and `qualification/replay.py` preserve the exact replay input and launcher. Run `python -I -S -B qualification/replay.py` from this crate to assemble the pinned inputs in RAM. The assembler verifies every input hash, reconstructs the tested input exactly, and refuses drift; those two properties were also executed in RAM. Paths identify provisioned read-only tool/cache inputs; a different machine must provide and qualify its own equivalent paths rather than claim this host proof.

Run the ordinary Python regression file in a checkout with `python -I -S -B m3/runtime-integration/tests/test_m3_precompute_canonical.py`. Use the existing migration-recipes Maven classpath and `recipe/descriptor.yml` to replay the text recipe. The included POM is the exact Maven/RAM qualification harness, requiring the pinned replay inputs rather than downloading dependencies.

## Boundaries and storage

Full reactor, complete filesystem export/catalogue opening, JNI and other-platform qualification were not run. No global speedup or exhaustive binary64 proof is claimed. This is offline producer correctness; runtime lookup ownership and layout stay unchanged. Existing unsupported receiver shapes still fail; this patch does not expand the Java payload schema.

The active 545 MiB task cap stayed intact: sealed read-only F input budget 571,469,824 bytes plus one exclusive 4 KiB temporary POM, removed after the child was reaped. K free space remained above 8 GiB; the proof retained zero task files. No clone, cache copy, browser/model download or new runtime-generation call occurred.
