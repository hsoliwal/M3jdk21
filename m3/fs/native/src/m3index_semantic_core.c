// SPDX-License-Identifier: Apache-2.0
#include "m3index_semantic_core.h"

#include <limits.h>
#include <stdlib.h>
#include <string.h>

static uint16_t m3_be16(const uint8_t *data, size_t offset) {
  return (uint16_t)(((uint16_t)data[offset] << 8)
      | (uint16_t)data[offset + 1U]);
}

static uint32_t m3_be32(const uint8_t *data, size_t offset) {
  return ((uint32_t)data[offset] << 24)
      | ((uint32_t)data[offset + 1U] << 16)
      | ((uint32_t)data[offset + 2U] << 8)
      | (uint32_t)data[offset + 3U];
}

static uint64_t m3_be64(const uint8_t *data, size_t offset) {
  uint64_t value = UINT64_C(0);
  for (size_t index = 0U; index < 8U; ++index) {
    value = (value << 8) | (uint64_t)data[offset + index];
  }
  return value;
}

static int m3_add_size(size_t left, size_t right, size_t *out) {
  if (left > SIZE_MAX - right) return 0;
  *out = left + right;
  return 1;
}

static int m3_mul_size(size_t left, size_t right, size_t *out) {
  if (left != 0U && right > SIZE_MAX / left) return 0;
  *out = left * right;
  return 1;
}

static int m3_lane_end(
    uint32_t offset,
    size_t count,
    size_t width,
    size_t bytes,
    size_t *out_end) {
  size_t payload;
  size_t end;
  if (!m3_mul_size(count, width, &payload)
      || !m3_add_size((size_t)offset, payload, &end)
      || end > bytes) {
    return 0;
  }
  *out_end = end;
  return 1;
}

