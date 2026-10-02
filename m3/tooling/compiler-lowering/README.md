# Experimental explicit view compiler recipe

This Maven tooling module uses OpenRewrite 8.17.1 to remove a temporary combined
`String` at one already explicit view admission boundary. The recipe is disabled
unless its `enabled` option is exactly `true`:

```java
M3String.fromString(left + right)
// with attributed String operands and the matching public static helper:
M3String.fromConcatOperands(left, right)
```

Both operand ASTs and the original qualifier remain in their original evaluation
order. An instance-qualified static call therefore still evaluates its receiver.
The helper renders null String operands as `"null"`, after both operand
expressions have evaluated. Ordinary String concatenations and public String APIs
are untouched. This is an explicit-view admission optimization, not general
Java-to-M3 lowering or an `invokedynamic` replacement.

The recipe requires the resolved `com.m3.indexstring.M3String.fromString(String)`
owner and the exact public static `fromConcatOperands(String,String):M3String`
destination ABI. It refuses unresolved or non-String operands, implicit static
imports, layouts containing comments, and calls inside the M3String implementation.
It conservatively refuses String literals and final String variable references
anywhere in the invocation rather than attempting incomplete constant analysis.
Parenthesized top-level sums are currently left unchanged.

Allocation order, out-of-memory behavior, oversized concatenation exception
parity, linkage failures, allocation-sensitive reflection and identity observations
are **not proved equivalent**. The optimization changes retained atom/directory
structure and materialization cost. Keep it opt-in and review the dry-run diff;
the tests establish text/null and explicit operand/qualifier exception behavior
for the covered cases, not a complete Java semantic admission gate.

Run with JDK 21:

```sh
MAVEN_OPTS='-Xms32m -Xmx512m -XX:ActiveProcessorCount=2' mvn -B test
mvn -B install -DskipTests
```

The local [JDK 21 receipt](evidence/local-jdk21.json) binds raw candidate inputs
to 13 passing tests and records the actual compiler and child JVM arguments,
exit codes and equal before/after output. The covered fixture evaluates a static
selector, both operands, null values, and exceptions from each expression. Its
logging is captured in Surefire and the receipt; it does not change recipe behavior.

To try it in a separate Maven caller project, add a local `rewrite.yml`:

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: com.m3.tooling.OptInExplicitViewConcat
displayName: Opt in to explicit M3 view concatenation
description: Experimental allocation-changing transformation at explicit M3 view boundaries.
recipeList:
  - com.m3.tooling.ExplicitViewConcatRecipe:
      enabled: true
```

Then run the external Maven plugin with this recipe artifact on its tooling
classpath, while retaining the actual M3 text API on the caller's compile classpath:

```sh
mvn org.openrewrite.maven:rewrite-maven-plugin:5.23.1:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.m3.tooling:m3-explicit-view-lowering:0.1.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.m3.tooling.OptInExplicitViewConcat
```

The module, its dependencies and recipe configuration stay outside `java.base`
and the explicit text runtime. Donor inventory and attribution are recorded in
`FOSS_REUSE_DECISION.tsv` and `THIRD_PARTY_NOTICES.md`.
