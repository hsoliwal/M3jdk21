/* Hierarchical Chrome built-in Summarizer executor.
 *
 * Consumes the deterministic Python tree plan. The default runtime creates one
 * operator-approved Summarizer session and reuses it serially for every planned
 * leaf and reduce node. A caller-supplied summarizeNode hook remains supported
 * for deterministic tests/adapters. Model text never changes the mechanical root.
 */
const encoder = new TextEncoder();
const decoder = new TextDecoder("utf-8", { fatal: true });
const sha256re = /^[0-9a-f]{64}$/;
const availabilityStates = new Set(["available", "downloadable", "downloading"]);
const error = (code) => Object.assign(new Error(code), { code });
const MAX_TIMEOUT_MS = 30 * 60 * 1000;

async function sha256Hex(bytes) {
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)]
    .map((value) => value.toString(16).padStart(2, "0"))
    .join("");
}

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === "object") {
    const out = {};
    for (const key of Object.keys(value).sort()) out[key] = canonical(value[key]);
    return out;
  }
  return value;
}

async function candidateRoot(value) {
  return sha256Hex(encoder.encode(JSON.stringify(canonical(value))));
}

function asInteger(value) {
  return Number.isSafeInteger(value) ? value : null;
}

export async function validateSummaryTreePlan(plan, sourceText) {
  if (!plan || plan.schema !== "cpullm.chrome-summary-tree-plan.v1" || plan.revision !== 1) {
    throw error("invalid_tree_plan_schema");
  }
  if (
    plan.candidate_only !== true ||
    plan.execution_authorized !== false ||
    plan.merge_authorized !== false ||
    plan.network_calls !== 0
  ) {
    throw error("invalid_tree_authority");
  }
  if (
    !sha256re.test(plan.source_sha256) ||
    !sha256re.test(plan.mechanical_root) ||
    !sha256re.test(plan.root_node_id)
  ) {
    throw error("invalid_tree_hash");
  }
  if (typeof sourceText !== "string" || !sourceText.length) throw error("empty_source");
  const bytes = encoder.encode(sourceText);
  if (bytes.length !== plan.source_bytes || (await sha256Hex(bytes)) !== plan.source_sha256) {
    throw error("source_identity_mismatch");
  }
  if (
    !Array.isArray(plan.nodes) ||
    plan.nodes.length !== plan.node_count ||
    plan.nodes.length !== plan.model_calls_planned
  ) {
    throw error("node_count_mismatch");
  }

  const nodes = new Map();
  const leaves = [];
  for (const node of plan.nodes) {
    if (!node || !sha256re.test(node.node_id) || nodes.has(node.node_id)) {
      throw error("invalid_node_identity");
    }
    nodes.set(node.node_id, node);
    if (node.kind === "LEAF") leaves.push(node);
  }
  leaves.sort((left, right) => left.ordinal - right.ordinal);
  if (leaves.length !== plan.leaf_count) throw error("leaf_count_mismatch");

  let cursor = 0;
  for (let ordinal = 0; ordinal < leaves.length; ordinal += 1) {
    const leaf = leaves[ordinal];
    if (leaf.ordinal !== ordinal || leaf.level !== 0 || leaf.start_byte !== cursor) {
      throw error("leaf_coverage_order");
    }
    const end = asInteger(leaf.end_byte);
    if (end === null || end <= cursor || end > bytes.length) throw error("leaf_range");
    const slice = bytes.slice(cursor, end);
    decoder.decode(slice);
    if ((await sha256Hex(slice)) !== leaf.input_sha256) throw error("leaf_hash_mismatch");
    if (leaf.source_sha256 !== plan.source_sha256) throw error("leaf_source_mismatch");
    if (JSON.stringify(leaf.coverage_leaf_ordinals) !== JSON.stringify([ordinal])) {
      throw error("leaf_coverage_identity");
    }
    cursor = end;
  }
  if (cursor !== bytes.length) throw error("source_not_fully_covered");

  for (const node of plan.nodes) {
    if (node.kind === "LEAF") continue;
    if (node.kind !== "REDUCE" || !Array.isArray(node.children) || !node.children.length) {
      throw error("invalid_reduce_node");
    }
    const coverage = [];
    for (const childId of node.children) {
      const child = nodes.get(childId);
      if (!child || child.level >= node.level) throw error("invalid_reduce_edge");
      coverage.push(...child.coverage_leaf_ordinals);
    }
    if (JSON.stringify(coverage) !== JSON.stringify(node.coverage_leaf_ordinals)) {
      throw error("reduce_coverage_mismatch");
    }
  }

  const root = nodes.get(plan.root_node_id);
  if (
    !root ||
    JSON.stringify(root.coverage_leaf_ordinals) !== JSON.stringify([...leaves.keys()])
  ) {
    throw error("root_coverage_mismatch");
  }
  return { bytes, nodes, leaves };
}

