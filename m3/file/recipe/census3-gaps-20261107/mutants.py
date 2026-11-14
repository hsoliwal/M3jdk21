#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""census3 m3/file mutants (P3-012): each mutant edits one restored member of the applied source, is compiled over the head
classes and replayed through the differential owner side; KILLED = digest differs from the donor digest.

usage: python -I mutants.py <repo-root> <head-classpath> <diff-out-dir (from differential.sh)> <work-dir> <donor-classes> [episodes]
  head-classpath: m3/file target/classes plus the m3-collections and m3-arrays jars (path-separator joined)
"""
import hashlib
import shutil
import subprocess
import sys
from pathlib import Path

SRC = "m3/file/src/main/java/jdk/m3/file/"
MUTANTS = (
    ("L01 file facade writes the semantic projection", SRC + "M3File.java",
     "    return asM3String().writeUtf8To(target);", "    return semanticProjection().writeUtf8To(target);"),
    ("L02 projection facade writes the whole file", SRC + "M3FileProjection.java",
     "    return asM3String().writeUtf8To(target);", "    return file.asM3String().writeUtf8To(target);"),
    ("L03 span facade writes the whole file", SRC + "M3FileSpan.java",
     "    return asM3String().writeUtf8To(target);", "    return file.asM3String().writeUtf8To(target);"),
    ("L04 snapshot facade writes an empty selection", SRC + "M3FileSnapshot.java",
     "    return indexedText(Objects.requireNonNull(resolver, \"resolver\")).writeUtf8To(target);",
     "    return indexedText(Objects.requireNonNull(resolver, \"resolver\"), new int[0]).writeUtf8To(target);"),
    ("L05 channel writer drops the magic", SRC + "M3FileEventWriter.java",
     "    return new M3FileEventWriter(Channels.newOutputStream(Objects.requireNonNull(channel, \"channel\")));",
     "    return new M3FileEventWriter(Channels.newOutputStream(Objects.requireNonNull(channel, \"channel\")), false);"),
    ("L06 sinkless read bounds frames at 73 bytes", SRC + "M3FileEventReader.java",
     "    return read(input, M3FileStreamEvents.noOp());", "    return read(input, M3FileStreamEvents.noOp(), 73);"),
    ("L07 readIndexedText selects no kind", SRC + "M3FileEventReader.java",
     "    return readSelectedText(input, resolver, -1);", "    return readSelectedText(input, resolver, 0);"),
    ("L08 readProjectionText ignores the kinds", SRC + "M3FileEventReader.java",
     "    return readSelectedText(input, resolver, M3FileProjection.maskOf(selectedKinds));",
     "    return readSelectedText(input, resolver, -1);"),
    ("L09 collector kind test inverted", SRC + "M3FileEventReader.java",
     "        if ((kindMask & (1 << token.kind().ordinal())) != 0) {",
     "        if ((kindMask & (1 << token.kind().ordinal())) == 0) {"),
)


def main():
    root, head, diff, work, donor = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3]), Path(sys.argv[4]), sys.argv[5]
    episodes = sys.argv[6] if len(sys.argv) > 6 else "1000"
    jdk = Path("C:/Program Files/Java/jdk-21/bin")
    tmp = "-Djava.io.tmpdir=S:/m3-scratch/eb-conv/c3g/tmp"
    want = subprocess.run([str(jdk / "java"), tmp, "-cp", "%s;%s" % (diff / "donor", donor),
                           "census3.Census3FileDifferential", "donor", episodes], capture_output=True, text=True,
                          check=True).stdout
    killed = 0
    for name, rel, old, new in MUTANTS:
        text = (root / rel).read_text(encoding="utf-8")
        if text.count(old) != 1:
            print("MUTANT %s ANCHOR_COUNT=%d" % (name, text.count(old)))
            continue
        mdir = work / name.split()[0]
        shutil.rmtree(mdir, ignore_errors=True)
        (mdir / "src").mkdir(parents=True)
        (mdir / "classes").mkdir()
        msrc = mdir / "src" / Path(rel).name
        msrc.write_text(text.replace(old, new), encoding="utf-8", newline="\n")
        subprocess.run([str(jdk / "javac"), "-J" + tmp, "-nowarn", "-cp", head, "-d", str(mdir / "classes"),
                        str(msrc)], check=True, capture_output=True)
        run = subprocess.run([str(jdk / "java"), tmp, "-cp", "%s;%s;%s" % (mdir / "classes", diff / "owner", head),
                              "census3.Census3FileDifferential", "owner", episodes], capture_output=True, text=True)
        got = run.stdout if run.returncode == 0 else "EXIT %d %s" % (run.returncode, run.stderr[-200:])
        state = "SURVIVED" if got == want else "KILLED"
        killed += state == "KILLED"
        print("MUTANT %s %s %s" % (name, state, hashlib.sha256(got.encode()).hexdigest()[:12]))
    print("MUTANTS killed=%d/%d episodes=%s" % (killed, len(MUTANTS), episodes))


if __name__ == "__main__":
    main()