int m3_mindex_open(
    const void *data,
    size_t bytes,
    m3_mindex_view *out_view) {
  const uint8_t *raw = (const uint8_t *)data;
  uint32_t values;
  uint32_t tokens;
  uint32_t units;
  uint32_t token_starts;
  uint32_t utf16_starts;
  uint32_t token_ids;
  uint32_t utf16_units;
  uint32_t hashes;
  uint32_t signals;
  uint32_t end32;
  size_t end;

  if (raw == NULL || out_view == NULL || bytes < M3_MINDEX_IMAGE_HEADER_BYTES) {
    return M3_MINDEX_REGEX_INVALID;
  }
  if (m3_be64(raw, 0U) != M3_MINDEX_IMAGE_MAGIC
      || m3_be32(raw, 8U) != M3_MINDEX_IMAGE_FORMAT) {
    return M3_MINDEX_REGEX_INVALID;
  }

  values = m3_be32(raw, 12U);
  tokens = m3_be32(raw, 16U);
  units = m3_be32(raw, 20U);
  token_starts = m3_be32(raw, 24U);
  utf16_starts = m3_be32(raw, 28U);
  token_ids = m3_be32(raw, 32U);
  utf16_units = m3_be32(raw, 36U);
  hashes = m3_be32(raw, 40U);
  signals = m3_be32(raw, 44U);
  end32 = m3_be32(raw, 48U);

  if (m3_be32(raw, 52U) != 0U || m3_be64(raw, 56U) != UINT64_C(0)) {
    return M3_MINDEX_REGEX_INVALID;
  }
  if ((size_t)end32 != bytes
      || token_starts != M3_MINDEX_IMAGE_HEADER_BYTES) {
    return M3_MINDEX_REGEX_INVALID;
  }

  if (!m3_lane_end(
          token_starts,
          (size_t)values + 1U,
          sizeof(uint32_t),
          bytes,
          &end)
      || end != (size_t)utf16_starts
      || !m3_lane_end(
          utf16_starts,
          (size_t)values + 1U,
          sizeof(uint32_t),
          bytes,
          &end)
      || end != (size_t)token_ids
      || !m3_lane_end(
          token_ids,
          (size_t)tokens,
          sizeof(uint64_t),
          bytes,
          &end)
      || end != (size_t)utf16_units
      || !m3_lane_end(
          utf16_units,
          (size_t)units,
          sizeof(uint16_t),
          bytes,
          &end)
      || end != (size_t)hashes
      || !m3_lane_end(
          hashes,
          (size_t)values,
          sizeof(uint32_t),
          bytes,
          &end)
      || end != (size_t)signals
      || !m3_lane_end(
          signals,
          (size_t)values,
          sizeof(uint64_t),
          bytes,
          &end)
      || end != bytes) {
    return M3_MINDEX_REGEX_INVALID;
  }

  if (m3_be32(raw, token_starts) != 0U
      || m3_be32(raw, utf16_starts) != 0U
      || m3_be32(
              raw,
              (size_t)token_starts + (size_t)values * sizeof(uint32_t))
          != tokens
      || m3_be32(
              raw,
              (size_t)utf16_starts + (size_t)values * sizeof(uint32_t))
          != units) {
    return M3_MINDEX_REGEX_INVALID;
  }

  for (uint32_t value = 0U; value < values; ++value) {
    uint32_t token_begin =
        m3_be32(
            raw,
            (size_t)token_starts + (size_t)value * sizeof(uint32_t));
    uint32_t token_end =
        m3_be32(
            raw,
            (size_t)token_starts + ((size_t)value + 1U) * sizeof(uint32_t));
    uint32_t char_begin =
        m3_be32(
            raw,
            (size_t)utf16_starts + (size_t)value * sizeof(uint32_t));
    uint32_t char_end =
        m3_be32(
            raw,
            (size_t)utf16_starts + ((size_t)value + 1U) * sizeof(uint32_t));
    if (token_begin > token_end
        || token_end > tokens
        || char_begin > char_end
        || char_end > units) {
      return M3_MINDEX_REGEX_INVALID;
    }
  }

  out_view->data = raw;
  out_view->bytes = bytes;
  out_view->value_count = values;
  out_view->token_count = tokens;
  out_view->utf16_count = units;
  out_view->token_starts = token_starts;
  out_view->utf16_starts = utf16_starts;
  out_view->token_ids = token_ids;
  out_view->utf16_units = utf16_units;
  out_view->java_hashes = hashes;
  out_view->signals = signals;
  return M3_MINDEX_REGEX_OK;
}

static uint32_t m3_token_start(
    const m3_mindex_view *view,
    uint32_t value) {
  return m3_be32(
      view->data,
      (size_t)view->token_starts + (size_t)value * sizeof(uint32_t));
}

static uint32_t m3_char_start(
    const m3_mindex_view *view,
    uint32_t value) {
  return m3_be32(
      view->data,
      (size_t)view->utf16_starts + (size_t)value * sizeof(uint32_t));
}

uint32_t m3_mindex_value_token_count(
    const m3_mindex_view *view,
    uint32_t value) {
  if (view == NULL || value >= view->value_count) return 0U;
  return m3_token_start(view, value + 1U) - m3_token_start(view, value);
}

uint64_t m3_mindex_value_token(
    const m3_mindex_view *view,
    uint32_t value,
    uint32_t ordinal) {
  uint32_t start;
  uint32_t count;
  if (view == NULL || value >= view->value_count) return UINT64_C(0);
  start = m3_token_start(view, value);
  count = m3_token_start(view, value + 1U) - start;
  if (ordinal >= count) return UINT64_C(0);
  return m3_be64(
      view->data,
      (size_t)view->token_ids
          + ((size_t)start + (size_t)ordinal) * sizeof(uint64_t));
}

uint32_t m3_mindex_value_utf16_length(
    const m3_mindex_view *view,
    uint32_t value) {
  if (view == NULL || value >= view->value_count) return 0U;
  return m3_char_start(view, value + 1U) - m3_char_start(view, value);
}