function leafInput(bytes, node) {
  return decoder.decode(bytes.slice(node.start_byte, node.end_byte));
}

function reduceInput(node, results) {
  const sections = [];
  for (const childId of node.children) {
    const child = results.get(childId);
    if (!child) throw error("missing_child_candidate");
    sections.push(
      `[[CHILD node=${childId} result=${child.result_id}]]\n${child.summary}\n[[END CHILD]]`,
    );
  }
  return sections.join("\n\n");
}

async function checkpointMap(plan, checkpoint) {
  if (checkpoint == null) return new Map();
  if (
    checkpoint.mechanical_root !== plan.mechanical_root ||
    checkpoint.source_sha256 !== plan.source_sha256 ||
    !Array.isArray(checkpoint.nodes)
  ) {
    throw error("checkpoint_plan_binding");
  }
  const byId = new Map();
  const planIds = new Set(plan.nodes.map((node) => node.node_id));
  for (const receipt of checkpoint.nodes) {
    if (!receipt || !planIds.has(receipt.node_id) || byId.has(receipt.node_id)) {
      throw error("checkpoint_node_identity");
    }
    byId.set(receipt.node_id, receipt);
  }
  return byId;
}

async function verifyCheckpointReceipt(node, receipt, inputBytes, results) {
  if (
    receipt.node_id !== node.node_id ||
    receipt.kind !== node.kind ||
    receipt.level !== node.level ||
    receipt.ordinal !== node.ordinal ||
    JSON.stringify(receipt.children) !== JSON.stringify(node.children) ||
    JSON.stringify(receipt.coverage_leaf_ordinals) !== JSON.stringify(node.coverage_leaf_ordinals) ||
    receipt.candidate_only !== true ||
    receipt.admitted !== false ||
    receipt.execution_authorized !== false ||
    receipt.merge_authorized !== false ||
    typeof receipt.summary !== "string" ||
    !receipt.summary.trim() ||
    !sha256re.test(receipt.output_sha256) ||
    !sha256re.test(receipt.result_id)
  ) {
    return false;
  }
  const inputSha = await sha256Hex(inputBytes);
  if (receipt.input_sha256 !== inputSha) return false;
  const outputBytes = encoder.encode(receipt.summary);
  if ((await sha256Hex(outputBytes)) !== receipt.output_sha256) return false;
  const childResultIds = node.children.map((childId) => results.get(childId)?.result_id);
  if (JSON.stringify(receipt.child_result_ids) !== JSON.stringify(childResultIds)) return false;
  const identity = {
    node_id: node.node_id,
    kind: node.kind,
    level: node.level,
    ordinal: node.ordinal,
    children: [...node.children],
    coverage_leaf_ordinals: [...node.coverage_leaf_ordinals],
    input_sha256: inputSha,
    output_sha256: receipt.output_sha256,
    child_result_ids: childResultIds,
  };
  return receipt.result_id === await candidateRoot(identity);
}

function checkpointCoversPlan(plan, checkpoint) {
  if (
    !checkpoint ||
    checkpoint.mechanical_root !== plan.mechanical_root ||
    checkpoint.source_sha256 !== plan.source_sha256 ||
    !Array.isArray(checkpoint.nodes) ||
    checkpoint.nodes.length !== plan.nodes.length
  ) {
    return false;
  }
  const ids = new Set(checkpoint.nodes.map((row) => row?.node_id));
  return ids.size === plan.nodes.length && plan.nodes.every((node) => ids.has(node.node_id));
}

