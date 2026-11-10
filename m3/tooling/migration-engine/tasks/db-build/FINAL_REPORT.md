# DbBuild candidate

The previous exact-head CI run executed all51 database JUnit tests successfully
and measured663/681 lines and235/241 branches. Both coverage ratios remain below
the unchanged0.99 gate. It separately failed compiling the existing JDK snapshot
recipe because the chained generic withId call exposed Tree, not SourceFile.

This pass reuses the existing sibling's six sequential SourceFile assignments.
It changes no guard, path rule, copy argument/order, checksum policy, public
signature, POM, workflow or DB source. The existing Java snapshot engine owns
both the production and additive test changes through exact before/after LST
templates. A small test-only donor fixture executes the corrected JDK engine.

All six existing DbRecipeTest methods remain; three additional cases cover
the repair's exact replay, JDK recipe metadata preservation/fixed point, and
missing/duplicate/wrong-tree refusal. Source/config/hash checks are executed.
The new head's actual Maven compilation and nine JUnit cases remain pending
until the hosted workflow provides evidence. No fabricated dependency stubs
or inferred green coverage are used.

Execution root: m3/tooling/migration-recipes.
Required focused command:
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml -Dtest=DbRecipeTest test

This is not whole-reactor verification, a99percent coverage pass, a modified-JDK
build, native/JNI parity or completion of the entire M3JDK21 programme.
