# Qualified license-fixture policy application

This change receives exactly the three postimages emitted by the existing Maven/OpenRewrite `M3QualificationFixtureWhitespacePolicyRecipe`. The recipe, original providers, tests, fixtures, licenses and pinned verifier are already present in the parent PR1017; this application adds no replacement engine.

The actual [runner job](https://github.com/hsoliwal/M3jdk21/actions/runs/39009255463/job/117328013387) completed successfully at synthetic checkout `6a74036b38caeed3256af14e6c7ce2b0a908f026`, whose second parent is PR1017 head `94aeac9b20f30a16a4339617984b782e3638b98c`. The policy suite passed all four SDK tests, 104 closure and 32 report negative controls, eight partial states in both source orders, 21 recipe refusals and 18 raw-byte refusals. The existing one-workflow SDK passed its two tests separately. This does not claim execution of the new application commit.

The policy adds only four exact root-anchored license paths with `whitespace=-blank-at-eol`. All four license owners remain byte-identical. Both receiving workflows run their raw-input verifiers before their existing diff check. Unrelated, nested and suffixed trailing-whitespace cases, space-before-tab and blank-at-EOF remain rejected. The first-release workflow was already its qualified postimage and is kept exact.

[APPLICATION.json](APPLICATION.json) records every preimage, postimage, KEEP owner, observed receipt, report/log digest and artifact metadata. Artifact ZIP bytes were not independently downloaded or read.

The same run's immutable-text job still fails on existing `M3String.java` recipe drift. Its control-reactor job still fails on missing forked dependencies and Python backport tests. Those failures are separate and remain open. Matched complete-JDK tests, runtime acceptance and phase advancement remain unqualified.

The local bulk-output hold prevents local Maven and the structured autoreview helper. Independent source/preimage review passed. Storage protection and the complete migration/continual optimization goal remain active.

The [project recipe skill](https://github.com/hsoliwal/com.synexia/blob/5cd39d31b23f91db23176409a947529b3476eb1e/.claude/skills/m3-recipe-mastery/SKILL.md) states: “Keep the PR draft until the required Maven/OpenRewrite dry run, reactor compile/lint/JUnit gates, native/runtime gates and custody receipts actually execute.” This application remains a draft because its new commit and the broader integration gates are not qualified.