function structuredEntropy(plan, receipts, providerCalls, resumedNodes, deduplicatedNodes) {
  const leafRows = plan.nodes.filter((node) => node.kind === "LEAF");
  const uniqueLeafInputs = new Set(leafRows.map((node) => node.input_sha256)).size;
  const outputs = receipts.map((row) => row.output_sha256);
  const uniqueOutputs = new Set(outputs).size;
  const root = receipts.find((row) => row.node_id === plan.root_node_id);
  const rootBytes = root ? encoder.encode(root.summary).length : 0;
  return {
    source_utf8_bytes: plan.source_bytes,
    leaf_nodes: leafRows.length,
    exact_duplicate_leaf_occurrences: Math.max(0, leafRows.length - uniqueLeafInputs),
    tree_nodes: plan.node_count,
    tree_depth: plan.level_count - 1,
    provider_calls: providerCalls,
    resumed_nodes: resumedNodes,
    exact_duplicate_leaf_reuses: deduplicatedNodes,
    coverage_ppm: receipts.length === plan.node_count ? 1_000_000 : Math.floor(receipts.length * 1_000_000 / plan.node_count),
    root_compression_ppm: plan.source_bytes === 0 ? 0 : Math.floor(rootBytes * 1_000_000 / plan.source_bytes),
    repeated_output_ppm: receipts.length === 0 ? 0 : Math.floor((receipts.length - uniqueOutputs) * 1_000_000 / receipts.length),
  };
}

function modelConfig(options, progress) {
  return {
    type: options.type ?? "key-points",
    format: options.format ?? "plain-text",
    length: options.length ?? "medium",
    preference: options.preference ?? "auto",
    sharedContext:
      options.sharedContext ||
      "Compress only mechanically bound tree nodes. Preserve conflicts, uncertainty, numbers, modality, omissions and locality. Never authorize execution, deletion, merge, admission or scope widening.",
    ...(options.expectedInputLanguages ? { expectedInputLanguages: options.expectedInputLanguages } : {}),
    ...(options.expectedContextLanguages ? { expectedContextLanguages: options.expectedContextLanguages } : {}),
    ...(options.outputLanguage ? { outputLanguage: options.outputLanguage } : {}),
    monitor(monitor) {
      monitor.addEventListener("downloadprogress", (event) => {
        const loaded = Number.isFinite(event.loaded) ? Math.max(0, Math.min(1, event.loaded)) : 0;
        const million = Math.round(loaded * 1_000_000);
        progress.push(million);
        options.onTreeStatus?.({ state: "MODEL_DOWNLOAD", loaded_million: million });
      });
    },
  };
}

async function withOwnedSession(options, operation) {
  if (options.approved !== true) throw error("operator_approval_required");
  const secureContext = options.secureContext ?? globalThis.isSecureContext;
  const navigatorObject = options.navigatorObject ?? globalThis.navigator;
  if (secureContext !== true) throw error("secure_context_required");
  if (navigatorObject?.userActivation?.isActive !== true) throw error("user_activation_required");

  const api = options.summarizerApi ?? globalThis.Summarizer;
  if (!api?.availability || !api?.create) throw error("summarizer_api_unavailable");
  const progress = [];
  const config = modelConfig(options, progress);
  const { monitor: _monitor, ...availabilityConfig } = config;
  const availability = await api.availability(availabilityConfig);
  if (!availabilityStates.has(availability)) throw error("summarizer_unavailable");
  if (availability !== "available" && options.allowDownload !== true) {
    throw error("model_download_approval_required");
  }
  if (navigatorObject?.userActivation?.isActive !== true) throw error("user_activation_expired");

  const timeoutMs = options.timeoutMs ?? 10 * 60 * 1000;
  if (!Number.isSafeInteger(timeoutMs) || timeoutMs < 1 || timeoutMs > MAX_TIMEOUT_MS) {
    throw error("invalid_timeout");
  }
  const controller = new AbortController();
  const external = options.signal;
  const abort = () => controller.abort(external?.reason || error("cancelled"));
  if (external?.aborted) abort();
  else external?.addEventListener("abort", abort, { once: true });
  const timer = setTimeout(() => controller.abort(error("deadline_exceeded")), timeoutMs);
  const aborted = new Promise((_, reject) => {
    if (controller.signal.aborted) reject(controller.signal.reason);
    else controller.signal.addEventListener("abort", () => reject(controller.signal.reason), { once: true });
  });
  aborted.catch(() => {});
  const wait = async (value) => {
    if (controller.signal.aborted) throw controller.signal.reason;
    return Promise.race([Promise.resolve(value), aborted]);
  };

  let session;
  let closed = false;
  try {
    const creation = Promise.resolve(api.create({ ...config, signal: controller.signal })).then((created) => {
      if (closed || controller.signal.aborted) {
        created?.destroy?.();
        throw controller.signal.reason || error("session_arrived_after_close");
      }
      session = created;
      return created;
    });
    await wait(creation);
    if (!session?.destroy) throw error("invalid_summarizer_session");
    options.onTreeStatus?.({ state: "SESSION_READY", availability });
    return await operation(session, controller.signal, wait, availability, progress);
  } finally {
    closed = true;
    clearTimeout(timer);
    external?.removeEventListener("abort", abort);
    try {
      session?.destroy?.();
    } catch {
      throw error("session_cleanup_failed");
    }
  }
}

