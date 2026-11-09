import assert from "node:assert/strict";
import test from "node:test";
import { runSummaryTree, validateSummaryTreePlan } from "../web/chrome_summary_tree.mjs";

const encoder = new TextEncoder();

async function sha256Hex(bytes) {
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)]
    .map((value) => value.toString(16).padStart(2, "0"))
    .join("");
}

async function fixture() {
  const source = "alpha\nbeta\ngamma\n";
  const bytes = encoder.encode(source);
  const first = encoder.encode("alpha\n");
  const second = bytes.slice(first.length);
  const leaf0 = await sha256Hex(encoder.encode("leaf-0"));
  const leaf1 = await sha256Hex(encoder.encode("leaf-1"));
  const root = await sha256Hex(encoder.encode("root"));
  return {
    source,
    plan: {
      schema: "cpullm.chrome-summary-tree-plan.v1",
      revision: 1,
      source_ref: "fixture://tree",
      source_sha256: await sha256Hex(bytes),
      source_bytes: bytes.length,
      mechanical_root: await sha256Hex(encoder.encode("mechanical")),
      root_node_id: root,
      leaf_count: 2,
      level_count: 2,
      node_count: 3,
      model_calls_planned: 3,
      network_calls: 0,
      candidate_only: true,
      execution_authorized: false,
      merge_authorized: false,
      config: {
        max_leaf_bytes: 1024,
        min_leaf_bytes: 1,
        fanin: 2,
        max_node_output_bytes: 512,
        max_parent_input_bytes: 4096,
        max_nodes: 100,
      },
      nodes: [
        {
          kind: "LEAF", level: 0, ordinal: 0, node_id: leaf0,
          source_sha256: await sha256Hex(bytes), start_byte: 0, end_byte: first.length,
          input_sha256: await sha256Hex(first), children: [], coverage_leaf_ordinals: [0],
        },
        {
          kind: "LEAF", level: 0, ordinal: 1, node_id: leaf1,
          source_sha256: await sha256Hex(bytes), start_byte: first.length, end_byte: bytes.length,
          input_sha256: await sha256Hex(second), children: [], coverage_leaf_ordinals: [1],
        },
        {
          kind: "REDUCE", level: 1, ordinal: 0, node_id: root,
          children: [leaf0, leaf1], coverage_leaf_ordinals: [0, 1],
        },
      ],
    },
  };
}

async function fakeSummarizer(request) {
  return {
    provider: "Summarizer",
    candidate_id: request.candidate_id,
    summary: `summary:${request.input.replace(/\s+/g, " ").slice(0, 80)}`,
    candidate_only: true,
    admitted: false,
    execution_authorized: false,
    merge_authorized: false,
  };
}

function fakeApi({ availability = "available", quota = 1000000, longOutput = false } = {}) {
  const state = { creates: 0, destroys: 0, calls: 0, inputs: [] };
  return {
    state,
    async availability() { return availability; },
    async create() {
      state.creates += 1;
      return {
        inputQuota: quota,
        async measureInputUsage(text) { return encoder.encode(text).length; },
        async *summarizeStreaming(text) {
          state.calls += 1;
          state.inputs.push(text);
          const output = longOutput
            ? "x".repeat(2048)
            : `summary-${state.calls}:${text.replace(/\s+/g, " ").slice(0, 80)}`;
          const cut = Math.ceil(output.length / 2);
          yield output.slice(0, cut);
          yield output.slice(cut);
        },
        destroy() { state.destroys += 1; },
      };
    },
  };
}

const activeNavigator = { userActivation: { isActive: true } };

test("custom summarizer compatibility still executes every planned node", async () => {
  const { source, plan } = await fixture();
  await validateSummaryTreePlan(plan, source);
  const result = await runSummaryTree(plan, source, {}, fakeSummarizer);
  assert.equal(result.model_calls_observed, plan.node_count);
  assert.equal(result.session_strategy, "CUSTOM_NODE_SUMMARIZER");
  assert.equal(result.mechanical_root, plan.mechanical_root);
  assert.equal(result.nodes.length, plan.node_count);
  assert.equal(result.candidate_only, true);
  assert.equal(result.admitted, false);
  assert.match(result.candidate_root, /^[0-9a-f]{64}$/);
});

