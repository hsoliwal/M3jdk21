# Jackson ObjectMapper in an M3 JMOD pack

The pack builds the existing Jackson 2.19.2 source modules and the small
`jdk.m3.jackson` integration module, then runs the same executable contracts on
the selected JDK and a linked image. Upstream `com.fasterxml.jackson.*` packages,
ObjectMapper behavior, service declarations and module names remain available.

## Source and module ownership

The runtime inputs are the existing in-repository source inclusions of Jackson
annotations/core/databind 2.19.2 and FastDoubleParser 1.0.90. `SOURCE_LOCK.json`
pins their complete source/build/legal trees. Their exact Maven POMs perform the
source builds, FastDoubleParser shading and multi-release compilation in fresh
staging directories. No upstream source or POM is patched by this build.

The existing `m3/tooling/jmod-recipes/tools/jmod_pack.py` owns JMOD assembly and
release-21 flattening. Its existing module graph checks the selected closure and
split packages. The linked image explicitly includes `java.se`, retaining the
JDK modules that Jackson's descriptor marks as optional. Four JMODs are produced:

- `com.fasterxml.jackson.annotation.jmod`
- `com.fasterxml.jackson.core.jmod`
- `com.fasterxml.jackson.databind.jmod`
- `jdk.m3.jackson.jmod`

FastDoubleParser is built from source and shaded by Jackson core's original
build configuration; it is not installed as a competing runtime module.

The build passes the existing Maven `project.build.outputTimestamp` property as
`2026-01-01T00:00:00Z` to the pinned archive plugins. This fixes ZIP entry dates
without editing any donor POM or source, so separate receiving builds can compare
the complete Jackson JAR bytes and reuse exact-artifact upstream test evidence.

## Build and execute

Use a complete JDK 21, Maven 3.9 or later, Python 3.10 or later, and the repository
checkout with all four source directories materialized. A partial clone must
materialize the needed donor trees before an offline build.

First populate the Maven build-plugin cache if necessary:

```sh
python3 m3/tooling/module-packs/jackson/build.py \
  --jdk-home /path/to/jdk21 --maven /path/to/maven/bin/mvn \
  --maven-repo /path/to/maven-cache --resolve-build-tools \
  --out /new/jackson-proof
```

The default is offline. Repeat with a new output directory and the same Maven
cache, omitting `--resolve-build-tools`, to prove that dependency resolution no
longer needs the network. Runtime libraries are source-built in either mode;
network access in the first mode is for Maven build plugins and their tooling
dependencies. `--maven-settings` and `--maven-global-settings` can select explicit
task settings without changing global Maven configuration.

The standalone Maven entry point is:

```sh
mvn -B -ntp -f m3/tooling/module-packs/jackson/pom.xml verify \
  -Dm3.jackson.out=/new/jackson-proof \
  -Dm3.jackson.maven.repo=/path/to/maven-cache
```

The output directory must be absent. Existing proof directories are retained
and refused; no host JDK installation or user file is overwritten. The pack is
an optional separately licensed module and does not advance the JDK-wide
String → arrays → collections → AST/compiler receiving sequence.

## Executed contracts and remaining runtime gates

`JacksonContract`, `M3FileContract` and `JacksonStreamingContract` run first from named source-built modules,
then from the linked image. Test classes are patched into `jdk.m3.jackson` only
for execution. Their private fixture POJOs use an explicit test-only qualified
opening to `com.fasterxml.jackson.databind`. An application's own named model
module must similarly export public members or open its model package for
Jackson reflection; this pack does not bypass JPMS access rules.

`RESULT.json`, `SOURCE_RECEIPT.json`, `SOURCE_BUILD.json`, the existing assembler
receipts and command logs identify the exact inputs and executed lanes. A stock
JDK run records the M3String storage flag as unavailable. A selected M3 runtime
that accepts `-XX:+UseM3StringStorage` additionally runs host/image contracts with
the flag enabled and disabled. `--require-m3-runtime` refuses completion without
that separate lane. The complete upstream fork suites have their own reproducible
qualification entry point below. Full JDK/jtreg coverage, all platforms, and
performance measurements remain separate gates.

