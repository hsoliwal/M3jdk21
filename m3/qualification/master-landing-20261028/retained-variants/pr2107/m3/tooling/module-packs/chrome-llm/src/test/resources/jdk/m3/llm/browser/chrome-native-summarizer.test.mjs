// SPDX-License-Identifier: Apache-2.0
// Regression checks for the recovered Synexia summarizer, independent of its implementation.
import assert from 'node:assert/strict';
import test from 'node:test';
import { createHash } from 'node:crypto';
import { availability, createChromeAiSummarizer, splitText } from './chrome-native-summarizer.mjs';

const deferred = () => {
  let resolve;
  const promise = new Promise(yes => { resolve = yes; });
  return { promise, resolve };
};
const tick = () => new Promise(resolve => setImmediate(resolve));
const sha = value => createHash('sha256').update(value, 'utf8').digest('hex');
function fixture(overrides = {}, state = 'available') {
  const calls = []; let destroyed = 0;
  const session = {
    inputQuota: 1024,
    measureInputUsage: async (value, request) => value.length + (request.context?.length ?? 0),
    summarize: async () => 'short',
    async *summarizeStreaming() { yield 'one'; yield ' two'; },
    destroy() { destroyed++; }, ...overrides,
  };
  const api = {
    async availability(options) { calls.push({ kind: 'availability', options }); return state; },
    async create(options) { calls.push({ kind: 'create', options }); return session; },
  };
  return { api, session, calls, get destroyed() { return destroyed; } };
}
function code(expected) { return error => error?.code === expected; }

