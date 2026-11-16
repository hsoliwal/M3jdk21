#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Recompile an unchanged wildcard-static-import client against old/new Java21 API shapes.

Binary compatibility does not imply universal Java source compatibility. The
product owner signature is verified before building proof-only matching types.
"""
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[4]
OWNER = ROOT / "src/jdk.m3.collections/share/classes/jdk/m3/collections/CollectionViews.java"

def run(*args):
    return subprocess.run(args, text=True, check=False,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

def write(root, path, data):
    filename = root / path
    filename.parent.mkdir(parents=True, exist_ok=True)
    filename.write_text(data, encoding="utf-8", newline="\n")
    return filename

def main():
    source_bytes = OWNER.read_bytes()
    expected_digest = "e44eb0497a6f0cda7b369cf19c1b0546022012976028b6a1edce93cbd1759407"
    if hashlib.sha256(source_bytes).hexdigest() != expected_digest:
        raise AssertionError("M3 product owner drift: new proof required")
    source = source_bytes.decode("utf-8")
    expected = "public static <E> void forEachRange(List<? extends E> source,"
    assert expected in source, "source API declaration drift"

    old_api = """package jdk.m3.collections;
public final class CollectionViews { private CollectionViews() {} }
"""
    new_api = """package jdk.m3.collections;
import java.util.List;
import java.util.function.Consumer;
public final class CollectionViews {
    private CollectionViews() {}
    public static <E> void forEachRange(List<? extends E> source, int fromIndex,
            int toIndex, Consumer<? super E> action) {
        for (int i = fromIndex; i < toIndex; i++) action.accept(source.get(i));
    }
}
"""
    old_utility = """package legacy.other;
import java.util.List;
import java.util.function.Consumer;
public final class RangeOps {
    public static <E> void forEachRange(List<? extends E> source, int fromIndex,
            int toIndex, Consumer<? super E> action) {
        for (int i = fromIndex; i < toIndex; i++) action.accept(source.get(i));
    }
}
"""
    unchanged_client = """import static jdk.m3.collections.CollectionViews.*;
import static legacy.other.RangeOps.*;
import java.util.ArrayList;
import java.util.List;
public final class FrozenSourceClient {
    public static void main(String[] args) {
        List<Integer> selected = new ArrayList<>();
        forEachRange(List.of(1, 2, 3, 4), 1, 3, selected::add);
        System.out.println(selected);
    }
}
"""
    flags = ("--release", "21", "-proc:none", "-Xlint:all", "-Werror")
    with tempfile.TemporaryDirectory(prefix="m3-static-import-") as temp:
        root = Path(temp)
        old = write(root, "old/jdk/m3/collections/CollectionViews.java", old_api)
        new = write(root, "new/jdk/m3/collections/CollectionViews.java", new_api)
        helper = write(root, "client/legacy/other/RangeOps.java", old_utility)
        client = write(root, "client/FrozenSourceClient.java", unchanged_client)
        old_classes, new_classes = root / "old-classes", root / "new-classes"
        old_classes.mkdir()
        new_classes.mkdir()

        first = run("javac", *flags, "-d", str(old_classes),
                    str(old), str(helper), str(client))
        if first.returncode:
            raise AssertionError("old client must compile:\n" + first.stdout)
        old_bytes = (old_classes / "FrozenSourceClient.class").read_bytes()
        before = run("java", "-cp", str(old_classes), "FrozenSourceClient")
        if before.returncode or before.stdout.strip() != "[2, 3]":
            raise AssertionError("unexpected baseline behavior:\n" + before.stdout)

        second = run("javac", *flags, "-d", str(new_classes),
                     str(new), str(helper))
        if second.returncode:
            raise AssertionError("new API fixture failed:\n" + second.stdout)
        frozen = run("java", "-cp",
                     os.pathsep.join((str(new_classes), str(old_classes))),
                     "FrozenSourceClient")
        if frozen.returncode or frozen.stdout.strip() != "[2, 3]":
            raise AssertionError("old binary changed:\n" + frozen.stdout)
        if old_bytes != (old_classes / "FrozenSourceClient.class").read_bytes():
            raise AssertionError("old classfile mutated")

        rebuilt = run("javac", *flags, "-classpath", str(new_classes),
                      "-d", str(new_classes), str(client))
        if rebuilt.returncode == 0 or "ambiguous" not in rebuilt.stdout:
            raise AssertionError("expected ambiguous static import:\n" + rebuilt.stdout)

        # Explicitly qualified calls remain source-selectable even when
        # wildcard static imports expose two applicable methods.
        for class_name, qualified in (
            ("QualifiedLegacyRangeClient", "legacy.other.RangeOps"),
            ("QualifiedM3RangeClient", "jdk.m3.collections.CollectionViews"),
        ):
            qualified_text = unchanged_client.replace(
                "public final class FrozenSourceClient",
                "public final class " + class_name,
            ).replace(
                "        forEachRange(List.of",
                "        " + qualified + ".forEachRange(List.of",
            )
            if qualified_text == unchanged_client:
                raise AssertionError("qualified-client source rewrite did not happen")
            qualified_source = write(
                root, "client/" + class_name + ".java", qualified_text
            )
            rebuilt_qualified = run(
                "javac", *flags, "-classpath", str(new_classes),
                "-d", str(new_classes), str(qualified_source),
            )
            if rebuilt_qualified.returncode:
                raise AssertionError(
                    "qualified call failed against new API:\n" + rebuilt_qualified.stdout
                )
            qualified_output = run(
                "java", "-cp", str(new_classes), class_name
            )
            if qualified_output.returncode or qualified_output.stdout.strip() != "[2, 3]":
                raise AssertionError("qualified call changed behavior:\n" + qualified_output.stdout)

        print("M3_STATIC_IMPORT_QUALIFIED_CALLS_PASS cases=2")
        print("M3_STATIC_IMPORT_FROZEN_BINARY_PASS")
        print("M3_STATIC_IMPORT_UNCHANGED_SOURCE_COLLISION_CONFIRMED")
        print("SOURCE_COMPATIBILITY_HOLD: wildcard static imports can be ambiguous")

if __name__ == "__main__":
    main()
