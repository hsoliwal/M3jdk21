<!-- SPDX-License-Identifier: Apache-2.0 -->
# CI repair

This packet repairs four observed failures at M3JDK commit
`a707bc5d64ba40c69910600f827939dfa6df6839`. It leaves product sources,
mapping states, coverage thresholds and the original port plans unchanged.

| Failure | Repair |
| --- | --- |
| Lane28 donor licence hash mismatch, Actions job 111757012164 | Restore the donor's original CRLF bytes. The publication script had used text-mode reading and changed this one file. All 99 published Lane28 entries were compared again with local bytes; this was the only transport difference. |
| TQ patch compilation discovers `java/lang/BootstrapMethodError.java`, job 111757011749 | Stage the three intended patch sources under `target/product-sources`, including the chosen original or mutant kernel. Keep `-Xlint:all -Werror`, native checks and all runtime modes. Full JDK compilation remains the image job's responsibility. |
| Foundation Maven compiler rejects release 21, job 111757011885 | Select the already checksum-pinned JDK through `JAVA_HOME` and `GITHUB_PATH`, as well as `M3_JDK`. |
| Mapped backing configure cannot find ALSA, job 111757011750 | Install the native JDK build prerequisites, retaining configure, build and jtreg commands. |

The existing `M3Jdk21HashPinnedTextSnapshotRecipe` generates the two workflows,
the test harness source and the restored licence from exact preimages. The
harness is a tooling text snapshot and is subsequently compiled by its own
Maven suite. No recipe engine is modified. The foundation workflow also runs
this packet's verification before its existing full recipe reactor.

```sh
mvn -B -ntp -f m3/tooling/ci-repair/pom.xml verify
python3 m3/tooling/ci-repair/verify-plan.py
mvn -B -ntp -f m3/tooling/tq/pom.xml verify
mvn -B -ntp -f m3/tooling/lane28/pom.xml verify
```

The sealed plan retains exact preimages and postimages. Its verifier checks
actual Maven output, replay, fixed point, rollback and no-write refusals for
drift, mixed states and changed dependencies. Hashes use raw bytes. In
particular, do not normalize `donor/LICENSE` or `LICENSE.txt` when publishing.

Local reruns use Temurin 21.0.8+9 and Maven 3.9.9: CI recipe 3/3,
TQ 12/12 and Lane28 12/12, with no failures or skipped tests. The JDK
`BootstrapMethodError.java` observed in the failed CI job was present during
the TQ rerun. These results do not establish a complete JDK image build,
jtreg success, the foundation reactor's coverage gate, or production consumer
integration. Those remain explicit CI/admission gates.

The original Lane28/TQ receipts remain historical evidence. This packet adds
its own receipt; it does not rewrite prior results. Synexia feedback from the
port is published in `hsoliwal/com.synexia#9316` and remains tied to its original
two cost-probe runs.
