#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
"""Assemble the exact, readable qualification inputs without copying caches."""
import hashlib
import json


def assemble(read_text):
    cfg = json.loads(read_text("config.json"))
    for name, item in cfg.pop("textInputs").items():
        text = read_text(item["path"])
        if hashlib.sha256(text.encode("utf-8")).hexdigest() != item["sha256"]:
            raise ValueError("qualification text input hash drift: " + name)
        cfg[name] = text
    for group in ("sources", "recipeSources"):
        for name, item in cfg[group].items():
            text = read_text(item.pop("resource"))
            data = text.encode("utf-8")
            blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
            if blob != item["sha"]:
                raise ValueError("qualification source hash drift: " + name)
            item["content"] = text
    return cfg


def main():
    import os
    import sys
    sys.dont_write_bytecode = True
    for name in ("TEMP", "TMP", "TMPDIR"):
        os.environ[name] = r"K:\codex-work\agent-temp\lexicon-maven-r241"
    import ctypes
    import shutil
    import zlib
    from pathlib import Path
    lane = Path(os.environ["TEMP"])
    if shutil.disk_usage(lane).free - 65536 < 8589934592:
        raise RuntimeError("qualification reserve held")
    native = ctypes.WinDLL("kernel32")
    buf = ctypes.create_unicode_buffer(32768)
    if not native.GetTempPathW(len(buf), buf) or Path(buf.value).resolve() != lane.resolve():
        raise RuntimeError("qualification temp routing held")
    root = Path(__file__).resolve().parent
    cfg = assemble(lambda path: (root / path).read_bytes().decode("utf-8"))

    def emit(value):
        data = json.dumps(value, ensure_ascii=True, separators=(",", ":")).encode()
        payload = zlib.compress(data, 9).hex()
        print("RECEIPT_BEGIN", flush=True)
        print("N " + str(len(data)))
        print("H " + hashlib.sha256(data).hexdigest())
        for at in range(0, len(payload), 64):
            print("P " + payload[at:at + 64])
        print("RECEIPT_END", flush=True)

    exec(compile(cfg["code"], "<sealed-qualification-runner>", "exec"),
         {"INPUT": cfg, "EMIT": emit, "__name__": "m3_precompute_qualification"})


if __name__ == "__main__":
    main()
