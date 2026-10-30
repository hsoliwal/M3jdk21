/** Thin candidate-only Chrome Summarizer adapter. */
const REQUEST_SCHEMA = "cpullm.chrome-summarizer-request.v1";
const RESULT_SCHEMA = "cpullm.chrome-summarizer-result.v2";
const RESULT_NAMESPACE = "CPULLM_CHROME_SUMMARIZER_RESULT_V2";
const STATUSES = new Set(["CANDIDATE_ONLY", "ABSTAIN"]);
const AVAILABILITY = new Set(["unavailable", "downloadable", "downloading", "available"]);
const SHA256 = /^[0-9a-f]{64}$/;
const MAX_OUTPUT_BYTES = 64 * 1024;
const DEFAULT_CONTEXT = "Summarize only source-bound evidence. Preserve negation, numbers, modality, omissions, conflicts, and locality. Never authorize execution, deletion, merging, admission, or scope widening.";
function normalize(value) {
  if (Array.isArray(value)) return value.map(normalize);
  if (value && typeof value === "object") {
    const result = {};
    for (const key of Object.keys(value).sort()) result[key] = normalize(value[key]);
    return result;
  }
  if (value === null || ["string", "number", "boolean"].includes(typeof value)) {
    if (typeof value === "number" && !Number.isSafeInteger(value)) throw new TypeError("semantic numbers must be safe integers");
    return value;
  }
  throw new TypeError(`unsupported canonical value: ${typeof value}`);
}
function canonical(value) { return JSON.stringify(normalize(value)); }
async function hash(namespace, value) {
  const bytes = new TextEncoder().encode(`${namespace}\0${canonical(value)}`);
  const digest = await globalThis.crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}
function validateRequest(request) {
  if (!request || typeof request !== "object" || Array.isArray(request)) throw new TypeError("request object required");
  if (request.schema !== REQUEST_SCHEMA || request.api !== "Summarizer") throw new TypeError("unsupported request schema or API");
  if (!SHA256.test(request.candidate_id)) throw new TypeError("candidate_id must be SHA-256");
  if (request.authority !== "CANDIDATE_ONLY") throw new TypeError("request authority escalation");
  if (request.status !== undefined && !STATUSES.has(request.status)) throw new TypeError("invalid CPU candidate status");
  if (request.model_calls !== 0 || request.network_calls !== 0) throw new TypeError("CPU request must be zero-call");
  if (typeof request.input !== "string" || new TextEncoder().encode(request.input).length > 16 * 1024) throw new TypeError("request input exceeds bounded packet size");
  if (!Array.isArray(request.source_refs) || !Array.isArray(request.omitted_refs)) throw new TypeError("source references required");
  const sourceRefs = [...new Set(request.source_refs)].sort();
  const omittedRefs = [...new Set(request.omitted_refs)].sort();
  if (sourceRefs.length !== request.source_refs.length || omittedRefs.length !== request.omitted_refs.length) throw new TypeError("source references must be unique");
  if (sourceRefs.some((ref, index) => ref !== request.source_refs[index])) throw new TypeError("source references must be sorted");
  return { sourceRefs, omittedRefs };
}
function createOptions(config, progress) {
  const options = {
    type: config.type ?? "key-points",
    format: config.format ?? "plain-text",
    length: config.length ?? "medium",
    preference: config.preference ?? "auto",
    sharedContext: config.sharedContext ?? DEFAULT_CONTEXT,
    monitor(monitor) {
      monitor.addEventListener("downloadprogress", (event) => {
        const loaded = Number.isFinite(event.loaded) ? Math.max(0, Math.round(event.loaded * 1_000_000)) : 0;
        progress.push(loaded);
        config.onDownloadProgress?.(event.loaded);
      });
    },
  };
  if (config.expectedInputLanguages !== undefined) options.expectedInputLanguages = config.expectedInputLanguages;
  if (config.expectedContextLanguages !== undefined) options.expectedContextLanguages = config.expectedContextLanguages;
  if (config.outputLanguage !== undefined) options.outputLanguage = config.outputLanguage;
  return options;
}
async function envelope({ status, candidateId, sourceRefs, text = "", availability = "unavailable", reason = "", modelCalls = 0, progress = [] }) {
  const identity = { schema: RESULT_SCHEMA, status, source_candidate_id: candidateId, source_refs: [...sourceRefs].sort(), text, availability, reason, authority: "MODEL_CANDIDATE", candidate_only: true, admitted: false, model_calls: modelCalls, direct_network_calls: 0 };
  return { ...identity, result_id: await hash(RESULT_NAMESPACE, identity), browser_managed_download_possible: true, download_progress_million: [...progress] };
}
function abortCheck(signal) {
  if (signal?.aborted) throw new DOMException("Summary cancelled", "AbortError");
}
function abortable(work, signal) {
  if (!signal) return Promise.resolve(work);
  return new Promise((resolve, reject) => {
    const cancel = () => reject(new DOMException("Summary cancelled", "AbortError"));
    signal.addEventListener("abort", cancel, { once: true });
    Promise.resolve(work).then(value => {
      signal.removeEventListener("abort", cancel); resolve(value);
    }, error => { signal.removeEventListener("abort", cancel); reject(error); });
    if (signal.aborted) cancel();
  });
}
function failure(code) { const error = new Error(code); error.code = code; return error; }

