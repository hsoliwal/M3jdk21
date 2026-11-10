# SPDX-License-Identifier: Apache-2.0
"""Developer image/link-kit atoms. Reuses the existing assembler; never installs into JAVA_HOME."""
import json
import os
import shutil
from pathlib import Path
from build_pack import STAMP, command, digest

DEVELOPER_ROOTS = (
    "jdk.charsets", "jdk.compiler", "jdk.crypto.ec", "jdk.jartool", "jdk.javadoc",
    "jdk.jdeps", "jdk.jlink", "jdk.jpackage", "jdk.jshell", "jdk.zipfs",
)
MODULE = "example.toolproof"
MAIN = "example.toolproof.Main"
EXPECTED = "M3_DEVELOPER_FIXTURE_PASS"
JAVA = '''package example.toolproof;
/** Minimal toolchain contract fixture, not an application framework. */
public final class Main {
    private Main() {}
    /**
     * Execute the fixture.
     * @param args unused fixture arguments
     */
    public static void main(String[] args) {
        if (java.util.stream.IntStream.rangeClosed(1, 6).sum() != 21) {
            throw new AssertionError("fixture");
        }
        System.out.println("M3_DEVELOPER_FIXTURE_PASS");
    }
}
'''


def copy_link_inputs(home, output):
    """Copy matching JDK JMODs as a separate explicit link kit, with before/after byte pins."""
    destination = output / "link-inputs"
    destination.mkdir()
    rows = []
    for archive in sorted((home / "jmods").glob("*.jmod")):
        before = digest(archive)
        target = destination / archive.name
        shutil.copyfile(archive, target)
        if before != digest(target) or before != digest(archive):
            raise ValueError("JDK JMOD changed during copy: " + archive.name)
        rows.append((archive.name, before))
    if not {"java.base.jmod", "jdk.compiler.jmod", "jdk.jlink.jmod"}.issubset(
            {name for name, _ in rows}):
        raise ValueError("Incomplete Java21 development link kit")
    (destination / "SHA256.tsv").write_text("file\tsha256\n" + "".join(
        name + "\t" + checksum + "\n" for name, checksum in rows))
    return rows


def verify_link_pins(inventory, copied, home):
    """Bind selected platform link inputs to the earlier inspected artifact bytes."""
    expected = dict(copied)
    for line in inventory.splitlines()[1:]:
        cells = line.split("\t")
        if Path(cells[4]).parent == home / "jmods":
            if expected.get(cells[0] + ".jmod") != cells[3]:
                raise ValueError("Selected JDK module changed since inventory: " + cells[0])


def verify_developer_tools(image, output, link_inputs):
    """Exercise linked tools and an actual native app-image launcher, with no ambient JDK paths."""
    root = output / "developer-proof"
    root.mkdir()
    logs = root / "logs"
    logs.mkdir()
    source = root / "src" / MODULE
    package = source / "example/toolproof"
    package.mkdir(parents=True)
    (source / "module-info.java").write_text(
        "/** Developer toolchain fixture. */ module " + MODULE + " { exports example.toolproof; }\n")
    (package / "Main.java").write_text(JAVA)
    (package / "package-info.java").write_text("/** Developer toolchain fixture. */ package example.toolproof;\n")
    classes = root / "classes"
    args = [image / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-d", classes]
    command(args + sorted(source.rglob("*.java")), logs / "javac.log")
    jars = root / "jars"
    jars.mkdir()
    jar = jars / "toolproof.jar"
    command([image / "bin/jar", "--create", "--file", jar, "--date", STAMP,
             "--main-class", MAIN, "-C", classes, "."], logs / "jar.log")
    description = command([image / "bin/jar", "--describe-module", "--file", jar], logs / "describe.log")
    if MODULE not in description:
        raise ValueError("Missing explicit fixture module")
    execution = command([image / "bin/java", "-Xcheck:jni", "--module-path", jar,
                         "-m", MODULE + "/" + MAIN], logs / "java.log")
    if EXPECTED not in execution:
        raise ValueError("Fixture execution failed")
    bytecode = command([image / "bin/javap", "-verbose", "-classpath", jar, MAIN], logs / "javap.log")
    if "major version: 65" not in bytecode:
        raise ValueError("Fixture is not Java21 bytecode")
    dependencies = command([image / "bin/jdeps", "--print-module-deps", jar], logs / "jdeps.log")
    if "java.base" not in dependencies:
        raise ValueError("Dependency probe failed")
    command([image / "bin/javadoc", "-quiet", "-Xdoclint:all", "-Werror", "-d", root / "api",
             *sorted(source.rglob("*.java"))], logs / "javadoc.log")
    jmod = root / "toolproof.jmod"
    command([image / "bin/jmod", "create", "--date", STAMP, "--class-path", classes, jmod], logs / "jmod.log")
    runtime = root / "runtime"
    command([image / "bin/jlink", "--module-path", os.pathsep.join([str(link_inputs), str(jmod)]),
             "--add-modules", MODULE, "--output", runtime], logs / "jlink.log")
    linked = command([runtime / "bin/java", "-m", MODULE + "/" + MAIN], logs / "linked-run.log")
    if EXPECTED not in linked:
        raise ValueError("Linked application failed")
    script = root / "probe.jsh"
    script.write_text('{ int total = java.util.stream.IntStream.rangeClosed(1, 6).sum(); if (total != 21) throw new AssertionError(); System.out.println("M3_JSHELL_PASS"); }\n/exit\n')
    shell = command([image / "bin/jshell", "-J-Djava.util.prefs.userRoot=" + str(root / "prefs"),
                     "-J-Djava.util.prefs.systemRoot=" + str(root / "system-prefs"),
                     "--feedback", "silent", script], logs / "jshell.log")
    if "M3_JSHELL_PASS" not in shell:
        raise ValueError("JShell execution failed")
    # This password protects disposable fixture material, not a user key or credential.
    store = root / "fixture.p12"
    command([image / "bin/keytool", "-genkeypair", "-alias", "fixture", "-dname", "CN=M3Fixture",
             "-keyalg", "RSA", "-keysize", "2048", "-validity", "1", "-storetype", "PKCS12",
             "-storepass", "changeit", "-keystore", store], logs / "keytool-create.log")
    listing = command([image / "bin/keytool", "-list", "-storepass", "changeit", "-keystore", store],
                      logs / "keytool-read.log")
    if "fixture" not in listing:
        raise ValueError("KeyStore readback failed")
    applications = root / "applications"
    command([image / "bin/jpackage", "--type", "app-image", "--runtime-image", image,
             "--input", jars, "--main-jar", jar.name, "--main-class", MAIN,
             "--dest", applications, "--temp", root / "package-temp", "--name", "M3DeveloperProbe", "--java-options", "-Xcheck:jni"],
            logs / "jpackage.log", 180)
    launcher = applications / "M3DeveloperProbe/bin/M3DeveloperProbe"
    packaged = command([launcher], logs / "native-launcher.log")
    if EXPECTED not in packaged:
        raise ValueError("Native packaged launcher failed")
    receipt = {"status": "LOCAL_DEVELOPER_TOOLS_PROVEN",
               "tools": ["java", "javac", "jar", "javap", "jdeps", "javadoc", "jmod", "jlink",
                         "jshell", "keytool", "jpackage"],
               "native_app_launcher": "PASS", "jmod_sha256": digest(jmod), "jar_sha256": digest(jar),
               "m3jdk21_product_acceptance": False, "platform": "linux-only-candidate"}
    (root / "RECEIPT.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    return receipt
