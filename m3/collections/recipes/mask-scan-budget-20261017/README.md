# M3 recipe: source-budgeted mask scanning

Named recipe: com.synexia.m3.MaskScanBudget20261017. Base: BASE_SHA. Existing M3 Java
and native owners are refined, not replaced by a new engine or collection hierarchy.
Synexia recipe classes retain their original names and Apache-2.0 provenance, as build
infrastructure only. The product remains in hsoliwal/M3jdk21/com.m3.collections.

Every source change follows category review -> recipe/template refinement -> qualification
-> serial file emission -> exact verification. Original preimages and current co-input
hashes are sealed; do not delete original source to make an absent-only recipe admit it.
The core manifest engine and OpenRewrite wrapper are reused unchanged. Direct execution
of that manifest engine is NOT execution of the actual OpenRewrite scheduler.

From repository root:

    python3 m3/collections/recipes/mask-scan-budget-20261017/verify.py
    mvn -B -f m3/collections/recipes/mask-scan-budget-20261017/pom.xml clean verify

After changing reviewed templates, verify.py --apply qualifies fresh candidates before
serial installation. This is not a filesystem transaction; use an isolated checkout.
Partial states are admitted only when every target has an exact before/after hash.
A failed qualification installs nothing. Fix the recipe/templates, not output source.

The local runner compiles actual Java21 owners, original/current C17 window code, direct
JNI and the instrumented native Region boundary fixture. The latter uses a fake function
table with virtual array lengths, and is NOT counted as a real JVM test. Real JNI tests
are independent. The test .so is a scoped window-only binary, not a distribution replacement.

The full default scan remains unchanged. New source budget, empty-page resumption and
checkpoint semantics are additive; live predicate wrappers, sorted storage, donor names,
SWT/Nebula/RWT/GWT and java.base are left untouched. See generated docs and category TSV.
Actual SDK/JUnit, full reactor/JDK/platform and application gates have separate status in
this packet's evidence; no missing dependency is replaced by a type stub to claim success.
