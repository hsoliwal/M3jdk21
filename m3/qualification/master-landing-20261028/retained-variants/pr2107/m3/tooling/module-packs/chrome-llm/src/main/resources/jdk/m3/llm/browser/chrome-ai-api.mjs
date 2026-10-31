// SPDX-License-Identifier: Apache-2.0
// M3JDK 21 migration source: hsoliwal/com.synexia@2ab439480ecb0643615e85b42def954dbf3b5627
// aix-chrome-ai-studio/mechanical/browser/chrome-ai-api.mjs
// Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Load each optional native task only when selected; Prompt does not parse Summarizer's module.
import { bytes, canonical, limits as normalizeLimits, measure, scope as deadlineScope,
  text as boundedText } from './native-ai-session.mjs';

export const OPERATIONS = Object.freeze({
  summarize: { api: 'Summarizer', method: 'summarize', stream: 'summarizeStreaming' },
  prompt: { api: 'LanguageModel', method: 'prompt', stream: 'promptStreaming' },
  write: { api: 'Writer', method: 'write', stream: 'writeStreaming' },
  rewrite: { api: 'Rewriter', method: 'rewrite', stream: 'rewriteStreaming' },
  proofread: { api: 'Proofreader', method: 'proofread' },
  translate: { api: 'Translator', method: 'translate', stream: 'translateStreaming' },
  detect: { api: 'LanguageDetector', method: 'detect' },
});

const COMMON = ['expectedInputLanguages', 'expectedContextLanguages', 'outputLanguage'];
const KEYS = {
  summarize: ['type', 'format', 'length', 'preference', 'sharedContext', ...COMMON],
  prompt: ['initialPrompts', 'expectedInputs', 'expectedOutputs'],
  write: ['tone', 'format', 'length', 'sharedContext', ...COMMON],
  rewrite: ['tone', 'format', 'length', 'sharedContext', ...COMMON],
  proofread: ['expectedInputLanguages'],
  translate: ['sourceLanguage', 'targetLanguage'],
  detect: ['expectedInputLanguages'],
};
const ENUMS = {
  summarize: { type: ['key-points', 'tldr', 'teaser', 'headline'], format: ['markdown', 'plain-text'], length: ['short', 'medium', 'long'], preference: ['auto', 'speed', 'capability'] },
  write: { tone: ['formal', 'neutral', 'casual'], format: ['markdown', 'plain-text'], length: ['short', 'medium', 'long'] },
  rewrite: { tone: ['as-is', 'more-formal', 'more-casual'], format: ['as-is', 'markdown', 'plain-text'], length: ['as-is', 'shorter', 'longer'] },
};
const states = new Set(['unavailable', 'downloadable', 'downloading', 'available']);
function spec(operation) {
  if (!Object.hasOwn(OPERATIONS, operation)) throw new TypeError(`Unknown operation: ${operation}`);
  return OPERATIONS[operation];
}
function object(value, name) {
  if (!value || typeof value !== 'object' || Array.isArray(value) || ![Object.prototype, null].includes(Object.getPrototypeOf(value))) throw new TypeError(`${name} must be a plain object`);
}
function text(value, name, empty = false) {
  if (typeof value !== 'string' || (!empty && !value.trim())) throw new TypeError(`${name} must be ${empty ? 'a' : 'a non-empty'} string`);
  if (!value.isWellFormed()) throw new TypeError(`${name} must be well-formed Unicode text`);
}
function language(value, name) {
  text(value, name);
  try { return Intl.getCanonicalLocales(value)[0]; } catch { throw new TypeError(`${name} must be a valid language tag`); }
}
function languageList(value, name) {
  if (!Array.isArray(value) || !value.length) throw new TypeError(`${name} must be a non-empty language list`);
  return value.map(item => language(item, name));
}
function copy(value) { return JSON.parse(JSON.stringify(value)); }

