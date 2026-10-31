#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Wrap frozen joined qualifiers; retain complete three-cohort evidence in the existing lane."""
import argparse
import base64
import hashlib
import itertools
import json
import os
from pathlib import Path, PurePosixPath
import re
import runpy
import statistics
import subprocess

CFG = json.loads(r'''{"schema":"M3_JOINED_FRESH_EXISTING_LANE_AUTHOR_INPUT_V1","copyright":"Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project","license":"Apache-2.0","admissionCommit":"7781500be49985e388fb08bf3a32358ca94167c1","baseCommit":"8a38c6f84ab3052df51c018025fce5dcb0a22f8a","sourceSdkCommit":"3a29973c723e1432219cdf09a0650b7458c91197","sdkTree":"0079fba204e2a2820e66c4a83268ca6bb51cd329","publicPrefix":"m3/tooling/joined-range-directory/fresh-owned/","taskPrefix":"synexia-openrewrite-recipes/tasks/joined-fresh-owned-ranges-20261020/integration/","phases":["binding","fresh-binding","toolchain","sdk-inputs","fresh-sdk-inputs","sdk-maven","sdk-reports","range-runtime","sdk-report","fresh-sdk-maven","fresh-sdk-reports","fresh-runtime","fresh-report"],"legacyMembers":["TEST-com.synexia.rewrite.M3JoinedRangeDirectoryRecipeTest.xml","authoring-recipe-test-counters.json","authoring-sdk-receipt.json","baseline-benchmark-0.log","baseline-benchmark-1.log","baseline-benchmark-2.log","baseline-benchmark-3.log","baseline-benchmark-4.log","baseline-original-build-logs.jsonl","baseline-original-build.log","baseline-range-c2.log","baseline-range-compile.log","baseline-range-int.log","baseline-range-jit.log","baseline-range-nocompact.log","binding-receipt.json","binding.log","candidate-benchmark-0.log","candidate-benchmark-1.log","candidate-benchmark-2.log","candidate-benchmark-3.log","candidate-benchmark-4.log","candidate-original-build-logs.jsonl","candidate-original-build.log","candidate-range-c2.log","candidate-range-compile.log","candidate-range-int.log","candidate-range-jit.log","candidate-range-nocompact.log","com.synexia.rewrite.M3JoinedRangeDirectoryRecipeTest.txt","commands.json","emitted-forward-JoinedM3StringPiece.java","emitted-inverse-JoinedM3StringPiece.java","hosted-receipt.json","inner-commands.jsonl","inventory.json","log-tail.txt","qualification-receipt.json","range-runtime.log","runtime-receipt.json","sdk-inputs.log","sdk-maven.log","sdk-report.log","sdk-reports.log","source-binding.json","storage-guard-receipt.json","toolchain.log"],"freshMembers":["fresh-source-binding.json","fresh-binding-receipt.json","fresh-TEST-com.synexia.rewrite.M3JoinedRangeDirectoryRecipeTest.xml","fresh-authoring-recipe-test-counters.json","fresh-authoring-sdk-receipt.json","fresh-owned-java-execution.json","fresh-emitted-forward-JoinedM3StringPiece.java","fresh-emitted-inverse-JoinedM3StringPiece.java","fresh-host-raw.jsonl","fresh-original-build-logs.jsonl","fresh-runtime-raw.jsonl","fresh-runtime-receipt.json","fresh-qualification-receipt.json","fresh-outer-commands.jsonl","fresh-hosted-receipt.json","fresh-storage-guard-receipt.json","fresh-inventory.json"],"legacyBinding":{"schema":"M3_JOINED_RANGE_DIRECTORY_SOURCE_BINDING_V1","repository":"hsoliwal/com.synexia","receiverRepository":"hsoliwal/M3jdk21","sourceCommit":"7012002387d601b8343063eb8d35014d2530d1d5","admissionCommit":"ec4309cde9a5311347aab535b1cee4a2be442742","sdkTree":"2086a6175e8b573fa340a3458eaa382f11b3378d","sdkFiles":34,"runtimeApplied":false,"phaseAdvanced":false,"files":[{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/FOSS_REUSE_DECISION.tsv","targetPath":"m3/tooling/joined-range-directory/sdk/FOSS_REUSE_DECISION.tsv","gitBlob":"58ea3f9d3e6a5c3b25594f0d130b31f8a0f49cc2","bytes":467,"sha256":"c42fbcd08fa3f1327c6fbbbafd42c6465407ce8afdea30a36ffe36984a5dc172"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/README.md","targetPath":"m3/tooling/joined-range-directory/sdk/README.md","gitBlob":"002a9c1814184ac276c971bdb5dec164ec0eba33","bytes":1528,"sha256":"c7d01c9fd9a97eb6d9a8824a1b2a30f470616e05cf513d07d3558acedf277c93"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/THIRD_PARTY_NOTICES.md","targetPath":"m3/tooling/joined-range-directory/sdk/THIRD_PARTY_NOTICES.md","gitBlob":"70c29b10e5d06f66bfdf8759ef37dd1c4b9f4b0c","bytes":836,"sha256":"014ae516720b317b4f3dbdaadabd191dae817f2fad060fc3dc0d7f0547de5b0c"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/pinned-engine/com/synexia/rewrite/M3JoinedRangeDirectoryInverseRecipe.java","targetPath":"m3/tooling/joined-range-directory/sdk/pinned-engine/com/synexia/rewrite/M3JoinedRangeDirectoryInverseRecipe.java","gitBlob":"0fbb61ead7047e0b2ab0627bd381967113d99db1","bytes":766,"sha256":"3a621135ee30d294cb86539604e245684084dc11f4fb44be0129c3f3214120f7"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/pinned-engine/com/synexia/rewrite/M3JoinedRangeDirectoryRecipe.java","targetPath":"m3/tooling/joined-range-directory/sdk/pinned-engine/com/synexia/rewrite/M3JoinedRangeDirectoryRecipe.java","gitBlob":"f1bafb2c8e156e2a0ad8479d3996bf139a6977df","bytes":755,"sha256":"75ea82b2c12b7680c7dbd61f6334b053522f918a72045b61e03be408409f2600"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java","targetPath":"m3/tooling/joined-range-directory/sdk/pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java","gitBlob":"38df5f97bbeec8a8a8cfc77091d56bda925c07f2","bytes":10964,"sha256":"56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java","targetPath":"m3/tooling/joined-range-directory/sdk/pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java","gitBlob":"1c1b1b568d369f688f6953161636bf36b484bf5a","bytes":5187,"sha256":"ee90a2b8689699745553c2f4d2d07121b91777f4e9a671fdb33ba8edea0c14ec"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/pom.xml","targetPath":"m3/tooling/joined-range-directory/sdk/pom.xml","gitBlob":"d79867bcc09ad6329e12418e15907e54b8ab025f","bytes":2678,"sha256":"f0ab99c3dafb38e82f8e5fd4c6d22db07831808c251c4622f292114c83e6673e"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/META-INF/rewrite/joined-range-directory-20261020.yml","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/META-INF/rewrite/joined-range-directory-20261020.yml","gitBlob":"e1c6c89d97f90f16a7f3a6660f3f86b6e36340b0","bytes":698,"sha256":"632f935ee7bc550dd3814ea3d0cb3d18cb0b74216fd65bfe274313e5f22a3f72"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/test/java/com/synexia/rewrite/M3JoinedRangeDirectoryRecipeTest.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/test/java/com/synexia/rewrite/M3JoinedRangeDirectoryRecipeTest.java","gitBlob":"a5d075d9b9cb49df6e7bcd70aba804155fa4a79e","bytes":15923,"sha256":"0ab6ca345d2d7d08e078b381964802d21343f8140329e43abb755e7678ec1bb2"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/verify.py","targetPath":"m3/tooling/joined-range-directory/sdk/verify.py","gitBlob":"42f83467ff9255688aa1608a296ff64388fa2fce","bytes":24815,"sha256":"fa92d494ff01fa38f50248c3ea59e60486b4712991edac0db8bcce82f9707810"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/JoinedM3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/JoinedM3StringPiece.java","gitBlob":"cc3c84f5ea0230e2f57e3d3ae0f5d29b9241e597","bytes":2634,"sha256":"694ee6a9fa06803244fd56d7a846da16f1560d242a0660ae2f4a927c43fc3dcb"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/JoinedM3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/JoinedM3StringPiece.java","gitBlob":"3edc23c22362b08af00f7332ddc445a0274a6dd4","bytes":2919,"sha256":"10ddd082c142420c29985a85d8b2f77507fdbcde2552b8bfc154e658f3b2a9e3"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/LocalM3Arena.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/LocalM3Arena.java","gitBlob":"d82710cd1a49b54d92afcd74cdd903cd009766ba","bytes":1975,"sha256":"99e6adce8be83bf6ebbe2128409e4f34cff88e0d06602c95e21f64a27898b73d"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/LocalM3Arena.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/LocalM3Arena.java","gitBlob":"d82710cd1a49b54d92afcd74cdd903cd009766ba","bytes":1975,"sha256":"99e6adce8be83bf6ebbe2128409e4f34cff88e0d06602c95e21f64a27898b73d"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/LocalM3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/LocalM3StringPiece.java","gitBlob":"6b745c9472144ad6de01de4d06a533974e558fb1","bytes":2115,"sha256":"06370c193953ead287f6fd6c423e12bd7761da8bf1c825dacd7445afe1876b51"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/LocalM3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/LocalM3StringPiece.java","gitBlob":"6b745c9472144ad6de01de4d06a533974e558fb1","bytes":2115,"sha256":"06370c193953ead287f6fd6c423e12bd7761da8bf1c825dacd7445afe1876b51"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/M3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/M3StringPiece.java","gitBlob":"99cb72623fa6bc2d5426553e04aaf0e5bfa993cc","bytes":767,"sha256":"d43e66b414d96ba4b16b8cc94f64fa5a01c2dcb350ad58050105b04996486479"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/M3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/M3StringPiece.java","gitBlob":"99cb72623fa6bc2d5426553e04aaf0e5bfa993cc","bytes":767,"sha256":"d43e66b414d96ba4b16b8cc94f64fa5a01c2dcb350ad58050105b04996486479"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/StorageIdentity.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/StorageIdentity.java","gitBlob":"7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a","bytes":494,"sha256":"b1e4fcbd385adc7c01c56d0f6a6af63d42ef0e6fa75b46d66d8d75c64c7a6417"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/StorageIdentity.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/StorageIdentity.java","gitBlob":"7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a","bytes":494,"sha256":"b1e4fcbd385adc7c01c56d0f6a6af63d42ef0e6fa75b46d66d8d75c64c7a6417"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/manifest.tsv","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/manifest.tsv","gitBlob":"fee91b96a6807c8f01b38db58043db48609d146d","bytes":950,"sha256":"c9b58940e77a31df101e0b1be6254e6d73c37bcd4e3ab3ccd098a944c9c3cfde"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/JoinedM3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/JoinedM3StringPiece.java","gitBlob":"3edc23c22362b08af00f7332ddc445a0274a6dd4","bytes":2919,"sha256":"10ddd082c142420c29985a85d8b2f77507fdbcde2552b8bfc154e658f3b2a9e3"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/JoinedM3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/JoinedM3StringPiece.java","gitBlob":"cc3c84f5ea0230e2f57e3d3ae0f5d29b9241e597","bytes":2634,"sha256":"694ee6a9fa06803244fd56d7a846da16f1560d242a0660ae2f4a927c43fc3dcb"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/LocalM3Arena.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/LocalM3Arena.java","gitBlob":"d82710cd1a49b54d92afcd74cdd903cd009766ba","bytes":1975,"sha256":"99e6adce8be83bf6ebbe2128409e4f34cff88e0d06602c95e21f64a27898b73d"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/LocalM3Arena.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/LocalM3Arena.java","gitBlob":"d82710cd1a49b54d92afcd74cdd903cd009766ba","bytes":1975,"sha256":"99e6adce8be83bf6ebbe2128409e4f34cff88e0d06602c95e21f64a27898b73d"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/LocalM3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/LocalM3StringPiece.java","gitBlob":"6b745c9472144ad6de01de4d06a533974e558fb1","bytes":2115,"sha256":"06370c193953ead287f6fd6c423e12bd7761da8bf1c825dacd7445afe1876b51"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/LocalM3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/LocalM3StringPiece.java","gitBlob":"6b745c9472144ad6de01de4d06a533974e558fb1","bytes":2115,"sha256":"06370c193953ead287f6fd6c423e12bd7761da8bf1c825dacd7445afe1876b51"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/M3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/M3StringPiece.java","gitBlob":"99cb72623fa6bc2d5426553e04aaf0e5bfa993cc","bytes":767,"sha256":"d43e66b414d96ba4b16b8cc94f64fa5a01c2dcb350ad58050105b04996486479"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/M3StringPiece.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/M3StringPiece.java","gitBlob":"99cb72623fa6bc2d5426553e04aaf0e5bfa993cc","bytes":767,"sha256":"d43e66b414d96ba4b16b8cc94f64fa5a01c2dcb350ad58050105b04996486479"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/StorageIdentity.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/StorageIdentity.java","gitBlob":"7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a","bytes":494,"sha256":"b1e4fcbd385adc7c01c56d0f6a6af63d42ef0e6fa75b46d66d8d75c64c7a6417"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/StorageIdentity.java","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/StorageIdentity.java","gitBlob":"7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a","bytes":494,"sha256":"b1e4fcbd385adc7c01c56d0f6a6af63d42ef0e6fa75b46d66d8d75c64c7a6417"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/manifest.tsv","targetPath":"m3/tooling/joined-range-directory/sdk/src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/manifest.tsv","gitBlob":"c831676b2a829839e348f3815223f48d9a216452","bytes":950,"sha256":"1f66b537f72694851c98c090ae9be51da753d191b1295b0017879e6d9cb088b9"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/sdk/pins.json","targetPath":"m3/tooling/joined-range-directory/sdk/pins.json","gitBlob":"3014fdfc4e4e4d5008815ef5e23077246a545a1f","bytes":10006,"sha256":"d268fd16adb27e8b94b6444b12c249e36e915a1c09e83501955bd51b3a8f0959"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/hosted/qualify.py","targetPath":"m3/tooling/joined-range-directory/qualify.py","gitBlob":"677ecb2bcd6811862922990261037bb7648d33ea","bytes":20530,"sha256":"2d1b63c2c3ba9ad01fbd3d44b3cf1aa1146608f57197ff58103c81bf0c73422e"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/hosted/qualification.yml","targetPath":".github/workflows/m3-joined-range-directory.yml","gitBlob":"e38f1340062246a355ac0e6080706a008593eecd","bytes":32362,"sha256":"20d9e3b8eb0b32a43c4aef32fa7a0f643e89178d9eb9f1e237311d0320653ba5"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/verification/qualify.py","targetPath":"m3/tooling/joined-range-directory/runtime/qualify.py","gitBlob":"214017285242d75cb78b7fb5392243fb2d5b018e","bytes":17994,"sha256":"539a433663548fb0393972e0e66c1bb0a0153f2c45bc58ead05735e44877283b"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/verification/M3JoinedRangeProof.java","targetPath":"m3/tooling/joined-range-directory/runtime/M3JoinedRangeProof.java","gitBlob":"833b13b220110b6942277c2b8b8a4fd78ce9e285","bytes":12007,"sha256":"0ac096f7d0a939877d348b0492185d8226eb61611d2634fb6b50a1813004e004"},{"sourcePath":"synexia-openrewrite-recipes/tasks/joined-range-directory-20261020/verification/BUILD_INPUTS.json","targetPath":"m3/tooling/joined-range-directory/runtime/BUILD_INPUTS.json","gitBlob":"921d708e2a0f1f1c43e95989ea9e37e02e22790a","bytes":14938,"sha256":"161b3356c94e93182e63e3c914f42ded0494c401ad7285b163738c18bc7ff4f2"}],"receiverOriginalOwners":[{"path":"m3/core/src/com/m3/text/JoinedM3StringPiece.java","role":"REPLACE","gitBlob":"cc3c84f5ea0230e2f57e3d3ae0f5d29b9241e597","bytes":2634,"sha256":"694ee6a9fa06803244fd56d7a846da16f1560d242a0660ae2f4a927c43fc3dcb"},{"path":"m3/core/src/com/m3/text/M3StringPiece.java","role":"KEEP","gitBlob":"99cb72623fa6bc2d5426553e04aaf0e5bfa993cc","bytes":767,"sha256":"d43e66b414d96ba4b16b8cc94f64fa5a01c2dcb350ad58050105b04996486479"},{"path":"m3/core/src/com/m3/text/LocalM3StringPiece.java","role":"KEEP","gitBlob":"6b745c9472144ad6de01de4d06a533974e558fb1","bytes":2115,"sha256":"06370c193953ead287f6fd6c423e12bd7761da8bf1c825dacd7445afe1876b51"},{"path":"m3/core/src/com/m3/text/LocalM3Arena.java","role":"KEEP","gitBlob":"d82710cd1a49b54d92afcd74cdd903cd009766ba","bytes":1975,"sha256":"99e6adce8be83bf6ebbe2128409e4f34cff88e0d06602c95e21f64a27898b73d"},{"path":"m3/core/src/com/m3/text/StorageIdentity.java","role":"KEEP","gitBlob":"7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a","bytes":494,"sha256":"b1e4fcbd385adc7c01c56d0f6a6af63d42ef0e6fa75b46d66d8d75c64c7a6417"}],"receiverOriginalBuildInputs":58},"sdkPins":{"schema":"M3_JOINED_FRESH_OWNED_RANGE_SDK_INPUTS_V1","admissionCommit":"80253b14a14191ad826a5c7c7594100c7c3c588a","sourceOnly":true,"providers":{"openrewriteAdapter":"1c1b1b568d369f688f6953161636bf36b484bf5a","manifestKernel":"38df5f97bbeec8a8a8cfc77091d56bda925c07f2"},"files":[{"path":"FOSS_REUSE_DECISION.tsv","gitBlob":"39e6a213f1e03b491a7dfbaa4f4cf3d8b397bc65","bytes":1564,"sha256":"e8104b89f703ef35bc726bb1000744e2a80b8a0bf4196853251243d115db3c53"},{"path":"README.md","gitBlob":"04d4034742a583784f118541b275d45bcdde0f1c","bytes":2959,"sha256":"8061a80a44e09f7749bdd33ae32abee22accac7d5c95685e0e52d5a46d7f5bd1"},{"path":"THIRD_PARTY_NOTICES.md","gitBlob":"e33a0ce6c98280a816dbb51413d358628c680453","bytes":1383,"sha256":"8471c914183472440b5e91f887efd2b061a512e2516442e007bf2513efdc5b03"},{"path":"pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java","gitBlob":"38df5f97bbeec8a8a8cfc77091d56bda925c07f2","bytes":10964,"sha256":"56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999"},{"path":"pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java","gitBlob":"1c1b1b568d369f688f6953161636bf36b484bf5a","bytes":5187,"sha256":"ee90a2b8689699745553c2f4d2d07121b91777f4e9a671fdb33ba8edea0c14ec"},{"path":"pinned-engine/com/synexia/rewrite/M3JoinedRangeDirectoryInverseRecipe.java","gitBlob":"0fbb61ead7047e0b2ab0627bd381967113d99db1","bytes":766,"sha256":"3a621135ee30d294cb86539604e245684084dc11f4fb44be0129c3f3214120f7"},{"path":"pinned-engine/com/synexia/rewrite/M3JoinedRangeDirectoryRecipe.java","gitBlob":"f1bafb2c8e156e2a0ad8479d3996bf139a6977df","bytes":755,"sha256":"75ea82b2c12b7680c7dbd61f6334b053522f918a72045b61e03be408409f2600"},{"path":"pom.xml","gitBlob":"d79867bcc09ad6329e12418e15907e54b8ab025f","bytes":2678,"sha256":"f0ab99c3dafb38e82f8e5fd4c6d22db07831808c251c4622f292114c83e6673e"},{"path":"src/main/resources/META-INF/rewrite/joined-range-directory-20261020.yml","gitBlob":"e1c6c89d97f90f16a7f3a6660f3f86b6e36340b0","bytes":698,"sha256":"632f935ee7bc550dd3814ea3d0cb3d18cb0b74216fd65bfe274313e5f22a3f72"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/JoinedM3StringPiece.java","gitBlob":"3edc23c22362b08af00f7332ddc445a0274a6dd4","bytes":2919,"sha256":"10ddd082c142420c29985a85d8b2f77507fdbcde2552b8bfc154e658f3b2a9e3"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/LocalM3Arena.java","gitBlob":"d82710cd1a49b54d92afcd74cdd903cd009766ba","bytes":1975,"sha256":"99e6adce8be83bf6ebbe2128409e4f34cff88e0d06602c95e21f64a27898b73d"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/LocalM3StringPiece.java","gitBlob":"6b745c9472144ad6de01de4d06a533974e558fb1","bytes":2115,"sha256":"06370c193953ead287f6fd6c423e12bd7761da8bf1c825dacd7445afe1876b51"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/M3StringPiece.java","gitBlob":"99cb72623fa6bc2d5426553e04aaf0e5bfa993cc","bytes":767,"sha256":"d43e66b414d96ba4b16b8cc94f64fa5a01c2dcb350ad58050105b04996486479"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/before/m3/core/src/com/m3/text/StorageIdentity.java","gitBlob":"7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a","bytes":494,"sha256":"b1e4fcbd385adc7c01c56d0f6a6af63d42ef0e6fa75b46d66d8d75c64c7a6417"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/manifest.tsv","gitBlob":"87a12624563e4c8af45ac7ea4a82efd90f9d59a4","bytes":950,"sha256":"13541b4fc43566e2c84ed77e6e38a114fa0a7cb5a26ffaffd7256cdd12fe2ca4"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/JoinedM3StringPiece.java","gitBlob":"4f88f794266375ad8174fa56e952c30d32689877","bytes":3377,"sha256":"d69ef5cc0e4bd5d95808978b4a0d73982322230a623d897074840b58fab1993e"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/LocalM3Arena.java","gitBlob":"d82710cd1a49b54d92afcd74cdd903cd009766ba","bytes":1975,"sha256":"99e6adce8be83bf6ebbe2128409e4f34cff88e0d06602c95e21f64a27898b73d"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/LocalM3StringPiece.java","gitBlob":"6b745c9472144ad6de01de4d06a533974e558fb1","bytes":2115,"sha256":"06370c193953ead287f6fd6c423e12bd7761da8bf1c825dacd7445afe1876b51"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/M3StringPiece.java","gitBlob":"99cb72623fa6bc2d5426553e04aaf0e5bfa993cc","bytes":767,"sha256":"d43e66b414d96ba4b16b8cc94f64fa5a01c2dcb350ad58050105b04996486479"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/forward/out/m3/core/src/com/m3/text/StorageIdentity.java","gitBlob":"7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a","bytes":494,"sha256":"b1e4fcbd385adc7c01c56d0f6a6af63d42ef0e6fa75b46d66d8d75c64c7a6417"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/JoinedM3StringPiece.java","gitBlob":"4f88f794266375ad8174fa56e952c30d32689877","bytes":3377,"sha256":"d69ef5cc0e4bd5d95808978b4a0d73982322230a623d897074840b58fab1993e"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/LocalM3Arena.java","gitBlob":"d82710cd1a49b54d92afcd74cdd903cd009766ba","bytes":1975,"sha256":"99e6adce8be83bf6ebbe2128409e4f34cff88e0d06602c95e21f64a27898b73d"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/LocalM3StringPiece.java","gitBlob":"6b745c9472144ad6de01de4d06a533974e558fb1","bytes":2115,"sha256":"06370c193953ead287f6fd6c423e12bd7761da8bf1c825dacd7445afe1876b51"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/M3StringPiece.java","gitBlob":"99cb72623fa6bc2d5426553e04aaf0e5bfa993cc","bytes":767,"sha256":"d43e66b414d96ba4b16b8cc94f64fa5a01c2dcb350ad58050105b04996486479"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/before/m3/core/src/com/m3/text/StorageIdentity.java","gitBlob":"7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a","bytes":494,"sha256":"b1e4fcbd385adc7c01c56d0f6a6af63d42ef0e6fa75b46d66d8d75c64c7a6417"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/manifest.tsv","gitBlob":"3f1f1fc694c4dd28cdbac948b7f639d87a8cf708","bytes":950,"sha256":"90c2fdfebb9c2d3997515bb0d5f75ad2f3c7a25202e14b5f372501068491bf74"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/JoinedM3StringPiece.java","gitBlob":"3edc23c22362b08af00f7332ddc445a0274a6dd4","bytes":2919,"sha256":"10ddd082c142420c29985a85d8b2f77507fdbcde2552b8bfc154e658f3b2a9e3"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/LocalM3Arena.java","gitBlob":"d82710cd1a49b54d92afcd74cdd903cd009766ba","bytes":1975,"sha256":"99e6adce8be83bf6ebbe2128409e4f34cff88e0d06602c95e21f64a27898b73d"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/LocalM3StringPiece.java","gitBlob":"6b745c9472144ad6de01de4d06a533974e558fb1","bytes":2115,"sha256":"06370c193953ead287f6fd6c423e12bd7761da8bf1c825dacd7445afe1876b51"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/M3StringPiece.java","gitBlob":"99cb72623fa6bc2d5426553e04aaf0e5bfa993cc","bytes":767,"sha256":"d43e66b414d96ba4b16b8cc94f64fa5a01c2dcb350ad58050105b04996486479"},{"path":"src/main/resources/com/synexia/rewrite/joined-range-directory-20261020/inverse/out/m3/core/src/com/m3/text/StorageIdentity.java","gitBlob":"7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a","bytes":494,"sha256":"b1e4fcbd385adc7c01c56d0f6a6af63d42ef0e6fa75b46d66d8d75c64c7a6417"},{"path":"src/test/java/com/synexia/rewrite/M3JoinedRangeDirectoryRecipeTest.java","gitBlob":"73471b80d618b4ab2b016d097459dd5cb390ef93","bytes":22058,"sha256":"a36dd62e6d2cb03614473e90e77298971bb8203eeeefcf1f11689f3d128ec61f"},{"path":"src/test/resources/com/synexia/rewrite/joined-range-directory-20261020/proof/M3JoinedFreshRangeProof.java.txt","gitBlob":"fa35d3170995ad74f6d7363d6e3fe85dd0b2449c","bytes":14816,"sha256":"8194f210675ff2da605989cf885687549a77ac6037757ac0a8fabcea307cee1f"},{"path":"verify.py","gitBlob":"40c2e4a35315e953500ec3a3942bdc584db2f2b7","bytes":25785,"sha256":"b1cf55f585929b49f6ffeea0f4e5c93314c0e1bf3e89976c1ddc95fa070236d3"}]},"sdkPinsIdentity":{"path":"synexia-openrewrite-recipes/tasks/joined-fresh-owned-ranges-20261020/sdk/pins.json","gitBlob":"329efb21b5878910ff3db1277bae25a948d62e13","bytes":10316,"sha256":"72274d3e0dbdaf86d8cadd8a41c696bcd7a43ba3ab729f0fb0da8c018547db9e"},"statusGit":"e4be7819e216ccbc3ca6de3254bf0f1131be462b","planGit":"91a5e17276a1e16dff87a62faf58dc4fe35ae8f2","workloadGit":"ca8dfd1646918e8b87952a67e10e60d5e431fe45","originalLogPaths":["m3/build/logs/artifacts.sha256","m3/build/logs/compile.log","m3/build/logs/dictionary-frequency-contract-c2.log","m3/build/logs/dictionary-frequency-contract-int.log","m3/build/logs/dictionary-frequency-contract-jit.log","m3/build/logs/dictionary-frequency-contract-nocompact.log","m3/build/logs/grammar-support-c2.log","m3/build/logs/grammar-support-int.log","m3/build/logs/grammar-support-jit.log","m3/build/logs/grammar-support-nocompact.log","m3/build/logs/instance-index-c2.log","m3/build/logs/instance-index-int.log","m3/build/logs/instance-index-jit.log","m3/build/logs/instance-index-nocompact.log","m3/build/logs/langdex-receiver-c2.log","m3/build/logs/langdex-receiver-int.log","m3/build/logs/langdex-receiver-jit.log","m3/build/logs/langdex-receiver-nocompact.log","m3/build/logs/langdex-source-envelope-c2.log","m3/build/logs/langdex-source-envelope-int.log","m3/build/logs/langdex-source-envelope-jit.log","m3/build/logs/langdex-source-envelope-nocompact.log","m3/build/logs/number-contract-c2.log","m3/build/logs/number-contract-int.log","m3/build/logs/number-contract-jit.log","m3/build/logs/number-contract-nocompact.log","m3/build/logs/number-space-c2.log","m3/build/logs/number-space-int.log","m3/build/logs/number-space-jit.log","m3/build/logs/number-space-nocompact.log","m3/build/logs/synexia-family-admission-c2.log","m3/build/logs/synexia-family-admission-int.log","m3/build/logs/synexia-family-admission-jit.log","m3/build/logs/synexia-family-admission-nocompact.log","m3/build/logs/synexia-family-contract-c2.log","m3/build/logs/synexia-family-contract-int.log","m3/build/logs/synexia-family-contract-jit.log","m3/build/logs/synexia-family-contract-nocompact.log","m3/build/logs/synexia-family-sidecar-c2.log","m3/build/logs/synexia-family-sidecar-int.log","m3/build/logs/synexia-family-sidecar-jit.log","m3/build/logs/synexia-family-sidecar-nocompact.log","m3/build/logs/synexia-payload-c2.log","m3/build/logs/synexia-payload-int.log","m3/build/logs/synexia-payload-jit.log","m3/build/logs/synexia-payload-nocompact.log","m3/build/logs/synexia-receiver-c2.log","m3/build/logs/synexia-receiver-int.log","m3/build/logs/synexia-receiver-jit.log","m3/build/logs/synexia-receiver-nocompact.log","m3/build/logs/synexia-si-unit-c2.log","m3/build/logs/synexia-si-unit-int.log","m3/build/logs/synexia-si-unit-jit.log","m3/build/logs/synexia-si-unit-nocompact.log","m3/build/logs/test-c2.log","m3/build/logs/test-int.log","m3/build/logs/test-jit.log","m3/build/logs/test-nocompact.log","m3/build/logs/translation-receiver-map-c2.log","m3/build/logs/translation-receiver-map-int.log","m3/build/logs/translation-receiver-map-jit.log","m3/build/logs/translation-receiver-map-nocompact.log","m3/build/logs/typed-precompute-c2.log","m3/build/logs/typed-precompute-int.log","m3/build/logs/typed-precompute-jit.log","m3/build/logs/typed-precompute-nocompact.log"],"classes":["com/m3/text/JoinedM3StringPiece.class","com/m3/text/LexiconTool.class","com/m3/text/LocalM3Arena.class","com/m3/text/LocalM3StringPiece$Encoding.class","com/m3/text/LocalM3StringPiece.class","com/m3/text/M3BigString$ChunkSlice.class","com/m3/text/M3BigString$IntView.class","com/m3/text/M3BigString$Storage.class","com/m3/text/M3BigString$Summary.class","com/m3/text/M3BigString$SummaryAccumulator.class","com/m3/text/M3BigString.class","com/m3/text/M3CorpusProjectionDigest.class","com/m3/text/M3CorpusSourceReceipt.class","com/m3/text/M3FixedAddressMapping$Image.class","com/m3/text/M3FixedAddressMapping.class","com/m3/text/M3GvasLiteralImage.class","com/m3/text/M3InstanceIndexPrecompute$InstanceRecord.class","com/m3/text/M3InstanceIndexPrecompute$InstanceType.class","com/m3/text/M3InstanceIndexPrecompute.class","com/m3/text/M3LanguageCorpusManifest$Artifact.class","com/m3/text/M3LanguageCorpusManifest$Plane.class","com/m3/text/M3LanguageCorpusManifest.class","com/m3/text/M3LanguageGrammarSupport.class","com/m3/text/M3LexiconPrecompute$IndexWordFacts.class","com/m3/text/M3LexiconPrecompute$IndexWordSignal.class","com/m3/text/M3LexiconPrecompute$PrefixCounts.class","com/m3/text/M3LexiconPrecompute$RangeFingerprint.class","com/m3/text/M3LexiconPrecompute$SiUnitPrecompute.class","com/m3/text/M3LexiconPrecompute$SpellIndex.class","com/m3/text/M3LexiconPrecompute$TokenFrequency.class","com/m3/text/M3LexiconPrecompute$TokenHashPrecompute.class","com/m3/text/M3LexiconPrecompute$TranslationProjection.class","com/m3/text/M3LexiconPrecompute.class","com/m3/text/M3NumberSpace.class","com/m3/text/M3RopeTextIndex$Window.class","com/m3/text/M3RopeTextIndex.class","com/m3/text/M3StringPiece.class","com/m3/text/M3WordRows$Cursor.class","com/m3/text/M3WordRows.class","com/m3/text/M3WordRowsNative.class","com/m3/text/SharedLexiconCatalog$Coordinate.class","com/m3/text/SharedLexiconCatalog$PrecomputeFacts.class","com/m3/text/SharedLexiconCatalog$PrecomputeProfile.class","com/m3/text/SharedLexiconCatalog$ShardSlot.class","com/m3/text/SharedLexiconCatalog$SourceIdentity.class","com/m3/text/SharedLexiconCatalog$SourceMapping.class","com/m3/text/SharedLexiconCatalog.class","com/m3/text/SharedLexiconFamilySidecarCatalog$FrequencyEntry.class","com/m3/text/SharedLexiconFamilySidecarCatalog$FrequencyKey.class","com/m3/text/SharedLexiconFamilySidecarCatalog$IndexEntry.class","com/m3/text/SharedLexiconFamilySidecarCatalog$PrefixEntry.class","com/m3/text/SharedLexiconFamilySidecarCatalog$PrefixKey.class","com/m3/text/SharedLexiconFamilySidecarCatalog$SourceScope.class","com/m3/text/SharedLexiconFamilySidecarCatalog$SpellAccumulator.class","com/m3/text/SharedLexiconFamilySidecarCatalog$SpellEntry.class","com/m3/text/SharedLexiconFamilySidecarCatalog$SpellKey.class","com/m3/text/SharedLexiconFamilySidecarCatalog$TokenHashEntry.class","com/m3/text/SharedLexiconFamilySidecarCatalog$TokenHashKey.class","com/m3/text/SharedLexiconFamilySidecarCatalog$TranslationEntry.class","com/m3/text/SharedLexiconFamilySidecarCatalog$TranslationKey.class","com/m3/text/SharedLexiconFamilySidecarCatalog.class","com/m3/text/SharedLexiconImage.class","com/m3/text/SharedLexiconPrecomputeCatalog$Builder.class","com/m3/text/SharedLexiconPrecomputeCatalog$Coordinate.class","com/m3/text/SharedLexiconPrecomputeCatalog$SpellScope.class","com/m3/text/SharedLexiconPrecomputeCatalog$TokenRange.class","com/m3/text/SharedLexiconPrecomputeCatalog$TranslationIdentity.class","com/m3/text/SharedLexiconPrecomputeCatalog$ValueScope.class","com/m3/text/SharedLexiconPrecomputeCatalog$ValueToken.class","com/m3/text/SharedLexiconPrecomputeCatalog$WordIdentity.class","com/m3/text/SharedLexiconPrecomputeCatalog.class","com/m3/text/SharedRelatedLexemeCatalog$Mapping.class","com/m3/text/SharedRelatedLexemeCatalog$Relation.class","com/m3/text/SharedRelatedLexemeCatalog.class","com/m3/text/StorageIdentity.class","com/m3/text/SynexiaLangDexSourceEnvelope$Receipt.class","com/m3/text/SynexiaLangDexSourceEnvelope.class","com/m3/text/SynexiaPrecomputePayload$Expected.class","com/m3/text/SynexiaPrecomputePayload$Field.class","com/m3/text/SynexiaPrecomputePayload$Kind.class","com/m3/text/SynexiaPrecomputePayload$Parser.class","com/m3/text/SynexiaPrecomputePayload.class","module-info.class"],"triplets":[["original-product","qualified-range","fresh-owned"],["fresh-owned","qualified-range","original-product"],["qualified-range","fresh-owned","original-product"],["original-product","fresh-owned","qualified-range"],["fresh-owned","original-product","qualified-range"],["qualified-range","original-product","fresh-owned"]],"cohorts":[{"name":"original-product","gitBlob":"cc3c84f5ea0230e2f57e3d3ae0f5d29b9241e597","sha256":"694ee6a9fa06803244fd56d7a846da16f1560d242a0660ae2f4a927c43fc3dcb","bytes":2634},{"name":"qualified-range","gitBlob":"3edc23c22362b08af00f7332ddc445a0274a6dd4","sha256":"10ddd082c142420c29985a85d8b2f77507fdbcde2552b8bfc154e658f3b2a9e3","bytes":2919},{"name":"fresh-owned","gitBlob":"4f88f794266375ad8174fa56e952c30d32689877","sha256":"d69ef5cc0e4bd5d95808978b4a0d73982322230a623d897074840b58fab1993e","bytes":3377}],"javaFixture":{"schema":"M3_JOINED_FRESH_OWNERSHIP_FIXTURE_V1","javaRelease":21,"compiledVariants":4,"semanticCohorts":2,"rejectedMutants":2,"qualifiedBeforeSha256":"10ddd082c142420c29985a85d8b2f77507fdbcde2552b8bfc154e658f3b2a9e3","afterSha256":"d69ef5cc0e4bd5d95808978b4a0d73982322230a623d897074840b58fab1993e","proofSummary":"JOINED_FRESH_RANGE_PASS fixtures=5 ranges=466 nested=466 freshResults=1864 invalidRanges=25 indexRefusals=3738 aliasMutations=3"},"donors":{"workflow":"e38f1340062246a355ac0e6080706a008593eecd","hosted":"677ecb2bcd6811862922990261037bb7648d33ea","runtime":"214017285242d75cb78b7fb5392243fb2d5b018e","buildInputs":"921d708e2a0f1f1c43e95989ea9e37e02e22790a","proof":"833b13b220110b6942277c2b8b8a4fd78ce9e285"},"limits":{"artifactMembers":64,"artifactBytes":8388608,"taskBytes":536870912,"reserveBytes":8589934592,"seconds":1500},"runtimeRawRecords":112,"buildRawRecords":198,"hostRawRecords":10,"processRecordKeys":["name","argv","cwd","timeoutSeconds","returncode","parentReaped","log","logIdentity"]}''')
HERE = Path(__file__).resolve().parent
PARENT = HERE.parent
SOURCE = Path(os.environ["M3_SOURCE_ROOT"]).resolve()
PROOF = Path(os.environ["M3_PROOF_ROOT"]).resolve()
ROOT = PROOF / "joined-fresh-owned"
OUTER = ROOT / "outer"
RUNTIME = ROOT / "runtime"
SDK = HERE / "sdk"
ARTIFACTS = PROOF / "artifacts"
PREFIX = CFG["publicPrefix"]
WORKFLOW = ".github/workflows/m3-joined-range-directory.yml"
TARGET = "m3/core/src/com/m3/text/JoinedM3StringPiece.java"
SUITE = "com.synexia.rewrite.M3JoinedRangeDirectoryRecipeTest"
METHODS = ["allTwoPartialStatesBothOrdersReachExactFixedPoint",
           "allTwoInverseStatesBothOrdersRecoverExactBefore",
           "eachOwnedInputRefusesDriftMissingAndDuplicateOwnership"]
