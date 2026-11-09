// SPDX-License-Identifier: Apache-2.0
// rv32im_slice.wgsl - RV32IM virtual-CPU slice kernel in WGSL: the hand-written twin of
// kernels/rv32im_slice.cl for the "webgpu" backend (m3-parallel-vcpu-design.md section 3.4).
//
// m3k-twin-of: rv32im_slice.cl sha256=8ecfc67e35e13159121403acbbe3dfda8b9858508bf632665472bac8ad7cfe66
//
// The pin above is the SHA-256 of the OpenCL C source whose semantics this file restates. The
// build (src/backends/webgpu/m3vcpu_webgpu_embed.cmake) refuses to embed the twin when
// rv32im_slice.cl changes: edit both, run the full RV32IM suite on the webgpu backend, then
// update the pin (parity before admission).
//
// Observable semantics are identical to the OpenCL kernel (and the donor Rv32iKernel.java):
//   - one invocation = one virtual core; invocations with index >= CORE_COUNT return
//   - RV32I 2.1 + M 2.0, FENCE/FENCE.I no-ops, ECALL (cause 8, tval 0), EBREAK (cause 3,
//     tval = pc), custom-0 XM3_HALT 0x0000000B (exit code = a0, retired, pc += 4, HALTED)
//   - x0 reads as 0; its storage slot is zeroed after every retire, at halt and at yield
//   - HALTED / TRAPPED cores are skipped; any other status runs as READY
//   - traps use RISC-V cause numbers; the faulting instruction is not retired, PC stays on it
//   - INSTRUCTION_BUDGET instructions at most per launch; budget <= 0 yields at once
//   - all arithmetic is on u32 bit patterns (wrap-around = Java int semantics)
//
// WGSL specifics (each restates, never changes, the OpenCL behaviour):
//   - 32-bit integers only: MULH / MULHSU / MULHU are built from 16-bit partial products
//     (proven bit-exact: m3/docs/m3sdk/webgpu/rv32im_alu.wgsl, WEBGPU_PROBE_RECEIPTS.tsv)
//   - WGSL defines x / 0 and INT_MIN / -1 differently from RISC-V: both corners are guarded
//     before the division, exactly as in the OpenCL helpers
//   - RAM is array<u32>; LB/LH/LBU/LHU extract lanes and SB/SH read-modify-write the owning
//     little-endian word (one core owns its RAM slice, so no atomics are needed)
//   - break inside a WGSL switch leaves the switch, so every loop exit sits outside it
//
// M3 WGSL kernel convention (the backend relies on it for every M3K_WGSL blob):
//   - arguments are @group(0) @binding(i) storage buffers, i = launch argument index
//   - workgroup size is the pipeline-overridable constant m3_local (backend sets it from the
//     launch's local size)
//   - linear index = (workgroup_id.y * num_workgroups.x + workgroup_id.x) * m3_local
//                    + local_invocation_index  (the backend uses a 2-D grid above 65,535
//     workgroups); the kernel guards index >= its own item count

@group(0) @binding(0) var<storage, read_write> memory: array<u32>;
@group(0) @binding(1) var<storage, read_write> registers: array<u32>;
@group(0) @binding(2) var<storage, read_write> state: array<i32>;
@group(0) @binding(3) var<storage, read> config: array<i32>;

override m3_local: u32 = 64u;