/** Explicit native creation options; unknown keys fail instead of being silently ignored. */
export function normalizeCreateOptions(operation, options = {}) {
  spec(operation); object(options, 'options');
  const out = operation === 'summarize' ? { type: 'key-points', format: 'markdown', length: 'medium' } : operation === 'write' ? { tone: 'neutral', format: 'markdown', length: 'medium' } : {};
  for (const [key, value] of Object.entries(options)) {
    if (!KEYS[operation].includes(key)) throw new TypeError(`Unsupported ${operation} option: ${key}`);
    if (ENUMS[operation]?.[key]) {
      if (!ENUMS[operation][key].includes(value)) throw new TypeError(`Invalid ${operation} ${key}`);
      out[key] = value;
    } else if (key.endsWith('Languages')) out[key] = languageList(value, key);
    else if (key.endsWith('Language')) out[key] = language(value, key);
    else if (key === 'sharedContext') { text(value, key); out[key] = value; }
    else if (key.startsWith('includeCorrection')) {
      if (typeof value !== 'boolean') throw new TypeError(`${key} must be boolean`);
      out[key] = value;
    } else if (key === 'initialPrompts') {
      if (!Array.isArray(value)) throw new TypeError('initialPrompts must be an array');
      out[key] = value.map(item => {
        object(item, 'initial prompt');
        if (Object.keys(item).some(k => !['role', 'content'].includes(k)) || !['system', 'user', 'assistant'].includes(item.role)) throw new TypeError('Invalid initial prompt');
        text(item.content, 'initial prompt content');
        return { role: item.role, content: item.content };
      });
    } else if (key === 'expectedInputs' || key === 'expectedOutputs') {
      if (!Array.isArray(value) || !value.length) throw new TypeError(`${key} must be a non-empty array`);
      out[key] = value.map(item => {
        object(item, key);
        // This text-task facade does not promise browser media or tool support.
        if (item.type !== 'text' || Object.keys(item).some(k => !['type', 'languages'].includes(k))) throw new TypeError(`${key} supports text descriptors only`);
        return item.languages === undefined ? { type: 'text' } : { type: 'text', languages: languageList(item.languages, key) };
      });
    }
  }
  if (operation === 'translate' && (!out.sourceLanguage || !out.targetLanguage)) throw new TypeError('translate requires sourceLanguage and targetLanguage');
  return out;
}

function abortError() { return new DOMException('Operation aborted', 'AbortError'); }
function validateSignal(signal) {
  if (signal !== undefined && (!signal || typeof signal.aborted !== 'boolean' || typeof signal.addEventListener !== 'function' || typeof signal.removeEventListener !== 'function')) throw new TypeError('signal must be an AbortSignal');
}
function checkAbort(signal) { if (signal?.aborted) throw signal.reason ?? abortError(); }
function destroySession(session) {
  try { if (typeof session?.destroy === 'function') session.destroy(); }
  catch { /* Cleanup must not hide the operation's failure or resurrect closed state. */ }
}
function validateResult(operation, result) {
  if (operation === 'proofread') {
    if (!result || typeof result.correctedInput !== 'string' || !Array.isArray(result.corrections)) throw new TypeError('Proofreader returned an invalid result');
  } else if (operation === 'detect') {
    if (!Array.isArray(result) || result.some(item => !item || typeof item.detectedLanguage !== 'string' || !Number.isFinite(item.confidence) || item.confidence < 0 || item.confidence > 1)) throw new TypeError('LanguageDetector returned an invalid result');
  } else if (typeof result !== 'string' || !result.trim()) throw new TypeError(`${spec(operation).api} returned empty or invalid text`);
  canonical(result); // Reject malformed Unicode, non-finite numbers, and non-JSON structured fields.
}
function availabilityOptions(options) {
  const { sharedContext, initialPrompts, ...core } = options;
  return core;
}
function abortable(promise, signal) {
  return new Promise((resolve, reject) => {
    const abort = () => reject(signal.reason ?? abortError());
    if (signal.aborted) abort(); else signal.addEventListener('abort', abort, { once: true });
    Promise.resolve(promise).then(resolve, reject).finally(() => signal.removeEventListener('abort', abort));
  });
}

