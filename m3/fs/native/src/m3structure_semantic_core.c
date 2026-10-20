// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
#include "m3structure_semantic_core.h"

#include <stddef.h>
#include <stdint.h>

static uint32_t m3s_be32(const uint8_t *data, size_t offset) {
  return ((uint32_t)data[offset] << 24)
      | ((uint32_t)data[offset + 1U] << 16)
      | ((uint32_t)data[offset + 2U] << 8)
      | (uint32_t)data[offset + 3U];
}

static uint64_t m3s_be64(const uint8_t *data, size_t offset) {
  uint64_t value = UINT64_C(0);
  for (size_t index = 0U; index < 8U; ++index) {
    value = (value << 8) | (uint64_t)data[offset + index];
  }
  return value;
}

static int m3s_add(size_t left, size_t right, size_t *out) {
  if (left > SIZE_MAX - right) return 0;
  *out = left + right;
  return 1;
}

static int m3s_mul(size_t left, size_t right, size_t *out) {
  if (left != 0U && right > SIZE_MAX / left) return 0;
  *out = left * right;
  return 1;
}

static size_t m3s_align8(size_t value) {
  return (value + 7U) & ~(size_t)7U;
}

static int m3s_node_valid(const m3_structure_view *view, uint32_t node_id) {
  return view != NULL && node_id > 0U && node_id <= view->node_count;
}

static size_t m3s_node_offset(uint32_t lane, uint32_t node_id, size_t width) {
  return (size_t)lane + ((size_t)node_id - 1U) * width;
}

uint8_t m3_structure_kind(const m3_structure_view *view, uint32_t node_id) {
  if (!m3s_node_valid(view, node_id)) return UINT8_MAX;
  return view->data[m3s_node_offset(view->kinds, node_id, 1U)];
}

uint64_t m3_structure_first(const m3_structure_view *view, uint32_t node_id) {
  if (!m3s_node_valid(view, node_id)) return UINT64_C(0);
  return m3s_be64(
      view->data,
      m3s_node_offset(view->first, node_id, sizeof(uint64_t)));
}

uint64_t m3_structure_second(const m3_structure_view *view, uint32_t node_id) {
  if (!m3s_node_valid(view, node_id)) return UINT64_C(0);
  return m3s_be64(
      view->data,
      m3s_node_offset(view->second, node_id, sizeof(uint64_t)));
}

uint64_t m3_structure_hash(const m3_structure_view *view, uint32_t node_id) {
  if (!m3s_node_valid(view, node_id)) return UINT64_C(0);
  return m3s_be64(
      view->data,
      m3s_node_offset(view->hashes, node_id, sizeof(uint64_t)));
}

static uint32_t m3s_child_start(const m3_structure_view *view, uint32_t node_id) {
  if (!m3s_node_valid(view, node_id)) return 0U;
  return m3s_be32(
      view->data,
      m3s_node_offset(view->child_starts, node_id, sizeof(uint32_t)));
}

uint32_t m3_structure_child_count(const m3_structure_view *view, uint32_t node_id) {
  if (!m3s_node_valid(view, node_id)) return 0U;
  return m3s_be32(
      view->data,
      m3s_node_offset(view->child_counts, node_id, sizeof(uint32_t)));
}

uint32_t m3_structure_child(
    const m3_structure_view *view, uint32_t node_id, uint32_t ordinal) {
  uint32_t start;
  uint32_t count;
  if (!m3s_node_valid(view, node_id)) return 0U;
  start = m3s_child_start(view, node_id);
  count = m3_structure_child_count(view, node_id);
  if (ordinal >= count) return 0U;
  return m3s_be32(
      view->data,
      (size_t)view->children + ((size_t)start + ordinal) * sizeof(uint32_t));
}

uint32_t m3_structure_text_token_count(
    const m3_structure_view *view, uint32_t node_id) {
  uint8_t kind = m3_structure_kind(view, node_id);
  uint64_t count;
  if (kind != M3_STRUCTURE_TEXT && kind != M3_STRUCTURE_DECIMAL) return 0U;
  count = m3_structure_second(view, node_id);
  return count <= UINT32_MAX ? (uint32_t)count : 0U;
}