FRESH_PHASES = [p for p in CFG["phases"] if p.startswith("fresh-")]
FALSE_GATES = ("targetMutationByQualificationJob", "nativeCodeChanged", "fullMatchedJdkQualified",
               "stringDispatchQualified", "performanceRetentionQualified", "phaseAdvanced",
               "defaultPromotion")
RAW_KEYS = {"schema", "path", "bytes", "gitBlob", "sha256", "encoding", "content"}


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def unique(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "duplicate JSON key:" + key)
        result[key] = value
    return result


def parse(raw):
    return json.loads(raw, object_pairs_hook=unique)


def regular(path):
    require(path.is_file() and all(not p.is_symlink() for p in (path, *path.parents)),
            "missing or redirected input:" + str(path))
    return path


def relative(value):
    require(isinstance(value, str) and value and not any(ord(c) < 32 or ord(c) == 127 for c in value),
            "unsafe source path")
    path = PurePosixPath(value)
    require(not path.is_absolute() and "\\" not in value and ":" not in value
            and all(p not in ("", ".", "..") for p in value.split("/")), "unsafe source path")
    return path


def ident(raw):
    return {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(),
            "gitBlob": hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()}


def read(path):
    return regular(path).read_bytes()


def git(*args):
    return subprocess.check_output(["git", *args], cwd=SOURCE, timeout=60).decode("utf-8").strip()


