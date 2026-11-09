// SPDX-License-Identifier: Apache-2.0
// M3JDK 21 migration source: hsoliwal/com.synexia@c26433b5856192807616c9fb09741de0b1bdd93c
// aix-chrome-ai-studio/mechanical/browser/chrome-native-summarizer.mjs
import { availabilityOf, bytes, canonical, check, enumValue, fail, keys, language, languages,
  measure, openSession, receipt, sha256, text, wait } from './native-ai-session.mjs';

const DEFAULTS = Object.freeze({ type: 'key-points', format: 'markdown', length: 'short', preference: 'auto' });
const OPTION_KEYS = ['type', 'format', 'length', 'preference', 'sharedContext', 'outputLanguage',
  'expectedInputLanguages', 'expectedContextLanguages'];
export function summarizerOptions(options = {}) {
  keys(options, OPTION_KEYS);
  const value = { ...DEFAULTS, ...options };
  enumValue(value.type, ['key-points', 'tldr', 'teaser', 'headline'], 'type');
  enumValue(value.format, ['markdown', 'plain-text'], 'format');
  enumValue(value.length, ['short', 'medium', 'long'], 'length');
  enumValue(value.preference, ['auto', 'speed', 'capability'], 'preference');
  if (value.sharedContext !== undefined) text(value.sharedContext, 'sharedContext', 65536, true);
  if (value.outputLanguage !== undefined) value.outputLanguage = language(value.outputLanguage);
  for (const name of ['expectedInputLanguages', 'expectedContextLanguages']) {
    if (value[name] !== undefined) value[name] = languages(value[name]);
  }
  // Language syntax is checked here; actual language/pair support belongs to Chrome.availability().
  return Object.freeze(value);
}
function requestOptions(request = {}) {
  keys(request, ['context', 'signal'], 'request');
  return request.context === undefined ? {} : { context: text(request.context, 'context', 65536, true) };
}
export async function availability({ api = globalThis.Summarizer, ...options } = {}) {
  return availabilityOf(api, summarizerOptions(options));
}
function boundary(value, maximum) {
  let end = Math.min(value.length, maximum);
  if (end === value.length) return end;
  const minimum = Math.floor(end / 2);
  // Retain separators. Prefer paragraphs, then sentence/word boundaries, never split a surrogate pair.
  let preferred = value.lastIndexOf('\n', end - 1) + 1;
  if (preferred < minimum) preferred = Math.max(value.lastIndexOf(' ', end - 1), value.lastIndexOf('\t', end - 1)) + 1;
  if (preferred >= minimum && preferred > 0) end = preferred;
  if (end > 0 && /[\uD800-\uDBFF]/u.test(value[end - 1]) && /[\uDC00-\uDFFF]/u.test(value[end] ?? '')) end--;
  return end;
}
/** Exact contiguous source slices; unlike the old implementation this never trims away whitespace. */
export function splitText(value, maxChars = 12000) {
  text(value);
  if (!Number.isSafeInteger(maxChars) || maxChars < 256 || maxChars > 4 * 1024 * 1024)
    throw new RangeError('maxChars must be an integer of at least 256 and at most 4194304');
  const pieces = [];
  for (let start = 0; start < value.length;) {
    const end = start + boundary(value.slice(start, start + maxChars + 1), maxChars);
    pieces.push(value.slice(start, end)); start = end;
  }
  return pieces;
}
function corpusOptions(options) {
  keys(options, ['maxChars', 'maxCalls', 'maxRounds', 'maxChunks', 'maxMeasurements', 'context', 'signal', 'onProgress'], 'corpus options');
  const result = { maxChars: 12000, maxCalls: 128, maxRounds: 8, maxChunks: 256, maxMeasurements: 2048, ...options };
  for (const [name, minimum, maximum] of [['maxChars', 256, 65536], ['maxCalls', 1, 512], ['maxRounds', 1, 16],
    ['maxChunks', 1, 1024], ['maxMeasurements', 1, 8192]]) {
    if (!Number.isSafeInteger(result[name]) || result[name] < minimum || result[name] > maximum) fail('INVALID_LIMIT', `Invalid ${name}`);
  }
  if (result.onProgress !== undefined && typeof result.onProgress !== 'function') fail('INVALID_CALLBACK');
  requestOptions({ context: result.context, signal: result.signal });
  return Object.freeze(result);
}

