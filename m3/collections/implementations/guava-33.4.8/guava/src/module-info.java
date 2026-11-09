/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Google Guava */
module jdk.m3.collections.guava {
  requires java.logging;
  requires transitive jdk.m3.collections.guava.util.concurrent.internal;
  requires static jdk.unsupported;
  requires static com.google.errorprone.annotations;
  requires static com.google.j2objc.annotations;
  requires static org.jspecify;

  exports jdk.m3.collections.guava.annotations;
  exports jdk.m3.collections.guava.base;
  exports jdk.m3.collections.guava.cache;
  exports jdk.m3.collections.guava.collect;
  exports jdk.m3.collections.guava.escape;
  exports jdk.m3.collections.guava.eventbus;
  exports jdk.m3.collections.guava.graph;
  exports jdk.m3.collections.guava.hash;
  exports jdk.m3.collections.guava.html;
  exports jdk.m3.collections.guava.io;
  exports jdk.m3.collections.guava.math;
  exports jdk.m3.collections.guava.net;
  exports jdk.m3.collections.guava.primitives;
  exports jdk.m3.collections.guava.reflect;
  exports jdk.m3.collections.guava.util.concurrent;
  exports jdk.m3.collections.guava.xml;
}
