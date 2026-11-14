"""apply_naming.py <dir> : apply a3/jn/jdk_naming.rewrite_text to every file under <dir> (LF kept), print counts."""
import sys
from pathlib import Path
sys.path.insert(0, "S:/m3-scratch/converge-20261106/a3/jn")
import jdk_naming as j  # noqa: E402
for p in sorted(Path(sys.argv[1]).rglob("*")):
    if p.is_file():
        text = p.read_bytes().decode("utf-8")
        new, counts = j.rewrite_text(text)
        if new != text:
            p.write_bytes(new.encode("utf-8"))
            print("CHANGED", p.as_posix(), counts)
