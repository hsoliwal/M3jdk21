// SPDX-License-Identifier: Apache-2.0
#ifndef M3FS_INTERNAL_H
#define M3FS_INTERNAL_H

#include "m3fs.h"

#include <stdint.h>

#define M3FS_MAX_SEMANTIC_IMAGE_BYTES (512U * 1024U * 1024U)

/*
 * Internal same-generation full-file read. The returned buffer is malloc-owned by the caller.
 * Stat, inode resolution and payload read execute inside one existing M3FS read transaction.
 */
int m3fs_read_owned(
    m3fs *fs,
    const char *path,
    unsigned char **out_bytes,
    uint32_t *out_length);

#endif
