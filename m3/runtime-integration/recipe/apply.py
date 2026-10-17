#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Apply a complete reviewed runtime patch; refuse mixed or drifted states."""
import argparse,hashlib,json,subprocess
from pathlib import Path,PurePosixPath
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
def digest(data): return hashlib.sha256(data).hexdigest()

# Exact four-owner JNI modified-UTF8 source receipt: Synexia #11288,
# M3JDK21 commit 56d29d32b128de0d998e933f957e090383e8d2dc.
# Historical runtime.patch bytes are immutable and cannot replace these owners.
MUTF8_FANIN = {
    "src/hotspot/share/classfile/javaClasses.cpp": (
        "10816a3adb62ae3dd5b7fdefb79a43947637fb90924298ace56c8320ea4d60cb",
        (),
        "933764d74cca6a40dc01fef1798c38d71a4ac0b754e5b17d9ed42d2a05bc68f6",
    ),
    "src/hotspot/share/classfile/javaClasses.hpp": (
        "36a6ae73f908a42bd565ea8caca830f7de63e423092c92141f0fd4110dc75df3",
        (),
        "d20fa631a2df68353d7e0645690fbb36dd4a1d38c8ce99379596ded1aa912a59",
    ),
    "src/hotspot/share/classfile/javaClasses.inline.hpp": (
        "18edfbc5b4ae95130b6fcfe19fd9927abd0e4cc5504129d9a20b025b10f0b7a1",
        (),
        "bbe505ccc6b5ab1482f0e0235284d625f0cc75181a7b0d418f2fb3c722d30cdf",
    ),
    "src/java.base/share/classes/java/lang/M3StringFacts.java": (
        "c834e63534db855f06e99bbfdf45caf0ecf36b88f5760bfe7fdaa8bcc355509f",
        ("c834e63534db855f06e99bbfdf45caf0ecf36b88f5760bfe7fdaa8bcc355509f",
         "f1cc0ec9d4fd0bbc15d9c684e80b9907b60321cb15ad4fd7fa39f3cf973fc4cd",),
        "2152c154d3cdece330667ee09675fd21d86f3b483a23a3156b8a3626c3f9f420",
    ),
}

def verify_mutf8_fanin(manifest, observed=None):
    advanced = []
    for name, (old_postimage, legacy, admitted) in MUTF8_FANIN.items():
        entry = manifest["files"].get(name)
        if (entry is None or entry.get("after") != old_postimage
                or entry.get("superseded") != [*legacy, admitted]):
            raise ValueError("modified UTF-8 recipe seal drift: " + name)
        if observed is None:
            continue
        actual = observed.get(name)
        if actual not in {entry.get("before"), old_postimage, *entry["superseded"]}:
            raise ValueError("modified UTF-8 source drift: " + name)
        advanced.append(actual == admitted)
    if advanced and any(advanced) and not all(advanced):
        raise ValueError("partial modified UTF-8 native/Java owner fan-in")

# Current source custody is a separate whole-cohort state. These identities are
# never added to the historical per-file "superseded" allowlist.
CURRENT_COHORT_FILE = "current-cohort-20261017.json"
CURRENT_COHORT_SHA256 = "a2481edf200087aa9efb0d2825ef95a083124bf32a8182359b04c56b2d022bb3"
CURRENT_SDK_ROOT = "m3/runtime-integration/current-custody-20261017/sdk"
CURRENT_SUCCESSOR_PATHS = frozenset({
    "src/java.base/share/classes/java/lang/M3String.java",
    "src/java.base/share/classes/java/lang/M3StringPool.java",
    "src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java",
    "src/java.base/share/classes/java/lang/String.java",
    "src/java.base/share/classes/java/util/regex/M3PatternQuery.java",
    "src/java.base/share/classes/java/util/regex/Pattern.java",
})
CURRENT_AUXILIARY_PATHS = frozenset({
    "src/java.base/share/classes/java/lang/M3StringRegexDp.java",
    "src/java.base/share/native/libjava/M3StringRegexDp.c",
    "test/jdk/java/util/regex/M3PatternAsciiFrontierTest.java",
    "test/jdk/java/util/regex/libM3PatternAsciiFrontierTest.c",
    "m3/docs/name-mapping.d/m3-pattern-ascii-frontier.json",
    "m3/compatibility/m3-pattern-ascii-receive-20261014.json",
})


def _relative_path(name):
    if not isinstance(name, str) or not name or "\\" in name:
        raise ValueError("unsafe custody path: " + str(name))
    safe = PurePosixPath(name)
    if safe.is_absolute() or ".." in safe.parts or safe.as_posix() != name:
        raise ValueError("unsafe custody path: " + name)
    return safe


def _hex(value, width):
    return (isinstance(value, str) and len(value) == width
            and all(char in "0123456789abcdef" for char in value))


