# M3TreeList exact-source allocation-failure gate (proposal, 2026-11-14)

Owner: `hsoliwal/M3jdk21`, stacked after draft PR #3027, with canonical Java implementation source Git blob `ab861ce0c29d53080b54117414a4ba8486b1c58e` received from Synexia #17321. No changes to the original `M3TreeList.java` are permitted by this gate.

The existing Synexia `TreeListCursorAllocationFailure.java` is copied as an exact Git blob (`ab8a43dfb0191c81b039e8e49e419de964a126de`), preserving the author's license header. A separate verification script validates both source Git blobs, requires the source entry in the current HEAD tree, inserts exactly two test-only instrumentation atoms into a disposable copy of the canonical Java source, compiles it with strict Java 21, and runs all 60 injected allocation failures and 491,520 retry/traversal checks under default, interpreted and compiled JVM modes. Positive JVM receipts must be byte-identical. Missing source identity, malformed markers, a failed compile, a missing injection, and any runtime mismatch all fail closed.

The script writes only beneath its designated `target/tree-cursor/fault` evidence directory. It cannot edit, check in or replace the original canonical source. The focused CI workflow must run its existing source and contract tests before this additional execution stage; no gate is bypassed.

Qualification order: inventory and source seal -> Python syntax/lint -> strict javac -> reflection injection tests -> JVM modes -> receipt. Original target runtime, ABI, serialization, JMOD/JLink, jtreg and independent performance gates remain distinct. Until hosted execution completes, the change remains DRAFT/HOLD.

The previous isolated model failure reproduction is not a replacement for executing this exact-source test.
