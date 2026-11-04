# Joined view reachability tests

These are CPU tests for existing M3 owners, delivered as sealed source data by
com.synexia.m3.JoinedRetention20261102. No production String/array owner, native
pointer contract, source lock or promotion flag is changed.

The four JUnit tests distinguish discarded leaf arrays, the selected backing,
intentional within-leaf retention and empty Joined views. Sizes 32/1024/8192
exercise discarded leaves. A tiny tail view retains exactly its selected
256-byte backing. A one-character local slice intentionally retains its 1 MiB
backing until the view is released. Each success requires clearing and queue
notification; rooted negative phases include independent positive GC witnesses.

ForceGC.java.txt is the unmodified OpenJDK test helper, Git blob
e3587b9a2bebc4fd7a3e61d0790451ff2edcb0f0. Preserve its GPL-2.0-only copyright/license
header. New M3 tests and qualifier are Apache-2.0; the helper is not relicensed.

For a new component test run, materialize M3JoinedRetentionTest.java.txt and
ForceGC.java.txt as M3JoinedRetentionTest.java and ForceGC.java in an approved
temporary source directory. Use retention-pom.xml with Maven test and explicit
absolute properties m3.retention.module.jar, m3.retention.sources,
m3.retention.build, m3.retention.reports, m3.retention.temp and
m3.retention.home. Set m3.jvm.extra to -Xint, -XX:-CompactStrings, or
-Xbatch -XX:-TieredCompilation for additional modes. Route Maven/JVM caches and
temporary output explicitly. This direct Maven path accepts your built M3 module
JAR and produces a new test result; it does not inherit the recorded result.

qualify.py is the stricter recorded-artifact replay. It requires task-root, out,
jdk-home, maven, repo-head and repo-tail arguments, and the exact carrier JAR
identified by INPUTS.json. That relative fixture path refers to retained
qualification artifacts; it is not a runtime dependency on Synexia. The complete
JAR identity and five supplemental component class identities are checked against
the completed current 27-source/83-class build. Test execution is on the Java
classpath, with reflection used only to observe owned backing identity.

The recorded local Windows JDK21 run passed four JUnit methods in each of four
JVM configurations. This is a bounded reachability observation. It does not
measure total retained heap, prove C2 compilation occurred, guarantee GC liveness,
or qualify module encapsulation, JNI, POSIX execution or the whole JDK.

The canonical Synexia crate contains recipe replay, exact SDK results, Surefire
reports, actual command identities, input-tamper/process cleanup model controls
and retained first-run corrections. All reports keep promotion authority false.

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.
