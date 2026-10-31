#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify the retained installer and foundation environment wiring for this exact crate.

The wiring control stubs acquisition, Java and Maven. It proves environment propagation
and command preservation only; the separate Maven lane executes the actual recipe tests.
"""
from __future__ import annotations
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import textwrap
import unittest

INSTALLER_SHA = "453d976a5f29526432be138b357d3f52f868fcc40a4ee697d969cc0836359628"
PLAN_SHA = "c096db7ea053cbfc2885714dd3abfdc521d703fa688cf2abe3b100aabb75700c"
RECEIPTS = []
REFUSALS = []
REPO = PLAN = OUT = OWNER = None

def digest(data):
    return hashlib.sha256(data).hexdigest()

def capture_tree(root):
    return {p.relative_to(root).as_posix(): (digest(p.read_bytes()), p.stat().st_mode & 0o777)
            for p in root.rglob("*") if p.is_file()}

def seed(root):
    root.mkdir(parents=True, exist_ok=True)
    plan = json.loads(PLAN.read_text())
    for guard in plan["guards"]:
        source = REPO / guard["path"]
        assert digest(source.read_bytes()) == guard["sha256"], guard["path"]
        path = root/guard["path"]
        path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, path)
    for row in plan["outputs"]:
        source = PLAN.parent/row["before"]["resource"]
        path = root/row["path"]
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(source.read_bytes())
        path.chmod(0o640)
    return root, plan

def expected_images(root, plan, state):
    for row in plan["outputs"]:
        assert (root/row["path"]).read_bytes() == (PLAN.parent/row[state]["resource"]).read_bytes()
        assert (root/row["path"]).stat().st_mode & 0o777 == 0o640

def run_section(yaml, step):
    marker = "      - name: "+step+"\n"
    start = yaml.index(marker) + len(marker)
    end = yaml.find("\n      - ", start)
    section = yaml[start:] if end < 0 else yaml[start:end]
    rows = section.splitlines()
    index = next(i for i,line in enumerate(rows) if line.startswith("        run:"))
    inline = rows[index].split("run:",1)[1].strip()
    return textwrap.dedent("\n".join(rows[index+1:]))+"\n" if inline == "|" else inline+"\n"

class InstallerTests(unittest.TestCase):
    def test_lifecycle_materializes_three_files_and_preserves_modes(self):
        root, plan = seed(OUT/"installer-workspace")
        for name, mode, writes in (
                ("check-before","check",0),("apply","apply",3),("fixed-point","apply",0),
                ("rollback","rollback",3),("replay","apply",3),("check-after","check",0)):
            result = OWNER.execute(PLAN, root, mode)
            self.assertEqual(writes,result["writes"])
            self.assertEqual(3,result["files"])
            RECEIPTS.append({"stage":name,"result":result})
            expected_images(root, plan, "before" if name in ("check-before","rollback") else "after")
        self.assertFalse((root/OWNER.LOCK).exists())
        self.assertFalse((root/OWNER.JOURNAL).exists())

    def test_each_target_drift_refuses_before_any_write(self):
        for index in range(3):
            for mode in ("check","apply","rollback"):
                with tempfile.TemporaryDirectory(prefix="m3-ci-target-") as directory:
                    root,plan=seed(Path(directory))
                    row=plan["outputs"][index]
                    path=root/row["path"]
                    path.write_bytes(path.read_bytes()+b"\nexternal drift\n")
                    before=capture_tree(root)
                    with self.assertRaisesRegex(OWNER.Refusal,"destination drift"):
                        OWNER.execute(PLAN,root,mode)
                    self.assertEqual(before,capture_tree(root))
                    REFUSALS.append({"kind":"target-drift","path":row["path"],"mode":mode})

    def test_each_guard_drift_refuses_before_any_write(self):
        guards=json.loads(PLAN.read_text())["guards"]
        self.assertEqual(5,len(guards))
        for index in range(len(guards)):
            for mode in ("check","apply","rollback"):
                with tempfile.TemporaryDirectory(prefix="m3-ci-guard-") as directory:
                    root,plan=seed(Path(directory))
                    guard=plan["guards"][index]
                    path=root/guard["path"]
                    path.write_bytes(path.read_bytes()+b"\nexternal guard drift\n")
                    before=capture_tree(root)
                    with self.assertRaisesRegex(OWNER.Refusal,"dependency guard drift"):
                        OWNER.execute(PLAN,root,mode)
                    self.assertEqual(before,capture_tree(root))
                    REFUSALS.append({"kind":"guard-drift","path":guard["path"],"mode":mode})

    def test_mixed_states_refuse_before_any_write(self):
        for mode in ("check","apply","rollback"):
            with tempfile.TemporaryDirectory(prefix="m3-ci-mixed-") as directory:
                root,plan=seed(Path(directory))
                row=plan["outputs"][1]
                (root/row["path"]).write_bytes((PLAN.parent/row["after"]["resource"]).read_bytes())
                before=capture_tree(root)
                with self.assertRaisesRegex(OWNER.Refusal,"mixed before/after state"):
                    OWNER.execute(PLAN,root,mode)
                self.assertEqual(before,capture_tree(root))
                REFUSALS.append({"kind":"mixed-state","mode":mode})

    def test_before_and_after_resource_drift_refuse_before_any_write(self):
        for state in ("before","after"):
            for mode in ("check","apply","rollback"):
                with tempfile.TemporaryDirectory(prefix="m3-ci-resource-") as directory:
                    parent=Path(directory)
                    root,plan=seed(parent/"workspace")
                    resource_dir=parent/"resources"
                    shutil.copytree(PLAN.parent,resource_dir)
                    row=plan["outputs"][0]
                    changed=resource_dir/row[state]["resource"]
                    changed.write_bytes(changed.read_bytes()+b"\nresource drift\n")
                    before=capture_tree(root)
                    with self.assertRaisesRegex(OWNER.Refusal,state+" resource drift"):
                        OWNER.execute(resource_dir/PLAN.name,root,mode)
                    self.assertEqual(before,capture_tree(root))
                    REFUSALS.append({"kind":state+"-resource-drift","mode":mode})

    def test_foundation_selects_unchanged_pinned_jdk_for_next_maven_step(self):
        plan=json.loads(PLAN.read_text())
        row=next(x for x in plan["outputs"] if x["path"]==".github/workflows/m3-foundation.yml")
        observations={}
        for state in ("before","after"):
            yaml=(PLAN.parent/row[state]["resource"]).read_text()
            prepare=run_section(yaml,"Prepare isolated pinned compiler")
            verify=run_section(yaml,"Verify OpenRewrite convergence DAG")
            with tempfile.TemporaryDirectory(prefix="m3 ci wiring ") as directory:
                root=Path(directory)
                selected=root/"jdk-21+35"
                unselected=root/"unselected-jdk"
                maven=root/"maven/bin"
                for path in (selected/"bin",unselected/"bin",maven):
                    path.mkdir(parents=True)
                for home in (selected,unselected):
                    java=home/"bin/java"
                    java.write_text("#!/bin/bash\nprintf '%s\\n' \"$0\" >> \"$JAVA_OBSERVATIONS\"\n")
                    java.chmod(0o755)
                mvn=maven/"mvn"
                mvn.write_text("#!/bin/bash\nprintf '%s\\n' \"$JAVA_HOME\" >> \"$MAVEN_HOMES\"\nprintf '%s\\n' \"$*\" >> \"$MAVEN_ARGS\"\n")
                mvn.chmod(0o755)
                envfile=root/"environment"
                pathfile=root/"path"
                envfile.touch()
                pathfile.touch()
                env=dict(os.environ, RUNNER_TEMP=str(root),GITHUB_ENV=str(envfile),GITHUB_PATH=str(pathfile),
                         JAVA_HOME=str(unselected),PATH=str(unselected/"bin")+":"+str(maven)+":"+os.environ["PATH"],
                         JAVA_OBSERVATIONS=str(root/"java-observations"),MAVEN_HOMES=str(root/"maven-homes"),
                         MAVEN_ARGS=str(root/"maven-args"),ACQUISITION=str(root/"acquisition"),HASH_INPUT=str(root/"hash-input"))
                stubs=("curl() { printf '%s\\n' \"$*\" >> \"$ACQUISITION\"; }\n"
                       "sha256sum() { cat > \"$HASH_INPUT\"; }\n"
                       "tar() { :; }\n")
                subprocess.run(["bash","-n"],input=prepare+"\n"+verify,text=True,check=True,capture_output=True)
                subprocess.run(["bash","-euo","pipefail","-c",stubs+prepare],env=env,text=True,check=True,capture_output=True)
                selected_env=dict(env)
                for line in envfile.read_text().splitlines():
                    key,value=line.split("=",1)
                    selected_env[key]=value
                paths=pathfile.read_text().splitlines()
                selected_env["PATH"]=":".join(paths+[env["PATH"]])
                subprocess.run(["bash","-euo","pipefail","-c",verify],env=selected_env,text=True,check=True,capture_output=True)
                homes=(root/"maven-homes").read_text().splitlines()
                args=(root/"maven-args").read_text().splitlines()
                acquisition=(root/"acquisition").read_text()
                hash_input=(root/"hash-input").read_text()
                self.assertIn("OpenJDK21U-jdk_x64_linux_hotspot_21_35.tar.gz",acquisition)
                self.assertIn("82f64c53acaa045370d6762ebd7441b74e6fda14b464d54d1ff8ca941ec069e6",hash_input)
                self.assertIn("-B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify",args)
                self.assertEqual(str(selected),selected_env["M3_JDK"])
                if state=="before":
                    self.assertEqual([str(unselected)],homes)
                    self.assertEqual([],paths)
                else:
                    self.assertEqual([str(selected),str(selected)],homes)
                    self.assertEqual([str(selected/"bin")],paths)
                    self.assertEqual([str(selected/"bin/java")],(root/"java-observations").read_text().splitlines())
                    self.assertEqual(["-version","-B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify"],args)
                observations[state]={"maven_calls":len(homes),"selected_pinned_jdk":all(x==str(selected) for x in homes),
                                     "verification_arguments":args,"archive_name_preserved":True,"archive_sha256_preserved":True}
        RECEIPTS.append({"stage":"foundation-wiring-control","mode":"MOCKED_ACQUISITION_AND_JAVA_AND_MAVEN",
                         "actual_maven_executed":False,"actual_archive_acquired":False,"observations":observations})

def main():
    global REPO,PLAN,OUT,OWNER
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo",type=Path,required=True)
    parser.add_argument("--plan",type=Path,required=True)
    parser.add_argument("--out",type=Path,required=True)
    args=parser.parse_args()
    REPO=args.repo.resolve();PLAN=args.plan.resolve();OUT=args.out.resolve()
    OUT.mkdir(parents=True,exist_ok=False)
    owner_path=REPO/"m3/migration/recipe.py"
    if digest(owner_path.read_bytes())!=INSTALLER_SHA:
        raise RuntimeError("installer identity drift")
    spec=importlib.util.spec_from_file_location("retained_m3_installer",owner_path)
    OWNER=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(OWNER)
    plan,_=OWNER.sealed_plan(PLAN)
    if plan["plan_sha256"]!=PLAN_SHA:
        raise RuntimeError("plan identity drift")
    result=unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(InstallerTests))
    report={"schema":"m3-jcc-ci-installer-tests/1","tests_run":result.testsRun,"failures":len(result.failures),
            "errors":len(result.errors),"skipped":len(result.skipped),"successful":result.wasSuccessful(),
            "installer_sha256":INSTALLER_SHA,"plan_sha256":PLAN_SHA,"receipts":RECEIPTS,"refusals":REFUSALS,
            "refusal_call_count":len(REFUSALS),"scope":"Six installer/wiring methods; the wiring method uses explicitly mocked acquisition, Java and Maven."}
    (OUT/"receipt.json").write_text(json.dumps(report,indent=2)+"\n")
    return 0 if result.wasSuccessful() else 1

if __name__=="__main__":
    raise SystemExit(main())