/** One explicitly user-created session; the batch owner must destroy it. */
export async function openChromeSummarizerSession({
  summarizerApi = globalThis.Summarizer, navigatorObject = globalThis.navigator,
  config = {}, signal,
} = {}) {
  abortCheck(signal);
  if (!summarizerApi?.availability || !summarizerApi?.create) throw failure("SUMMARIZER_API_UNAVAILABLE");
  const progress = [], options = createOptions(config, progress);
  let availability;
  try { availability = await abortable(summarizerApi.availability(options), signal); }
  catch { throw failure("AVAILABILITY_ERROR"); }
  if (!AVAILABILITY.has(availability)) throw failure("UNKNOWN_AVAILABILITY");
  if (availability === "unavailable") throw failure("MODEL_UNAVAILABLE");
  if (config.allowDownload === false && availability !== "available") throw failure("DOWNLOAD_APPROVAL_REQUIRED");
  abortCheck(signal);
  if (!navigatorObject?.userActivation?.isActive) throw failure("USER_ACTIVATION_REQUIRED");
  const creation = Promise.resolve(summarizerApi.create({ ...options, ...(signal ? { signal } : {}) }));
  let session;
  try { session = await abortable(creation, signal); }
  catch (error) {
    // A provider that finishes after cancellation must not leak a live session.
    creation.then(late => late?.destroy?.(), () => {});
    throw error;
  }
  let closed = false;
  const handle = {
    session, availability, progress,
    options: Object.freeze(Object.fromEntries(Object.entries(options).filter(([key]) => key !== "monitor"))),
    get closed() { return closed; },
    destroy() {
      if (closed) return;
      closed = true;
      signal?.removeEventListener("abort", cancel);
      session.destroy?.();
    },
  };
  const cancel = () => handle.destroy();
  signal?.addEventListener("abort", cancel, { once: true });
  if (signal?.aborted) { handle.destroy(); abortCheck(signal); }
  return handle;
}

async function streamingText(session, input, context, signal, maxOutputBytes) {
  if (typeof session.summarizeStreaming !== "function") throw new TypeError("summarizeStreaming is unavailable");
  let text = "", bytes = 0;
  for await (const chunk of session.summarizeStreaming(input, { context, signal })) {
    abortCheck(signal);
    const piece = String(chunk);
    bytes += new TextEncoder().encode(piece).length;
    if (bytes > maxOutputBytes) throw failure("MODEL_OUTPUT_BUDGET");
    text += piece;
  }
  return text;
}

export async function runChromeSummarizer(request, {
  summarizerApi = globalThis.Summarizer, navigatorObject = globalThis.navigator,
  config = {}, streaming = false, maxOutputBytes = MAX_OUTPUT_BYTES,
  sessionHandle = null, signal,
} = {}) {
  const { sourceRefs } = validateRequest(request);
  if (request.status === "ABSTAIN" || sourceRefs.length === 0) return envelope({ status: "ABSTAIN", candidateId: request.candidate_id, sourceRefs, reason: "CPU_CANDIDATE_ABSTAIN" });
  if (!Number.isSafeInteger(maxOutputBytes) || maxOutputBytes < 1 || maxOutputBytes > MAX_OUTPUT_BYTES) throw new TypeError("invalid output budget");
  let handle = sessionHandle, modelCalls = 0;
  const base = () => ({ candidateId: request.candidate_id, sourceRefs,
    availability: handle?.availability ?? "unavailable", progress: handle?.progress ?? [], modelCalls });
  try {
    abortCheck(signal);
    if (!handle) handle = await openChromeSummarizerSession({ summarizerApi, navigatorObject, config, signal });
    if (handle.closed || !handle.session) throw failure("SESSION_CLOSED");
    const session = handle.session;
    const context = config.context ?? "Preserve source identities, conflicts, omissions, and authority boundaries.";
    if (typeof session.measureInputUsage === "function" && Number.isFinite(session.inputQuota)) {
      const usage = await abortable(session.measureInputUsage(request.input, { context, signal }), signal);
      if (!Number.isFinite(usage) || usage < 0) throw failure("INVALID_INPUT_USAGE");
      if (usage > session.inputQuota) throw failure("INPUT_QUOTA_EXCEEDED");
    }
    abortCheck(signal);
    modelCalls = 1;
    const text = String(await abortable(streaming
      ? streamingText(session, request.input, context, signal, maxOutputBytes)
      : session.summarize(request.input, { context, signal }), signal));
    abortCheck(signal);
    if (!text.trim()) throw failure("EMPTY_MODEL_OUTPUT");
    if (new TextEncoder().encode(text).length > maxOutputBytes) throw failure("MODEL_OUTPUT_BUDGET");
    return envelope({ ...base(), status: "CANDIDATE_ONLY", text });
  } catch (error) {
    const reason = signal?.aborted || error?.name === "AbortError" ? "ABORTED"
      : error?.name === "QuotaExceededError" ? "INPUT_QUOTA_EXCEEDED"
      : error?.code ?? `SUMMARIZER_ERROR:${error?.name ?? "Error"}`;
    if (reason === "ABORTED") handle?.destroy();
    return envelope({ ...base(), status: "ABSTAIN", reason });
  } finally {
    // A batch-owned session stays live until its owner pauses, completes or fails.
    if (!sessionHandle) handle?.destroy();
  }
}
export const ChromeSummarizerTool = Object.freeze({ requestSchema: REQUEST_SCHEMA,
  resultSchema: RESULT_SCHEMA, run: runChromeSummarizer, open: openChromeSummarizerSession });
