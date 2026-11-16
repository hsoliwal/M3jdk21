#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed host-Java21 validation for the exact draft M3JDK CollectionViews receiver.

Uses the entire real CollectionViews.java product source, not extracted methods.
Proof-only ProgressMonitor implementations replace the module's monitor SPI for this
bounded compile. This does NOT qualify the full module, jtreg or installed M3JDK.
"""
import hashlib
from pathlib import Path
import subprocess
import tempfile

REPO = Path(__file__).resolve().parents[4]
OWNER = REPO / "src/jdk.m3.collections/share/classes/jdk/m3/collections/CollectionViews.java"
TEST = REPO / "test/jdk/jdk/m3/collections/CollectionViewsRangeCompatibilityTest.java"
EXPECTED_OLD = "d877ca569ebd0b0071709aef2e9a1e5f3c6e3f42f96a5f2ccaa0cd99e69dadd1"
EXPECTED_NEW = "e44eb0497a6f0cda7b369cf19c1b0546022012976028b6a1edce93cbd1759407"
EXPECTED_TEST = "9d6dc566ee4d5a25e7c16e802309e4e050cf3f4bda26021e8c5a412082a710f2"
MARKER = (
    "\n    /*\n"
    "     * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.\n"
    "     * Additive static M3JDK operations; inherited java.util APIs remain unchanged.\n"
)

PROGRESS = """package jdk.m3.collections;
public interface ProgressMonitor {
    boolean isCanceled();
    void worked(long units);
    default void checkCanceled() {
        if (isCanceled()) throw new java.util.concurrent.CancellationException("cancelled");
    }
}
"""
MONITOR = """package jdk.m3.collections;
public final class DefaultProgressMonitor implements ProgressMonitor {
    private long units;
    private long generation;
    private boolean done;
    private boolean canceled;
    public void beginTask(String name, long total) {
        generation++;
        units = 0L;
        done = false;
    }
    public void setCanceled(boolean value) { canceled = value; }
    @Override public boolean isCanceled() { return canceled; }
    @Override public void worked(long amount) { units += amount; }
    public State state() { return new State(units, done, generation); }
    public record State(long worked, boolean done, long generation) {}
}
"""
LEGACY = """import java.util.*;
import jdk.m3.collections.CollectionViews;
public final class LegacyCollectionViewsClient {
    public static void main(String[] args) {
        Collection<Integer> result = CollectionViews.filtered(
                new ArrayList<>(List.of(1, 2, 3, 4)), number -> number % 2 == 0);
        System.out.println("LEGACY:" + result);
    }
}
"""

def digest(content):
    return hashlib.sha256(content).hexdigest()

def execute(*args):
    p = subprocess.run(args, text=True, stdout=subprocess.PIPE,
                       stderr=subprocess.STDOUT, check=False)
    if p.returncode != 0:
        raise AssertionError(f"{' '.join(map(str, args))} exit={p.returncode}\n{p.stdout}")
    return p.stdout

def put(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")

def api_rows(classes):
    result = execute("javap", "-classpath", str(classes), "-public",
                     "jdk.m3.collections.CollectionViews")
    return {line.strip() for line in result.splitlines()
            if line.strip().startswith("public static")
            or line.strip().startswith("protected ")}

def main():
    full_bytes = OWNER.read_bytes()
    test_bytes = TEST.read_bytes()
    assert digest(full_bytes) == EXPECTED_NEW, "source postimage drift"
    assert digest(test_bytes) == EXPECTED_TEST, "test postimage drift"
    full = full_bytes.decode("utf-8")
    assert full.count(MARKER) == 1 and full.endswith("\n}\n"), "source marker/suffix drift"
    original = full[:full.index(MARKER)] + "}\n"
    assert digest(original.encode("utf-8")) == EXPECTED_OLD, "original owner preimage drift"
    for path, forbidden in [
        (REPO / "src/java.base/share/classes/java/util/List.java",
         "default void forEachRangeMonitored("),
        (REPO / "src/java.base/share/classes/java/util/AbstractList.java",
         "public void forEachRange("),
    ]:
        assert forbidden not in path.read_text(encoding="utf-8"), f"inherited JDK API collision: {path}"
    print("M3_STATIC_RANGE_EXACT_PRODUCT_SEALS_PASS targets=2 original-owner-retained=true")

    with tempfile.TemporaryDirectory(prefix="m3-cviews-product-") as temp:
        directory = Path(temp)
        client = directory / "legacy/LegacyCollectionViewsClient.java"
        put(client, LEGACY)
        classes = {}
        for label, text in (("old", original), ("new", full)):
            src = directory / label / "src/jdk/m3/collections"
            output = directory / label / "classes"
            output.mkdir(parents=True)
            put(src / "CollectionViews.java", text)
            put(src / "ProgressMonitor.java", PROGRESS)
            put(src / "DefaultProgressMonitor.java", MONITOR)
            execute("javac", "--release", "21", "-proc:none", "-Xlint:all",
                    "-Werror", "-d", str(output),
                    str(src / "CollectionViews.java"),
                    str(src / "ProgressMonitor.java"),
                    str(src / "DefaultProgressMonitor.java"))
            classes[label] = output
        print("M3_STATIC_RANGE_ENTIRE_OWNER_HOST_JAVA21_COMPILE_PASS original=1 candidate=1")

        original_api = api_rows(classes["old"])
        changed_api = api_rows(classes["new"])
        assert original_api <= changed_api, sorted(original_api - changed_api)
        added = changed_api - original_api
        assert len(added) == 2 and all(
            "forEachRange(" in row or "forEachRangeMonitored(" in row
            for row in added), sorted(added)
        print("M3_STATIC_RANGE_REFLECTIVE_API_PASS retained=" + str(len(original_api))
              + " added=2")

        old_client = directory / "legacy/classes"
        old_client.mkdir(parents=True)
        execute("javac", "--release", "21", "-proc:none", "-Xlint:all",
                "-Werror", "-classpath", str(classes["old"]),
                "-d", str(old_client), str(client))
        frozen_digest = digest((old_client / "LegacyCollectionViewsClient.class").read_bytes())
        old_stdout = execute("java", "-cp",
                str(old_client) + ":" + str(classes["old"]), "LegacyCollectionViewsClient")
        new_stdout = execute("java", "-cp",
                str(old_client) + ":" + str(classes["new"]), "LegacyCollectionViewsClient")
        assert old_stdout == new_stdout == "LEGACY:[2, 4]\n", (old_stdout, new_stdout)
        assert digest((old_client / "LegacyCollectionViewsClient.class").read_bytes()) == frozen_digest
        updated_client = directory / "legacy/recompiled"
        updated_client.mkdir()
        execute("javac", "--release", "21", "-proc:none", "-Xlint:all",
                "-Werror", "-classpath", str(classes["new"]),
                "-d", str(updated_client), str(client))
        assert execute("java", "-cp", str(updated_client) + ":" +
                str(classes["new"]), "LegacyCollectionViewsClient") == old_stdout
        print("M3_STATIC_RANGE_OLD_CLIENT_PASS frozen_sha256=" + frozen_digest)

        test_classes = directory / "test/classes"
        test_classes.mkdir(parents=True)
        execute("javac", "--release", "21", "-proc:none", "-Xlint:all",
                "-Werror", "-classpath", str(classes["new"]), "-d",
                str(test_classes), str(TEST))
        for mode in ((), ("-Xint",), ("-XX:TieredStopAtLevel=1",)):
            out = execute("java", *mode, "-cp", str(test_classes) + ":" +
                          str(classes["new"]), "CollectionViewsRangeCompatibilityTest")
            assert "M3_STATIC_RANGE_COMPAT_PASS checks=46" in out, out
            print(out.strip())
        print("M3_STATIC_RANGE_ENTIRE_OWNER_BOUNDED_HOST_PASS configurations=3")

    print("HOLD: SDK_NOT_RUN MODULE_BUILD_NOT_RUN JTREG_NOT_RUN INSTALLED_IMAGE_NOT_RUN")

if __name__ == "__main__":
    main()
