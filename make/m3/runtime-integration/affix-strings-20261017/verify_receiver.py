#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact five-file source-sealed M3JDK receiver, not a full modified JVM proof."""
from pathlib import Path
from hashlib import sha256
import sys
root = Path(__file__).resolve().parents[3]
expected = {
"src/java.base/share/classes/java/lang/M3StringAffixPrecompute.java":"4ed050c240c4b5cec7c78f0c9732618df2046ce975cfcc5733fe874468a7aa98",
"src/java.base/share/classes/java/lang/M3StringRegexRowPrecompute.java":"c36341519d961257ed892c9e35a139258ebf711016bfe09e7c8e0af36c8f5f00",
"src/java.base/share/classes/java/util/Strings.java":"7147989eba1d6def126049042d8fe4bc7780fbaa972ea932bed411961117b717",
"test/jdk/java/lang/String/M3StringAffixPrecomputeTest.java":"5f5779e1b5521df29a14461739217fcb69906d88f98da9cf468a543abaab962e",
"test/jdk/java/util/Strings/StringsContractTest.java":"a77eadd3d62b9135f0d5cc8a88b72811a0b1b50e5591ec2f73cc0b3e87fedc6b"
}
for path,want in expected.items():
    f=root/path
    if not f.is_file() or sha256(f.read_bytes()).hexdigest()!=want:
        sys.exit("M3_AFFIX_STRINGS_RECEIVER_FAIL drift="+path)
affix=(root/"src/java.base/share/classes/java/lang/M3StringAffixPrecompute.java").read_text()
regex=(root/"src/java.base/share/classes/java/lang/M3StringRegexRowPrecompute.java").read_text()
for token in ("prefixRange(", "suffixRange(", "prefixParent(", "suffixParent(", "rowsInSourceOrder("):
    if token not in affix:sys.exit("M3_AFFIX_STRINGS_RECEIVER_FAIL lost affix="+token)
for token in ("reusedAffixes(", "indexedCandidates()", "Pattern.compile(", "execution.reset(values[row]).matches()"):
    if token not in regex:sys.exit("M3_AFFIX_STRINGS_RECEIVER_FAIL lost regex="+token)
print("M3_AFFIX_STRINGS_RECEIVER_SOURCE_PASS exactly_sealed=5 unchanged_jni=true")
