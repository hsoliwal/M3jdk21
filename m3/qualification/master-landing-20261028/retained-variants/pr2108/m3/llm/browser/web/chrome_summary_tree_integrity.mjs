/** Deterministic browser replay of cpullm.chrome-summary-tree-plan.v1. */
const encoder = new TextEncoder();
const decoder = new TextDecoder("utf-8", { fatal: true });
const DOMAIN = "CPULLM_CHROME_SUMMARY_TREE_V1\0";
const NODE_DOMAIN = "CPULLM_CHROME_SUMMARY_TREE_NODE_V1\0";
const LAWS = Object.freeze([
  "SOURCE_BYTES_ARE_NEVER_REPLACED_BY_SUMMARIES",
  "TREE_SHAPE_IS_DETERMINISTIC",
  "MODEL_OUTPUT_IS_CANDIDATE_ONLY",
  "CHUNK_COVERAGE_IS_EXACT_AND_NON_OVERLAPPING",
  "PARENT_SUMMARY_CANNOT_WIDEN_CHILD_SOURCE_COVERAGE",
  "SUMMARY_TREE_IS_NOT_SOURCE_AUTHORITY",
]);

const fail = (code) => Object.assign(new Error(code), { code });

function normalize(value) {
  if (Array.isArray(value)) return value.map(normalize);
  if (value && typeof value === "object") {
    const out = {};
    for (const key of Object.keys(value).sort()) out[key] = normalize(value[key]);
    return out;
  }
  if (value === null || ["string", "boolean"].includes(typeof value)) return value;
  if (typeof value === "number" && Number.isSafeInteger(value)) return value;
  throw fail("noncanonical_plan_value");
}

function canonical(value) {
  return JSON.stringify(normalize(value));
}

async function sha256Hex(bytes) {
  const digest = await globalThis.crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)]
    .map((value) => value.toString(16).padStart(2, "0"))
    .join("");
}

async function root(domain, value) {
  return sha256Hex(encoder.encode(`${domain}${canonical(value)}`));
}

function integer(name, value, low, high) {
  if (!Number.isSafeInteger(value) || value < low || value > high) throw fail(`invalid_${name}`);
  return value;
}

function readConfig(plan) {
  const raw = plan?.config;
  if (!raw || typeof raw !== "object" || Array.isArray(raw)) throw fail("invalid_tree_config");
  const expected = ["fanin", "max_leaf_bytes", "max_node_output_bytes", "max_nodes", "max_parent_input_bytes", "min_leaf_bytes"].sort();
  if (JSON.stringify(Object.keys(raw).sort()) !== JSON.stringify(expected)) throw fail("invalid_tree_config_fields");
  const cfg = {
    max_leaf_bytes: integer("max_leaf_bytes", raw.max_leaf_bytes, 1024, 60 * 1024),
    min_leaf_bytes: integer("min_leaf_bytes", raw.min_leaf_bytes, 1, raw.max_leaf_bytes),
    fanin: integer("fanin", raw.fanin, 2, 32),
    max_node_output_bytes: integer("max_node_output_bytes", raw.max_node_output_bytes, 256, 32 * 1024),
    max_parent_input_bytes: integer("max_parent_input_bytes", raw.max_parent_input_bytes, 1024, 60 * 1024),
    max_nodes: integer("max_nodes", raw.max_nodes, 1, 1_000_000),
  };
  if (cfg.fanin * (cfg.max_node_output_bytes + 256) > cfg.max_parent_input_bytes) throw fail("tree_parent_budget_overflow");
  return cfg;
}

function safeEnd(data, start, desired, minimum) {
  if (desired >= data.length) return data.length;
  const floor = Math.min(desired, start + minimum);
  for (let at = desired - 1; at >= floor; at -= 1) {
    if (data[at] === 10) return at + 1;
  }
  let end = desired;
  while (end > start && end < data.length && (data[end] & 0xc0) === 0x80) end -= 1;
  if (end <= start) throw fail("unable_to_create_utf8_safe_chunk");
  decoder.decode(data.slice(start, end));
  return end;
}

