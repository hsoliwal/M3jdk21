// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
// Standalone native-API fixtures; these tests do not execute Chrome/Gemini Nano.
import test from 'node:test';
import assert from 'node:assert/strict';
import { createInterface } from 'node:readline';
import { createChromeProtocolBridge, makeChromeRequest, requestPacketId, resultPacketId,
  encodePacket, DEFAULT_LIMITS, PROTOCOL } from './chrome-protocol-bridge.mjs';

const output = 'Candidate 📚\nKeep source.';
const fixture = () => makeChromeRequest('node-π', 'source text 📚\n', ['source:a, b', 'source:[x]', 'source:\u001fΩ']);
const base = (action, operation = 'summarize', sessionId = 'fixture-session', sequence = 0) =>
  ({ protocol: PROTOCOL, action, operation, sessionId, sequence });
const wire = value => encodePacket(value);
const send = async (bridge, value) => JSON.parse(await bridge.exchange(wire(value)));
const open = (bridge, operation = 'summarize', limits = DEFAULT_LIMITS) => send(bridge,
  { ...base('open', operation), limits });
const close = (bridge, sequence, operation = 'summarize') => send(bridge, base('close', operation, 'fixture-session', sequence));
async function packet(request = undefined, sequence = 1, operation = 'summarize') {
  request ??= await fixture();
  return { ...base('execute', operation, 'fixture-session', sequence),
    packetId: await requestPacketId(operation, 'fixture-session', sequence, request), request };
}
function fake(options = {}) {
  const stats = { created: 0, destroyed: 0, cloned: 0, calls: 0, measured: 0, active: 0, maximumActive: 0 };
  const activation = { isActive: true };
  const quota = options.quota ?? 10000;
  function session(op) {
    let closed = false;
    const raw = {
      inputQuota: quota, contextWindow: quota, contextUsage: options.contextUsage ?? 0,
      async measureInputUsage() { stats.measured++; return options.measured ?? 10; },
      async measureContextUsage() { stats.measured++; return options.measured ?? 10; },
      async clone() { stats.cloned++; return session('prompt'); },
      async [op === 'summarize' ? 'summarize' : 'prompt'](input, request) {
        assert.equal(closed, false); stats.calls++; stats.active++;
        stats.maximumActive = Math.max(stats.maximumActive, stats.active);
        try {
          options.onCall?.(input, request);
          if (options.wait) await options.wait;
          if (options.throwNative) throw new Error('fixture native failure');
          return options.output ?? output;
        } finally { stats.active--; }
      },
      destroy() { assert.equal(closed, false, 'native destroy must be idempotently wrapped'); closed = true; stats.destroyed++; },
    };
    if (options.noMeasure) { delete raw.measureInputUsage; delete raw.measureContextUsage; }
    if (options.noClone) delete raw.clone;
    if (options.noMethod) delete raw[op === 'summarize' ? 'summarize' : 'prompt'];
    return raw;
  }
  const api = op => ({
    async availability() {
      options.onAvailability?.(activation);
      return options.availability ?? 'available';
    },
    async create() { stats.created++; return session(op); },
  });
  const scope = { isSecureContext: true, navigator: { userActivation: activation },
    Summarizer: api('summarize'), LanguageModel: api('prompt') };
  return { stats, scope, activation };
}
async function ready(options = {}, operation = 'summarize', limits = {}) {
  const native = fake(options);
  const bridge = createChromeProtocolBridge({ scope: native.scope, limits });
  await bridge.prepare(operation);
  await open(bridge, operation, { ...DEFAULT_LIMITS, ...limits });
  return { ...native, bridge };
}

