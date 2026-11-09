#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Prepare a bounded, source-built OpenRewrite Javadoc parser successor input.

Original Maven recipes seal and emit these candidates. No receiving owner or
generated SDK output is changed by this preparation helper.
"""
from pathlib import Path
import argparse
import hashlib
import shutil

OWNER = "rewrite-java-21/src/main/java/org/openrewrite/java/isolated/ReloadableJava21JavadocVisitor.java"
BEFORE = "6ba33d69d5835db0185fc1fe8912ed2645a1c7d9c8564d904a1df90affc4ad40"
FRAGMENT = """        // M3: a local URI fragment is literal Javadoc content, not a Java member.
        if (node.getReference() != null && node.getReference().getSignature().startsWith("##")) {
            List<Javadoc> content = new ArrayList<>(spaceBeforeRef);
            content.addAll(visitText(node.getReference().getSignature()));
            content.addAll(convertMultiline(node.getLabel()));
            return new Javadoc.UnknownInline(randomId(), Markers.EMPTY,
                    node.getTagName(), content, endBrace());
        }

"""
SPEC = """    // M3: Java 21 recognizes @spec; retain its URL and title with the existing block model.
    @Override
    public Tree visitSpec(SpecTree node, List<Javadoc> body) {
        body.addAll(sourceBefore("@" + node.getTagName()));
        List<DocTree> content = new ArrayList<>();
        content.add(node.getURL());
        content.addAll(node.getTitle());
        return new Javadoc.UnknownBlock(randomId(), Markers.EMPTY,
                node.getTagName(), convertMultiline(content));
    }

"""
NOTICE = " * Modified 2026 by Hitesh Soliwal and contributors: retain @spec and local URI-fragment Javadoc.\n"


def main():
    parser = argparse.ArgumentParser()
    for name in ("upstream", "templates", "runtime-before", "runtime-after", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise ValueError("fresh parser input required")
    original = args.upstream / OWNER
    if hashlib.sha256(original.read_bytes()).hexdigest() != BEFORE:
        raise ValueError("upstream parser source identity drift")
    files = sorted(args.upstream.rglob("*.java"))
    if len(files) != 9:
        raise ValueError("expected the exact nine-source original Java21 parser module")
    for source in files:
        target = args.output / source.relative_to(args.upstream)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    text = original.read_text()
    anchors = [
        (" * Copyright 2023 the original author or authors.\n", " * Copyright 2023 the original author or authors.\n" + NOTICE),
        ("        List<Javadoc> spaceBeforeRef = whitespaceBefore();\n", "        List<Javadoc> spaceBeforeRef = whitespaceBefore();\n" + FRAGMENT),
        ("    @Override\n    public Tree visitUnknownBlockTag(UnknownBlockTagTree node, List<Javadoc> body) {", SPEC + "    @Override\n    public Tree visitUnknownBlockTag(UnknownBlockTagTree node, List<Javadoc> body) {"),
    ]
    for before, after in anchors:
        if text.count(before) != 1:
            raise ValueError("parser atom anchor drift")
        text = text.replace(before, after, 1)
    if text.replace(NOTICE, "", 1).replace(FRAGMENT, "", 1).replace(SPEC, "", 1) != original.read_text():
        raise ValueError("parser changed outside the three reviewed insertions")
    (args.output / OWNER).write_text(text)
    for name in ("pom.xml", "lombok.config.txt", "LICENSE.txt", "src/test/java/M3RuntimeJavadocParserTest.java"):
        target = args.output / "rewrite-java-21" / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(args.templates / name, target)
    for name, source in (("Runtime-before.java.txt", args.runtime_before), ("Runtime-after.java.txt", args.runtime_after)):
        target = args.output / "rewrite-java-21/src/test/resources/runtime" / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)


if __name__ == "__main__":
    main()
