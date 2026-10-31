# OpenRewrite Java recipes on the M3 compiler

`m3-rewrite` compiles an explicit Java source set with M3JDK21, retains the
OpenRewrite lossless semantic trees converted directly from that compiler's
attributed units, and runs the selected recipes after compilation succeeds. It
prints a unified review patch by default. It never replaces the input files.

The optional OpenRewrite tools pack contains this runner, the existing
`m3-shared-openrewrite-frontend`, OpenRewrite **8.17.1** Java/core/YAML runtime,
and their complete Maven-resolved runtime dependency closure. The original JARs,
packages and dependency metadata are retained. Recipe libraries and their
additional dependencies can be supplied with repeated `--recipe-jar` options;
they must be compatible with the packaged framework version. The pack does not
include every language, third-party recipe catalog, or proprietary recipe.

## Run a configured recipe

Save `rewrite.yml`:

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: example.RenameApi
displayName: Rename typed API
description: Rename the declaration and its typed callers.
recipeList:
  - org.openrewrite.java.ChangeMethodName:
      methodPattern: 'demo.Api oldName(java.lang.String)'
      newMethodName: renamed
      matchOverrides: false
      ignoreDefinition: false
```

Run from the selected tools image:

```sh
bin/m3-rewrite \
  --source-root /path/to/project/src/main/java \
  --source demo/Api.java --source demo/Use.java \
  --recipe example.RenameApi --recipe-config /path/to/rewrite.yml \
  --patch /path/to/new-review.patch \
  -- --release 21 -proc:none
```

`--source` paths resolve against `--source-root`; other paths resolve against
the working directory. Input sources are UTF-8. Supply ordinary javac options
after `--`, including `--class-path`, `--module-path`, `--release`, or processor
options. An existing patch destination is refused before compilation. Without
`--patch`, patch text goes to standard output and diagnostics go to standard
error. Class output uses a temporary directory that is removed after the run;
`--classes-dir DIR` retains the normal javac class output when requested. These
class files represent the original compiled sources, before recipe changes.

For an external recipe, add its JAR and each of its dependencies:

```sh
bin/m3-rewrite --source-root src/main/java --source demo/Api.java \
  --recipe com.example.recipes.MyRecipe \
  --recipe-jar /path/to/my-recipes.jar \
  --recipe-jar /path/to/its-extra-dependency.jar \
  -- -proc:none
```

Repeated `--recipe` options select multiple qualified recipe names. YAML
configuration supplies constructor options and composite recipe definitions.
External JARs use a caller-scoped class loader whose parent owns the original
OpenRewrite runtime. The previous thread context class loader is restored when
the run ends. Recipes are executable code, with their ordinary runtime behavior;
the runner itself provides patch output, not a recipe sandbox.

## Compiler and source-set contract

The runner creates one `JavacTask` and opens its task-owned `M3AST` session with
syntax retention enabled. It forces `-XDcompilePolicy=byfile` so every top-level
type in a compilation unit has been attributed when the existing frontend
converts that unit. The first eligible `ANALYZE` callback retains one LST for
each requested source URI. Subsequent callbacks for other declarations in the
same file reuse that retained LST. The adapter validates exact source custody,
end positions, compiler context and lossless round-trip printing.

Input compilation units are never handed to a second Java parser or compiler
task. The existing frontend invokes OpenRewrite's Java 21 conversion visitor
over the actual javac unit. A recipe may still use OpenRewrite's normal parser
for synthetic JavaTemplate fragments; that is separate from converting the
supplied input source set. No prohibition on such recipe internals is claimed.

Only explicit `--source` inputs are recipe inputs. Implicit and annotation
processor generated sources participate in javac normally, but they are not
automatically added to the recipe source set. This matters for changes that
depend on all callers: list the complete intended input set yourself. Each
listed source must produce a supported, fully attributed LST. Units without an
analyzed top-level type, including the current `module-info.java` and
`package-info.java` cases, are refused. Other converter limitations fail the
run; there is no parser fallback. Recipe output paths must remain within the
source-root namespace.

The runner loads recipes and YAML only after successful compilation and complete
input coverage. Compiler errors, failed conversions, invalid configuration,
recipe exceptions and unsafe result paths produce no review patch. All result
diffs are assembled before patch output starts; file output uses `CREATE_NEW`.
Normal compilation may create class files or invoke explicitly configured
processors. The runner rejects options that override its encoding, class output,
compilation policy or syntax retention contract.

Exit status is `0` for successful recipe execution (including no changes), `1`
for compiler errors, and `2` for configuration, conversion or recipe failures.
The compiler diagnostic summary reports actual parsed-unit, analyzed-type,
conversion and source-validation counts. The recipe summary reports selected
recipe count, retained input LSTs and changed files. These are execution
counters, not performance claims.

## Build and package

Use the actual M3JDK21 image as `JAVA_HOME`, install the sibling existing-unit
frontend, then package this module with Maven:

```sh
mvn -f ../openrewrite/pom.xml install
mvn package
```

`mvn package` runs strict source and test compilation with `-Xlint:all -Werror`,
then the real JUnit/CLI recipe tests. Its outputs include:

- `target/m3-openrewrite-tool-1.0-SNAPSHOT.jar`, with the runner main class.
- `target/runtime-classpath.txt`, Maven's complete runtime classpath. Test-only
  dependencies are excluded.
- `target/runtime-dependencies.txt`, the resolved runtime coordinates and
  absolute artifact paths used by packaging.

The tools pack builder consumes these outputs plus the local Maven repository.
It preserves each runtime JAR, adjacent/parent POM metadata and license bytes,
and records artifact checksums. Its launchers read the exact
`m3.compiler.exports` property in this POM; the same exports are used by the test
JVM. OpenRewrite runs on the classpath to preserve the compiler/visitor loader
contract; its packages are neither relocated nor converted into fake modules.

`-Dm3.proof.directory=/absolute/proof/directory` redirects all Maven outputs,
which allows canonical OpenRewrite recipe qualification to keep source
candidates separate from build artifacts. The runtime closure resolves from
the real dependency graph rather than an abbreviated list of convenient JARs.

The integration tests execute the actual child-JVM CLI on two typed source
files and multiple top-level declarations, verify an external compiled recipe
JAR, preserve original source bytes and comments, and verify compilation-error,
recipe-error, unsupported-input, overwrite, compile-policy and path-traversal
refusals. The runner and adapter use the target repository's GPLv2 with
Classpath Exception license; bundled upstream artifacts retain their licenses.