uint16_t m3_mindex_value_char(
    const m3_mindex_view *view,
    uint32_t value,
    uint32_t ordinal) {
  uint32_t start;
  uint32_t count;
  if (view == NULL || value >= view->value_count) return 0U;
  start = m3_char_start(view, value);
  count = m3_char_start(view, value + 1U) - start;
  if (ordinal >= count) return 0U;
  return m3_be16(
      view->data,
      (size_t)view->utf16_units
          + ((size_t)start + (size_t)ordinal) * sizeof(uint16_t));
}

int32_t m3_mindex_value_java_hash(
    const m3_mindex_view *view,
    uint32_t value) {
  if (view == NULL || value >= view->value_count) return 0;
  return (int32_t)m3_be32(
      view->data,
      (size_t)view->java_hashes + (size_t)value * sizeof(uint32_t));
}

uint64_t m3_mindex_value_signal64(
    const m3_mindex_view *view,
    uint32_t value) {
  if (view == NULL || value >= view->value_count) return UINT64_C(0);
  return m3_be64(
      view->data,
      (size_t)view->signals + (size_t)value * sizeof(uint64_t));
}

int m3_mindex_value_equals(
    const m3_mindex_view *view,
    uint32_t left,
    uint32_t right) {
  uint32_t left_length;
  uint32_t right_length;
  if (view == NULL
      || left >= view->value_count
      || right >= view->value_count) {
    return 0;
  }
  if (left == right) return 1;
  if (m3_mindex_value_java_hash(view, left)
      != m3_mindex_value_java_hash(view, right)) {
    return 0;
  }
  left_length = m3_mindex_value_utf16_length(view, left);
  right_length = m3_mindex_value_utf16_length(view, right);
  if (left_length != right_length) return 0;
  for (uint32_t index = 0U; index < left_length; ++index) {
    if (m3_mindex_value_char(view, left, index)
        != m3_mindex_value_char(view, right, index)) {
      return 0;
    }
  }
  return 1;
}

int m3_mindex_value_compare(
    const m3_mindex_view *view,
    uint32_t left,
    uint32_t right) {
  uint32_t left_length;
  uint32_t right_length;
  uint32_t common;
  if (view == NULL
      || left >= view->value_count
      || right >= view->value_count) {
    return 0;
  }
  left_length = m3_mindex_value_utf16_length(view, left);
  right_length = m3_mindex_value_utf16_length(view, right);
  common = left_length < right_length ? left_length : right_length;
  for (uint32_t index = 0U; index < common; ++index) {
    uint16_t a = m3_mindex_value_char(view, left, index);
    uint16_t b = m3_mindex_value_char(view, right, index);
    if (a < b) return -1;
    if (a > b) return 1;
  }
  if (left_length < right_length) return -1;
  if (left_length > right_length) return 1;
  return 0;
}

int32_t m3_mindex_token_index_of(
    const m3_mindex_view *view,
    uint32_t value,
    const uint64_t *needle,
    uint32_t needle_count,
    uint32_t from_token) {
  uint32_t haystack_count;
  uint32_t limit;
  if (view == NULL
      || value >= view->value_count
      || (needle == NULL && needle_count != 0U)) {
    return -1;
  }
  haystack_count = m3_mindex_value_token_count(view, value);
  if (from_token > haystack_count) return -1;
  if (needle_count == 0U) {
    return from_token <= (uint32_t)INT32_MAX ? (int32_t)from_token : -1;
  }
  if (needle_count > haystack_count - from_token) return -1;
  limit = haystack_count - needle_count;
  for (uint32_t start = from_token; start <= limit; ++start) {
    uint32_t index = 0U;
    while (index < needle_count
        && m3_mindex_value_token(view, value, start + index)
            == needle[index]) {
      ++index;
    }
    if (index == needle_count) {
      return start <= (uint32_t)INT32_MAX ? (int32_t)start : -1;
    }
  }
  return -1;
}

