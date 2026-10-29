import test from 'node:test';
import assert from 'node:assert/strict';
import { createChromeAiClient, normalizeCreateOptions, OPERATIONS } from './chrome-ai-api.mjs';

function mocks({ availability = 'available', overrides = {} } = {}) {
  const calls = []; const destroyed = []; let serial = 0;
  const scope = { isSecureContext: true };
  for (const [operation, info] of Object.entries(OPERATIONS)) {
    const makeSession = (clone = false) => {
      const id = ++serial;
      const value = operation === 'detect' ? [{ detectedLanguage: 'en', confidence: 0.9 }] : operation === 'proofread' ? { correctedInput: 'fixed', corrections: [] } : `${operation}:result`;
      const session = {
        [info.method]: async (input, options) => { calls.push({ kind: 'execute', operation, input, options, clone }); return value; },
        destroy: () => destroyed.push(id),
        contextUsage: 5, contextWindow: 100, inputQuota: 10000,
        measureContextUsage: async () => 4,
        measureInputUsage: async input => input.length,
        ...overrides[operation],
      };
      if (info.stream) session[info.stream] = async function* (input, options) { calls.push({ kind: 'stream', operation, input, options }); yield 'one'; yield ' two'; };
      if (operation === 'prompt') session.clone = async options => { calls.push({ kind: 'clone', options }); return makeSession(true); };
      return session;
    };
    scope[info.api] = {
      availability: async options => { calls.push({ kind: 'availability', operation, options }); return availability; },
      create: async options => { calls.push({ kind: 'create', operation, options }); return makeSession(); },
    };
  }
  return { scope, calls, destroyed };
}
const options = operation => operation === 'translate' ? { sourceLanguage: 'en', targetLanguage: 'es' } : {};

test('capabilities passively detect all APIs, translator needs explicit language pair', async () => {
  const m = mocks(); const c = createChromeAiClient({ scope: m.scope });
  const state = await c.capabilities();
  assert.equal(Object.keys(state).length, 7);
  assert.equal(state.translate.availability, 'requires-options');
  assert.equal(state.summarize.availability, 'available');
  assert.ok(m.calls.every(x => x.kind === 'availability'));
  assert.equal((await c.capabilities({ translate: options('translate') })).translate.availability, 'available');
});

for (const operation of Object.keys(OPERATIONS)) {
  test(`${operation} uses its native API with explicit preparation and destruction`, async () => {
    const m = mocks(); const c = createChromeAiClient({ scope: m.scope });
    await assert.rejects(c.execute(operation, 'hello'), /not prepared/);
    await c.prepare(operation, options(operation));
    const value = await c.execute(operation, 'hello');
    assert.deepEqual(value, operation === 'detect' ? [{ detectedLanguage: 'en', confidence: 0.9 }] : operation === 'proofread' ? { correctedInput: 'fixed', corrections: [] } : `${operation}:result`);
    assert.ok(m.calls.some(x => x.kind === 'execute' && x.operation === operation));
    if (operation === 'prompt') {
      assert.ok(m.calls.find(x => x.kind === 'execute').clone);
      assert.equal(m.destroyed.length, 1, 'prompt clone cleaned after execution');
      assert.equal((await c.contextUsage('more')).inputUsage, 4);
    }
    c.destroy(operation); assert.ok(m.destroyed.length > 0); assert.deepEqual(c.prepared(), {});
  });
}

test('unsupported, unavailable and unknown states never create or fall back', async () => {
  const empty = createChromeAiClient({ scope: {} });
  assert.equal((await empty.capabilities()).summarize.availability, 'unsupported');
  await assert.rejects(empty.prepare('summarize'), /unsupported/);
  for (const state of ['unavailable', 'new-unknown-state']) {
    const m = mocks({ availability: state }); const c = createChromeAiClient({ scope: m.scope });
    await assert.rejects(c.prepare('write'), /is /);
    assert.equal(m.calls.filter(x => x.kind === 'create').length, 0);
  }
  const m = mocks(); m.scope.isSecureContext = false;
  await assert.rejects(createChromeAiClient({ scope: m.scope }).prepare('prompt'), /secure context/);
});

test('downloadable APIs create only in explicit prepare and report native progress', async () => {
  const m = mocks({ availability: 'downloadable' });
  const original = m.scope.Writer.create;
  m.scope.Writer.create = async config => { config.monitor({ addEventListener: (event, listener) => { assert.equal(event, 'downloadprogress'); listener({ loaded: 0.5 }); } }); return original(config); };
  const c = createChromeAiClient({ scope: m.scope }); const progress = [];
  await c.capabilities(); assert.equal(m.calls.filter(x => x.kind === 'create').length, 0);
  await c.prepare('write', {}, { onDownloadProgress: event => progress.push(event) });
  assert.deepEqual(progress, [{ loaded: 0.5, total: 1 }]); c.destroy();
});

