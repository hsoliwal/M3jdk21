// SPDX-License-Identifier: Apache-2.0
#define FUSE_USE_VERSION 31
#include <fuse.h>

#include "m3fs.h"

#include <errno.h>
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

typedef struct m3_fuse_context {
  m3fs *fs;
} m3_fuse_context;

static m3fs *m3_fuse_fs(void) {
  struct fuse_context *context = fuse_get_context();
  m3_fuse_context *private_data =
      context == NULL ? NULL : (m3_fuse_context *)context->private_data;
  return private_data == NULL ? NULL : private_data->fs;
}

static int m3_errno(int status) {
  switch (status) {
    case M3FS_OK: return 0;
    case M3FS_NOT_FOUND: return -ENOENT;
    case M3FS_EXISTS: return -EEXIST;
    case M3FS_NOT_DIRECTORY: return -ENOTDIR;
    case M3FS_IS_DIRECTORY: return -EISDIR;
    case M3FS_NOT_EMPTY: return -ENOTEMPTY;
    case M3FS_INVALID: return -EINVAL;
    case M3FS_READ_ONLY: return -EROFS;
    case M3FS_BUSY: return -EBUSY;
    case M3FS_NOSPACE: return -ENOSPC;
    case M3FS_STALE: return -ESTALE;
    case M3FS_NOMEM: return -ENOMEM;
    case M3FS_CORRUPT:
    case M3FS_IO:
    default: return -EIO;
  }
}

static mode_t m3_mode(const m3fs_stat *stat) {
  mode_t kind = 0;
  if (stat->kind == M3FS_KIND_FILE) kind = S_IFREG;
  else if (stat->kind == M3FS_KIND_DIRECTORY) kind = S_IFDIR;
  else if (stat->kind == M3FS_KIND_SYMLINK) kind = S_IFLNK;
  return kind | (mode_t)(stat->mode & UINT32_C(07777));
}

static void m3_fill_stat(const m3fs_stat *source, struct stat *target) {
  memset(target, 0, sizeof(*target));
  target->st_ino = (ino_t)source->inode;
  target->st_mode = m3_mode(source);
  target->st_nlink = (nlink_t)source->nlink;
  target->st_size = (off_t)source->size;
  target->st_blksize = (blksize_t)source->block_size;
  target->st_blocks = (blkcnt_t)((source->size + UINT64_C(511)) / UINT64_C(512));
#if defined(__APPLE__)
  target->st_mtimespec.tv_sec = (time_t)(source->mtime_ns / UINT64_C(1000000000));
  target->st_mtimespec.tv_nsec = (long)(source->mtime_ns % UINT64_C(1000000000));
  target->st_atimespec = target->st_mtimespec;
  target->st_ctimespec = target->st_mtimespec;
#else
  target->st_mtim.tv_sec = (time_t)(source->mtime_ns / UINT64_C(1000000000));
  target->st_mtim.tv_nsec = (long)(source->mtime_ns % UINT64_C(1000000000));
  target->st_atim = target->st_mtim;
  target->st_ctim = target->st_mtim;
#endif
}

static int m3_getattr(
    const char *path, struct stat *stbuf, struct fuse_file_info *fi) {
  (void)fi;
  m3fs_stat stat;
  int status = m3fs_stat_path(m3_fuse_fs(), path, &stat);
  if (status != M3FS_OK) return m3_errno(status);
  m3_fill_stat(&stat, stbuf);
  return 0;
}

typedef struct m3_readdir_context {
  void *buffer;
  fuse_fill_dir_t filler;
  int full;
} m3_readdir_context;

static int m3_readdir_entry(
    void *context,
    const char *name,
    uint32_t name_bytes,
    const m3fs_stat *stat) {
  m3_readdir_context *state = (m3_readdir_context *)context;
  if (name_bytes > M3FS_MAX_NAME_BYTES) return M3FS_CORRUPT;
  char terminated[M3FS_MAX_NAME_BYTES + 1U];
  memcpy(terminated, name, name_bytes);
  terminated[name_bytes] = '\0';
  struct stat buffer;
  m3_fill_stat(stat, &buffer);
  if (state->filler(
          state->buffer,
          terminated,
          &buffer,
          0,
          (enum fuse_fill_dir_flags)0)
      != 0) {
    state->full = 1;
  }
  return state->full ? M3FS_BUSY : M3FS_OK;
}

static int m3_readdir(
    const char *path,
    void *buffer,
    fuse_fill_dir_t filler,
    off_t offset,
    struct fuse_file_info *fi,
    enum fuse_readdir_flags flags) {
  (void)offset;
  (void)fi;
  (void)flags;
  if (filler(buffer, ".", NULL, 0, (enum fuse_fill_dir_flags)0) != 0
      || filler(buffer, "..", NULL, 0, (enum fuse_fill_dir_flags)0) != 0) {
    return 0;
  }
  m3_readdir_context context = {buffer, filler, 0};
  int status = m3fs_list(m3_fuse_fs(), path, m3_readdir_entry, &context);
  if (context.full && status == M3FS_BUSY) return 0;
  return m3_errno(status);
}

static int m3_open(const char *path, struct fuse_file_info *fi) {
  m3fs_stat stat;
  int status = m3fs_stat_path(m3_fuse_fs(), path, &stat);
  if (status != M3FS_OK) return m3_errno(status);
  if (stat.kind == M3FS_KIND_DIRECTORY) return -EISDIR;
  if (stat.kind != M3FS_KIND_FILE) return -EINVAL;
  (void)fi;
  return 0;
}

