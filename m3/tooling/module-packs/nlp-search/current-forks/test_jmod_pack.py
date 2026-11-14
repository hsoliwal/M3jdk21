#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-JDK packer probes. Prepare fixtures in COMPILE; run assertions afterward in TEST."""
import argparse
import contextlib
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import stat
import subprocess
import sys
import unittest
from unittest import mock
import warnings
import zipfile


STAMP = "2001-01-01T00:00:00Z"
MODULE = "fixture.sample"


def digest(path):
    value = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def command(log, *argv):
    argv = [str(value) for value in argv]
    result = subprocess.run(argv, text=True, capture_output=True, encoding="utf-8", errors="replace")
    with log.open("a", encoding="utf-8") as stream:
        stream.write(json.dumps({"argv": argv, "exit": result.returncode,
                                 "stdout": result.stdout, "stderr": result.stderr}) + "\n")
    return result


def write_jar(path, entries, final_metadata=None):
    with zipfile.ZipFile(path, "x", compression=zipfile.ZIP_STORED) as archive:
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)  # Deliberate duplicate-path refusal fixture.
            for name, payload in entries:
                entry = name if isinstance(name, zipfile.ZipInfo) else zipfile.ZipInfo(name, (2001, 1, 1, 0, 0, 0))
                if not isinstance(name, zipfile.ZipInfo):
                    entry.external_attr = (stat.S_IFREG | 0o644) << 16
                archive.writestr(entry, payload)
        # Deliberate central-directory refusal fixtures; compiled class bytes stay intact.
        for name, fields in (final_metadata or {}).items():
            for key, value in fields.items():
                setattr(archive.getinfo(name), key, value)


