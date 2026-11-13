# SPDX-License-Identifier: Apache-2.0
"""Source-backed wiring regressions for the two standalone observable contracts.

This reads only this workflow's run blocks; it does not interpret general YAML
or execute shell, compile Java, or qualify the Java 21 runtime contracts.
"""

from pathlib import Path
import re
import shlex
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[3]
WORKFLOW = Path(".github/workflows/m3-observable-indexed-views-20261021.yml")
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


def pull_request_paths(workflow):
    """Read the explicit path list in this workflow's pull_request event."""
    lines = workflow.splitlines()
    matches = [i for i, line in enumerate(lines) if line == "  pull_request:"]
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


def module_source_inputs(root, workflow, compiler, descriptor):
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
    filters = pull_request_paths(workflow)
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

def inspect_wiring(root, workflow=None, *, verify_module=True):
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
    filters = pull_request_paths(workflow)
    required = [*inputs.values(), owners[0], descriptors[0], WORKFLOW,
                Path("make/m3/collections/test_observable_workflow.py")]
    for relative in required:
        require(relative.as_posix() in filters, f"missing exact PR path filter: {relative}")
    module_inputs = (module_source_inputs(root, workflow, compiler, descriptors[0])
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
        self.assertIn("      - '" + contract + "'", self.workflow)
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
            for relative in wiring["compiled_inputs"] + [wiring["owner"]]:
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
        changed = self.workflow.replace("      - '" + contract + "'\n", "", 1)
        with self.assertRaisesRegex(AssertionError, "missing exact PR path filter"):
            inspect_wiring(ROOT, changed)


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
            for relative in sorted(set(wiring["compiled_inputs"] + wiring["module_inputs"])):
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
        trigger = "      - '" + wiring["module_source_roots"][0] + "/**'\n"
        changed = self.workflow.replace(trigger, "", 1)
        self.assertNotEqual(changed, self.workflow)
        with self.assertRaisesRegex(AssertionError, "complete module source tree needs a PR path filter"):
            inspect_wiring(ROOT, changed)


if __name__ == "__main__":
    unittest.main()