static int m3_create(
    const char *path, mode_t mode, struct fuse_file_info *fi) {
  (void)fi;
  return m3_errno(
      m3fs_create(m3_fuse_fs(), path, (uint32_t)(mode & 07777)));
}

static int m3_mkdir(const char *path, mode_t mode) {
  return m3_errno(
      m3fs_mkdir(m3_fuse_fs(), path, (uint32_t)(mode & 07777)));
}

static int m3_read(
    const char *path,
    char *buffer,
    size_t size,
    off_t offset,
    struct fuse_file_info *fi) {
  (void)fi;
  if (offset < 0 || size > (size_t)INT_MAX) return -EINVAL;
  size_t total = 0U;
  while (total < size) {
    size_t remaining = size - total;
    uint32_t chunk = remaining > UINT32_MAX ? UINT32_MAX : (uint32_t)remaining;
    uint32_t actual = 0U;
    int status =
        m3fs_read(
            m3_fuse_fs(),
            path,
            (uint64_t)offset + (uint64_t)total,
            buffer + total,
            chunk,
            &actual);
    if (status != M3FS_OK) return total == 0U ? m3_errno(status) : (int)total;
    total += actual;
    if (actual < chunk) break;
    if (total > (size_t)INT_MAX) return -EOVERFLOW;
  }
  return (int)total;
}

static int m3_write(
    const char *path,
    const char *buffer,
    size_t size,
    off_t offset,
    struct fuse_file_info *fi) {
  (void)fi;
  if (offset < 0 || size > (size_t)INT_MAX) return -EINVAL;
  size_t total = 0U;
  while (total < size) {
    size_t remaining = size - total;
    uint32_t chunk = remaining > UINT32_MAX ? UINT32_MAX : (uint32_t)remaining;
    int status =
        m3fs_write(
            m3_fuse_fs(),
            path,
            (uint64_t)offset + (uint64_t)total,
            buffer + total,
            chunk,
            NULL);
    if (status != M3FS_OK) return total == 0U ? m3_errno(status) : (int)total;
    total += chunk;
  }
  return (int)total;
}

static int m3_truncate(
    const char *path, off_t size, struct fuse_file_info *fi) {
  (void)fi;
  if (size < 0) return -EINVAL;
  return m3_errno(m3fs_truncate(m3_fuse_fs(), path, (uint64_t)size));
}

static int m3_unlink(const char *path) {
  return m3_errno(m3fs_unlink(m3_fuse_fs(), path));
}

static int m3_rmdir(const char *path) {
  return m3_errno(m3fs_rmdir(m3_fuse_fs(), path));
}

static int m3_rename(const char *from, const char *to, unsigned int flags) {
  if (flags != 0U) return -EINVAL;
  return m3_errno(m3fs_rename(m3_fuse_fs(), from, to));
}

static int m3_link(const char *from, const char *to) {
  return m3_errno(m3fs_link(m3_fuse_fs(), from, to));
}

static int m3_symlink(const char *target, const char *link_path) {
  return m3_errno(m3fs_symlink(m3_fuse_fs(), target, link_path));
}

static int m3_readlink(const char *path, char *buffer, size_t size) {
  if (size == 0U) return -EINVAL;
  uint32_t bytes = 0U;
  uint32_t capacity = size - 1U > UINT32_MAX ? UINT32_MAX : (uint32_t)(size - 1U);
  int status = m3fs_readlink(m3_fuse_fs(), path, buffer, capacity, &bytes);
  if (status != M3FS_OK) return m3_errno(status);
  buffer[bytes] = '\0';
  return 0;
}

static int m3_fsync(
    const char *path, int datasync, struct fuse_file_info *fi) {
  (void)path;
  (void)datasync;
  (void)fi;
  return m3_errno(m3fs_checkpoint(m3_fuse_fs()));
}

static const struct fuse_operations M3_OPERATIONS = {
    .getattr = m3_getattr,
    .readdir = m3_readdir,
    .open = m3_open,
    .create = m3_create,
    .mkdir = m3_mkdir,
    .read = m3_read,
    .write = m3_write,
    .truncate = m3_truncate,
    .unlink = m3_unlink,
    .rmdir = m3_rmdir,
    .rename = m3_rename,
    .link = m3_link,
    .symlink = m3_symlink,
    .readlink = m3_readlink,
    .fsync = m3_fsync,
};

int main(int argc, char **argv) {
  if (argc < 3) {
    fprintf(stderr, "usage: %s DATABASE MOUNTPOINT [FUSE options]\n", argv[0]);
    return 2;
  }

  m3fs *fs = NULL;
  int status = m3fs_open(argv[1], 0U, &fs);
  if (status != M3FS_OK) {
    fprintf(stderr, "open: %s\n", m3fs_status_string(status));
    return 1;
  }

  int fuse_argc = argc - 1;
  char **fuse_argv = (char **)calloc((size_t)fuse_argc + 1U, sizeof(char *));
  if (fuse_argv == NULL) {
    m3fs_close(fs);
    return 1;
  }
  fuse_argv[0] = argv[0];
  fuse_argv[1] = argv[2];
  for (int index = 3; index < argc; ++index) {
    fuse_argv[index - 1] = argv[index];
  }

  m3_fuse_context context = {fs};
  int result = fuse_main(fuse_argc, fuse_argv, &M3_OPERATIONS, &context);
  free(fuse_argv);
  m3fs_close(fs);
  return result;
}
