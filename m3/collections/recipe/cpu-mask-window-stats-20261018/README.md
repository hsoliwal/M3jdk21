<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 CPU masked-window stats — recipe receiver

This Maven POM is a **target receiving driver**, not a second canonical recipe.
The original Java source, test, exact before/after fingerprints and reusable
OpenRewrite recipe belong to hsoliwal/com.synexia.
The standalone M3 collection module remains Apache-2.0 and Java-base-only.

The receiver overlay manifest.tsv seals exactly two new Java source paths;
the .java.txt templates are copied unchanged from the Synexia recipe.
verify_donation.py admits these paths only when this exact manifest SHA-256
matches and all real installed source files match their postimage hashes.

To verify the bounded runtime after checking out this source with Java 21:

    python3 -m unittest discover -s m3/collections -p 'test_verify_cpu_mask_window_stats.py' -v
    mkdir -p target/m3-cpu-window/classes
    javac --release 21 -Xlint:all,-module -Werror -d target/m3-cpu-window/classes \
      $(find m3/collections/src/main/java -name '*.java' | sort) \
      m3/collections/src/test/java/com/m3/collections/M3CpuMaskWindowStatsSelfTest.java
    java -Xmx24m -cp target/m3-cpu-window/classes \
      com.m3.collections.M3CpuMaskWindowStatsSelfTest

The separate Synexia Maven/OpenRewrite SDK test and actual native-JNI
parity/whole-product jtreg gates must still run. The command is not evidence
that these tests have passed in GitHub Actions.