async function sessionCandidate(request, nodeOptions, session, signal, wait) {
  const inputBytes = encoder.encode(request.input).length;
  if (inputBytes > nodeOptions.maxInputBytes) throw error("tree_node_input_budget");
  if (
    typeof session.measureInputUsage === "function" &&
    Number.isFinite(session.inputQuota) &&
    session.inputQuota > 0
  ) {
    const usage = await wait(session.measureInputUsage(request.input));
    if (!Number.isFinite(usage) || usage < 0) throw error("invalid_input_usage");
    if (usage > session.inputQuota) throw error("summarizer_input_quota");
  }

  let summary = "";
  const context = nodeOptions.context;
  if (typeof session.summarizeStreaming === "function") {
    const stream = session.summarizeStreaming(request.input, { context, signal });
    if (!stream?.[Symbol.asyncIterator]) throw error("invalid_summary_stream");
    const iterator = stream[Symbol.asyncIterator]();
    for (;;) {
      const step = await wait(iterator.next());
      if (step.done) break;
      summary += String(step.value ?? "");
      if (encoder.encode(summary).length > nodeOptions.maxOutputBytes) {
        throw error("tree_node_output_budget");
      }
    }
  } else if (typeof session.summarize === "function") {
    summary = String(await wait(session.summarize(request.input, { context, signal })));
    if (encoder.encode(summary).length > nodeOptions.maxOutputBytes) {
      throw error("tree_node_output_budget");
    }
  } else {
    throw error("summarizer_method_unavailable");
  }
  if (!summary.trim()) throw error("empty_summary");
  return {
    provider: "Summarizer",
    candidate_id: request.candidate_id,
    summary,
    candidate_only: true,
    admitted: false,
    execution_authorized: false,
    merge_authorized: false,
  };
}

