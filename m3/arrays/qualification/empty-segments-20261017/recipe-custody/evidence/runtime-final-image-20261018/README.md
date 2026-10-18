<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Fresh M3 JDK image qualification

These are executed local Linux x86_64 fastdebug results from the received source
inputs identified in QUALIFICATION.json. The full JDK images target passed with
the original warning-as-error and doclint gates. HotSpot, JNI libraries and CDS
archives were built. Earlier failure receipts remain distinct predecessor
observations; the missing offline Maven clean plugin was subsequently fetched
through the configured repository and both modules were rebuilt from clean.

The six Object jtreg test files passed using jtreg 7.3.1-dev+0, the new image,
`-Xcheck:jni`, and an explicitly supplied stock Java 21 compiler. The compatibility
witness is expected to detect a HOLD: adding Object.toAdapter preempts a legacy
interface default and rejects previously valid source. A passing witness does
not mean universal compatibility passed. Ordinary dual Runnable/Callable
dispatch and checked exception cause identity passed the tested cases.

The existing M3PrimitiveImageCompiler, M3PrimitiveIdentityTest and fixed-arena
probe were used without changing their implementation. The fixture comes from
the existing primitive-address-identity verifier; no third-party fixture is
copied here. JNI reports 131403 checks and two independent sequential JVM arena
mappings at 0x600000000000. Each existing probe also checks collision refusal.
The binaries, class files and generated primitive image are not source custody
inputs; their SHA-256 identities and executed commands are in the receipts.

The original array native-linux module passed 10 JUnit tests plus its existing
CTest target, and the collection module passed 226 JUnit tests, both compiled
from clean and executed with the new M3 JDK. No original POM was changed.
QUALIFICATION.json retains each executed suite/test case and original XML hash.

These results are local qualification, not hosted CI, JCK, whole-SDK completion,
or phase promotion. The String-first rollout and Object compatibility HOLD
remain in force. Reproduce commands using the pinned repository sources and
replace transient workspace/toolchain paths with the local equivalents.
