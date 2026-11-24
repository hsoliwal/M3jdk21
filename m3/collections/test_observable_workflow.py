# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Source-backed wiring regressions for the two standalone observable contracts.

This reads only this workflow's run blocks; it does not interpret general YAML
or execute shell, compile Java, or qualify the Java 21 runtime contracts.
"""

from pathlib import Path
import json
import re
import shlex
import tempfile
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = Path(".github/workflows/m3-observable-indexed-views-20261021.yml")
FILTERS = Path(".github/checks-superset/filters.json")
CONTRACTS = {"ObservableListContract", "ObservableIndexedViewsContract"}


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def run_commands(workflow):
    """Read the existing one-line and literal-block run forms, failing closed."""
    lines = workflow.splitlines()
    commands = []
    for index, line in enumerate(lines):
        match = re.fullmatch(r"( +)run: *(.*)", line)
        if match is None:
            continue
        indent, value = match.groups()
        if value == "|":
            body = []
            for following in lines[index + 1:]:
                if following.strip() and len(following) - len(following.lstrip()) <= len(indent):
                    break
                body.append(following)
            value = "\n".join(body)
        else:
            require(not value.startswith((">", "|")), "unsupported run scalar")
        for logical in re.sub(r"\\\n\s*", " ", value).splitlines():
            if re.match(r"\s*(?:javac|java)\s", logical):
                commands.append(shlex.split(logical))
    return commands


def pull_request_paths(workflow, registry=None, *, root=ROOT):
    """Read live inline filters or the registered superset member, never comments.

    The original workflow is now dispatch-only. filters.json owns its PR
    routing; validate member identity and unique shard custody before using it.
    The optional registry is an immutable test input for refusal fixtures.
    Otherwise the registry belongs to the inspected source root.
    """
    lines = workflow.splitlines()
    matches = [i for i, line in enumerate(lines) if line == "  pull_request:"]
    if not matches:
        events = re.findall(r"(?m)^on:\n((?:[ \t]+[^\n]*\n|\n)*)", workflow)
        require(len(events) == 1 and events[0].strip() == "workflow_dispatch:",
                "expected dispatch-only member or one pull_request event")
        if registry is None:
            registry = json.loads((root / FILTERS).read_text(encoding="utf-8"))
        require(isinstance(registry, dict), "invalid superset registry")
        members, shards = registry.get("members"), registry.get("shards")
        require(isinstance(members, dict) and isinstance(shards, dict), "invalid superset registry")
        member = members.get(WORKFLOW.stem)
        require(isinstance(member, dict) and member.get("file") == WORKFLOW.name
                and member.get("key") == WORKFLOW.stem, "superset member identity mismatch")
        shard = member.get("shard")
        require(isinstance(shard, str) and re.fullmatch(r"[0-9]{2}", shard),
                "invalid superset shard identifier")
        require(all(isinstance(names, list) and all(isinstance(n, str) for n in names)
                    for names in shards.values()), "invalid shard membership")
        custody = [shard for shard, names in shards.items() for name in names if name == WORKFLOW.stem]
        require(custody == [shard], "superset member must have one matching shard")
        event = member.get("pull_request")
        require(isinstance(event, dict) and "paths-ignore" not in event,
                "missing or unsupported registered pull_request event")
        paths = event.get("paths")
        require(isinstance(paths, list) and paths and all(isinstance(p, str) and p for p in paths),
                "invalid registered pull_request paths")
        require(len(paths) == len(set(paths)) and not any(p.startswith("!") for p in paths),
                "duplicate or negative registered pull_request paths")
        return list(paths)
    require(len(matches) == 1, "expected one pull_request event")
    paths = []
    in_paths = False
    for line in lines[matches[0] + 1:]:
        if line.strip() and len(line) - len(line.lstrip()) <= 2:
            break
        if line == "    paths:":
            in_paths = True
        elif in_paths and line.startswith("      - "):
            values = shlex.split(line[8:])
            require(len(values) == 1, "expected one path per filter")
            paths.append(values[0])
        elif line.strip():
            require(False, "unsupported pull_request path layout")
    require(in_paths, "missing pull_request paths")
    return paths


def java_identity(root, relative, source_roots):
    require(not relative.is_absolute() and ".." not in relative.parts,
            "source path must stay in the checkout")
    source = root / relative
    require(source.is_file(), f"missing compile source: {relative}")
    text = source.read_text(encoding="utf-8")
    package = re.search(r"(?m)^\s*package\s+([\w.]+)\s*;", text)
    public_class = re.search(r"(?m)^public\s+(?:final\s+)?class\s+(\w+)\b", text)
    require(package is not None and public_class is not None,
            f"missing Java declaration: {relative}")
    package_name = package.group(1)
    class_name = public_class.group(1)
    suffix = Path(*package_name.split(".")) / (class_name + ".java")
    require(any(relative == source_root / suffix for source_root in source_roots),
            f"source identity path mismatch: {relative}")
    return package_name + "." + class_name, text


def module_source_inputs(root, workflow, compiler, descriptor, filters):
    """Verify the complete actual Java21 module tree is an explicit javac input.

    Both declared source roots must be scanned; selecting only collections
    and internal.collections hides exported packages and weakens the build.
    The sorted, zero-terminated full finder is the sole accepted grammar.
    """
    pattern = (r"(?m)^\s*mapfile -d '' -t ([A-Za-z_][A-Za-z_0-9]*) < <\(\s*\n"
               r"\s*(find [^\n]+)\n\s*\)")
    blocks = re.findall(r"(?m)^        run: \|\n((?:          [^\n]*\n|\n)*)", workflow)
    compile_blocks = [block for block in blocks
                      if compiler in run_commands("        run: |\n" + block)]
    require(len(compile_blocks) == 1, "expected one literal collections compile block")
    declarations = []
    for variable, finder in re.findall(pattern, compile_blocks[0]):
        arguments = shlex.split(finder)
        if "module-info.java" in arguments:
            declarations.append((variable, arguments))
    require(len(declarations) == 1, "expected one complete module-source finder")
    variable, finder = declarations[0]
    require(compiler.count("$" + "{" + variable + "[@]}") == 1,
            "full module sources must be actual javac operands")
    expected_roots = ["m3/collections/src/main/java",
                      "src/jdk.m3.collections/share/classes/jdk/m3/collections",
                      "src/jdk.m3.collections/share/classes/jdk/internal/m3/collections"]
    # Never admit the sibling jdk.m3.arrays package from the old flattened
    # directory: it belongs to an independently built module.
    require(finder[0] == "find" and finder[1:4] == expected_roots,
            "finder must use the complete module source roots")
    require(finder[4:] == ["-type", "f", "-name", "*.java", "!", "-name",
                           "module-info.java", "-print0", "|", "sort", "-z"],
            "unsupported module-source finder")
    require(descriptor.parent.as_posix() == expected_roots[0],
            "module descriptor outside the canonical source root")
    for source_root in expected_roots:
        require(source_root + "/**" in filters,
                "complete module source tree needs a PR path filter")
    sources = sorted({source for source_root in expected_roots
                      for source in (root / source_root).rglob("*.java")
                      if source.is_file() and not source.is_symlink()
                      and source.name != "module-info.java"})
    require(sources, "module-source finder must select real sources")
    packages = set()
    for source in sources:
        if source.name == "package-info.java":
            continue
        package = re.search(r"(?m)^\s*package\s+([\w.]+)\s*;",
                            source.read_text(encoding="utf-8"))
        if package:
            packages.add(package.group(1))
    exports = set(re.findall(r"(?m)^\s*exports\s+([\w.]+)(?:\s+to\s+[^;]+)?\s*;",
                             (root / descriptor).read_text(encoding="utf-8")))
    require(exports <= packages, "every exported package must have actual compiled source")
    return {"source_array": variable, "module_source_roots": expected_roots,
            "module_inputs": [p.relative_to(root).as_posix() for p in sources]}

def inspect_wiring(root, workflow=None, *, verify_module=True, registry=None):
    """Cross-check actual compile operands, declarations, launches and filters."""
    if workflow is None:
        workflow = (root / WORKFLOW).read_text(encoding="utf-8")
    commands = run_commands(workflow)
    compilers = [c for c in commands if c[0] == "javac" and "-sourcepath" in c]
    require(len(compilers) == 1, "expected one collections compile command")
    compiler = compilers[0]
    source_roots = [Path(p) for p in compiler[compiler.index("-sourcepath") + 1].split(":")]
    require(source_roots == [Path("m3/collections/src/main/java"),
                             Path("src/jdk.m3.collections/share/classes"),
                             Path("test/jdk")],
            "sourcepath must include the two full product roots and test/jdk")
    explicit = [Path(arg) for arg in compiler if arg.endswith(".java")]
    require(len(explicit) == len(set(explicit)), "duplicate explicit Java input")
    descriptors = [p for p in explicit if p.name == "module-info.java"]
    require(len(descriptors) == 1, "expected one explicit module descriptor")
    descriptor = root / descriptors[0]
    require(descriptor.is_file(), "missing module descriptor")
    declaration = re.search(r"(?m)^\s*module\s+([\w.]+)\s*\{",
                            descriptor.read_text(encoding="utf-8"))
    require(declaration is not None, "missing module declaration")
    module = declaration.group(1)
    inputs = {}
    for relative in explicit:
        if relative in descriptors:
            continue
        identity, text = java_identity(root, relative, source_roots)
        require(re.search(r"public\s+static\s+void\s+main\s*\(\s*String\[\]\s+\w+\s*\)", text),
                f"missing standalone main: {relative}")
        inputs[identity] = relative
    require({name.rsplit(".", 1)[1] for name in inputs} == CONTRACTS,
            "both contract inputs must be explicit javac operands")
    launches = [c[c.index("-m") + 1] for c in commands if c[0] == "java" and "-m" in c]
    require(len(launches) == 2 and len(set(launches)) == 2,
            "expected both distinct standalone launches")
    for launch in launches:
        launch_module, separator, identity = launch.partition("/")
        require(separator and launch_module == module, f"launch module mismatch: {launch}")
        require(identity in inputs, f"launch is not an explicit compiled main: {launch}")
    require({p.split("/", 1)[1] for p in launches} == set(inputs),
            "every compiled contract must be launched")

    # The List contract shares its package with its actual implementation owner.
    list_identity = next(name for name in inputs if name.endswith(".ObservableListContract"))
    owner_identity = list_identity.removesuffix("Contract")
    owner_suffix = Path(*owner_identity.split(".")).with_suffix(".java")
    owners = [source_root / owner_suffix for source_root in source_roots
              if (root / source_root / owner_suffix).is_file()]
    require(len(owners) == 1, "expected one observable owner in the source path")
    identity, _ = java_identity(root, owners[0], source_roots)
    require(identity == owner_identity, "observable owner declaration mismatch")
    filters = pull_request_paths(workflow, registry, root=root)
    required = [*inputs.values(), owners[0], descriptors[0], WORKFLOW,
                Path("m3/collections/test_observable_workflow.py")]
    for relative in required:
        require(relative.as_posix() in filters, f"missing exact PR path filter: {relative}")
    module_inputs = (module_source_inputs(root, workflow, compiler, descriptors[0], filters)
                     if verify_module else {"source_array": "collection_sources",
                                            "module_source_roots": [
                                                "m3/collections/src/main/java",
                                                "src/jdk.m3.collections/share/classes/jdk/m3/collections",
                                                "src/jdk.m3.collections/share/classes/jdk/internal/m3/collections"],
                                            "module_inputs": []})
    return {"module": module, "compiled_inputs": [p.as_posix() for p in explicit],
            "launches": launches, "owner": owners[0].as_posix(),
            "qualification": "source_wiring_only_java21_not_executed", **module_inputs}


class ObservableWorkflowWiringTest(unittest.TestCase):
    def setUp(self):
        self.workflow = (ROOT / WORKFLOW).read_text(encoding="utf-8")

    def test_checked_in_workflow_matches_actual_sources(self):
        inspect_wiring(ROOT, self.workflow)

    def test_compile_omission_is_not_masked_by_path_filter(self):
        paths = inspect_wiring(ROOT, self.workflow)["compiled_inputs"]
        contract = next(p for p in paths if p.endswith("ObservableIndexedViewsContract.java"))
        self.assertIn(contract, pull_request_paths(self.workflow))
        changed = self.workflow.replace("            " + contract + "\n", "\n", 1)
        self.assertNotEqual(changed, self.workflow)
        with self.assertRaisesRegex(AssertionError, "both contract inputs"):
            inspect_wiring(ROOT, changed)

    def test_absent_compile_path_is_rejected(self):
        paths = inspect_wiring(ROOT, self.workflow)["compiled_inputs"]
        contract = next(p for p in paths if p.endswith("ObservableListContract.java"))
        changed = self.workflow.replace("            " + contract,
                                        "            " + contract + ".absent.java", 1)
        with self.assertRaisesRegex(AssertionError, "missing compile source"):
            inspect_wiring(ROOT, changed)

    def test_stale_launcher_is_rejected(self):
        launch = inspect_wiring(ROOT, self.workflow)["launches"][0]
        module, identity = launch.split("/", 1)
        changed = self.workflow.replace(launch, module + "/stale.package." + identity.rsplit(".", 1)[1])
        with self.assertRaisesRegex(AssertionError, "not an explicit compiled main"):
            inspect_wiring(ROOT, changed)

    def test_package_declaration_drift_is_rejected(self):
        wiring = inspect_wiring(ROOT, self.workflow)
        with tempfile.TemporaryDirectory(prefix="m3-observable-wiring-") as temporary:
            root = Path(temporary)
            for relative in wiring["compiled_inputs"] + [wiring["owner"], FILTERS.as_posix()]:
                target = root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((ROOT / relative).read_bytes())
            contract = root / next(p for p in wiring["compiled_inputs"]
                                   if p.endswith("ObservableListContract.java"))
            contract.write_text(re.sub(r"(?m)^(package\s+)[\w.]+;", r"\1stale.package;",
                                       contract.read_text(encoding="utf-8")), encoding="utf-8")
            with self.assertRaisesRegex(AssertionError, "source identity path mismatch"):
                inspect_wiring(root, self.workflow, verify_module=False)

    def test_wrong_launch_module_is_rejected(self):
        launch = inspect_wiring(ROOT, self.workflow)["launches"][0]
        changed = self.workflow.replace(launch, "wrong.module/" + launch.split("/", 1)[1])
        with self.assertRaisesRegex(AssertionError, "launch module mismatch"):
            inspect_wiring(ROOT, changed)

    def test_missing_contract_trigger_is_rejected(self):
        wiring = inspect_wiring(ROOT, self.workflow)
        contract = next(p for p in wiring["compiled_inputs"] if p.endswith("ObservableListContract.java"))
        registry = json.loads((ROOT / FILTERS).read_text(encoding="utf-8"))
        registry["members"][WORKFLOW.stem]["pull_request"]["paths"].remove(contract)
        with self.assertRaisesRegex(AssertionError, "missing exact PR path filter"):
            inspect_wiring(ROOT, self.workflow, registry=registry)


    def test_missing_module_input_array_is_rejected(self):
        wiring = inspect_wiring(ROOT, self.workflow)
        operand = '"${' + wiring["source_array"] + '[@]}"'
        changed = self.workflow.replace("            " + operand + " \\\n", "", 1)
        self.assertNotEqual(changed, self.workflow)
        with self.assertRaisesRegex(AssertionError, "full module sources must be actual javac operands"):
            inspect_wiring(ROOT, changed)

    def test_narrowed_module_source_root_is_rejected(self):
        wiring = inspect_wiring(ROOT, self.workflow)
        source_root = wiring["module_source_roots"][0]
        changed = self.workflow.replace("find " + source_root,
                                        "find " + source_root + "/jdk/m3/collections", 1)
        self.assertNotEqual(changed, self.workflow)
        with self.assertRaisesRegex(AssertionError, "complete module source root"):
            inspect_wiring(ROOT, changed)

    def test_arrays_owner_subtree_must_not_enter_collections_module(self):
        inspect_wiring(ROOT, self.workflow)
        separate = (
            "src/jdk.m3.collections/share/classes/jdk/m3/collections "
            "src/jdk.m3.collections/share/classes/jdk/internal/m3/collections"
        )
        self.assertIn(separate, self.workflow)
        changed = self.workflow.replace(
            separate, "src/jdk.m3.collections/share/classes", 1)
        with self.assertRaisesRegex(AssertionError, "complete module source roots"):
            inspect_wiring(ROOT, changed)

    def test_unbacked_exported_package_is_rejected(self):
        wiring = inspect_wiring(ROOT, self.workflow)
        with tempfile.TemporaryDirectory(prefix="m3-observable-exports-") as temporary:
            root = Path(temporary)
            for relative in sorted(set(wiring["compiled_inputs"] + wiring["module_inputs"]
                                       + [FILTERS.as_posix()])):
                target = root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((ROOT / relative).read_bytes())
            descriptor = root / next(p for p in wiring["compiled_inputs"] if p.endswith("module-info.java"))
            source = descriptor.read_text(encoding="utf-8")
            descriptor.write_text(source.replace("\n}", "\n    exports missing.exported.contracts;\n}", 1),
                                  encoding="utf-8")
            with self.assertRaisesRegex(AssertionError, "exported package must have actual compiled source"):
                inspect_wiring(root, self.workflow)


    def test_missing_complete_module_trigger_is_rejected(self):
        wiring = inspect_wiring(ROOT, self.workflow)
        registry = json.loads((ROOT / FILTERS).read_text(encoding="utf-8"))
        registry["members"][WORKFLOW.stem]["pull_request"]["paths"].remove(
                wiring["module_source_roots"][0] + "/**")
        with self.assertRaisesRegex(AssertionError, "complete module source tree needs a PR path filter"):
            inspect_wiring(ROOT, self.workflow, registry=registry)

    def test_archived_trigger_comments_are_not_authority(self):
        changed = re.sub(r"(?m)^#.*\n", "", self.workflow)
        self.assertEqual(pull_request_paths(self.workflow), pull_request_paths(changed))
        with patch.object(Path, "read_text", side_effect=FileNotFoundError("missing registry")):
            with self.assertRaises(FileNotFoundError):
                pull_request_paths(self.workflow)

    def test_inline_pull_request_layout_is_still_checked(self):
        self.assertEqual(["src/**"], pull_request_paths("on:\n  pull_request:\n    paths:\n      - 'src/**'\n"))
        with self.assertRaisesRegex(AssertionError, "expected one pull_request event"):
            pull_request_paths("on:\n  pull_request:\n  pull_request:\n")

    def test_invalid_registry_custody_and_filters_are_rejected(self):
        original = json.loads((ROOT / FILTERS).read_text(encoding="utf-8"))
        mutations = [
            lambda r: r["members"].pop(WORKFLOW.stem),
            lambda r: r["members"][WORKFLOW.stem].update(file="wrong.yml"),
            lambda r: r["members"][WORKFLOW.stem].update(key="wrong"),
            lambda r: r["members"][WORKFLOW.stem].update(shard="99"),
            lambda r: r["shards"]["03"].remove(WORKFLOW.stem),
            lambda r: r["shards"]["03"].append(WORKFLOW.stem),
            lambda r: r["shards"].update(other=[WORKFLOW.stem]),
            lambda r: r["members"][WORKFLOW.stem].pop("pull_request"),
            lambda r: r["members"][WORKFLOW.stem]["pull_request"].update({"paths-ignore": ["**"]}),
            lambda r: r["members"][WORKFLOW.stem]["pull_request"].update(paths="**"),
            lambda r: r["members"][WORKFLOW.stem]["pull_request"].update(paths=[]),
            lambda r: r["members"][WORKFLOW.stem]["pull_request"].update(paths=[1]),
            lambda r: r["members"][WORKFLOW.stem]["pull_request"].update(paths=["a", "a"]),
            lambda r: r["members"][WORKFLOW.stem]["pull_request"].update(paths=["!**"]),
        ]
        for index, mutate in enumerate(mutations):
            with self.subTest(index=index):
                registry = json.loads(json.dumps(original))
                mutate(registry)
                with self.assertRaises(AssertionError):
                    pull_request_paths(self.workflow, registry)
        for events in ("on:\n  push:\n", "on:\n  workflow_dispatch:\n  push:\n",
                       "on:\n  workflow_dispatch:\non:\n  workflow_dispatch:\n"):
            with self.subTest(events=events):
                with self.assertRaisesRegex(AssertionError, "dispatch-only"):
                    pull_request_paths(events, original)



    def test_inspected_root_missing_registry_is_rejected(self):
        wiring = inspect_wiring(ROOT, self.workflow)
        with tempfile.TemporaryDirectory(prefix="m3-observable-missing-registry-") as temporary:
            root = Path(temporary)
            for relative in wiring["compiled_inputs"] + [wiring["owner"]]:
                target = root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((ROOT / relative).read_bytes())
            with self.assertRaises(FileNotFoundError):
                inspect_wiring(root, self.workflow, verify_module=False)

    def test_inspected_root_registry_cannot_borrow_an_outer_contract_filter(self):
        wiring = inspect_wiring(ROOT, self.workflow)
        with tempfile.TemporaryDirectory(prefix="m3-observable-root-registry-") as temporary:
            root = Path(temporary)
            for relative in wiring["compiled_inputs"] + [wiring["owner"], FILTERS.as_posix()]:
                target = root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((ROOT / relative).read_bytes())
            registry = json.loads((root / FILTERS).read_text(encoding="utf-8"))
            contract = next(p for p in wiring["compiled_inputs"] if p.endswith("ObservableListContract.java"))
            registry["members"][WORKFLOW.stem]["pull_request"]["paths"].remove(contract)
            (root / FILTERS).write_text(json.dumps(registry), encoding="utf-8")
            with self.assertRaisesRegex(AssertionError, "missing exact PR path filter"):
                inspect_wiring(root, self.workflow, verify_module=False)

    def test_invalid_shard_identifier_refuses_even_matching_custody(self):
        original = json.loads((ROOT / FILTERS).read_text(encoding="utf-8"))
        for shard in ("3", "003", "not-a-shard"):
            with self.subTest(shard=shard):
                registry = json.loads(json.dumps(original))
                member = registry["members"][WORKFLOW.stem]
                previous = member["shard"]
                member["shard"] = shard
                registry["shards"][shard] = registry["shards"].pop(previous)
                with self.assertRaisesRegex(AssertionError, "invalid superset shard identifier"):
                    pull_request_paths(self.workflow, registry)


if __name__ == "__main__":
    unittest.main()
