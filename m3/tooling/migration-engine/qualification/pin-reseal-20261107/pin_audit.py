"""pin_audit.py <resources-root> : HASH_PINNED_CRATE_AUDIT v1.
For every manifest.tsv under <resources-root> whose rows have the four-column hash-pinned shape
(target_path, preimage ABSENT|sha256, postimage sha256, template file name), checks what M3HashPinnedJavaSnapshotRecipe
checks when it loads a crate: the template file exists next to the manifest, and sha256(template UTF-8 text) equals the
postimage column. Prints one row per finding (MISSING_TEMPLATE, HASH_DRIFT) with the crate, the row and, for a missing
template, the renamed candidate that exists (Synexia -> M3), then a summary per crate."""
import hashlib
import re
import sys
from pathlib import Path

root = Path(sys.argv[1])
SHA = re.compile(r"^[0-9a-f]{64}$")
crates = bad_crates = 0
for manifest in sorted(root.rglob("manifest.tsv")):
    rows = [l.split("\t") for l in manifest.read_text(encoding="utf-8").splitlines() if l and not l.startswith("#")]
    rows = [r for r in rows if len(r) == 4 and (r[1] == "ABSENT" or SHA.match(r[1])) and SHA.match(r[2])]
    if not rows:
        continue
    crates += 1
    findings = 0
    for target, pre, post, name in rows:
        template = manifest.parent / name
        if not template.exists():
            alt = name.replace("Synexia", "M3")
            hint = alt if (manifest.parent / alt).exists() else "-"
            print(f"MISSING_TEMPLATE\t{manifest.parent.name}\t{name}\trenamed_candidate={hint}")
            findings += 1
            continue
        digest = hashlib.sha256(template.read_bytes()).hexdigest()
        if digest != post:
            print(f"HASH_DRIFT\t{manifest.parent.name}\t{name}\tpinned={post[:12]}\tactual={digest[:12]}")
            findings += 1
    bad_crates += findings > 0
print(f"PIN_AUDIT crates={crates} crates_with_findings={bad_crates}")
