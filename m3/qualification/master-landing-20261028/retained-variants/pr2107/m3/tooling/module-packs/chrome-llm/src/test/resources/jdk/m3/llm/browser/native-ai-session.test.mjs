// SPDX-License-Identifier: Apache-2.0
import assert from 'node:assert/strict';
import test from 'node:test';
import { createChromeAiClient } from './chrome-ai-api.mjs';

function fixture(overrides = {}, state = 'available') {
  let runs = 0, destroyed = 0;
  const native = { inputQuota: 10000, measureInputUsage: async value => value.length,
    async write() { runs++; return 'written'; }, destroy() { destroyed++; }, ...overrides };
  const scope = { isSecureContext: true, navigator: { userActivation: { isActive: true } },
    Writer: { availability: async () => state, create: async () => native } };
  return { scope, native, get runs() { return runs; }, get destroyed() { return destroyed; } };
}
const deadline = error => error?.code === 'DEADLINE';

test('client rejects malformed input and output Unicode', async () => {
  const f = fixture(); const client = createChromeAiClient({ scope: f.scope }); await client.prepare('write');
  await assert.rejects(client.execute('write', '\uD800'), /Unicode|well-formed/); assert.equal(f.runs, 0); client.destroy();
  const bad = fixture({ write: async () => '\uDC00' }); const second = createChromeAiClient({ scope: bad.scope }); await second.prepare('write');
  await assert.rejects(second.execute('write', 'source'), /Unicode|well-formed/); assert.equal(bad.destroyed, 1);
});
test('all native structured text fields must be well-formed Unicode', async () => {
  const scope = { Proofreader: { availability: async () => 'available', create: async () => ({
    proofread: async () => ({ correctedInput: '\uD800', corrections: [] }), destroy() {} }) } };
  const client = createChromeAiClient({ scope }); await client.prepare('proofread');
  await assert.rejects(client.execute('proofread', 'source'), /Unicode|well-formed/);
});
test('invalid native quotas and usage cannot silently bypass checking', async () => {
  for (const overrides of [{ inputQuota: NaN }, { inputQuota: -1 }, { measureInputUsage: async () => NaN },
    { measureInputUsage: async () => -1 }, { measureInputUsage: async () => Infinity }]) {
    const f = fixture(overrides); const client = createChromeAiClient({ scope: f.scope }); await client.prepare('write');
    await assert.rejects(client.execute('write', 'source'), /quota|usage/i); assert.equal(f.runs, 0); assert.equal(f.destroyed, 1);
  }
});
test('native explicit infinite quota remains supported', async () => {
  const f = fixture({ inputQuota: Infinity, measureInputUsage: async () => 0 }); const client = createChromeAiClient({ scope: f.scope });
  await client.prepare('write'); assert.equal(await client.execute('write', 'source'), 'written'); client.destroy();
});
test('availability and creation deadlines clean up late sessions', async () => {
  const absent = createChromeAiClient({ scope: { Writer: { availability: () => new Promise(() => {}), create() {} } }, limits: { initializeTimeoutMs: 20 } });
  await assert.rejects(absent.prepare('write'), deadline);
  let resolve, destroyed = 0;
  const slow = createChromeAiClient({ scope: { Writer: { availability: async () => 'available', create: () => new Promise(yes => { resolve = yes; }) } }, limits: { initializeTimeoutMs: 20 } });
  await assert.rejects(slow.prepare('write'), deadline);
  resolve({ write() {}, destroy() { destroyed++; } }); await new Promise(yes => setImmediate(yes));
  assert.equal(destroyed, 1); assert.deepEqual(slow.prepared(), {});
});
test('capability discovery is bounded without downloading', async () => {
  let creates = 0;
  const client = createChromeAiClient({ scope: { Writer: { availability: () => new Promise(() => {}), create() { creates++; } } }, limits: { initializeTimeoutMs: 20 } });
  const state = await client.capabilities(); assert.equal(state.write.supported, true); assert.match(state.write.error, /deadline/i); assert.equal(creates, 0);
});
test('native inference deadline discards its prepared session', async () => {
  const f = fixture({ write: () => new Promise(() => {}) }); const client = createChromeAiClient({ scope: f.scope, limits: { timeoutMs: 20 } });
  await client.prepare('write'); await assert.rejects(client.execute('write', 'source'), deadline); assert.equal(f.destroyed, 1); assert.deepEqual(client.prepared(), {});
});
test('stream callback rejection awaits completion and closes iterator', async () => {
  let returned = 0;
  const f = fixture({ writeStreaming() { let produced = 0; return { [Symbol.asyncIterator]() { return this; }, async next() { return ++produced > 10 ? { done: true } : { done: false, value: 'chunk' }; }, return() { returned++; return { done: true }; } }; } });
  const client = createChromeAiClient({ scope: f.scope }); await client.prepare('write');
  await assert.rejects(client.execute('write', 'source', { async onChunk() { await Promise.resolve(); throw new Error('async chunk failed'); } }), /async chunk failed/);
  assert.equal(returned, 1); assert.equal(f.destroyed, 1);
});
test('runaway empty native stream has a finite fragment budget', async () => {
  let produced = 0, returned = 0;
  const f = fixture({ async *writeStreaming() { try { while (produced < 9000) { produced++; yield ''; } yield 'late'; } finally { returned++; } } });
  const client = createChromeAiClient({ scope: f.scope }); await client.prepare('write');
  await assert.rejects(client.execute('write', 'source', { onChunk() {} }), /stream|fragment/i);
  assert.ok(produced <= 8193); assert.equal(returned, 1);
});
test('output bound applies to UTF8 bytes for all native operations', async () => {
  const f = fixture({ write: async () => '😀😀' }); const client = createChromeAiClient({ scope: f.scope, limits: { maxOutputBytes: 7 } });
  await client.prepare('write'); await assert.rejects(client.execute('write', 'source'), /byte|budget/i); assert.equal(f.destroyed, 1);
});
test('summarizer uses injected scope activation and recovered chunk/measurement caps', async () => {
  const f = fixture({}, 'downloadable'); f.scope.Summarizer = { availability: async () => 'downloadable', create: async () => ({
    inputQuota: 1000, measureInputUsage: async value => value.length, summarize: async () => 'short', destroy() {} }) };
  const client = createChromeAiClient({ scope: f.scope });
  await client.prepare('summarize');
  await assert.rejects(client.execute('summarize', 'x'.repeat(600), { corpus: true, maxChars: 256, maxChunks: 1 }), error => error.code === 'CHUNK_LIMIT');
  await client.prepare('summarize');
  await assert.rejects(client.execute('summarize', 'x'.repeat(600), { corpus: true, maxChars: 256, maxMeasurements: 1 }), error => error.code === 'MEASUREMENT_LIMIT');
});
test('contextUsage respects source bounds and a native measurement deadline', async () => {
  const scope = { LanguageModel: { availability: async () => 'available', create: async () => ({
    prompt() {}, clone() {}, measureContextUsage: () => new Promise(() => {}), destroy() {} }) } };
  const client = createChromeAiClient({ scope, maxInputChars: 5, limits: { timeoutMs: 20 } }); await client.prepare('prompt');
  await assert.rejects(client.contextUsage('123456'), /exceeds/);
  await assert.rejects(client.contextUsage('small'), deadline); assert.deepEqual(client.prepared(), {});
});
test('native destroy exceptions cannot hide the original execution error', async () => {
  const f = fixture({ write: async () => { throw new Error('native work failed'); }, destroy() { throw new Error('cleanup failed'); } });
  const client = createChromeAiClient({ scope: f.scope }); await client.prepare('write');
  await assert.rejects(client.execute('write', 'source'), /native work failed/); assert.deepEqual(client.prepared(), {});
});
