// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
// Synexia-owned reviewed bridge template for ChromeProtocol/ChromeSummarizerPort and
// aix-chrome-ai-studio/mechanical/browser/chrome-ai-api.mjs at 2ab439480ecb0643615e85b42def954dbf3b5627.
import { createChromeAiClient, normalizeCreateOptions } from './chrome-ai-api.mjs';

export const PROTOCOL = 'M3_CHROME_BRIDGE_V1';
export const DEFAULT_LIMITS = Object.freeze({ maxInputBytes: 4 * 1024 * 1024,
  maxOutputBytes: 64 * 1024, maxSourceRefs: 4096, maxMetadataBytes: 64 * 1024, maxModelCalls: 4096 });
const encoder = new TextEncoder();
const REQUEST_KEYS = ['requestId', 'nodeId', 'input', 'inputSha256', 'sourceRefs',
  'authority', 'candidateOnly', 'modelCalls', 'directNetworkCalls'];
const BASE_KEYS = ['protocol', 'action', 'operation', 'sessionId', 'sequence'];
const javaWhitespace = /^[\u0009-\u000d\u001c-\u0020\u1680\u2000-\u2006\u2008-\u200a\u2028\u2029\u205f\u3000]*$/u;

function object(value) {
  if (!value || Object.getPrototypeOf(value) !== Object.prototype) throw new TypeError('Expected a plain packet object');
}
function exact(value, keys) {
  object(value);
  if (Object.keys(value).length !== keys.length || keys.some(key => !Object.hasOwn(value, key))) throw new TypeError('Unknown or missing packet fields');
}
function unicode(value, name = 'text') {
  if (typeof value !== 'string' || !value.isWellFormed()) throw new TypeError(`${name} must be well-formed Unicode`);
  return value;
}
function bytes(value, maximum, name) {
  unicode(value, name);
  if (value.length > maximum) throw new RangeError(`${name} byte budget exceeded`);
  const size = encoder.encode(value).length;
  if (size > maximum) throw new RangeError(`${name} byte budget exceeded`);
  return size;
}
function integer(value, maximum, name, minimum = 1) {
  if (!Number.isSafeInteger(value) || value < minimum || value > maximum) throw new RangeError(`Invalid ${name}`);
  return value;
}
function operation(value) {
  if (value !== 'summarize' && value !== 'prompt') throw new TypeError('Unsupported browser operation');
  return value;
}
function sessionId(value) {
  if (typeof value !== 'string' || !/^[A-Za-z0-9._-]{1,128}$/.test(value)) throw new TypeError('Invalid session identifier');
  return value;
}
function hashValue(value) {
  if (typeof value !== 'string' || !/^[0-9a-f]{64}$/.test(value)) throw new TypeError('Invalid SHA-256 identity');
  return value;
}
function bounds(value = {}, ceiling = DEFAULT_LIMITS, complete = false) {
  object(value);
  if (Object.keys(value).some(key => !Object.hasOwn(DEFAULT_LIMITS, key))) throw new TypeError('Unknown limit');
  if (complete) exact(value, Object.keys(DEFAULT_LIMITS));
  const out = { ...ceiling, ...value };
  for (const key of Object.keys(DEFAULT_LIMITS)) integer(out[key], ceiling[key], key);
  return Object.freeze(out);
}
function packetCharacters(limits) {
  return 8192 + 6 * (limits.maxInputBytes + limits.maxOutputBytes + limits.maxMetadataBytes) + 3 * limits.maxSourceRefs;
}

