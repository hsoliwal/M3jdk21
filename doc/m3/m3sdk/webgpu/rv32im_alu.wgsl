// RV32IM register-register ALU semantics in WGSL (M3 vCPU WebGPU probe; Apache-2.0).
// One invocation = one (a, b) pair; writes the 18 R-type results in RV32IM funct order:
// ADD SUB SLL SLT SLTU XOR SRL SRA OR AND MUL MULH MULHSU MULHU DIV DIVU REM REMU.
// WGSL has 32-bit integers only, so the high multiply words are built from 16-bit partial products.
@group(0) @binding(0) var<storage, read> pairs: array<u32>;
@group(0) @binding(1) var<storage, read_write> results: array<u32>;
@group(0) @binding(2) var<uniform> params: vec4<u32>;

const OPS: u32 = 18u;
const MIN_I32: u32 = 0x80000000u;

fn mulhu(a: u32, b: u32) -> u32 {
  let a0 = a & 0xFFFFu; let a1 = a >> 16u;
  let b0 = b & 0xFFFFu; let b1 = b >> 16u;
  let p00 = a0 * b0; let p01 = a0 * b1; let p10 = a1 * b0; let p11 = a1 * b1;
  let mid = (p00 >> 16u) + (p01 & 0xFFFFu) + (p10 & 0xFFFFu);
  return p11 + (p01 >> 16u) + (p10 >> 16u) + (mid >> 16u);
}

fn neg_fix(x: u32, y: u32) -> u32 {
  return select(0u, y, (x & MIN_I32) != 0u);
}

fn div_s(a: u32, b: u32) -> u32 {
  if (b == 0u) { return 0xFFFFFFFFu; }
  if (a == MIN_I32 && b == 0xFFFFFFFFu) { return MIN_I32; }
  return bitcast<u32>(bitcast<i32>(a) / bitcast<i32>(b));
}

fn rem_s(a: u32, b: u32) -> u32 {
  if (b == 0u) { return a; }
  if (a == MIN_I32 && b == 0xFFFFFFFFu) { return 0u; }
  return bitcast<u32>(bitcast<i32>(a) % bitcast<i32>(b));
}

@compute @workgroup_size(256)
fn main(@builtin(global_invocation_id) gid: vec3<u32>) {
  let i = gid.x;
  if (i >= params.x) { return; }
  let a = pairs[2u * i];
  let b = pairs[2u * i + 1u];
  let sh = b & 31u;
  let o = OPS * i;
  results[o + 0u] = a + b;
  results[o + 1u] = a - b;
  results[o + 2u] = a << sh;
  results[o + 3u] = select(0u, 1u, bitcast<i32>(a) < bitcast<i32>(b));
  results[o + 4u] = select(0u, 1u, a < b);
  results[o + 5u] = a ^ b;
  results[o + 6u] = a >> sh;
  results[o + 7u] = bitcast<u32>(bitcast<i32>(a) >> sh);
  results[o + 8u] = a | b;
  results[o + 9u] = a & b;
  results[o + 10u] = a * b;
  let hu = mulhu(a, b);
  results[o + 11u] = hu - neg_fix(a, b) - neg_fix(b, a);
  results[o + 12u] = hu - neg_fix(a, b);
  results[o + 13u] = hu;
  results[o + 14u] = div_s(a, b);
  results[o + 15u] = select(a / b, 0xFFFFFFFFu, b == 0u);
  results[o + 16u] = rem_s(a, b);
  results[o + 17u] = select(a % b, a, b == 0u);
}
