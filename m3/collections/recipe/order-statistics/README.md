# M3 LongRank source-sealed Maven recipe

This Maven module is the generation and verification authority for four new files under `m3/collections`. Its `M3RankIndexRecipe` reads original Java/C templates, checks their SHA-256 digests, and installs byte-identical product files. The product files are **generated outputs**, not the editing surface.

`mvn -B -f m3/collections/recipe/order-statistics/pom.xml verify` runs a fail-closed postimage check. `java -cp m3/collections/recipe/order-statistics/target/classes com.m3.recipe.M3RankIndexRecipe --apply .` installs missing outputs; it refuses to overwrite divergent files. If a transformation is incomplete, edit the template, update the pinned hash and rerun the recipe, then rebuild/test.

OpenRewrite remains the Maven-bound recipe framework for changes to existing Java ASTs. This recipe adds new files without modifying any existing Java interface, JDK source file, or JNI signature; a source-sealed file emitter is safer and simpler than an unnecessary AST visitor. The algorithm selection rationale is in `m3/collections/algorithm-catalog/order-statistics.tsv`.
