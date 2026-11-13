<!-- SPDX-License-Identifier: Apache-2.0 -->
# CPU tool receiver: signed block INT8 quantizer (2026-10-17)

Synexia source snapshot: hsoliwal/com.synexia, module
`com.synexia.m3.model.quantize`; `BlockInt8Quantizer.java` Git blob
`399962a51d4781139e831e806764b482027c5120`, descriptor blob
`8d58d0814653b574ec8175207af226fb6887641e`, native source
`439841ccd49f24d15ea06aa73d0b72c3406a3265`, and original
JUnit-equivalent Java21 differential test blob
`7735c3a5e5f2df46397dbadd4fa5e401be95c58f`.

The independent product-side CPU module is
`com.m3.tools.model.quantize` under `cpu-tools/model-quantize/`.
The port is a **mechanical package/namespace transform** of the exact
Apache-2.0 Synexia Java/C donor, preserving the signed INT8 symmetric
per-block contract, 32..4096 power-of-two block sizes, partial blocks,
nearest-even rounding, finite input validation, defensive Packed
array ownership, exact byte/scale comparisons and explicit one-library
SHA-256 JNI admission. No model weights or GPU providers are included.

The receiving CPU catalogue advances from **20 to 21** donor module
entries: 19 candidate mapped modules, while `model.distill` and
`compute.webgpu` remain explicit separate qualification deferrals.
**QUALIFICATION_PENDING** is not a claim that the JDK image contains
or can execute an imported module. Quantization is intentionally lossy
and cannot substitute for float32 Java API or GGUF/Q8_0 wire contracts.

The independent `verify_quantize.py` invokes the actual installed
JDK21 `javac`, `java`, `jmod`, compiled deterministic Java oracle
and optionally C11/JNI with `-Xcheck:jni`; it compares repeated
SHA-256 JMOD images and all three embedded legal entries. `--jni`
is opt-in, requires a platform C toolchain and tests real JNI symbols
after package lowering. Legal metadata and Apache-2.0 license are
included, not upstream GNU/BSD command binaries.

**Recipe-first admission:** reuse
`M3HashPinnedJavaSnapshotRecipe`, executable Maven OpenRewrite
descriptor `com.synexia.m3jdk.CpuInt8Quantizer20261017` and
content-addressed before/after templates for every Java source
change. The native C and ancillary metadata are byte-pinned by
the same crate's custody verifier. The canonical generative
recipe and donors are retained in Synexia; this is an M3JDK21
receiving artefact. Source receipts alone do not prove runtime
semantics. JMOD/JNI, M3JDK21 merged source image, native ABI,
jtreg and performance promotion remain independent gates.

Catalogue candidates:
- Existing Synexia module: first-party Apache-2.0, selected.
- JDK 21 basic math and arrays: baseline comparator, selected.
- LeetCode/HackerRank/GeeksforGeeks vector/array problem categories:
  adversarial test shapes only, no solution code copied.
- GPU/Dawn kernels and Q8_0/GGUF: not equivalent, deferred/rejected.

JDK 21 interface and OpenJDK license remain unchanged. No M3String
conversion is imposed on primitive weight arrays; M3String is only a
logical text/identifier boundary, never a numeric cell representation.
