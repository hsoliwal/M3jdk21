<!-- SPDX-License-Identifier: Apache-2.0 -->
# mergeloss-s3-20261106

Cases 10 and 14 of the M3jdk21 merge-loss audit, both on the CPU mask viewport owners:

* case 10: the retain merge `487ed3c5b7aa` (#1766, 2026-10-28) kept master's bodies of `projectFramePage` in
  `M3MaskViewportKernel.java` and `m3/collections/src/main/c/m3maskviewport.c` over the bodies origin `3f4e51ad80a3`
  (2026-10-21) added; #2547 held the merge's `M3MaskViewportNative.java` (pr1766) because it declared
  `projectFramePage` twice;
* case 14: the compiler-accepts merge `2f9a1a2a9d0e` (#1497, 2026-10-28) kept master's `project()` over the
  `read_region` / `REGION_SIZE` bounded transfers of origin `d0cec9717801` (2026-10-19).

Decision: **SUPERSEDED, no source change** (`DECISION.tsv`). Later non-merge master commits implement the same members
with the same signatures (C `62a9d54fa777`, `b2f9fdb4d462`, `aa9fdc637a53`, `ef970796fede`; Java `facc2e13d0c6`;
bounded `project()` regions `7fb4de24b1e8`, `3ed252b92a5b`, `22009882312e`), and the dropped code's own contracts pass
against them: `M3ViewportFrameContract` (#1766's oracle) Java and native, `M3MaskViewportWindowContract`
(`d0cec9717801`'s oracle) native, default and -Xint, -Xcheck:jni clean. The only code-shaped remainder, #2547's held
`M3MaskViewportNative.java`, is a second declaration with master's exact descriptor, so master's single declaration is
the one method set that compiles; there is nothing further to restore.

| file | content |
|---|---|
| `DECISION.tsv` | per case: decision, cited commits, evidence, action |
| `stage.sh` | stages master's owners, the origin contracts and blobs, and the held file (any host with the repository) |
| `proof.sh` | Linux proof: javac -h prototypes, gcc -Werror, both origin contracts on master, regions test, held javac |
| `GATES.tsv` | gate results |

Open item (owner): `src/test/native/m3_mask_viewport_regions.c`, landed by the #1497 merge, instruments
`d0cec9717801`'s internals (`REGION_SIZE`, read/write counts) and does not compile against master; no workflow runs it.

Replay: `bash stage.sh <repo> <master> <dir> && TMPDIR=<tmp> JAVA_HOME=<jdk21> bash proof.sh <dir>`.
