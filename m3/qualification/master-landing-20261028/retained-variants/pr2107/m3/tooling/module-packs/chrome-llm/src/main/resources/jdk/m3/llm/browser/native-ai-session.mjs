// SPDX-License-Identifier: Apache-2.0
// M3JDK 21 migration source: hsoliwal/com.synexia@2ab439480ecb0643615e85b42def954dbf3b5627
// aix-chrome-ai-studio/mechanical/browser/native-ai-session.mjs
/** Shared lifecycle only. All language transformations execute in Chrome's native API. */
export class NativeAiError extends Error {
  constructor(code, message = code) { super(message); this.name = 'NativeAiError'; this.code = code; }
}
export const DEFAULT_LIMITS = Object.freeze({
  maxInputBytes: 4 * 1024 * 1024, maxOutputBytes: 64 * 1024,
  timeoutMs: 120000, initializeTimeoutMs: 600000,
});
const encoder = new TextEncoder();
export function fail(code, message) { throw new NativeAiError(code, message); }
export function text(value, name = 'text', maximum = DEFAULT_LIMITS.maxInputBytes, empty = false) {
  if (typeof value !== 'string' || !value.isWellFormed()) fail('INVALID_TEXT', `${name} must be well-formed Unicode text`);
  if (!empty && !value.trim()) fail('EMPTY_TEXT', `${name} must not be empty`);
  if (encoder.encode(value).length > maximum) fail('BYTE_LIMIT', `${name} exceeds its byte budget`);
  return value;
}
export function bytes(value) { return encoder.encode(value).length; }
export function keys(value, allowed, label = 'options') {
  if (!value || Object.getPrototypeOf(value) !== Object.prototype) fail('INVALID_OPTIONS', `${label} must be a plain object`);
  for (const key of Object.keys(value)) if (!allowed.includes(key)) fail('UNSUPPORTED_OPTION', `Unsupported ${label}: ${key}`);
}
export function enumValue(value, allowed, label) {
  if (!allowed.includes(value)) fail('INVALID_OPTIONS', `Invalid ${label}`);
  return value;
}
export function language(value) {
  text(value, 'language', 64);
  try { return Intl.getCanonicalLocales(value)[0]; }
  catch { return fail('INVALID_LANGUAGE', 'Use a BCP 47 language tag'); }
}
export function languages(value) {
  if (!Array.isArray(value) || value.length < 1 || value.length > 16) fail('INVALID_LANGUAGE', 'Supply 1..16 language tags');
  return Object.freeze([...new Set(value.map(language))]);
}
export function limits(value = {}) {
  keys(value, Object.keys(DEFAULT_LIMITS), 'limits');
  const result = { ...DEFAULT_LIMITS, ...value };
  for (const [key, number] of Object.entries(result)) {
    if (!Number.isSafeInteger(number) || number < 1 || number > DEFAULT_LIMITS[key]) fail('INVALID_LIMIT', `Invalid ${key}`);
  }
  return Object.freeze(result);
}
export function canonical(value, depth = 0) {
  if (depth > 32) fail('DEPTH_LIMIT');
  if (typeof value === 'string') text(value, 'JSON text', 16 * 1024 * 1024, true);
  if (value === null || typeof value === 'string' || typeof value === 'boolean') return JSON.stringify(value);
  if (typeof value === 'number' && Number.isFinite(value)) return JSON.stringify(value);
  if (Array.isArray(value)) return `[${value.map(item => canonical(item, depth + 1)).join(',')}]`;
  if (value && Object.getPrototypeOf(value) === Object.prototype) return `{${Object.keys(value).sort()
    .filter(key => value[key] !== undefined).map(key => `${canonical(key, depth + 1)}:${canonical(value[key], depth + 1)}`).join(',')}}`;
  return fail('INVALID_RESULT', 'Only JSON-compatible native results can be recorded');
}
export async function sha256(value) {
  text(value, 'hash input', 16 * 1024 * 1024, true);
  const digest = await globalThis.crypto.subtle.digest('SHA-256', encoder.encode(value));
  return [...new Uint8Array(digest)].map(n => n.toString(16).padStart(2, '0')).join('');
}
export function check(signal) { if (signal?.aborted) throw signal.reason ?? new DOMException('Aborted', 'AbortError'); }
export function scope(milliseconds, signals = []) {
  const parents = signals.filter(Boolean);
  if (parents.some(signal => typeof signal.aborted !== 'boolean' || typeof signal.addEventListener !== 'function' || typeof signal.removeEventListener !== 'function')) fail('INVALID_SIGNAL');
  const controller = new AbortController();
  const listeners = [];
  for (const signal of parents) {
    const abort = () => controller.abort(signal.reason ?? new DOMException('Aborted', 'AbortError'));
    if (signal.aborted) abort();
    else { signal.addEventListener('abort', abort, { once: true }); listeners.push([signal, abort]); }
  }
  const timer = setTimeout(() => controller.abort(new NativeAiError('DEADLINE', 'Native API deadline exceeded')), milliseconds);
  return { controller, signal: controller.signal, dispose() {
    clearTimeout(timer); for (const [signal, listener] of listeners) signal.removeEventListener('abort', listener);
  } };
}
export function wait(promise, signal) {
  // Attach a handler even when already aborted: late native rejections must not be unhandled.
  return new Promise((resolve, reject) => {
    const aborted = () => { signal.removeEventListener('abort', aborted); reject(signal.reason); };
    if (signal.aborted) aborted(); else signal.addEventListener('abort', aborted, { once: true });
    Promise.resolve(promise).then(value => {
      signal.removeEventListener('abort', aborted);
      if (signal.aborted) reject(signal.reason); else resolve(value);
    }, error => { signal.removeEventListener('abort', aborted); reject(error); });
  });
}
export async function availabilityOf(api, options, timeoutMs = 10000) {
  if (!api || typeof api.availability !== 'function' || typeof api.create !== 'function') return 'unavailable';
  const work = scope(timeoutMs);
  try {
    const state = await wait(api.availability(availabilityOptions(options)), work.signal);
    return enumValue(state, ['available', 'downloadable', 'downloading', 'unavailable'], 'availability state');
  } finally { work.dispose(); }
}
function availabilityOptions(options = {}) {
  const { sharedContext, initialPrompts, ...core } = options;
  return core;
}
export async function openSession(api, options, { signal, monitor, onDownload = () => {},
  userActivation = () => globalThis.navigator?.userActivation?.isActive === true, limits: requestedLimits = {} } = {}) {
  const bounds = limits(requestedLimits);
  if (!api || typeof api.availability !== 'function' || typeof api.create !== 'function') fail('UNAVAILABLE', 'Native API is unavailable in this document');
  if (monitor !== undefined && typeof monitor !== 'function' || typeof onDownload !== 'function' || typeof userActivation !== 'function') fail('INVALID_CALLBACK');
  const work = scope(bounds.initializeTimeoutMs, [signal]);
  let session, initializing = true;
  const release = value => { try { value?.destroy?.(); } catch { /* native cleanup is best effort */ } };
  try {
    check(work.signal);
    const state = await wait(api.availability(availabilityOptions(options)), work.signal);
    if (!['available', 'downloadable', 'downloading'].includes(state)) fail('UNAVAILABLE', 'Native API reports unavailable');
    // Read actual activation after availability; a saved boolean is not a browser gesture.
    if (state !== 'available' && !userActivation()) fail('ACTIVATION_REQUIRED', 'Click Initialize to authorize the browser model download');
    const pending = Promise.resolve(api.create({ ...options, signal: work.signal, monitor(m) {
      try {
        m.addEventListener('downloadprogress', event => {
          if (!initializing || work.signal.aborted) return;
          try { onDownload(Number.isFinite(event.loaded) ? Math.max(0, Math.min(1, event.loaded)) : null); }
          catch (error) { work.controller.abort(error); }
        });
        monitor?.(m);
      } catch (error) { work.controller.abort(error); }
    } }));
    pending.then(value => { if (work.signal.aborted) release(value); }, () => {});
    session = await wait(pending, work.signal);
    if (!session || typeof session.destroy !== 'function') fail('INVALID_SESSION');
    check(work.signal);
    return { session, state, guard: new SessionGuard(session, bounds, signal) };
  } catch (error) { release(session); throw error; }
  finally { initializing = false; work.dispose(); }
}
export class SessionGuard {
  #session; #lifetime = new AbortController(); #busy = false; #closed = false; #parent; #onParent;
  constructor(session, bounds, parent) {
    this.#session = session; this.bounds = bounds; this.#parent = parent;
    this.#onParent = () => this.destroy();
    if (parent?.aborted) this.destroy(); else parent?.addEventListener('abort', this.#onParent, { once: true });
  }
  get closed() { return this.#closed; }
  #enter(signal) {
    check(signal);
    if (this.#closed) fail('CLOSED', 'Initialize a fresh native session');
    if (this.#busy) fail('BUSY', 'One operation per native session');
    const work = scope(this.bounds.timeoutMs, [this.#lifetime.signal, signal]);
    this.#busy = true;
    return work;
  }
  async run(action, signal) {
    const work = this.#enter(signal);
    try { check(work.signal); return await wait(action(work.signal), work.signal); }
    catch (error) { this.destroy(); throw error; }
    finally { work.dispose(); this.#busy = false; }
  }
  async *stream(action, signal) {
    const work = this.#enter(signal);
    let iterator, finished = false, total = 0, chunks = 0, meaningful = false;
    try {
      check(work.signal);
      const stream = await wait(action(work.signal), work.signal);
      if (!stream?.[Symbol.asyncIterator]) fail('STREAM_UNAVAILABLE');
      iterator = stream[Symbol.asyncIterator]();
      while (true) {
        const next = await wait(iterator.next(), work.signal);
        if (next.done) {
          if (!meaningful) fail('EMPTY_TEXT', 'Native stream produced no text');
          finished = true; break;
        }
        if (++chunks > 8192) fail('STREAM_LIMIT');
        text(next.value, 'stream fragment', this.bounds.maxOutputBytes, true);
        meaningful ||= next.value.trim().length > 0;
        total += bytes(next.value);
        if (total > this.bounds.maxOutputBytes) fail('BYTE_LIMIT', 'Stream output exceeds byte budget');
        yield next.value; // Provisional. Native end-of-stream is required before caller publishes a result.
      }
    } catch (error) { this.destroy(); throw error; }
    finally {
      if (!finished) {
        this.destroy();
        try { Promise.resolve(iterator?.return?.()).catch(() => {}); } catch { /* already closed */ }
      }
      work.dispose(); this.#busy = false;
    }
  }
  destroy() {
    if (this.#closed) return;
    this.#closed = true; this.#lifetime.abort(new NativeAiError('CLOSED'));
    this.#parent?.removeEventListener('abort', this.#onParent);
    try { this.#session.destroy(); } catch { /* closed status must remain authoritative */ }
  }
}
/** A measurement is never estimated from characters. Native quota errors propagate. */
export async function measure(session, input, request, signal) {
  check(signal);
  if (typeof session.measureInputUsage !== 'function' || session.inputQuota === undefined) return null;
  const quota = session.inputQuota;
  if (typeof quota !== 'number' || Number.isNaN(quota) || quota < 0) fail('INVALID_QUOTA');
  const usage = await wait(session.measureInputUsage(input, { ...request, signal }), signal);
  if (typeof usage !== 'number' || !Number.isFinite(usage) || usage < 0) fail('INVALID_QUOTA');
  return { usage, quota, fits: usage <= quota };
}
export async function receipt(api, input, output, options, request = {}, extra = {}) {
  const value = typeof output === 'string' ? output : canonical(output);
  return deepFreeze({ ...extra, schema: 'aix.native-ai.candidate.v1', api, authority: 'CANDIDATE_ONLY',
    promotable: false, claims: [], sourceSha256: await sha256(input), outputSha256: await sha256(value),
    optionsSha256: await sha256(canonical(options)), requestSha256: await sha256(canonical(request)),
    sourceBytes: bytes(input), outputBytes: bytes(value), gpuExecution: 'UNKNOWN',
    modelIdentity: 'NOT_EXPOSED' });
}

function deepFreeze(value) {
  if (value && typeof value === 'object') { Object.values(value).forEach(deepFreeze); Object.freeze(value); }
  return value;
}