async function executeNodes(plan, sourceBytes, options, summarizeNode, sessionStrategy) {
  const maxNodeOutputBytes = plan.config?.max_node_output_bytes;
  const maxParentInputBytes = plan.config?.max_parent_input_bytes;
  const maxLeafInputBytes = plan.config?.max_leaf_bytes;
  if (
    !Number.isInteger(maxNodeOutputBytes) ||
    !Number.isInteger(maxParentInputBytes) ||
    !Number.isInteger(maxLeafInputBytes)
  ) {
    throw error("invalid_tree_config");
  }
  if ((options.maxOutputBytes ?? maxNodeOutputBytes) > maxNodeOutputBytes) {
    throw error("tree_plan_budget_widening");
  }

  const checkpoint = await checkpointMap(plan, options.checkpoint);
  const results = new Map();
  const nodeReceipts = [];
  const leafReceiptByInputSha = new Map();
  let providerCalls = 0;
  let resumedNodes = 0;
  let deduplicatedNodes = 0;

  for (let index = 0; index < plan.nodes.length; index += 1) {
    const node = plan.nodes[index];
    const input = node.kind === "LEAF" ? leafInput(sourceBytes, node) : reduceInput(node, results);
    const inputBytes = encoder.encode(input);
    const inputLimit = node.kind === "LEAF" ? maxLeafInputBytes : maxParentInputBytes;
    if (inputBytes.length > inputLimit) throw error("tree_node_input_budget");

    const retained = checkpoint.get(node.node_id);
    if (retained) {
      if (!(await verifyCheckpointReceipt(node, retained, inputBytes, results))) {
        throw error("checkpoint_candidate_invalid");
      }
      if (encoder.encode(retained.summary).length > maxNodeOutputBytes) {
        throw error("checkpoint_output_budget");
      }
      results.set(node.node_id, retained);
      nodeReceipts.push(retained);
      if (node.kind === "LEAF") {
        leafReceiptByInputSha.set(retained.input_sha256, retained);
      }
      resumedNodes += 1;
      options.onTreeStatus?.({
        state: "NODE_RESUME",
        node_id: node.node_id,
        level: node.level,
        ordinal: node.ordinal,
        index,
        total: plan.nodes.length,
      });
      continue;
    }

    if (node.kind === "LEAF") {
      const inputSha = await sha256Hex(inputBytes);
      const prior = leafReceiptByInputSha.get(inputSha);
      if (prior) {
        const outputBytes = encoder.encode(prior.summary);
        const receiptIdentity = {
          node_id: node.node_id,
          kind: node.kind,
          level: node.level,
          ordinal: node.ordinal,
          children: [...node.children],
          coverage_leaf_ordinals: [...node.coverage_leaf_ordinals],
          input_sha256: inputSha,
          output_sha256: await sha256Hex(outputBytes),
          child_result_ids: [],
        };
        const receipt = {
          ...receiptIdentity,
          result_id: await candidateRoot(receiptIdentity),
          summary: prior.summary,
          provider: "EXACT_LEAF_REUSE",
          reused_from_node_id: prior.node_id,
          candidate_only: true,
          admitted: false,
          execution_authorized: false,
          merge_authorized: false,
        };
        results.set(node.node_id, receipt);
        nodeReceipts.push(receipt);
        deduplicatedNodes += 1;
        options.onTreeStatus?.({
          state: "NODE_DEDUP",
          node_id: node.node_id,
          reused_from_node_id: prior.node_id,
          level: node.level,
          ordinal: node.ordinal,
          index,
          total: plan.nodes.length,
        });
        continue;
      }
    }

    const request = {
      schema: "cpullm.chrome-summarizer-request.v1",
      api: "Summarizer",
      authority: "CANDIDATE_ONLY",
      model_calls: 0,
      network_calls: 0,
      candidate_id: node.node_id,
      input,
      source_refs: [plan.source_ref],
      omitted_refs: [],
    };
    options.onTreeStatus?.({
      state: "NODE_START",
      node_id: node.node_id,
      level: node.level,
      ordinal: node.ordinal,
      index,
      total: plan.nodes.length,
    });
    const candidate = await summarizeNode(request, {
      maxInputBytes: inputLimit,
      maxOutputBytes: Math.min(options.maxOutputBytes ?? maxNodeOutputBytes, maxNodeOutputBytes),
      context:
        options.context ||
        (node.kind === "LEAF"
          ? "Summarize only this exact source leaf. Preserve facts, uncertainty, numbers, negation and conflicts."
          : "Consolidate these ordered child summaries without majority vote, conflict erasure or authority inference."),
    });
    providerCalls += 1;
    if (
      !candidate ||
      candidate.candidate_only !== true ||
      candidate.admitted !== false ||
      candidate.execution_authorized !== false ||
      candidate.merge_authorized !== false
    ) {
      throw error("summarizer_authority_violation");
    }
    if (
      candidate.candidate_id !== node.node_id ||
      typeof candidate.summary !== "string" ||
      !candidate.summary.trim()
    ) {
      throw error("summarizer_binding_failure");
    }
    const outputBytes = encoder.encode(candidate.summary);
    if (outputBytes.length > maxNodeOutputBytes) throw error("tree_node_output_budget");

    const receiptIdentity = {
      node_id: node.node_id,
      kind: node.kind,
      level: node.level,
      ordinal: node.ordinal,
      children: [...node.children],
      coverage_leaf_ordinals: [...node.coverage_leaf_ordinals],
      input_sha256: await sha256Hex(inputBytes),
      output_sha256: await sha256Hex(outputBytes),
      child_result_ids: node.children.map((childId) => results.get(childId)?.result_id),
    };
    const receipt = {
      ...receiptIdentity,
      result_id: await candidateRoot(receiptIdentity),
      summary: candidate.summary,
      provider: candidate.provider || "Summarizer",
      candidate_only: true,
      admitted: false,
      execution_authorized: false,
      merge_authorized: false,
    };
    results.set(node.node_id, receipt);
    nodeReceipts.push(receipt);
    if (node.kind === "LEAF") {
      leafReceiptByInputSha.set(receipt.input_sha256, receipt);
    }
    options.onTreeStatus?.({
      state: "NODE_DONE",
      node_id: node.node_id,
      level: node.level,
      ordinal: node.ordinal,
      index,
      total: plan.nodes.length,
    });
  }

  const root = results.get(plan.root_node_id);
  if (!root) throw error("missing_root_candidate");
  const semanticIdentity = {
    schema: "cpullm.chrome-summary-tree-semantic.v1",
    source_sha256: plan.source_sha256,
    mechanical_root: plan.mechanical_root,
    root_node_id: plan.root_node_id,
    root_result_id: root.result_id,
    node_result_ids: nodeReceipts.map((receipt) => receipt.result_id),
  };
  const body = {
    schema: "cpullm.chrome-summary-tree-candidate.v1",
    revision: 1,
    source_ref: plan.source_ref,
    source_sha256: plan.source_sha256,
    mechanical_root: plan.mechanical_root,
    root_node_id: plan.root_node_id,
    root_result_id: root.result_id,
    root_summary: root.summary,
    nodes: nodeReceipts,
    model_calls_observed: providerCalls,
    resumed_nodes: resumedNodes,
    exact_duplicate_leaf_reuses: deduplicatedNodes,
    node_count_completed: nodeReceipts.length,
    session_strategy: sessionStrategy,
    provider_fallback: false,
    candidate_only: true,
    admitted: false,
    execution_authorized: false,
    merge_authorized: false,
    deletion_authorized: false,
    locality_widening_authorized: false,
    structured_entropy: structuredEntropy(
      plan, nodeReceipts, providerCalls, resumedNodes, deduplicatedNodes
    ),
    semantic_candidate_root: await candidateRoot(semanticIdentity),
    note: "Hierarchical compression candidate only; exact source/tree and mechanical multipass remain authoritative.",
  };
  body.candidate_root = await candidateRoot(body);
  return body;
}

