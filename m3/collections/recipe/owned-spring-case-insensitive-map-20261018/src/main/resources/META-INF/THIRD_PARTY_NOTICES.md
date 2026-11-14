# Attribution and selected donors

Spring Framework 6.2.0 is the policy donor, repository
https://github.com/spring-projects/spring-framework, commit
`5024bb72279188f1d0dcfe19e8abdd4bdb9887c8`. Its
`spring-core/src/main/java/org/springframework/util/LinkedCaseInsensitiveMap.java`
blob is `3a1f9f102946de7d7997fb7d9158315012685187`; the original copyright
2002-2023 and authors Juergen Hoeller and Phillip Webb remain in the owner
header. The original license blob is
`ff77379631504c17ad0a94dddc6803f0d3fb0fbb` and is retained byte for byte.
The owner marks its 2026 storage and nominal-identity modifications. An exact
upstream NOTICE.txt lookup returned 404; this is a bounded lookup only.

The array/index/capacity and runtime atoms are first-party Synexia/M3
Apache-2.0 sources from https://github.com/hsoliwal/M3jdk21, originally
inventoried at `0efc1719ca5766aead09f4f7c49f5067975bd946`. Their copyright
2026 Hitesh Soliwal and Synexia contributors and Synexia-to-M3 modification
notices are retained in selected source snapshots. The 28 behavioral atoms,
serial composition order and rejected whole-map substitutes are preserved
in the qualification atom-plan and source inventory. Current selected core
inputs used by current qualification are bound separately to the fixed130
selected-source list at master `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`;
CURRENT_BASE_RUNTIME_V5.json records the actual current context proof. Earlier
07ba6e/V3 qualification and the original atom donor inventory remain historical
provenance; four new core sources outside the selected130 list are not qualified
by that fixed selection.

The receiving engine and two-test host are first-party Apache-2.0 artifacts
borrowed unchanged/mechanically adapted from the qualified Guava receiving
packet. OpenRewrite 8.17.1 and JUnit 5.10.2 are receiving/test dependencies
only. The installed JDK public APIs supply standard protocol scaffolding.
The production module has no Spring or OpenRewrite dependency.

OpenJDK source references were evaluated as GPL-2.0-with-Classpath-Exception
protocol references only. Their code is excluded from production and from
this packet's donor snapshots; their source-reference inventory remains
historical evidence and does not relicense those references. Apache Commons
CaseInsensitiveMap was evaluated and rejected for incompatible policy.
No LeetCode, HackerRank or GeeksforGeeks solution/article code was copied.

Raw inventories, before-code decisions, donor/test selection, original V1
failures and V2 proof receipts are retained in the qualification directory.
This attribution does not assert concrete Spring assignability, identical
serialized bytes, resource parity, performance, native map storage or full
five-library completion.