def _custody_rows(value, label, count, role=False):
    if not isinstance(value, list) or len(value) != count:
        raise ValueError("current custody " + label + " inventory drift")
    result = {}
    keys = {"path", "gitBlob", "sha256", "bytes"}
    if role:
        keys.add("role")
    for row in value:
        if not isinstance(row, dict) or set(row) != keys:
            raise ValueError("current custody " + label + " row drift")
        name = row["path"]
        _relative_path(name)
        if (name in result or not _hex(row["gitBlob"], 40)
                or not _hex(row["sha256"], 64)
                or type(row["bytes"]) is not int or not 0 < row["bytes"] <= 16 * 1024**2
                or role and (not isinstance(row["role"], str) or not row["role"])):
            raise ValueError("current custody " + label + " identity drift: " + name)
        result[name] = row
    return result


def validate_current_cohort(cohort, manifest):
    """Validate the sealed source-custody contract without reading test resources."""
    if (not isinstance(cohort, dict)
            or cohort.get("schema") != "M3_RUNTIME_CURRENT_SOURCE_CUSTODY_V1"
            or cohort.get("scope") != "EXACT_CURRENT_SOURCE_CUSTODY_ONLY"
            or cohort.get("sourceCustodyOnly") is not True
            or any(cohort.get(key) is not False for key in (
                "runtimeQualified", "jniQualified", "jdkQualified",
                "performanceQualified", "phaseAdvanced"))):
        raise ValueError("current custody scope drift")
    files = _custody_rows(cohort.get("files"), "source", 66)
    auxiliaries = _custody_rows(cohort.get("auxiliaries"), "auxiliary", 6, role=True)
    if set(files) != set(manifest["files"]):
        raise ValueError("current custody legacy target set drift")
    if set(auxiliaries) != CURRENT_AUXILIARY_PATHS or set(files) & set(auxiliaries):
        raise ValueError("current custody auxiliary target set drift")
    successors = cohort.get("successorPaths")
    if (not isinstance(successors, list) or len(successors) != 6
            or set(successors) != CURRENT_SUCCESSOR_PATHS):
        raise ValueError("current custody successor set drift")
    for name, row in files.items():
        if _relative_path(name).parts[0] != "src":
            raise ValueError("unsafe custody source path: " + name)
        old = manifest["files"][name]
        admitted = {old["after"], *old.get("superseded", [])}
        if (row["sha256"] in admitted) == (name in CURRENT_SUCCESSOR_PATHS):
            raise ValueError("current custody successor lineage drift: " + name)
    legacy = cohort.get("legacy")
    if not isinstance(legacy, dict):
        raise ValueError("current custody legacy binding drift")
    for label, expected_path in (
            ("manifest", "m3/runtime-integration/recipe/manifest.json"),
            ("patch", "m3/runtime-integration/recipe/runtime.patch")):
        row = legacy.get(label)
        if (not isinstance(row, dict) or set(row) != {"path", "gitBlob", "sha256"}
                or row.get("path") != expected_path
                or not _hex(row.get("gitBlob"), 40) or not _hex(row.get("sha256"), 64)):
            raise ValueError("current custody legacy binding drift: " + label)
    resources = cohort.get("testResources")
    if not isinstance(resources, dict) or resources.get("sdkRoot") != CURRENT_SDK_ROOT:
        raise ValueError("current custody public fixture root drift")
    rows = resources.get("files")
    if not isinstance(rows, list) or len(rows) != 11:
        raise ValueError("current custody fixture inventory drift")
    seen = set()
    historic = set()
    legacy_admitted = []
    prefix = "src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/"
    for row in rows:
        if (not isinstance(row, dict)
                or set(row) != {"targetPath", "resource", "role", "gitBlob", "sha256", "bytes"}):
            raise ValueError("current custody fixture row drift")
        name = row["targetPath"]
        resource = row["resource"]
        _relative_path(resource)
        if (name not in files or resource in seen
                or not _hex(row["gitBlob"], 40) or not _hex(row["sha256"], 64)
                or type(row["bytes"]) is not int or not 0 < row["bytes"] <= 16 * 1024**2):
            raise ValueError("current custody fixture identity drift")
        seen.add(resource)
        if row["role"] == "historical-after":
            if (resource != prefix + "historical-after/" + name
                    or row["sha256"] != manifest["files"][name]["after"]):
                raise ValueError("current custody historical fixture drift: " + name)
            historic.add(name)
        elif row["role"] == "legacy-admitted":
            if (resource != prefix + "legacy-admitted/" + name
                    or name != "src/java.base/share/classes/java/lang/M3String.java"
                    or row["sha256"] != "24ac1f87c1b2022b08d695ae6b116e9376dad59ed0ffb86f52da0003dd3e036d"
                    or row["sha256"] not in manifest["files"][name].get("superseded", [])):
                raise ValueError("current custody admitted fixture drift: " + name)
            legacy_admitted.append(name)
        else:
            raise ValueError("current custody fixture role drift")
    expected_historic = CURRENT_SUCCESSOR_PATHS | set(MUTF8_FANIN)
    if historic != expected_historic or len(legacy_admitted) != 1:
        raise ValueError("current custody fixture target set drift")
    return files, auxiliaries


