<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 parser framework

`jdk.m3.parser` is an optional Java 21 module and JMOD. It joins an immutable
`M3File` source with the received MIndex grammar engine and the existing M3JDK
String and javac owners. It has no Synexia runtime or external parser dependency.

| API | Responsibility |
| --- | --- |
| `M3File` | Immutable URI, retained `String`, exact UTF-16 source seal and optional original-byte seal; bounded strict file reads. |
| `M3Parser` | Reusable frozen grammar and limits; a fresh lexer/parser session for each call. |
| `M3ParseResult` | Detached source plus grammar validation and optional packed parse tree. |
| `jdk.m3.parser.grammar` | Configurable lexer, grammar builder, Earley recognition/parsing, grammar analysis and packed tree lanes. |
| `M3JavacParser` | Attach a detached primitive projection to a caller-owned javac task through existing `M3AST` events. |
| `M3JavacTree` | Immutable preorder node kinds, parents and source offsets; no retained compiler objects. |

The source is an ordinary `java.lang.String`. A matching M3JDK supplies its
existing package-private `java.lang.M3String` implementation behind that API.
There is no additional public M3String class or reflective access to its owner.
Offsets are UTF-16 code units and ranges are half-open. String inputs retain
their identity; mutable `CharSequence` inputs become stable snapshots.

## A small grammar

```java
import java.net.URI;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3Parser;
import jdk.m3.parser.grammar.*;
import static jdk.m3.parser.grammar.M3GrammarRef.*;

var lexer = M3LexerProfile.builder("sum").operators("+").build();
var grammar = new M3GrammarBuilder(
        "sum", M3GrammarFamily.CUSTOM, M3GrammarStrictness.STRICT, lexer)
    .start("Sum")
    .production("Sum", nonTerminal("Sum"), literal("+"), nonTerminal("Value"))
    .production("Sum", nonTerminal("Value"))
    .production("Value", token(M3TokenClass.NUMBER))
    .build();

var source = M3File.of(URI.create("memory:/sum.expr"), "12 + 30");
var result = new M3Parser(grammar).parse(source);
if (!result.conforms()) {
    throw new IllegalArgumentException(result.syntax().validation().toString());
}
var tree = result.requireTree();
System.out.println(tree.symbolName(tree.root())); // Sum
```

This framework builds syntax; application evaluation is a separate consumer.
The received lexer preserves broad donor numeric/quoting behavior. `NUMBER`
alone does not establish strict JSON or Java lexical conformance.
`accepted()` applies the configured strictness policy, while `conforms()` states
whether the grammar matched. Tolerant acceptance does not guarantee a tree.

For files, provide the charset and byte limit explicitly:

```java
var file = M3File.read(path, java.nio.charset.StandardCharsets.UTF_8, 1_048_576);
```

Malformed or unmappable input is refused. The supplied charset controls BOM
handling. The UTF-16 seal hashes every code unit in big-endian order, including
unpaired surrogates. `byteSha256()` is present only for file-byte admission and
is distinct from the text seal. A later file edit requires a new snapshot;
existing snapshots remain valid. The read does not promise an atomic disk revision.

## Shared javac projection

Use a `JavacTask` created by the application's normal compiler setup. Create one
projection key and share it among consumers that need the same node bound:

```java
var projection = M3JavacParser.projection(100_000);
try (var events = M3JavacParser.attach(task, projection, detachedTrees::add)) {
    task.parse();
    // task.analyze() may follow when the application needs attribution.
}
// detachedTrees remain usable after the event handle and compiler task close.
```

`M3AST` memoizes by projection identity within each live event. This projection
uses the exact event source seal, walks javac's children in preorder, and copies
only primitive node lanes and the source snapshot. Node ranges marked unavailable
by javac remain `-1`; call `hasSourceRange()` before `sourceText()`. Kind ordinals
are local compiler metadata, not a portable attributed-AST serialization format.
The generic grammar tree and javac tree have separate contracts.

## Limits and ownership

