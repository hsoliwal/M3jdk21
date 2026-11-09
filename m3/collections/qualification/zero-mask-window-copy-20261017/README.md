# Zero-mask window copy refinement

The int and long JNI selection windows now copy row chunks directly when the
required state mask is zero. The state window is still validated, but its bytes
are not read. Nonzero mask filtering, detached output, order, bounds, null checks,
exception precedence and public Java/native signatures are preserved. No entry
nodes, callbacks, critical array pins or retained pointers are introduced.

This adapts the existing zero-mask count/selection identity in the same owner.
Canonical Synexia snapshot engines are reused verbatim. The named Java-plus-C
recipe proves all eight partial installation/order cases, fresh-byte replay and
six refusal cases. Original source fixtures remain immutable.

Executed locally: strict C17 compilation, a real Windows JNI library, the complete
Java-only SelfTest, the complete native SelfTest with -Xcheck:jni (17433 checks),
and counted JNI probes across 15 zero/one/chunk-boundary windows for int and long.
The identical probe against the original C source fails its state-read assertion,
confirming it discriminates this change. The probe supplements the real JVM test.
There is no measured speedup claim. Linux sanitizer CI remains pending.

A separate Codex source review with command execution disabled found no findings
within this production patch and its unchanged validation context. It does not
substitute for broader collection-contract qualification.
