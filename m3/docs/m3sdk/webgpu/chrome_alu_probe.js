// M3 vCPU WebGPU probe for Chrome (Apache-2.0). Evaluated in a file:// page (secure context) whose
// <script type="text/wgsl" id="alu"> element holds rv32im_alu.wgsl byte-for-byte; the returned wgslSha proves it.
// Same inputs as dawn_alu_probe.c; the oracle is independent (BigInt for multiply-high and division).
async () => {
  const hex = b => [...new Uint8Array(b)].map(x => x.toString(16).padStart(2, '0')).join('');
  const src = document.getElementById('alu').textContent;
  const wgslSha = hex(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(src)));
  const C = [0,1,2,3,0x7FFFFFFF,0x80000000,0x80000001,0xFFFFFFFF,0xFFFFFFFE,0xFFFF,0x10000,0x1F,0x20,0x21,0x55555555,0xAAAAAAAA];
  const R = 262144, N = 256 + R;
  const pairs = new Uint32Array(2 * N);
  let k = 0;
  for (const a of C) for (const b of C) { pairs[k++] = a; pairs[k++] = b; }
  let x = 0x4D330001 >>> 0;
  const next = () => { x ^= x << 13; x >>>= 0; x ^= x >>> 17; x ^= x << 5; x >>>= 0; return x; };
  for (let r = 0; r < R; r++) { const a = next(); let b = next(); if ((r & 3) === 3) b = ((b & 7) + 0xFFFFFFFC) >>> 0; pairs[k++] = a; pairs[k++] = b; }
  const inSha = hex(await crypto.subtle.digest('SHA-256', pairs));
  const adapter = await navigator.gpu.requestAdapter({ powerPreference: 'high-performance' });
  const device = await adapter.requestDevice();
  const mod = device.createShaderModule({ code: src });
  const ci = await mod.getCompilationInfo();
  const msgs = ci.messages.filter(m => m.type === 'error').map(m => `${m.lineNum}:${m.linePos} ${m.message}`);
  if (msgs.length) return { wgslSha, errors: msgs };
  const pipe = await device.createComputePipelineAsync({ layout: 'auto', compute: { module: mod, entryPoint: 'main' } });
  const outBytes = N * 18 * 4;
  const bIn = device.createBuffer({ size: pairs.byteLength, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_DST });
  const bOut = device.createBuffer({ size: outBytes, usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_SRC });
  const bPar = device.createBuffer({ size: 16, usage: GPUBufferUsage.UNIFORM | GPUBufferUsage.COPY_DST });
  const bRd = device.createBuffer({ size: outBytes, usage: GPUBufferUsage.MAP_READ | GPUBufferUsage.COPY_DST });
  device.queue.writeBuffer(bIn, 0, pairs);
  device.queue.writeBuffer(bPar, 0, new Uint32Array([N, 0, 0, 0]));
  const bg = device.createBindGroup({ layout: pipe.getBindGroupLayout(0), entries: [
    { binding: 0, resource: { buffer: bIn } }, { binding: 1, resource: { buffer: bOut } }, { binding: 2, resource: { buffer: bPar } }] });
  const t0 = performance.now();
  const enc = device.createCommandEncoder();
  const pass = enc.beginComputePass(); pass.setPipeline(pipe); pass.setBindGroup(0, bg); pass.dispatchWorkgroups(Math.ceil(N / 256)); pass.end();
  enc.copyBufferToBuffer(bOut, 0, bRd, 0, outBytes);
  device.queue.submit([enc.finish()]);
  await bRd.mapAsync(GPUMapMode.READ);
  const t1 = performance.now();
  const gpu = new Uint32Array(bRd.getMappedRange().slice(0));
  bRd.unmap();
  const outSha = hex(await crypto.subtle.digest('SHA-256', gpu));
  const s = v => (v | 0), u = v => v >>> 0;
  const sB = v => BigInt(s(v)), uB = v => BigInt(u(v));
  const lo = v => Number(BigInt.asUintN(32, v));
  const hi = v => Number(BigInt.asUintN(32, v >> 32n));
  const names = ['ADD','SUB','SLL','SLT','SLTU','XOR','SRL','SRA','OR','AND','MUL','MULH','MULHSU','MULHU','DIV','DIVU','REM','REMU'];
  const bad = Object.fromEntries(names.map(n => [n, 0])); let firstBad = null;
  for (let i = 0; i < N; i++) {
    const a = pairs[2*i], b = pairs[2*i+1], sh = b & 31;
    const exp = [
      u(a + b), u(a - b), u(a << sh), s(a) < s(b) ? 1 : 0, a < b ? 1 : 0, u(a ^ b), a >>> sh, u(s(a) >> sh), u(a | b), u(a & b),
      lo(uB(a) * uB(b)), hi(sB(a) * sB(b)), hi(sB(a) * uB(b)), hi(uB(a) * uB(b)),
      b === 0 ? 0xFFFFFFFF : (a === 0x80000000 && b === 0xFFFFFFFF) ? 0x80000000 : lo(sB(a) / sB(b)),
      b === 0 ? 0xFFFFFFFF : u(Math.floor(a / b)),
      b === 0 ? a : (a === 0x80000000 && b === 0xFFFFFFFF) ? 0 : lo(sB(a) % sB(b)),
      b === 0 ? a : u(a % b)];
    for (let j = 0; j < 18; j++) if (gpu[18*i+j] !== (exp[j] >>> 0)) { bad[names[j]]++; if (!firstBad) firstBad = { i, op: names[j], a, b, gpu: gpu[18*i+j], exp: exp[j] >>> 0 }; }
  }
  const total = Object.values(bad).reduce((p, c) => p + c, 0);
  const info = adapter.info;
  return { chrome: navigator.userAgent.match(/Chrome\/[\d.]+/)[0], adapter: `${info.vendor}/${info.architecture}`, wgslSha, inSha, outSha,
    pairs: N, results: N * 18, mismatches: total, bad: total ? bad : undefined, firstBad, gpuMs: +(t1 - t0).toFixed(2) };
}
