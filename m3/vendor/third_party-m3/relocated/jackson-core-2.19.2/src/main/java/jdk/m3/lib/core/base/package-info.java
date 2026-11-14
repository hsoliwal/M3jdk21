/**
 * Base classes used by concrete Parser and Generator implementations;
 * contain functionality that is not specific to JSON or input
 * abstraction (byte vs char).
 * Most formats extend these types, although it is also possible to
 * directly extend {@link jdk.m3.lib.core.JsonParser} or
 * {@link jdk.m3.lib.core.JsonGenerator}.
 */
package jdk.m3.lib.core.base;
