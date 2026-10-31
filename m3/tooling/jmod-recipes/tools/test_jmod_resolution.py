#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Real Java 21 regression for opt-in JMOD default-resolution suppression.

Run this dedicated suite with JAVA_HOME21=/absolute/jdk21 python3 -B
test_jmod_resolution.py -v. A missing/incomplete/wrong-version JDK is an error,
not a skipped test. Both descriptor lanes use the existing jmod_pack.py owner;
the fixtures are linked and executed by native JDK tools, without mock tools.
"""

import csv
import hashlib
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import tempfile
import unittest
import zipfile


OWNER = Path(__file__).resolve().parent
MODULE = "com.m3test.resolution"
PACKAGE = "com.m3test.resolution.shared"
PAYLOAD = PACKAGE + ".Payload"
WITNESS = PACKAGE + ".ClasspathCompatibilityProbe"
PROBE = "com.m3test.resolution.client.ResolutionProbe"
CONSUMER = "com.m3test.resolution.consumer"
STAMP = "2026-01-01T00:00:00Z"


def java21():
    value = os.environ.get("JAVA_HOME21")
    if not value or not Path(value).is_absolute():
        raise RuntimeError("JAVA_HOME21 must explicitly name an absolute, complete Java 21 JDK")
    home = Path(value).resolve(strict=True)
    release = (home / "release").read_text(encoding="utf-8")
    version = re.search(r'(?m)^JAVA_VERSION="(\d+)(?:[.\-+" ])', release)
    if version is None or version.group(1) != "21":
        raise RuntimeError("JAVA_HOME21 must name Java 21: " + str(home))
    for name in ("java", "javac", "jar", "jmod", "jlink"):
        path = home / "bin" / (name + (".exe" if os.name == "nt" else ""))
        if not path.is_file() or not os.access(path, os.X_OK):
            raise RuntimeError("JAVA_HOME21 is missing an executable JDK tool: " + str(path))
    if not (home / "jmods/java.base.jmod").is_file():
        raise RuntimeError("JAVA_HOME21 is missing jmods/java.base.jmod")
    return home


def execute(arguments, jdk, directory, *, check=True, timeout=180):
    """Bound each real command and, on POSIX, its child process group as well."""
    environment = os.environ.copy()
    for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH"):
        environment.pop(name, None)
    environment.update(JAVA_HOME=str(jdk), JAVA_HOME21=str(jdk),
                       PYTHONDONTWRITEBYTECODE="1", LC_ALL="C", LANG="C")
    command = [str(argument) for argument in arguments]
    with subprocess.Popen(command, cwd=directory, env=environment,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                          text=True, encoding="utf-8", errors="strict",
                          start_new_session=(os.name == "posix")) as process:
        try:
            stdout, stderr = process.communicate(timeout=timeout)
        except subprocess.TimeoutExpired as error:
            if os.name == "posix":
                try:
                    os.killpg(process.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
            else:
                process.kill()
            stdout, stderr = process.communicate(timeout=10)
            raise AssertionError("Command timed out: " + repr(command) + "\n"
                                 + stdout + stderr) from error
        result = subprocess.CompletedProcess(command, process.returncode, stdout, stderr)
    if check and result.returncode:
        raise AssertionError("Command failed (" + str(result.returncode) + "): "
                             + repr(command) + "\n" + result.stdout + result.stderr)
    return result


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


class JmodResolutionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.jdk = java21()
        if not (OWNER / "jmod_pack.py").is_file():
            raise RuntimeError("The existing jmod_pack.py owner is required beside this test")

    def setUp(self):
        directory = tempfile.TemporaryDirectory(prefix="m3-jmod-resolution-")
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        self.classes = self.root / "module-classes"
        self.classpath = self.root / "classpath-classes"
        self.consumer = self.root / "consumer-classes"
        version = self.run_command([self.tool("java"), "-XshowSettings:properties", "-version"])
        self.assertRegex(version.stderr, r"(?m)^\s*java\.specification\.version = 21\s*$")
        compiler = self.run_command([self.tool("javac"), "--version"])
        self.assertRegex(compiler.stdout, r"^javac 21(?:[.\-+\s]|$)")

    def tool(self, name, image=None):
        return (image or self.jdk) / "bin" / (name + (".exe" if os.name == "nt" else ""))

    def run_command(self, arguments, **options):
        return execute(arguments, self.jdk, self.root, **options)

    def source(self, relative, text):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8", newline="\n")
        return path

    def compile(self, destination, sources, *options):
        self.run_command([self.tool("javac"), "--release", "21", "-Xlint:all", "-Werror",
                          *options, "-d", destination, *sources])

    def fixtures(self):
        descriptor = self.source("sources/module-info.java", """module com.m3test.resolution {
    exports com.m3test.resolution.shared;
}
""")
        payload = self.source("sources/com/m3test/resolution/shared/Payload.java", """package com.m3test.resolution.shared;
