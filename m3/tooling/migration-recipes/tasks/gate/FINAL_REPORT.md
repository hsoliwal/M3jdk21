# Recipe gate repair

The declared candidate changes six existing recipe tests, adds GateTest, corrects
two malformed TSV manifests, and moves the exact JEP458 java.base descriptor
afterimage from the Java lane to the existing text lane. All26 product target
paths/preimages/postimages remain identical. Product source, main recipe classes,
DB tests/source and POMs are unchanged.

Strict crate construction remains strict. Configured composite execution plus
independent RecipeSerializer roundtrip and invalid-constructor tests replace the
inapplicable unconfigured introspection assumption. Exact fixtures use noTrim.
Scope tests enumerate the existing14 entries including JEP458's explicit API
authority. Exception tests check the original deepest guarded cause and target.

The java.base module descriptor is NOT pretended to have a lossless attributed
Java tree. Its exact text is hash-sealed and has an independent javac module-syntax
probe. Actual product compilation/linked behavior remain obligatory.

Existing Java/PlainText snapshot engines own GateJava and GateMeta. Their real
generation/refusal/fixed-point tests are supplied. No second recipe engine or
lambda abstraction is introduced. Generated and original source retains licensing.

Executed locally: pinned directory/POM identity, full delta inspection,27 active
manifest parses/124 output hashes and Java21 syntax parsing of7 candidate sources.
Syntax is not compilation with dependencies or JUnit/OpenRewrite execution.
Local Maven is absent. Actual CI, scoped coverage and full product proof remain
pending. Do not convert candidate inventory into a completion statement.

Reproduction after the patch:
```
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify
```
Run source-preserving gate recipes from their declared execution roots. The
source-lane change is explicit metadata, not automatic fallback on parser error.
