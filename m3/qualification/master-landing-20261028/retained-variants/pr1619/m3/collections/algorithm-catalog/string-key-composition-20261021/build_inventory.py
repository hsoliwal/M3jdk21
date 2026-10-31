#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Build a finite source inventory from exact Git blobs, without editing a repository."""

import argparse
import csv
import hashlib
import io
import json
import re
import subprocess
from pathlib import Path

COLLECTION_ROOT = "m3/collections/src/main/java/com/m3/collections"
FAMILY_PATH = "m3/collections/src/main/resources/com/m3/collections/M3_SDK_FAMILY_CATALOG.tsv"
PROVIDER_ROOT = "m3/collections/implementations/commons-collections-4.5.0/src/main/java/com/m3/collections/apache/trie"
FAMILY_FIELDS = ["upstream_fqcn", "vendor", "existing_m3_owner", "proposed_m3_role_NOT_IMPLEMENTED", "disposition", "semantic_contract"]
OWNER_FIELDS = ["category", "source_path", "revision", "git_blob", "sha256", "source_scope", "source_signal", "string_relevance", "evidence_lines", "qualification"]
EXPLICIT = {
    "src/java.base/share/classes/java/lang/String.java": "string_substrate",
    "src/java.base/share/classes/java/lang/M3FrozenLexiconIndex.java": "lexicon_nontrie",
    "m3/collections/implementations/apache-patricia-trie/src/main/java/com/m3/collections/apache/M3PatriciaTrie.java": "provider_scaffold",
    "m3/collections/src/main/native/collections/m3_index_mask_compactor.c": "native_owner",
}
for _name in ("HashMap", "HashSet", "LinkedHashMap", "LinkedHashSet", "TreeMap", "TreeSet", "IdentityHashMap", "WeakHashMap", "ImmutableCollections", "Collections", "Arrays"):
    EXPLICIT["src/java.base/share/classes/java/util/" + _name + ".java"] = "jdk_consumer"
for _name in ("ConcurrentHashMap", "ConcurrentSkipListMap", "ConcurrentSkipListSet"):
    EXPLICIT["src/java.base/share/classes/java/util/concurrent/" + _name + ".java"] = "jdk_consumer"


