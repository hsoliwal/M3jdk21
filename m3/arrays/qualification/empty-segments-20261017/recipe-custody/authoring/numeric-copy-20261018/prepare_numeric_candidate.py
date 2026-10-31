#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Build reviewed recipe input; never write a receiving source tree."""
from pathlib import Path
import argparse
import hashlib
import json

PREIMAGE_SHA256 = "8d6afc24c677adb7ba612071d34453c8a7d8b9e1a81ac27fb06bc3f62c673edd"
OWNER = "m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def method(kind, shape):
    primitive = kind.lower()
    validate = f"""
        @Override
        public void copyTo(int sourceStart, {primitive}[] target, int targetStart, int count) {{
            Objects.requireNonNull(target, "target");
            Objects.checkFromIndexSize(sourceStart, count, length());
            Objects.checkFromIndexSize(targetStart, count, target.length);
"""
    if shape == "Join":
        operation = f"""            if (count == 0) return;
            int segment = locate(ends, sourceStart);
            int copied = 0;
            while (copied < count) {{
                int previous = segment == 0 ? 0 : ends[segment - 1];
                int from = sourceStart + copied;
                int take = Math.min(count - copied, ends[segment] - from);
                if (take > 0) {{
                    copy{kind}Range(segments[segment], from - previous,
                            target, targetStart + copied, take);
                    copied += take;
                }}
                segment++;
            }}
"""
    elif shape == "Slice":
        operation = f"""            copy{kind}Range(source, start + sourceStart, target, targetStart, count);
"""
    else:
        operation = f"""            System.arraycopy(data, sourceStart, target, targetStart, count);
"""
    return validate + operation + "        }\n"


def helper(kind):
    primitive = kind.lower()
    return f"""    /**
     * Bulk-copy only known immutable owners. Unknown views keep their original
     * element-accessor order and partial-write behavior; never dispatch copyTo.
     */
    private static void copy{kind}Range(M3{kind}ArrayView source, int sourceStart,
            {primitive}[] target, int targetStart, int count) {{
        if (source instanceof {kind}Snapshot snapshot) {{
            System.arraycopy(snapshot.data, sourceStart, target, targetStart, count);
        }} else if (source instanceof {kind}Slice slice && slice.source instanceof {kind}Snapshot snapshot) {{
            System.arraycopy(snapshot.data, slice.start + sourceStart, target, targetStart, count);
        }} else {{
            for (int index = 0; index < count; index++) {{
                target[targetStart + index] = source.{primitive}At(sourceStart + index);
            }}
        }}
    }}

"""


def refine(before):
    text = before.decode("utf-8")
    for kind in ("Int", "Long"):
        for shape in ("Snapshot", "Slice", "Join"):
            marker = f"    private static final class {kind}{shape} "
            if text.count(marker) != 1:
                raise ValueError("missing/duplicate known class: " + marker)
            begin = text.index(marker)
            following = text.find("\n    private ", begin + len(marker))
            if following < 0:
                raise ValueError("no next owner boundary")
            end = text.rfind("    }\n", begin, following + 1)
            if end < begin:
                raise ValueError("missing known closing brace")
            if "void copyTo(" in text[begin:end]:
                raise ValueError("copyTo already present in known preimage")
            text = text[:end] + method(kind, shape) + text[end:]
    marker = "    private static int locate(int[] ends, int index) {"
    if text.count(marker) != 1:
        raise ValueError("missing/duplicate lookup owner")
    text = text.replace(marker, helper("Int") + helper("Long") + marker, 1)
    return text.encode("utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("readback", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    before = (args.readback / OWNER).read_bytes()
    if digest(before) != PREIMAGE_SHA256:
        raise ValueError("exact M3Arrays preimage drift")
    after = refine(before)
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output / "M3Arrays.java").write_bytes(after)
    (args.output / "candidate-receipt.json").write_text(json.dumps({
        "kind": "REVIEWED_RECIPE_INPUT_ONLY_NOT_RECEIVING_APPLICATION",
        "target": OWNER,
        "target_revision": "4b24824c3aa09d802121447f8970d442a855f064",
        "before_sha256": digest(before), "after_sha256": digest(after),
        "atoms": ["IntSnapshot.copyTo", "IntSlice.copyTo", "IntJoin.copyTo",
                  "LongSnapshot.copyTo", "LongSlice.copyTo", "LongJoin.copyTo",
                  "copyIntRange", "copyLongRange"],
        "public_interface_change": False, "added_instance_fields": 0,
        "production_native_change": False,
        "receiving_application": "Maven-qualified original M3HashPinnedJavaSnapshotRecipe only"
    }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