def load_current_cohort(manifest, manifest_data, patch_data):
    path = HERE / CURRENT_COHORT_FILE
    if path.is_symlink() or not path.is_file() or path.stat().st_size > 256 * 1024:
        raise ValueError("current custody table missing or redirected")
    data = path.read_bytes()
    if digest(data) != CURRENT_COHORT_SHA256:
        raise ValueError("current custody table seal drift")
    cohort = json.loads(data)
    validate_current_cohort(cohort, manifest)
    if (digest(manifest_data) != cohort["legacy"]["manifest"]["sha256"]
            or digest(patch_data) != cohort["legacy"]["patch"]["sha256"]):
        raise ValueError("current custody historical artifact drift")
    return cohort


def observe_source(target, name):
    """Check a source path before reading; current and legacy files share this guard."""
    safe = _relative_path(name)
    path = target.joinpath(*safe.parts)
    for parent in (path, *path.parents):
        if parent == target:
            break
        if parent.is_symlink():
            raise ValueError("symlink path: " + name)
    if target not in path.resolve().parents:
        raise ValueError("symlink path: " + name)
    return digest(path.read_bytes()) if path.exists() else None


def verify_current_custody(target, manifest, cohort):
    """Return one exact source-only current state, or defer to historical dispatch.

    A known current source is observed for classification, never independently
    admitted. Any nonempty partial successor set must fail the whole cohort.
    """
    files, auxiliaries = validate_current_cohort(cohort, manifest)
    observed = {name: observe_source(target, name) for name in manifest["files"]}
    has_current_successor = any(
        observed[name] == files[name]["sha256"] for name in CURRENT_SUCCESSOR_PATHS)
    for name, actual in observed.items():
        if has_current_successor and actual is None:
            raise ValueError("current custody missing source: " + name)
        old = manifest["files"][name]
        known = {old["before"], old["after"], *old.get("superseded", []),
                 files[name]["sha256"]}
        if actual not in known:
            raise ValueError("source drift: " + name)
    verify_mutf8_fanin(manifest, observed)
    if not has_current_successor:
        return None
    for name, row in files.items():
        if observed[name] != row["sha256"]:
            raise ValueError("partial current runtime custody cohort: " + name)
    auxiliary_observed = {name: observe_source(target, name) for name in auxiliaries}
    for name, row in auxiliaries.items():
        if auxiliary_observed[name] != row["sha256"]:
            raise ValueError("current custody auxiliary drift: " + name)
    return "current-custody"


def apply(target,reverse=False,check=False):
    target=target.resolve()
    manifest_data=(HERE/'manifest.json').read_bytes()
    manifest=json.loads(manifest_data)
    verify_mutf8_fanin(manifest)
    patch=HERE/'runtime.patch'
    patch_data=patch.read_bytes()
    if digest(patch_data)!=manifest['patch_sha256']:raise ValueError('patch drift')
    cohort=load_current_cohort(manifest,manifest_data,patch_data)
    custody=verify_current_custody(target,manifest,cohort)
    if custody is not None:return custody
    states=set()
    observed={}
    for name,hashes in manifest['files'].items():
        safe=PurePosixPath(name)
        if safe.is_absolute() or '..' in safe.parts or safe.parts[0]!='src':raise ValueError('unsafe path')
        p=target/name
        if p.is_symlink() or target not in p.resolve().parents:raise ValueError('symlink path')
        actual=digest(p.read_bytes()) if p.exists() else None
        observed[name]=actual
        if actual==hashes['before']:states.add('before')
        elif actual==hashes['after']:states.add('after')
        elif actual in hashes.get('superseded',[]):states.add('superseded')
        else:raise ValueError('source drift: '+name)
    verify_mutf8_fanin(manifest,observed)
    if states=={'before'}:current='before'
    elif states and states.issubset({'after','superseded'}):
        current='superseded' if 'superseded' in states else 'after'
    else:raise ValueError('mixed runtime patch state')
    if current=='superseded':return current
    desired='before' if reverse else 'after'
    if current==desired or check:return current
    cmd=['git','-c','core.autocrlf=false','apply']+(['--reverse'] if reverse else [])
    subprocess.run(cmd+['--check',str(patch)],cwd=target,check=True)
    # git apply validates all hunks before writing and refuses partial patches.
    subprocess.run(cmd+[str(patch)],cwd=target,check=True)
    for name,hashes in manifest['files'].items():
        if (digest((target/name).read_bytes()) if (target/name).exists() else None)!=hashes[desired]:raise ValueError('postimage mismatch: '+name)
    return desired
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--target',type=Path,default=ROOT)
    p.add_argument('--reverse',action='store_true');p.add_argument('--check',action='store_true')
    a=p.parse_args()
    state=apply(a.target,a.reverse,a.check)
    print('MINDEX_INTEGRATION_RECIPE_PASS state='+state)
    if state=='current-custody':
        print('M3_RUNTIME_CURRENT_CUSTODY scope=SOURCE_ONLY owners=72 runtimeQualified=false jniQualified=false jdkQualified=false phaseAdvanced=false')