test("default Chrome path creates exactly one session for the entire tree", async () => {
  const { source, plan } = await fixture();
  const api = fakeApi();
  const result = await runSummaryTree(plan, source, {
    approved: true,
    secureContext: true,
    navigatorObject: activeNavigator,
    summarizerApi: api,
  });
  assert.equal(api.state.creates, 1);
  assert.equal(api.state.destroys, 1);
  assert.equal(api.state.calls, plan.node_count);
  assert.equal(result.session_strategy, "ONE_SESSION_SERIAL_TREE");
  assert.equal(result.session_creations, 1);
  assert.equal(result.model_calls_observed, plan.node_count);
  assert.equal(result.root_node_id, plan.root_node_id);
  assert.equal(result.mechanical_root, plan.mechanical_root);
  assert.equal(result.execution_authorized, false);
  assert.equal(result.merge_authorized, false);
});

test("verified complete checkpoint resumes without a model session", async () => {
  const { source, plan } = await fixture();
  const api = fakeApi();
  const first = await runSummaryTree(plan, source, {
    approved: true,
    secureContext: true,
    navigatorObject: activeNavigator,
    summarizerApi: api,
  });
  assert.equal(first.model_calls_observed, plan.node_count);
  assert.equal(first.resumed_nodes, 0);

  const resumed = await runSummaryTree(plan, source, { checkpoint: first });
  assert.equal(resumed.session_strategy, "VERIFIED_CHECKPOINT_ONLY");
  assert.equal(resumed.session_creations, 0);
  assert.equal(resumed.model_calls_observed, 0);
  assert.equal(resumed.resumed_nodes, plan.node_count);
  assert.equal(resumed.semantic_candidate_root, first.semantic_candidate_root);
  assert.notEqual(resumed.candidate_root, first.candidate_root);
  assert.equal(resumed.structured_entropy.coverage_ppm, 1_000_000);
});

test("tampered checkpoint fails closed before any model session", async () => {
  const { source, plan } = await fixture();
  const api = fakeApi();
  const first = await runSummaryTree(plan, source, {
    approved: true,
    secureContext: true,
    navigatorObject: activeNavigator,
    summarizerApi: api,
  });
  const tampered = structuredClone(first);
  tampered.nodes[0].summary += " tamper";
  await assert.rejects(
    runSummaryTree(plan, source, { checkpoint: tampered }),
    /checkpoint_candidate_invalid/,
  );
});

test("structured entropy reports model calls and bounded compression", async () => {
  const { source, plan } = await fixture();
  const result = await runSummaryTree(plan, source, {}, fakeSummarizer);
  assert.equal(result.structured_entropy.provider_calls, plan.node_count);
  assert.equal(result.structured_entropy.resumed_nodes, 0);
  assert.equal(result.structured_entropy.tree_nodes, plan.node_count);
  assert.equal(result.structured_entropy.coverage_ppm, 1_000_000);
  assert.ok(result.structured_entropy.root_compression_ppm >= 0);
  assert.match(result.semantic_candidate_root, /^[0-9a-f]{64}$/);
});