const M3K_RV_REGS: u32 = 32u;
const M3K_RV_STATE_PC: u32 = 0u;
const M3K_RV_STATE_STATUS: u32 = 1u;
const M3K_RV_STATE_RETIRED: u32 = 2u;
const M3K_RV_STATE_TRAP_CAUSE: u32 = 3u;
const M3K_RV_STATE_TRAP_VALUE: u32 = 4u;
const M3K_RV_STATE_EXIT_CODE: u32 = 5u;
const M3K_RV_STATE_STRIDE: u32 = 6u;
const M3K_RV_READY: i32 = 0;
const M3K_RV_HALTED: i32 = 1;
const M3K_RV_TRAPPED: i32 = 2;
const M3K_RV_YIELDED: i32 = 3;
const M3K_RV_TRAP_NONE: i32 = -1;
const M3K_RV_TRAP_FETCH_MISALIGNED: i32 = 0;
const M3K_RV_TRAP_FETCH_FAULT: i32 = 1;
const M3K_RV_TRAP_ILLEGAL: i32 = 2;
const M3K_RV_TRAP_BREAKPOINT: i32 = 3;
const M3K_RV_TRAP_LOAD_MISALIGNED: i32 = 4;
const M3K_RV_TRAP_LOAD_FAULT: i32 = 5;
const M3K_RV_TRAP_STORE_MISALIGNED: i32 = 6;
const M3K_RV_TRAP_STORE_FAULT: i32 = 7;
const M3K_RV_TRAP_ECALL: i32 = 8;
const M3K_RV_CONFIG_CORE_COUNT: u32 = 0u;
const M3K_RV_CONFIG_WORDS: u32 = 1u;
const M3K_RV_CONFIG_BUDGET: u32 = 2u;
const M3K_RV_XM3_HALT: u32 = 0x0000000Bu;
const M3K_MIN_I32: u32 = 0x80000000u;

// Sign-extends the low `bits` bits of value (1 <= bits <= 31); returns the bit pattern.
fn m3k_sext(value: u32, bits: u32) -> u32 {
  let sbit = 1u << (bits - 1u);
  let field = value & ((sbit << 1u) - 1u);
  return (field ^ sbit) - sbit;
}

fn m3k_imm_i(w: u32) -> u32 {
  return m3k_sext(w >> 20u, 12u);
}

fn m3k_imm_s(w: u32) -> u32 {
  return m3k_sext(((w >> 25u) << 5u) | ((w >> 7u) & 0x1Fu), 12u);
}

fn m3k_imm_b(w: u32) -> u32 {
  return m3k_sext(((w >> 31u) << 12u) | (((w >> 7u) & 0x1u) << 11u)
                  | (((w >> 25u) & 0x3Fu) << 5u) | (((w >> 8u) & 0xFu) << 1u), 13u);
}

fn m3k_imm_u(w: u32) -> u32 {
  return w & 0xFFFFF000u;
}

fn m3k_imm_j(w: u32) -> u32 {
  return m3k_sext(((w >> 31u) << 20u) | (((w >> 12u) & 0xFFu) << 12u)
                  | (((w >> 20u) & 0x1u) << 11u) | (((w >> 21u) & 0x3FFu) << 1u), 21u);
}

// Arithmetic shift right on the two's-complement bit pattern.
fn m3k_sra(v: u32, s: u32) -> u32 {
  let fill = 0u - (v >> 31u);
  return (v >> s) | (fill & ~(0xFFFFFFFFu >> s));
}

// Donor inRange: address >= 0 && address <= memoryBytes - width, with Java int wrap.
fn m3k_in_range(address: i32, width: i32, memory_bytes: i32) -> bool {
  let limit = bitcast<i32>(bitcast<u32>(memory_bytes) - bitcast<u32>(width));
  return address >= 0 && address <= limit;
}

fn m3k_div(a: u32, b: u32) -> u32 {
  if (b == 0u) {
    return 0xFFFFFFFFu;
  }
  if (a == M3K_MIN_I32 && b == 0xFFFFFFFFu) {
    return M3K_MIN_I32;
  }
  return bitcast<u32>(bitcast<i32>(a) / bitcast<i32>(b));
}

fn m3k_rem(a: u32, b: u32) -> u32 {
  if (b == 0u) {
    return a;
  }
  if (a == M3K_MIN_I32 && b == 0xFFFFFFFFu) {
    return 0u;
  }
  return bitcast<u32>(bitcast<i32>(a) % bitcast<i32>(b));
}

fn m3k_divu(a: u32, b: u32) -> u32 {
  return select(a / max(b, 1u), 0xFFFFFFFFu, b == 0u);
}

fn m3k_remu(a: u32, b: u32) -> u32 {
  return select(a % max(b, 1u), a, b == 0u);
}

