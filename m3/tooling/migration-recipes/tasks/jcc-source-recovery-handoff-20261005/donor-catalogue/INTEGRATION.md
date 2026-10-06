# Catalogue integration and evidence epochs

This catalogue accompanies [implementation PR #9134](https://github.com/hsoliwal/com.synexia/pull/9134).
Its 18 records organize donor references and map the seven implemented donor tests to their
finite categories. It does not create a second runtime catalogue or search engine.

The authoring receipt and executed-test mapping preserve the earlier logged snapshot:
`35-final-main-tests.log` records 68 total tests with native mode off, and
`37-required-jni-donors.log` records the seven donor tests with native required. Those
immutable logs are [included with the implementation evidence](../evidence/logs/).

The subsequent complete replay on source epoch `72f6cbe0643ee046e31f144eea3413af4997d0b8`
passed 75 focused tests, eight parent tests, seven required-JNI donor tests and the
72-lane required-JNI parent test. The added seven focused tests exercise the newer host
verifier API. The two donor test files are byte-identical across these epochs; distinct
literal/regex pair counts remain 247 and 1,477 per mode. Read the
[execution evidence](../EVIDENCE.md) for source, classpath and recipe identities.

Develop advanced again during publication. The implementation PR records that concurrent
shared-transpiler integration separately. Neither this metadata nor the earlier passing
replay grants authority to overwrite those newer source changes.

## Using the catalogue in a recipe task

Start with the existing owner and its contract. Select a category, inspect the listed
primary reference, and bind the exact grammar, coordinate system, failure behavior and
source/license identity before adoption. Encode the candidate as a recipe with independent
fixtures, then use the existing compiler/JUnit/native gates to decide whether it is eligible.

The wildcard, bracket and address-validation references remain category pointers without
executed implementations in this packet. The source-retrieval gaps and floating references
are explicit in the catalogue. The earlier 1,871-case proposal is not executed evidence.
