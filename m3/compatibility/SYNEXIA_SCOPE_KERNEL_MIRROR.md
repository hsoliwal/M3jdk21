# Synexia canonical scope-kernel mirror

Status: transitional M3JDK21 receiver binding.

Canonical source:
- repository: `hsoliwal/com.synexia`
- revision: `c3af4e829e58c7c6818dde43a35ed97d367f4a92`
- canonical manifest: `synexia-openrewrite-recipes/CANONICAL_RECIPE_HOME.tsv`

The reusable scope kernel is canonical in Synexia:

- `com.synexia.rewrite.scope.M3ContractMode`
- `com.synexia.rewrite.scope.M3EditScope`
- `com.synexia.rewrite.scope.M3RecipeScopePolicy`
- `com.synexia.rewrite.scope.M3ScopeInference`

M3JDK21 temporarily retains:
1. the historical `com.m3.rewrite.scope` implementation as a frozen compatibility mirror; and
2. an exact `com.synexia.rewrite.scope` mirror imported from the pinned Synexia revision.

The M3JDK21-specific `M3RecipeScopeRegistry` remains target-owned because it binds exact JDK/backport recipe classes.

Semantic changes to the reusable kernel must be made in Synexia first. M3JDK21 may only repin and re-import qualified canonical bytes after target-side parity/compile/test proof.

No runtime/JDK product owner or promotion authority moves to Synexia.