// High word of the unsigned 64-bit product from four 16 x 16 partial products.
fn m3k_mulhu(a: u32, b: u32) -> u32 {
  let a0 = a & 0xFFFFu;
  let a1 = a >> 16u;
  let b0 = b & 0xFFFFu;
  let b1 = b >> 16u;
  let p00 = a0 * b0;
  let p01 = a0 * b1;
  let p10 = a1 * b0;
  let p11 = a1 * b1;
  let mid = (p00 >> 16u) + (p01 & 0xFFFFu) + (p10 & 0xFFFFu);
  return p11 + (p01 >> 16u) + (p10 >> 16u) + (mid >> 16u);
}

// Signed correction: subtract y from the unsigned high word when x is negative.
fn m3k_neg_fix(x: u32, y: u32) -> u32 {
  return select(0u, y, (x & M3K_MIN_I32) != 0u);
}

fn m3k_mulh(a: u32, b: u32) -> u32 {
  return m3k_mulhu(a, b) - m3k_neg_fix(a, b) - m3k_neg_fix(b, a);
}

fn m3k_mulhsu(a: u32, b: u32) -> u32 {
  return m3k_mulhu(a, b) - m3k_neg_fix(a, b);
}

fn m3k_slt(a: u32, b: u32) -> u32 {
  return select(0u, 1u, bitcast<i32>(a) < bitcast<i32>(b));
}

fn m3k_sltu(a: u32, b: u32) -> u32 {
  return select(0u, 1u, a < b);
}