uint64_t m3_structure_text_token(
    const m3_structure_view *view, uint32_t node_id, uint32_t ordinal) {
  uint64_t start;
  uint32_t count = m3_structure_text_token_count(view, node_id);
  if (ordinal >= count) return UINT64_C(0);
  start = m3_structure_first(view, node_id);
  if (start > UINT32_MAX) return UINT64_C(0);
  return m3s_be64(
      view->data,
      (size_t)view->text_tokens
          + ((size_t)(uint32_t)start + ordinal) * sizeof(uint64_t));
}

uint32_t m3_structure_binary_count(
    const m3_structure_view *view, uint32_t node_id) {
  uint64_t count;
  if (m3_structure_kind(view, node_id) != M3_STRUCTURE_BINARY) return 0U;
  count = m3_structure_second(view, node_id);
  return count <= UINT32_MAX ? (uint32_t)count : 0U;
}

uint8_t m3_structure_binary_byte(
    const m3_structure_view *view, uint32_t node_id, uint32_t ordinal) {
  uint64_t start;
  uint32_t count = m3_structure_binary_count(view, node_id);
  if (ordinal >= count) return 0U;
  start = m3_structure_first(view, node_id);
  if (start > UINT32_MAX) return 0U;
  return view->data[
      (size_t)view->binary_bytes + (size_t)(uint32_t)start + ordinal];
}

uint32_t m3_structure_ref_target(
    const m3_structure_view *view, uint32_t node_id) {
  uint64_t target;
  if (m3_structure_kind(view, node_id) != M3_STRUCTURE_REF) return 0U;
  target = m3_structure_first(view, node_id);
  return target > 0U && target <= view->node_count ? (uint32_t)target : 0U;
}

uint32_t m3_structure_map_value(
    const m3_structure_view *view, uint32_t map_node_id, uint32_t key_node_id) {
  uint32_t count;
  if (!m3s_node_valid(view, map_node_id)
      || !m3s_node_valid(view, key_node_id)
      || m3_structure_kind(view, map_node_id) != M3_STRUCTURE_MAP) {
    return 0U;
  }
  count = m3_structure_child_count(view, map_node_id);
  for (uint32_t ordinal = 0U; ordinal + 1U < count; ordinal += 2U) {
    if (m3_structure_child(view, map_node_id, ordinal) == key_node_id) {
      return m3_structure_child(view, map_node_id, ordinal + 1U);
    }
  }
  return 0U;
}

static int m3s_validate_node(
    const m3_structure_view *view, uint32_t node_id) {
  uint8_t kind = m3_structure_kind(view, node_id);
  uint32_t start = m3s_child_start(view, node_id);
  uint32_t count = m3_structure_child_count(view, node_id);
  uint64_t first = m3_structure_first(view, node_id);
  uint64_t second = m3_structure_second(view, node_id);

  if (kind >= M3_STRUCTURE_KIND_COUNT
      || start > view->child_count
      || count > view->child_count - start) {
    return 0;
  }

  switch (kind) {
    case M3_STRUCTURE_NULL:
    case M3_STRUCTURE_BOOLEAN:
    case M3_STRUCTURE_INT32:
    case M3_STRUCTURE_INT64:
    case M3_STRUCTURE_FLOAT32:
    case M3_STRUCTURE_FLOAT64:
    case M3_STRUCTURE_TEMPORAL:
      return count == 0U;
    case M3_STRUCTURE_DECIMAL:
    case M3_STRUCTURE_TEXT:
      return count == 0U
          && first <= view->text_token_count
          && second <= (uint64_t)view->text_token_count - first;
    case M3_STRUCTURE_BINARY:
      return count == 0U
          && first <= view->binary_byte_count
          && second <= (uint64_t)view->binary_byte_count - first;
    case M3_STRUCTURE_TUPLE:
    case M3_STRUCTURE_SEQUENCE:
    case M3_STRUCTURE_TABLE:
      return 1;
    case M3_STRUCTURE_MAP:
      return (count & 1U) == 0U;
    case M3_STRUCTURE_EDGE:
      return count == 3U;
    case M3_STRUCTURE_REF:
      return count == 0U
          && second == UINT64_C(0)
          && first > UINT64_C(0)
          && first <= view->node_count;
    case M3_STRUCTURE_EVENT:
      return count == 2U;
    default:
      return 0;
  }
}