def prepare(args):
    out = args.out
    if out.exists() or out.is_symlink():
        raise FileExistsError("prepare requires an absent output root: " + str(out))
    for home in (args.jdk21_home, args.jdk25_home):
        for name in ("release", "bin/javac", "bin/jmod"):
            if not (home / name).is_file():
                raise ValueError("missing real JDK input: " + str(home / name))
    out.mkdir(parents=True)
    fixtures = out / "fixtures"
    fixtures.mkdir()
    log = out / "prepare-commands.jsonl"
    log.touch(exist_ok=False)
    for feature, home in ((21, args.jdk21_home), (25, args.jdk25_home)):
        source = fixtures / ("source" + str(feature))
        (source / "fixture").mkdir(parents=True)
        (source / "module-info.java").write_text("module fixture.sample { exports fixture; }\n", encoding="utf-8")
        (source / "fixture/Value.java").write_text(
            "package fixture; public final class Value { private Value() {} "
            "public static int selected() { return " + str(feature) + "; } }\n", encoding="utf-8")
        sources = [source / "module-info.java", source / "fixture/Value.java"]
        if feature == 25:
            (source / "future").mkdir()
            extra = source / "future/OnlyOn25.java"
            extra.write_text("package future; public final class OnlyOn25 { private OnlyOn25() {} }\n",
                             encoding="utf-8")
            sources.append(extra)
        classes = fixtures / ("classes" + str(feature))
        result = command(log, home / "bin/javac", "--release", str(feature), "-Xlint:all", "-Werror",
                         "-d", classes, *sources)
        if result.returncode:
            raise RuntimeError("real fixture compilation failed; see " + str(log))
    module_info = (fixtures / "classes21/module-info.class").read_bytes()
    class21 = (fixtures / "classes21/fixture/Value.class").read_bytes()
    class25 = (fixtures / "classes25/fixture/Value.class").read_bytes()
    base = [("module-info.class", module_info), ("fixture/Value.class", class21), ("payload.txt", b"base21")]
    manifest = ("META-INF/MANIFEST.MF", b"Manifest-Version: 1.0\r\nMulti-Release: true\r\n\r\n")
    overlays = [("META-INF/versions/25/fixture/Value.class", class25),
                ("META-INF/versions/25/future/OnlyOn25.class", (fixtures / "classes25/future/OnlyOn25.class").read_bytes()),
                ("META-INF/versions/25/payload.txt", b"selected25"),
                ("META-INF/versions/26/payload.txt", b"future26")]
    write_jar(fixtures / "mr.jar", [manifest, *base, *overlays])
    write_jar(fixtures / "base.jar", base)
    write_jar(fixtures / "high.jar", [("module-info.class", module_info), ("fixture/Value.class", class25)])
    write_jar(fixtures / "undeclared-mr.jar", [*base, *overlays])
    write_jar(fixtures / "false-mr.jar", [(manifest[0], manifest[1].replace(b"true", b"false")), *base, *overlays])
    write_jar(fixtures / "duplicate.jar", [*base, ("payload.txt", b"duplicate")])
    for filename, path in (("parent.jar", "../escape.bin"), ("absolute.jar", "/escape.bin"),
                           ("backslash.jar", "..\\escape.bin"), ("drive.jar", "C:/escape.bin")):
        write_jar(fixtures / filename, [*base, (path, b"refuse")])
    write_jar(fixtures / "conflict.jar", [*base, ("owner", b"file"), ("owner/child", b"refuse")])
    write_jar(fixtures / "effective-conflict.jar", [manifest, *base, ("owner", b"file"),
                                                    ("META-INF/versions/25/owner/child", b"refuse")])
    write_jar(fixtures / "signed.jar", [*base, ("META-INF/TEST.SF", b"Signature-Version: 1.0\n")])
    symlink = zipfile.ZipInfo("link", (2001, 1, 1, 0, 0, 0))
    symlink.create_system = 3
    symlink.external_attr = (stat.S_IFLNK | 0o777) << 16
    write_jar(fixtures / "symlink.jar", [*base, (symlink, b"../escape")])
    directory_name = "META-INF/maven/"

    def directory(name=directory_name, **fields):
        entry = zipfile.ZipInfo(name, (2001, 1, 1, 0, 0, 0))
        entry.create_system = 3
        entry.external_attr = 0xffff0010
        for key, value in fields.items():
            setattr(entry, key, value)
        return entry

    directory_entries = [(directory(), b""), ("META-INF/maven/fixture.properties", b"retained metadata")]
    write_jar(fixtures / "directory.jar", [*base, *directory_entries])
    write_jar(fixtures / "directory-mr.jar", [manifest, *base, *overlays, *directory_entries])
    for label, name, fields, payload, final in (
            ("not-slash", directory_name[:-1], {}, b"", {}),
            ("no-dos-bit", directory_name, {"external_attr": 0xffff0000}, b"", {}),
            ("system", directory_name, {"create_system": 0}, b"", {}),
            ("symlink", directory_name, {"external_attr": ((stat.S_IFLNK | 0o777) << 16) | 0x10}, b"", {}),
            ("payload", directory_name, {}, b"x", {}),
            ("deflated", directory_name, {"compress_type": zipfile.ZIP_DEFLATED}, b"", {}),
            ("encrypted", directory_name, {}, b"", {"flag_bits": 1}),
            ("flags", directory_name, {}, b"", {"flag_bits": 8}),
            ("crc", directory_name, {}, b"", {"CRC": 1}),
            ("size", directory_name, {}, b"", {"compress_size": 1}),
            ("unsafe", "../escape/", {}, b"", {}),
            ("local-name", directory_name, {}, b"", {"filename": "META-INF/other/"})):
        write_jar(fixtures / ("directory-" + label + ".jar"),
                  [*base, (directory(name, **fields), payload)], {name: final})
    write_jar(fixtures / "directory-duplicate.jar", [*base, (directory(), b""), (directory(), b"")])
    write_jar(fixtures / "directory-conflict.jar", [*base, ("META-INF", b"file"), (directory(), b"")])
    write_jar(fixtures / "bad-class.jar", [*base, ("fixture/Invalid.class", b"not a class")])
    for feature, body in ((21, class21), (25, class25)):
        write_jar(fixtures / ("plain" + str(feature) + ".jar"), [("fixture/Value.class", body)])
    layer = fixtures / "layer"
    layer.mkdir()
    (layer / "module-info.java").write_text("module fixture.generated { exports fixture; }\n", encoding="utf-8")
    lock = {str(path.relative_to(fixtures)): digest(path) for path in sorted(fixtures.rglob("*")) if path.is_file()}
    (out / "FIXTURE_LOCK.json").write_text(json.dumps({
        "files": lock,
        "jdk21_release_sha256": digest(args.jdk21_home / "release"),
        "jdk25_release_sha256": digest(args.jdk25_home / "release"),
        "stage": "FIXTURE_COMPILE_ONLY_NO_PACKER_TESTS",
    }, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("JMOD_PACK_FIXTURES_READY", out)


class PackerChecks(unittest.TestCase):
    out = None
    jdk21 = None
    jdk25 = None
    packer = None

    @classmethod
    def setUpClass(cls):
        lock = json.loads((cls.out / "FIXTURE_LOCK.json").read_text(encoding="utf-8"))
        cls.fixtures = cls.out / "fixtures"
        for name, expected in lock["files"].items():
            if digest(cls.fixtures / name) != expected:
                raise ValueError("fixture drift: " + name)
        for feature, home in ((21, cls.jdk21), (25, cls.jdk25)):
            if digest(home / "release") != lock["jdk" + str(feature) + "_release_sha256"]:
                raise ValueError("fixture JDK release identity changed")
        owner = Path(__file__).resolve().parents[3] / "jmod-recipes/tools/jmod_pack.py"
        spec = importlib.util.spec_from_file_location("m3_existing_jmod_packer_probe", owner)
        cls.packer = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.packer)
        cls.log = cls.out / "test-commands.jsonl"
        cls.log.touch(exist_ok=False)

    def setUp(self):
        self.case = self.out / self.id().split(".")[-1]
        self.case.mkdir()
        self.calls = []

    def manifests(self, jars=(), generated=()):
        image = self.case / "JMOD_IMAGE.tsv"
        image.write_text("module\tjar\tdisposition\n" + "".join(
            module + "\t" + str(self.fixtures / jar) + "\tJMOD\n" for module, jar in jars), encoding="utf-8")
        manifest = self.case / "DESCRIPTOR_MANIFEST.tsv"
        manifest.write_text("module\tunit_jars\ttarget\n" + "".join(
            module + "\t" + str(self.fixtures / jar) + "\tmodule-info.java\n"
            for module, jar in generated), encoding="utf-8")
        return image, manifest

    def invoke(self, image, manifest, feature=25, name="pack", extra=()):
        destination = self.case / name
        args = ["--image-tsv", str(image), "--manifest", str(manifest), "--layer-root",
                str(self.fixtures / "layer"), "--out", str(destination), "--jdk-home",
                str(self.jdk21 if feature == 21 else self.jdk25), "--release", str(feature),
                "--date", STAMP, "--jmods-only", *extra]

        def actual(*argv):
            self.calls.append([str(value) for value in argv])
            return command(self.log, *argv)

        output = io.StringIO()
        try:
            with mock.patch.object(self.packer, "run", side_effect=actual), contextlib.redirect_stdout(output):
                result = self.packer.main(args)
        finally:
            (self.case / (name + ".stdout.log")).write_text(output.getvalue(), encoding="utf-8")
        return result, destination, output.getvalue()

    def rejected(self, jar, pattern, feature=25, extra=()):
        image, manifest = self.manifests(((MODULE, jar),))
        with self.assertRaisesRegex((ValueError, zipfile.BadZipFile), pattern):
            self.invoke(image, manifest, feature=feature, extra=extra)
        self.assertFalse((self.case / "pack").exists())
        self.assertEqual(self.calls, [])

    def test_conflicting_flags_refuse_before_output(self):
        self.rejected("base.jar", "jmods-only", extra=("--root-module", MODULE))

    def test_newer_requested_release_refuses_before_output(self):
        self.rejected("base.jar", "older than requested", extra=("--release", "26"))

    def test_duplicate_named_modules_refuse_before_output(self):
        image, manifest = self.manifests(((MODULE, "base.jar"), (MODULE, "mr.jar")))
        with self.assertRaisesRegex(ValueError, "duplicate module"):
            self.invoke(image, manifest)
        self.assertFalse((self.case / "pack").exists())
        self.assertEqual(self.calls, [])

    def test_duplicate_generated_modules_refuse_before_output(self):
        image, manifest = self.manifests(generated=((MODULE, "plain21.jar"), (MODULE, "plain25.jar")))
        with self.assertRaisesRegex(ValueError, "duplicate or cross-lane"):
            self.invoke(image, manifest)
        self.assertFalse((self.case / "pack").exists())
        self.assertEqual(self.calls, [])

    def test_cross_lane_modules_refuse_before_output(self):
        image, manifest = self.manifests(((MODULE, "base.jar"),), ((MODULE, "plain21.jar"),))
        with self.assertRaisesRegex(ValueError, "duplicate or cross-lane"):
            self.invoke(image, manifest)
        self.assertFalse((self.case / "pack").exists())
        self.assertEqual(self.calls, [])

    def test_class_major_is_bounded_by_selected_release(self):
        self.rejected("high.jar", "effective class major 69", feature=21)

    def test_invalid_archive_admission(self):
        fixtures = {"undeclared-mr.jar": "Multi-Release", "false-mr.jar": "Multi-Release",
                    "duplicate.jar": "duplicate ZIP", "parent.jar": "unsafe ZIP",
                    "absolute.jar": "unsafe ZIP", "backslash.jar": "unsafe ZIP",
                    "drive.jar": "unsafe ZIP", "conflict.jar": "file/directory conflict",
                    "effective-conflict.jar": "effective ZIP file/directory conflict",
                    "signed.jar": "signed JAR", "symlink.jar": "special ZIP", "bad-class.jar": "class header"}
        for filename, message in fixtures.items():
            with self.subTest(jar=filename):
                image, manifest = self.manifests(((MODULE, filename),))
                with self.assertRaisesRegex(ValueError, message):
                    self.invoke(image, manifest, name=filename + "-pack")
                self.assertFalse((self.case / (filename + "-pack")).exists())
                self.assertEqual(self.calls, [])

    def test_effective_21_and_25_views_use_actual_class_bytes(self):
        image, manifest = self.manifests(((MODULE, "mr.jar"),))
        for feature, payload in ((21, b"base21"), (25, b"selected25")):
            result, destination, output = self.invoke(image, manifest, feature, "release" + str(feature))
            self.assertEqual(result, 0, output)
            with zipfile.ZipFile(destination / "jmods" / (MODULE + ".jmod")) as archive:
                self.assertEqual(archive.read("classes/payload.txt"), payload)
                self.assertEqual(archive.read("classes/fixture/Value.class"),
                                 (self.fixtures / ("classes" + str(feature)) / "fixture/Value.class").read_bytes())
                self.assertFalse(any("META-INF/versions/" in name for name in archive.namelist()))
            self.assertIn("runtime NOT_EXECUTED", output)
        self.assertTrue(self.calls)
        self.assertTrue(all(Path(argv[0]).stem not in ("java", "jlink") for argv in self.calls))

    def test_exact_empty_dos_directory_preserves_real_classes_and_resources(self):
        for filename, feature in (("directory.jar", 21), ("directory-mr.jar", 25)):
            before = digest(self.fixtures / filename)
            image, manifest = self.manifests(((MODULE, filename),))
            result, destination, output = self.invoke(image, manifest, feature, filename + "-pack")
            self.assertEqual(result, 0, output)
            with zipfile.ZipFile(destination / "jmods" / (MODULE + ".jmod")) as archive:
                self.assertEqual(archive.read("classes/fixture/Value.class"),
                                 (self.fixtures / ("classes" + str(feature)) / "fixture/Value.class").read_bytes())
                self.assertEqual(archive.read("classes/META-INF/maven/fixture.properties"), b"retained metadata")
                self.assertFalse(any("META-INF/versions/" in name for name in archive.namelist()))
            self.assertEqual(digest(self.fixtures / filename), before)
            self.assertIn("runtime NOT_EXECUTED", output)
        self.assertTrue(self.calls)
        self.assertTrue(all(Path(argv[0]).stem not in ("java", "jlink") for argv in self.calls))

    def test_directory_sentinel_variants_refuse_before_output(self):
        variants = {label: "special ZIP" for label in
                    ("not-slash", "no-dos-bit", "system", "symlink", "payload", "deflated",
                     "encrypted", "flags", "crc", "size")}
        variants.update({"unsafe": "unsafe ZIP", "duplicate": "duplicate ZIP",
                         "conflict": "file/directory conflict", "local-name": "differ"})
        for label, message in variants.items():
            with self.subTest(variant=label):
                filename = "directory-" + label + ".jar"
                image, manifest = self.manifests(((MODULE, filename),))
                with self.assertRaisesRegex((ValueError, zipfile.BadZipFile), message):
                    self.invoke(image, manifest, name=label + "-pack")
                self.assertFalse((self.case / (label + "-pack")).exists())
                self.assertEqual(self.calls, [])

    def test_generated_descriptors_target_the_requested_release(self):
        for feature in (21, 25):
            image, manifest = self.manifests(generated=(("fixture.generated", "plain" + str(feature) + ".jar"),))
            result, destination, output = self.invoke(image, manifest, feature, "release" + str(feature))
            self.assertEqual(result, 0, output)
            with zipfile.ZipFile(destination / "jmods/fixture.generated.jmod") as archive:
                descriptor = archive.read("classes/module-info.class")
                self.assertEqual(int.from_bytes(descriptor[6:8], "big"), feature + 44)
        self.assertTrue(any(Path(argv[0]).stem == "javac" for argv in self.calls))
        self.assertTrue(all(Path(argv[0]).stem not in ("java", "jlink") for argv in self.calls))

    def test_descriptor_inventory_and_normalization_receive_release(self):
        owner = Path(self.packer.__file__).with_name("descriptor_gen.py")
        spec = importlib.util.spec_from_file_location("m3_existing_descriptor_probe", owner)
        descriptors = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(descriptors)
        jar = self.fixtures / "mr.jar"
        self.packer.validate_jar(jar, 25)
        default = descriptors.class_names(jar)
        selected = descriptors.class_names(jar, release=25)
        self.assertEqual(default, descriptors.class_names(jar, release=21))
        self.assertIn("fixture.Value", default)
        self.assertNotIn("future.OnlyOn25", default)
        self.assertEqual(selected, default | {"future.OnlyOn25"})
        source = "module fixture.generated {\n    exports fixture;\n    exports future;\n}\n"
        original, dropped21 = descriptors.normalize(source, jar, default)
        current, dropped25 = descriptors.normalize(source, jar, selected, release=25)
        self.assertNotIn("exports future;", original)
        self.assertTrue(dropped21)
        self.assertIn("exports future;", current)
        self.assertEqual(dropped25, [])

    def test_two_independent_jmods_are_identical_with_date(self):
        image, manifest = self.manifests(((MODULE, "mr.jar"),))
        first = self.invoke(image, manifest, name="first")
        second = self.invoke(image, manifest, name="second")
        self.assertEqual((first[0], second[0]), (0, 0))
        relative = Path("jmods") / (MODULE + ".jmod")
        self.assertEqual(digest(first[1] / relative), digest(second[1] / relative))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phase", choices=("prepare", "test"), required=True)
    parser.add_argument("--jdk21-home", type=Path, required=True)
    parser.add_argument("--jdk25-home", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    args.out = args.out.absolute()
    if args.phase == "prepare":
        prepare(args)
        return 0
    PackerChecks.out, PackerChecks.jdk21, PackerChecks.jdk25 = args.out, args.jdk21_home, args.jdk25_home
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(PackerChecks)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main())