test('explicit allowlists reject deprecated or unsupported options and validate language tags', () => {
  assert.throws(() => normalizeCreateOptions('prompt', { temperature: 0.2 }), /Unsupported/);
  assert.throws(() => normalizeCreateOptions('proofread', { includeCorrectionTypes: true }), /Unsupported/);
  assert.throws(() => normalizeCreateOptions('write', { tone: 'funny' }), /Invalid/);
  assert.throws(() => normalizeCreateOptions('translate'), /requires/);
  assert.throws(() => normalizeCreateOptions('detect', { expectedInputLanguages: ['bad language'] }), /language tag/);
  assert.throws(() => normalizeCreateOptions('prompt', { expectedInputs: [{ type: 'audio' }] }), /text descriptors/);
  assert.throws(() => normalizeCreateOptions('__proto__'), /Unknown/);
  assert.equal(normalizeCreateOptions('write').format, 'markdown');
  assert.equal(normalizeCreateOptions('summarize').length, 'medium');
  assert.deepEqual(normalizeCreateOptions('detect', { expectedInputLanguages: ['EN-us'] }), { expectedInputLanguages: ['en-US'] });
});

test('availability excludes creation-only shared context and initial prompts', async () => {
  const m = mocks(); const c = createChromeAiClient({ scope: m.scope });
  await c.prepare('write', { sharedContext: 'context' });
  await c.prepare('prompt', { initialPrompts: [{ role: 'system', content: 'system' }] });
  for (const call of m.calls.filter(x => x.kind === 'availability')) { assert.equal(call.options.sharedContext, undefined); assert.equal(call.options.initialPrompts, undefined); }
  assert.equal(m.calls.find(x => x.kind === 'create' && x.operation === 'write').options.sharedContext, 'context');
  c.destroy();
});

test('native text streams append deltas; nonstreaming APIs reject streaming', async () => {
  const m = mocks(); const c = createChromeAiClient({ scope: m.scope });
  for (const operation of ['summarize', 'prompt', 'write', 'rewrite', 'translate']) {
    await c.prepare(operation, options(operation)); const chunks = [];
    assert.equal(await c.execute(operation, 'input', { onChunk: (delta, full) => chunks.push([delta, full]) }), 'one two');
    assert.deepEqual(chunks, [['one', 'one'], [' two', 'one two']]);
  }
  await assert.rejects(c.execute('proofread', 'text', { onChunk() {} }), /does not support streaming/);
  c.destroy();
});

test('summarizer corpus uses native summarize helper and bounded calls', async () => {
  const m = mocks(); const c = createChromeAiClient({ scope: m.scope });
  await c.prepare('summarize');
  assert.equal(await c.execute('summarize', 'A short corpus.', { corpus: true }), 'summarize:result');
  await assert.rejects(c.execute('write', 'input', { corpus: true }), /only available/);
  c.destroy();
});

test('cancellation releases session and rejects promptly even when native ignores signal', async () => {
  const m = mocks({ overrides: { write: { write: () => new Promise(() => {}) } } });
  const c = createChromeAiClient({ scope: m.scope }); await c.prepare('write');
  const controller = new AbortController();
  const running = c.execute('write', 'input', { signal: controller.signal }); controller.abort();
  await assert.rejects(running, { name: 'AbortError' });
  assert.equal(m.destroyed.length, 1); assert.equal(c.prepared().write, undefined);
});

test('cancelled late creation is reclaimed without publishing a prepared session', async () => {
  let resolve; let destroys = 0;
  const scope = { Writer: { availability: async () => 'available', create: () => new Promise(r => { resolve = r; }) } };
  const c = createChromeAiClient({ scope }); const controller = new AbortController();
  const running = c.prepare('write', {}, { signal: controller.signal });
  await new Promise(r => setImmediate(r)); controller.abort();
  await assert.rejects(running, { name: 'AbortError' });
  resolve({ write() {}, destroy() { destroys++; } }); await new Promise(r => setImmediate(r));
  assert.equal(destroys, 1); assert.deepEqual(c.prepared(), {});
});

test('destroy cancels availability check; consecutive preparation cannot resurrect stale session', async () => {
  const c = createChromeAiClient({ scope: { Writer: { availability: () => new Promise(() => {}), create() { throw new Error('unexpected'); } } } });
  const preparing = c.prepare('write'); c.destroy('write');
  await assert.rejects(preparing, { name: 'AbortError' });
});

test('concurrent requests to one session fail explicitly', async () => {
  let release; const m = mocks({ overrides: { rewrite: { rewrite: () => new Promise(resolve => { release = resolve; }) } } });
  const c = createChromeAiClient({ scope: m.scope }); await c.prepare('rewrite');
  const first = c.execute('rewrite', 'a');
  await assert.rejects(c.execute('rewrite', 'b'), /active request/);
  release('done'); assert.equal(await first, 'done'); c.destroy();
});