def context():
    head, task = os.environ["EXPECTED_SOURCE_SHA"], os.environ["M3_QUALIFICATION_ID"]
    require(re.fullmatch(r"[0-9a-f]{40}", head) is not None and git("rev-parse", "HEAD") == head,
            "exact candidate checkout required")
    require(re.fullmatch(r"[A-Za-z0-9_.-]{16,128}", task) is not None, "exact qualification id required")
    for path in (SOURCE, PROOF, ROOT, OUTER):
        require(path.is_dir() and all(not p.is_symlink() for p in (path, *path.parents)),
                "explicit unredirected owned roots required")
    return head, task


def save(path, value):
    require(not path.exists() and not path.is_symlink(), "fresh receipt required:" + str(path))
    require(path.parent.is_dir() and all(not p.is_symlink() for p in (path.parent, *path.parent.parents)),
            "unredirected output directory required")
    raw = (json.dumps(value, indent=2, allow_nan=False) + "\n").encode("utf-8")
    require(len(raw) <= 2*1024**2, "bounded receipt required")
    with path.open("xb") as stream:
        stream.write(raw)


def sdk_module():
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_fresh_owned_sdk_inputs")
    module["inputs"]()
    return module


def legacy_module():
    require(ident(read(PARENT / "qualify.py"))["gitBlob"] == CFG["donors"]["hosted"],
            "frozen hosted qualifier drift")
    require(ident(read(PARENT / "runtime/qualify.py"))["gitBlob"] == CFG["donors"]["runtime"],
            "frozen runtime qualifier drift")
    return runpy.run_path(str(regular(PARENT / "qualify.py")), run_name="m3_fresh_legacy_binding")


