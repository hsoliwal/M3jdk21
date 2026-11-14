import assert from "node:assert/strict";
import test from "node:test";

import { runChromeSummarizer } from "../web/chrome_ai_summarizer_tool.mjs";

const request = Object.freeze({
  schema: "cpullm.chrome-summarizer-request.v1",
  api: "Summarizer",
  candidate_id: "a".repeat(64),
  input: JSON.stringify({ schema: "fixture", sources: [{ source_ref: "fixture://a" }] }),
  source_refs: ["fixture://a"],
  omitted_refs: [],
  authority: "CANDIDATE_ONLY",
  model_calls: 0,
  network_calls: 0,
});

function fakeApi({ availability = "available", summary = "Bound summary", streaming = false } = {}) {
  const calls = { availability: 0, create: 0, summarize: 0, destroy: 0, options: null };
  return {
    calls,
    async availability() {
      calls.availability += 1;
      return availability;
    },
    async create(options) {
      calls.create += 1;
      calls.options = options;
      options.monitor?.({ addEventListener(name, listener) { if (name === "downloadprogress") listener({ loaded: 0.5 }); } });
      return {
        async summarize(input, { context }) {
          calls.summarize += 1;
          assert.equal(input, request.input);
          assert.match(context, /source identities/i);
          return summary;
        },
        async *summarizeStreaming(input, { context }) {
          calls.summarize += 1;
          assert.equal(input, request.input);
          assert.match(context, /source identities/i);
          if (!streaming) throw new Error("unexpected stream");
          yield "Bound ";
          yield "summary";
        },
        destroy() { calls.destroy += 1; },
      };
    },
  };
}

test("unavailable API abstains without a model call", async () => {
  const result = await runChromeSummarizer(request, { summarizerApi: undefined });
  assert.equal(result.status, "ABSTAIN");
  assert.equal(result.reason, "SUMMARIZER_API_UNAVAILABLE");
  assert.equal(result.model_calls, 0);
  assert.equal(result.admitted, false);
});

test("downloadable model requires user activation", async () => {
  const api = fakeApi({ availability: "downloadable" });
  const result = await runChromeSummarizer(request, { summarizerApi: api, navigatorObject: { userActivation: { isActive: false } } });
  assert.equal(result.status, "ABSTAIN");
  assert.equal(result.reason, "USER_ACTIVATION_REQUIRED");
  assert.equal(api.calls.create, 0);
});

test("available model still requires user activation", async () => {
  const api = fakeApi({ availability: "available" });
  const result = await runChromeSummarizer(request, { summarizerApi: api, navigatorObject: { userActivation: { isActive: false } } });
  assert.equal(result.status, "ABSTAIN");
  assert.equal(result.reason, "USER_ACTIVATION_REQUIRED");
  assert.equal(api.calls.create, 0);
});

test("available model returns a bound non-authoritative candidate", async () => {
  const api = fakeApi();
  const result = await runChromeSummarizer(request, {
    summarizerApi: api,
    navigatorObject: { userActivation: { isActive: true } },
    config: {
      type: "key-points",
      format: "plain-text",
      length: "medium",
      preference: "speed",
      expectedInputLanguages: ["en"],
      expectedContextLanguages: ["en"],
      outputLanguage: "en",
    },
  });
  assert.equal(result.status, "CANDIDATE_ONLY");
  assert.equal(result.text, "Bound summary");
  assert.equal(result.source_candidate_id, request.candidate_id);
  assert.deepEqual(result.source_refs, request.source_refs);
  assert.equal(result.authority, "MODEL_CANDIDATE");
  assert.equal(result.candidate_only, true);
  assert.equal(result.admitted, false);
  assert.equal(result.model_calls, 1);
  assert.equal(result.direct_network_calls, 0);
  assert.match(result.result_id, /^[0-9a-f]{64}$/);
  assert.deepEqual(result.download_progress_million, [500000]);
  assert.equal(api.calls.destroy, 1);
  assert.equal(api.calls.options.preference, "speed");
  assert.deepEqual(api.calls.options.expectedInputLanguages, ["en"]);
});

test("result identity matches Java cross-language vector", async () => {
  const vectorRequest = {
    ...request,
    candidate_id: "a".repeat(64),
    source_refs: ["b".repeat(64)],
  };
  const api = fakeApi({ summary: "Browser candidate synthesis." });
  const result = await runChromeSummarizer(vectorRequest, {
    summarizerApi: api,
    navigatorObject: { userActivation: { isActive: true } },
  });
  assert.equal(
    result.result_id,
    "6f024cc964d3ba31b6b0a339dc21770c672c8c9e8b57896d858e610a106485ef",
  );
});

test("streaming output is assembled and session is destroyed", async () => {
  const api = fakeApi({ streaming: true });
  const result = await runChromeSummarizer(request, { summarizerApi: api, navigatorObject: { userActivation: { isActive: true } }, streaming: true });
  assert.equal(result.status, "CANDIDATE_ONLY");
  assert.equal(result.text, "Bound summary");
  assert.equal(api.calls.summarize, 1);
  assert.equal(api.calls.destroy, 1);
});

test("oversized output fails closed", async () => {
  const api = fakeApi({ summary: "x".repeat(128) });
  const result = await runChromeSummarizer(request, { summarizerApi: api, navigatorObject: { userActivation: { isActive: true } }, maxOutputBytes: 32 });
  assert.equal(result.status, "ABSTAIN");
  assert.equal(result.reason, "MODEL_OUTPUT_BUDGET");
  assert.equal(result.text, "");
  assert.equal(result.model_calls, 1);
});

test("CPU abstention never creates a browser session", async () => {
  const api = fakeApi();
  const result = await runChromeSummarizer({ ...request, status: "ABSTAIN", source_refs: [] }, { summarizerApi: api });
  assert.equal(result.status, "ABSTAIN");
  assert.equal(result.reason, "CPU_CANDIDATE_ABSTAIN");
  assert.equal(api.calls.availability, 0);
  assert.equal(api.calls.create, 0);
});

test("authority escalation is rejected before API access", async () => {
  const api = fakeApi();
  await assert.rejects(() => runChromeSummarizer({ ...request, authority: "VERIFIED" }, { summarizerApi: api }), /authority escalation/);
  assert.equal(api.calls.availability, 0);
});