/** Compact ASCII JSON compatible with the existing Java internal.Json writer. */
export function encodePacket(value) {
  return JSON.stringify(value).replace(/[\u007f-\uffff]/g,
    codeUnit => `\\u${codeUnit.charCodeAt(0).toString(16).padStart(4, '0')}`);
}
function decodePacket(wire, maximum) {
  if (typeof wire !== 'string' || wire.length > maximum) throw new RangeError('Packet budget exceeded');
  const value = JSON.parse(wire);
  // Also refuses duplicate keys, alternative escapes, fractions/exponents and padding.
  if (wire !== encodePacket(value)) throw new TypeError('Noncanonical packet JSON');
  return value;
}
async function sha256(value) {
  const digest = await globalThis.crypto.subtle.digest('SHA-256', value);
  return [...new Uint8Array(digest)].map(byte => byte.toString(16).padStart(2, '0')).join('');
}
async function framedHash(domain, ...fields) {
  const encoded = [domain, ...fields].map(value => encoder.encode(unicode(String(value))));
  const size = encoded.reduce((total, field) => total + 4 + field.length, 0);
  const packet = new Uint8Array(size), view = new DataView(packet.buffer);
  let offset = 0;
  for (const field of encoded) {
    view.setUint32(offset, field.length, false); offset += 4;
    packet.set(field, offset); offset += field.length;
  }
  return sha256(packet);
}
function javaList(refs) { return `[${refs.join(', ')}]`; }
function metadata(nodeId, refs, limits) {
  if (!Array.isArray(refs) || refs.length > limits.maxSourceRefs) throw new RangeError('Source reference budget exceeded');
  let used = bytes(nodeId, limits.maxMetadataBytes, 'nodeId');
  for (const ref of refs) {
    used += bytes(ref, limits.maxMetadataBytes, 'source reference');
    if (used > limits.maxMetadataBytes) throw new RangeError('Metadata byte budget exceeded');
  }
}

/** The legacy Java identity is preserved; packet identities additionally frame each reference. */
export async function makeChromeRequest(nodeId, input, sourceRefs = []) {
  bytes(input, DEFAULT_LIMITS.maxInputBytes, 'input');
  metadata(nodeId, sourceRefs, DEFAULT_LIMITS);
  const refs = [...new Set(sourceRefs)].sort();
  const inputSha256 = await sha256(encoder.encode(input));
  const requestId = await framedHash('CPULLM_JAVA_CHROME_REQUEST_V1', nodeId, inputSha256, javaList(refs));
  return Object.freeze({ requestId, nodeId, input, inputSha256, sourceRefs: Object.freeze(refs),
    authority: 'CANDIDATE_ONLY', candidateOnly: true, modelCalls: 0, directNetworkCalls: 0 });
}
async function validateRequest(request, limits) {
  exact(request, REQUEST_KEYS);
  bytes(request.input, limits.maxInputBytes, 'input');
  if (javaWhitespace.test(request.input) || !request.input.trim()) throw new TypeError('Input must not be blank');
  metadata(request.nodeId, request.sourceRefs, limits);
  const expected = await makeChromeRequest(request.nodeId, request.input, request.sourceRefs);
  for (const key of REQUEST_KEYS) {
    const same = key === 'sourceRefs' ? request.sourceRefs.length === expected.sourceRefs.length
      && request.sourceRefs.every((ref, index) => ref === expected.sourceRefs[index]) : request[key] === expected[key];
    if (!same) throw new TypeError('Request identity or authority mismatch');
  }
}
export async function requestPacketId(op, id, sequence, request) {
  operation(op); sessionId(id); integer(sequence, 4096, 'sequence');
  return framedHash('M3_CHROME_BRIDGE_REQUEST_V1', op, id, sequence,
    request.requestId, request.nodeId, request.inputSha256, request.sourceRefs.length, ...request.sourceRefs,
    request.authority, request.candidateOnly, request.modelCalls, request.directNetworkCalls);
}
export async function resultPacketId(op, id, sequence, requestRoot, result) {
  operation(op); sessionId(id); integer(sequence, 4096, 'sequence'); hashValue(requestRoot);
  return framedHash('M3_CHROME_BRIDGE_RESULT_V1', op, id, sequence, requestRoot, result.resultId,
    result.status, result.sourceRequestId, result.sourceRefs.length, ...result.sourceRefs,
    result.text, result.authority, result.candidateOnly, result.admitted, result.modelCalls, result.directNetworkCalls);
}
async function candidate(request, output) {
  const result = { status: 'CANDIDATE_ONLY', resultId: '', sourceRequestId: request.requestId,
    sourceRefs: [...request.sourceRefs], text: output, authority: 'MODEL_CANDIDATE',
    candidateOnly: true, admitted: false, modelCalls: 1, directNetworkCalls: 0 };
  result.resultId = await framedHash('CPULLM_JAVA_CHROME_RESULT_V1', result.status, result.sourceRequestId,
    javaList(result.sourceRefs), result.text, result.authority, result.candidateOnly, result.admitted,
    result.modelCalls, result.directNetworkCalls);
  return result;
}
function base(action, op, id, sequence) { return { protocol: PROTOCOL, action, operation: op, sessionId: id, sequence }; }
function checkAbort(signal) { if (signal?.aborted) throw signal.reason ?? new DOMException('Aborted', 'AbortError'); }

