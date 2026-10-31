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
