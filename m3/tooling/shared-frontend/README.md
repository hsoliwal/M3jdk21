# Shared M3 compiler frontend

This opt-in frontend lets multiple consumers use the compilation that a build
already runs. `com.sun.source.util.M3AST` exposes the actual javac unit and its
phase; `com.sun.tools.javac.m3.M3ASTPC` owns the session inside `jdk.compiler`.
There is no Synexia runtime dependency in the JDK modules.

## Reverse implementation order

The receiving path starts with selected build-tool packs and retained runtime
assets, then Maven/Gradle/Ant compiler arguments, the existing compiler task and
OpenRewrite conversion, canonical existing-unit cache ingestion, and explicit
name/provenance reconciliation. See the adjacent algorithm catalogue and
`m3/docs/name-mapping.d/m3-shared-frontend.json`.

## Compiler use

```sh
javac '-Xplugin:M3AST lossless trace=/fresh/invocation.tsv' Example.java
javac '-Xplugin:M3AST image=/owned/invocation.m3prim slots=/owned/invocation.tsv fold-expressions' Example.java
```

`trace` uses CREATE_NEW and must be unique per compiler invocation. The trace
is an event/counter receipt, not a portable AST. `image` reuses the existing
primitive-image encoder and format. `slots` and `fold-expressions` require
`image`; output aliases are refused. An image is published only at a successful
COMPILATION event with no prior consumer failure. Separate parse/analyze calls
do not authorize publication. Use paths without spaces with javac's plugin
argument syntax; javac tokenizes that syntax before the plugin receives it.

Without arguments, the plugin loads `M3AST.Consumer` providers through javac's
existing plugin/processor loader. It does not write output or request an AST
projection. The enabled session still freezes and hashes the exact characters
passed to the parser. Module providers must declare `provides` for the consumer
service; classpath providers use
`META-INF/services/com.sun.source.util.M3AST$Consumer`.

For an in-process consumer:

```java
try (M3AST frontend = M3AST.open(task, true)) {
    var projection = new M3AST.Projection<>("my-snapshot", unit -> capture(unit));
    frontend.addConsumer(new M3AST.Consumer() {
        public void parsed(M3AST.Unit unit) throws Exception {
            immutableSnapshots.accept(unit.project(projection));
        }
    });
    task.call();
}
```

Reuse the exact projection-key instance among consumers to share one conversion
within an event. Equal names do not share data. Null values and failures are
memoized; recursive requests are refused. Units are confined to the callback
thread and expire when dispatch ends. Retain detached snapshots, not javac
trees, types, elements, the Unit, or the task. The handle releases compiler
references on completion, explicit close, and consumer failure.

PARSE callbacks receive the same frozen character source used by javac. After
that callback only URI, length, and digest seals remain. A later source request
rereads once per event and refuses changed characters. The hash covers UTF-16
code units, including unpaired surrogates, in big-endian order. It is not a raw
file-byte checksum and cannot authorize semantic reuse across compiler contexts.

## OpenRewrite

`openrewrite/` contains a version-bound Java 21 / OpenRewrite 8.17.1 adapter.
It invokes the upstream `ReloadableJava21ParserVisitor` in the active compiler
loader. It does not construct a parser or run attribution. Its caller-owned
projection also owns a compilation-local Rewrite type cache and refuses reuse
by another compiler context.

Enable `lossless` before parsing. The adapter requires a fully attributed unit
that has not begun lowering. Ordinary javac processes top-level classes one at
a time, so a source with several top-level types requires the explicit
`-XDcompilePolicy=byfile` option or separate parse/analyze before generate. The
adapter refuses unsuitable input; it does not silently create another parser.
Package-info/module-info units without an analyzed type are outside this adapter.
The internal exports listed in its POM are only for this optional version-bound
converter. Ordinary M3 consumers use the already exported `com.sun.source.util`.

The differential test checks original printing, CRLF, Unicode comments,
`"a" + "b"` binary structure, generics, a record, method binding, and a real typed
ChangeMethodName recipe. It compares that recipe with the stock Rewrite parser
as a test oracle, then compiles the changed source as a new generation. No
cached attribution from the previous generation is reused.

## Cache ownership and process boundaries

Canonical Synexia capture and the optional M3 bridge feed the existing
MIndexASTPartialCache/TieredLoader. Those owners supply lazy projections,
per-document canonical pools, weighted LRU, `.miast` and SQLite persistence,
eviction, and reload. The bridge captures during PARSE only on a real cache
miss and does not retain the callback Unit. A missing persistent document after
the compiler callback is refused rather than reparsed. The caller supplies a
parse-configuration SHA namespace in addition to source identity.

This qualification does not merge the three distinct Synexia AST families into
one class. The existing `com.synexia.m3index.M3AST` atom facade, the indexstring
AST document/cache family, and the compiler API have distinct owners. The name
fragment records that distinction. The optional private bridge is the receiver
for the canonical cache integration; the JDK core has no dependency on it.

Only detached syntax documents or established primitive images cross process
boundaries here. Javac objects and attributed facts remain in their compiler
context. Full semantic snapshots require dependency, option, processor,
generated-source, toolchain, and source-generation identity; this change does
not claim a general portable semantic-cache format.

LRU bounds the cache's owned resident working set. It does not bound javac's
heap or objects retained by a client. Eviction can release cache ownership while
a client still holds an immutable document. No total-input/heap ratio or speedup
is asserted by these operation-count tests.

## Reproduction and acceptance

Use the source-owned recipes and their SDK/receiver integrity checks before
applying generated changes. The public recipe custody contains the reproducible
target transformation; the canonical source PR owns its convergence history.

Run the actual compiler and Rewrite contracts against a supplied built JDK:

```sh
python3 m3/tooling/shared-frontend/verify_frontend.py \
  --jdk /path/to/m3jdk --out /fresh/frontend-proof \
  --maven /path/to/mvn --maven-repository /path/to/maven-cache
```

The receipt pins the supplied image's modules and test source. Full JDK source
build, fork source builds, target CI, other operating systems, and benchmarks
have separate receipts; a packaging success never promotes those gates.
The build-tool adapters and selector are documented under
`m3/tooling/module-packs/build-tools/`.
