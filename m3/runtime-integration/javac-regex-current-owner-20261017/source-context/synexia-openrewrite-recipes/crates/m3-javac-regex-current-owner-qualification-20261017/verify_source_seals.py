#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Admit exact current owners and new qualification sources without changing historical seals."""
import hashlib
import os
import sys
from pathlib import Path, PurePosixPath

SOURCE_CONTEXT = "5cd39d31b23f91db23176409a947529b3476eb1e"
SOURCE_INPUTS = {"synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexJdkConstantRegexPlan.java":{"gitBlob":"d45fd92bc212a02e7415e31a558a6fb941e608bd","sha256":"940efc1c846bd869d542f2104c70b1c972cf89361133082850a6b0e888b9af87"},"synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFacts.java":{"gitBlob":"0003406b4d62499e919a9e5a1be0853906225158","sha256":"85e36a76dc85b75fda2d3e8d4577a73d586c33ce26ef04367c9236cda62425dd"},"synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/MIndexCompilerASTPrecompute.java":{"gitBlob":"1ac623b8fa822f4f8ce9ed9c3bb8c9a5194184b6","sha256":"74dd92f6a378763e040dc868a267f06b6c9af035d26fbfad0ac1224c3438a834"},"synexia-indexstring/pom.xml":{"gitBlob":"ae122d96a2363c4f0dc914ebfb9a06081b140217","sha256":"cb8d9fb8e14df44afac0ad164b3a0ff7439fd1d3a64c46389a0da489af1f3b26"},"synexia-indexstring/src/test/java/com/synexia/indexstring/MIndexJdkConstantRegexPlanContract.java":{"gitBlob":"92d77554ea1a8a242bc7a4b891a6af6a56bb4431","sha256":"cdd2d4704c647adbd5e6888a4228355a53b6dd7dc400d0e86fc04673e760fddf"},"synexia-mindex/compiler/src/test/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFactsContract.java":{"gitBlob":"901db786b0ad9306f2750524255b8bb8046f05e4","sha256":"12d064b6e11a9ca3c670340890a22e699125ded1e051a18407f085adf53c76f2"}}
QUALIFICATION_OUTPUTS = {"synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/pom.xml":{"gitBlob":"dd95ca0abeb4c7981cf3aaad7a8ec657be6fa2b1","sha256":"52428721ed0386f00311d4cbbc5deb96947220ce55e2698e13170bf173656c47"},"synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/verify.sh":{"gitBlob":"edefccb1828cddc85c58d71b50326d7ef0d7bb61","sha256":"50c697e1821bd1b358d326fdcb9fb3d13caa340af18875c9ca52e89ec92c7e1e"},"synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/src/test/java/com/synexia/indexstring/MIndexJdkConstantRegexCurrentOwnerContract.java":{"gitBlob":"ad2ac03772af9919be8a8a782b8308666cf1a27a","sha256":"1b87be56a58604ce1a5bb1f6ecb448771c269a8de21c78eecb81cb8e71cdbf29"}}

HISTORICAL_INPUTS = {"synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3JavacConstantRegexPrecomputeRecipe.java":{"gitBlob":"7c49bc0d1472f48386ed52476840079c4a488bde","sha256":"076be555dba3719f342b9259695c24cea887d66b7ce72827c74a361ed4680cb1"},"synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/manifest.tsv":{"gitBlob":"2ea8bbbaf7a93decb09f1bf4ecc756f2101892b8","sha256":"c623d462fa245abc1d3b964a908dac5bd7a2968703e507c64e2da587af4a49bc"},"synexia-openrewrite-recipes/crates/m3-javac-regex-precompute-20261014/verify_source_seals.py":{"gitBlob":"c8fafe24441a138e246068b4efac5c791bf359d0","sha256":"e4b4e7a2c4efec34661bca472c61ee164a75432d0781eccc0c1037e36f793b3e"},"synexia-openrewrite-recipes/crates/m3-javac-regex-precompute-20261014/verify.sh":{"gitBlob":"71559043407cf6cc03883d722e327e8ea44ed42a","sha256":"95dfdf356d3b3e641e126db80c6da2a1cf0aa12f0b19a2e80320548ed1154ee4"},"synexia-openrewrite-recipes/crates/m3-javac-regex-precompute-20261014/pom.xml":{"gitBlob":"646d4c82d2d621a14055239ad91f38bb2ad34b17","sha256":"5e20ae198167ac3281bae217157bab5a5d9f9ff37892a305deaac0c980aa3c60"},"synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3JavacConstantRegexPrecomputeRecipeTest.java":{"gitBlob":"2fd1927532cf27818a44024199256aec776b1df4","sha256":"3e19e63b38fb97b1964c050d422c6494a7acc3b46cda11b0da1cb3e09ddefb79"}}