def check_legacy_binding():
    actual = parse(read(PARENT / "source-binding.json"))
    frozen = CFG["legacyBinding"]
    require(set(actual) == set(frozen) and re.fullmatch(r"[0-9a-f]{40}", actual["sourceCommit"]) is not None,
            "legacy binding schema/source identity drift")
    for key in frozen.keys() - {"files", "sourceCommit"}:
        require(actual[key] == frozen[key], "legacy binding metadata drift:" + key)
    require(len(actual["files"]) == 39 and len({r["targetPath"] for r in actual["files"]}) == 39,
            "legacy39 row closure required")
    by_path = {r["targetPath"]: r for r in actual["files"]}
    require(set(by_path) == {r["targetPath"] for r in frozen["files"]}, "legacy path set drift")
    for row in frozen["files"]:
        current = by_path[row["targetPath"]]
        if row["targetPath"] == WORKFLOW:
            require(set(current) == set(row) and current["targetPath"] == WORKFLOW,
                    "workflow row schema drift")
        else:
            require(current == row, "frozen legacy row drift:" + row["targetPath"])
    return actual


def binding(fresh=False):
    head, task = context()
    check_legacy_binding()
    parent = legacy_module()["binding"](fresh=False)
    raw = read(HERE / "source-binding.json")
    record = parse(raw)
    require(git("rev-parse", "HEAD:" + PREFIX + "source-binding.json") == ident(raw)["gitBlob"],
            "child source binding differs from checkout")
    require(record["schema"] == "M3_JOINED_FRESH_OWNED_SOURCE_BINDING_V1"
            and record["repository"] == "hsoliwal/com.synexia"
            and record["receiverRepository"] == "hsoliwal/M3jdk21"
            and record["admissionCommit"] == CFG["admissionCommit"]
            and record["sdkTree"] == CFG["sdkTree"] and record["sdkFiles"] == 35
            and record["artifactMembers"] == 64 and record["runtimeApplied"] is False
            and record["phaseAdvanced"] is False
            and re.fullmatch(r"[0-9a-f]{40}", record["sourceCommit"]) is not None,
            "exact admitted child binding required")
    pins = {PREFIX + "sdk/" + row["path"]: row for row in CFG["sdkPins"]["files"]}
    pins[PREFIX + "sdk/pins.json"] = CFG["sdkPinsIdentity"]
    wanted = set(pins) | {PREFIX + name for name in
              ("qualify.py", "M3JoinedFreshRangeWorkload.java", "QUALIFICATION_PLAN.json", "SOURCE_STATUS.json")}
    wanted |= {PARENT.relative_to(SOURCE).as_posix() + "/source-binding.json", WORKFLOW}
    require(len(wanted) == 41 and len(record["files"]) == 41, "complete41-row child binding required")
    seen = set()
    for row in record["files"]:
        require(set(row) == {"sourcePath", "targetPath", "gitBlob", "bytes", "sha256"},
                "exact source tuple schema")
        path = str(relative(row["targetPath"]))
        relative(row["sourcePath"])
        require(path in wanted and path not in seen, "missing/extra/duplicate child source row")
        seen.add(path)
        body = read(SOURCE / path)
        expected = {k: row[k] for k in ("bytes", "sha256", "gitBlob")}
        require(ident(body) == expected and git("rev-parse", "HEAD:" + path) == row["gitBlob"],
                "child source bytes differ:" + path)
        if path in pins:
            require(expected == {k: pins[path][k] for k in expected}, "frozen new SDK pin drift")
    require(seen == wanted and git("rev-parse", "HEAD:" + PREFIX + "sdk") == CFG["sdkTree"],
            "complete frozen35 SDK tree required")
    tracked = set(subprocess.check_output(["git", "ls-files", "-z", "--", PREFIX + "sdk/"],
                     cwd=SOURCE, timeout=60).decode("utf-8").split("\0")) - {""}
    require(tracked == set(pins), "complete tracked SDK source closure required")
    for name, blob in (("QUALIFICATION_PLAN.json", CFG["planGit"]),
                       ("SOURCE_STATUS.json", CFG["statusGit"]),
                       ("M3JoinedFreshRangeWorkload.java", CFG["workloadGit"])):
        require(ident(read(HERE / name))["gitBlob"] == blob, "frozen plan/status/workload drift")
    module = sdk_module()
    require(module["SUITES"] == {SUITE: METHODS}, "exact new named methods required")
    plan = parse(read(HERE / "QUALIFICATION_PLAN.json"))
    require(plan["measurements"]["counterbalancedForkBlocks"] == CFG["triplets"]
            and [{k: r[k] for k in ("name", "gitBlob", "sha256", "bytes")} for r in plan["cohorts"]]
                == CFG["cohorts"], "three distinct admitted cohorts and six blocks required")
    if fresh:
        require(not (SDK / "target").exists() and not RUNTIME.exists()
                and not (ROOT / "binding-receipt.json").exists(), "new SDK/runtime outputs must start absent")
    return {"schema": "M3_JOINED_FRESH_BINDING_RECEIPT_V1", "checkoutCommit": head,
            "qualificationId": task, "canonicalSourceCommit": record["sourceCommit"],
            "sourceBindingIdentity": ident(raw), "boundFiles": 41, "sdkFiles": 35,
            "observedCurrentOwners": parent["observedCurrentOwners"],
            "observedBuildInputs": parent["observedBuildInputs"],
            **{key: False for key in FALSE_GATES}}


