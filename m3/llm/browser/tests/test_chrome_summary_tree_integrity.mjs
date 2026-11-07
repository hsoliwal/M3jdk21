import assert from "node:assert/strict";
import test from "node:test";
import { replaySummaryTreePlan, verifySummaryTreeIntegrity } from "../web/chrome_summary_tree_integrity.mjs";

const cfg = {
  max_leaf_bytes: 1024,
  min_leaf_bytes: 128,
  fanin: 3,
  max_node_output_bytes: 512,
  max_parent_input_bytes: 4096,
  max_nodes: 100000,
};

test("browser mechanical replay is deterministic", async () => {
  const source = ("αβγ🙂\nline one\nline two\n".repeat(120)) + "終わり";
  const a = await replaySummaryTreePlan(source, { sourceRef: "fixture", config: cfg });
  const b = await replaySummaryTreePlan(source, { sourceRef: "fixture", config: cfg });
  assert.deepEqual(a, b);
  await verifySummaryTreeIntegrity(a, source);
  assert.ok(a.leaf_count > 1);
  assert.equal(a.nodes.length, a.model_calls_planned);
  assert.equal(a.mechanical_root, "e8300489dc11a51478addbc473052c0f713f50faa4efec0e6ba4d85fca25d723");
});

test("newline at exclusive leaf boundary never widens the leaf", async () => {
  const source = `${"x".repeat(1024)}\n${"y".repeat(1024)}\n`;
  const plan = await replaySummaryTreePlan(source, { sourceRef: "boundary", config: cfg });
  const leaves = plan.nodes.filter((node) => node.kind === "LEAF");
  assert.ok(leaves.every((node) => node.end_byte - node.start_byte <= cfg.max_leaf_bytes));
});

test("tampered but source-preserving plan fails strict replay", async () => {
  const source = "row\n".repeat(2000);
  const plan = await replaySummaryTreePlan(source, { sourceRef: "fixture", config: cfg });
  const tampered = structuredClone(plan);
  tampered.nodes.at(-1).coverage_leaf_ordinals = [...tampered.nodes.at(-1).coverage_leaf_ordinals].reverse();
  await assert.rejects(() => verifySummaryTreeIntegrity(tampered, source), /mechanical_tree_replay_mismatch/);
});

test("source drift fails strict replay", async () => {
  const source = "abc\n".repeat(1000);
  const plan = await replaySummaryTreePlan(source, { sourceRef: "fixture", config: cfg });
  await assert.rejects(() => verifySummaryTreeIntegrity(plan, `${source}x`), /mechanical_tree_replay_mismatch/);
});
