#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Contradiction probe: write copies of the master owners with the crate snapshot's superseded form put back.
usage: python -I contradict.py <m3/fs module dir> <out dir>   (GATES.tsv 5_contradiction)
P2-026: MFileContent.transferAndCloseInput copy loop -> the snapshot's try (input) { return input.transferTo(destination); }
P2-027: MFiles root resolution and fork guard -> the crate postimage's toFileObject(path.toAbsolutePath().toFile())
        and plan.path == null ? null : node -> fork(plan, node)
Refuses unless every anchor matches exactly once.
"""
import os, sys
src, out = sys.argv[1], sys.argv[2]
EDITS = {
    'src/main/java/jdk/m3/fs/MFileContent.java': [(
        """        long count = 0;
        try {
            // InputStream overrides may flush the destination. This boundary
            // promises a borrowed output with no flush, so own the copy loop.
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) != -1;) {
                destination.write(buffer, 0, read);
                count += read;
            }
        } catch (IOException | RuntimeException | Error failure) {
            MFileStreams.closeAfterFailure(input, failure);
            throw failure;
        }
        input.close();
        return count;
""",
        """        try (input) {
            return input.transferTo(destination);
        }
""")],
    'src/main/java/jdk/m3/fs/MFiles.java': [
        ("                        : session.resolve(path);\n",
         "                        : session.manager.toFileObject(path.toAbsolutePath().toFile());\n"),
        ("                            : session.resolve(plan.path);\n",
         "                            : session.manager.toFileObject(plan.path.toAbsolutePath().toFile());\n"),
        ("                            plan.path == null || plan.path.getFileSystem() != FileSystems.getDefault()\n"
         "                                    ? null : node -> fork(plan, node),\n",
         "                            plan.path == null ? null : node -> fork(plan, node),\n"),
    ],
}
for rel, edits in EDITS.items():
    text = open(os.path.join(src, rel), encoding='utf-8', newline='').read()
    for old, new in edits:
        n = text.count(old)
        if n != 1:
            sys.exit('REFUSE %s anchor count %d: %r' % (rel, n, old[:60]))
        text = text.replace(old, new)
    dst = os.path.join(out, rel)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    open(dst, 'w', encoding='utf-8', newline='').write(text)
    print('WROTE', rel, len(edits))