## Optional M3AST compiler module

`jdk.m3.jackson.compiler` supplies the integration with the existing public
`com.sun.source.util.M3AST` compiler API. Its dependency on `jdk.compiler` stays
in that separate optional module. The four-module runtime pack above can be used
without the compiler bridge.

After the core build succeeds, the compiler qualification entry point is:

```sh
python3 m3/tooling/module-packs/jackson/compiler-build.py \
  --jdk-home /path/to/jdk21 --repo-root /path/to/M3jdk21 \
  --jars /new/jackson-proof/jars --out /new/jackson-compiler-proof
```

The harness compiles the actual four-source M3AST/compiler owner closure from
the selected repository and records every source hash. It compiles the bridge
against that API, packages a fifth JMOD with the existing assembler, and exercises
real compiler callbacks on the host and the five-module linked image. The
compiler patch is supplied explicitly through `--patch-module jdk.compiler`
during qualification and remains outside the bridge JMOD. Its original source
headers and GPLv2-with-Classpath-Exception ownership remain intact.

This bounded compiler harness is distinct from a full M3JDK VM build. Use a
qualified M3JDK compiler for production; the harness records its limited scope
in `COMPILER_RECEIPT.json` and does not establish the M3String VM storage lane.
The fifth-module pack reuses the complete legal bundle from the core build.

## Indexed input, output and prepared sequences

`M3ObjectMapper` inherits the full upstream ObjectMapper API and adds M3File and
NIO Path boundaries. `M3File.read(Reader, limit)` admits decoded text into indexed
String chunks without taking ownership of the supplied reader. The Path factory
requires an explicit charset. Mapper Path overloads retain Jackson's byte encoding
and BOM detection. `writeValueAsM3File` streams JSON into bounded chunks; it does
not first construct a complete document String.

```java
M3ObjectMapper mapper = new M3ObjectMapper();
var binding = mapper.prepare(Person.class);
M3File document = binding.writeValueAsM3File(new Person("Hitesh", 31), 1_000_000);
Person person = binding.readValue(document);

M3File array = binding.writeValuesAsM3File(people, true, 1_000_000);
try (var values = binding.readValues(array)) {
    while (values.hasNextValue()) consume(values.nextValue());
}
```

The prepared reader and writer preserve the full resolved generic type and the
configured Jackson serializers, annotations, modules, filters and root separator.
Sequences use Jackson's MappingIterator and SequenceWriter. Close a reader iterator
when stopping early or handling malformed data. Details and complete imports are
in `INTEGRATION.md`.

The M3AST bridge also accepts `TypeReference<T>` and `JavaType`. Its default
projection retains the existing top-level declaration contract; the explicit
`allDeclarations` consumer adds named member declarations. All retained metadata
is detached from the compiler session.

## Full upstream fork qualification

`upstream-qualification.py` executes the original tests from the pinned user forks,
then runs the same selected cases against the exact three generated Jackson JARs.
It verifies nonempty test reports, exact testcase selection, the known upstream
skips, and actual loaded class origins. See `UPSTREAM_QUALIFICATION.md` for the
command, fork revisions, JVM prerequisites, environment adjustments and scope.

## Jackson fork workspace

The generated development receiver in `hsoliwal/jackson-databind` lives at
`m3/m3jdk-jackson`. Its sources and tests are the same optional module pack.
The original upstream source, tests and POM remain the pinned test oracle. From
that checkout, provide the M3JDK source repository explicitly:

```sh
python3 m3/m3jdk-jackson/build.py \
  --repo-root /path/to/M3jdk21 --jdk-home /path/to/jdk21 \
  --maven /path/to/maven/bin/mvn --maven-repo /path/to/maven-cache \
  --out /new/fork-jackson-proof
python3 m3/m3jdk-jackson/compiler-build.py \
  --repo-root /path/to/M3jdk21 --jdk-home /path/to/jdk21 \
  --jars /new/fork-jackson-proof/jars --out /new/fork-compiler-proof
```

For the standalone Maven entry point in the fork, pass
`-Dm3.jackson.repo.root=/path/to/M3jdk21` and an absent `m3.jackson.out` directory.
The source lock still verifies the M3JDK donor trees and existing assembler owners.
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Jackson ObjectMapper JMOD receiver