export function createChromeAiClient({ scope = globalThis, maxInputChars = 1_000_000, limits: requestedLimits = {} } = {}) {
  if (!Number.isSafeInteger(maxInputChars) || maxInputChars < 1) throw new RangeError('maxInputChars must be a positive integer');
  const bounds = normalizeLimits(requestedLimits);
  const sessions = new Map();
  const preparations = new Map();
  function nativeApi(operation) {
    const api = scope[spec(operation).api];
    if (scope.isSecureContext === false) throw new Error('Chrome built-in AI requires a secure context');
    if (!api || typeof api.availability !== 'function' || typeof api.create !== 'function') throw new Error(`${spec(operation).api} is unsupported in this browser context`);
    return api;
  }
  async function capabilities(optionsByOperation = {}) {
    object(optionsByOperation, 'optionsByOperation');
    return Object.fromEntries(await Promise.all(Object.entries(OPERATIONS).map(async ([operation, info]) => {
      const state = { api: info.api, supported: false, availability: 'unsupported', prepared: sessions.has(operation) };
      const work = deadlineScope(Math.min(10000, bounds.initializeTimeoutMs));
      try {
        const api = nativeApi(operation); state.supported = true;
        if (operation === 'translate' && !optionsByOperation[operation]) state.availability = 'requires-options';
        else {
          const options = normalizeCreateOptions(operation, optionsByOperation[operation] ?? {});
          const available = await abortable(api.availability(availabilityOptions(options)), work.signal);
          state.availability = states.has(available) ? available : 'unknown';
        }
      } catch (error) { state.error = String(error.message ?? error); }
      finally { work.dispose(); }
      return [operation, state];
    })));
  }
  async function prepare(operation, createOptions = {}, { signal, onDownloadProgress } = {}) {
    const api = nativeApi(operation);
    const options = normalizeCreateOptions(operation, createOptions);
    if (onDownloadProgress !== undefined && typeof onDownloadProgress !== 'function') throw new TypeError('onDownloadProgress must be a function');
    validateSignal(signal);
    checkAbort(signal);
    destroy(operation);
    const work = deadlineScope(bounds.initializeTimeoutMs, [signal]);
    const controller = work.controller;
    const unlink = () => work.dispose();
    preparations.set(operation, controller);
    let session;
    try {
      const available = await abortable(api.availability(availabilityOptions(options)), controller.signal);
      checkAbort(controller.signal);
      if (!states.has(available) || available === 'unavailable') throw new Error(`${spec(operation).api} is ${available}`);
      const monitor = onDownloadProgress ? m => m.addEventListener('downloadprogress', event => onDownloadProgress({ loaded: event.loaded, total: event.total ?? 1 })) : undefined;
      const nativeOptions = { ...options, signal: controller.signal, ...(monitor ? { monitor } : {}) };
      const creating = Promise.resolve(operation === 'summarize' ? (await import('./chrome-native-summarizer.mjs')).createChromeAiSummarizer({ api, ...nativeOptions,
        limits: bounds, userActivation: () => scope.navigator?.userActivation?.isActive === true }) : api.create(nativeOptions));
      session = await abortable(creating.then(created => {
        session = created;
        if (controller.signal.aborted) { destroySession(created); session = undefined; throw controller.signal.reason ?? abortError(); }
        return created;
      }), controller.signal);
      checkAbort(controller.signal);
      if (typeof session?.[spec(operation).method] !== 'function') throw new Error(`${spec(operation).api} returned an invalid session`);
      if (operation === 'prompt' && typeof session.clone !== 'function') throw new Error('LanguageModel clone is required for independent jobs');
      sessions.set(operation, { session, options, busy: false, controller: null });
      return { operation, options: copy(options), availability: available };
    } catch (error) { destroySession(session); throw error; }
    finally { unlink(); if (preparations.get(operation) === controller) preparations.delete(operation); }
  }
  async function execute(operation, input, { context, signal, onChunk, corpus = false, onProgress, responseConstraint, omitResponseConstraintInput, ...limits } = {}) {
    const info = spec(operation);
    text(input, 'input');
    if (input.length > maxInputChars) throw new RangeError(`input exceeds ${maxInputChars} characters`);
    boundedText(input, 'input', bounds.maxInputBytes);
    if (context !== undefined) text(context, 'context', true);
    if (context?.length > maxInputChars) throw new RangeError('context exceeds input limit');
    if (context !== undefined) boundedText(context, 'context', bounds.maxInputBytes, true);
    if (context && !['summarize', 'write', 'rewrite'].includes(operation)) throw new TypeError(`${operation} does not accept context; configure Prompt initialPrompts during preparation`);
    if (typeof corpus !== 'boolean' || (corpus && operation !== 'summarize')) throw new TypeError('corpus is only available for summarize');
    if (onChunk !== undefined && typeof onChunk !== 'function') throw new TypeError('onChunk must be a function');
    if (onChunk && !info.stream && !corpus) throw new TypeError(`${operation} does not support streaming`);
    if ((responseConstraint !== undefined || omitResponseConstraintInput !== undefined) && operation !== 'prompt') throw new TypeError('Response constraints are only available for prompt');
    if (responseConstraint !== undefined) object(responseConstraint, 'responseConstraint');
    if (omitResponseConstraintInput !== undefined && typeof omitResponseConstraintInput !== 'boolean') throw new TypeError('omitResponseConstraintInput must be boolean');
    const constraint = responseConstraint === undefined ? undefined : copy(responseConstraint);
    const corpusKeys = ['maxChars', 'maxRounds', 'maxCalls', 'maxChunks', 'maxMeasurements'];
    if (Object.keys(limits).some(k => !corpus || !corpusKeys.includes(k))) throw new TypeError('Unsupported execution option');
    validateSignal(signal); checkAbort(signal);
    const record = sessions.get(operation);
    if (!record) throw new Error(`${operation} is not prepared; click Prepare first`);
    if (record.busy) throw new Error(`${operation} already has an active request`);
    record.busy = true;
    const work = deadlineScope(bounds.timeoutMs, [signal]);
    const controller = work.controller; record.controller = controller;
    const unlink = () => work.dispose();
    const request = { signal: controller.signal, ...(context ? { context } : {}), ...(constraint === undefined ? {} : { responseConstraint: constraint }), ...(omitResponseConstraintInput === undefined ? {} : { omitResponseConstraintInput }) };
    let session = record.session;
    const overflow = () => controller.abort(new RangeError('Prompt context overflow would discard history; shorten the request'));
    try {
      if (operation === 'prompt') {
        const cloning = Promise.resolve(record.session.clone({ signal: controller.signal })).then(cloned => {
          session = cloned;
          if (controller.signal.aborted) { destroySession(cloned); session = record.session; throw controller.signal.reason ?? abortError(); }
          return cloned;
        });
        session = await abortable(cloning, controller.signal);
        if (typeof session?.prompt !== 'function') throw new Error('LanguageModel returned an invalid cloned session');
        session.addEventListener?.('contextoverflow', overflow);
        if (typeof session.measureContextUsage === 'function') {
          const measured = await abortable(session.measureContextUsage(input, request), controller.signal);
          if (!Number.isFinite(measured) || measured < 0) throw new RangeError('Invalid native context usage');
          for (const value of [session.contextUsage, session.contextWindow]) {
            if (value !== undefined && (!Number.isFinite(value) || value < 0)) throw new RangeError('Invalid native context quota');
          }
          if (Number.isFinite(measured) && Number.isFinite(session.contextUsage) && Number.isFinite(session.contextWindow) && measured + session.contextUsage > session.contextWindow) throw new RangeError('Prompt exceeds the measured context window; shorten the request');
        }
      } else if (!corpus && operation !== 'summarize' && typeof session.measureInputUsage === 'function') {
        const quota = await measure(session, input, request, controller.signal);
        if (quota && !quota.fits) throw new RangeError(`${info.api} input exceeds its measured quota`);
      }
      let result;
      if (corpus) result = await abortable(session.summarizeCorpus(input, { ...limits, ...request, onProgress }), controller.signal);
      else if (onChunk) {
        if (typeof session[info.stream] !== 'function') throw new Error(`${info.api} streaming is unsupported`);
        let output = '', fragments = 0, outputBytes = 0, finished = false;
        const stream = session[info.stream](input, request);
        const iterator = stream[Symbol.asyncIterator]();
        try { while (true) {
          const next = await abortable(iterator.next(), controller.signal);
          if (next.done) { finished = true; break; }
          const chunk = next.value;
          checkAbort(controller.signal);
          if (typeof chunk !== 'string') throw new TypeError('Native stream returned a non-text chunk');
          if (++fragments > 8192) throw new RangeError('Native stream fragment limit exceeded');
          boundedText(chunk, 'stream fragment', bounds.maxOutputBytes, true);
          outputBytes += bytes(chunk);
          if (outputBytes > bounds.maxOutputBytes) throw new RangeError('Native stream exceeds byte budget');
          output += chunk;
          if (output.length > maxInputChars * 4) throw new RangeError('Native response exceeds output limit');
          await abortable(onChunk(chunk, output), controller.signal);
        } } finally {
          if (!finished) {
            try { Promise.resolve(iterator.return?.()).catch(() => {}); } catch { /* best effort native cleanup */ }
          }
        }
        result = output;
      } else result = await abortable(session[info.method](input, request), controller.signal);
      checkAbort(controller.signal);
      validateResult(operation, result);
      boundedText(typeof result === 'string' ? result : canonical(result), 'native output', bounds.maxOutputBytes, true);
      if (JSON.stringify(result)?.length > maxInputChars * 4) throw new RangeError('Native response exceeds output limit');
      if (operation === 'prompt') record.lastExecution = { contextUsage: session.contextUsage ?? null, contextWindow: session.contextWindow ?? null };
      return result;
    } catch (error) {
      if (sessions.get(operation) === record) destroy(operation);
      throw error;
    } finally { if (operation === 'prompt' && session !== record.session) { session?.removeEventListener?.('contextoverflow', overflow); destroySession(session); } unlink(); record.busy = false; record.controller = null; }
  }
  function destroy(operation) {
    const operations = operation === undefined ? new Set([...sessions.keys(), ...preparations.keys()]) : [operation];
    for (const name of operations) {
      spec(name);
      preparations.get(name)?.abort(abortError()); preparations.delete(name);
      const record = sessions.get(name); sessions.delete(name);
      record?.controller?.abort(abortError());
      destroySession(record?.session);
    }
  }
  function prepared() { return Object.fromEntries([...sessions].map(([op, record]) => [op, copy(record.options)])); }
  async function contextUsage(input) {
    const record = sessions.get('prompt');
    if (!record) throw new Error('prompt is not prepared');
    if (input !== undefined) {
      text(input, 'input');
      if (input.length > maxInputChars) throw new RangeError(`input exceeds ${maxInputChars} characters`);
      boundedText(input, 'input', bounds.maxInputBytes);
    }
    if (record.busy) throw new Error('prompt already has an active request');
    const work = deadlineScope(bounds.timeoutMs); record.busy = true; record.controller = work.controller;
    try {
      const session = record.session;
      const usage = { contextUsage: session.contextUsage ?? null, contextWindow: session.contextWindow ?? null };
      if (record.lastExecution) usage.lastExecution = { ...record.lastExecution };
      if (input !== undefined) {
        usage.inputUsage = typeof session.measureContextUsage === 'function'
          ? await abortable(session.measureContextUsage(input, { signal: work.signal }), work.signal) : null;
        if (usage.inputUsage !== null && (!Number.isFinite(usage.inputUsage) || usage.inputUsage < 0)) throw new RangeError('Invalid native context usage');
      }
      checkAbort(work.signal); return usage;
    } catch (error) { if (sessions.get('prompt') === record) destroy('prompt'); throw error; }
    finally { work.dispose(); record.busy = false; record.controller = null; }
  }
  return Object.freeze({ capabilities, prepare, execute, destroy, prepared, contextUsage });
}

