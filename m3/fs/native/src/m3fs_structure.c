// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
#include "m3fs_structure.h"
#include "m3fs_internal.h"
#include "m3structure_semantic_core.h"

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

static int m3_structure_image_open(
    m3fs *fs,
    const char *path,
    unsigned char **owned,
    uint32_t *length,
    m3_structure_view *view) {
  int status = m3fs_read_owned(fs, path, owned, length);
  if (status != M3FS_OK) return status;
  if (!m3_structure_open(*owned, (size_t)*length, view)) {
    free(*owned);
    *owned = NULL;
    *length = 0U;
    return M3FS_CORRUPT;
  }
  return M3FS_OK;
}

int m3fs_structure_info(
    m3fs *fs,
    const char *path,
    m3fs_structure_info_data *out) {
  if (fs == NULL || path == NULL || out == NULL) return M3FS_INVALID;
  memset(out, 0, sizeof(*out));

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_structure_view view;
  int status = m3_structure_image_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK) {
    out->domain_id = view.domain_id;
    out->node_count = view.node_count;
    out->child_count = view.child_count;
    out->text_token_count = view.text_token_count;
    out->binary_byte_count = view.binary_byte_count;
    out->image_bytes = length;
  }
  free(owned);
  return status;
}

int m3fs_structure_node(
    m3fs *fs,
    const char *path,
    uint32_t node_id,
    m3fs_structure_node_data *out) {
  if (fs == NULL || path == NULL || out == NULL) return M3FS_INVALID;
  memset(out, 0, sizeof(*out));

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_structure_view view;
  int status = m3_structure_image_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK && (node_id == 0U || node_id > view.node_count)) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) {
    out->kind = m3_structure_kind(&view, node_id);
    out->child_count = m3_structure_child_count(&view, node_id);
    out->first = m3_structure_first(&view, node_id);
    out->second = m3_structure_second(&view, node_id);
    out->semantic_hash64 = m3_structure_hash(&view, node_id);
  }
  free(owned);
  return status;
}

int m3fs_structure_child(
    m3fs *fs,
    const char *path,
    uint32_t node_id,
    uint32_t ordinal,
    uint32_t *out_child_id) {
  if (fs == NULL || path == NULL || out_child_id == NULL) return M3FS_INVALID;
  *out_child_id = 0U;

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_structure_view view;
  int status = m3_structure_image_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK
      && (node_id == 0U
          || node_id > view.node_count
          || ordinal >= m3_structure_child_count(&view, node_id))) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) {
    *out_child_id = m3_structure_child(&view, node_id, ordinal);
  }
  free(owned);
  return status;
}

int m3fs_structure_text_token(
    m3fs *fs,
    const char *path,
    uint32_t node_id,
    uint32_t ordinal,
    uint64_t *out_token) {
  if (fs == NULL || path == NULL || out_token == NULL) return M3FS_INVALID;
  *out_token = UINT64_C(0);

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_structure_view view;
  int status = m3_structure_image_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK
      && (node_id == 0U
          || node_id > view.node_count
          || ordinal >= m3_structure_text_token_count(&view, node_id))) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) {
    *out_token = m3_structure_text_token(&view, node_id, ordinal);
  }
  free(owned);
  return status;
}

int m3fs_structure_binary(
    m3fs *fs,
    const char *path,
    uint32_t node_id,
    void *output,
    uint32_t capacity,
    uint32_t *out_length) {
  if (fs == NULL || path == NULL || out_length == NULL
      || (capacity != 0U && output == NULL)) {
    return M3FS_INVALID;
  }
  *out_length = 0U;

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_structure_view view;
  int status = m3_structure_image_open(fs, path, &owned, &length, &view);
  uint32_t needed = 0U;
  if (status == M3FS_OK) {
    if (node_id == 0U || node_id > view.node_count
        || m3_structure_kind(&view, node_id) != M3_STRUCTURE_BINARY) {
      status = M3FS_INVALID;
    } else {
      needed = m3_structure_binary_count(&view, node_id);
      *out_length = needed;
      if (capacity < needed) status = M3FS_NOSPACE;
    }
  }
  if (status == M3FS_OK) {
    unsigned char *bytes = (unsigned char *)output;
    for (uint32_t ordinal = 0U; ordinal < needed; ++ordinal) {
      bytes[ordinal] = m3_structure_binary_byte(&view, node_id, ordinal);
    }
  }
  free(owned);
  return status;
}

int m3fs_structure_ref_target(
    m3fs *fs,
    const char *path,
    uint32_t node_id,
    uint32_t *out_target) {
  if (fs == NULL || path == NULL || out_target == NULL) return M3FS_INVALID;
  *out_target = 0U;

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_structure_view view;
  int status = m3_structure_image_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK
      && (node_id == 0U
          || node_id > view.node_count
          || m3_structure_kind(&view, node_id) != M3_STRUCTURE_REF)) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) {
    *out_target = m3_structure_ref_target(&view, node_id);
    if (*out_target == 0U) status = M3FS_CORRUPT;
  }
  free(owned);
  return status;
}

int m3fs_structure_map_value(
    m3fs *fs,
    const char *path,
    uint32_t map_node_id,
    uint32_t key_node_id,
    uint32_t *out_value_id) {
  if (fs == NULL || path == NULL || out_value_id == NULL) return M3FS_INVALID;
  *out_value_id = 0U;

  unsigned char *owned = NULL;
  uint32_t length = 0U;
  m3_structure_view view;
  int status = m3_structure_image_open(fs, path, &owned, &length, &view);
  if (status == M3FS_OK
      && (map_node_id == 0U
          || map_node_id > view.node_count
          || key_node_id == 0U
          || key_node_id > view.node_count
          || m3_structure_kind(&view, map_node_id) != M3_STRUCTURE_MAP)) {
    status = M3FS_INVALID;
  }
  if (status == M3FS_OK) {
    *out_value_id = m3_structure_map_value(&view, map_node_id, key_node_id);
  }
  free(owned);
  return status;
}