Copyright 2026 Hitesh Soliwal and contributors.

This optional pack builds the complete source-vendored Jackson 2.19.2 annotations,
core and databind modules, and the Apache-2.0 `jdk.m3.jackson` companion. Original
`com.fasterxml.jackson.databind.ObjectMapper` names and implementation remain intact.
Jackson core's existing Maven build retains its shaded FastDoubleParser dependency,
module descriptor, service resources and Java 21 multi-release view.

## Use the original ObjectMapper

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.m3.jackson.M3AST;
import jdk.m3.jackson.M3JacksonModule;

ObjectMapper mapper = new ObjectMapper().registerModule(new M3JacksonModule());
M3AST ast = mapper.readValue("{\"items\":[1,2,3]}", M3AST.class);
String json = mapper.writeValueAsString(ast);
```

The module also advertises Jackson's `Module` service for explicit
`findAndRegisterModules()` discovery. An application module requires
`jdk.m3.jackson`; packages whose private POJO members Jackson must access need
their normal `opens ... to com.fasterxml.jackson.databind` declaration.

## File and String input

```java
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import jdk.m3.jackson.M3File;
import jdk.m3.jackson.M3ObjectMapper;

M3File file = M3File.read(Path.of("input.json"), StandardCharsets.UTF_8, 8_388_608);
M3ObjectMapper mapper = new M3ObjectMapper();
M3AST ast = mapper.readAST(file);
var value = mapper.readTreeValue(ast, java.util.Map.class);
```

`M3File` is an immutable, bounded source receiver. It snapshots mutable character
inputs, decodes file bytes with the supplied strict Charset decoder, and retains
one String owner. Original-byte SHA-256 and domain-separated exact UTF-16 source
SHA-256 are distinct. It does not normalize BOMs, line endings or surrogate code
units. Character-only inputs have no asserted byte hash. This receives the existing
compiler M3File snapshot/hash atoms and the MIndexFile exact text boundary; it is
not a migration of the token-file lexicon, postings, search or transfer codecs.

Text remains `java.lang.String`. On M3JDK that public owner uses the existing
internal M3String implementation. This companion introduces no competing String
payload or Synexia runtime dependency. Tests on stock Java prove the adapter's
behavior; they do not prove execution of modified M3String internals.

## Immutable JSON AST

The received generic MIndexAST mechanism stores String coordinates and ordered
child indexes in primitive arrays, with offset/count lanes and exact hash-consing.
Its pool belongs to one document and seals on construction. Hashes nominate an
existing atom; exact kind, name, value and ordered children decide equality.
Repeated subtrees can share atoms, while duplicate object field occurrences and
their order remain represented by repeated child references.

Jackson's actual parser handles grammar, escapes, encoding and numeric syntax.
The companion projects its tokens iteratively and writes through the supplied
Jackson generator. M3AST is an immutable JSON projection, separate from both the
mutable JsonNode API and the javac M3AST frontend. Its source positions, hash format,
pool indexes and scope are not claimed equivalent to those other AST families.

`M3JsonLimits` bounds input tokens, unique atoms, child references, retained and
expanded UTF-16 text, and container depth. Output traversal also checks expanded
work so shared DAGs cannot bypass those limits. Non-finite numbers and embedded
Java-object tokens are outside this finite JSON AST contract. Numeric text is the
lexeme exposed by Jackson, which may normalize permissive source syntax. The existing
strict numeric-lexeme check validates Jackson's token text without creating a
second parser; text such as `.5` or `1.` is refused. Ordinary inherited
ObjectMapper methods retain their existing support and configuration.

`readAST` requires exactly one complete root. M3AST property deserialization consumes
one value within its enclosing document. Existing ObjectMapper trailing-token,
duplicate-key, coercion, annotation and polymorphic behavior remains unchanged.
The existing `readValue(M3AST, ...)` overloads use an explicit JSON String projection
through Jackson and preserve registered M3AST serializers. Use the explicit
`readTreeValue` operation below when the authoritative tree itself is the value to bind.
Jackson TokenBuffer output uses its integer-aware writer to preserve numeric token
kinds in `valueToTree` and `convertValue`. Polymorphic type metadata on M3AST itself
is not part of this new datatype contract; Jackson reports its ordinary unsupported
type-id diagnostic. Existing POJO polymorphic serializers remain Jackson-owned.

## Bind the authoritative tree without reading or parsing its source again

```java
M3ObjectMapper mapper = new M3ObjectMapper();
M3AST ast = mapper.readAST("{\"items\":[1,2,3]}"); // authoritative parse
var first = mapper.readTreeValue(ast, java.util.Map.class);
var again = mapper.readTreeValue(ast, new com.fasterxml.jackson.core.type.TypeReference<
        java.util.Map<String, java.util.List<Integer>>>() { });