/** No browser/model work occurs until a caller invokes prepare from a user gesture. */
export function createChromeProtocolBridge({ scope = globalThis, limits: requested = {} } = {}) {
  const limits = bounds(requested);
  let busy = false, preparing = null, readyOperation = null, attached = null, currentTask = null;
  const nativeSessions = new Set();
  function gesture() {
    if (scope.isSecureContext !== true) throw new Error('A secure browser context is required');
    if (scope.navigator?.userActivation?.isActive !== true) throw new Error('Prepare must run from a current browser user gesture');
  }
  async function serial(action) {
    if (busy) throw new Error('One browser bridge operation at a time');
    busy = true;
    try { return await action(); } finally { busy = false; }
  }
  function wrapSession(op, raw) {
    const method = op === 'summarize' ? 'summarize' : 'prompt';
    if (!raw || typeof raw.destroy !== 'function' || typeof raw[method] !== 'function'
        || (op === 'prompt' && typeof raw.clone !== 'function')) throw new Error('Invalid native session');
    let destroyed = false;
    const wrapped = new Proxy(raw, { get(target, key) {
      if (key === 'destroy') return () => {
        if (destroyed) return;
        destroyed = true; nativeSessions.delete(wrapped); target.destroy();
      };
      if (key === 'clone' && op === 'prompt') return async options => {
        if (destroyed) throw new Error('Native session is closed');
        const clone = await target.clone(options);
        try { return wrapSession(op, clone); }
        catch (error) { clone?.destroy?.(); throw error; }
      };
      if (key === method) return async (input, options = {}) => {
        const task = currentTask;
        if (destroyed || !task || !attached || attached.failed) throw new Error('No active native job');
        checkAbort(options.signal);
        let measured, quota, used = 0;
        if (op === 'summarize') {
          if (typeof target.measureInputUsage !== 'function') throw new Error('Native input quota measurement is required');
          quota = target.inputQuota;
          measured = await target.measureInputUsage(input, options);
        } else {
          if (typeof target.measureContextUsage !== 'function') throw new Error('Native context measurement is required');
          quota = target.contextWindow; used = target.contextUsage;
          measured = await target.measureContextUsage(input, options);
        }
        checkAbort(options.signal);
        if (destroyed || task !== currentTask || !attached || attached.failed) throw new Error('Native job was cancelled');
        if (typeof quota !== 'number' || !(quota > 0) || (!Number.isFinite(quota) && quota !== Infinity)
            || typeof measured !== 'number' || !Number.isFinite(measured) || measured < 0
            || typeof used !== 'number' || !Number.isFinite(used) || used < 0) throw new RangeError('Invalid native quota');
        if (measured > quota - used) throw new RangeError('Native input exceeds measured quota');
        if (task.calls !== 0 || attached.modelCalls >= attached.limits.maxModelCalls) throw new RangeError('Native model call budget exceeded');
        task.calls++; attached.modelCalls++;
        return target[method](input, options);
      };
      const value = Reflect.get(target, key, target);
      return typeof value === 'function' ? value.bind(target) : value;
    } });
    nativeSessions.add(wrapped);
    return wrapped;
  }
  function wrapApi(op, api) {
    if (!api || typeof api.availability !== 'function' || typeof api.create !== 'function') return undefined;
    async function availability(options) {
      if (!preparing) throw new Error('Explicit preparation is required');
      const state = await api.availability(options);
      if (!['available', 'downloadable', 'downloading'].includes(state)) throw new Error(`Native API is ${state}`);
      if (state !== 'available' && !preparing?.allowDownload) throw new Error('Explicit model download consent is required');
      return state;
    }
    return { availability, async create(options) {
      if (!preparing) throw new Error('Explicit preparation is required');
      const { sharedContext, initialPrompts, ...availabilityOptions } = preparing.options;
      await availability(availabilityOptions);
      gesture(); checkAbort(options.signal);
      const raw = await api.create(options);
      try { return wrapSession(op, raw); }
      catch (error) { raw?.destroy?.(); throw error; }
    } };
  }
  const nativeScope = { get isSecureContext() { return scope.isSecureContext; },
    get navigator() { return scope.navigator; },
    get Summarizer() { return wrapApi('summarize', scope.Summarizer); },
    get LanguageModel() { return wrapApi('prompt', scope.LanguageModel); } };
  const client = createChromeAiClient({ scope: nativeScope, maxInputChars: limits.maxInputBytes });
  function destroyNative() {
    let failure;
    try { client.destroy(); } catch (error) { failure = error; }
    for (const session of [...nativeSessions]) {
      try { session.destroy(); } catch (error) { failure ??= error; }
    }
    readyOperation = null;
    if (failure) throw failure;
  }
  async function prepare(op, options = {}, { allowDownload = false, signal, onDownloadProgress } = {}) {
    return serial(async () => {
      operation(op); gesture(); checkAbort(signal);
      if (typeof allowDownload !== 'boolean') throw new TypeError('allowDownload must be boolean');
      if (attached) throw new Error('Close the attached Java session before preparing another model');
      const normalized = normalizeCreateOptions(op, options);
      const checkText = value => {
        if (typeof value === 'string') unicode(value, 'creation option');
        else if (Array.isArray(value)) value.forEach(checkText);
        else if (value && typeof value === 'object') Object.values(value).forEach(checkText);
      };
      checkText(normalized);
      bytes(JSON.stringify(normalized), limits.maxMetadataBytes, 'creation options');
      destroyNative(); preparing = { allowDownload, options: normalized };
      try {
        const info = await client.prepare(op, normalized, { signal, onDownloadProgress });
        readyOperation = op;
        return Object.freeze({ protocol: PROTOCOL, operation: op, availability: info.availability,
          candidateOnly: true, admitted: false });
      } catch (error) { destroyNative(); throw error; }
      finally { preparing = null; }
    });
  }
  async function exchange(wire) {
    return serial(async () => {
      const packet = decodePacket(wire, packetCharacters(limits));
      object(packet);
      const op = operation(packet.operation), id = sessionId(packet.sessionId);
      if (packet.protocol !== PROTOCOL) throw new TypeError('Protocol version mismatch');
      if (packet.action === 'open') {
        exact(packet, [...BASE_KEYS, 'limits']); integer(packet.sequence, 0, 'sequence', 0);
        if (attached || readyOperation !== op) throw new Error('Prepare the selected operation before attaching one Java session');
        const selected = bounds(packet.limits, limits, true);
        attached = { operation: op, sessionId: id, sequence: 0, modelCalls: 0, limits: selected, failed: false };
        return encodePacket({ ...base('opened', op, id, 0), limits: selected,
          candidateOnly: true, admitted: false, modelCalls: 0, directNetworkCalls: 0 });
      }
      if (!attached || attached.operation !== op || attached.sessionId !== id) throw new Error('Session binding mismatch');
      const session = attached;
      if (packet.action === 'close') {
        exact(packet, BASE_KEYS); integer(packet.sequence, session.limits.maxModelCalls, 'sequence', session.sequence);
        attached = null;
        destroyNative();
        return encodePacket({ ...base('closed', op, id, packet.sequence), modelCalls: session.modelCalls, directNetworkCalls: 0 });
      }
      exact(packet, [...BASE_KEYS, 'packetId', 'request']);
      if (packet.action !== 'execute' || session.failed || readyOperation !== op) throw new Error('Session is not ready to execute');
      integer(packet.sequence, session.limits.maxModelCalls, 'sequence');
      if (packet.sequence !== session.sequence + 1) throw new Error('Replayed or out-of-order request');
      await validateRequest(packet.request, session.limits);
      const root = await requestPacketId(op, id, packet.sequence, packet.request);
      if (packet.packetId !== root) throw new TypeError('Request packet identity mismatch');
      session.sequence = packet.sequence;
      const task = { calls: 0 }; currentTask = task;
      try {
        const output = await client.execute(op, packet.request.input);
        bytes(output, session.limits.maxOutputBytes, 'output');
        if (javaWhitespace.test(output) || !output.trim() || task.calls !== 1) throw new TypeError('One nonempty native candidate is required');
        const result = await candidate(packet.request, output);
        if (attached !== session || session.failed) throw new Error('Session was destroyed');
        return encodePacket({ ...base('result', op, id, packet.sequence), packetId: root,
          resultPacketId: await resultPacketId(op, id, packet.sequence, root, result), result });
      } catch (error) {
        session.failed = true;
        try { destroyNative(); } catch (cleanup) { error.cause ??= cleanup; }
        throw error;
      } finally { currentTask = null; }
    });
  }
  function destroy() {
    if (attached) attached.failed = true;
    destroyNative();
  }
  return Object.freeze({ prepare, exchange, destroy });
}