public final class Payload {
    private Payload() {}
    public static String value() { return "fixture"; }
    public static void main(String[] args) {
        Module owner = Payload.class.getModule();
        if (!owner.isNamed() || !"com.m3test.resolution".equals(owner.getName())) {
            throw new AssertionError("Payload was not loaded from the installed fixture module");
        }
        System.out.println("M3_MODULE_ACTIVATION_PASS|module=" + owner.getName() + "|value=" + value());
    }
}
""")
        self.compile(self.classes, [descriptor, payload])
        witness = self.source("classpath/com/m3test/resolution/shared/ClasspathCompatibilityProbe.java", """package com.m3test.resolution.shared;
public final class ClasspathCompatibilityProbe {
    private ClasspathCompatibilityProbe() {}
    public static void main(String[] args) {
        if (ClasspathCompatibilityProbe.class.getModule().isNamed()) {
            throw new AssertionError("The unchanged classpath witness must remain unnamed");
        }
        System.out.println("M3_CLASSPATH_COMPATIBILITY_PASS");
    }
}
""")
        probe = self.source("classpath/com/m3test/resolution/client/ResolutionProbe.java", """package com.m3test.resolution.client;
public final class ResolutionProbe {
    private ResolutionProbe() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !("true".equals(args[0]) || "false".equals(args[0]))) {
            throw new AssertionError("Exactly one expected resolution state is required");
        }
        boolean expected = Boolean.parseBoolean(args[0]);
        boolean actual = ModuleLayer.boot().findModule("com.m3test.resolution").isPresent();
        if (actual != expected) {
            throw new AssertionError("Unexpected boot-layer module resolution: " + actual);
        }
        if (actual) {
            Class<?> type = Class.forName("com.m3test.resolution.shared.Payload");
            if (!"com.m3test.resolution".equals(type.getModule().getName())
                    || !"fixture".equals(type.getMethod("value").invoke(null))) {
                throw new AssertionError("Explicitly resolved module payload is unusable");
            }
        }
        System.out.println("M3_RESOLUTION_PASS|resolved=" + actual);
    }
}
""")
        self.compile(self.classpath, [witness, probe])
        consumer_descriptor = self.source("consumer/module-info.java", """module com.m3test.resolution.consumer {
    requires com.m3test.resolution;
}
""")
        consumer = self.source("consumer/com/m3test/resolution/consumer/Main.java", """package com.m3test.resolution.consumer;