def sdk_evidence():
    head, task = context()
    module = sdk_module()
    pins, pins_raw = module["inputs"]()
    directory = SDK / "target/surefire-reports"
    require(sorted(p.name for p in directory.glob("TEST-*.xml")) == ["TEST-" + SUITE + ".xml"],
            "one exact new Surefire XML required")
    xml = read(directory / ("TEST-" + SUITE + ".xml"))
    counts = module["validate_report"](xml, SUITE, METHODS)
    module["validate_marker"](xml, task)
    counter_raw = read(SDK / "target/authoring-recipe-test-counters.json")
    module["validate_counters"](parse(counter_raw), task, head)
    java_raw = read(SDK / "target/fresh-owned-java-execution.json")
    java = parse(java_raw)
    require(java == CFG["javaFixture"], "actual four-variant/two-mutant ownership proof required")
    receipt = parse(read(SDK / "target/authoring-sdk-receipt.json"))
    require(receipt["schema"] == "M3_JOINED_FRESH_OWNED_RANGE_SDK_RECEIPT_V1"
            and receipt["scope"] == "SOURCE_ONLY_JOINED_FRESH_OWNED_RANGE_REPLAY"
            and receipt["checkoutCommit"] == head and receipt["qualificationId"] == task
            and receipt["inputPinsSha256"] == ident(pins_raw)["sha256"]
            and receipt["providers"] == pins["providers"]
            and (receipt["tests"], receipt["successfulRuns"], receipt["refusedRuns"], receipt["emittedOutputs"])
                == (3, 16, 30, 2)
            and receipt["recipeJUnitQualified"] is True and receipt["recipeReplayQualified"] is True
            and receipt["actualEmittedFreshOwnershipFixtureQualified"] is True
            and receipt["freshOwnershipFixture"] ==
                {"path": "target/fresh-owned-java-execution.json", "sha256": ident(java_raw)["sha256"], **java}
            and receipt["reports"] == {SUITE: {"counts": counts, "sha256": ident(xml)["sha256"]}}
            and receipt["counterReceipt"] == {"path": "target/authoring-recipe-test-counters.json",
                                               "bytes": len(counter_raw), "sha256": ident(counter_raw)["sha256"]},
            "actual new SDK receipt projection differs")
    for flag in module["FALSE_GATES"]:
        require(receipt[flag] is False, "new SDK scope drift:" + flag)
    for name in module["COMPILED_PROVIDERS"]:
        raw = read(SDK / ("target/classes/com/synexia/rewrite/" + name + ".class"))
        item = receipt["compiledProviders"][name]
        require(raw[:4] == bytes.fromhex("cafebabe") and len(raw) > 8
                and int.from_bytes(raw[6:8], "big") == 65 and item ==
                {"path": "target/classes/com/synexia/rewrite/" + name + ".class",
                 "bytes": len(raw), "sha256": ident(raw)["sha256"], "majorVersion": 65},
                "actual new compiled provider identity differs")
    require(set(receipt["compiledProviders"]) == set(module["COMPILED_PROVIDERS"]),
            "complete four compiled providers required")
    expected_custody = {row["path"]: row["sha256"] for row in CFG["legacyBinding"]["receiverOriginalOwners"]
                        if row["path"] != TARGET}
    require(receipt["unchangedReceiverCustody"] == expected_custody
            and receipt["emittedProductSourceOutputs"] == module["RECEIVER_OUTPUTS"],
            "exact emitted/custody SDK projection differs")
    emissions = {}
    for direction, cohort in (("inverse", CFG["cohorts"][1]), ("forward", CFG["cohorts"][2])):
        raw = read(SDK / "target/emitted" / direction / TARGET)
        require(ident(raw) == {k: cohort[k] for k in ("bytes", "sha256", "gitBlob")}
                and raw == read(SDK / module["RESOURCE"] / direction / "out" / TARGET),
                "actual fresh directional source differs")
        emissions[direction] = ident(raw)
    require(read(PARENT / "sdk/target/emitted/forward" / TARGET)
            == read(SDK / "target/emitted/inverse" / TARGET), "old3edc/new3edc emission bridge differs")
    require("[INFO] BUILD SUCCESS" in read(OUTER / "fresh-sdk-maven.log").decode("utf-8"),
            "actual new Maven success required")
    return receipt, emissions


def check_legacy_projection():
    expected = parse(read(ROOT / "legacy-projection.json"))
    require(set(expected) == set(CFG["legacyMembers"]) and len(expected) == 47, "legacy47 projection required")
    for name, item in expected.items():
        require(ident(read(ARTIFACTS / str(relative(name)))) == item, "legacy evidence changed:" + name)
    return expected


def freeze_legacy():
    context()
    require(sorted(p.name for p in ARTIFACTS.iterdir()) == sorted(CFG["legacyMembers"]),
            "legacy artifact must retain exact47-member census")
    inventory = parse(read(ARTIFACTS / "inventory.json"))
    require(set(inventory) == set(CFG["legacyMembers"]) - {"inventory.json"} and len(inventory) == 46,
            "legacy46-row inventory required")
    for name, item in inventory.items():
        raw = read(ARTIFACTS / str(relative(name)))
        require(item["bytes"] == len(raw) and item["sha256"] == ident(raw)["sha256"],
                "legacy raw inventory identity differs")
    commands = parse(read(ARTIFACTS / "commands.json"))
    require([r["name"] for r in commands] == [p for p in CFG["phases"] if not p.startswith("fresh-")],
            "legacy seven outer phases changed")
    require(all(r["returncode"] == 0 and r["parentReaped"] is True and r["ownedSessionAbsent"] is True
                for r in commands), "legacy command lifecycle incomplete")
    save(ROOT / "legacy-projection.json", {name: ident(read(ARTIFACTS / name)) for name in CFG["legacyMembers"]})


def diagnostic(raw, cohort, block, mode, leaves):
    rows = [parse(line) for line in raw.decode("utf-8").splitlines() if line.startswith("{")]
    require(len(rows) == 1, "one actual diagnostic row required")
    row = rows[0]
    require(set(row) == {"schema", "mode", "leaves", "iterations", "warmup", "elapsedNanos",
                        "allocatedBytes", "checksum"} and row["schema"] == "M3_JOINED_FRESH_RANGE_DIAGNOSTIC_V1",
            "exact diagnostic schema required")
    require(all(type(row[k]) is int for k in ("leaves", "iterations", "warmup", "elapsedNanos", "allocatedBytes", "checksum"))
            and row["mode"] == mode and row["leaves"] == leaves and row["iterations"] == 1024
            and row["warmup"] == 128 and row["elapsedNanos"] > 0 and row["allocatedBytes"] >= 0,
            "diagnostic context/counters drift")
    def unit(at):
        return 0xd800 if at % 7 == 0 else 0xdc00 if at % 7 == 1 else at % 256
    expected = 0 if mode == "construction" else (leaves + unit(0) + unit(leaves - 1))*1024
    require(row["checksum"] == expected, "independent diagnostic endpoint checksum differs")
    return {**row, "cohort": cohort, "block": block}


def summarize(rows, keys):
    output = []
    for coordinates in sorted({tuple(row[k] for k in keys) for row in rows}):
        group = [r for r in rows if tuple(r[k] for k in keys) == coordinates]
        cohorts = {}
        for cohort in [r["name"] for r in CFG["cohorts"]]:
            selected = [r for r in group if r["cohort"] == cohort]
            require(len(selected) == 6 and {r["block"] for r in selected} == set(range(6)),
                    "complete fresh six-block measurement closure required")
            cohorts[cohort] = {str(r["block"]): r for r in selected}
        contrasts = []
        for left, right in itertools.combinations(cohorts, 2):
            values = {}
            for metric in ("elapsedNanos", "allocatedBytes"):
                a = [cohorts[left][str(i)][metric] for i in range(6)]
                b = [cohorts[right][str(i)][metric] for i in range(6)]
                paired = [x/y if y else None for x, y in zip(a, b)]
                denominator = statistics.median(b)
                values[metric] = {"leftMedian": statistics.median(a), "rightMedian": denominator,
                    "ratioOfMedians": statistics.median(a)/denominator if denominator else None,
                    "pairedRatiosByBlock": paired,
                    "medianPairedRatio": statistics.median(paired) if all(v is not None for v in paired) else None,
                    "undefinedZeroDenominators": sum(y == 0 for y in b),
                    "leftMin": min(a), "leftMax": max(a), "rightMin": min(b), "rightMax": max(b)}
            contrasts.append({"numeratorCohort": left, "denominatorCohort": right, **values})
        output.append({**dict(zip(keys, coordinates)), "contrasts": contrasts})
    return output


def command_plan():
    java_home = Path(os.environ["JAVA_HOME"])
    java, javac = str(java_home / "bin/java"), str(java_home / "bin/javac")
    modes = {"jit": [], "int": ["-Xint"], "nocompact": ["-XX:-CompactStrings"],
             "c2": ["-Xbatch", "-XX:-TieredCompilation"]}
    result = []
    bases = {}
    def add(name, argv, cwd, timeout=300):
        result.append({"name": name, "argv": argv, "cwd": str(cwd), "timeoutSeconds": timeout})
    for cohort in CFG["cohorts"]:
        name = cohort["name"]
        directory = RUNTIME / name
        jar, tests = directory / "m3/build/com.m3.text.jar", directory / "range-proof-classes"
        add(name + "-original-build", ["bash", str(directory / "m3/build.sh")], directory, 600)
        add(name + "-range-compile", [javac, "--release", "21", "-Xlint:all", "-Werror",
            "--module-path", str(jar), "--add-modules", "com.m3.text,jdk.management", "-d", str(tests),
            str(PARENT / "runtime/M3JoinedRangeProof.java"), str(HERE / "M3JoinedFreshRangeWorkload.java")], directory)
        base = [java, "-ea", "-esa", "--module-path", str(jar), "--add-modules", "com.m3.text,jdk.management",
                "--add-opens", "com.m3.text/com.m3.text=ALL-UNNAMED", "-cp", str(tests), "M3JoinedRangeProof"]
        bases[name] = base
        for mode, flags in modes.items():
            add(name + "-range-" + mode, [java, *flags, *base[1:], "contract"], directory, 120)
    for block, order in enumerate(CFG["triplets"]):
        for name in order:
            add(name + "-benchmark-" + str(block), [*bases[name], "benchmark", str(block)], RUNTIME / name, 180)
    for mode in ("construction", "endpoints"):
        for leaves in (1024, 8192):
            for block, order in enumerate(CFG["triplets"]):
                for name in order:
                    add(name + "-diagnostic-" + mode + "-" + str(leaves) + "-" + str(block),
                        [*bases[name][:-1], "M3JoinedFreshRangeWorkload", mode, str(leaves)], RUNTIME / name, 180)
    require(len(result) == 108 and len({r["name"] for r in result}) == 108, "complete108 exact argv plan required")
    return result


