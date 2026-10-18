<!-- SPDX-License-Identifier: Apache-2.0 -->
# Executed recipe compiler gate repair — 2026-10-18

Exact receiver head `5edf1f7ef80a5a0ad75a956a8e82e9a9aef40ff2`, dedicated Actions run
https://github.com/hsoliwal/M3jdk21/actions/runs/39086458033

Observed `javac -Werror` failure compiling the reusable existing OpenRewrite
`M3GpuToolJoinRecipe` / Manifest:
`Cannot find annotation method 'value()' in type 'org.intellij.lang.annotations.Language'`.
The package is an optional OpenRewrite annotation signature, not a change to
the generated Guava implementation. Add exact Maven coordinate
`org.jetbrains:annotations:24.1.0` to this **recipe crate's compile classpath**
instead of relaxing `-Werror`. No generated Java, public collection method or
JNI ABI changed.

This is a source-first recipe correction; the receiver must copy the exact
new Maven POM Git blob before rerunning its workflow.