export async function createChromeAiSummarizer({ api = globalThis.Summarizer, monitor, signal,
  limits, userActivation, onDownload, ...rawOptions } = {}) {
  const options = summarizerOptions(rawOptions);
  const { session, state, guard } = await openSession(api, options, { monitor, signal, limits, userActivation, onDownload });
  if (typeof session.summarize !== 'function') { guard.destroy(); fail('INVALID_SESSION'); }
  async function summarize(value, request, active) {
    const quota = await measure(session, value, request, active);
    if (quota && !quota.fits) fail('INPUT_QUOTA', 'Use summarizeCorpus for input larger than the native quota');
    check(active);
    const output = await wait(session.summarize(value, { ...request, signal: active }), active);
    return text(output, 'summary', guard.bounds.maxOutputBytes);
  }
  const helper = {
    availability: state, options,
    get closed() { return guard.closed; },
    get inputQuota() { return session.inputQuota ?? null; },
    measureInputUsage(value, request = {}) {
      text(value, 'text', guard.bounds.maxInputBytes); const native = requestOptions(request);
      return guard.run(async active => {
        const result = await measure(session, value, native, active);
        if (!result) fail('QUOTA_UNAVAILABLE', 'Native input measurement is not exposed');
        return result.usage;
      }, request.signal);
    },
    summarize(value, request = {}) {
      text(value, 'text', guard.bounds.maxInputBytes); const native = requestOptions(request);
      return guard.run(active => summarize(value, native, active), request.signal);
    },
    summarizeWithReceipt(value, request = {}) {
      text(value, 'text', guard.bounds.maxInputBytes); const native = requestOptions(request);
      return guard.run(async active => {
        const output = await summarize(value, native, active);
        const record = await receipt('Summarizer', value, output, options, native, { calls: 1, streaming: false });
        check(active); return { text: output, receipt: record };
      }, request.signal);
    },
    async *summarizeStreaming(value, request = {}) {
      text(value, 'text', guard.bounds.maxInputBytes); const native = requestOptions(request);
      if (typeof session.summarizeStreaming !== 'function') fail('STREAM_UNAVAILABLE');
      yield* guard.stream(async active => {
        const quota = await measure(session, value, native, active);
        if (quota && !quota.fits) fail('INPUT_QUOTA');
        check(active); return session.summarizeStreaming(value, { ...native, signal: active });
      }, request.signal);
    },
    async summarizeCorpus(value, request = {}) {
      return (await helper.summarizeCorpusWithReceipt(value, request)).text;
    },
    summarizeCorpusWithReceipt(value, request = {}) {
      text(value, 'corpus', guard.bounds.maxInputBytes);
      const plan = corpusOptions(request), native = requestOptions({ context: plan.context });
      return guard.run(async active => {
        const nodes = [], sourceSpans = [];
        let calls = 0, measurements = 0, level = value, lineage = [];
        const hasQuota = typeof session.measureInputUsage === 'function' && session.inputQuota !== undefined;
        if (!hasQuota && value.length > plan.maxChars) fail('QUOTA_UNAVAILABLE', 'Corpus splitting requires native measureInputUsage and inputQuota');
        async function fits(piece) {
          if (++measurements > plan.maxMeasurements) fail('MEASUREMENT_LIMIT');
          return (await measure(session, piece, native, active))?.fits ?? true;
        }
        async function splitLevel(input) {
          const pending = []; let offset = 0;
          for (const part of splitText(input, plan.maxChars)) {
            pending.push({ text: part, start: offset, end: offset + part.length }); offset += part.length;
          }
          if (pending.length > plan.maxChunks) fail('CHUNK_LIMIT');
          const fitted = [];
          // Bounded explicit stack avoids recursive stack growth; sort restores source order.
          while (pending.length) {
            check(active); const part = pending.pop();
            if (!part.text.trim() || await fits(part.text)) fitted.push(part);
            else {
              if (part.text.length <= 1 || [...part.text].length === 1) fail('INPUT_QUOTA', 'A single code point plus context exceeds the quota');
              const cut = boundary(part.text, Math.max(1, Math.floor(part.text.length / 2)))
                || (part.text.codePointAt(0) > 0xFFFF ? 2 : 1);
              if (cut >= part.text.length) fail('INPUT_QUOTA');
              pending.push({ text: part.text.slice(0, cut), start: part.start, end: part.start + cut },
                { text: part.text.slice(cut), start: part.start + cut, end: part.end });
            }
            if (fitted.length + pending.length > plan.maxChunks) fail('CHUNK_LIMIT');
          }
          return fitted.sort((a, b) => a.start - b.start);
        }
        for (let round = 0; round < plan.maxRounds; round++) {
          check(active);
          const pieces = await splitLevel(level), outputs = [];
          let generatedBytes = 0;
          for (const part of pieces) {
            check(active);
            const inputHash = await sha256(part.text);
            let output = '', kind = 'WHITESPACE_NOT_SENT';
            if (part.text.trim()) {
              if (calls >= plan.maxCalls) fail('CALL_LIMIT');
              // Re-measure immediately before each generation; session/options are unchanged.
              if (!(await fits(part.text))) fail('INPUT_QUOTA');
              calls++; kind = 'NATIVE_SUMMARY';
              output = await wait(session.summarize(part.text, { ...native, signal: active }), active);
              text(output, 'summary', guard.bounds.maxOutputBytes);
            }
            generatedBytes += bytes(output) + 2;
            if (pieces.length > 1 && generatedBytes > guard.bounds.maxInputBytes) fail('BYTE_LIMIT', 'Intermediate summaries exceed corpus budget');
            const id = `n${nodes.length}`;
            const parents = lineage.filter(p => p.start < part.end && p.end > part.start).map(p => p.id);
            nodes.push(Object.freeze({ id, round, kind, parents: Object.freeze(parents), inputSha256: inputHash,
              outputSha256: await sha256(output) }));
            if (round === 0) sourceSpans.push(Object.freeze({ start: part.start, end: part.end, inputSha256: inputHash, node: id }));
            outputs.push({ id, text: output });
            check(active);
            if (plan.onProgress) await wait(plan.onProgress(Object.freeze({ round, calls, measurements,
              completedChunks: outputs.length, chunks: pieces.length })), active);
            check(active);
          }
          if (outputs.length === 1) {
            const output = outputs[0].text;
            const graph = { sourceSpans, nodes, coordinateSystem: 'UTF16_CODE_UNITS', completeSourcePartition: true };
            const record = await receipt('Summarizer', value, output, options, native, { calls, measurements,
              nativeQuotaMeasured: hasQuota, rounds: round + 1, ...graph, graphSha256: await sha256(canonical(graph)) });
            check(active); return { text: output, receipt: record };
          }
          const combined = outputs.map(output => output.text).join('\n\n');
          if (bytes(combined) >= bytes(level)) fail('NON_SHRINKING', 'Summaries did not shrink; no partial corpus result was published');
          let position = 0;
          lineage = outputs.map(output => {
            const span = { id: output.id, start: position, end: position + output.text.length };
            position = span.end + 2; return span;
          });
          level = combined;
        }
        return fail('ROUND_LIMIT', 'Summary hierarchy exceeded its round budget');
      }, plan.signal);
    },
    destroy() { guard.destroy(); },
  };
  return Object.freeze(helper);
}