async function chunksFor(sourceText, cfg) {
  if (typeof sourceText !== "string" || !sourceText.length) throw fail("empty_source");
  const data = encoder.encode(sourceText);
  const chunks = [];
  let start = 0;
  let ordinal = 0;
  while (start < data.length) {
    const desired = Math.min(start + cfg.max_leaf_bytes, data.length);
    const end = safeEnd(data, start, desired, cfg.min_leaf_bytes);
    const raw = data.slice(start, end);
    if (!raw.length || raw.length > cfg.max_leaf_bytes) throw fail("leaf_byte_budget_violation");
    chunks.push({
      ordinal,
      start_byte: start,
      end_byte: end,
      byte_length: raw.length,
      sha256: await sha256Hex(raw),
    });
    start = end;
    ordinal += 1;
  }
  return { data, chunks };
}

async function leafNode(sourceSha256, chunk) {
  const core = {
    kind: "LEAF",
    level: 0,
    ordinal: chunk.ordinal,
    source_sha256: sourceSha256,
    start_byte: chunk.start_byte,
    end_byte: chunk.end_byte,
    input_sha256: chunk.sha256,
    children: [],
    coverage_leaf_ordinals: [chunk.ordinal],
  };
  return { ...core, node_id: await root(NODE_DOMAIN, core) };
}

async function parentNode(level, ordinal, children) {
  const coverage = children.flatMap((child) => child.coverage_leaf_ordinals);
  const sorted = [...coverage].sort((a, b) => a - b);
  if (JSON.stringify(coverage) !== JSON.stringify(sorted) || new Set(coverage).size !== coverage.length) {
    throw fail("child_coverage_overlap_or_order");
  }
  const core = {
    kind: "REDUCE",
    level,
    ordinal,
    children: children.map((child) => child.node_id),
    coverage_leaf_ordinals: coverage,
  };
  return { ...core, node_id: await root(NODE_DOMAIN, core) };
}

export async function replaySummaryTreePlan(sourceText, {
  sourceRef = "source",
  config,
} = {}) {
  if (typeof sourceRef !== "string" || !sourceRef) throw fail("source_ref_required");
  const cfg = config ? readConfig({ config }) : {
    max_leaf_bytes: 48 * 1024,
    min_leaf_bytes: 8 * 1024,
    fanin: 4,
    max_node_output_bytes: 8 * 1024,
    max_parent_input_bytes: 48 * 1024,
    max_nodes: 100_000,
  };
  const { data, chunks } = await chunksFor(sourceText, cfg);
  const sourceSha256 = await sha256Hex(data);
  const first = [];
  for (const chunk of chunks) first.push(await leafNode(sourceSha256, chunk));
  const levels = [first];
  let total = first.length;
  let level = 1;
  while (levels.at(-1).length > 1) {
    const previous = levels.at(-1);
    const current = [];
    for (let start = 0, ordinal = 0; start < previous.length; start += cfg.fanin, ordinal += 1) {
      current.push(await parentNode(level, ordinal, previous.slice(start, start + cfg.fanin)));
    }
    levels.push(current);
    total += current.length;
    if (total > cfg.max_nodes) throw fail("tree_exceeds_max_nodes");
    level += 1;
  }
  const nodes = levels.flat();
  const body = {
    schema: "cpullm.chrome-summary-tree-plan.v1",
    revision: 1,
    source_ref: sourceRef,
    source_sha256: sourceSha256,
    source_bytes: data.length,
    config: { ...cfg },
    leaf_count: chunks.length,
    level_count: levels.length,
    node_count: nodes.length,
    root_node_id: nodes.at(-1).node_id,
    nodes,
    model_calls_planned: nodes.length,
    network_calls: 0,
    candidate_only: true,
    execution_authorized: false,
    merge_authorized: false,
    laws: [...LAWS],
  };
  return { ...body, mechanical_root: await root(DOMAIN, body) };
}

export async function verifySummaryTreeIntegrity(plan, sourceText) {
  if (!plan || typeof plan !== "object" || Array.isArray(plan)) throw fail("plan_object_required");
  const cfg = readConfig(plan);
  const replay = await replaySummaryTreePlan(sourceText, { sourceRef: plan.source_ref, config: cfg });
  if (canonical(replay) !== canonical(plan)) throw fail("mechanical_tree_replay_mismatch");
  return true;
}

export const SummaryTreeIntegrity = Object.freeze({ replay: replaySummaryTreePlan, verify: verifySummaryTreeIntegrity });
