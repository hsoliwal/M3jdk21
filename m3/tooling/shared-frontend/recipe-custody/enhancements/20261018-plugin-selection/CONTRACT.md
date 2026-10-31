<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Read the original task's plugin selection

This contract precedes the source candidate. Independent review of the new Gradle
provider found that scanning all raw options confuses an operand with a switch.
For example, `--class-path` followed by the string `-Xplugin:M3AST` selects a
classpath and no plugin. Javac consumes operands through its existing Option
owner before recording plugin selections in Arguments.getPluginOpts().

## Existing owner and compatible extension

Add the static method `M3AST.isPluginSelected(JavacTask)` and its implementation
on the existing M3ASTPC owner. Preserve all existing public/protected declarations
and all instance interface methods; no implementor gains a new abstract method.
The method reports whether the builtin plugin name M3AST occurs in the original
task's already-parsed plugin options. It does not report successful plugin
initialization or compilation.

Read the original BasicJavacTask context and the existing Arguments entry via
Context.get(Arguments.argsKey). Do not create a new compiler, task, context,
Arguments owner, source reader, parser, attribution pass, or AST. Refuse a null
task, a foreign implementation, a closed context, or missing parsed arguments
with documented exceptions. Use the existing getPluginOpts() grammar so option
operands, aliases, inline operands, ASCII whitespace, NUL-separated plugin
selections and longer plugin names follow javac itself.

This is an additive extension beyond the separately qualified source-seal leaf.
The source-seal changes, its 117 checks, the existing 70-check suite and its
baseline failure witness remain intact. This change adds no cache and does not
promote any global porting phase, native ABI, build or performance gate.

## Provider behavior

At attach(originalTask), the Gradle provider asks this query before deciding
consumer-discovery ownership. It joins the existing M3AST session. Builtin plugin
selection leaves discovery with javac's plugin/processor loader; otherwise the
provider uses its explicit asset loader. Remove raw-option token recognition
from the provider. The original options, file manager, sources, processors,
diagnostics, setters and call() remain unchanged.

## Qualification and receiving chain

Canonical dataset `m3jdk21-plugin-selection` uses the unchanged current snapshot
recipe owners. Its receiving state is the pinned M3jdk21 master
2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49 after qualified dataset m3jdk21-seals.
Each manifest records the exact observed preimage; replay the two datasets in
that order. No historical whole-file variant is admitted automatically.

Add a separate real-task M3ASTPluginSelectionTest. Cover builtin selection,
space/tab and the remaining javac ASCII whitespace, NUL segment handling,
M3ASTIndex/other longer names, a classpath operand that looks like a plugin,
inline classpath operands, and an operand followed by a real plugin option.
Verify the query performs no source reads, creates no M3 listener/session,
and leaves the original task usable. Refusal tests must exercise the actual
null/foreign/closed/missing-arguments boundaries without a substitute parser.
Provider tests must exercise real consumer discovery for the operand case.

Follow patch -> lint -> compile -> tests -> runtime. Qualify and receive actual
SDK results before compiling the changed owner. Build a fresh descriptor-aware
image and run the existing suites plus the new query suite. Bind final Gradle
and Maven runtime receipts to that final image. Preserve prior logs and classify
their earlier compiler revision separately; no completed native-source test is
invented by a later runtime check.