`M3GrammarLimits.defaults()` admits 1,048,576 source characters, 10,000 tokens,
250,000 aggregate states, 100,000 nodes, depth 256, and 10,000,000 deterministic
work units. Depth counts the root as one and includes terminals. Resource
refusal throws `M3GrammarException`; all optimized/fallback/nullable-repair passes
consume one session budget. Use `new M3Parser(grammar, limits)` for explicit
limits. Grammar construction, analysis and the first immutable decision-index
build are configuration operations outside the per-input budget.

Each frozen grammar owns its decision index. Fingerprints are informational;
equal hashes do not share grammar identity. A late-nullable replay pass repairs
missed waiters only after the original two passes fail, preserving the choice
made by existing successful parses. Full provenance and exclusions are recorded
in `SOURCE_BORROW.tsv` and `GRAMMAR_ADAPTATIONS.md`.

## Build and qualify

Use Python 3.10 or newer, a complete Java 21 development JDK, and a checkout with
full Git ancestry for the unchanged receiver-policy gate. From the repository
root, choose a new output directory and run the phases in order:

```sh
python3 m3/tooling/module-packs/parser/build.py --jdk-home /path/to/jdk-21 --out /path/to/new-proof --phase lint
python3 m3/tooling/module-packs/parser/build.py --jdk-home /path/to/jdk-21 --out /path/to/new-proof --phase compile
python3 m3/tooling/module-packs/parser/build.py --jdk-home /path/to/jdk-21 --out /path/to/new-proof --phase test
python3 m3/tooling/module-packs/parser/build.py --jdk-home /path/to/jdk-21 --out /path/to/new-proof --phase runtime
```

The source and toolchain inventory, command logs and chained receipts are written
under the proof directory. A changed input, failed phase, modified artifact or
out-of-order invocation is refused; choose a new directory for a new pass.
All build-time `javac` work, including the consumers, finishes before tests.

Maven orchestrates the same phases through the optional `m3-parser` profile:

```sh
mvn -f m3/tooling/module-packs/pom.xml -P m3-parser -pl :m3-parser-jmod-pack verify -Dm3.parser.jdk=/path/to/jdk-21 -Dm3.parser.proof=/path/to/new-proof
```

The default module-pack profile graph is unchanged. The dedicated
`.github/workflows/m3-parser-jmod-20261018.yml` runs the same qualification.

The output contains:

| Path under the proof directory | Content |
| --- | --- |
| `jdk.m3.parser.jar` | Named modular JAR. |
| `pack/jmods/jdk.m3.parser.jmod` | Module classes and Apache license/notice files. |
| `pack/image/` | Linked development image used by the runtime consumers. |
| `compiler-patch/` | Source-compiled patch of four unchanged M3 javac owners. |
| `RUNTIME_PROOF.json` | Exact artifact hashes, replay evidence and qualification scope. |

### Compiler qualification scope

The module requires the existing public `com.sun.source.util.M3AST` API in
`jdk.compiler`. The build compiles the four unchanged M3JDK compiler source
owners as an explicit external patch against Java 21. Every host/image consumer
uses `--patch-module jdk.compiler=<proof>/compiler-patch`.
That patch is not embedded in the linked image. This workflow does not claim
that stock Java supplies M3AST, that plugin discovery is qualified, or that a full
M3JDK/HotSpot build or canonical M3String storage has been exercised.

On a matching built M3JDK, the API is supplied by its own compiler module.
Application launch must include the named parser module, for example
`--module-path /path/to/jdk.m3.parser.jar --add-modules jdk.m3.parser`.
JMOD is the linking/distribution input; use the modular JAR on an ordinary
application module path.

## Source custody

The 24-file first-party grammar closure is pinned to
`hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267` and adapted through
`com.synexia.rewrite.M3ParserJmod20261018`. The canonical recipe resides in
`synexia-openrewrite-recipes` in that repository and reuses its existing exact
Java/text snapshot recipes. M3File's source/hash contract references the pinned
`MIndexASTParser`; bounded file reads fill a new integration gap.

Language packs, importers, AST-spec converters, Synexia indexing-runtime hooks
and native accelerators are outside this initial runtime closure. This optional
Apache module does not advance the JDK's ordered runtime receiving phases.
See `DESIGN.md`, `CHANGE_CONTEXT.json`, the source mapping and
`evidence/QUALIFICATION.md` for the exact admitted and executed scope.
