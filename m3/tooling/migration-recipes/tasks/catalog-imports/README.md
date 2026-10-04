# Catalogue reuse in the existing A3 convergence recipe

Candidate scope: one existing production recipe and its existing JUnit owner. Baseline: 7ead93addf4a7b9614d4ad34c8dd02f4b93b9c36.

The user requested use of https://docs.openrewrite.org/recipes, not another transformation framework. Append the existing org.openrewrite.java.RemoveUnusedImports to M3Java21ConvergenceRecipe, after its unchanged inventory, atomization, pattern/IOP and documentation children. The existing A3Apply invokes this composite; no second scheduler, atomizer, function wrapper or runtime API is required.

The repository already pins rewrite-java 8.17.1. Its RemoveUnusedImports implementation keeps the upstream NoMissingTypes guard and can unfold wildcard imports according to the applicable ImportLayoutStyle. No dependency, POM, style, warning suppression or coverage threshold is changed. Incomplete attribution must not be described as proved import usage. Import cleanup does not prove arbitrary atom equivalence or implement a JEP.

OrderImports was also reviewed. It is not enabled here because its absent-style fallback is IntelliJ layout, not an independently verified original-JDK layout. A later explicit style decision must not be silently introduced by this change. The existing cohesive behavioral atoms, functions, lambdas, names and public interfaces remain the owners.

## Required proof

Preserve the two existing JUnit methods and extend the same test class. Verify exact child order, captured lambdas/method references, explicit import ordering, comments/literals, unknown-type refusal, native/throws declarations, Java LST/path/identity preservation and a second-pass fixed point. Non-Java source is not a Java AST transformation surface.

Run in a complete checkout:

```
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml -Dtest=M3Java21ConvergenceRecipeTest test
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify
```

The second command retains the complete module lifecycle and existing coverage gates. It must not be replaced by selected fixture compilation. A3 candidate processing, original OpenJDK configure/make, jtreg, JNI/GC/JIT and platform checks remain independently required for product acceptance.

## Authority and provenance

Existing caller: m3/tooling/a3/src/main/java/com/m3/a3/A3Apply.java (blob 9a0f66609a1450c30493b3316eb36cd0950ce414).
Pinned dependency source: openrewrite/rewrite tag v8.17.1, rewrite-java/src/main/java/org/openrewrite/java/RemoveUnusedImports.java; implementation is referenced through the existing dependency, not copied.
Catalogue: https://docs.openrewrite.org/recipes/java/removeunusedimports .
Reviewed but not enabled: https://docs.openrewrite.org/recipes/java/orderimports .

The recipe is a bounded Composite participant; the upstream leaf is a type-aware import visitor. The declared transformation scope is FILE, while this development change spans the existing recipe and its test within one module. Canonical promotion remains outside the recipe.

This is a draft integration until real OpenRewrite/JUnit and unchanged module gates execute. No full-JDK, all-backport or 99% coverage completion follows from source inspection or manually prepared fixture checks.
