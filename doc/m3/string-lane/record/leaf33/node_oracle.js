// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal and others

// node_oracle.js <work-dir>: node (V8, ECMAScript) as the oracle of the ENCODE family. Texts are UTF-16 code units in
// hex (big endian) as EncodeFamilyProof corpus writes them; JavaScript strings are UTF-16, so lone surrogates are kept.
// A cell is '=' + an ASCII result, 'h' + a result in hex units, or '!' when a URIError is thrown. Writes:
// NODE_ENC.txt per line of enc_texts.txt: encodeURIComponent(s) TAB encodeURI(s) TAB s.toWellFormed()
// NODE_DEC.txt per line of dec_texts.txt: decodeURIComponent(s) TAB decodeURI(s)
'use strict';
const fs = require('fs');
const path = require('path');

const work = process.argv[2];

function lines(name) {
  const all = fs.readFileSync(path.join(work, name), 'ascii').split('\n');
  all.pop();
  return all;
}

function text(h) {
  const units = new Array(h.length / 4);
  for (let i = 0; i < units.length; i++) units[i] = parseInt(h.substr(4 * i, 4), 16);
  let out = '';
  for (let i = 0; i < units.length; i += 4096) out += String.fromCharCode.apply(null, units.slice(i, i + 4096));
  return out;
}

function hex(s) {
  const out = new Array(s.length);
  for (let i = 0; i < s.length; i++) out[i] = s.charCodeAt(i).toString(16).toUpperCase().padStart(4, '0');
  return out.join('');
}

function cell(f, s, asText) {
  try {
    const r = f(s);
    return asText ? '=' + r : 'h' + hex(r);
  } catch (e) {
    if (e instanceof URIError) return '!';
    throw e;
  }
}

const enc = lines('enc_texts.txt').map(text);
fs.writeFileSync(path.join(work, 'NODE_ENC.txt'), enc.map(s => [cell(encodeURIComponent, s, true),
  cell(encodeURI, s, true), cell(t => t.toWellFormed(), s, false)].join('\t') + '\n').join(''), 'ascii');
const dec = lines('dec_texts.txt').map(text);
fs.writeFileSync(path.join(work, 'NODE_DEC.txt'), dec.map(s => [cell(decodeURIComponent, s, false),
  cell(decodeURI, s, false)].join('\t') + '\n').join(''), 'ascii');
console.log('node', process.version, 'v8', process.versions.v8, 'enc_texts', enc.length, 'dec_texts', dec.length);