def git(repository, *args):
    return subprocess.run(["git", "-C", str(repository), *args], check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout


def category(name):
    if "Patricia" in name or "Trie" in name or "Radix" in name or name == "StringKeyAnalyzer":
        return "collections.trie"
    if name.startswith("M3String") or name == "String":
        return "string.substrate"
    if name == "M3FrozenLexiconIndex":
        return "string.lexicon.hash"
    if "Identity" in name:
        return "collections.identity"
    if "Weak" in name or "Reference" in name:
        return "collections.reference"
    if "Filter" in name or "View" in name:
        return "collections.views"
    if "Mask" in name or "State" in name or "mask" in name:
        return "collections.array.mask"
    if "Tree" in name or "Sorted" in name or "Skip" in name:
        return "collections.tree.sorted"
    if "Multimap" in name or "MultiMap" in name or "MultiValueMap" in name:
        return "collections.multimap"
    if "Bag" in name or "Multiset" in name:
        return "collections.bag"
    if "BiMap" in name:
        return "collections.bimap"
    if "Table" in name:
        return "collections.table"
    if "Cache" in name or "Lru" in name or "LFU" in name:
        return "collections.cache"
    if "Map" in name or "Set" in name:
        return "collections.keyed"
    return "collections.support"


def relevance(row):
    name, contract = row["upstream_fqcn"], row["semantic_contract"]
    if "Identity" in name or "identity" in contract:
        return "REFERENCE_IDENTITY_PRESERVE"
    if "Enum" in name or "ClassToInstance" in name:
        return "NON_STRING_KEY_DOMAIN"
    if ".primitive." in name:
        return "PRIMITIVE_DOMAIN_NO_STRING_KEY_SUBSTITUTION"
    if "Weak" in name or "Reference" in name or "weak" in contract:
        return "STRING_CONTENT_WITH_REACHABILITY_GATE"
    if "CaseInsensitive" in name:
        return "STRING_WITH_EXPLICIT_CASE_POLICY"
    if any(word in name for word in ("Trie", "Map", "Set", "Bag", "Multiset", "MultiSet", "Table")):
        return "STRING_KEY_OR_ELEMENT_CANDIDATE"
    return "STRING_ELEMENT_OR_OPERATION_CONTEXT_ONLY"


def tsv_bytes(fields, rows):
    out = io.StringIO(newline="")
    writer = csv.DictWriter(out, fieldnames=fields, delimiter="\t", lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    return out.getvalue().encode("utf-8")


def build(repository, revision):
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("revision must be a complete 40-character lowercase Git commit")
    actual = git(repository, "rev-parse", "--verify", revision + "^{commit}").decode().strip()
    if actual != revision:
        raise ValueError("revision did not resolve to the exact requested commit")
    scopes = dict(EXPLICIT)
    roots = [COLLECTION_ROOT, PROVIDER_ROOT, "src/java.base/share/classes/java/lang"]
    paths = git(repository, "ls-tree", "-r", "--name-only", revision, "--", *roots).decode().splitlines()
    for path in paths:
        if path.startswith(COLLECTION_ROOT + "/") and path.endswith(".java"):
            scopes[path] = "collections_owner"
        elif path.startswith(PROVIDER_ROOT + "/") and path.endswith(".java"):
            scopes[path] = "provider_oracle"
        elif Path(path).name.startswith("M3String") and path.endswith(".java"):
            scopes[path] = "string_substrate"
    owners = []
    for path, scope in sorted(scopes.items()):
        data = git(repository, "show", revision + ":" + path)
        text = data.decode("utf-8")
        blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
        has_refusal = "M3NotYetReceived" in text
        has_scaffold = "SCAFFOLD" in text or "Scaffold" in text
        signal = "SCAFFOLD_WITH_REFUSAL_SIGNAL" if has_scaffold and has_refusal else "CONTAINS_RECEIPT_REFUSAL_SIGNAL" if has_refusal else "NO_RECEIPT_REFUSAL_TEXT_SIGNAL"
        needles = ("M3NotYetReceived", "SCAFFOLD", "TrieEntry", "charAt(", "compareTo(", "compare(", "hashCode(", "startsWith(", "int[]", "Object[]", "GetIntArrayRegion", "GetLongArrayRegion")
        anchors = []
        for token in needles:
            hit = next((str(i) + ":" + token for i, line in enumerate(text.splitlines(), 1) if token in line), None)
            if hit:
                anchors.append(hit)
        name = Path(path).stem
        key_relevance = "REFERENCE_IDENTITY_PRESERVE" if "Identity" in name else "EXACT_SOURCE_REVIEW_REQUIRED"
        if name.startswith(("M3Long", "M3Int", "M3Enum")):
            key_relevance = "PRIMITIVE_OR_ENUM_DOMAIN; NO_STRING_KEY_SUBSTITUTION_INFERRED"
        owners.append(dict(zip(OWNER_FIELDS, [category(name), path, revision, blob, hashlib.sha256(data).hexdigest(), scope, signal, key_relevance, ";".join(anchors), "SOURCE_INVENTORY_ONLY; NOT_BEHAVIORAL_QUALIFICATION"])))
    family_data = git(repository, "show", revision + ":" + FAMILY_PATH)
    reader = csv.DictReader(io.StringIO(family_data.decode()), delimiter="\t")
    if reader.fieldnames != FAMILY_FIELDS:
        raise ValueError("source SDK family schema changed; update explicit adapter after review")
    families = []
    for row in reader:
        if None in row or any(value is None for value in row.values()):
            raise ValueError("malformed SDK family row")
        row.update(string_relevance=relevance(row), qualification="EXISTING_DISPOSITION_PRESERVED; NO_NEW_ADAPTER_QUALIFIED", source_revision=revision)
        families.append(row)
    families.sort(key=lambda row: row["upstream_fqcn"])
    outputs = {
        "OWNER_INVENTORY.tsv": tsv_bytes(OWNER_FIELDS, owners),
        "SDK_FAMILY_INVENTORY.tsv": tsv_bytes(FAMILY_FIELDS + ["string_relevance", "qualification", "source_revision"], families),
    }
    receipt = {
        "schema": "M3_STRING_KEY_INVENTORY_V1", "source_repository": "hsoliwal/M3JDK21", "revision": revision,
        "source_roots": roots, "explicit_paths": sorted(EXPLICIT), "family_source_path": FAMILY_PATH,
        "family_source_sha256": hashlib.sha256(family_data).hexdigest(), "owner_count": len(owners), "family_count": len(families),
        "output_sha256": {name: hashlib.sha256(data).hexdigest() for name, data in outputs.items()},
        "reads_git_blobs_only": True, "writes_source_repository": False, "full_repository_inventory": False,
        "classification_limit": "Name categories and lexical source signals are routing aids; they do not prove storage or behavior. Existing SDK dispositions are copied unchanged.",
    }
    outputs["INVENTORY_RECEIPT.json"] = (json.dumps(receipt, indent=2, sort_keys=True) + "\n").encode()
    return outputs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True, type=Path)
    parser.add_argument("--revision", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    if args.output.exists():
        parser.error("output must be a fresh directory")
    try:
        outputs = build(args.repository, args.revision)
        args.output.mkdir(parents=True, exist_ok=False)
        for name, data in outputs.items():
            (args.output / name).write_bytes(data)
    except (ValueError, OSError, subprocess.CalledProcessError) as exc:
        parser.exit(2, "inventory refused: " + str(exc) + "\n")
    print(json.dumps({"output": str(args.output), "files": sorted(outputs)}))


if __name__ == "__main__":
    main()