export async function runSummaryTree(
  plan,
  sourceText,
  options = {},
  summarizeNode = undefined,
) {
  const { bytes } = await validateSummaryTreePlan(plan, sourceText);
  if (options.checkpoint != null) {
    await checkpointMap(plan, options.checkpoint);
  }
  if (summarizeNode !== undefined) {
    if (typeof summarizeNode !== "function") throw error("summarizer_function_required");
    return executeNodes(plan, bytes, options, summarizeNode, "CUSTOM_NODE_SUMMARIZER");
  }

  if (checkpointCoversPlan(plan, options.checkpoint)) {
    const result = await executeNodes(
      plan,
      bytes,
      options,
      async () => {
        throw error("checkpoint_incomplete_without_session");
      },
      "VERIFIED_CHECKPOINT_ONLY",
    );
    result.session_creations = 0;
    result.availability = "not-needed";
    result.download_progress_million = [];
    return result;
  }

  return withOwnedSession(options, async (session, signal, wait, availability, progress) => {
    const result = await executeNodes(
      plan,
      bytes,
      options,
      (request, nodeOptions) => sessionCandidate(request, nodeOptions, session, signal, wait),
      "ONE_SESSION_SERIAL_TREE",
    );
    result.session_creations = 1;
    result.availability = availability;
    result.download_progress_million = progress;
    return result;
  });
}