static int m3_regex_copy(
    void **target,
    const void *source,
    size_t count,
    size_t width) {
  size_t bytes;
  if (!m3_mul_size(count, width, &bytes)) return 0;
  if (bytes == 0U) {
    *target = NULL;
    return 1;
  }
  *target = source == NULL ? calloc(count, width) : malloc(bytes);
  if (*target == NULL) return 0;
  if (source != NULL) memcpy(*target, source, bytes);
  return 1;
}

static void m3_regex_clear(m3_mindex_regex_program *program) {
  if (program == NULL) return;
  free(program->op);
  free(program->out1);
  free(program->out2);
  free(program->class_id);
  free(program->ascii_low);
  free(program->ascii_high);
  free(program->range_offset);
  free(program->range_size);
  free(program->range_low);
  free(program->range_high);
  free(program->active);
  free(program->next);
  free(program->mark);
  free(program->stack);
  memset(program, 0, sizeof(*program));
}

int m3_mindex_regex_program_init(
    m3_mindex_regex_program *program,
    const uint8_t *op,
    const int32_t *out1,
    const int32_t *out2,
    const int32_t *class_id,
    uint32_t state_count,
    const uint64_t *ascii_low,
    const uint64_t *ascii_high,
    const int32_t *range_offset,
    const int32_t *range_size,
    uint32_t class_count,
    const uint16_t *range_low,
    const uint16_t *range_high,
    uint32_t range_count,
    uint32_t start_state,
    uint32_t match_state,
    int32_t min_length,
    int32_t max_length,
    uint64_t required_presence64,
    int may_consume_surrogate_unit) {
  if (program == NULL
      || op == NULL
      || out1 == NULL
      || out2 == NULL
      || class_id == NULL
      || state_count == 0U
      || start_state >= state_count
      || match_state >= state_count
      || (class_count != 0U
          && (ascii_low == NULL
              || ascii_high == NULL
              || range_offset == NULL
              || range_size == NULL))
      || (range_count != 0U
          && (range_low == NULL || range_high == NULL))
      || min_length < 0
      || max_length < -1) {
    return M3_MINDEX_REGEX_INVALID;
  }

  memset(program, 0, sizeof(*program));
  program->state_count = state_count;
  program->class_count = class_count;
  program->range_count = range_count;
  program->start_state = start_state;
  program->match_state = match_state;
  program->min_length = min_length;
  program->max_length = max_length;
  program->required_presence64 = required_presence64;
  program->may_consume_surrogate_unit = may_consume_surrogate_unit != 0;
  program->words = (state_count + 63U) / 64U;

  if (!m3_regex_copy((void **)&program->op, op, state_count, sizeof(uint8_t))
      || !m3_regex_copy((void **)&program->out1, out1, state_count, sizeof(int32_t))
      || !m3_regex_copy((void **)&program->out2, out2, state_count, sizeof(int32_t))
      || !m3_regex_copy((void **)&program->class_id, class_id, state_count, sizeof(int32_t))
      || !m3_regex_copy((void **)&program->ascii_low, ascii_low, class_count, sizeof(uint64_t))
      || !m3_regex_copy((void **)&program->ascii_high, ascii_high, class_count, sizeof(uint64_t))
      || !m3_regex_copy((void **)&program->range_offset, range_offset, class_count, sizeof(int32_t))
      || !m3_regex_copy((void **)&program->range_size, range_size, class_count, sizeof(int32_t))
      || !m3_regex_copy((void **)&program->range_low, range_low, range_count, sizeof(uint16_t))
      || !m3_regex_copy((void **)&program->range_high, range_high, range_count, sizeof(uint16_t))
      || !m3_regex_copy((void **)&program->active, NULL, program->words, sizeof(uint64_t))
      || !m3_regex_copy((void **)&program->next, NULL, program->words, sizeof(uint64_t))
      || !m3_regex_copy((void **)&program->mark, NULL, state_count, sizeof(uint32_t))
      || !m3_regex_copy((void **)&program->stack, NULL, state_count, sizeof(uint32_t))) {
    m3_regex_clear(program);
    return M3_MINDEX_REGEX_NO_MEMORY;
  }

  memset(program->active, 0, (size_t)program->words * sizeof(uint64_t));
  memset(program->next, 0, (size_t)program->words * sizeof(uint64_t));
  memset(program->mark, 0, (size_t)state_count * sizeof(uint32_t));

  for (uint32_t state = 0U; state < state_count; ++state) {
    uint8_t opcode = program->op[state];
    int32_t first = program->out1[state];
    int32_t second = program->out2[state];
    int32_t class_value = program->class_id[state];
    if (opcode < M3_MINDEX_REGEX_OP_CHAR
        || opcode > M3_MINDEX_REGEX_OP_ASSERT_NOT_WORD_BOUNDARY
        || first < -1
        || (first >= 0 && (uint32_t)first >= state_count)
        || second < -1
        || (second >= 0 && (uint32_t)second >= state_count)) {
      m3_regex_clear(program);
      return M3_MINDEX_REGEX_INVALID;
    }
    if (opcode == M3_MINDEX_REGEX_OP_CHAR) {
      if (first < 0
          || class_value < 0
          || (uint32_t)class_value >= class_count) {
        m3_regex_clear(program);
        return M3_MINDEX_REGEX_INVALID;
      }
    } else if (class_value != -1) {
      m3_regex_clear(program);
      return M3_MINDEX_REGEX_INVALID;
    }
    if (opcode == M3_MINDEX_REGEX_OP_SPLIT
        && (first < 0 || second < 0)) {
      m3_regex_clear(program);
      return M3_MINDEX_REGEX_INVALID;
    }
    if ((opcode == M3_MINDEX_REGEX_OP_JUMP
            || opcode == M3_MINDEX_REGEX_OP_ASSERT_START
            || opcode == M3_MINDEX_REGEX_OP_ASSERT_END
            || opcode == M3_MINDEX_REGEX_OP_ASSERT_WORD_BOUNDARY
            || opcode == M3_MINDEX_REGEX_OP_ASSERT_NOT_WORD_BOUNDARY)
        && first < 0) {
      m3_regex_clear(program);
      return M3_MINDEX_REGEX_INVALID;
    }
  }
  if (program->op[match_state] != M3_MINDEX_REGEX_OP_MATCH) {
    m3_regex_clear(program);
    return M3_MINDEX_REGEX_INVALID;
  }

  for (uint32_t id = 0U; id < class_count; ++id) {
    int32_t offset = program->range_offset[id];
    int32_t count = program->range_size[id];
    if (offset < 0
        || count < 0
        || (uint32_t)offset > range_count
        || (uint32_t)count > range_count - (uint32_t)offset) {
      m3_regex_clear(program);
      return M3_MINDEX_REGEX_INVALID;
    }
  }
  for (uint32_t index = 0U; index < range_count; ++index) {
    if (program->range_low[index] > program->range_high[index]) {
      m3_regex_clear(program);
      return M3_MINDEX_REGEX_INVALID;
    }
  }
  return M3_MINDEX_REGEX_OK;
}

