# DbMark candidate and verified DB result

At565bc02094731fbe0dcf3a2fff75706be8d0a60b the database job passed61 tests,
zero failures/errors/skips. JaCoCo measured685/687 lines and240/241 branches,
with124/124 methods and14/14 classes. Both unchanged0.99 gates explicitly passed.
The downloadable artifact identity is recorded in PROVENANCE.tsv.

The same head's focused recipe job compiled and executed10 cases;9 passed.
One metadata expectation incorrectly prohibited the scheduler's provenance marker.
This candidate now seeds real nonempty metadata, checks its ID and payload,
requires the ordered original marker collection, and separately verifies the
single provenance marker and exact recipe stack. All prior tests remain; one
exact current-preimage replay/refusal case is added. DbMark and the synchronized
DbBuild templates use the existing recipe engine; no new production code,
dependency, POM, workflow or threshold is changed.

Static source/diff/hash checks ran. The new11-case JUnit and compilation verdict
remain pending, not inferred from the previous9 passes. Local Maven remains
unavailable. Actual CI must run the unchanged focused and full module commands.
The broader77-case recipe suite at565bc02 had12 failures, catalogued separately;
a focused green result cannot certify that suite, the reactor or the JDK.
No canonical merge or full-JDK/JNI/backport completion claim is made.
