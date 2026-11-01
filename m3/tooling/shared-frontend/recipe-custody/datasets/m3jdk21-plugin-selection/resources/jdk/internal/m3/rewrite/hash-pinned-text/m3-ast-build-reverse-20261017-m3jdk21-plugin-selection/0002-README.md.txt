<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Read plugin selection from the original javac task

`M3AST.isPluginSelected(JavacTask)` reports whether the original task's parsed
plugin options select the builtin plugin named `M3AST`. Call it after the host
has created its real task and before choosing the consumer-discovery owner:

```java
boolean pluginOwnsConsumers = M3AST.isPluginSelected(originalTask);
M3AST session = M3AST.open(originalTask);
```

When the result is true, the builtin plugin owns service discovery through
javac's existing processor/plugin loader. Otherwise an admitted host provider
can discover its consumers and register them on that same session. The query
reports selection at the time of the call; it does not certify successful
plugin initialization, compilation, consumer admission or output publication.

## Original option authority

The compiler owner reads the existing `BasicJavacTask` context and its existing
`Arguments` entry through `context.get(Arguments.argsKey)`. It asks that owner's
`getPluginOpts()` for the selected names. It creates no compiler, task, context,
arguments owner, M3 session, listener, source reader, parser, attribution pass or
AST. Calling it does not enable syntax retention or alter the original options.
As with the task itself, call the query on the task's owning thread.

This distinction matters for options such as:

```text
--class-path -Xplugin:M3AST
```

Here `-Xplugin:M3AST` is a classpath operand. The plugin is not selected. An
additional real `-Xplugin:M3AST` switch selects it. Aliases, inline operands,
ASCII whitespace, NUL-separated plugin segments, and effective internal `-XD`
option overrides follow the original javac argument owner without another
token parser. `M3ASTIndex` and other longer names do not select `M3AST`.

## Refusals and compatibility

| Input state | Exception |
| --- | --- |
| Null task | `NullPointerException` (`task`) |
| Foreign `JavacTask` implementation | `IllegalArgumentException` (`M3AST requires the active javac task`) |
| Task whose compiler context has been released | `IllegalStateException` (`closed javac context`) |
| Context without an existing arguments owner | `IllegalStateException` (`javac arguments not initialized`) |

Closing an M3 session and releasing the javac task context are separate events.
The query reads task options and does not open or inspect an M3 session.

This is an additive static interface method. Existing instance methods and
implementors remain unchanged. Source sealing, source-change refusals,
identity-key projections and callback lifetimes keep their existing owners.
The query adds no cache and grants no reuse across tasks, classloaders, workers
or processes.

## Qualification

`test/langtools/tools/javac/m3/M3ASTPluginSelectionTest.java` exercises the actual
task and option owner, including supported operand cases and internal option
overrides. Before execution it verifies zero source reads, unchanged compiler
and listener owners, and no M3 session. It then calls the original task and
checks its normal success or plugin-not-found result. Separate refusal cases
cover null, foreign, released-context and missing-arguments inputs.

Run this test with the actual M3 module descriptor, alongside
`M3ASTSharedFrontendTest` and `M3ASTSourceSealTest`. Source compilation and a
hybrid compiler image do not establish a full source-built JDK or Gradle
distribution, a performance result, or a global porting-phase advance.