void m3_mindex_regex_program_destroy(m3_mindex_regex_program *program) {
  m3_regex_clear(program);
}

static uint32_t m3_ctz64(uint64_t value) {
  uint32_t count = 0U;
  while ((value & UINT64_C(1)) == UINT64_C(0)) {
    value >>= 1;
    ++count;
  }
  return count;
}

static void m3_regex_set(uint64_t *bits, uint32_t state) {
  bits[state >> 6] |= UINT64_C(1) << (state & 63U);
}

static int m3_regex_contains(const uint64_t *bits, uint32_t state) {
  return (bits[state >> 6] & (UINT64_C(1) << (state & 63U))) != 0U;
}

static int m3_is_ascii_word(uint16_t value) {
  return (value >= (uint16_t)'A' && value <= (uint16_t)'Z')
      || (value >= (uint16_t)'a' && value <= (uint16_t)'z')
      || (value >= (uint16_t)'0' && value <= (uint16_t)'9')
      || value == (uint16_t)'_';
}

static int m3_word_boundary(
    const m3_mindex_view *view,
    uint32_t value,
    uint32_t position,
    uint32_t length) {
  int left =
      position > 0U
          && m3_is_ascii_word(
              m3_mindex_value_char(view, value, position - 1U));
  int right =
      position < length
          && m3_is_ascii_word(
              m3_mindex_value_char(view, value, position));
  return left != right;
}

