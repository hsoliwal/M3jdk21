# M3 UTF-8 cursor candidate receiver

Canonical recipe and proof source: hsoliwal/com.synexia PR #11448, commit
4f9b2646d74ab39031a54d1f56d6777c52a21852. Source documentation-first commit:
8552a43be6162910d349e0712f4bc6d42a6a5f23. Reusable recipes, templates, fixtures and
Java/JNI proof code remain in synexia-openrewrite-recipes. This target receives
only its generated runtime candidate and target-specific provenance.

## Contract

Target base d5abed865bd47c4dd5ad78e95704348fe2095ed2. Only the private
java.lang.M3String.encodeUtf8 method changes: construct the existing CodeUnitCursor
after the unchanged ASCII return, then reuse it at three receiver-local reads.
Preserve argument evaluation including ++index, surrogate replacement, sizing,
all signatures/fields, canonical owner+coordinate representation and JNI declarations.
No new payload owner, dictionary, runtime dependency or production native symbol.
The complete 1419-line source was read and independently sealed before generation.

## Qualification is incomplete

The source-sealed Java task generated the source; it was not hand-patched afterward.
Finite proof passed on extracted encoder/cursor code in a HEAP COORDINATE MODEL:
1455212 encoding cases and 13375 cursor cases, with 17 recipe refusals and eight
fixed-point cycles. The model is not the actual M3 owners, native memory or java.base.
A separate Java/JNI range-hash probe passed 409083 split checks; it is NOT installed
in this target. Neither result proves activated M3 runtime behavior or a speedup.

Maven invocation failed before startup (missing executable, exit 127). Actual Maven,
OpenRewrite SDK, target JDK build/jtreg, interpreter/C1/C2, GC/native lifetime,
API qualification and allocation/performance measurement remain UNEXECUTED.
Keep the PR draft. No receiving phase, default branch or merge authority is advanced.
Existing mapping MIndexString -> M3String is reused; no donor-name ABI renaming.

Reproduce from the canonical source crate using its cursor-proof Maven profile and
verify-cursor.sh after approved offline dependencies are available. Five SDK tests
are authored but unexecuted; no parser stubs or packet hashes substitute for them.
The original GPLv2-with-Classpath-exception source header and target LICENSE remain.