test('availability receives core options; creation retains shared context and preference', async () => {
  const f = fixture();
  const options = { sharedContext: 'topic', type: 'headline', preference: 'capability', outputLanguage: 'EN-us' };
  assert.equal(await availability({ api: f.api, ...options }), 'available');
  const handle = await createChromeAiSummarizer({ api: f.api, ...options });
  for (const call of f.calls.filter(call => call.kind === 'availability')) {
    assert.equal(call.options.sharedContext, undefined);
    assert.equal(call.options.preference, 'capability');
    assert.equal(call.options.outputLanguage, 'en-US');
  }
  assert.equal(f.calls.at(-1).options.sharedContext, 'topic'); handle.destroy();
});
test('language snapshots and monitor are immutable and correctly forwarded', async () => {
  const f = fixture(); const input = ['en']; const monitor = { addEventListener() {} }; let observed;
  f.api.create = async options => { options.monitor(monitor); return f.session; };
  const handle = await createChromeAiSummarizer({ api: f.api, expectedInputLanguages: input, monitor: value => { observed = value; } });
  input.push('fr'); assert.deepEqual(handle.options.expectedInputLanguages, ['en']);
  assert.throws(() => handle.options.expectedInputLanguages.push('ja'), TypeError);
  assert.equal(observed, monitor); handle.destroy();
});
test('download states require actual activation; unavailable APIs cannot create', async () => {
  for (const state of ['downloadable', 'downloading']) {
    const f = fixture({}, state);
    await assert.rejects(createChromeAiSummarizer({ api: f.api, userActivation: () => false }), code('ACTIVATION_REQUIRED'));
    assert.equal(f.calls.filter(call => call.kind === 'create').length, 0);
    const handle = await createChromeAiSummarizer({ api: f.api, userActivation: () => true });
    assert.equal(handle.availability, state); handle.destroy();
  }
  await assert.rejects(createChromeAiSummarizer({ api: fixture({}, 'unavailable').api }), code('UNAVAILABLE'));
  await assert.rejects(createChromeAiSummarizer({ api: fixture({}, 'mystery').api }), code('UNAVAILABLE'));
});
test('source partition preserves every UTF16 code unit without splitting Unicode pairs', () => {
  const source = `  ${'x'.repeat(253)}😀${'\n \t😀 word'.repeat(500)}  `;
  const pieces = splitText(source, 256); assert.equal(pieces.join(''), source);
  for (const piece of pieces) { assert.ok(piece.length <= 256); assert.ok(piece.isWellFormed()); }
  assert.deepEqual(splitText('  source  '), ['  source  ']);
  assert.throws(() => splitText('\uD800'), code('INVALID_TEXT'));
});
test('corpus is serial and receipt exactly partitions raw source including whitespace', async () => {
  let active = 0, maximum = 0; const sent = [], progress = [];
  const f = fixture({ async summarize(value) { active++; maximum = Math.max(maximum, active); sent.push(value); await tick(); active--; return 'short'; } });
  const handle = await createChromeAiSummarizer({ api: f.api });
  const source = `  ${'😀 meaningful text\n'.repeat(90)}${' \n'.repeat(140)} tail  `;
  const result = await handle.summarizeCorpusWithReceipt(source, { maxChars: 256, onProgress: value => { progress.push(value); } });
  assert.equal(maximum, 1); assert.equal(result.text, 'short');
  assert.equal(result.receipt.sourceSha256, sha(source));
  assert.equal(result.receipt.outputSha256, sha(result.text));
  assert.equal(result.receipt.authority, 'CANDIDATE_ONLY'); assert.equal(result.receipt.promotable, false);
  assert.equal(result.receipt.gpuExecution, 'UNKNOWN'); assert.equal(result.receipt.coordinateSystem, 'UTF16_CODE_UNITS');
  assert.equal(result.receipt.completeSourcePartition, true); assert.equal(result.receipt.nativeQuotaMeasured, true);
  let end = 0;
  for (const span of result.receipt.sourceSpans) {
    assert.equal(span.start, end); assert.ok(span.end > span.start);
    const original = source.slice(span.start, span.end); assert.ok(original.isWellFormed());
    assert.equal(span.inputSha256, sha(original)); end = span.end;
    const node = result.receipt.nodes.find(node => node.id === span.node); assert.ok(node);
    assert.equal(node.inputSha256, span.inputSha256);
    if (!original.trim()) assert.equal(node.kind, 'WHITESPACE_NOT_SENT');
  }
  assert.equal(end, source.length); assert.equal(result.receipt.calls, sent.length);
  assert.ok(progress.every(Object.isFrozen)); assert.equal(progress.at(-1).calls, sent.length);
  assert.ok(result.receipt.nodes.some(node => node.round > 0 && node.parents.length > 1));
  handle.destroy();
});
test('nonshrinking generation fails within one round and closes the native session', async () => {
  let calls = 0; const f = fixture({ summarize: async value => { calls++; return value; } });
  const handle = await createChromeAiSummarizer({ api: f.api });
  await assert.rejects(handle.summarizeCorpus('a'.repeat(600), { maxChars: 256 }), code('NON_SHRINKING'));
  assert.equal(calls, 3); assert.equal(f.destroyed, 1);
});
for (const [setting, expected] of [[{ maxCalls: 1 }, 'CALL_LIMIT'], [{ maxRounds: 1 }, 'ROUND_LIMIT'],
  [{ maxChunks: 1 }, 'CHUNK_LIMIT'], [{ maxMeasurements: 1 }, 'MEASUREMENT_LIMIT']]) {
  test(`corpus ${expected} is enforced without a partial result`, async () => {
    const handle = await createChromeAiSummarizer({ api: fixture().api });
    await assert.rejects(handle.summarizeCorpus('x'.repeat(600), { maxChars: 256, ...setting }), code(expected));
    assert.equal(handle.closed, true);
  });
}
test('source and generated output budgets use UTF8 bytes', async () => {
  const input = await createChromeAiSummarizer({ api: fixture().api, limits: { maxInputBytes: 512 } });
  await assert.rejects(input.summarizeCorpus('😀'.repeat(129)), code('BYTE_LIMIT')); input.destroy();
  const output = await createChromeAiSummarizer({ api: fixture({ summarize: async () => '😀😀' }).api, limits: { maxOutputBytes: 7 } });
  await assert.rejects(output.summarize('source'), code('BYTE_LIMIT')); assert.equal(output.closed, true);
});
test('native quota measurement includes context and adapts exact source slices', async () => {
  const measured = [], inputs = [];
  const f = fixture({ inputQuota: 110,
    async measureInputUsage(value, request) { measured.push(request.context); return value.length + request.context.length; },
    async summarize(value, request) { assert.ok(value.length + request.context.length <= 110); inputs.push(value); return 'short'; },
  });
  const handle = await createChromeAiSummarizer({ api: f.api }); const source = '😀abc'.repeat(110);
  const result = await handle.summarizeCorpusWithReceipt(source, { maxChars: 256, context: 'context' });
  assert.equal(inputs.slice(0, result.receipt.sourceSpans.length).join(''), source);
  assert.ok(measured.every(value => value === 'context')); assert.ok(measured.length > inputs.length); handle.destroy();
});
test('a single code point that cannot fit quota fails without truncation', async () => {
  const handle = await createChromeAiSummarizer({ api: fixture({ inputQuota: 1, measureInputUsage: async value => value.length + 1 }).api });
  await assert.rejects(handle.summarizeCorpus('😀'), code('INPUT_QUOTA')); assert.equal(handle.closed, true);
});
test('explicit native infinite quota is accepted with actual finite measurement', async () => {
  let measured = 0;
  const handle = await createChromeAiSummarizer({ api: fixture({ inputQuota: Infinity, measureInputUsage: async () => { measured++; return 0; } }).api });
  assert.equal(await handle.measureInputUsage('source'), 0);
  assert.equal(await handle.summarizeCorpus('x'.repeat(600), { maxChars: 256 }), 'short');
  assert.ok(measured > 1); handle.destroy();
});
test('invalid native quotas and usage are rejected', async () => {
  for (const overrides of [{ inputQuota: NaN }, { inputQuota: -1 }, { inputQuota: -Infinity },
    { measureInputUsage: async () => NaN }, { measureInputUsage: async () => -1 }, { measureInputUsage: async () => Infinity }]) {
    const handle = await createChromeAiSummarizer({ api: fixture(overrides).api });
    await assert.rejects(handle.summarize('source'), code('INVALID_QUOTA')); assert.equal(handle.closed, true);
  }
});
test('large corpus without native quota refuses speculative splitting', async () => {
  const handle = await createChromeAiSummarizer({ api: fixture({ inputQuota: undefined, measureInputUsage: undefined }).api });
  assert.equal(await handle.summarize('short source'), 'short');
  await assert.rejects(handle.summarizeCorpus('x'.repeat(600), { maxChars: 256 }), code('QUOTA_UNAVAILABLE'));
});
test('async progress rejection closes session before publishing a result', async () => {
  const f = fixture(); const handle = await createChromeAiSummarizer({ api: f.api });
  await assert.rejects(handle.summarizeCorpus('source', { async onProgress() { await tick(); throw new Error('async observer failed'); } }), /async observer failed/);
  assert.equal(f.destroyed, 1);
});
test('async progress is serial and cancellation from final callback cannot publish success', async () => {
  const controller = new AbortController(); const handle = await createChromeAiSummarizer({ api: fixture().api });
  await assert.rejects(handle.summarizeCorpus('source', { signal: controller.signal, async onProgress() { await tick(); controller.abort(); } }), { name: 'AbortError' });
  assert.equal(handle.closed, true);
});
test('progress that never settles is bounded by the operation deadline', async () => {
  const handle = await createChromeAiSummarizer({ api: fixture().api, limits: { timeoutMs: 20 } });
  await assert.rejects(handle.summarizeCorpus('source', { onProgress: () => new Promise(() => {}) }), code('DEADLINE'));
  assert.equal(handle.closed, true);
});
test('late native creation after cancellation is reclaimed exactly once', async () => {
  const ready = deferred(), started = deferred(), controller = new AbortController(); let destroyed = 0;
  const creating = createChromeAiSummarizer({ api: { availability: async () => 'available', create() { started.resolve(); return ready.promise; } }, signal: controller.signal });
  await started.promise; controller.abort(); await assert.rejects(creating, { name: 'AbortError' });
  ready.resolve({ summarize() {}, destroy() { destroyed++; } }); await tick(); assert.equal(destroyed, 1);
});
test('request abort closes work even when native inference ignores its signal', async () => {
  const ready = deferred(), started = deferred(), controller = new AbortController(); let nativeSignal;
  const f = fixture({ summarize(_value, request) { nativeSignal = request.signal; started.resolve(); return ready.promise; } });
  const handle = await createChromeAiSummarizer({ api: f.api });
  const running = handle.summarize('source', { signal: controller.signal }); await started.promise; controller.abort();
  await assert.rejects(running, { name: 'AbortError' }); assert.equal(nativeSignal.aborted, true); assert.equal(f.destroyed, 1); ready.resolve('late');
});
test('busy refusal leaves the first request and session usable', async () => {
  const ready = deferred(); const handle = await createChromeAiSummarizer({ api: fixture({ summarize: () => ready.promise }).api });
  const first = handle.summarize('first'); await assert.rejects(handle.summarize('second'), code('BUSY'));
  assert.equal(handle.closed, false); ready.resolve('short'); assert.equal(await first, 'short'); handle.destroy();
});
test('native inference and initialization each have enforced deadlines', async () => {
  const handle = await createChromeAiSummarizer({ api: fixture({ summarize: () => new Promise(() => {}) }).api, limits: { timeoutMs: 20 } });
  await assert.rejects(handle.summarize('source'), code('DEADLINE')); assert.equal(handle.closed, true);
  await assert.rejects(createChromeAiSummarizer({ api: { availability: () => new Promise(() => {}), create() {} }, limits: { initializeTimeoutMs: 20 } }), code('DEADLINE'));
});
test('destroy is idempotent and aborts current operation with closed state', async () => {
  const f = fixture({ summarize: () => new Promise(() => {}) }); const handle = await createChromeAiSummarizer({ api: f.api });
  const pending = handle.summarize('source'); handle.destroy(); handle.destroy();
  await assert.rejects(pending, code('CLOSED')); assert.equal(f.destroyed, 1);
  await assert.rejects(handle.summarize('again'), code('CLOSED'));
});
test('stream cancellation and early consumer return close native resources', async () => {
  for (const cancel of [true, false]) {
    const pending = deferred(), controller = new AbortController();
    const f = fixture({ async *summarizeStreaming() { yield 'first'; await pending.promise; yield 'late'; } });
    const handle = await createChromeAiSummarizer({ api: f.api }); const iterator = handle.summarizeStreaming('source', { signal: controller.signal });
    assert.equal((await iterator.next()).value, 'first');
    if (cancel) { const next = iterator.next(); controller.abort(); await assert.rejects(next, { name: 'AbortError' }); }
    else await iterator.return();
    assert.equal(f.destroyed, 1); pending.resolve();
  }
});
test('empty and whitespace-only native streams cannot complete successfully', async () => {
  for (const whitespace of [false, true]) {
    const handle = await createChromeAiSummarizer({ api: fixture({ async *summarizeStreaming() { if (whitespace) yield '  '; } }).api });
    await assert.rejects(async () => { for await (const _chunk of handle.summarizeStreaming('source')) { /* provisional */ } }, code('EMPTY_TEXT'));
    assert.equal(handle.closed, true);
  }
});
test('stream output uses byte bounds and malformed Unicode is rejected', async () => {
  const handle = await createChromeAiSummarizer({ api: fixture({ async *summarizeStreaming() { yield '😀'; yield '😀'; } }).api, limits: { maxOutputBytes: 7 } });
  const stream = handle.summarizeStreaming('source'); assert.equal((await stream.next()).value, '😀');
  await assert.rejects(stream.next(), code('BYTE_LIMIT'));
  const invalid = await createChromeAiSummarizer({ api: fixture({ summarize: async () => '\uD800' }).api });
  await assert.rejects(invalid.summarize('source'), code('INVALID_TEXT'));
});