static void m3_regex_begin_closure(m3_mindex_regex_program *program) {
  ++program->stamp;
  if (program->stamp == 0U) {
    memset(
        program->mark,
        0,
        (size_t)program->state_count * sizeof(uint32_t));
    program->stamp = 1U;
  }
}

static int m3_regex_push(
    m3_mindex_regex_program *program,
    uint32_t top,
    int32_t state) {
  uint32_t coordinate;
  if (state < 0) return (int)top;
  coordinate = (uint32_t)state;
  if (program->mark[coordinate] == program->stamp) {
    return (int)top;
  }
  if (top >= program->state_count) return -1;
  program->mark[coordinate] = program->stamp;
  program->stack[top] = coordinate;
  return (int)(top + 1U);
}

static int m3_regex_add_closure(
    m3_mindex_regex_program *program,
    uint64_t *target,
    int32_t initial,
    const m3_mindex_view *view,
    uint32_t value,
    uint32_t position,
    uint32_t length) {
  uint32_t top = 0U;
  int pushed;
  if (initial < 0) return M3_MINDEX_REGEX_OK;

  pushed = m3_regex_push(program, top, initial);
  if (pushed < 0) return M3_MINDEX_REGEX_INVALID;
  top = (uint32_t)pushed;

  while (top != 0U) {
    uint32_t state = program->stack[--top];
    uint8_t opcode = program->op[state];
    int first;
    switch (opcode) {
      case M3_MINDEX_REGEX_OP_SPLIT:
        first = m3_regex_push(program, top, program->out2[state]);
        if (first < 0) return M3_MINDEX_REGEX_INVALID;
        top = (uint32_t)first;
        first = m3_regex_push(program, top, program->out1[state]);
        if (first < 0) return M3_MINDEX_REGEX_INVALID;
        top = (uint32_t)first;
        break;
      case M3_MINDEX_REGEX_OP_JUMP:
        first = m3_regex_push(program, top, program->out1[state]);
        if (first < 0) return M3_MINDEX_REGEX_INVALID;
        top = (uint32_t)first;
        break;
      case M3_MINDEX_REGEX_OP_ASSERT_START:
        if (position == 0U) {
          first = m3_regex_push(program, top, program->out1[state]);
          if (first < 0) return M3_MINDEX_REGEX_INVALID;
          top = (uint32_t)first;
        }
        break;
      case M3_MINDEX_REGEX_OP_ASSERT_END:
        if (position == length) {
          first = m3_regex_push(program, top, program->out1[state]);
          if (first < 0) return M3_MINDEX_REGEX_INVALID;
          top = (uint32_t)first;
        }
        break;
      case M3_MINDEX_REGEX_OP_ASSERT_WORD_BOUNDARY:
        if (m3_word_boundary(view, value, position, length)) {
          first = m3_regex_push(program, top, program->out1[state]);
          if (first < 0) return M3_MINDEX_REGEX_INVALID;
          top = (uint32_t)first;
        }
        break;
      case M3_MINDEX_REGEX_OP_ASSERT_NOT_WORD_BOUNDARY:
        if (!m3_word_boundary(view, value, position, length)) {
          first = m3_regex_push(program, top, program->out1[state]);
          if (first < 0) return M3_MINDEX_REGEX_INVALID;
          top = (uint32_t)first;
        }
        break;
      case M3_MINDEX_REGEX_OP_CHAR:
      case M3_MINDEX_REGEX_OP_MATCH:
        m3_regex_set(target, state);
        break;
      default:
        return M3_MINDEX_REGEX_INVALID;
    }
  }
  return M3_MINDEX_REGEX_OK;
}

