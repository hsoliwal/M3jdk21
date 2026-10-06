# JEP 423 current-tree FILE admission

Receiving head: `7fd4c0bdec006691ce0e8f2408c8963f191cd2e5`

Baseline oracle: `openjdk/jdk@jdk-21+35`

Donor target state: `openjdk/jdk@jdk-22+36`

The 57 admitted JEP 423 paths were compared before recipe generation.

- 56 paths retain their exact JDK 21 GA coordinate (including five paths that were absent in JDK21 and remain absent).
- 1 path is held: `test/hotspot/jtreg/gc/g1/TestEvacuationFailure.java` is absent on current M3JDK21 master although JDK21 GA contains it.

Only the 56 `MECHANICAL_FILE_REPLAY` rows may enter automatic source-sealed FILE recipe generation. The held row requires history/dependency review and a target-specific adapted proof.

This receipt is admission evidence only. It grants no product mutation or promotion authority.
