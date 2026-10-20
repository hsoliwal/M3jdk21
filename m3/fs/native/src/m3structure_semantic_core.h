// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
#ifndef M3STRUCTURE_SEMANTIC_CORE_H
#define M3STRUCTURE_SEMANTIC_CORE_H

#include <stddef.h>
#include <stdint.h>

#define M3_STRUCTURE_MAGIC UINT64_C(0x4d334d5354523031)
#define M3_STRUCTURE_FORMAT 1U
#define M3_STRUCTURE_HEADER_BYTES 96U
#define M3_STRUCTURE_KIND_COUNT 17U

enum {
  M3_STRUCTURE_NULL = 0,
  M3_STRUCTURE_BOOLEAN = 1,
  M3_STRUCTURE_INT32 = 2,
  M3_STRUCTURE_INT64 = 3,
  M3_STRUCTURE_FLOAT32 = 4,
  M3_STRUCTURE_FLOAT64 = 5,
  M3_STRUCTURE_DECIMAL = 6,
  M3_STRUCTURE_TEXT = 7,
  M3_STRUCTURE_BINARY = 8,
  M3_STRUCTURE_TEMPORAL = 9,
  M3_STRUCTURE_TUPLE = 10,
  M3_STRUCTURE_SEQUENCE = 11,
  M3_STRUCTURE_MAP = 12,
  M3_STRUCTURE_TABLE = 13,
  M3_STRUCTURE_EDGE = 14,
  M3_STRUCTURE_REF = 15,
  M3_STRUCTURE_EVENT = 16
};

typedef struct m3_structure_view {
  const uint8_t *data;
  size_t bytes;
  uint32_t domain_id;
  uint32_t node_count;
  uint32_t child_count;
  uint32_t text_token_count;
  uint32_t binary_byte_count;
  uint32_t kinds;
  uint32_t first;
  uint32_t second;
  uint32_t hashes;
  uint32_t child_starts;
  uint32_t child_counts;
  uint32_t children;
  uint32_t text_tokens;
  uint32_t binary_bytes;
} m3_structure_view;

int m3_structure_open(
    const void *data,
    size_t bytes,
    m3_structure_view *out);

uint8_t m3_structure_kind(const m3_structure_view *view, uint32_t node_id);
uint64_t m3_structure_first(const m3_structure_view *view, uint32_t node_id);
uint64_t m3_structure_second(const m3_structure_view *view, uint32_t node_id);
uint64_t m3_structure_hash(const m3_structure_view *view, uint32_t node_id);
uint32_t m3_structure_child_count(const m3_structure_view *view, uint32_t node_id);
uint32_t m3_structure_child(
    const m3_structure_view *view, uint32_t node_id, uint32_t ordinal);
uint32_t m3_structure_text_token_count(
    const m3_structure_view *view, uint32_t node_id);
uint64_t m3_structure_text_token(
    const m3_structure_view *view, uint32_t node_id, uint32_t ordinal);
uint32_t m3_structure_binary_count(
    const m3_structure_view *view, uint32_t node_id);
uint8_t m3_structure_binary_byte(
    const m3_structure_view *view, uint32_t node_id, uint32_t ordinal);
uint32_t m3_structure_ref_target(
    const m3_structure_view *view, uint32_t node_id);
uint32_t m3_structure_map_value(
    const m3_structure_view *view, uint32_t map_node_id, uint32_t key_node_id);

#endif
