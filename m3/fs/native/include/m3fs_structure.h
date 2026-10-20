// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
#ifndef M3FS_STRUCTURE_H
#define M3FS_STRUCTURE_H

#include "m3fs.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct m3fs_structure_info_data {
  uint32_t domain_id;
  uint32_t node_count;
  uint32_t child_count;
  uint32_t text_token_count;
  uint32_t binary_byte_count;
  uint32_t image_bytes;
} m3fs_structure_info_data;

typedef struct m3fs_structure_node_data {
  uint32_t kind;
  uint32_t child_count;
  uint64_t first;
  uint64_t second;
  uint64_t semantic_hash64;
} m3fs_structure_node_data;

M3FS_EXPORT int m3fs_structure_info(
    m3fs *fs,
    const char *path,
    m3fs_structure_info_data *out);

M3FS_EXPORT int m3fs_structure_node(
    m3fs *fs,
    const char *path,
    uint32_t node_id,
    m3fs_structure_node_data *out);

M3FS_EXPORT int m3fs_structure_child(
    m3fs *fs,
    const char *path,
    uint32_t node_id,
    uint32_t ordinal,
    uint32_t *out_child_id);

M3FS_EXPORT int m3fs_structure_text_token(
    m3fs *fs,
    const char *path,
    uint32_t node_id,
    uint32_t ordinal,
    uint64_t *out_token);

M3FS_EXPORT int m3fs_structure_binary(
    m3fs *fs,
    const char *path,
    uint32_t node_id,
    void *output,
    uint32_t capacity,
    uint32_t *out_length);

M3FS_EXPORT int m3fs_structure_ref_target(
    m3fs *fs,
    const char *path,
    uint32_t node_id,
    uint32_t *out_target);

M3FS_EXPORT int m3fs_structure_map_value(
    m3fs *fs,
    const char *path,
    uint32_t map_node_id,
    uint32_t key_node_id,
    uint32_t *out_value_id);

#ifdef __cplusplus
}
#endif
#endif