test("byte-identical leaf chunks reuse one model result mechanically", async () => {
  const source = "same\nsame\n";
  const bytes = encoder.encode(source);
  const chunk = encoder.encode("same\n");
  const leaf0 = await sha256Hex(encoder.encode("duplicate-leaf-0"));
  const leaf1 = await sha256Hex(encoder.encode("duplicate-leaf-1"));
  const root = await sha256Hex(encoder.encode("duplicate-root"));
  const sourceSha = await sha256Hex(bytes);
  const inputSha = await sha256Hex(chunk);
  const plan = {
    schema: "cpullm.chrome-summary-tree-plan.v1",
    revision: 1,
    source_ref: "fixture://duplicate",
    source_sha256: sourceSha,
    source_bytes: bytes.length,
    mechanical_root: await sha256Hex(encoder.encode("duplicate-mechanical")),
    root_node_id: root,
    leaf_count: 2,
    level_count: 2,
    node_count: 3,
    model_calls_planned: 3,
    network_calls: 0,
    candidate_only: true,
    execution_authorized: false,
    merge_authorized: false,
    config: {
      max_leaf_bytes: 1024,
      min_leaf_bytes: 1,
      fanin: 2,
      max_node_output_bytes: 512,
      max_parent_input_bytes: 4096,
      max_nodes: 100,
    },
    nodes: [
      {
        kind: "LEAF", level: 0, ordinal: 0, node_id: leaf0,
        source_sha256: sourceSha, start_byte: 0, end_byte: chunk.length,
        input_sha256: inputSha, children: [], coverage_leaf_ordinals: [0],
      },
      {
        kind: "LEAF", level: 0, ordinal: 1, node_id: leaf1,
        source_sha256: sourceSha, start_byte: chunk.length, end_byte: bytes.length,
        input_sha256: inputSha, children: [], coverage_leaf_ordinals: [1],
      },
      {
        kind: "REDUCE", level: 1, ordinal: 0, node_id: root,
        children: [leaf0, leaf1], coverage_leaf_ordinals: [0, 1],
      },
    ],
  };

  let calls = 0;
  const result = await runSummaryTree(plan, source, {}, async (request) => {
    calls += 1;
    return fakeSummarizer(request);
  });
  assert.equal(calls, 2);
  assert.equal(result.model_calls_observed, 2);
  assert.equal(result.exact_duplicate_leaf_reuses, 1);
  assert.equal(result.structured_entropy.exact_duplicate_leaf_occurrences, 1);
  assert.equal(result.structured_entropy.exact_duplicate_leaf_reuses, 1);
  assert.equal(result.nodes[1].provider, "EXACT_LEAF_REUSE");
  assert.equal(result.nodes[1].summary, result.nodes[0].summary);
});

test("source drift fails before any model call or session", async () => {
  const { source, plan } = await fixture();
  const api = fakeApi();
  await assert.rejects(
    runSummaryTree(plan, `${source}x`, {
      approved: true, secureContext: true, navigatorObject: activeNavigator, summarizerApi: api,
    }),
    /source_identity_mismatch/,
  );
  assert.equal(api.state.creates, 0);
});

test("operator approval and activation are explicit session gates", async () => {
  const { source, plan } = await fixture();
  const api = fakeApi();
  await assert.rejects(
    runSummaryTree(plan, source, { secureContext: true, navigatorObject: activeNavigator, summarizerApi: api }),
    /operator_approval_required/,
  );
  await assert.rejects(
    runSummaryTree(plan, source, {
      approved: true, secureContext: true,
      navigatorObject: { userActivation: { isActive: false } }, summarizerApi: api,
    }),
    /user_activation_required/,
  );
  assert.equal(api.state.creates, 0);
});

test("downloadable model requires explicit download approval", async () => {
  const { source, plan } = await fixture();
  const api = fakeApi({ availability: "downloadable" });
  await assert.rejects(
    runSummaryTree(plan, source, {
      approved: true, secureContext: true, navigatorObject: activeNavigator, summarizerApi: api,
    }),
    /model_download_approval_required/,
  );
  assert.equal(api.state.creates, 0);
});

test("input quota is checked for each node and fails closed", async () => {
  const { source, plan } = await fixture();
  const api = fakeApi({ quota: 3 });
  await assert.rejects(
    runSummaryTree(plan, source, {
      approved: true, secureContext: true, navigatorObject: activeNavigator, summarizerApi: api,
    }),
    /summarizer_input_quota/,
  );
  assert.equal(api.state.creates, 1);
  assert.equal(api.state.calls, 0);
  assert.equal(api.state.destroys, 1);
});

test("output budget fails closed and still destroys the one session", async () => {
  const { source, plan } = await fixture();
  plan.config.max_node_output_bytes = 256;
  const api = fakeApi({ longOutput: true });
  await assert.rejects(
    runSummaryTree(plan, source, {
      approved: true, secureContext: true, navigatorObject: activeNavigator, summarizerApi: api,
    }),
    /tree_node_output_budget/,
  );
  assert.equal(api.state.creates, 1);
  assert.equal(api.state.destroys, 1);
});

test("authority widening from a custom summarizer remains rejected", async () => {
  const { source, plan } = await fixture();
  await assert.rejects(
    runSummaryTree(plan, source, {}, async (request) => ({
      ...(await fakeSummarizer(request)),
      admitted: true,
    })),
    /summarizer_authority_violation/,
  );
});