test('prompt clones are independent, constraints measured, context overflow rejects', async () => {
  const m = mocks(); let measuredOptions; let prompts = 0; let clonedDestroyed = 0;
  m.scope.LanguageModel.create = async () => ({ prompt() {}, destroy() {}, clone: async () => ({
    contextUsage: 90, contextWindow: 100, destroy() { clonedDestroyed++; },
    measureContextUsage: async (_input, options) => { measuredOptions = options; return 20; },
    prompt: async () => { prompts++; return 'bad'; },
  }) });
  const c = createChromeAiClient({ scope: m.scope }); await c.prepare('prompt');
  await assert.rejects(c.execute('prompt', 'too long', { responseConstraint: { type: 'object' }, omitResponseConstraintInput: false }), /context window/);
  assert.equal(prompts, 0); assert.equal(clonedDestroyed, 1);
  assert.deepEqual(measuredOptions.responseConstraint, { type: 'object' });
  assert.equal(measuredOptions.omitResponseConstraintInput, false);
});

test('prompt contextoverflow event aborts and discards lossy response', async () => {
  const m = mocks(); let listener; let destroys = 0;
  m.scope.LanguageModel.create = async () => ({ prompt() {}, destroy() {}, clone: async () => ({
    addEventListener(type, cb) { assert.equal(type, 'contextoverflow'); listener = cb; }, removeEventListener() {},
    prompt: async () => { listener(); return 'lossy'; }, destroy() { destroys++; },
  }) });
  const c = createChromeAiClient({ scope: m.scope }); await c.prepare('prompt');
  await assert.rejects(c.execute('prompt', 'text'), /overflow/); assert.equal(destroys, 1);
});

test('input bounds and unsupported execution options fail before native work', async () => {
  const c = createChromeAiClient({ scope: mocks().scope, maxInputChars: 5 });
  await assert.rejects(c.execute('write', '123456'), /exceeds/);
  await assert.rejects(c.execute('detect', 'abc', { context: 'xx' }), /does not accept context/);
  await assert.rejects(c.execute('write', 'abc', { madeUp: true }), /Unsupported/);
  await assert.rejects(c.execute('write', 'abc', { responseConstraint: {} }), /only available for prompt/);
});

test('native task quota is respected when browser exposes measurement', async () => {
  let runs = 0;
  const m = mocks({ overrides: { write: { inputQuota: 2, measureInputUsage: async () => 3, write: async () => { runs++; return 'bad'; } } } });
  const c = createChromeAiClient({ scope: m.scope }); await c.prepare('write');
  await assert.rejects(c.execute('write', 'input'), /measured quota/);
  assert.equal(runs, 0); assert.equal(m.destroyed.length, 1);
});

test('invalid native text and structured results are not reported as success', async () => {
  for (const [operation, invalid] of [['write', '  '], ['detect', [{ detectedLanguage: 'en', confidence: 5 }]], ['proofread', { text: 'unexpected' }]]) {
    const m = mocks({ overrides: { [operation]: { [OPERATIONS[operation].method]: async () => invalid } } });
    const c = createChromeAiClient({ scope: m.scope }); await c.prepare(operation);
    await assert.rejects(c.execute(operation, 'input'), /invalid/);
    assert.equal(m.destroyed.length, 1);
  }
});

test('abort between native create resolution and await reclaims resolved session', async () => {
  let destroys = 0; const controller = new AbortController();
  const c = createChromeAiClient({ scope: { Writer: {
    availability: async () => 'available',
    create() {
      queueMicrotask(() => queueMicrotask(() => controller.abort()));
      return Promise.resolve({ write() {}, destroy() { destroys++; } });
    },
  } } });
  await assert.rejects(c.prepare('write', {}, { signal: controller.signal }), { name: 'AbortError' });
  assert.equal(destroys, 1); assert.deepEqual(c.prepared(), {});
});

test('abort between clone resolution and await reclaims resolved clone', async () => {
  let destroys = 0; const controller = new AbortController();
  const c = createChromeAiClient({ scope: { LanguageModel: {
    availability: async () => 'available',
    create: async () => ({ prompt() {}, destroy() {}, clone() {
      queueMicrotask(() => queueMicrotask(() => controller.abort()));
      return Promise.resolve({ prompt() {}, destroy() { destroys++; } });
    } }),
  } } });
  await c.prepare('prompt');
  await assert.rejects(c.execute('prompt', 'input', { signal: controller.signal }), { name: 'AbortError' });
  assert.equal(destroys, 1); assert.deepEqual(c.prepared(), {});
});

test('invalid constraints and signals cannot poison prepared session state', async () => {
  const c = createChromeAiClient({ scope: mocks().scope }); await c.prepare('prompt');
  const circular = {}; circular.self = circular;
  await assert.rejects(c.execute('prompt', 'input', { responseConstraint: circular }), /circular/i);
  await assert.rejects(c.execute('prompt', 'input', { signal: {} }), /AbortSignal/);
  assert.equal(await c.execute('prompt', 'input'), 'prompt:result'); c.destroy();
});