```

`readTreeValue(M3AST, Class/TypeReference/JavaType)` binds directly from the existing
sealed atom lanes. Its private Jackson token cursor emits every ordered occurrence,
including duplicate fields and shared subtrees. It opens no source, invokes no JSON
generator or grammar parser, and builds no TokenBuffer, JsonNode intermediary,
second atom pool or document-wide scalar copy. An existing `M3ASTAtom.asAST()`
subtree view can be bound independently using the same retained document.

The cursor retains two primitive container/child-ordinal stacks and Jackson's reused
contexts proportional to open depth. Text getters return the existing String.
Character-array access and numeric coercion may use Jackson's reusable current-token
buffer. Requested POJOs/collections are ordinary binding output; optional strict
duplicate detection retains Jackson's normal per-object name set. This is a bounded
storage design and an observed absence of source parsing, not a zero-allocation or
measured speedup claim.

The operation uses this mapper's real deserializers, type construction, naming,
coercion, object-identity resolution, numeric conversion and duplicate-detection
features. M3 retained-state, expanded token/text and depth bounds still apply, as do
Jackson's token-kind length, nesting and token-count read constraints. There is no
source stream for a byte/character document-length limit or source decorator to
process. Error locations are explicitly unavailable because this JSON AST does not
retain source positions. Errors and closure remain owned by Jackson binding.

This explicit tree operation bypasses M3AST serialization and generator settings.
Existing `readValue(M3AST, ...)` methods retain their serializer-based JSON
materialization behavior; custom serializers can intentionally produce a different
value. Neither a text-ingress overload nor a format-specific byte/character parser
is silently redirected to the new operation.

The original `M3JacksonContract` now invokes `M3ASTBindingContract` in both host
and linked-image qualification. Its real Jackson cases cover all three overloads,
source-parser/generator refusal counters, unchanged legacy serializer behavior,
numeric coercion/overflow, Unicode, duplicate/shared occurrences, independent
subtrees, configured deserializers and object identities, token contexts, failure
closure, resource limits and 3,000 nested containers. Focused host receipts must be
reported separately from the full source/JMOD and modified M3JDK runtime gates.

## Build and proof boundary

Use the staged `build.py` driver with an intact Java 21 JDK, Maven and an explicit
Maven repository. Execute `lint`, `compile`, `test`, then `runtime` against the same
fresh output directory. It refuses source drift or stage-order violations. The
runtime stage calls the existing M3 JMOD packager only after behavior tests pass.
JMODs are link inputs; use the resulting linked image or modular JARs at runtime.

Pass `--maven-settings /absolute/settings.xml` when the environment requires a
specific proxy or repository mirror. The driver records that file's SHA-256 and
uses it as both user and global Maven settings. Without this option it supplies
an empty settings file. It never discovers settings from a user's home directory.
When an installed network trust store is required, pass
`--java-trust-store /absolute/cacerts`. Its hash is recorded and it is used only
by the Maven JVM. Certificate verification stays enabled; the supplied JDK and
the linked image's trust store are not modified.

Exact commands, source hashes, stdout, stderr and exit codes remain in the output.
The original source snapshots and historical evidence are not edited. Recipe
custody and donor/receiver hashes are recorded separately. This independently built
module does not advance the repository-wide String/array/collection/AST receiving
phase. Full M3JDK image/jtreg, Windows/macOS and performance qualification remain
separate gates unless explicitly backed by executed receipts.