@compute @workgroup_size(m3_local)
fn m3k_rv32im_slice(@builtin(workgroup_id) wid: vec3<u32>, @builtin(num_workgroups) nwg: vec3<u32>,
                    @builtin(local_invocation_index) lidx: u32) {
  let core = (wid.y * nwg.x + wid.x) * m3_local + lidx;
  let core_count = config[M3K_RV_CONFIG_CORE_COUNT];
  if (core_count <= 0 || core >= u32(core_count)) {
    return;
  }
  let words = config[M3K_RV_CONFIG_WORDS];
  let budget = config[M3K_RV_CONFIG_BUDGET];
  let memory_bytes = bitcast<i32>(bitcast<u32>(words) << 2u);
  // Per-VM base indices; the core's geometry guard keeps cores * words below 2^32.
  let mem_base = core * bitcast<u32>(words);
  let reg_base = core * M3K_RV_REGS;
  let st_base = core * M3K_RV_STATE_STRIDE;

  let status = state[st_base + M3K_RV_STATE_STATUS];
  if (status == M3K_RV_HALTED || status == M3K_RV_TRAPPED) {
    return;
  }
  state[st_base + M3K_RV_STATE_STATUS] = M3K_RV_READY;
  var pc: u32 = bitcast<u32>(state[st_base + M3K_RV_STATE_PC]);
  var retired: u32 = bitcast<u32>(state[st_base + M3K_RV_STATE_RETIRED]);
  var x: array<u32, 32>;
  for (var r: u32 = 0u; r < M3K_RV_REGS; r++) {
    x[r] = registers[reg_base + r];
  }
  x[0] = 0u;

  var outcome: i32 = M3K_RV_YIELDED;
  var cause: i32 = M3K_RV_TRAP_NONE;
  var tval: i32 = 0;
  var exit_code: i32 = 0;
  var any_retired: bool = false;

  for (var n: i32 = 0; n < budget; n++) {
    if ((pc & 3u) != 0u) {
      cause = M3K_RV_TRAP_FETCH_MISALIGNED;
      tval = bitcast<i32>(pc);
      break;
    }
    if (!m3k_in_range(bitcast<i32>(pc), 4, memory_bytes)) {
      cause = M3K_RV_TRAP_FETCH_FAULT;
      tval = bitcast<i32>(pc);
      break;
    }
    let insn = memory[mem_base + (pc >> 2u)];
    if (insn == M3K_RV_XM3_HALT) {
      retired += 1u;
      pc += 4u;
      exit_code = bitcast<i32>(x[10]);
      outcome = M3K_RV_HALTED;
      break;
    }

    let opcode = insn & 0x7Fu;
    let rd = (insn >> 7u) & 0x1Fu;
    let f3 = (insn >> 12u) & 0x7u;
    let rs1 = (insn >> 15u) & 0x1Fu;
    let rs2 = (insn >> 20u) & 0x1Fu;
    let f7 = insn >> 25u;
    let a = x[rs1];
    let b = x[rs2];
    var next: u32 = pc + 4u;
    var value: u32 = 0u;
    var legal: bool = true;
    var wb: bool = false;

    switch (opcode) {
      case 0x37u: { // LUI
        value = m3k_imm_u(insn);
        wb = true;
      }
      case 0x17u: { // AUIPC
        value = pc + m3k_imm_u(insn);
        wb = true;
      }
      case 0x6Fu: { // JAL
        let dest = pc + m3k_imm_j(insn);
        if ((dest & 3u) != 0u) {
          cause = M3K_RV_TRAP_FETCH_MISALIGNED;
          tval = bitcast<i32>(dest);
        } else {
          value = next;
          wb = true;
          next = dest;
        }
      }
      case 0x67u: { // JALR
        if (f3 != 0u) {
          legal = false;
        } else {
          let dest = (a + m3k_imm_i(insn)) & ~1u;
          if ((dest & 3u) != 0u) {
            cause = M3K_RV_TRAP_FETCH_MISALIGNED;
            tval = bitcast<i32>(dest);
          } else {
            value = next;
            wb = true;
            next = dest;
          }
        }
      }
      case 0x63u: { // BRANCH
        var take: bool = false;
        if (f3 == 0u) {
          take = a == b;
        } else if (f3 == 1u) {
          take = a != b;
        } else if (f3 == 4u) {
          take = bitcast<i32>(a) < bitcast<i32>(b);
        } else if (f3 == 5u) {
          take = bitcast<i32>(a) >= bitcast<i32>(b);
        } else if (f3 == 6u) {
          take = a < b;
        } else if (f3 == 7u) {
          take = a >= b;
        } else {
          legal = false;
        }
        if (legal && take) {
          let dest = pc + m3k_imm_b(insn);
          if ((dest & 3u) != 0u) {
            cause = M3K_RV_TRAP_FETCH_MISALIGNED;
            tval = bitcast<i32>(dest);
          } else {
            next = dest;
          }
        }
      }
      case 0x03u: { // LOAD: LB LH LW LBU LHU
        let address = bitcast<i32>(a + m3k_imm_i(insn));
        let width = 1i << (f3 & 3u);
        if (f3 == 3u || f3 > 5u) {
          legal = false;
        } else if ((address & (width - 1i)) != 0i) {
          cause = M3K_RV_TRAP_LOAD_MISALIGNED;
          tval = address;
        } else if (!m3k_in_range(address, width, memory_bytes)) {
          cause = M3K_RV_TRAP_LOAD_FAULT;
          tval = address;
        } else {
          let word = memory[mem_base + (bitcast<u32>(address) >> 2u)];
          let lane = word >> ((bitcast<u32>(address) & 3u) << 3u);
          if (f3 == 0u) {
            value = m3k_sext(lane & 0xFFu, 8u);
          } else if (f3 == 1u) {
            value = m3k_sext(lane & 0xFFFFu, 16u);
          } else if (f3 == 2u) {
            value = word;
          } else if (f3 == 4u) {
            value = lane & 0xFFu;
          } else {
            value = lane & 0xFFFFu;
          }
          wb = true;
        }
      }
      case 0x23u: { // STORE: SB SH SW
        let address = bitcast<i32>(a + m3k_imm_s(insn));
        let width = 1i << (f3 & 3u);
        if (f3 > 2u) {
          legal = false;
        } else if ((address & (width - 1i)) != 0i) {
          cause = M3K_RV_TRAP_STORE_MISALIGNED;
          tval = address;
        } else if (!m3k_in_range(address, width, memory_bytes)) {
          cause = M3K_RV_TRAP_STORE_FAULT;
          tval = address;
        } else {
          let index = mem_base + (bitcast<u32>(address) >> 2u);
          if (f3 == 2u) {
            memory[index] = b;
          } else {
            let shift = (bitcast<u32>(address) & 3u) << 3u;
            let mask = select(0xFFFFu, 0xFFu, f3 == 0u) << shift;
            let word = memory[index];
            memory[index] = (word & ~mask) | ((b << shift) & mask);
          }
        }
      }
      case 0x13u: { // OP-IMM
        let imm = m3k_imm_i(insn);
        let shamt = (insn >> 20u) & 0x1Fu;
        wb = true;
        switch (f3) {
          case 0u: {
            value = a + imm;
          }
          case 2u: {
            value = m3k_slt(a, imm);
          }
          case 3u: {
            value = m3k_sltu(a, imm);
          }
          case 4u: {
            value = a ^ imm;
          }
          case 6u: {
            value = a | imm;
          }
          case 7u: {
            value = a & imm;
          }
          case 1u: {
            if (f7 != 0u) {
              legal = false;
            } else {
              value = a << shamt;
            }
          }
          default: { // 5: SRLI / SRAI
            if (f7 == 0u) {
              value = a >> shamt;
            } else if (f7 == 0x20u) {
              value = m3k_sra(a, shamt);
            } else {
              legal = false;
            }
          }
        }
      }
      case 0x33u: { // OP and M extension
        wb = true;
        if (f7 == 0x01u) {
          switch (f3) {
            case 0u: {
              value = a * b;
            }
            case 1u: {
              value = m3k_mulh(a, b);
            }
            case 2u: {
              value = m3k_mulhsu(a, b);
            }
            case 3u: {
              value = m3k_mulhu(a, b);
            }
            case 4u: {
              value = m3k_div(a, b);
            }
            case 5u: {
              value = m3k_divu(a, b);
            }
            case 6u: {
              value = m3k_rem(a, b);
            }
            default: {
              value = m3k_remu(a, b);
            }
          }
        } else if (f7 == 0u) {
          switch (f3) {
            case 0u: {
              value = a + b;
            }
            case 1u: {
              value = a << (b & 0x1Fu);
            }
            case 2u: {
              value = m3k_slt(a, b);
            }
            case 3u: {
              value = m3k_sltu(a, b);
            }
            case 4u: {
              value = a ^ b;
            }
            case 5u: {
              value = a >> (b & 0x1Fu);
            }
            case 6u: {
              value = a | b;
            }
            default: {
              value = a & b;
            }
          }
        } else if (f7 == 0x20u && f3 == 0u) {
          value = a - b;
        } else if (f7 == 0x20u && f3 == 5u) {
          value = m3k_sra(a, b & 0x1Fu);
        } else {
          legal = false;
        }
      }
      case 0x0Fu: { // MISC-MEM: FENCE, FENCE.I are no-ops
        if (f3 > 1u) {
          legal = false;
        }
      }
      case 0x73u: { // SYSTEM: exact ECALL / EBREAK only
        if (insn == 0x00000073u) {
          cause = M3K_RV_TRAP_ECALL;
          tval = 0;
        } else if (insn == 0x00100073u) {
          cause = M3K_RV_TRAP_BREAKPOINT;
          tval = bitcast<i32>(pc);
        } else {
          legal = false;
        }
      }
      default: {
        legal = false;
      }
    }

    if (!legal) {
      cause = M3K_RV_TRAP_ILLEGAL;
      tval = bitcast<i32>(insn);
    }
    if (cause != M3K_RV_TRAP_NONE) {
      break;
    }
    if (wb) {
      x[rd] = value;
    }
    x[0] = 0u;
    retired += 1u;
    pc = next;
    any_retired = true;
  }

  for (var r: u32 = 1u; r < M3K_RV_REGS; r++) {
    registers[reg_base + r] = x[r];
  }
  state[st_base + M3K_RV_STATE_PC] = bitcast<i32>(pc);
  state[st_base + M3K_RV_STATE_RETIRED] = bitcast<i32>(retired);
  if (cause != M3K_RV_TRAP_NONE) {
    state[st_base + M3K_RV_STATE_STATUS] = M3K_RV_TRAPPED;
    state[st_base + M3K_RV_STATE_TRAP_CAUSE] = cause;
    state[st_base + M3K_RV_STATE_TRAP_VALUE] = tval;
    if (any_retired) {
      registers[reg_base] = 0u;
    }
  } else {
    state[st_base + M3K_RV_STATE_STATUS] = outcome;
    if (outcome == M3K_RV_HALTED) {
      state[st_base + M3K_RV_STATE_EXIT_CODE] = exit_code;
    }
    registers[reg_base] = 0u;
  }
}
