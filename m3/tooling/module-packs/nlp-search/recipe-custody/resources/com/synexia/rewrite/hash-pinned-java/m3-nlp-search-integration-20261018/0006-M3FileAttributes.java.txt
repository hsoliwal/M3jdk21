// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable filesystem metadata snapshot associated with a one-pass M3File admission.
 *
 * <p>The portable basic attributes are first-class fields. Optional POSIX, DOS, ACL, user-defined
 * and provider-specific Unix attributes are captured when the active filesystem exposes those
 * views. Unsupported views are absent; values are never invented.</p>
 */
public final class M3FileAttributes {
  private final String normalizedPath;
  private final long size;
  private final FileTime creationTime;
  private final FileTime lastModifiedTime;
  private final FileTime lastAccessTime;
  private final boolean regularFile;
  private final boolean directory;
  private final boolean symbolicLinkPath;
  private final boolean other;
  private final String fileKey;
  private final String owner;
  private final String group;
  private final boolean posixAvailable;
  private final EnumSet<PosixFilePermission> posixPermissions;
  private final boolean dosAvailable;
  private final boolean dosArchive;
  private final boolean dosHidden;
  private final boolean dosReadOnly;
  private final boolean dosSystem;
  private final List<String> aclEntries;
  private final Map<String, String> userDefinedAttributesBase64;
  private final Map<String, String> providerAttributes;

  private M3FileAttributes(
      String normalizedPath,
      long size,
      FileTime creationTime,
      FileTime lastModifiedTime,
      FileTime lastAccessTime,
      boolean regularFile,
      boolean directory,
      boolean symbolicLinkPath,
      boolean other,
      String fileKey,
      String owner,
      String group,
      boolean posixAvailable,
      EnumSet<PosixFilePermission> posixPermissions,
      boolean dosAvailable,
      boolean dosArchive,
      boolean dosHidden,
      boolean dosReadOnly,
      boolean dosSystem,
      List<String> aclEntries,
      Map<String, String> userDefinedAttributesBase64,
      Map<String, String> providerAttributes) {
    this.normalizedPath = Objects.requireNonNull(normalizedPath, "normalizedPath");
    this.size = size;
    this.creationTime = Objects.requireNonNull(creationTime, "creationTime");
    this.lastModifiedTime = Objects.requireNonNull(lastModifiedTime, "lastModifiedTime");
    this.lastAccessTime = Objects.requireNonNull(lastAccessTime, "lastAccessTime");
    this.regularFile = regularFile;
    this.directory = directory;
    this.symbolicLinkPath = symbolicLinkPath;
    this.other = other;
    this.fileKey = fileKey;
    this.owner = owner;
    this.group = group;
    this.posixAvailable = posixAvailable;
    this.posixPermissions =
        posixPermissions.isEmpty()
            ? EnumSet.noneOf(PosixFilePermission.class)
            : EnumSet.copyOf(posixPermissions);
    this.dosAvailable = dosAvailable;
    this.dosArchive = dosArchive;
    this.dosHidden = dosHidden;
    this.dosReadOnly = dosReadOnly;
    this.dosSystem = dosSystem;
    this.aclEntries = List.copyOf(aclEntries);
    this.userDefinedAttributesBase64 =
        immutableSortedMap(userDefinedAttributesBase64);
    this.providerAttributes = immutableSortedMap(providerAttributes);
  }

  public static M3FileAttributes capture(Path path) throws IOException {
    Path source = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    BasicFileAttributes basic = Files.readAttributes(source, BasicFileAttributes.class);
    FileStore store = Files.getFileStore(source);

    String owner = null;
    FileOwnerAttributeView ownerView =
        Files.getFileAttributeView(source, FileOwnerAttributeView.class);
    if (ownerView != null) owner = ownerView.getOwner().getName();

    boolean posixAvailable = store.supportsFileAttributeView(PosixFileAttributeView.class);
    String group = null;
    EnumSet<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
    if (posixAvailable) {
      PosixFileAttributes posix = Files.readAttributes(source, PosixFileAttributes.class);
      group = posix.group().getName();
      permissions.addAll(posix.permissions());
    }

    boolean dosAvailable = store.supportsFileAttributeView(DosFileAttributeView.class);
    boolean dosArchive = false;
    boolean dosHidden = false;
    boolean dosReadOnly = false;
    boolean dosSystem = false;
    if (dosAvailable) {
      DosFileAttributes dos = Files.readAttributes(source, DosFileAttributes.class);
      dosArchive = dos.isArchive();
      dosHidden = dos.isHidden();
      dosReadOnly = dos.isReadOnly();
      dosSystem = dos.isSystem();
    }

    List<String> aclEntries = new ArrayList<>();
    if (store.supportsFileAttributeView(AclFileAttributeView.class)) {
      AclFileAttributeView aclView =
          Files.getFileAttributeView(source, AclFileAttributeView.class);
      if (aclView != null) {
        for (AclEntry entry : aclView.getAcl()) aclEntries.add(entry.toString());
      }
    }

    Map<String, String> userAttributes = new TreeMap<>();
    UserDefinedFileAttributeView userView =
        Files.getFileAttributeView(source, UserDefinedFileAttributeView.class);
    if (userView != null) {
      List<String> names = new ArrayList<>(userView.list());
      Collections.sort(names);
      for (String name : names) {
        int length = userView.size(name);
        ByteBuffer buffer = ByteBuffer.allocate(length);
        int read = userView.read(name, buffer);
        if (read != length) {
          throw new IOException(
              "user attribute length changed while reading: " + name + " expected=" + length
                  + " actual=" + read);
        }
        buffer.flip();
        byte[] value = new byte[buffer.remaining()];
        buffer.get(value);
        userAttributes.put(name, Base64.getEncoder().encodeToString(value));
      }
    }

    Map<String, String> providerAttributes = new TreeMap<>();
    try {
      Map<String, Object> unix = Files.readAttributes(source, "unix:*");
      for (Map.Entry<String, Object> entry : unix.entrySet()) {
        providerAttributes.put("unix." + entry.getKey(), canonicalValue(entry.getValue()));
      }
    } catch (UnsupportedOperationException | IllegalArgumentException ignored) {
      // Provider has no unix:* namespace.
    }
    if (Files.isSymbolicLink(source)) {
      providerAttributes.put("symlink.target", Files.readSymbolicLink(source).toString());
    }

    Object key = basic.fileKey();
    return new M3FileAttributes(
        source.toString(),
        basic.size(),
        basic.creationTime(),
        basic.lastModifiedTime(),
        basic.lastAccessTime(),
        basic.isRegularFile(),
        basic.isDirectory(),
        Files.isSymbolicLink(source),
        basic.isOther(),
        key == null ? null : key.toString(),
        owner,
        group,
        posixAvailable,
        permissions,
        dosAvailable,
        dosArchive,
        dosHidden,
        dosReadOnly,
        dosSystem,
        aclEntries,
        userAttributes,
        providerAttributes);
  }