static int m3_regex_class_matches(
    const m3_mindex_regex_program *program,
    int32_t class_id,
    uint16_t value) {
  uint32_t id = (uint32_t)class_id;
  if (value < 64U) {
    return ((program->ascii_low[id] >> value) & UINT64_C(1)) != 0U;
  }
  if (value < 128U) {
    return ((program->ascii_high[id] >> (value - 64U)) & UINT64_C(1)) != 0U;
  }

  {
    int32_t low = program->range_offset[id];
    int32_t high = low + program->range_size[id] - 1;
    while (low <= high) {
      int32_t middle = low + ((high - low) >> 1);
      if (value < program->range_low[middle]) {
        high = middle - 1;
      } else if (value > program->range_high[middle]) {
        low = middle + 1;
      } else {
        return 1;
      }
    }
  }
  return 0;
}

static int m3_regex_step(
    m3_mindex_regex_program *program,
    const m3_mindex_view *view,
    uint32_t value_index,
    uint16_t value,
    uint32_t next_position,
    uint32_t length) {
  memset(
      program->next,
      0,
      (size_t)program->words * sizeof(uint64_t));
  m3_regex_begin_closure(program);

  for (uint32_t word = 0U; word < program->words; ++word) {
    uint64_t bits = program->active[word];
    while (bits != UINT64_C(0)) {
      uint32_t bit = m3_ctz64(bits);
      uint32_t state = (word << 6) + bit;
      bits &= bits - UINT64_C(1);
      if (state < program->state_count
          && program->op[state] == M3_MINDEX_REGEX_OP_CHAR
          && m3_regex_class_matches(
              program,
              program->class_id[state],
              value)) {
        int status =
            m3_regex_add_closure(
                program,
                program->next,
                program->out1[state],
                view,
                value_index,
                next_position,
                length);
        if (status != M3_MINDEX_REGEX_OK) return status;
      }
    }
  }

  {
    uint64_t *swap = program->active;
    program->active = program->next;
    program->next = swap;
  }
  return M3_MINDEX_REGEX_OK;
}

static int m3_regex_surrogate_sensitive(
    const m3_mindex_regex_program *program,
    const m3_mindex_view *view,
    uint32_t value,
    uint32_t length) {
  if (!program->may_consume_surrogate_unit) return 0;
  for (uint32_t index = 0U; index < length; ++index) {
    uint16_t unit = m3_mindex_value_char(view, value, index);
    if (unit >= UINT16_C(0xd800) && unit <= UINT16_C(0xdfff)) {
      return 1;
    }
  }
  return 0;
}

static int m3_regex_required_signal_missing(
    const m3_mindex_regex_program *program,
    const m3_mindex_view *view,
    uint32_t value) {
  uint64_t required = program->required_presence64;
  uint64_t available;
  if (required == UINT64_C(0)) return 0;
  available = m3_mindex_value_signal64(view, value);
  return (available & required) != required;
}

static int m3_regex_matches(
    m3_mindex_regex_program *program,
    const m3_mindex_view *view,
    uint32_t value,
    uint32_t length,
    int *out_match) {
  int status;
  memset(program->active, 0, (size_t)program->words * sizeof(uint64_t));
  m3_regex_begin_closure(program);
  status =
      m3_regex_add_closure(
          program,
          program->active,
          (int32_t)program->start_state,
          view,
          value,
          0U,
          length);
  if (status != M3_MINDEX_REGEX_OK) return status;

  for (uint32_t position = 0U; position < length; ++position) {
    status =
        m3_regex_step(
            program,
            view,
            value,
            m3_mindex_value_char(view, value, position),
            position + 1U,
            length);
    if (status != M3_MINDEX_REGEX_OK) return status;
  }
  *out_match = m3_regex_contains(program->active, program->match_state);
  return M3_MINDEX_REGEX_OK;
}

