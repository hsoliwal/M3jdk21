// SPDX-License-Identifier: Apache-2.0
#include "m3fs_semantic.h"
#include "m3fs_internal.h"
#include "m3index_semantic_core.h"

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

static int m3_semantic_open(
    m3fs *fs,
    const char *path,
    unsigned char **owned,
    uint32_t *length,
    m3_mindex_view *view) {
  int status = m3fs_read_owned(fs, path, owned, length);
  if (status != M3FS_OK) return status;
  if (m3_mindex_open(*owned, (size_t)*length, view) != M3_MINDEX_REGEX_OK) {
    free(*owned);
    *owned = NULL;
    *length = 0U;
    return M3FS_CORRUPT;
  }
  return M3FS_OK;
}

int m3fs_mindex_info(
    m3fs *fs,
    const char *path,
    m3fs_mindex_info_data *out) {
  if (fs == NULL || path == NULL || out == NULL) return M3FS_INVALID;
  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_mindex_view view;
  int status = m3_semantic_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK) {
    out->value_count = view.value_count;
    out->token_count = view.token_count;
    out->utf16_count = view.utf16_count;
    out->image_bytes = length;
  }
  free(owned);
  return status;
}

int m3fs_mindex_value_info(
    m3fs *fs,
    const char *path,
    uint32_t value,
    m3fs_mindex_value *out) {
  if (fs == NULL || path == NULL || out == NULL) return M3FS_INVALID;
  memset(out, 0, sizeof(*out));

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_mindex_view view;
  int status = m3_semantic_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK && value >= view.value_count) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) {
    out->token_count = m3_mindex_value_token_count(&view, value);
    out->utf16_length = m3_mindex_value_utf16_length(&view, value);
    out->java_hash = m3_mindex_value_java_hash(&view, value);
    out->reserved = 0U;
    out->signal64 = m3_mindex_value_signal64(&view, value);
  }
  free(owned);
  return status;
}

int m3fs_mindex_token(
    m3fs *fs,
    const char *path,
    uint32_t value,
    uint32_t ordinal,
    uint64_t *out_token) {
  if (fs == NULL || path == NULL || out_token == NULL) return M3FS_INVALID;
  *out_token = UINT64_C(0);

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_mindex_view view;
  int status = m3_semantic_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK
      && (value >= view.value_count
          || ordinal >= m3_mindex_value_token_count(&view, value))) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) {
    *out_token = m3_mindex_value_token(&view, value, ordinal);
  }
  free(owned);
  return status;
}

int m3fs_mindex_char(
    m3fs *fs,
    const char *path,
    uint32_t value,
    uint32_t ordinal,
    uint16_t *out_unit) {
  if (fs == NULL || path == NULL || out_unit == NULL) return M3FS_INVALID;
  *out_unit = 0U;

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_mindex_view view;
  int status = m3_semantic_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK
      && (value >= view.value_count
          || ordinal >= m3_mindex_value_utf16_length(&view, value))) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) {
    *out_unit = m3_mindex_value_char(&view, value, ordinal);
  }
  free(owned);
  return status;
}

int m3fs_mindex_compare(
    m3fs *fs,
    const char *path,
    uint32_t left,
    uint32_t right,
    int32_t *out_comparison) {
  if (fs == NULL || path == NULL || out_comparison == NULL) return M3FS_INVALID;
  *out_comparison = 0;

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_mindex_view view;
  int status = m3_semantic_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK
      && (left >= view.value_count || right >= view.value_count)) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) {
    *out_comparison = (int32_t)m3_mindex_value_compare(&view, left, right);
  }
  free(owned);
  return status;
}

int m3fs_mindex_token_index_of(
    m3fs *fs,
    const char *path,
    uint32_t value,
    const uint64_t *needle,
    uint32_t needle_count,
    uint32_t from_token,
    int32_t *out_index) {
  if (fs == NULL || path == NULL || out_index == NULL
      || (needle_count != 0U && needle == NULL)) {
    return M3FS_INVALID;
  }
  *out_index = -1;

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_mindex_view view;
  int status = m3_semantic_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK && value >= view.value_count) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) {
    uint32_t token_count = m3_mindex_value_token_count(&view, value);
    if (from_token > token_count) {
      status = M3FS_INVALID;
    } else {
      *out_index =
          m3_mindex_token_index_of(
              &view, value, needle, needle_count, from_token);
    }
  }
  free(owned);
  return status;
}

static int m3_regex_mode(uint32_t mode) {
  switch (mode) {
    case M3FS_REGEX_MATCHES: return M3_MINDEX_REGEX_MATCHES;
    case M3FS_REGEX_FIND: return M3_MINDEX_REGEX_FIND;
    case M3FS_REGEX_LOOKING_AT: return M3_MINDEX_REGEX_LOOKING_AT;
    default: return -1;
  }
}

int m3fs_regex_match_all(
    m3fs *fs,
    const char *path,
    const m3fs_regex_program *spec,
    uint32_t mode,
    uint64_t *match_words,
    uint64_t *unsupported_words,
    uint32_t word_capacity,
    uint32_t *value_count,
    uint32_t *words_required) {
  if (fs == NULL || path == NULL || spec == NULL
      || value_count == NULL || words_required == NULL) {
    return M3FS_INVALID;
  }
  *value_count = 0U;
  *words_required = 0U;

  int native_mode = m3_regex_mode(mode);
  if (native_mode < 0) return M3FS_INVALID;

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_mindex_view view;
  int status = m3_semantic_open(fs, path, &owned, &length, &view);
  if (status != M3FS_OK) {
    free(owned);
    return status;
  }

  uint32_t words = (view.value_count + 63U) / 64U;
  *value_count = view.value_count;
  *words_required = words;
  if (word_capacity < words
      || (words != 0U && (match_words == NULL || unsupported_words == NULL))) {
    free(owned);
    return M3FS_NOSPACE;
  }
  if (words != 0U) {
    memset(match_words, 0, (size_t)words * sizeof(uint64_t));
    memset(unsupported_words, 0, (size_t)words * sizeof(uint64_t));
  }

  m3_mindex_regex_program program;
  int compiled =
      m3_mindex_regex_program_init(
          &program,
          spec->op,
          spec->out1,
          spec->out2,
          spec->class_id,
          spec->state_count,
          spec->ascii_low,
          spec->ascii_high,
          spec->range_offset,
          spec->range_count,
          spec->class_count,
          spec->range_low,
          spec->range_high,
          spec->range_count_total,
          spec->start_state,
          spec->match_state,
          spec->min_length,
          spec->max_length,
          spec->required_presence64,
          spec->may_consume_surrogate_unit != 0U);
  if (compiled == M3_MINDEX_REGEX_NO_MEMORY) {
    free(owned);
    return M3FS_NOMEM;
  }
  if (compiled != M3_MINDEX_REGEX_OK) {
    free(owned);
    return M3FS_INVALID;
  }

  for (uint32_t value = 0U; value < view.value_count; ++value) {
    int matched = 0;
    int result =
        m3_mindex_regex_execute(
            &program,
            &view,
            value,
            native_mode,
            &matched);
    if (result == M3_MINDEX_REGEX_OK) {
      if (matched != 0) {
        match_words[value >> 6] |= UINT64_C(1) << (value & 63U);
      }
    } else if (result == M3_MINDEX_REGEX_UNSUPPORTED) {
      unsupported_words[value >> 6] |= UINT64_C(1) << (value & 63U);
    } else {
      status = M3FS_CORRUPT;
      break;
    }
  }

  m3_mindex_regex_program_destroy(&program);
  free(owned);
  return status;
}