  /** Restore the exact attributes carried in a verified source-facts image. */
  static M3FileAttributes restore(
      String normalizedPath,
      long size,
      FileTime creationTime,
      FileTime lastModifiedTime,
      FileTime lastAccessTime,
      boolean regularFile,
      boolean directory,
      boolean symbolicLinkPath,
      boolean other,
      String fileKey,
      String owner,
      String group,
      boolean posixAvailable,
      EnumSet<PosixFilePermission> posixPermissions,
      boolean dosAvailable,
      boolean dosArchive,
      boolean dosHidden,
      boolean dosReadOnly,
      boolean dosSystem,
      List<String> aclEntries,
      Map<String, String> userDefinedAttributesBase64,
      Map<String, String> providerAttributes) {
    return new M3FileAttributes(
        normalizedPath,
        size,
        creationTime,
        lastModifiedTime,
        lastAccessTime,
        regularFile,
        directory,
        symbolicLinkPath,
        other,
        fileKey,
        owner,
        group,
        posixAvailable,
        posixPermissions,
        dosAvailable,
        dosArchive,
        dosHidden,
        dosReadOnly,
        dosSystem,
        aclEntries,
        userDefinedAttributesBase64,
        providerAttributes);
  }

  public String normalizedPath() {
    return normalizedPath;
  }

  public long size() {
    return size;
  }

  public FileTime creationTime() {
    return creationTime;
  }

  public FileTime lastModifiedTime() {
    return lastModifiedTime;
  }

  public FileTime lastAccessTime() {
    return lastAccessTime;
  }

  public boolean isRegularFile() {
    return regularFile;
  }

  public boolean isDirectory() {
    return directory;
  }

  public boolean isSymbolicLinkPath() {
    return symbolicLinkPath;
  }

  public boolean isOther() {
    return other;
  }

  public String fileKey() {
    return fileKey;
  }

  public String owner() {
    return owner;
  }

  public String group() {
    return group;
  }

  public boolean posixAvailable() {
    return posixAvailable;
  }

  public java.util.Set<PosixFilePermission> posixPermissions() {
    return Collections.unmodifiableSet(posixPermissions);
  }

  public boolean dosAvailable() {
    return dosAvailable;
  }

  public boolean dosArchive() {
    return dosArchive;
  }

  public boolean dosHidden() {
    return dosHidden;
  }

  public boolean dosReadOnly() {
    return dosReadOnly;
  }

  public boolean dosSystem() {
    return dosSystem;
  }

  public List<String> aclEntries() {
    return aclEntries;
  }

  /** Exact user-defined attribute payloads encoded as Base64, keyed by attribute name. */
  public Map<String, String> userDefinedAttributesBase64() {
    return userDefinedAttributesBase64;
  }

  /** Additional provider-specific scalar/collection attributes in canonical textual form. */
  public Map<String, String> providerAttributes() {
    return providerAttributes;
  }

  /**
   * Content-stability identity used by the before/after admission gate.
   *
   * <p>Access time is intentionally excluded because reading the payload may itself update it.</p>
   */
  public boolean sameContentIdentity(M3FileAttributes other) {
    Objects.requireNonNull(other, "other");
    return size == other.size
        && regularFile == other.regularFile
        && directory == other.directory
        && symbolicLinkPath == other.symbolicLinkPath
        && this.other == other.other
        && Objects.equals(fileKey, other.fileKey)
        && creationTime.equals(other.creationTime)
        && lastModifiedTime.equals(other.lastModifiedTime);
  }

  private static Map<String, String> immutableSortedMap(Map<String, String> source) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(source)));
  }

  private static String canonicalValue(Object value) {
    if (value == null) return "null";
    if (value instanceof byte[] bytes) {
      return "base64:" + Base64.getEncoder().encodeToString(bytes);
    }
    if (value instanceof Collection<?> values) {
      List<String> rendered = values.stream().map(M3FileAttributes::canonicalValue).sorted().toList();
      return String.join(",", rendered);
    }
    if (value.getClass().isArray()) {
      int length = Array.getLength(value);
      List<String> rendered = new ArrayList<>(length);
      for (int i = 0; i < length; i++) rendered.add(canonicalValue(Array.get(value, i)));
      Collections.sort(rendered);
      return String.join(",", rendered);
    }
    return String.valueOf(value);
  }
}