if (process.argv[2] === '--java-fixture') {
  // A finite stdin/stdout fixture for a caller-owned Java transport; no HTTP or live browser.
  const { scope } = fake();
  const bridge = createChromeProtocolBridge({ scope });
  const lines = createInterface({ input: process.stdin, crlfDelay: Infinity });
  try {
    for await (const line of lines) {
      const request = JSON.parse(line);
      if (request.action === 'open') await bridge.prepare(request.operation);
      process.stdout.write((await bridge.exchange(line)) + '\n');
    }
  } finally { bridge.destroy(); }
} else {

test('frozen Java request/result and packet roots agree, including Unicode and comma/bracket refs', async () => {
  const request = await fixture();
  assert.equal(request.inputSha256, '311766686fa50936589f472c91ab632b1eabaf977846e96a22a2feca2dc8fb2e');
  assert.equal(request.requestId, '1e32b11bd3213de1658d319ad8897c7e80c6d54506019de1d883438496723fef');
  const { bridge } = await ready();
  const response = await send(bridge, await packet(request));
  assert.equal(response.result.resultId, 'bd454934b6e17b96b1b9749b00b611a6b00a10ed712a1a5e2fd21687ca80b983');
  const summary = await requestPacketId('summarize', 'fixture-session', 7, request);
  const prompt = await requestPacketId('prompt', 'fixture-session', 7, request);
  assert.equal(summary, 'f223d91e16ddff94bc581315d8327bff68c82292e2f5b0237886e9dae6bc8a2d');
  assert.equal(prompt, '2ea63d89f3bb39a8e66b8cc6367e0396b054a931d4483ffdbb680046dd684d3a');
  assert.equal(await resultPacketId('summarize', 'fixture-session', 7, summary, response.result),
    '254da60acfbd171786389c5f08618d92518433850ba6d666cf2fe8494161220c');
  assert.equal(await resultPacketId('prompt', 'fixture-session', 7, prompt, response.result),
    '90af39cb7d6806526276ce7d5a338a9882d816d0967fdf49dce6fc4e7cba8e45');
  await close(bridge, 1);
});

test('supplemental packet hashes distinguish inherited list display collisions', async () => {
  const one = await makeChromeRequest('n', 'text', ['a, b']);
  const two = await makeChromeRequest('n', 'text', ['a', 'b']);
  assert.equal(one.requestId, two.requestId);
  assert.notEqual(await requestPacketId('summarize', 's', 1, one), await requestPacketId('summarize', 's', 1, two));
  const { bridge, stats } = await ready();
  const forged = await packet(one); forged.request = two;
  await assert.rejects(send(bridge, forged), /packet identity/);
  assert.equal(stats.calls, 0);
  await close(bridge, 1);
});

test('factory and unprepared transport never create/download a model', async () => {
  const { scope, stats } = fake();
  const bridge = createChromeProtocolBridge({ scope });
  assert.equal(stats.created, 0);
  await assert.rejects(open(bridge), /Prepare/);
  assert.equal(stats.created, 0);
});

test('preparation requires current activation and a secure context, checked again at create', async () => {
  for (const mode of ['inactive', 'insecure', 'expired']) {
    const { scope, stats, activation } = fake(mode === 'expired' ? { onAvailability: a => { a.isActive = false; } } : {});
    if (mode === 'inactive') activation.isActive = false;
    if (mode === 'insecure') scope.isSecureContext = false;
    const bridge = createChromeProtocolBridge({ scope });
    await assert.rejects(bridge.prepare('summarize'), /gesture|secure/);
    assert.equal(stats.created, 0);
  }
});

test('download consent is explicit, and unavailable APIs do not become candidates', async () => {
  const native = fake({ availability: 'downloadable' });
  const bridge = createChromeProtocolBridge({ scope: native.scope });
  await assert.rejects(bridge.prepare('summarize'), /download consent/);
  assert.equal(native.stats.created, 0);
  await bridge.prepare('summarize', {}, { allowDownload: true });
  assert.equal(native.stats.created, 1);
  await open(bridge); await close(bridge, 0);
  for (const state of ['unavailable', 'unknown']) {
    const unavailable = fake({ availability: state });
    await assert.rejects(createChromeProtocolBridge({ scope: unavailable.scope }).prepare('summarize'));
    assert.equal(unavailable.stats.created, 0);
  }
});

test('Summarizer calls actual client once and closes native session, with immutable authority fields', async () => {
  const { bridge, stats } = await ready();
  await assert.rejects(bridge.prepare('prompt'), /Close/);
  await assert.rejects(open(bridge), /Prepare/);
  const response = await send(bridge, await packet());
  assert.deepEqual(response.result.sourceRefs, (await fixture()).sourceRefs);
  assert.equal(response.result.authority, 'MODEL_CANDIDATE'); assert.equal(response.result.candidateOnly, true);
  assert.equal(response.result.admitted, false); assert.equal(response.result.modelCalls, 1);
  assert.equal(response.result.directNetworkCalls, 0); assert.equal(stats.calls, 1);
  assert.ok(stats.measured > 0);
  assert.equal((await close(bridge, 1)).modelCalls, 1); assert.equal(stats.destroyed, 1);
  await assert.rejects(send(bridge, await packet()), /binding/);
});

test('Prompt shares the actual client and uses a fresh clone for each independent job', async () => {
  const { bridge, stats } = await ready({}, 'prompt');
  const response = await send(bridge, await packet(undefined, 1, 'prompt'));
  assert.equal(response.operation, 'prompt'); assert.equal(response.result.text, output);
  assert.equal(stats.calls, 1); assert.equal(stats.cloned, 1); assert.equal(stats.destroyed, 1);
  await send(bridge, await packet(undefined, 2, 'prompt'));
  assert.equal(stats.cloned, 2); assert.equal(stats.destroyed, 2);
  await close(bridge, 2, 'prompt'); assert.equal(stats.destroyed, 3);
});

test('invalid native sessions fail preparation and are released', async () => {
  for (const [operation, settings] of [['summarize', { noMethod: true }],
    ['prompt', { noMethod: true }], ['prompt', { noClone: true }]]) {
    const { scope, stats } = fake(settings);
    await assert.rejects(createChromeProtocolBridge({ scope }).prepare(operation), /Invalid native session/);
    assert.equal(stats.created, 1); assert.equal(stats.destroyed, 1); assert.equal(stats.calls, 0);
  }
});

test('tampered input, counters, authority, references and hashes fail before a native call', async () => {
  const { bridge, stats } = await ready();
  const original = await packet();
  const mutations = [
    r => { r.input += 'altered'; }, r => { r.inputSha256 = '0'.repeat(64); },
    r => { r.requestId = '0'.repeat(64); }, r => { r.authority = 'VERIFIED'; },
    r => { r.candidateOnly = false; }, r => { r.modelCalls = 1; }, r => { r.directNetworkCalls = 1; },
    r => { r.sourceRefs.reverse(); }, r => { r.sourceRefs.push(r.sourceRefs[0]); }, r => { r.extra = true; },
  ];
  for (const mutate of mutations) {
    const forged = structuredClone(original); mutate(forged.request);
    await assert.rejects(send(bridge, forged));
  }
  assert.equal(stats.calls, 0); await close(bridge, 1);
});

test('strict lexical JSON rejects duplicate keys, alternative escapes, invalid unicode and unknown fields', async () => {
  const { bridge, stats } = await ready();
  const original = wire(await packet());
  const mutated = [
    ' ' + original,
    original.replace('"sequence":1', '"sequence":1,"sequence":1'),
    original.replace('"sequence":1', '"sequence":1e0'),
    original.replace('source text', '\\u+041'), original.replace('source text', '\\u-001'),
    original.replace('source text', '\\ud800'), original.replace('source text', '\\udc00'),
    original.replace('source text', '\\u0073ource text'),
    original.slice(0, -1) + ',"extra":true}',
  ];
  for (const text of mutated) await assert.rejects(bridge.exchange(text));
  assert.equal(stats.calls, 0); await close(bridge, 1);
  for (const invalid of ['\ud800', '\udc00']) {
    await assert.rejects(makeChromeRequest('n', invalid, []));
    await assert.rejects(makeChromeRequest('n', 'text', [invalid]));
  }
});

test('configured UTF-8 bytes, metadata, reference count and output budgets are enforced', async () => {
  const limits = { maxInputBytes: 64, maxOutputBytes: 64, maxSourceRefs: 3, maxMetadataBytes: 100, maxModelCalls: 2 };
  const { bridge, stats } = await ready({}, 'summarize', limits);
  for (const request of [
    await makeChromeRequest('n', '📚'.repeat(17), []),
    await makeChromeRequest('n', 'text', ['1', '2', '3', '4']),
    await makeChromeRequest('n', 'text', ['r'.repeat(101)]),
  ]) await assert.rejects(send(bridge, await packet(request)), /budget/);
  assert.equal(stats.calls, 0); await close(bridge, 1);
  const over = await ready({ output: '📚'.repeat(17) }, 'summarize', limits);
  await assert.rejects(send(over.bridge, await packet()), /budget/);
  assert.equal(over.stats.calls, 1); assert.equal(over.stats.destroyed, 1); await close(over.bridge, 1);
});

test('limits negotiation cannot enlarge browser budgets', async () => {
  assert.throws(() => createChromeProtocolBridge({ limits: { maxModelCalls: 4097 } }));
  const { scope, stats } = fake();
  const bridge = createChromeProtocolBridge({ scope, limits: { maxModelCalls: 1 } });
  await bridge.prepare('summarize');
  await assert.rejects(open(bridge), /maxModelCalls/);
  assert.equal(stats.calls, 0); bridge.destroy(); assert.equal(stats.destroyed, 1);
});

test('native Summarizer quota telemetry is mandatory, measured and never estimated from characters', async () => {
  for (const settings of [{ quota: 1, measured: 2 }, { quota: NaN }, { quota: -1 },
    { measured: NaN }, { measured: Infinity }, { measured: -1 }, { noMeasure: true }]) {
    const { bridge, stats } = await ready(settings);
    await assert.rejects(send(bridge, await packet()));
    assert.equal(stats.calls, 0); assert.equal(stats.destroyed, 1);
    assert.equal((await close(bridge, 1)).modelCalls, 0);
  }
  const infinite = await ready({ quota: Infinity });
  await send(infinite.bridge, await packet()); assert.equal(infinite.stats.calls, 1);
  await close(infinite.bridge, 1);
});

test('Prompt rejects missing, invalid or overflowing measured context before generation', async () => {
  for (const settings of [{ quota: 12, measured: 10, contextUsage: 3 }, { measured: NaN },
    { contextUsage: NaN }, { quota: -1 }, { noMeasure: true }]) {
    const { bridge, stats } = await ready(settings, 'prompt');
    await assert.rejects(send(bridge, await packet(undefined, 1, 'prompt')));
    assert.equal(stats.calls, 0); assert.equal(stats.cloned, 1); assert.equal(stats.destroyed, 2);
    await close(bridge, 1, 'prompt');
  }
});

test('sequence replay and call budget refusal do not generate another candidate', async () => {
  const { bridge, stats } = await ready({}, 'summarize', { maxModelCalls: 1 });
  const request = await packet(); await send(bridge, request);
  await assert.rejects(send(bridge, request), /Replayed/);
  await assert.rejects(send(bridge, await packet(undefined, 2)), /sequence/);
  assert.equal(stats.calls, 1); await close(bridge, 1);
});

test('one operation runs at a time and explicit destroy aborts pending work', async () => {
  let release, entered;
  const waiting = new Promise(resolve => { release = resolve; });
  const started = new Promise(resolve => { entered = resolve; });
  const { bridge, stats } = await ready({ wait: waiting, onCall: () => entered() });
  const pending = send(bridge, await packet());
  await started;
  await assert.rejects(send(bridge, await packet(undefined, 2)), /One browser/);
  await assert.rejects(bridge.prepare('prompt'), /One browser/);
  bridge.destroy();
  await assert.rejects(pending);
  release();
  assert.equal(stats.maximumActive, 1); assert.equal(stats.destroyed, 1);
  await close(bridge, 1);
});

test('native failures count the attempted API call and require a fresh prepared session', async () => {
  const { bridge, stats } = await ready({ throwNative: true });
  await assert.rejects(send(bridge, await packet()), /fixture native/);
  await assert.rejects(send(bridge, await packet(undefined, 2)), /not ready/);
  assert.equal(stats.calls, 1); assert.equal(stats.destroyed, 1);
  assert.equal((await close(bridge, 1)).modelCalls, 1);
});

test('invalid/blank native output never becomes an admitted or fabricated candidate', async () => {
  for (const text of ['\t', '\u001f', '\ud800', '\udc00']) {
    const { bridge, stats } = await ready({ output: text });
    await assert.rejects(send(bridge, await packet()));
    assert.equal(stats.calls, 1); assert.equal(stats.destroyed, 1); await close(bridge, 1);
  }
});

test('fixture execution has not performed live Chrome inference', () => {
  assert.equal(typeof globalThis.Summarizer, 'undefined');
  console.log('Chrome protocol bridge contract fixtures PASS; native Chrome inference NOT_EXECUTED');
});

}
