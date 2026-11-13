<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia complete-asset reuse: receiving policy candidate

This packet receives the named OpenRewrite recipe definition and exact guarded
resource data from [Synexia PR #9476](https://github.com/hsoliwal/com.synexia/pull/9476),
source commit `6ce2ea6d2e4b300ef7da239576f41852b3fafda9`.
The existing `m3-jdk-handoff` crate and `M3HashPinnedTextSnapshotRecipe` remain
the authoring/qualification owner. No new convergence engine is implemented.
This data-only snapshot is not a standalone Maven project or a runtime module.

Eligible Synexia-owned code, recipe bodies, templates, tests, fixtures and
proof artifacts are reusable assets. Keep applicable Apache-2.0 and provenance;
existing OpenJDK/third-party licences and target admission gates retain scope.
The target's existing donor-intake and naming policies are not replaced.
The guard is an unchanged Apache-2.0 target build-script snapshot, not a new build.

## Recovery boundary

At historical target base `d6da66ad1795271cf17769992a62cb47e76f3639`, #207 was
retained as ancestry but its policy document was absent. This remains absent at
receiving base `b4c26ad42d37198162e02d85ae57cc24ed7a80ef`; the build-script guard
still has blob `7ab72a5c85e84b2d97fdad3c305a885c233d0dd1`.

Recover only `m3/docs/M3JDK21_PORTING_INVARIANT.md` as a review candidate from
historical document `05daa15c75524cb2ed76cfdb0b44d62bacaa47fb`, with a corrected
preamble and complete-asset reuse section. Do not claim the machine policy,
BSR/runtime/tooling or full #207 changes have been restored. Existing maps,
statuses, product files, licensing and newer donor-intake work remain untouched.

## Qualification

In the exact source checkout, using its existing offline dependency cache:

```sh
mvn -o -f synexia-openrewrite-recipes/crates/m3-jdk-handoff/pom.xml \
  -Dtest=FreeReuseHandoffTest test
```

The test writes both source and target candidates under that crate's
`target/free-reuse-generated/`. Compare the generated target policy with this
packet's `policy.md.txt` and the receiving document, and bind results to the
receiving commit. A different existing policy, missing/wrong guard, source drift,
duplicate input or tampered template must refuse; second application must be
zero-diff. The YAML uses the existing classpath recipe engine, not a private
runtime service. Product builds acquire no Synexia dependency.

The authoring session passed 31 static hash/manifest/policy checks only.
Maven/OpenRewrite/JUnit were **not executed**. The documentation candidate is
serialized from the sealed template, not claimed as an executed recipe result.
Keep draft pending executable proof. No Java/JNI, JDK image, performance or
whole-estate completion is claimed.