import com.m3test.resolution.shared.Payload;
public final class Main {
    private Main() {}
    public static void main(String[] args) {
        String owner = Payload.class.getModule().getName();
        if (!"com.m3test.resolution".equals(owner) || !"fixture".equals(Payload.value())) {
            throw new AssertionError("A requires edge did not activate the installed module");
        }
        System.out.println("M3_REQUIRES_ACTIVATION_PASS|module=" + owner + "|value=" + Payload.value());
    }
}
""")
        self.compile(self.consumer, [consumer_descriptor, consumer], "--module-path", self.classes)
        return descriptor

    def resolution(self, image, expected, *options):
        result = self.run_command([self.tool("java", image), *options, "-cp", self.classpath,
                                   PROBE, str(expected).lower()])
        self.assertEqual("M3_RESOLUTION_PASS|resolved=" + str(expected).lower() + "\n", result.stdout)
        self.assertEqual("", result.stderr)

    def exercise(self, lane, suppress_default):
        descriptor = self.fixtures()
        # These identical class files must work on the input JDK before linking.
        self.resolution(self.jdk, False)
        baseline = self.run_command([self.tool("java"), "-cp", self.classpath, WITNESS])
        self.assertEqual("M3_CLASSPATH_COMPATIBILITY_PASS\n", baseline.stdout)
        self.assertEqual("", baseline.stderr)

        archive = self.root / (MODULE + ".jar")
        self.run_command([self.tool("jar"), "--create", "--file", archive, "--date", STAMP,
                          "-C", self.classes, "." if lane == "published" else "com"])
        with zipfile.ZipFile(archive) as jar:
            self.assertEqual(lane == "published", "module-info.class" in jar.namelist())
            self.assertNotIn(WITNESS.replace(".", "/") + ".class", jar.namelist())
        image_tsv = self.source("JMOD_IMAGE.tsv", "module\tjar\tdisposition\n" + (
            MODULE + "\t" + str(archive) + "\tMODULAR_JAR\n" if lane == "published" else ""))
        manifest = self.source("DESCRIPTOR_MANIFEST.tsv", "module\tunit_jars\ttarget\n" + (
            MODULE + "\t" + str(archive) + "\t" + descriptor.relative_to(self.root).as_posix() + "\n"
            if lane == "generated" else ""))
        sealed = {path: digest(path) for path in self.root.rglob("*") if path.is_file()}
        output = self.root / "packed"
        # --d was already an unambiguous --date abbreviation before the new flag.
        date_option = "--d" if lane == "published" and not suppress_default else "--date"
        command = [sys.executable, "-B", OWNER / "jmod_pack.py", "--jdk-home", self.jdk,
                   "--image-tsv", image_tsv, "--manifest", manifest, "--layer-root", self.root,
                   "--out", output, "--release", "21", date_option, STAMP, "--jmods-only"]
        if suppress_default:
            command.append("--do-not-resolve-by-default")
        packaged = self.run_command(command)
        self.assertIn("JMOD_PACK_PASS modules 1 release 21 runtime NOT_EXECUTED", packaged.stdout)
        with (output / "JMOD_PACK.tsv").open(encoding="utf-8", newline="") as stream:
            rows = list(csv.DictReader(stream, delimiter="\t"))
        self.assertEqual([{"module": MODULE, "descriptor_source": (
            "published" if lane == "published" else "m3-descriptor-for-jmod"), "result": "JMOD"}], rows)
        jmod = output / "jmods" / (MODULE + ".jmod")
        self.assertTrue(jmod.is_file())
        description = self.run_command([self.tool("jmod"), "describe", jmod]).stdout.splitlines()
        self.assertEqual(MODULE, description[0].split("@")[0])
        self.assertIn("exports " + PACKAGE, description)

        image = self.root / "image"
        self.run_command([self.tool("jlink"), "--module-path",
                          os.pathsep.join((str(self.jdk / "jmods"), str(jmod.parent))),
                          "--add-modules", "java.base," + MODULE, "--output", image])
        installed = self.run_command([self.tool("java", image), "--list-modules"])
        self.assertEqual({"java.base", MODULE},
                         {line.split("@")[0] for line in installed.stdout.splitlines()})
        self.run_command([self.tool("java", image), "--validate-modules"])
        self.resolution(image, not suppress_default)

        # Without the new option, retain and demonstrate the old default-root behavior.
        # With it, the precompiled same-package classpath application must still launch.
        witness = self.run_command([self.tool("java", image), "-cp", self.classpath, WITNESS], check=False)
        if suppress_default:
            self.assertEqual(0, witness.returncode, witness.stdout + witness.stderr)
            self.assertEqual("M3_CLASSPATH_COMPATIBILITY_PASS\n", witness.stdout)
            self.assertEqual("", witness.stderr)
        else:
            self.assertEqual(1, witness.returncode, witness.stdout + witness.stderr)
            self.assertEqual("", witness.stdout)
            self.assertIn("java.lang.ClassNotFoundException: " + WITNESS, witness.stderr)

        self.resolution(image, True, "--add-modules", MODULE)
        activated = self.run_command([self.tool("java", image), "--add-modules", MODULE,
                                      "-m", MODULE + "/" + PAYLOAD])
        self.assertEqual("M3_MODULE_ACTIVATION_PASS|module=" + MODULE + "|value=fixture\n", activated.stdout)
        self.assertEqual("", activated.stderr)
        required = self.run_command([self.tool("java", image), "--module-path", self.consumer,
                                     "-m", CONSUMER + "/" + CONSUMER + ".Main"])
        self.assertEqual("M3_REQUIRES_ACTIVATION_PASS|module=" + MODULE + "|value=fixture\n", required.stdout)
        self.assertEqual("", required.stderr)
        self.assertEqual(sealed, {path: digest(path) for path in sealed})

    def test_published_descriptor_preserves_default_without_option(self):
        self.exercise("published", False)

    def test_published_descriptor_opt_in_preserves_classpath(self):
        self.exercise("published", True)

    def test_generated_descriptor_preserves_default_without_option(self):
        self.exercise("generated", False)

    def test_generated_descriptor_opt_in_preserves_classpath(self):
        self.exercise("generated", True)


if __name__ == "__main__":
    unittest.main()