int m3_structure_open(
    const void *data,
    size_t bytes,
    m3_structure_view *out) {
  const uint8_t *raw = (const uint8_t *)data;
  m3_structure_view view;
  size_t expected;
  size_t payload;

  if (raw == NULL
      || out == NULL
      || bytes < M3_STRUCTURE_HEADER_BYTES
      || m3s_be64(raw, 0U) != M3_STRUCTURE_MAGIC
      || m3s_be32(raw, 8U) != M3_STRUCTURE_FORMAT) {
    return 0;
  }

  view.data = raw;
  view.bytes = bytes;
  view.domain_id = m3s_be32(raw, 12U);
  view.node_count = m3s_be32(raw, 16U);
  view.child_count = m3s_be32(raw, 20U);
  view.text_token_count = m3s_be32(raw, 24U);
  view.binary_byte_count = m3s_be32(raw, 28U);
  view.kinds = m3s_be32(raw, 32U);
  view.first = m3s_be32(raw, 36U);
  view.second = m3s_be32(raw, 40U);
  view.hashes = m3s_be32(raw, 44U);
  view.child_starts = m3s_be32(raw, 48U);
  view.child_counts = m3s_be32(raw, 52U);
  view.children = m3s_be32(raw, 56U);
  view.text_tokens = m3s_be32(raw, 60U);
  view.binary_bytes = m3s_be32(raw, 64U);

  if ((size_t)m3s_be32(raw, 68U) != bytes
      || m3s_be32(raw, 72U) != 0U
      || m3s_be64(raw, 76U) != UINT64_C(0)
      || m3s_be64(raw, 84U) != UINT64_C(0)
      || m3s_be32(raw, 92U) != 0U
      || view.kinds != M3_STRUCTURE_HEADER_BYTES) {
    return 0;
  }

  expected = m3s_align8((size_t)view.kinds + view.node_count);
  if (expected != view.first) return 0;

  if (!m3s_mul(view.node_count, sizeof(uint64_t), &payload)
      || !m3s_add(expected, payload, &expected)
      || expected != view.second
      || !m3s_add(expected, payload, &expected)
      || expected != view.hashes
      || !m3s_add(expected, payload, &expected)
      || expected != view.child_starts) {
    return 0;
  }

  if (!m3s_mul(view.node_count, sizeof(uint32_t), &payload)
      || !m3s_add(expected, payload, &expected)
      || expected != view.child_counts
      || !m3s_add(expected, payload, &expected)
      || expected != view.children
      || !m3s_mul(view.child_count, sizeof(uint32_t), &payload)
      || !m3s_add(expected, payload, &expected)) {
    return 0;
  }

  expected = m3s_align8(expected);
  if (expected != view.text_tokens
      || !m3s_mul(view.text_token_count, sizeof(uint64_t), &payload)
      || !m3s_add(expected, payload, &expected)
      || expected != view.binary_bytes
      || !m3s_add(expected, view.binary_byte_count, &expected)
      || expected != bytes) {
    return 0;
  }

  for (uint32_t id = 1U; id <= view.node_count; ++id) {
    if (!m3s_validate_node(&view, id)) return 0;
  }
  for (uint32_t ordinal = 0U; ordinal < view.child_count; ++ordinal) {
    uint32_t child =
        m3s_be32(
            raw,
            (size_t)view.children + (size_t)ordinal * sizeof(uint32_t));
    if (child == 0U || child > view.node_count) return 0;
  }

  *out = view;
  return 1;
}