def verify_files(root: Path, expected: dict) -> None:
    for relative, identities in expected.items():
        name = PurePosixPath(relative)
        if name.is_absolute() or ".." in name.parts or "\\" in relative:
            raise ValueError("unsafe sealed source path")
        path = root / relative
        if path.is_symlink() or not path.is_file():
            raise ValueError("missing or redirected sealed source: " + relative)
        data = path.read_bytes()
        if hashlib.sha256(data).hexdigest() != identities["sha256"]:
            raise ValueError("current-owner source drift: " + relative)
        identity = hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()
        if identity != identities["gitBlob"]:
            raise ValueError("current-owner Git identity drift: " + relative)

def verify_successor_contract(root: Path) -> None:
    old_path = root / "synexia-indexstring/src/test/java/com/synexia/indexstring/MIndexJdkConstantRegexPlanContract.java"
    current_path = root / "synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/src/test/java/com/synexia/indexstring/MIndexJdkConstantRegexCurrentOwnerContract.java"
    current = current_path.read_text(encoding="utf-8")
    marker = "\n    private static long charSequenceChecks;\n"
    start = current.find(marker)
    finish = current.rfind("}\n")
    if start < 0 or start != current.rfind(marker) or finish <= start:
        raise ValueError("successor additive region drift")
    restored = current[:start] + current[finish:]
    call = "        verifyCurrentCharSequences();\n"
    if restored.count(call) != 1:
        raise ValueError("successor call inventory drift")
    restored = restored.replace(call, "", 1)
    for after, before in (
        ("public final class MIndexJdkConstantRegexCurrentOwnerContract {",
         "public final class MIndexJdkConstantRegexPlanContract {"),
        ("private MIndexJdkConstantRegexCurrentOwnerContract() {}",
         "private MIndexJdkConstantRegexPlanContract() {}"),
    ):
        if restored.count(after) != 1:
            raise ValueError("successor declaration inventory drift")
        restored = restored.replace(after, before, 1)
    if restored.encode("utf-8") != old_path.read_bytes():
        raise ValueError("original contract corpus or main was not preserved byte-exactly")

def verify_storage() -> None:
    value = os.environ.get("TMPDIR")
    if not value:
        raise ValueError("HOLD_STORAGE: explicit admitted TMPDIR is required")
    temporary = Path(value)
    if not temporary.is_absolute() or temporary.is_symlink() or not temporary.is_dir():
        raise ValueError("HOLD_STORAGE: explicit existing non-symlink TMPDIR required")
    if not hasattr(os, "statvfs"):
        raise ValueError("HOLD_STORAGE: existing POSIX bounded worker is required")
    facts = os.statvfs(temporary)
    if facts.f_bavail * facts.f_frsize < 8 * 1024**3 + 512 * 1024**2:
        raise ValueError("HOLD_STORAGE: 8 GiB reserve after planned 512 MiB")
    # The existing external worker retains continuous aggregate output/process admission.
    # This read-only gate never claims that an environment variable proves supervision.

def verify(root: Path) -> None:
    verify_files(root, SOURCE_INPUTS)
    verify_files(root, QUALIFICATION_OUTPUTS)
    verify_files(root, HISTORICAL_INPUTS)
    verify_successor_contract(root)
    verify_storage()
    print("M3_JAVAC_CURRENT_OWNER_SOURCE_SEALS=PASS owners=6 historicalInputs=6 qualificationOutputs=3 originalCorpus=EXACT")
    print("SCOPE JDK-only four-source proof; real MIndex bridge/JNI/transparent lowering remain NOT_RUN")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: verify_source_seals.py REPOSITORY_ROOT")
    verify(Path(sys.argv[1]).resolve())