def runtime():
    head, task = context()
    require(not RUNTIME.exists(), "fresh runtime root required")
    check_legacy_projection()
    sdk_evidence()
    first = parse(read(ROOT / "binding-receipt.json"))
    require(first == binding(), "bound sources changed before fresh runtime")
    RUNTIME.mkdir()
    donor = runpy.run_path(str(regular(PARENT / "runtime/qualify.py")), run_name="m3_fresh_frozen_runtime_helpers")
    globals_ = donor["run"].__globals__
    globals_["ROOT"] = RUNTIME
    globals_["records"] = []
    globals_["failures"] = []
    donor_run = donor["run"]
    commands = []
    planned = command_plan()
    def run(name, argv, cwd, timeout=300):
        require(len(commands) < len(planned) and planned[len(commands)] ==
                {"name": name, "argv": argv, "cwd": str(cwd), "timeoutSeconds": timeout},
                "actual command differs from exact admitted argv/order")
        code, log = donor_run(name, argv, cwd, timeout=timeout)
        original = globals_["records"][-1]
        require(original["returncode"] == 0 and original["parentReaped"] is True, "actual inner command must succeed")
        commands.append({**original, "cwd": str(cwd), "timeoutSeconds": timeout})
        return code, log
    globals_["run"] = run
    metadata_raw = read(PARENT / "runtime/BUILD_INPUTS.json")
    require(ident(metadata_raw)["gitBlob"] == CFG["donors"]["buildInputs"], "literal original58 model changed")
    metadata = parse(metadata_raw)
    require(len(metadata["files"]) == 58 and len(metadata["testMainClasses"]) == 16
            and metadata["commit"] == "73f59218dc4ef1a3ef79aad990e77273db2a2d60"
            and metadata["modes"] == list(donor["MODES"]), "original build schema/census drift")
    source_values = {}
    for row in metadata["files"]:
        name = str(relative(row["path"]))
        require(name not in source_values, "duplicate original build input")
        body = read(SOURCE / name)
        if name != TARGET:
            require(ident(body) == {k: row[k] for k in ("bytes", "sha256", "gitBlob")}, "original57 input drift")
        else:
            require(ident(body)["gitBlob"] in {r["gitBlob"] for r in CFG["cohorts"][:2]},
                    "physical owner must retain original or qualified state")
        source_values[name] = body
    require(len(source_values) == 58 and TARGET in source_values, "complete original58 input set required")
    owners = {
        "original-product": read(PARENT / "sdk/target/emitted/inverse" / TARGET),
        "qualified-range": read(SDK / "target/emitted/inverse" / TARGET),
        "fresh-owned": read(SDK / "target/emitted/forward" / TARGET)}
    for row in CFG["cohorts"]:
        require(ident(owners[row["name"]]) == {k: row[k] for k in ("bytes", "sha256", "gitBlob")},
                "three distinct cohort source identities required")
    java_home = Path(os.environ["JAVA_HOME"])
    java, javac = str(regular(java_home / "bin/java")), str(regular(java_home / "bin/javac"))
    os.environ["M3_JDK"] = str(java_home)
    require(ident(read(PARENT / "runtime/M3JoinedRangeProof.java"))["gitBlob"] == CFG["donors"]["proof"],
            "unchanged original118/4026 combined proof required")
    legacy = parse(read(ARTIFACTS / "runtime-receipt.json"))
    expected_contract = legacy["cohorts"]["baseline"]["contracts"]["jit"]
    require(expected_contract[:3] == ["118", "4026", "7424"], "legacy complete corpus projection differs")
    cohorts = {}
    for name, owner in owners.items():
        directory = RUNTIME / name
        directory.mkdir()
        for path, body in source_values.items():
            destination = directory / path
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(owner if path == TARGET else body)
        build = donor["original_build"](name, directory, java)
        require(build["returncode"] == 0 and not globals_["failures"]
                and set(build["classes"]) == set(CFG["classes"]) and len(build["classes"]) == 83,
                "all16 original programs/four modes and exact83 classes required")
        bundle = read(RUNTIME / build["completeBuildLogBundle"]["path"])
        rows = [parse(line) for line in bundle.splitlines()]
        require(len(rows) == 66 and {r["path"] for r in rows} == set(CFG["originalLogPaths"]),
                "all66 nested original build logs required")
        for row in rows:
            require(set(row) == {"path", "bytes", "sha256", "gitBlob", "content"}
                    and ident(row["content"].encode("utf-8")) == {k: row[k] for k in ("bytes", "sha256", "gitBlob")},
                    "complete original raw-log identity differs")
        jar = directory / "m3/build/com.m3.text.jar"
        tests = directory / "range-proof-classes"
        argv = [javac, "--release", "21", "-Xlint:all", "-Werror", "--module-path", str(jar),
                "--add-modules", "com.m3.text,jdk.management", "-d", str(tests),
                str(PARENT / "runtime/M3JoinedRangeProof.java"), str(HERE / "M3JoinedFreshRangeWorkload.java")]
        code, log = run(name + "-range-compile", argv, directory)
        base = [java, "-ea", "-esa", "--module-path", str(jar), "--add-modules", "com.m3.text,jdk.management",
                "--add-opens", "com.m3.text/com.m3.text=ALL-UNNAMED", "-cp", str(tests), "M3JoinedRangeProof"]
        contracts = {}
        for mode, flags in donor["MODES"].items():
            code, log = run(name + "-range-" + mode, [java, *flags, *base[1:], "contract"], directory, timeout=120)
            matches = re.findall(r"^JOINED_RANGE_CONTRACT_PASS directories=([1-9][0-9]*) cases=([1-9][0-9]*) backingUnits=([1-9][0-9]*) digest=([0-9a-f]{64})$", read(log).decode("utf-8"), re.M)
            require(code == 0 and len(matches) == 1 and list(matches[0]) == expected_contract,
                    "complete unchanged original range proof must match all cohorts/four modes")
            contracts[mode] = list(matches[0])
        cohorts[name] = {"directory": directory, "argv": base, "build": build, "contracts": contracts}
    for left, right in itertools.combinations(cohorts, 2):
        a, b = cohorts[left]["build"]["classes"], cohorts[right]["build"]["classes"]
        require(set(a) == set(b) == set(CFG["classes"])
                and [p for p in sorted(a) if a[p] != b[p]] == ["com/m3/text/JoinedM3StringPiece.class"],
                "only admitted Joined class may differ across three full modules")
    samples, diagnostics = [], []
    for block, order in enumerate(CFG["triplets"]):
        for name in order:
            cohort = cohorts[name]
            code, log = run(name + "-benchmark-" + str(block), [*cohort["argv"], "benchmark", str(block)],
                            cohort["directory"], timeout=180)
            rows = donor["sample"](read(log), name, block)
            require([(r["leaves"], r["family"]) for r in rows] ==
                    [(n, family) for n in (32, 1024, 8192)
                     for family in ("empty-middle", "one-middle", "one-tail", "two-middle", "full")],
                    "unchanged fifteen-case combined order/full-last required")
            samples.extend({**r, "block": block} for r in rows)
    for mode in ("construction", "endpoints"):
        for leaves in (1024, 8192):
            for block, order in enumerate(CFG["triplets"]):
                for name in order:
                    cohort = cohorts[name]
                    argv = [*cohort["argv"][:-1], "M3JoinedFreshRangeWorkload", mode, str(leaves)]
                    code, log = run(name + "-diagnostic-" + mode + "-" + str(leaves) + "-" + str(block),
                                    argv, cohort["directory"], timeout=180)
                    diagnostics.append(diagnostic(read(log), name, block, mode, leaves))
    for name, cohort in cohorts.items():
        for path, body in source_values.items():
            require(read(cohort["directory"] / path) == (owners[name] if path == TARGET else body),
                    "cohort build changed original source/fixture")
    require(len(commands) == 108 and len(globals_["records"]) == 108
            and len(samples) == 270 and len(diagnostics) == 72 and not globals_["failures"],
            "complete admitted108-command/270-combined/72-diagnostic matrix required")
    receipt = {"schema": "M3_JOINED_FRESH_RUNTIME_RECEIPT_V1", "state": "PASS",
        "checkoutCommit": head, "qualificationId": task, "measurementJvmPolicy": "EXISTING_NORMAL_JIT",
        "environment": {"javaHome": str(java_home), "jvmOptions": os.environ.get("JAVA_TOOL_OPTIONS", ""),
                        "platform": __import__("platform").platform(), "machine": __import__("platform").machine(),
                        "affinity": sorted(os.sched_getaffinity(0)) if hasattr(os, "sched_getaffinity") else None},
        "originalBuildInputs": ident(metadata_raw), "cohortOwners": {k: ident(v) for k, v in owners.items()},
        "cohorts": {name: {"build": value["build"], "contracts": value["contracts"]} for name, value in cohorts.items()},
        "commands": commands, "rawInnerJournal": ident(read(RUNTIME / "inner-commands.jsonl")),
        "samples": samples, "diagnostics": diagnostics,
        "combinedSummary": summarize(samples, ("leaves", "family")),
        "diagnosticSummary": summarize(diagnostics, ("mode", "leaves")),
        "originalTestProgramsPerCohort": 16, "originalJvmModesPerCohort": 4,
        "classesPerCohort": 83, "originalProgramExecutions": 192,
        "rangeContractProcesses": 12, "combinedBenchmarkProcesses": 18, "diagnosticBenchmarkProcesses": 72,
        "allThreeOriginalCoreBuildsQualified": True, "completeRangeCorpusQualified": True,
        "pairedMeasurementsComplete": True, "failures": [], **{key: False for key in FALSE_GATES}}
    save(RUNTIME / "runtime-receipt.json", receipt)
    print("M3_JOINED_FRESH_RUNTIME_PASS")


