<!-- SPDX-License-Identifier: Apache-2.0 -->
# Optional per-tool JMOD receiver for M3JDK21

**Synexia donor authority:** `hsoliwal/com.synexia`, branch
`m3/cpu-tool-jmods-per-atom-20261014`, inspected source candidate commit
`1b5856bbfd3e21779aadbf1ed79e16ce4458a243`.

The donor implements 18 JDK 21 named source modules and a one-JMOD-per-tool
builder. The receiving M3JDK21 module names are **target owned**
(`com.m3.tools.*`); this map does **not** rename modules or make a runtime
dependency on `com.synexia.*`. The JDK's standard `String`, `Pattern`,
`BreakIterator`, `MessageDigest` and collection contracts remain public
authorities. The existing `M3ModuleDescriptorRecipe` and `jmod_pack.py`
are the target's module packaging authorities; no second image packer
or wildcard copy is added.

The source receiver requires, in order: exact donor source and license
admission, real 18-JMOD receipt, actual module names and byte hashes,
target-owned OpenRewrite package/descriptor rename with exact pre/post
pins, recompile into 18 *target-named* JMODs, repeat/fixed point, then
M3JDK21 independent module tests, jlink and installed-runtime testing.
Text Lucene/OpenNLP JARs are optional separate Apache-2.0 components;
they are not imported into java.base or mislabeled as first-party source.

`tools/verify_cpu_jmod_packet.py` verifies the **candidate donor JMOD
packet only**: exact mapping, canonical source manifest, all 18 nonzero
JMOD files with correct SHA-256, Java21 JMOD magic and genuine `jmod
describe` module name readback. It never copies/renames or installs
unqualified binaries. A passing packet is **custody evidence**; not
evidence that target modules have been built or linked.

Example when an independently checked donor worktree is available:

```sh
python3 m3/tooling/jmod-recipes/tools/verify_cpu_jmod_packet.py \
  --source-root /path/synexia-m3-jmods \
  --source-manifest /path/synexia-m3-jmods/tool-modules.tsv \
  --receipt /path/synexia-m3-jmods/target/JMOD_BUILD.tsv \
  --jmods /path/synexia-m3-jmods/target/jmods \
  --map m3/tooling/jmod-recipes/CPU_TOOL_RECEIVER_MAP_20261014.tsv
```

Status: candidate-only. **The current M3JDK21 receiver order is still
String -> arrays -> collections -> AST/compiler -> other families.**
Do not claim this work advances those runtime phases, completes OpenJDK
licensing review, or authorizes importing search corpus images into the
JDK. The prior SDK JMOD owner and its recorded 87-vendored-JMOD image
remain distinct; no historical receipts are overwritten.