test('historical c26433b5 smoke assertions remain compatible', async () => {
  
  const calls = [];
  const fakeSession = {
    async summarize(text, options) {
      calls.push({ method: "summarize", text, options });
      return `summary:${text}`;
    },
    async *summarizeStreaming(text, options) {
      calls.push({ method: "stream", text, options });
      yield "one";
      yield " two";
    },
    destroy() {
      calls.push({ method: "destroy" });
    },
  };
  const fakeApi = {
    async availability() {
      return "available";
    },
    async create(options) {
      calls.push({ method: "create", options });
      return fakeSession;
    },
  };
  
  assert.equal(await availability({ api: fakeApi }), "available");
  assert.deepEqual(splitText("alpha beta gamma", 256), ["alpha beta gamma"]);
  const summarizer = await createChromeAiSummarizer({
    api: fakeApi,
    type: "key-points",
    format: "markdown",
    length: "long",
    preference: "capability",
    expectedInputLanguages: ["en"],
    outputLanguage: "en",
  });
  assert.equal(summarizer.options.preference, "capability");
  assert.equal(await summarizer.summarize("source", { context: "context" }), "summary:source");
  assert.deepEqual([...await (async () => { const values = []; for await (const chunk of summarizer.summarizeStreaming("source")) values.push(chunk); return values; })()], ["one", " two"]);
  assert.equal((await summarizer.summarizeCorpus("short corpus")).startsWith("summary:"), true);
  summarizer.destroy();
  assert.throws(() => splitText("too short", 8), /at least 256/);
  await assert.rejects(() => createChromeAiSummarizer({ api: { availability: async () => "unavailable", create: async () => fakeSession } }), /unavailable/);
  console.log("Chrome native summarizer: OK");
});