def verify_runtime():
    head, task = context()
    receipt = parse(read(RUNTIME / "runtime-receipt.json"))
    require(receipt["schema"] == "M3_JOINED_FRESH_RUNTIME_RECEIPT_V1" and receipt["state"] == "PASS"
            and receipt["checkoutCommit"] == head and receipt["qualificationId"] == task
            and receipt["measurementJvmPolicy"] == "EXISTING_NORMAL_JIT"
            and not receipt["failures"] and receipt["allThreeOriginalCoreBuildsQualified"] is True
            and receipt["completeRangeCorpusQualified"] is True and receipt["pairedMeasurementsComplete"] is True
            and receipt["classesPerCohort"] == 83 and receipt["originalTestProgramsPerCohort"] == 16
            and receipt["originalJvmModesPerCohort"] == 4 and receipt["originalProgramExecutions"] == 192
            and receipt["rangeContractProcesses"] == 12 and receipt["combinedBenchmarkProcesses"] == 18
            and receipt["diagnosticBenchmarkProcesses"] == 72, "complete fresh runtime qualification required")
    planned = command_plan()
    require([{k: r[k] for k in ("name", "argv", "cwd", "timeoutSeconds")} for r in receipt["commands"]]
            == planned, "runtime exact command argv/order/deadline drift")
    journal_raw = read(RUNTIME / "inner-commands.jsonl")
    require(receipt["rawInnerJournal"] == ident(journal_raw), "raw inner journal identity drift")
    journal = [parse(line) for line in journal_raw.splitlines()]
    require(len(journal) == 108, "complete raw108-command inner journal required")
    for command, original in zip(receipt["commands"], journal):
        require(set(command) == set(CFG["processRecordKeys"])
                and {k: command[k] for k in original} == original and command["returncode"] == 0
                and command["parentReaped"] is True
                and ident(read(RUNTIME / str(relative(command["log"])))) == command["logIdentity"],
                "raw runtime command/log/lifecycle differs")
        require(command["cwd"] == str(RUNTIME / next(n for n in [r["name"] for r in CFG["cohorts"]]
                    if command["name"].startswith(n + "-")))
                and type(command["timeoutSeconds"]) is int and 0 < command["timeoutSeconds"] <= 600,
                "runtime cwd/deadline differs")
    helper = runpy.run_path(str(regular(PARENT / "runtime/qualify.py")), run_name="m3_fresh_raw_replay_helpers")
    samples, diagnostics = [], []
    for block, order in enumerate(CFG["triplets"]):
        for cohort in order:
            rows = helper["sample"](read(RUNTIME / (cohort + "-benchmark-" + str(block) + ".log")), cohort, block)
            require([(r["leaves"], r["family"]) for r in rows] ==
                    [(n, family) for n in (32, 1024, 8192)
                     for family in ("empty-middle", "one-middle", "one-tail", "two-middle", "full")],
                    "combined raw case/full-last order differs")
            samples.extend({**row, "block": block} for row in rows)
    for mode in ("construction", "endpoints"):
        for leaves in (1024, 8192):
            for block, order in enumerate(CFG["triplets"]):
                for cohort in order:
                    name = cohort + "-diagnostic-" + mode + "-" + str(leaves) + "-" + str(block) + ".log"
                    diagnostics.append(diagnostic(read(RUNTIME / name), cohort, block, mode, leaves))
    require(len(samples) == 270 and len(diagnostics) == 72
            and receipt["samples"] == samples and receipt["diagnostics"] == diagnostics
            and receipt["combinedSummary"] == summarize(samples, ("leaves", "family"))
            and receipt["diagnosticSummary"] == summarize(diagnostics, ("mode", "leaves")),
            "fresh actual raw sample/summary replay differs")
    require(set(receipt["cohorts"]) == {c["name"] for c in CFG["cohorts"]}
            and receipt["cohortOwners"] == {c["name"]: {k: c[k] for k in ("bytes", "sha256", "gitBlob")} for c in CFG["cohorts"]},
            "all three exact cohort owners required")
    metadata_raw = read(PARENT / "runtime/BUILD_INPUTS.json")
    require(ident(metadata_raw)["gitBlob"] == CFG["donors"]["buildInputs"]
            and receipt["originalBuildInputs"] == ident(metadata_raw), "literal original58 model identity differs")
    metadata = parse(metadata_raw)
    legacy = parse(read(ARTIFACTS / "runtime-receipt.json"))
    expected_contract = legacy["cohorts"]["baseline"]["contracts"]["jit"]
    observed_class_maps = {}
    for cohort in CFG["cohorts"]:
        name = cohort["name"]
        directory = RUNTIME / name
        stored = receipt["cohorts"][name]
        build = stored["build"]
        raw = read(RUNTIME / (name + "-original-build.log"))
        require(build["returncode"] == 0 and build["logIdentity"] == ident(raw)
                and build["jar"] == ident(read(directory / "m3/build/com.m3.text.jar")),
                "actual original build command/jar identity differs")
        counts = {}
        for contract_marker in helper["MARKERS"]:
            matched = [line for line in raw.decode("utf-8").splitlines()
                       if re.match(re.escape(contract_marker) + r" checks=[1-9][0-9]*(?:\s|$)", line)]
            require(len(matched) == 4, "all16 original program markers in all four modes required")
            counts[contract_marker] = matched
        require(build["contracts"] == counts, "raw original program marker projection differs")
        class_root = directory / "m3/build/classes"
        actual_classes = {}
        for path in sorted(class_root.rglob("*.class")):
            body = read(path)
            require(body[:4] == bytes.fromhex("cafebabe") and len(body) > 8
                    and int.from_bytes(body[6:8], "big") == 65, "actual Java21 class required")
            actual_classes[path.relative_to(class_root).as_posix()] = ident(body)
        require(set(actual_classes) == set(CFG["classes"]) and len(actual_classes) == 83
                and actual_classes == build["classes"], "exact83 actual compiled class projection differs")
        observed_class_maps[name] = actual_classes
        owner = read(SDK / "target/emitted/forward" / TARGET) if name == "fresh-owned" else read(
            SDK / "target/emitted/inverse" / TARGET) if name == "qualified-range" else read(
            PARENT / "sdk/target/emitted/inverse" / TARGET)
        require(ident(owner) == {k: cohort[k] for k in ("bytes", "sha256", "gitBlob")}, "exact actual cohort owner differs")
        for row in metadata["files"]:
            path = str(relative(row["path"]))
            expected = owner if path == TARGET else read(SOURCE / path)
            require(read(directory / path) == expected, "cohort original58 source custody differs")
        bundle_path = RUNTIME / (name + "-original-build-logs.jsonl")
        bundle_raw = read(bundle_path)
        require(build["completeBuildLogBundle"] == {"path": bundle_path.name, **ident(bundle_raw)},
                "complete nested bundle identity differs")
        nested = [parse(line) for line in bundle_raw.splitlines()]
        require(len(nested) == 66 and len({r["path"] for r in nested}) == 66
                and {r["path"] for r in nested} == set(CFG["originalLogPaths"]), "exact66 original log identities required")
        for row in nested:
            require(set(row) == {"path", "bytes", "sha256", "gitBlob", "content"}
                    and ident(row["content"].encode("utf-8")) == {k: row[k] for k in ("bytes", "sha256", "gitBlob")}
                    and read(directory / str(relative(row["path"]))) == row["content"].encode("utf-8"),
                    "retained raw original log differs")
        require(set(stored["contracts"]) == {"jit", "int", "nocompact", "c2"}, "four actual range modes required")
        for mode in ("jit", "int", "nocompact", "c2"):
            matches = re.findall(r"^JOINED_RANGE_CONTRACT_PASS directories=([1-9][0-9]*) cases=([1-9][0-9]*) backingUnits=([1-9][0-9]*) digest=([0-9a-f]{64})$",
                                read(RUNTIME / (name + "-range-" + mode + ".log")).decode("utf-8"), re.M)
            require(len(matches) == 1 and list(matches[0]) == expected_contract == stored["contracts"][mode],
                    "actual118/4026/7424 range proof differs")
    for left, right in itertools.combinations(observed_class_maps, 2):
        a, b = observed_class_maps[left], observed_class_maps[right]
        require([p for p in sorted(a) if a[p] != b[p]] == ["com/m3/text/JoinedM3StringPiece.class"],
                "unadmitted compiled class difference")
    expected_files = {command["log"] for command in receipt["commands"]} | {"inner-commands.jsonl", "runtime-receipt.json"}
    expected_files |= {c["name"] + "-original-build-logs.jsonl" for c in CFG["cohorts"]}
    require({p.name for p in RUNTIME.iterdir() if p.is_file() or p.is_symlink()} == expected_files,
            "runtime raw evidence has missing/extra files")
    for flag in FALSE_GATES:
        require(receipt[flag] is False, "runtime scope drift:" + flag)
    return receipt


def report():
    record = binding()
    require(record == parse(read(ROOT / "binding-receipt.json")), "source inputs changed during qualification")
    sdk, emissions = sdk_evidence()
    runtime_receipt = verify_runtime()
    check_legacy_projection()
    result = {"schema": "M3_JOINED_FRESH_HOSTED_QUALIFICATION_V1", "checkoutCommit": record["checkoutCommit"],
        "qualificationId": record["qualificationId"], "canonicalSourceCommit": record["canonicalSourceCommit"],
        "sourceBindingIdentity": record["sourceBindingIdentity"],
        "sdkReceiptIdentity": ident(read(SDK / "target/authoring-sdk-receipt.json")),
        "runtimeReceiptIdentity": ident(read(RUNTIME / "runtime-receipt.json")),
        "rawJavaFixtureIdentity": ident(read(SDK / "target/fresh-owned-java-execution.json")),
        "authenticDirectionalEmissions": emissions, "sdkCounters": [3, 16, 30, 2],
        "cohortOwners": runtime_receipt["cohortOwners"],
        "legacy47ProjectionQualified": True, "sourceRecipeQualified": True,
        "actualFourVariantOwnershipFixtureQualified": True, "allThreeOriginalCoreBuildsQualified": True,
        "completeRangeCorpusQualified": True, "pairedMeasurementsComplete": True,
        "combinedRows": 270, "diagnosticRows": 72, "runtimeCommands": 108,
        "nestedOriginalBuildLogs": 198, "physicalReceiverFreshOwnedApplied": False,
        "interpretation": "MEASURED_COMPONENT_EXPERIMENT_ONLY_NO_PERFORMANCE_OR_PROMOTION_GATE",
        **{key: False for key in FALSE_GATES}}
    save(ROOT / "qualification-receipt.json", result)
    print("M3_JOINED_FRESH_REPORT_PASS")


def raw_record(path, raw):
    relative(path)
    return {"schema": "M3_JOINED_FRESH_RAW_MEMBER_V1", "path": path, **ident(raw),
            "encoding": "base64", "content": base64.b64encode(raw).decode("ascii")}


def raw_bundle(records, expected_paths):
    require(len(records) == len(expected_paths) and {r["path"] for r in records} == set(expected_paths)
            and len({r["path"] for r in records}) == len(records), "exact raw logical inventory required")
    lines = []
    for row in records:
        require(set(row) == RAW_KEYS and row["schema"] == "M3_JOINED_FRESH_RAW_MEMBER_V1"
                and row["encoding"] == "base64", "exact raw envelope keys required")
        decoded = base64.b64decode(row["content"], validate=True)
        require(ident(decoded) == {k: row[k] for k in ("bytes", "sha256", "gitBlob")},
                "raw envelope decoded identity differs")
        lines.append(json.dumps(row, sort_keys=True, separators=(",", ":")).encode("utf-8") + b"\n")
    return b"".join(lines)


