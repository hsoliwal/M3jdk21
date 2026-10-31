# Exact Spring owner receiving recipe

Activate `com.synexia.m3.OwnedSpringCaseInsensitiveMap20261018` from
`META-INF/rewrite/m3-owned-spring-case-insensitive-map.yml`.

The unchanged canonical `M3HashPinnedJavaSnapshotRecipe` (13805 bytes,
SHA-256 `3fc6e77a1394e292747d689a4a42a328b496000865aa390afe05d5571febd599`)
installs exactly one absent production source or accepts its exact current
postimage. The source is 31224 bytes with SHA-256
`450e3f70be37f0fa0fcee7edd4adc8a2eea4a05a40646fb3e5c531e4ee8447ca`.

This preset configures `lst: false`. The receiving host must supply
`PlainTextParser`-produced `SourceFile`s, including when the target already has
the exact installed postimage. A Java parser produces `J.CompilationUnit`
inputs, which fail the representation gate in this preset even when their
printed text matches. Activating the YAML in a Java-LST host alone does not
establish the qualified fixed point.

The original JUnit `freshSources` helper creates `Parser.Input.fromString`
with each relative path and parses those inputs with `PlainTextParser`. For
an existing exact target, use the same pipeline and assign its source path
explicitly before running the activated `recipe`:

```java
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainTextParser;

var target = Path.of("m3/collections/implementations/spring-case-insensitive-map/src/main/java/com/m3/collections/spring/M3LinkedCaseInsensitiveMap.java");
String text = Files.readString(target, StandardCharsets.UTF_8);
var errors = new ArrayList<Throwable>();
var ctx = new InMemoryExecutionContext(errors::add);
var source = PlainTextParser.builder().build().parseInputs(
        List.of(Parser.Input.fromString(target, text)), null, ctx)
        .findFirst().orElseThrow().withSourcePath(target);
var changes = recipe.run(new InMemoryLargeSourceSet(List.of(source)), ctx, 1)
        .getChangeset().getAllResults();
```

For the absent-target installation, omit that target from the supplied source
set; parse any unrelated sources through the same path-preserving PlainText
pipeline. After installation, re-read the exact UTF-8 postimage, parse it again
as PlainText with the same target path, and activate a fresh recipe instance.
That exact-source reactivation is the fixed point exercised by the original
tests. Check the execution context for errors as the test helper does. Java
compilation and the provider/owned differential runtime proofs are separate
qualification steps; the receiving pipeline establishes source custody.

The two borrowed JUnit tests qualify eight supplied-source states (including
coincident empty/singleton orders), fresh parsing and reactivation fixedpoints,
and four drift/duplicate refusals before this supplied batch emits changes.
They retain unrelated source text and existing source identities. They establish
PlainText source custody; the frozen public map fixtures qualify runtime behavior
separately. They do not assert arbitrary composed-recipe transactionality.

Use Java 21 and the pinned POM. On a receiving host, route Maven's repository,
output and JVM temporary directory explicitly. Local Maven and hosted execution
are pending until recorded by the root task. The module has its own nominal and
serialization identity and is not a Spring concrete-class replacement.
