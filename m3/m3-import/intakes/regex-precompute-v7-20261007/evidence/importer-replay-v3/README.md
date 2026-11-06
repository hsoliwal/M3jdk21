# Complete importer correction recipe

This additive successor preserves the preceding importer proof and incomplete v3 packaging. `templates/` contains the identical three-target manifest/postimages plus the unchanged base-revision.txt from the previously passed recipe. No existing production owner or test was recompiled or rerun.

The existing sealed Replay executed through offline Maven: three changes applied, fixed-point check with zero changes, second application with zero changes and identical hashes/modification times, then a deliberately drifted third input refused before any receiver mutation. An unrelated file remained unchanged. Original Replay sources and all eight compiled classes remained unchanged.

The earlier proof records all 21 original unit cases passing with no skips and retains original failures. `IMPORTER_FIX_RECIPE_V3.diff` is the exact production qualifier and input-fixture delta; assertions and public exception ordering remain unchanged. The root accepted the small mechanical change for CPU review under the autoreview skill's non-trivial-edit trigger. No additional model review is claimed.

This qualifies the correction recipe and existing receiver importer only. Runtime regex source closure, java.base integration, the full-repository SeedSnapshot fixture, Jacoco verify thresholds, native providers and performance remain outside this receipt.