def collect(records, head, maven_identity, gate):
    """Called by the same supervised workflow after all six fresh outer commands terminate."""
    require([r["name"] for r in records] == FRESH_PHASES and len(records) == 6,
            "complete six fresh outer commands required")
    require(all(r["returncode"] == 0 and r["parentReaped"] is True and r["ownedSessionAbsent"] is True
                for r in records), "fresh outer command lifecycle incomplete")
    legacy = check_legacy_projection()
    receipt = parse(read(ROOT / "qualification-receipt.json"))
    require(receipt["sourceRecipeQualified"] is True and receipt["allThreeOriginalCoreBuildsQualified"] is True
            and receipt["completeRangeCorpusQualified"] is True and receipt["pairedMeasurementsComplete"] is True,
            "complete fresh report required")
    runtime_receipt = verify_runtime()
    fixed = {
        "fresh-source-binding.json": HERE / "source-binding.json",
        "fresh-binding-receipt.json": ROOT / "binding-receipt.json",
        "fresh-TEST-" + SUITE + ".xml": SDK / "target/surefire-reports" / ("TEST-" + SUITE + ".xml"),
        "fresh-authoring-recipe-test-counters.json": SDK / "target/authoring-recipe-test-counters.json",
        "fresh-authoring-sdk-receipt.json": SDK / "target/authoring-sdk-receipt.json",
        "fresh-owned-java-execution.json": SDK / "target/fresh-owned-java-execution.json",
        "fresh-emitted-forward-JoinedM3StringPiece.java": SDK / "target/emitted/forward" / TARGET,
        "fresh-emitted-inverse-JoinedM3StringPiece.java": SDK / "target/emitted/inverse" / TARGET,
        "fresh-runtime-receipt.json": RUNTIME / "runtime-receipt.json",
        "fresh-qualification-receipt.json": ROOT / "qualification-receipt.json"}
    payloads = {name: read(path) for name, path in fixed.items()}
    host_paths = {"sdk/pins.json": SDK / "pins.json",
                  "sdk/surefire/" + SUITE + ".txt": SDK / "target/surefire-reports" / (SUITE + ".txt"),
                  "source/QUALIFICATION_PLAN.json": HERE / "QUALIFICATION_PLAN.json",
                  "source/SOURCE_STATUS.json": HERE / "SOURCE_STATUS.json"}
    for record in records:
        name = record["name"]
        path = OUTER / (name + ".log")
        require(record["log"] == path.name and record["logBytes"] == len(read(path)), "fresh outer raw stdout differs")
        host_paths["outer/" + name + ".log"] = path
    require(len(host_paths) == CFG["hostRawRecords"] == 10, "complete host raw logical inventory required")
    payloads["fresh-host-raw.jsonl"] = raw_bundle([raw_record(name, read(path)) for name, path in sorted(host_paths.items())], host_paths)
    nested = []
    runtime_paths = {command["log"]: RUNTIME / command["log"] for command in runtime_receipt["commands"]}
    runtime_paths["inner-commands.jsonl"] = RUNTIME / "inner-commands.jsonl"
    for cohort in [r["name"] for r in CFG["cohorts"]]:
        bundle_name = cohort + "-original-build-logs.jsonl"
        runtime_paths[bundle_name] = RUNTIME / bundle_name
        rows = [parse(line) for line in read(RUNTIME / bundle_name).splitlines()]
        require(len(rows) == 66 and {row["path"] for row in rows} == set(CFG["originalLogPaths"]),
                "complete66 raw build logs per cohort required")
        for row in rows:
            raw = row["content"].encode("utf-8")
            require(ident(raw) == {k: row[k] for k in ("bytes", "sha256", "gitBlob")}, "nested raw build log differs")
            nested.append(raw_record(cohort + "/" + row["path"], raw))
    expected_nested = [cohort["name"] + "/" + path for cohort in CFG["cohorts"] for path in CFG["originalLogPaths"]]
    require(len(expected_nested) == CFG["buildRawRecords"] == 198, "exact198 nested build logs required")
    payloads["fresh-original-build-logs.jsonl"] = raw_bundle(nested, expected_nested)
    require(len(runtime_paths) == CFG["runtimeRawRecords"] == 112, "108 raw process logs plus raw journal/three raw build bundles required")
    payloads["fresh-runtime-raw.jsonl"] = raw_bundle([raw_record(name, read(path)) for name, path in sorted(runtime_paths.items())], runtime_paths)
    payloads["fresh-outer-commands.jsonl"] = b"".join((json.dumps(r, sort_keys=True) + "\n").encode("utf-8") for r in records)
    storage_raw = read(ROOT / "storage-guard-receipt.json")
    storage = parse(storage_raw)
    require(storage["failures"] == 0 and storage["legacyControls"] == 16 and storage["freshControls"] == 16
            and storage["tests"] == 32 and len(storage["cases"]) == len(set(storage["cases"])) == 32
            and storage["controllerEnvironmentControls"]["tests"] == 2
            and storage["controllerEnvironmentControls"]["failures"] == 0
            and storage["controllerEnvironmentControls"]["cases"] ==
                ["wrong-default-java-and-missing-qualification-root-replaced",
                 "parent-environment-restored-after-refusal"],
            "all original16/new16 storage controls and exact scoped collector environment controls required")
    payloads["fresh-storage-guard-receipt.json"] = storage_raw
    hosted = {"schema": "M3_JOINED_FRESH_HOSTED_V1", "checkoutCommit": head,
        "qualificationId": os.environ["M3_QUALIFICATION_ID"], "phases": CFG["phases"], "freshCommands": records,
        "mavenIdentity": maven_identity, "qualificationReceiptIdentity": ident(payloads["fresh-qualification-receipt.json"]),
        "legacyProjection": legacy, "logicalRawCounts": {"host": 10, "runtime": 112, "nestedBuild": 198},
        "storageGuardReceiptIdentity": ident(storage_raw), "aggregateBytesBeforeCollection": gate(496*1024**2),
        "artifactBudgetIncludesEncoding": True, **{key: False for key in FALSE_GATES}}
    payloads["fresh-hosted-receipt.json"] = (json.dumps(hosted, indent=2, allow_nan=False) + "\n").encode("utf-8")
    require(set(payloads) == set(CFG["freshMembers"]) - {"fresh-inventory.json"} and len(payloads) == 16,
            "exact16 added payloads before final inventory required")
    check_legacy_projection()
    inventory = {name: {**item, "source": "IMMUTABLE_CURRENT_RUN_LEGACY47_PROJECTION"} for name, item in legacy.items()}
    inventory.update({name: {**ident(raw), "source": "FRESH_OWNED_COMPLETE_SOURCE_BOUND_EVIDENCE"} for name, raw in payloads.items()})
    require(len(inventory) == 63, "final63 nonself inventory required")
    payloads["fresh-inventory.json"] = (json.dumps(inventory, indent=2, sort_keys=True) + "\n").encode("utf-8")
    before = gate(496*1024**2)
    addition = sum(len(raw) for raw in payloads.values())
    total = sum(item["bytes"] for item in legacy.values()) + addition
    require(len(legacy) + len(payloads) == 64 and total <= 8*1024**2
            and before + addition + 1024**2 <= 496*1024**2,
            "complete encoded evidence cannot fit same64-member/8MiB/512MiB budget")
    for name, raw in sorted(payloads.items()):
        require(not (ARTIFACTS / name).exists(), "fresh artifact name already occupied")
        gate(496*1024**2)
        with (ARTIFACTS / name).open("xb") as stream:
            stream.write(raw)
    require(sorted(p.name for p in ARTIFACTS.iterdir()) == sorted(CFG["legacyMembers"] + CFG["freshMembers"]),
            "final complete64-member census differs")
    for name, item in inventory.items():
        require(ident(read(ARTIFACTS / name)) == {k: item[k] for k in ("bytes", "sha256", "gitBlob")},
                "final raw inventory identity differs")
    require(sum(p.stat().st_size for p in ARTIFACTS.iterdir()) == total, "final encoded artifact byte census differs")
    check_legacy_projection()
    gate(496*1024**2)
    print("M3_JOINED_FRESH_ARTIFACT_COMPLETE members=64 bytes=" + str(total))


def collect_failure(records, error, gate):
    """Retain observed failure evidence; this path never issues a qualification success."""
    payloads = {}
    host = {}
    if OUTER.is_dir():
        for path in sorted(OUTER.iterdir()):
            require(path.suffix == ".log" and path.name[:-4] in FRESH_PHASES, "unexpected fresh outer log")
            host["outer/" + path.name] = read(path)
    for logical, path in (("sdk/pins.json", SDK / "pins.json"),
                          ("source/QUALIFICATION_PLAN.json", HERE / "QUALIFICATION_PLAN.json"),
                          ("source/SOURCE_STATUS.json", HERE / "SOURCE_STATUS.json")):
        if path.exists() or path.is_symlink():
            host[logical] = read(path)
    directory = SDK / "target/surefire-reports"
    if directory.is_dir():
        for path in sorted(directory.iterdir()):
            if path.suffix in (".txt", ".xml"):
                host["sdk/surefire/" + path.name] = read(path)
    for path in sorted(ROOT.glob("*.json")):
        host["internal/" + path.name] = read(path)
    payloads["fresh-host-raw.jsonl"] = raw_bundle([raw_record(p, raw) for p, raw in sorted(host.items())], host)
    runtime_raw, nested = {}, {}
    if RUNTIME.is_dir():
        for path in sorted(RUNTIME.iterdir()):
            if path.is_file() or path.is_symlink():
                runtime_raw[path.name] = read(path)
        for cohort in [r["name"] for r in CFG["cohorts"]]:
            directory = RUNTIME / cohort / "m3/build/logs"
            if directory.is_dir():
                for path in sorted(directory.rglob("*")):
                    if path.is_file() or path.is_symlink():
                        logical = cohort + "/" + path.relative_to(RUNTIME / cohort).as_posix()
                        nested[logical] = read(path)
    payloads["fresh-runtime-raw.jsonl"] = raw_bundle([raw_record(p, raw) for p, raw in sorted(runtime_raw.items())], runtime_raw)
    payloads["fresh-original-build-logs.jsonl"] = raw_bundle([raw_record(p, raw) for p, raw in sorted(nested.items())], nested)
    payloads["fresh-outer-commands.jsonl"] = b"".join((json.dumps(r, sort_keys=True) + "\n").encode("utf-8") for r in records)
    fixed = {
        "fresh-source-binding.json": HERE / "source-binding.json",
        "fresh-binding-receipt.json": ROOT / "binding-receipt.json",
        "fresh-TEST-" + SUITE + ".xml": SDK / "target/surefire-reports" / ("TEST-" + SUITE + ".xml"),
        "fresh-authoring-recipe-test-counters.json": SDK / "target/authoring-recipe-test-counters.json",
        "fresh-authoring-sdk-receipt.json": SDK / "target/authoring-sdk-receipt.json",
        "fresh-owned-java-execution.json": SDK / "target/fresh-owned-java-execution.json",
        "fresh-emitted-forward-JoinedM3StringPiece.java": SDK / "target/emitted/forward" / TARGET,
        "fresh-emitted-inverse-JoinedM3StringPiece.java": SDK / "target/emitted/inverse" / TARGET,
        "fresh-runtime-receipt.json": RUNTIME / "runtime-receipt.json",
        "fresh-storage-guard-receipt.json": ROOT / "storage-guard-receipt.json"}
    for name, path in fixed.items():
        if path.exists() or path.is_symlink():
            payloads[name] = read(path)
    failure = {"schema": "M3_JOINED_FRESH_QUALIFICATION_REFUSAL_V1", "state": "REFUSED",
        "errorType": type(error).__name__, "message": str(error),
        "checkoutCommit": os.environ.get("EXPECTED_SOURCE_SHA"), "qualificationId": os.environ.get("M3_QUALIFICATION_ID"),
        "attemptedFreshOuterCommands": records, "observedRawCounts": {"host": len(host), "runtime": len(runtime_raw), "nestedBuild": len(nested)},
        "expectedSuccessCounts": {"runtimeProcesses": 108, "nestedBuildRecords": 198, "originalProgramInvocations": 192},
        "sourceRecipeQualified": False, "allThreeOriginalCoreBuildsQualified": False,
        "completeRangeCorpusQualified": False, "pairedMeasurementsComplete": False,
        "qualificationComplete": False, **{key: False for key in FALSE_GATES}}
    payloads["fresh-qualification-receipt.json"] = (json.dumps(failure, indent=2) + "\n").encode("utf-8")
    payloads["fresh-hosted-receipt.json"] = (json.dumps({"schema": "M3_JOINED_FRESH_HOSTED_REFUSAL_V1",
        "state": "REFUSED", "failureReceiptIdentity": ident(payloads["fresh-qualification-receipt.json"]),
        "allObservedRawStdoutRetained": True, "expectedSuccessInventoryComplete": False,
        **{key: False for key in FALSE_GATES}}, indent=2) + "\n").encode("utf-8")
    present = {p.name: ident(read(p)) for p in ARTIFACTS.iterdir()}
    require(not set(present) & set(CFG["freshMembers"]), "failure collection cannot overwrite earlier artifacts")
    inventory = {name: {**item, "source": "CURRENT_RUN_LEGACY_ATTEMPT"} for name, item in present.items()}
    inventory.update({name: {**ident(raw), "source": "OBSERVED_FRESH_REFUSAL_EVIDENCE"} for name, raw in payloads.items()})
    require(set(payloads) <= set(CFG["freshMembers"]), "failure physical evidence names escaped admission")
    payloads["fresh-inventory.json"] = (json.dumps(inventory, indent=2, sort_keys=True) + "\n").encode("utf-8")
    addition = sum(len(raw) for raw in payloads.values())
    total = sum(item["bytes"] for item in present.values()) + addition
    before = gate(496*1024**2)
    require(len(present) + len(payloads) <= 64 and total <= 8*1024**2
            and before + addition + 1024**2 <= 496*1024**2,
            "complete failure raw evidence cannot fit admitted caps; refuse without dropping records")
    for name, raw in sorted(payloads.items()):
        gate(496*1024**2)
        with (ARTIFACTS / name).open("xb") as stream:
            stream.write(raw)
    gate(496*1024**2)
    print("M3_JOINED_FRESH_REFUSED_COMPLETE_OBSERVED_EVIDENCE members=" + str(len(present) + len(payloads)))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("binding", "runtime", "report"))
    operation = parser.parse_args().operation
    if operation == "binding":
        save(ROOT / "binding-receipt.json", binding(fresh=True))
        print("M3_JOINED_FRESH_BINDING_PASS")
    elif operation == "runtime":
        runtime()
    else:
        report()


if __name__ == "__main__":
    main()
