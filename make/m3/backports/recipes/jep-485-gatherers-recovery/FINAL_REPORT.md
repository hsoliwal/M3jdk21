# JEP 485 current-master recovery

Current master again lost the JEP 485 product tree while retaining historical merge ancestry. Recovery is safe to attempt because all four modified Java 21 Stream preimages are still byte-identical to the preimages used by merged recovery PR #105.

This branch therefore replays, without regeneration, the exact reviewed PR #105 blobs for the seven Stream production owners, eight focused JDK tests, the hash-pinned OpenRewrite recipe/crate, preimage fixtures, packet/atom/dependency/upstream evidence, and named recipe.

Current metadata is merged rather than replaced. The restored wrapper is registered as `LIBRARY_API / EXPLICIT_CONTRACT_CHANGE`; the post-21 compatibility policy continues to classify JEP 485 as an opt-in SE API extension, never default stock Java 21.

The historical PR #105 recipe lane failed before reaching JEP 485 because of an unrelated A3 compiler defect that was repaired later. Its OpenJDK lane reached native prerequisite checks and failed only because `X11/extensions/Xrandr.h` was absent. This recovery adds `libxrandr-dev` to the existing workflow and changes no product semantics for that reason.

No build, jtreg, or hosted recipe success is claimed yet for this head. Exact-head CI must compile the recipes, prove source-sealed replay/fixed point, build the JDK image, run the Gatherer jtreg family and existing stream regressions, then compile/run the built-JDK API smoke before this candidate can be promoted.
