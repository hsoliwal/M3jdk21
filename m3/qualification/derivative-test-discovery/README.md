Canonical recipe and executed qualification: [Synexia PR #17362](https://github.com/hsoliwal/com.synexia/pull/17362), commit c27dc5bf47cdc25471dde38008dbe6d77808ac51.

# Derivative test discovery

The existing derivative workflow requests M3RegexDerivativeTest and M3RegexDerivativeEffectsBridgeTest, while the receiving classes are RegexDerivativeTest and RegexDerivativeEffectsBridgeTest. Hosted run 43386455818 compiled 67 production and 47 test files, then Surefire executed no matching tests; the native step was skipped.

Reuse the existing hash-pinned YAML recipe. Its generated output changes only the two test selectors. Java/native commands, Maven no-test refusal, timeouts and other workflow content remain unchanged. No runtime API or Java/JNI implementation changes are proposed.

Executed qualification: nine JUnit checks exercise typed YAML replay, exact output, fixed point, missing/duplicate/drift/wrong-AST/case-alias/noncanonical-path/after-scan refusals and selector binding to pinned class declarations. The actual two test classes were then compiled with all 67 production sources using Java 21 warnings-as-errors in the existing Maven/RAM toolchain. Six tests were found, five passed and the optional JNI test aborted because no compatible library was loaded. This is not native parity proof.

The receiving master has the earlier IndexPrecomputeManifest owner because the source-ID repair is still a separate open PR. The master rebind receipt recompiles and reruns the exact current master source set and reuses the unchanged, verified YAML proof. Both original and receiving source pins are retained. The original recipe fixture failure is recorded: OpenRewrite wraps the correct post-scan IllegalStateException; the qualified fixture checks the exact root cause/message. The source engine and workflow candidate were unchanged.

QUALIFICATION.zlib.json and MASTER_QUALIFICATION.zlib.json contain compressed complete executed receipts, each with uncompressed SHA-256 and length. Decode their zlib-hex payload in memory and verify both values before using the report. inputs/master-input.json represents the exact executed sealed input using the existing immutable Git blobs, recipe resources, fixtures and Maven/bootstrap bindings. Restore the documented content references, preserve object property order, serialize compact JSON plus LF, and verify its original Git blob SHA before execution. This avoids duplicating the 67 production sources in the recipe crate. Its code requires INPUT and EMIT; the existing RAM bootstrap and framed transport provide those, with no new source/class/log files. Never rerun by treating a receipt or stored authorization as a fresh capacity approval.

Mutable routing is the user-approved S:\\codex-work\\agent-temp\\lexicon-maven-r224 lane. Keep the 545 MiB combined task file-allocation cap and at least 8 GiB free after planned output; F:/C: dependencies remain read-only. The executed receipts record unchanged F file allocation and zero S file allocation. Do not copy caches or lower the reserve.

This repair restores test discovery. It does not establish JNI execution, full Java regex compatibility, lexicon integration, new performance gains, complete corpus admission or JDK promotion. In particular the optional native test uses assumeTrue(available); a native profile result needs an actual native test execution report.