static int m3_regex_looking_at(
    m3_mindex_regex_program *program,
    const m3_mindex_view *view,
    uint32_t value,
    uint32_t length,
    int *out_match) {
  int status;
  memset(program->active, 0, (size_t)program->words * sizeof(uint64_t));
  m3_regex_begin_closure(program);
  status =
      m3_regex_add_closure(
          program,
          program->active,
          (int32_t)program->start_state,
          view,
          value,
          0U,
          length);
  if (status != M3_MINDEX_REGEX_OK) return status;
  if (m3_regex_contains(program->active, program->match_state)) {
    *out_match = 1;
    return M3_MINDEX_REGEX_OK;
  }

  for (uint32_t position = 0U; position < length; ++position) {
    status =
        m3_regex_step(
            program,
            view,
            value,
            m3_mindex_value_char(view, value, position),
            position + 1U,
            length);
    if (status != M3_MINDEX_REGEX_OK) return status;
    if (m3_regex_contains(program->active, program->match_state)) {
      *out_match = 1;
      return M3_MINDEX_REGEX_OK;
    }
  }
  *out_match = 0;
  return M3_MINDEX_REGEX_OK;
}

static int m3_regex_find(
    m3_mindex_regex_program *program,
    const m3_mindex_view *view,
    uint32_t value,
    uint32_t length,
    int *out_match) {
  int status;
  memset(program->active, 0, (size_t)program->words * sizeof(uint64_t));

  for (uint32_t position = 0U; position <= length; ++position) {
    m3_regex_begin_closure(program);
    status =
        m3_regex_add_closure(
            program,
            program->active,
            (int32_t)program->start_state,
            view,
            value,
            position,
            length);
    if (status != M3_MINDEX_REGEX_OK) return status;
    if (m3_regex_contains(program->active, program->match_state)) {
      *out_match = 1;
      return M3_MINDEX_REGEX_OK;
    }
    if (position == length) break;

    status =
        m3_regex_step(
            program,
            view,
            value,
            m3_mindex_value_char(view, value, position),
            position + 1U,
            length);
    if (status != M3_MINDEX_REGEX_OK) return status;
    if (m3_regex_contains(program->active, program->match_state)) {
      *out_match = 1;
      return M3_MINDEX_REGEX_OK;
    }
  }

  *out_match = 0;
  return M3_MINDEX_REGEX_OK;
}

int m3_mindex_regex_execute(
    m3_mindex_regex_program *program,
    const m3_mindex_view *view,
    uint32_t value,
    int mode,
    int *out_match) {
  uint32_t length;
  if (program == NULL
      || view == NULL
      || out_match == NULL
      || value >= view->value_count
      || (mode != M3_MINDEX_REGEX_MATCHES
          && mode != M3_MINDEX_REGEX_FIND
          && mode != M3_MINDEX_REGEX_LOOKING_AT)) {
    return M3_MINDEX_REGEX_INVALID;
  }

  length = m3_mindex_value_utf16_length(view, value);
  if (m3_regex_surrogate_sensitive(program, view, value, length)) {
    return M3_MINDEX_REGEX_UNSUPPORTED;
  }

  if ((int64_t)length < (int64_t)program->min_length
      || (mode == M3_MINDEX_REGEX_MATCHES
          && program->max_length >= 0
          && (int64_t)length > (int64_t)program->max_length)
      || m3_regex_required_signal_missing(program, view, value)) {
    *out_match = 0;
    return M3_MINDEX_REGEX_OK;
  }

  switch (mode) {
    case M3_MINDEX_REGEX_MATCHES:
      return m3_regex_matches(program, view, value, length, out_match);
    case M3_MINDEX_REGEX_FIND:
      return m3_regex_find(program, view, value, length, out_match);
    case M3_MINDEX_REGEX_LOOKING_AT:
      return m3_regex_looking_at(program, view, value, length, out_match);
    default:
      return M3_MINDEX_REGEX_INVALID;
  }
}
