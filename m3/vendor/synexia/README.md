# Pinned Synexia Apache donor snapshot

This tree is a verified donor/source plane imported from:

- repository: hsoliwal/com.synexia
- revision: 62aea466cf2f2bb4668f38aad9343d59525965b5
- manifest: m3/synexia-import/synexia-seed-export.tsv
- manifest root: ca570e874586c327f98e3982bf9e62a404e87e266b3195df69be06a0b5111ef6

The imported files are not added to M3JDK21 Maven or OpenJDK source roots automatically.

Their purpose is to let M3JDK21 recipes reuse polished Synexia atoms, tests, JNI mechanics,
machine invariants and OpenRewrite implementations without repeatedly rediscovering or rewriting
them.

For any capability:

    pinned Synexia snapshot
      -> M3JDK21 import inventory
      -> exact atom/recipe selection
      -> target-specific OpenRewrite port/adaptation
      -> JUnit / compiler / runtime / JNI parity as required
      -> fixed point
      -> promotion

The automatic import lane accepts only manifest rows licensed Apache-2.0 and restricted to
m3/vendor/synexia/. The preserved LICENSE and NOTICE in this directory apply to imported Synexia
material. Third-party donor material with other obligations is not admitted automatically.

OpenJDK source outside m3/ remains under its existing license regime.
