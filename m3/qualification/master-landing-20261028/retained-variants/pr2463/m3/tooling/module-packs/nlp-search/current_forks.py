#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build the pinned current Lucene/OpenNLP forks and the optional M3 source integration.

Source changes are canonical Maven/OpenRewrite recipe outputs. This driver verifies
their locks, compiles existing owners, and reuses the M3 JMOD packager. It does not
rewrite Java or replace the older vendored build.py lane.
"""
import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
MODULE = "jdk.m3.nlp.search"
EPOCH = (2000, 1, 1, 0, 0, 0)


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(command, log, *, env=None):
    with log.open("w", encoding="utf-8") as output:
        result = subprocess.run(list(map(str, command)), stdout=output,
                                stderr=subprocess.STDOUT, env=env, check=False)
    if result.returncode:
        raise RuntimeError("command failed (%d): %s; see %s" %
                           (result.returncode, command[0], log))


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


_RECEIVER_LOCK_SHA256 = "68ac22eb0dd7e609297517b75c6670242afe51e723c456eb251eb1e7017053cf"
_RECEIVER_TRANSITION_PATH = "m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V5.json"
_RECEIVER_TRANSITION_SHA256 = "4e00d68a96907e23b10670c32ca76bdae5b9bc7f1b6d27cdf323f17b6a1fd178"
_RECEIVER_TRANSITION_V6_PATH = "m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V6.json"
_RECEIVER_TRANSITION_V6_SHA256 = "25c475dc211870634a0a156ddd02ce3db78a492997019ecdf01c599db9b296b4"
_RECEIVER_TRANSITION_V7_PATH = "m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V7.json"
_RECEIVER_TRANSITION_V7_SHA256 = "e3f7f6c569d701f2c663c1ca34dcf2d8d5330ecbe8e2e8ddf61764975f636b5e"
_RECEIVER_TRANSITION_V8_PATH = "m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V8.json"
_RECEIVER_TRANSITION_V8_SHA256 = "5abff0ff5462a74439ceb13610913417ca5a1b2c4c8ddf0ba9d26a2428f1237d"
_RECEIVER_TRANSITION_V9_PATH = "m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V9.json"
_RECEIVER_TRANSITION_V9_SHA256 = "8b9033adaea873f6253558db142e6823762913da1899b714f019eaf6816e99d3"
_RECEIVER_TRANSITION_V10_PATH = "m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V10.json"
_RECEIVER_TRANSITION_V10_SHA256 = "178b7c3a0e120b0d5ea00eaf5b1724227e5261f73fe3677e4686b1d2d2b2ac6a"
_RECEIVER_TRANSITION_V11_PATH = "m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V11.json"
_RECEIVER_TRANSITION_V11_SHA256 = "d937901cb78b253a969f25d74f98cb715cc4064241115e0bbf1704aeb1e88f20"
_RECEIVER_TRANSITION_V12_PATH = "m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V12.json"
_RECEIVER_TRANSITION_V12_SHA256 = "2a596a61ecac808df5f5e1ca935ca7dd58e5a5c1cef9836537998ef21b094f6f"
_RECEIVER_SUCCESSORS = frozenset(
    "m3/core/src/com/m3/text/" + name for name in (
        "JoinedM3StringPiece.java", "SharedLexiconCatalog.java",
        "SharedLexiconPrecomputeCatalog.java", "SharedRelatedLexemeCatalog.java"))
_RECEIVER_V8_SUCCESSORS = _RECEIVER_SUCCESSORS | {"m3/core/src/com/m3/text/SynexiaPrecomputePayload.java"}


def _receiver_identity(raw):
    return {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(),
            "gitBlob": hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()}


def _receiver_inputs(root, lock):
    files, bodies = [], {}
    for module, entry in lock["modules"].items():
        for relative, expected in entry["files"].items():
            path = root / relative
            if not path.is_file():
                raise ValueError("receiver source drift: " + relative)
            raw = path.read_bytes()
            identity = _receiver_identity(raw)
            if identity["sha256"] != expected:
                raise ValueError("receiver source drift: " + relative)
            files.append(dict(module=module, path=relative, **identity))
            bodies[relative] = raw
        if entry.get("source_roots"):
            actual = {p.relative_to(root).as_posix() for source in entry["source_roots"]
                      for p in (root / source).rglob("*.java")}
            expected = {p for p in entry["files"] if p.endswith(".java")}
            if actual != expected:
                raise ValueError("receiver source coverage drift: " + module)
    return sorted(files, key=lambda row: (row["path"], row["module"])), bodies


def verify_receiver(root):
    # Preserve the original strict API and its unchanged lock return value.
    lock = json.loads((HERE / "current-source-lock.json").read_text(encoding="utf-8"))
    _receiver_inputs(root, lock)
    return lock


def _receiver_admission(root, transition=None):
    raw = (HERE / "current-source-lock.json").read_bytes()
    lock = json.loads(raw)
    effective = json.loads(raw)
    selected, declaration = [], None
    if transition is not None:
        path = transition if transition.is_absolute() else root / transition
        choices = {_RECEIVER_TRANSITION_PATH: _RECEIVER_TRANSITION_SHA256,
                   _RECEIVER_TRANSITION_V6_PATH: _RECEIVER_TRANSITION_V6_SHA256,
                   _RECEIVER_TRANSITION_V7_PATH: _RECEIVER_TRANSITION_V7_SHA256,
                   _RECEIVER_TRANSITION_V8_PATH: _RECEIVER_TRANSITION_V8_SHA256,
                   _RECEIVER_TRANSITION_V9_PATH: _RECEIVER_TRANSITION_V9_SHA256,
                   _RECEIVER_TRANSITION_V10_PATH: _RECEIVER_TRANSITION_V10_SHA256,
                   _RECEIVER_TRANSITION_V11_PATH: _RECEIVER_TRANSITION_V11_SHA256,
                   _RECEIVER_TRANSITION_V12_PATH: _RECEIVER_TRANSITION_V12_SHA256}
        matches = [name for name in choices if path.resolve() == (root / name).resolve()]
        if (len(matches) != 1
                or any(p.is_symlink() or p.is_junction() for p in (path, *path.parents))):
            raise ValueError("unknown or redirected receiver transition")
        selected_path = matches[0]
        data = path.read_bytes()
        if (hashlib.sha256(raw).hexdigest() != _RECEIVER_LOCK_SHA256
                or hashlib.sha256(data).hexdigest() != choices[selected_path]):
            raise ValueError("receiver transition or original lock seal differs")
        document = json.loads(data)
        if document["schema"] != "M3_FROZEN_RECEIVER_TRANSITIONS_V2":
            raise ValueError("receiver transition schema differs")
        successors = (_RECEIVER_V8_SUCCESSORS if selected_path in (_RECEIVER_TRANSITION_V8_PATH, _RECEIVER_TRANSITION_V9_PATH, _RECEIVER_TRANSITION_V10_PATH, _RECEIVER_TRANSITION_V11_PATH, _RECEIVER_TRANSITION_V12_PATH)
                      else _RECEIVER_SUCCESSORS)
        rows = {row["path"]: row for row in document["files"]
                if row["path"] in successors}
        expected_rows = 9 if selected_path in (_RECEIVER_TRANSITION_V8_PATH, _RECEIVER_TRANSITION_V9_PATH, _RECEIVER_TRANSITION_V10_PATH, _RECEIVER_TRANSITION_V11_PATH, _RECEIVER_TRANSITION_V12_PATH) else 8
        if set(rows) != successors or len(document["files"]) != expected_rows:
            raise ValueError("complete receiver source transition cohort required")
        source = lock["modules"]["com.m3.text"]["files"]
        for relative in sorted(rows):
            row = rows[relative]
            if row["before"]["sha256"] != source[relative] or row["before"] == row["after"]:
                raise ValueError("receiver transition basis differs: " + relative)
            effective["modules"]["com.m3.text"]["files"][relative] = row["after"]["sha256"]
            selected.append(row)
        for relative, digest in document["custodySha256"].items():
            if source.get(relative) != digest:
                raise ValueError("receiver transition custody differs: " + relative)
        declaration = dict(path=selected_path, **_receiver_identity(data))
    files, bodies = _receiver_inputs(root, effective)
    observed = {row["path"]: {key: row[key] for key in ("bytes", "sha256", "gitBlob")}
                for row in files}
    for row in selected:
        if observed[row["path"]] != row["after"]:
            raise ValueError("receiver transition postimage differs: " + row["path"])
    encoded = json.dumps(files, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return dict(lock=lock, effective=effective, lockIdentity=_receiver_identity(raw),
                transition=declaration, changes=selected, files=files, bodies=bodies,
                manifestSha256=hashlib.sha256(encoded).hexdigest())


def _stage_receiver_inputs(out, admission):
    root = out / "verified-receiver-sources"
    root.mkdir()
    for relative, raw in admission["bodies"].items():
        path = root / relative
        if not path.resolve().is_relative_to(root.resolve()) or path.exists():
            raise ValueError("unsafe or occupied receiver snapshot")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(raw)
    files, _ = _receiver_inputs(root, admission["effective"])
    if files != admission["files"]:
        raise ValueError("staged receiver inputs differ")
    return root


def _revalidate_receiver(root, admission, staged=None):
    transition = root / admission["transition"]["path"] if admission["transition"] else None
    current = _receiver_admission(root, transition)
    for key in ("lockIdentity", "transition", "changes", "files", "manifestSha256"):
        if current[key] != admission[key]:
            raise ValueError("receiver input changed during build: " + key)
    if staged is not None:
        files, _ = _receiver_inputs(staged, admission["effective"])
        if files != admission["files"]:
            raise ValueError("staged receiver inputs changed during build")


def _receiver_revision(root):
    try:
        value = subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=root, text=True, stderr=subprocess.DEVNULL,
            env=dict(os.environ, GIT_NO_LAZY_FETCH="1", GIT_OPTIONAL_LOCKS="0"), timeout=30).strip()
    except (FileNotFoundError, subprocess.CalledProcessError, subprocess.TimeoutExpired):
        return None
    return value if len(value) == 40 and all(c in "0123456789abcdef" for c in value) else None


def _receiver_report(admission, revision, checkpoints, staged):
    cohort = "original-strict-lock"
    if staged:
        selected = admission["transition"]["path"]
        cohort = ("declared-v12-five-lexicon-successors" if selected == _RECEIVER_TRANSITION_V12_PATH
                  else "declared-v11-five-lexicon-successors" if selected == _RECEIVER_TRANSITION_V11_PATH
                  else "declared-v10-five-lexicon-successors" if selected == _RECEIVER_TRANSITION_V10_PATH
                  else "declared-v9-five-lexicon-successors" if selected == _RECEIVER_TRANSITION_V9_PATH
                  else "declared-v8-five-lexicon-successors" if selected == _RECEIVER_TRANSITION_V8_PATH
                  else "declared-v7-four-lexicon-successors" if selected == _RECEIVER_TRANSITION_V7_PATH
                  else "declared-v6-four-lexicon-successors" if selected == _RECEIVER_TRANSITION_V6_PATH
                  else "declared-v5-four-lexicon-successors")
    return dict(schema="M3_NLP_VERIFIED_RECEIVER_INPUTS_V1", cohort=cohort,
                basis=admission["lockIdentity"], observed_revision=revision,
                declared_transition=admission["transition"], transitions=admission["changes"],
                effective_manifest=admission["files"],
                effective_manifest_sha256=admission["manifestSha256"],
                compilation_inputs="verified-owned-snapshot" if staged else "original-physical-inputs",
                revalidation=checkpoints)


def deterministic_jar(classes, target):
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as archive:
        for source in sorted(p for p in classes.rglob("*") if p.is_file()):
            item = zipfile.ZipInfo(str(source.relative_to(classes)), EPOCH)
            item.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(item, source.read_bytes())


def collect_jmods(build, jars, legal, owners):
    paths = sorted((build / "pack/jmods").glob("*.jmod"))
    if not paths:
        raise ValueError("donor produced no JMODs: " + str(build))
    receipt = {}
    for source in paths:
        module = source.stem
        entries = {}
        with zipfile.ZipFile(source) as archive:
            for item in archive.infolist():
                if item.is_dir():
                    continue
                name = item.filename
                if name.startswith("/") or ".." in Path(name).parts:
                    raise ValueError("unsafe JMOD entry: " + name)
                data = archive.read(item)
                if name.startswith("classes/"):
                    relative = name[len("classes/"):]
                    if relative in entries:
                        raise ValueError("duplicate JMOD class/resource: " + name)
                    entries[relative] = data
                elif name.startswith("legal/"):
                    # Donor legal paths are already scoped; reuse them across JMODs.
                    target = legal / name[len("legal/"):]
                    target.parent.mkdir(parents=True, exist_ok=True)
                    if target.exists() and target.read_bytes() != data:
                        raise ValueError("conflicting legal notice: " + name)
                    target.write_bytes(data)
                else:
                    raise ValueError("Unaccounted JMOD section; preserve it explicitly: " + name)
        if "module-info.class" not in entries:
            raise ValueError("donor JMOD has no explicit module descriptor: " + module)
        fingerprint = hashlib.sha256()
        for name, data in sorted(entries.items()):
            encoded = name.encode("utf-8")
            fingerprint.update(len(encoded).to_bytes(4, "big"))
            fingerprint.update(encoded)
            fingerprint.update(len(data).to_bytes(8, "big"))
            fingerprint.update(data)
        content_sha256 = fingerprint.hexdigest()
        if module in owners:
            if owners[module] != content_sha256:
                raise ValueError("different implementations claim the same module: " + module)
        else:
            owners[module] = content_sha256
            with zipfile.ZipFile(jars / (module + ".jar"), "w", zipfile.ZIP_DEFLATED) as archive:
                for name, data in sorted(entries.items()):
                    item = zipfile.ZipInfo(name, EPOCH)
                    item.compress_type = zipfile.ZIP_DEFLATED
                    archive.writestr(item, data)
        receipt[module] = {"input_jmod_sha256": sha256(source), "content_sha256": content_sha256}
    return receipt


def compile_owner(jdk, root, out, module, entry):
    classes = out / "classes" / module
    classes.mkdir(parents=True)
    sources = [root / p for p in sorted(entry["files"])
               if p.endswith(".java") and p != entry["descriptor"]]
    argfile = out / (module + ".sources")
    argfile.write_text("\n".join('"' + str(p).replace("\\", "\\\\").replace('"', '\\"')
                                + '"' for p in sources) + "\n", encoding="utf-8")
    jars = sorted((out / "jars").glob("*.jar"))
    # Compile the exact existing classpath surface first. Descriptor publication is a separate
    # step so inherited exported-constructor lint does not mutate unrelated source contracts.
    run([jdk / "bin/javac", "--release", "21", "--enable-preview", "-encoding", "UTF-8",
         "-Xlint:all,-preview", "-Werror", "-classpath", os.pathsep.join(map(str, jars)),
         "-d", classes, "@" + str(argfile)], out / (module + "-compile.log"))
    run([jdk / "bin/javac", "--release", "21", "-Xlint:all,-module", "-Werror",
         "--module-path", os.pathsep.join(map(str, jars)), "--patch-module", module + "=" + str(classes),
         "-d", classes, root / entry["descriptor"]], out / (module + "-descriptor.log"))
    deterministic_jar(classes, out / "jars" / (module + ".jar"))


def verify_image(jdk, out):
    image = out / "pack/image"
    tests = out / "tests"
    tests.mkdir()
    services = tests / "META-INF/services"
    services.mkdir(parents=True)
    (services / "com.synexia.mindex.MIndexTensorBackend").write_text(
        "jdk.m3.nlp.search.test.SourceIntegrationProbe$ExternalBackend\n", encoding="utf-8")
    run([image / "bin/javac", "-source", "21", "-target", "21", "--enable-preview",
         "-Xlint:all,-preview", "-Werror", "--add-modules", MODULE,
         "-d", tests, HERE / "tests/SourceFactsProbe.java", HERE / "tests/SourceIntegrationProbe.java",
         HERE / "tests/ModelSourceProbe.java"],
        out / "probe-compile.log")
    for name in ("SourceFactsProbe", "SourceIntegrationProbe", "ModelSourceProbe"):
        command = [image / "bin/java", "--enable-preview", "--add-modules", MODULE,
                   "-ea", "-Xmx512m", "-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
                   "-cp", tests, "jdk.m3.nlp.search.test." + name]
        if name == "SourceIntegrationProbe":
            command.append(out / "source-search-fixture")
        run(command, out / (name + ".log"))
    run([image / "bin/java", "--enable-preview", "-m",
         "org.apache.opennlp.tools/opennlp.tools.cmdline.CLI"], out / "opennlp-cli.log")
    run([image / "bin/java", "--validate-modules"], out / "validate-modules.log")
    run([image / "bin/java", "--list-modules"], out / "modules.txt")
    return {name: (out / (name + ".log")).read_text(encoding="utf-8").strip()
            for name in ("SourceFactsProbe", "SourceIntegrationProbe", "ModelSourceProbe")}


def build(args):
    root = args.receiver.resolve()
    jdk = args.jdk_home.resolve()
    for executable in ("java", "javac", "jar", "jmod", "jlink"):
        if not (jdk / "bin" / (executable + (".exe" if os.name == "nt" else ""))).is_file():
            raise ValueError("complete JDK 21 required: missing " + executable)
    admission = _receiver_admission(root, getattr(args, "receiver_transition", None))
    lock = admission["lock"]
    revision = _receiver_revision(root)
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=False)
    (out / "jars").mkdir()
    (out / "legal").mkdir()
    staged = _stage_receiver_inputs(out, admission) if admission["transition"] else None
    compile_root = staged if staged is not None else root
    checkpoints = []
    def check_inputs(phase):
        _revalidate_receiver(root, admission, staged)
        if _receiver_revision(root) != revision:
            raise ValueError("receiver revision changed during build")
        checkpoints.append(dict(phase=phase, manifest_sha256=admission["manifestSha256"]))

    packager = root / "m3/tooling/jmod-recipes/tools/jmod_pack.py"
    opennlp = args.opennlp_source.resolve()
    lucene = args.lucene_source.resolve()
    environment = dict(os.environ, JAVA_HOME=str(jdk))
    command = [sys.executable, opennlp / "dev/m3-jmod/build.py", "--source-root", opennlp,
               "--jdk-home", jdk, "--packager", packager, "--out", out / "opennlp"]
    if args.opennlp_dependencies:
        command += ["--dependency-dir", args.opennlp_dependencies.resolve()]
    else:
        command += ["--maven", args.maven]
        if args.maven_settings:
            command += ["--maven-settings", args.maven_settings.resolve()]
    run(command, out / "opennlp-build.log", env=environment)
    owners = {}
    donor_receipts = {"opennlp": collect_jmods(out / "opennlp", out / "jars", out / "legal", owners)}
    command = [sys.executable, lucene / ".m3/current-jmods/build.py", "--source", lucene,
               "--jdk-home", jdk, "--out", out / "lucene", "--packager", packager,
               "--recipe-runner", args.recipe_runner.resolve(),
               "--recipe-resources", args.recipe_resources.resolve()]
    for jar in sorted((out / "jars").glob("*.jar")):
        command += ["--opennlp-module", jar]
    if args.lucene_dependencies:
        command += ["--dependency-dir", args.lucene_dependencies.resolve()]
    else:
        command += ["--maven", args.maven]
        if args.maven_settings:
            command += ["--maven-settings", args.maven_settings.resolve()]
    run(command, out / "lucene-build.log", env=environment)
    donor_receipts["lucene"] = collect_jmods(out / "lucene", out / "jars", out / "legal", owners)
    for module in ("com.m3.text", "jdk.m3.compiletime", MODULE):
        check_inputs("before:" + module)
        compile_owner(jdk, compile_root, out, module, admission["effective"]["modules"][module])
        check_inputs("after:" + module)
    with (out / "IMAGE.tsv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("artifact", "version", "jar", "module", "requires", "disposition"))
        for jar in sorted((out / "jars").glob("*.jar")):
            writer.writerow((jar.stem, "source-locked", jar, jar.stem, "-", "JMOD"))
    (out / "DESCRIPTORS.tsv").write_text("module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n")
    run([sys.executable, packager, "--jdk-home", jdk, "--image-tsv", out / "IMAGE.tsv",
         "--manifest", out / "DESCRIPTORS.tsv", "--layer-root", root, "--out", out / "pack",
         "--legal-notices", out / "legal", "--add-modules",
         "java.se,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jdeps,jdk.unsupported,jdk.incubator.vector"],
        out / "pack.log")
    tests = verify_image(jdk, out)
    check_inputs("before-report")
    report = {"schema": "M3_CURRENT_FORK_NLP_SEARCH_1", "receiver": lock["receiver"],
              "source_lock_sha256": sha256(HERE / "current-source-lock.json"),
              "java_release": 21, "preview_required": True,
              "runtime_dependency_on_com_synexia_repository": False,
              "donor_jmods": donor_receipts,
              "module_jars": {p.stem: sha256(p) for p in sorted((out / "jars").glob("*.jar"))},
              "tests": tests,
              "receiver_input_admission": _receiver_report(admission, revision, checkpoints, staged)}
    write_json(out / "BUILD_REPORT.json", report)
    print("M3_CURRENT_FORK_NLP_SEARCH_PASS modules=%d image=%s" %
          (len(report["module_jars"]), out / "pack/image"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--receiver", type=Path, default=ROOT)
    parser.add_argument("--receiver-transition", type=Path,
                        help="Explicit sealed V5/V6/V7 four-source or V8/V9/V10/V11/V12 five-source cohort; omitted keeps strict original admission")
    parser.add_argument("--opennlp-source", type=Path, required=True)
    parser.add_argument("--lucene-source", type=Path, required=True)
    parser.add_argument("--recipe-runner", type=Path, required=True)
    parser.add_argument("--recipe-resources", type=Path, required=True)
    parser.add_argument("--opennlp-dependencies", type=Path)
    parser.add_argument("--lucene-dependencies", type=Path)
    parser.add_argument("--maven", default="mvn")
    parser.add_argument("--maven-settings", type=Path)
    build(parser.parse_args())


if __name__ == "__main__":
    main()
