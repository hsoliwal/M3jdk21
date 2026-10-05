#!/usr/bin/env python3
"""Append a sealed, explicit source/delivery census; no discovery, copies or gates."""
import argparse
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import tarfile


def require(value, message):
    if not value:
        raise ValueError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def rel(value):
    require(isinstance(value, str) and value and "\\" not in value,
            "relative path required")
    require(not PurePosixPath(value).is_absolute()
            and all(part not in ("", ".", "..") and ":" not in part
                    for part in value.split("/"))
            and all(ord(char) >= 32 and ord(char) != 127 for char in value),
            "unsafe relative path")
    return value


def data_file(path):
    path = Path(path)
    require(path.is_file() and not path.is_symlink(), "regular input file required: " + str(path))
    return path.read_bytes()


def unique(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "duplicate JSON key: " + key)
        result[key] = value
    return result


def parse(data):
    return json.loads(data, object_pairs_hook=unique)


def check(data, row):
    require(type(row["bytes"]) is int and row["bytes"] >= 0
            and len(data) == row["bytes"] and sha(data) == row["sha256"], "file identity drift")
    blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
    for key in ("sha", "gitBlob"):
        if key in row:
            require(row[key] == blob, "Git blob identity drift")
    return blob


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, required=True)
    parser.add_argument("--plan-sha256", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    require(not args.output.exists(), "fresh companion output required")
    inputs = {}

    def sealed(reference):
        path = Path(reference["file"]).absolute()
        data = data_file(path)
        require(sha(data) == reference["sha256"], "sealed document drift: " + str(path))
        row = {"file": str(path), "sha256": sha(data), "bytes": len(data)}
        if str(path) in inputs:
            require(inputs[str(path)] == row, "conflicting input seal")
        inputs[str(path)] = row
        return parse(data)

    plan = sealed({"file": str(args.plan), "sha256": args.plan_sha256})
    require(plan["schema"] == "m3.source-delivery-append-plan/1", "unknown plan")
    require(set(plan) == {"schema", "label", "prior", "segments"}, "closed plan fields required")
    prior = sealed(plan["prior"])
    prior_rows = prior.get("rows", prior.get("newRows"))
    require(isinstance(prior_rows, list), "prior coverage rows missing")
    if prior.get("schema") == "synexia.source-delivery-coverage/1":
        known = [row["sourcePath"] for row in prior_rows]
        pending = len(prior["publicationAdditions"])
    else:
        require(prior.get("schema") == "m3.source-delivery-append/1", "unknown prior coverage")
        known = list(prior["allSourcePaths"])
        pending = prior["pendingPublicationAdditionCount"]
    require(len(known) == len(set(known)), "duplicate prior source path")
    require(plan["segments"] and len(plan["segments"]) <= 8, "bounded explicit segment list required")
    new_rows = []
    bundle_cache = {}

    def logical_bytes(mapping):
        reference = {"file": mapping["bundleFile"], "sha256": mapping["bundleSHA256"]}
        key = (reference["file"], reference["sha256"])
        if key not in bundle_cache:
            bundle = sealed(reference)
            require(bundle["schema"] == "synexia.evidence.bundle/1", "unknown evidence bundle")
            directory = Path(reference["file"]).parent
            members = {}
            for part in bundle["parts"]:
                body = data_file(directory / rel(part["path"]))
                check(body, part)
                with tarfile.open(fileobj=io.BytesIO(body), mode="r:gz") as archive:
                    require(len(archive.getmembers()) == part["members"], "bundle member count drift")
                    for member in archive.getmembers():
                        rel(member.name)
                        require(member.isfile() and member.name not in members, "duplicate/special bundle member")
                        members[member.name] = archive.extractfile(member).read()
            indexed = {}
            for shard in bundle["metadataShards"]:
                body = members[rel(shard["member"])]
                check(body, shard)
                records = [parse(line) for line in body.splitlines()]
                require(len(records) == shard["records"], "bundle shard count drift")
                if shard["kind"] != "files":
                    continue
                for row in records:
                    name = rel(row["path"])
                    require(name not in indexed, "duplicate logical source path")
                    chunks = []
                    for chunk in row["chunks"]:
                        content = members["objects/" + chunk["sha256"][:2] + "/" + chunk["sha256"]]
                        check(content, chunk)
                        chunks.append(content)
                    content = b"".join(chunks)
                    check(content, row)
                    indexed[name] = content
            bundle_cache[key] = indexed
        logical = rel(mapping["logicalPath"])
        require(logical in bundle_cache[key], "logical file absent; references/omissions are not delivery")
        return bundle_cache[key][logical]

    labels = set()
    for segment in plan["segments"]:
        require(set(segment) == {"id", "whitelist", "mapping"}, "closed segment fields required")
        require(segment["id"] not in labels, "duplicate segment")
        labels.add(segment["id"])
        whitelist = sealed(segment["whitelist"])
        mapping = sealed(segment["mapping"])
        source_rows = whitelist["files"]
        source_paths = [rel(row["path"]) for row in source_rows]
        mapped = {rel(row["sourcePath"]): row for row in mapping["files"]}
        require(len(source_paths) == len(set(source_paths))
                and len(mapped) == len(mapping["files"])
                and set(source_paths) == set(mapped), "mapping must cover the exact declared source set once")
        for source in source_rows:
            path = source["path"]
            require(path not in known, "source path overlaps prior or another segment: " + path)
            file = source.get("file", source.get("localPath"))
            require(file is not None, "source whitelist requires explicit local body")
            source_data = data_file(file)
            blob = check(source_data, source)
            delivery = mapped[path]
            require(delivery["sha256"] == source["sha256"]
                    and delivery["bytes"] == source["bytes"], "mapping source identity drift")
            if "logicalPath" in delivery:
                require(logical_bytes(delivery) == source_data, "logical source body differs")
                destination = rel(delivery["receivingBundle"])
                evidence = {key: delivery[key] for key in ("bundleFile", "bundleSHA256", "logicalPath", "receivingBundle")}
                kind = "VERIFIED_PORTABLE_LOGICAL_BYTES"
            else:
                destination = rel(delivery.get("receivingPath", delivery.get("path")))
                target_file = delivery.get("file", delivery.get("localPath"))
                target_data = data_file(target_file)
                check(target_data, delivery)
                require(target_data == source_data, "outer publication copy differs")
                evidence = {"file": str(Path(target_file).absolute()), "receivingPath": destination}
                kind = "VERIFIED_PHYSICAL_PUBLICATION_COPY"
            require(destination.startswith("m3/migration/intake-20261005/current-752-ci/"),
                    "receiving evidence is outside the admitted task")
            new_rows.append({"sourcePath": path, "sourceFile": str(Path(file).absolute()),
                             "sha256": source["sha256"], "bytes": source["bytes"], "gitBlob": blob,
                             "segment": segment["id"], "classification": kind, "delivery": evidence})
            known.append(path)

    # Detect document mutation during collection without rewriting earlier evidence.
    for row in inputs.values():
        check(data_file(row["file"]), row)
    output = {"schema": "m3.source-delivery-append/1", "state": "READ_ONLY_CENSUS_APPEND",
              "label": plan["label"], "priorCoverage": plan["prior"],
              "inputs": sorted(inputs.values(), key=lambda row: row["file"]),
              "helperSHA256": sha(Path(__file__).read_bytes()),
              "priorSourcePathCount": len(known) - len(new_rows), "newSourcePathCount": len(new_rows),
              "newSourceBytes": sum(row["bytes"] for row in new_rows),
              "allSourcePathCount": len(known), "allSourcePaths": sorted(known), "newRows": new_rows,
              "pendingPublicationAdditionCount": pending, "publicationAdditions": [],
              "proofGatesExecuted": False, "priorCoverageModified": False, "consumerQualified": False,
              "boundary": "Content presence and explicit source-path mapping only; local publication copies are not remote publication or qualification. Prior pending additions remain pending."}
    with args.output.open("x") as stream:
        stream.write(json.dumps(output, indent=2, sort_keys=True) + "\n")
    print(json.dumps({"allSourcePaths": len(known), "newSourcePaths": len(new_rows),
                      "outputSHA256": sha(args.output.read_bytes())}))


if __name__ == "__main__":
    main()
