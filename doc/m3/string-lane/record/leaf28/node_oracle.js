// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal and others
//
// node_oracle.js <dir>: Node.js as the oracle of String.padStart and String.padEnd (String.prototype.padStart and
// padEnd). Reads corpus.bin (int32 count, then per text int32 units + UTF-16LE units) and params.txt (op, abs|rel, n,
// pad as dot-joined hex units or '-', texts) written by BuildFamilyProof --write-corpus. JS strings are UTF-16 unit
// sequences, so every text and pad is rebuilt unit for unit. For every padStart/padEnd row, in order, writes the answer
// for the first <texts> texts to pad.ans, unit for unit (lone surrogates kept).
'use strict';
const fs = require('fs');
const path = require('path');

const dir = process.argv[2];
const corpus = fs.readFileSync(path.join(dir, 'corpus.bin'));
const count = corpus.readInt32LE(0);
const texts = [];
let offset = 4;
for (let i = 0; i < count; i++) {
  const units = corpus.readInt32LE(offset);
  offset += 4;
  const codes = new Array(units);
  for (let k = 0; k < units; k++) codes[k] = corpus.readUInt16LE(offset + 2 * k);
  offset += 2 * units;
  texts.push(String.fromCharCode(...codes));
}
const chunks = [];
let rows = 0;
for (const line of fs.readFileSync(path.join(dir, 'params.txt'), 'ascii').split('\n')) {
  if (!line) continue;
  const [op, kind, n, pad, limit] = line.split('\t');
  if (op !== 'padStart' && op !== 'padEnd') continue;
  rows++;
  const fill = pad === '-' ? '' : String.fromCharCode(...pad.split('.').map((h) => parseInt(h, 16)));
  for (const text of texts.slice(0, Number(limit))) {
    const target = Number(n) + (kind === 'rel' ? text.length : 0);
    const answer = op === 'padStart' ? text.padStart(target, fill) : text.padEnd(target, fill);
    const buf = Buffer.alloc(4 + 2 * answer.length);
    buf.writeInt32LE(answer.length, 0);
    for (let k = 0; k < answer.length; k++) buf.writeUInt16LE(answer.charCodeAt(k), 4 + 2 * k);
    chunks.push(buf);
  }
}
fs.writeFileSync(path.join(dir, 'pad.ans'), Buffer.concat(chunks));
console.log('node', process.version, 'pad rows', rows, 'texts', texts.length);
