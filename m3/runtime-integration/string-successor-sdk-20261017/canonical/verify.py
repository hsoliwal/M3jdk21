#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify exact receiving SDK inputs and actual six-test Surefire evidence."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import tempfile
from pathlib import Path, PurePosixPath
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
SUITES = {"com.synexia.rewrite.M3StringHistoricalPrecomputePinRecipeTest":["everyPartialStateAndBothOrdersReachExactFixedPoint","eachRequiredInputAndAdditionRefusesDriftMissingAndDuplicates","emittedFocusedPythonRegressionsExposeBaselineAndAcceptCandidate"],"com.synexia.rewrite.M3StringUtf8SuccessorChainRecipeTest":["everyPartialStateAndBothOrdersReachExactFixedPoint","eachRequiredInputAndAdditionRefusesDriftMissingAndDuplicates","namedRecipeEmitsSerialHistoryAndCompletePythonSpectrum"]}
REQUIRED_PATHS = ["licenses/ADDITIONAL_LICENSE_INFO","licenses/ASSEMBLY_EXCEPTION","licenses/GPL-2.0.txt","pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java","pinned-engine/com/synexia/rewrite/M3StringHistoricalPrecomputePinRecipe.java","pinned-engine/com/synexia/rewrite/M3StringUtf8SuccessorChainRecipe.java","pom.xml","src/main/resources/META-INF/rewrite/m3-string-historical-precompute-pin-20261017.yml","src/main/resources/META-INF/rewrite/m3-string-utf8-successor-chain-20261017.yml","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/RECEIPT.json","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/check_string_phase.py","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/m3-string-precompute-observation-v2.tsv","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/string-source-target-map.tsv","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/string-target-successor-chain.tsv","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/string-target-successors.tsv","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/synexia-string-phase-checker-successor-20261013.json","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/synexia-string-phase-checker-successor-20261015-v2.json","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/test_string_phase_successors.py","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/test_synexia_string_phase_checker_successor.py","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/check_string_phase.py","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/string-phase-checker-v2-20261017.py","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/synexia-string-phase-checker-successor-20261017-v3.json","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/test_string_phase_successors.py","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/test_synexia_string_phase_checker_successor.py","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3String.java","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringFacts.java","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringPool.java","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringPositionPrecompute.java","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringSearchPrecompute.java","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/RECEIPT.json","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/check_string_phase.py","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/leaf-cursor-receipt-20261015.md","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/m3-string-precompute-observation-v2.tsv","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/modified-utf8-dp-receipt-20261014.md","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/name-mapping.json","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-phase-checker-v2-20261017.py","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-source-target-map.tsv","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-target-successor-chain.tsv","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-target-successors.tsv","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/synexia-string-phase-checker-successor-20261013.json","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/synexia-string-phase-checker-successor-20261015-v2.json","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/synexia-string-phase-checker-successor-20261017-v3.json","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/test_string_phase_successors.py","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/test_synexia_string_phase_checker_successor.py","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/check_string_phase.py","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/string-phase-checker-v3-20261017.py","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/string-target-owner-successor-chain-20261017.tsv","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/synexia-string-phase-checker-successor-20261017-v4.json","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/test_string_phase_successors.py","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/test_synexia_string_phase_checker_successor.py","src/test/java/com/synexia/rewrite/M3StringHistoricalPrecomputePinRecipeTest.java","src/test/java/com/synexia/rewrite/M3StringUtf8SuccessorChainRecipeTest.java","verify.py"]
REQUIRED_GIT_BLOBS = {"licenses/ADDITIONAL_LICENSE_INFO":"ff700cd09f6ea0cc497f68169fd5c210bdaa7c2b","licenses/ASSEMBLY_EXCEPTION":"429666664e9ff5029d51dc7f807e1ec9eff2dd9f","licenses/GPL-2.0.txt":"8b400c7ab81b7b18baff3f81d597f5e511883134","pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"38df5f97bbeec8a8a8cfc77091d56bda925c07f2","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"1c1b1b568d369f688f6953161636bf36b484bf5a","pinned-engine/com/synexia/rewrite/M3StringHistoricalPrecomputePinRecipe.java":"0bc92e10a154c64a5420059836c5b6d6c05d5820","pinned-engine/com/synexia/rewrite/M3StringUtf8SuccessorChainRecipe.java":"99abb1217e1ffc8b70154425bdde8c497f531370","src/main/resources/META-INF/rewrite/m3-string-historical-precompute-pin-20261017.yml":"bc2b93be6687a9412c478cb0f7ceb8b006b86e9d","src/main/resources/META-INF/rewrite/m3-string-utf8-successor-chain-20261017.yml":"1b17e7ed0812273e48f39910fd0af16188e62d1a","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/RECEIPT.json":"e3ee12859fd0a272dfcf56e85741a95ac4acbf76","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/check_string_phase.py":"274753ece4a5cfd11dc2e98feca50f2601540b8d","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/m3-string-precompute-observation-v2.tsv":"3f7ece2cebe1c429256acfa904134a6d0fd47ef5","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/string-source-target-map.tsv":"088ee2ae6ccc550af768ac17314d7b0d460e9cf3","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/string-target-successor-chain.tsv":"75d949e5aa2c8ff4e5c70d648e81fc0d37b68651","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/string-target-successors.tsv":"141806b3119342dfba2efe6487664ba3a0c53158","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/synexia-string-phase-checker-successor-20261013.json":"a0373fd42a1fe2b8104e3675abc1919254950fff","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/synexia-string-phase-checker-successor-20261015-v2.json":"52cacd20c406f57027734d2e78b405c8b6176e9f","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/test_string_phase_successors.py":"e23fa5f44554e9dc2c841040f06a68657637ae6f","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/test_synexia_string_phase_checker_successor.py":"1c1aeabe6212188815961f160fd719e862625bbe","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/manifest.tsv":"b1f8c7749013d5b6e198e855d4dfe1d034331a58","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/check_string_phase.py":"368842c70f4671bac14ad902ed047a486109f27d","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/string-phase-checker-v2-20261017.py":"274753ece4a5cfd11dc2e98feca50f2601540b8d","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/synexia-string-phase-checker-successor-20261017-v3.json":"22d49606563cfb93d06024ba6ed34fd3fd682d6e","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/test_string_phase_successors.py":"70f3abde194e3437ac4e6c68b1d320f82e7d896b","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/test_synexia_string_phase_checker_successor.py":"9a2c5b0a5583a04b145443df966e29369fcd707e","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3String.java":"35ed998540e044eed8cb7596e2e2c019ae369bd3","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringFacts.java":"d8dd7dc30db6a9e3897146572bd4a7755c954ffb","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringPool.java":"0905414556ad0b698a5b5f835f53d12bc040bc8c","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringPositionPrecompute.java":"c0074f4b144da23040e63e0ba8866d56d0d304ac","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringSearchPrecompute.java":"0a88ed559b23def5d2229bba3316ea2268b4d720","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/RECEIPT.json":"e3ee12859fd0a272dfcf56e85741a95ac4acbf76","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/check_string_phase.py":"368842c70f4671bac14ad902ed047a486109f27d","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/leaf-cursor-receipt-20261015.md":"13b2a36514736d6357cb8ccf130fede577ee059b","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/m3-string-precompute-observation-v2.tsv":"3f7ece2cebe1c429256acfa904134a6d0fd47ef5","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/modified-utf8-dp-receipt-20261014.md":"0777d79c2d32361c40f1198e9bff5c312fbf1229","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/name-mapping.json":"a44460f982940623cf6ba1ca625caeeaa889f633","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-phase-checker-v2-20261017.py":"274753ece4a5cfd11dc2e98feca50f2601540b8d","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-source-target-map.tsv":"088ee2ae6ccc550af768ac17314d7b0d460e9cf3","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-target-successor-chain.tsv":"75d949e5aa2c8ff4e5c70d648e81fc0d37b68651","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-target-successors.tsv":"141806b3119342dfba2efe6487664ba3a0c53158","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/synexia-string-phase-checker-successor-20261013.json":"a0373fd42a1fe2b8104e3675abc1919254950fff","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/synexia-string-phase-checker-successor-20261015-v2.json":"52cacd20c406f57027734d2e78b405c8b6176e9f","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/synexia-string-phase-checker-successor-20261017-v3.json":"22d49606563cfb93d06024ba6ed34fd3fd682d6e","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/test_string_phase_successors.py":"70f3abde194e3437ac4e6c68b1d320f82e7d896b","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/test_synexia_string_phase_checker_successor.py":"9a2c5b0a5583a04b145443df966e29369fcd707e","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/manifest.tsv":"d4125078eb44dc625cac0a914fd435147ae47a99","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/check_string_phase.py":"2f8b3b75c1de3110c30cf3b860b6fbad8f00fd72","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/string-phase-checker-v3-20261017.py":"368842c70f4671bac14ad902ed047a486109f27d","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/string-target-owner-successor-chain-20261017.tsv":"c7b52d16a15277c0aa2a09fada6f1fb3840aa70e","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/synexia-string-phase-checker-successor-20261017-v4.json":"1bf38b06df3bef3dc07e6c4d4f9c818561592cfe","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/test_string_phase_successors.py":"1c45ef4a88d6f941e158be2529b33e3dedbe4d89","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/test_synexia_string_phase_checker_successor.py":"68faa1c5f4c6807d6d162ee40eae362b6a683ce3","src/test/java/com/synexia/rewrite/M3StringHistoricalPrecomputePinRecipeTest.java":"d87f54324e5dc338ce1d2dc34639bda9dacfede4","src/test/java/com/synexia/rewrite/M3StringUtf8SuccessorChainRecipeTest.java":"5fe36d01accd5d8c55fa0d61fc77ba97bf472812"}
REQUIRED_SHA256 = {"licenses/ADDITIONAL_LICENSE_INFO":"a69bce275ba7a3570af6579cb0f55682cd75fedfcd49e0e8e9022270c447c916","licenses/ASSEMBLY_EXCEPTION":"75292f03bf23d3db7c985aecc191029b93883200721ed23ed34a2e601463df33","licenses/GPL-2.0.txt":"4b9abebc4338048a7c2dc184e9f800deb349366bdf28eb23c2677a77b4c87726","pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"ee90a2b8689699745553c2f4d2d07121b91777f4e9a671fdb33ba8edea0c14ec","pinned-engine/com/synexia/rewrite/M3StringHistoricalPrecomputePinRecipe.java":"8e188d4d9d482f7d345b0d37f3437aaa2d0ce6dee51967b145b56dfc3c411a70","pinned-engine/com/synexia/rewrite/M3StringUtf8SuccessorChainRecipe.java":"ac0c8fb10cd90f5344f043f6c61df4eb1dde9f8648074574e501b27ba8b0cea1","pom.xml":"a00ccd905424b5ca57b1b8202af8c75d95eefc76ea02bacdf9e3cd976b0d95bd","src/main/resources/META-INF/rewrite/m3-string-historical-precompute-pin-20261017.yml":"ca25e94319cf666f4293205f991f27b2d5e3088dc114f2f0e600a9869d013798","src/main/resources/META-INF/rewrite/m3-string-utf8-successor-chain-20261017.yml":"b6bb242cc6ea5333d35780ab6bf588a797751018ad6a92260a9a5e8018254860","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/RECEIPT.json":"510d635a87eac7f0d0dcfcb5901b1e985ed770571b5f98f46ae4bb286cdf9f68","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/check_string_phase.py":"bad0fe420fc677a977079e0aa557ff733e2858939781786bc37d0c9f38d96ff3","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/m3-string-precompute-observation-v2.tsv":"d29cc98a5611059b69e08edd3d7990c0dd37100fb16e95cbf2136716aa35f52d","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/string-source-target-map.tsv":"954714bf6ada59b7dd22e886f59a403a042141785697781869c78278a0bb9b98","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/string-target-successor-chain.tsv":"cfdbd550ca16d6f28e718347409b328b677f473f1f498cb903da69c1cb386ca0","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/string-target-successors.tsv":"ab1c56d567e739a6a5d2f630ec667751a8821ebdab2857a225c2650d84ca7e25","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/synexia-string-phase-checker-successor-20261013.json":"78fc05a0cb9b7f26e9a6226b2406cc38801f09040dc14e445e31651f7a9385af","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/synexia-string-phase-checker-successor-20261015-v2.json":"e47b404b87e9748f323cfbca53f07fcde3c3d1a5b14f221e14df6a92613cb1b2","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/test_string_phase_successors.py":"55de1bbd8c7eb003b33e8c969fcfe4581d44a7abbbccc883d981daf2246fe130","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/before/test_synexia_string_phase_checker_successor.py":"8cbf60faa89b6ab227a15d35b5e5cee000bff485f76216f51d8da98446e3432f","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/manifest.tsv":"5bb1464433a252f38c38c1f8ab1240eb8c3f0760bbbc05551e8130d68b8b73c0","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/check_string_phase.py":"3dd800d3d76523a52918f77f68371965b77527ad1bdefd1e267846da7aaf8b47","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/string-phase-checker-v2-20261017.py":"bad0fe420fc677a977079e0aa557ff733e2858939781786bc37d0c9f38d96ff3","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/synexia-string-phase-checker-successor-20261017-v3.json":"45a7b377ec949d75f12d030952eeab9b5175c42f4581a0d9e900ee994da7208c","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/test_string_phase_successors.py":"992b8524d4ed3f1fabdd4f2685adb6d55eb23d9e3de0683675695afa71e03e75","src/main/resources/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/out/test_synexia_string_phase_checker_successor.py":"b569114a7e2e0d4380f1bbbb503f9a9c5c5914cdeca3aeebe4c9b4bab26c470f","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3String.java":"cd1192e27c981da03aeae2385d3d54ad31d912f043cf8c7dfba18c7b9106b728","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringFacts.java":"2152c154d3cdece330667ee09675fd21d86f3b483a23a3156b8a3626c3f9f420","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringPool.java":"b58c82047f795807ff32229146c370c9e596eff19942a92cd0128ce4987d20ca","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringPositionPrecompute.java":"a96f49012368859f5bcd22673b3393b49f2e592520741c069ae5ee4bcde2fa1b","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/M3StringSearchPrecompute.java":"edae3c8d3afbd7c1b78aa3f88a4d3fdcd6bd03c9f8ee2ace3044255b44f571ac","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/RECEIPT.json":"510d635a87eac7f0d0dcfcb5901b1e985ed770571b5f98f46ae4bb286cdf9f68","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/check_string_phase.py":"3dd800d3d76523a52918f77f68371965b77527ad1bdefd1e267846da7aaf8b47","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/leaf-cursor-receipt-20261015.md":"34d15a97962500108f67909ea3389bd94dceb16111b1617b163ae67039b225d9","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/m3-string-precompute-observation-v2.tsv":"d29cc98a5611059b69e08edd3d7990c0dd37100fb16e95cbf2136716aa35f52d","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/modified-utf8-dp-receipt-20261014.md":"e4875b1656c2d9c8c2a28888190c794416e14d501fc9b68fe4ed5dcb305448dc","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/name-mapping.json":"fa9832d48ec7dc0334fcb3a02e1b665612ca3a9f3dfe13cd104ff3f2c42871ac","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-phase-checker-v2-20261017.py":"bad0fe420fc677a977079e0aa557ff733e2858939781786bc37d0c9f38d96ff3","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-source-target-map.tsv":"954714bf6ada59b7dd22e886f59a403a042141785697781869c78278a0bb9b98","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-target-successor-chain.tsv":"cfdbd550ca16d6f28e718347409b328b677f473f1f498cb903da69c1cb386ca0","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/string-target-successors.tsv":"ab1c56d567e739a6a5d2f630ec667751a8821ebdab2857a225c2650d84ca7e25","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/synexia-string-phase-checker-successor-20261013.json":"78fc05a0cb9b7f26e9a6226b2406cc38801f09040dc14e445e31651f7a9385af","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/synexia-string-phase-checker-successor-20261015-v2.json":"e47b404b87e9748f323cfbca53f07fcde3c3d1a5b14f221e14df6a92613cb1b2","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/synexia-string-phase-checker-successor-20261017-v3.json":"45a7b377ec949d75f12d030952eeab9b5175c42f4581a0d9e900ee994da7208c","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/test_string_phase_successors.py":"992b8524d4ed3f1fabdd4f2685adb6d55eb23d9e3de0683675695afa71e03e75","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/before/test_synexia_string_phase_checker_successor.py":"b569114a7e2e0d4380f1bbbb503f9a9c5c5914cdeca3aeebe4c9b4bab26c470f","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/manifest.tsv":"77f641c14dded217f98736a6abd918b0a538e976a7fe0b849a6ee90901e2654f","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/check_string_phase.py":"db38acfb6f2b04b5f119544e91642a3639bd6b73ae32e88f077e819b0a4375c1","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/string-phase-checker-v3-20261017.py":"3dd800d3d76523a52918f77f68371965b77527ad1bdefd1e267846da7aaf8b47","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/string-target-owner-successor-chain-20261017.tsv":"c56d86b7981dd31533a509aad97c25368df57b6e0297847dde8b3435e9c618ad","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/synexia-string-phase-checker-successor-20261017-v4.json":"b9e78abacc2a7e6bdf9ac21711812381f476288af2a1e289cf8368ec66e75643","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/test_string_phase_successors.py":"48796f0fd1be609bd50c7b83293c5c08bcf6de3261298d818f41529926da16cc","src/main/resources/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/out/test_synexia_string_phase_checker_successor.py":"b39ed26dfbeef7d830d54c234d4c6de73a823b41412d3161a200c1a0e4389073","src/test/java/com/synexia/rewrite/M3StringHistoricalPrecomputePinRecipeTest.java":"efcf52bb0a9fc332feda16a2d245f17e3c2c011aa68eff2cd8dff7ecab4ffcf0","src/test/java/com/synexia/rewrite/M3StringUtf8SuccessorChainRecipeTest.java":"9664eb776f62d020d27f85d78a58a6042763755cb333e6faa5cd8d16c9570d89"}
RECEIVER_OUTPUTS = {"m3/compatibility/fixtures/string-phase-checker-v2-20261017.py":"bad0fe420fc677a977079e0aa557ff733e2858939781786bc37d0c9f38d96ff3","m3/compatibility/fixtures/string-phase-checker-v3-20261017.py":"3dd800d3d76523a52918f77f68371965b77527ad1bdefd1e267846da7aaf8b47","m3/compatibility/synexia-string-phase-checker-successor-20261013.json":"78fc05a0cb9b7f26e9a6226b2406cc38801f09040dc14e445e31651f7a9385af","m3/compatibility/synexia-string-phase-checker-successor-20261015-v2.json":"e47b404b87e9748f323cfbca53f07fcde3c3d1a5b14f221e14df6a92613cb1b2","m3/compatibility/synexia-string-phase-checker-successor-20261017-v3.json":"45a7b377ec949d75f12d030952eeab9b5175c42f4581a0d9e900ee994da7208c","m3/compatibility/synexia-string-phase-checker-successor-20261017-v4.json":"b9e78abacc2a7e6bdf9ac21711812381f476288af2a1e289cf8368ec66e75643","m3/compatibility/test_synexia_string_phase_checker_successor.py":"b39ed26dfbeef7d830d54c234d4c6de73a823b41412d3161a200c1a0e4389073","m3/docs/name-mapping.json":"fa9832d48ec7dc0334fcb3a02e1b665612ca3a9f3dfe13cd104ff3f2c42871ac","m3/runtime-integration/leaf-cursor-receipt-20261015.md":"34d15a97962500108f67909ea3389bd94dceb16111b1617b163ae67039b225d9","m3/runtime-integration/modified-utf8-dp-receipt-20261014.md":"e4875b1656c2d9c8c2a28888190c794416e14d501fc9b68fe4ed5dcb305448dc","m3/runtime-integration/utf8-cursor-20261014/RECEIPT.json":"510d635a87eac7f0d0dcfcb5901b1e985ed770571b5f98f46ae4bb286cdf9f68","m3/synexia-import/current-full-borrow/check_string_phase.py":"db38acfb6f2b04b5f119544e91642a3639bd6b73ae32e88f077e819b0a4375c1","m3/synexia-import/current-full-borrow/m3-string-precompute-observation-v2.tsv":"d29cc98a5611059b69e08edd3d7990c0dd37100fb16e95cbf2136716aa35f52d","m3/synexia-import/current-full-borrow/string-source-target-map.tsv":"954714bf6ada59b7dd22e886f59a403a042141785697781869c78278a0bb9b98","m3/synexia-import/current-full-borrow/string-target-owner-successor-chain-20261017.tsv":"c56d86b7981dd31533a509aad97c25368df57b6e0297847dde8b3435e9c618ad","m3/synexia-import/current-full-borrow/string-target-successor-chain.tsv":"cfdbd550ca16d6f28e718347409b328b677f473f1f498cb903da69c1cb386ca0","m3/synexia-import/current-full-borrow/string-target-successors.tsv":"ab1c56d567e739a6a5d2f630ec667751a8821ebdab2857a225c2650d84ca7e25","m3/synexia-import/current-full-borrow/test_string_phase_successors.py":"48796f0fd1be609bd50c7b83293c5c08bcf6de3261298d818f41529926da16cc","src/java.base/share/classes/java/lang/M3String.java":"cd1192e27c981da03aeae2385d3d54ad31d912f043cf8c7dfba18c7b9106b728","src/java.base/share/classes/java/lang/M3StringFacts.java":"2152c154d3cdece330667ee09675fd21d86f3b483a23a3156b8a3626c3f9f420","src/java.base/share/classes/java/lang/M3StringPool.java":"b58c82047f795807ff32229146c370c9e596eff19942a92cd0128ce4987d20ca","src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java":"a96f49012368859f5bcd22673b3393b49f2e592520741c069ae5ee4bcde2fa1b","src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java":"edae3c8d3afbd7c1b78aa3f88a4d3fdcd6bd03c9f8ee2ace3044255b44f571ac"}

def validate_report(data: bytes, classname: str, methods: list[str]) -> dict[str, int]:
    root = ET.fromstring(data)
    if root.tag != "testsuite":
        raise ValueError("expected one Surefire testsuite")
    counts = {key: int(root.get(key, "-1"))
              for key in ("tests", "failures", "errors", "skipped")}
    if counts != {"tests": len(methods), "failures": 0, "errors": 0, "skipped": 0}:
        raise ValueError("SDK suite did not execute all required gates successfully")
    cases = root.findall("testcase")
    if len(cases) != len(methods) or {case.get("name") for case in cases} != set(methods):
        raise ValueError("SDK test inventory drift")
    for case in cases:
        if case.get("classname") != classname:
            raise ValueError("SDK test class drift")
        if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
            raise ValueError("SDK test failure, error or skip present")
    return counts


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def inputs() -> tuple[dict, bytes]:
    pins_data = (ROOT / "pins.json").read_bytes()
    pins = json.loads(pins_data)
    if pins["schema"] != "M3_STRING_SUCCESSOR_RECEIVING_SDK_INPUTS_V1":
        raise ValueError("unknown receiving input schema")
    rows = pins["files"]
    if not 1 <= len(rows) <= 64:
        raise ValueError("receiving file budget")
    if {row["path"] for row in rows} != set(REQUIRED_PATHS):
        raise ValueError("required receiving input inventory drift")
    for name, expected in REQUIRED_SHA256.items():
        matches = [row for row in rows if row["path"] == name]
        if len(matches) != 1 or matches[0].get("sha256") != expected:
            raise ValueError("required receiving digest drift: " + name)
    for name, blob in REQUIRED_GIT_BLOBS.items():
        matches = [row for row in rows if row["path"] == name]
        if len(matches) != 1 or matches[0].get("unchangedGitBlob") != blob:
            raise ValueError("required receiving identity drift: " + name)
    seen = set()
    for row in rows:
        name = row["path"]
        path = PurePosixPath(name)
        if path.is_absolute() or ".." in path.parts or "\\" in name or name in seen:
            raise ValueError("invalid receiving path")
        seen.add(name)
        data = (ROOT / name).read_bytes()
        if hashlib.sha256(data).hexdigest() != row["sha256"]:
            raise ValueError("receiving source drift: " + name)
        if row.get("unchangedGitBlob") and git_blob(data) != row["unchangedGitBlob"]:
            raise ValueError("receiving Git blob drift: " + name)
    def inventory(base: Path, pattern: str) -> set[str]:
        found = set()
        for path in base.rglob(pattern):
            if path.is_file():
                found.add(path.relative_to(ROOT).as_posix())
                if len(found) > 64:
                    raise ValueError("receiving source inventory budget")
        return found
    engine = inventory(ROOT / "pinned-engine", "*.java")
    tests = inventory(ROOT / "src/test/java", "*.java")
    if engine != {name for name in REQUIRED_PATHS if name.startswith("pinned-engine/")}:
        raise ValueError("unsealed receiving provider")
    if tests != {name for name in REQUIRED_PATHS if name.startswith("src/test/java/")}:
        raise ValueError("receiving SDK test scope drift")
    resources = inventory(ROOT / "src/main/resources", "*")
    if resources != {name for name in REQUIRED_PATHS if name.startswith("src/main/resources/")}:
        raise ValueError("unsealed receiving resource")
    if inventory(ROOT / "src/test/resources", "*"):
        raise ValueError("test resources can shadow receiving fixtures")
    return pins, pins_data


def receiver(repository: Path, pins: dict) -> None:
    for name, expected in RECEIVER_OUTPUTS.items():
        path = repository / name
        if path.is_symlink() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise ValueError("receiving String tooling output drift: " + name)


def self_test(pins: dict, pins_data: bytes) -> None:
    global ROOT
    original = ROOT
    (original / "target").mkdir(exist_ok=True)
    refused = 0
    with tempfile.TemporaryDirectory(dir=original / "target") as directory:
        fixture = Path(directory)
        for row in pins["files"]:
            path = fixture / row["path"]
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes((original / row["path"]).read_bytes())
        (fixture / "pins.json").write_bytes(pins_data)
        try:
            ROOT = fixture
            inputs()
            def rejects() -> None:
                nonlocal refused
                try:
                    inputs()
                except ValueError:
                    refused += 1
                else:
                    raise AssertionError("unsealed receiving input admitted")
            for name in (
                "pinned-engine/com/synexia/rewrite/nested/Extra.java",
                "pinned-engine/other/Extra.java",
                "src/test/java/other/ExtraTest.java",
                "src/test/resources/com/synexia/rewrite/hash-pinned-java/"
                "m3jdk21-generic-filtered-purpose-20261017/M3FilteredMaps.java.txt",
                "src/main/resources/com/synexia/rewrite/hash-pinned-java/"
                "m3jdk21-generic-filtered-purpose-20261017/parse-context.tsv",
            ):
                path = fixture / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("unsealed", encoding="utf-8")
                rejects()
                path.unlink()
            for name in REQUIRED_PATHS:
                changed = json.loads(pins_data)
                changed["files"] = [row for row in changed["files"] if row["path"] != name]
                (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                rejects()
            for name in REQUIRED_GIT_BLOBS:
                changed = json.loads(pins_data)
                next(row for row in changed["files"] if row["path"] == name)[
                    "unchangedGitBlob"] = "0" * 40
                (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                rejects()
            for name in REQUIRED_SHA256:
                changed = json.loads(pins_data)
                next(row for row in changed["files"] if row["path"] == name)["sha256"] = "0" * 64
                (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                rejects()
            name = "src/test/java/com/synexia/rewrite/M3StringUtf8SuccessorChainRecipeTest.java"
            data = (fixture / name).read_bytes()
            altered = data + b"// changed test\n"
            (fixture / name).write_bytes(altered)
            changed = json.loads(pins_data)
            row = next(row for row in changed["files"] if row["path"] == name)
            row["sha256"] = hashlib.sha256(altered).hexdigest()
            row["unchangedGitBlob"] = git_blob(altered)
            (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
            rejects()
            (fixture / name).write_bytes(data)
            (fixture / "pins.json").write_bytes(pins_data)
            inputs()
            assert refused == 5 + len(REQUIRED_PATHS) + len(REQUIRED_GIT_BLOBS) + len(REQUIRED_SHA256) + 1
        finally:
            ROOT = original
    print("M3_STRING_SUCCESSOR_RECEIVING_CLOSURE_SELFTEST_PASS refused=" + str(refused))


def validate_python_log(data: bytes, tests: int, result: str) -> dict:
    text = data.decode("utf-8", errors="strict")
    counts = re.findall(r"(?m)^Ran ([0-9]+) tests? in [^\r\n]+$", text)
    if counts != [str(tests)] or "skipped=" in text:
        raise ValueError("emitted Python test inventory or skip drift")
    if not text.rstrip().endswith("\n" + result):
        raise ValueError("emitted Python result drift")
    return {"tests": tests, "result": result,
            "sha256": hashlib.sha256(data).hexdigest()}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--reports", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--repository", type=Path)
    args = parser.parse_args()
    pins, pins_data = inputs()
    if args.repository is not None:
        receiver(args.repository.resolve(), pins)
    if args.self_test:
        self_test(pins, pins_data)
    if not args.reports:
        print("M3_STRING_SUCCESSOR_RECEIVING_SDK_INPUTS_EXACT")
        return
    directory = ROOT / "target/surefire-reports"
    expected = sorted(directory / ("TEST-" + name + ".xml") for name in SUITES)
    if sorted(directory.glob("TEST-*.xml")) != expected:
        raise ValueError("missing or unexpected SDK test reports")
    reports = {}
    for name, methods in SUITES.items():
        data = (directory / ("TEST-" + name + ".xml")).read_bytes()
        reports[name] = {"counts": validate_report(data, name, methods),
                         "sha256": hashlib.sha256(data).hexdigest()}
    versions = {"v3":{"m3/synexia-import/current-full-borrow/check_string_phase.py":"3dd800d3d76523a52918f77f68371965b77527ad1bdefd1e267846da7aaf8b47","m3/synexia-import/current-full-borrow/test_string_phase_successors.py":"992b8524d4ed3f1fabdd4f2685adb6d55eb23d9e3de0683675695afa71e03e75","m3/compatibility/test_synexia_string_phase_checker_successor.py":"b569114a7e2e0d4380f1bbbb503f9a9c5c5914cdeca3aeebe4c9b4bab26c470f","m3/compatibility/fixtures/string-phase-checker-v2-20261017.py":"bad0fe420fc677a977079e0aa557ff733e2858939781786bc37d0c9f38d96ff3","m3/compatibility/synexia-string-phase-checker-successor-20261017-v3.json":"45a7b377ec949d75f12d030952eeab9b5175c42f4581a0d9e900ee994da7208c"},"v4":{"m3/compatibility/fixtures/string-phase-checker-v3-20261017.py":"3dd800d3d76523a52918f77f68371965b77527ad1bdefd1e267846da7aaf8b47","m3/compatibility/synexia-string-phase-checker-successor-20261017-v4.json":"b9e78abacc2a7e6bdf9ac21711812381f476288af2a1e289cf8368ec66e75643","m3/compatibility/test_synexia_string_phase_checker_successor.py":"b39ed26dfbeef7d830d54c234d4c6de73a823b41412d3161a200c1a0e4389073","m3/synexia-import/current-full-borrow/check_string_phase.py":"db38acfb6f2b04b5f119544e91642a3639bd6b73ae32e88f077e819b0a4375c1","m3/synexia-import/current-full-borrow/string-target-owner-successor-chain-20261017.tsv":"c56d86b7981dd31533a509aad97c25368df57b6e0297847dde8b3435e9c618ad","m3/synexia-import/current-full-borrow/test_string_phase_successors.py":"48796f0fd1be609bd50c7b83293c5c08bcf6de3261298d818f41529926da16cc"}}
    emitted = ROOT / "target/emitted"
    expected_outputs = {version + "/" + name: digest
                        for version, files in versions.items()
                        for name, digest in files.items()}
    actual = {path.relative_to(emitted).as_posix()
              for path in emitted.rglob("*") if path.is_file()}
    expected_logs = {"logs/v3-baseline.log", "logs/v4-candidate.log", "logs/v4-custody.log"}
    if actual != set(expected_outputs) | expected_logs:
        raise ValueError("serial recipe output inventory drift")
    for name, expected_digest in expected_outputs.items():
        if hashlib.sha256((emitted / name).read_bytes()).hexdigest() != expected_digest:
            raise ValueError("serial recipe output drift: " + name)
    python_logs = {}
    for name, tests, result in (
        ("v3-baseline.log", 26, "FAILED (failures=1, errors=2)"),
        ("v4-candidate.log", 29, "OK"),
        ("v4-custody.log", 13, "OK"),
    ):
        python_logs[name] = validate_python_log(
            (emitted / "logs" / name).read_bytes(), tests, result)
    receipt = {
        "schema": "M3_STRING_SUCCESSOR_RECEIVING_SDK_RECEIPT_V1",
        "scope": "ORIGINAL_1C1B_38DF_PROVIDER_AND_SERIAL_V3_V4_SOURCE_TOOLING",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": {"openrewriteAdapter": "1c1b1b568d369f688f6953161636bf36b484bf5a",
                      "manifestKernel": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2"},
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reports": reports,
        "pythonLogs": python_logs,
        "emittedVersionedOutputs": versions,
        "v3Baseline": {"tests": 26, "failures": 1, "errors": 2},
        "v4SuccessorSpectrum": {"tests": 29, "failures": 0, "errors": 0, "skipped": 0},
        "v4CompatibilitySpectrum": {"tests": 13, "failures": 0, "errors": 0, "skipped": 0},
        "v4NamedDescriptor": "EXECUTED",
        "wholePhaseCliByThisSdkModule": "NOT_RUN",
        "matchedJdkJtreg": "NOT_RUN",
        "runtimeJavaAndJniByThisSdkModule": "NOT_RUN",
        "runtimeAppliedByThisTask": False,
        "phaseAdvanced": False,
    }
    output = ROOT / "target/sdk-receipt.json"
    output.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))



if __name__ == "__main__":
    main()
