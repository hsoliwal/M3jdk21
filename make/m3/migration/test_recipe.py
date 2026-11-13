# SPDX-License-Identifier: Apache-2.0
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest import mock

spec = importlib.util.spec_from_file_location("recipe", Path(__file__).with_name("recipe.py"))
recipe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(recipe)

class RecipeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.home = Path(self.temp.name)
        self.workspace = self.home / "workspace"
        self.workspace.mkdir()
        self.resources = self.home / "resources"
        self.resources.mkdir()
        self.plan_path = self.resources / "plan.json"
        (self.workspace / "existing.txt").write_bytes(b"before\n")
        (self.workspace / "unrelated.txt").write_bytes(b"preserve\0\xff")
        self.plan = {"schema":recipe.SCHEMA, "recipe_id":"fixture/1", "outputs":[
            {"path":"existing.txt", "before":self.image("a.before",b"before\n"),
             "after":self.image("a.after",b"after\n")},
            {"path":"nested/new.txt", "before":None,
             "after":self.image("b.after",b"created\n")} ]}
        self.seal()
    def image(self, name, data):
        (self.resources/name).write_bytes(data)
        return {"resource":name,"sha256":recipe.digest(data)}
    def seal(self):
        self.plan.pop("plan_sha256",None)
        self.plan["plan_sha256"] = recipe.digest(recipe.canonical(self.plan))
        self.plan_path.write_text(json.dumps(self.plan))
    def run_mode(self, mode):
        return recipe.execute(self.plan_path,self.workspace,mode)
    def original(self):
        self.assertEqual((self.workspace/"existing.txt").read_bytes(),b"before\n")
        self.assertFalse((self.workspace/"nested/new.txt").exists())
        self.assertEqual((self.workspace/"unrelated.txt").read_bytes(),b"preserve\0\xff")
    def test_check_no_write(self):
        self.assertEqual(self.run_mode("check")["state"],"before");self.original()
    def test_apply_idempotent_rollback_replay(self):
        self.assertEqual(self.run_mode("apply")["writes"],2)
        self.assertEqual(self.run_mode("apply")["writes"],0)
        self.assertEqual(self.run_mode("check")["state"],"after")
        self.assertEqual(self.run_mode("rollback")["writes"],2);self.original()
        self.assertEqual(self.run_mode("rollback")["writes"],0)
        self.assertEqual(self.run_mode("apply")["writes"],2)
    def test_preimage_drift_preserves_other_outputs(self):
        (self.workspace/"existing.txt").write_text("user edit")
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        self.assertFalse((self.workspace/"nested/new.txt").exists())
    def test_postimage_drift_refuses_rollback(self):
        self.run_mode("apply");(self.workspace/"nested/new.txt").write_text("user edit")
        with self.assertRaises(recipe.Refusal):self.run_mode("rollback")
        self.assertEqual((self.workspace/"existing.txt").read_bytes(),b"after\n")
    def test_mixed_state_refused(self):
        (self.workspace/"existing.txt").write_bytes(b"after\n")
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        with self.assertRaises(recipe.Refusal):self.run_mode("check")
        self.assertFalse((self.workspace/"nested/new.txt").exists())
    def test_missing_required_preimage(self):
        (self.workspace/"existing.txt").unlink()
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
    def test_plan_seal_drift(self):
        self.plan["recipe_id"]="unsealed";self.plan_path.write_text(json.dumps(self.plan))
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        self.original()
    def test_resource_drift(self):
        (self.resources/"a.after").write_bytes(b"bad")
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        self.original()
    def test_duplicate_json_key(self):
        self.plan_path.write_text('{"schema":1,"schema":2}')
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
    def test_duplicate_output(self):
        self.plan["outputs"].append(self.plan["outputs"][0]);self.seal()
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
    def test_noop_output(self):
        self.plan["outputs"][0]["after"]=self.plan["outputs"][0]["before"];self.seal()
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
    def test_unsafe_destinations(self):
        for path in ("../escape","/absolute","a/../b","a//b","C:/escape","a\\b","./x"):
            with self.subTest(path=path):
                self.plan["outputs"][0]["path"]=path;self.seal()
                with self.assertRaises(recipe.Refusal):self.run_mode("apply")
                self.original()
    def test_resource_escape(self):
        self.plan["outputs"][0]["after"]["resource"]="../elsewhere";self.seal()
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        self.original()
    def test_symlink_parent(self):
        external=self.home/"external";external.mkdir()
        (self.workspace/"nested").symlink_to(external,target_is_directory=True)
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        self.assertEqual(list(external.iterdir()),[])
    def test_symlink_file(self):
        (self.workspace/"existing.txt").unlink()
        (self.workspace/"existing.txt").symlink_to(self.resources/"a.before")
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
    def test_output_directory(self):
        (self.workspace/"existing.txt").unlink();(self.workspace/"existing.txt").mkdir()
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
    def test_lock_refusal(self):
        (self.workspace/recipe.LOCK).write_text("another-plan")
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        self.original()
    def test_interrupted_write_rolls_back(self):
        actual=recipe.atomic_write
        def fail_once(path,data):
            if path.name=="new.txt":raise OSError("injected failure")
            return actual(path,data)
        with mock.patch.object(recipe,"atomic_write",side_effect=fail_once):
            with self.assertRaises(OSError):self.run_mode("apply")
        self.original();self.assertFalse((self.workspace/recipe.JOURNAL).exists())
        self.assertFalse((self.workspace/recipe.LOCK).exists())
    def crash(self, restore="before"):
        (self.workspace/recipe.LOCK).write_text(self.plan["plan_sha256"]+"\n")
        (self.workspace/recipe.JOURNAL).write_text(json.dumps({
            "plan_sha256":self.plan["plan_sha256"],"restore_state":restore,
            "paths":[r["path"] for r in self.plan["outputs"]]}))
    def test_explicit_crash_recovery(self):
        (self.workspace/"existing.txt").write_bytes(b"after\n");self.crash()
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        self.assertEqual(self.run_mode("recover")["state"],"recovered");self.original()
    def test_recovery_preserves_foreign_edits(self):
        (self.workspace/"existing.txt").write_bytes(b"foreign\n");self.crash()
        with self.assertRaises(recipe.Refusal):self.run_mode("recover")
        self.assertEqual((self.workspace/"existing.txt").read_bytes(),b"foreign\n")
    def test_recovery_foreign_plan(self):
        self.crash();(self.workspace/recipe.LOCK).write_text("foreign")
        with self.assertRaises(recipe.Refusal):self.run_mode("recover")
        self.original()
    def test_invalid_recovery_state(self):
        self.crash("arbitrary")
        with self.assertRaises(recipe.Refusal):self.run_mode("recover")
        self.original()
    def test_lock_only_recovery(self):
        (self.workspace/recipe.LOCK).write_text(self.plan["plan_sha256"])
        self.assertEqual(self.run_mode("recover")["state"],"recovered-lock-only");self.original()
    def test_mode_preserved(self):
        (self.workspace/"existing.txt").chmod(0o755)
        self.run_mode("apply");self.run_mode("rollback")
        self.assertEqual((self.workspace/"existing.txt").stat().st_mode & 0o777,0o755)
    def test_dependency_guard(self):
        self.plan["guards"]=[{"path":"unrelated.txt","sha256":recipe.digest(b"preserve\0\xff")}]
        self.seal();self.run_mode("check")
        (self.workspace/"unrelated.txt").write_text("dependency change")
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        self.assertEqual((self.workspace/"existing.txt").read_bytes(),b"before\n")
    def test_guard_missing(self):
        self.plan["guards"]=[{"path":"missing","sha256":"0"*64}];self.seal()
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        self.original()
    def test_guard_overlap(self):
        self.plan["guards"]=[{"path":"existing.txt","sha256":recipe.digest(b"before\n")}];self.seal()
        with self.assertRaises(recipe.Refusal):self.run_mode("apply")
        self.original()
    def test_empty_file_not_absent(self):
        self.plan["outputs"][0]["after"]=self.image("empty",b"");self.seal()
        self.run_mode("apply");self.assertTrue((self.workspace/"existing.txt").is_file())
        self.assertEqual((self.workspace/"existing.txt").read_bytes(),b"")
        self.run_mode("rollback");self.original()

if __name__=="__main__":unittest.main(verbosity=2)
