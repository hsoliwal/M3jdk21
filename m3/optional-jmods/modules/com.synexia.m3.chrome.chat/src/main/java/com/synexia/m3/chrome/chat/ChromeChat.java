// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.chrome.chat;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dependency-free Java 21 client for the local Chrome2api/ChromeML-compatible HTTP interface.
 * It never opens a browser, loads a model, starts a native process or downloads weights.
 * The only permitted destination is literal IPv4 loopback at a caller-selected TCP port.
 * A caller must run and trust its local server separately.
 */
public final class ChromeChat implements AutoCloseable {
    private static final int MAX_RESPONSE_BYTES = 1_048_576;
    private static final int MAX_CONTENT_CHARS = 32_768;
    private static final int MAX_MESSAGES = 64;
    private static final int MAX_DEPTH = 24;
    private final URI endpoint;
    private final HttpClient client;

    /** Explicit port. No DNS, HTTP redirects, remote server or proxy routing. */
    public ChromeChat(int port) {
        if (port < 1 || port > 65_535) throw new IllegalArgumentException("TCP port");
        endpoint = URI.create("http://127.0.0.1:" + port + "/v1/chat/completions");
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(new ProxySelector() {
                    @Override public List<Proxy> select(URI uri) {
                        if (!endpoint.equals(uri)) throw new IllegalArgumentException("non-loopback URI");
                        return List.of(Proxy.NO_PROXY);
                    }
                    @Override public void connectFailed(URI uri, SocketAddress address, IOException cause) {
                        throw new IllegalStateException("loopback connection failed", cause);
                    }
                }).build();
    }

    /** Immutable role/content pair. Only the four Chrome2api-compatible roles are permitted. */
    public record Message(String role, String content) {
        public Message {
            if (!List.of("system", "user", "assistant", "tool").contains(role))
                throw new IllegalArgumentException("unsupported role");
            if (content == null || content.isBlank() || content.length() > MAX_CONTENT_CHARS)
                throw new IllegalArgumentException("message content length");
            validateUtf16(content);
        }
    }

    /** Bound response with the model, assistant text and optional finish reason. */
    public record Completion(String model, String content, String finishReason) {
        public Completion {
            Objects.requireNonNull(model);
            Objects.requireNonNull(content);
        }
    }

    /** Non-streaming chat completion. IOException means no successful response was accepted. */
    public Completion complete(String model, List<Message> messages, int maxTokens)
            throws IOException, InterruptedException {
        String json = requestJson(model, messages, maxTokens);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        byte[] bounded;
        try (InputStream stream = response.body()) {
            bounded = stream.readNBytes(MAX_RESPONSE_BYTES + 1);
        }
        if (bounded.length > MAX_RESPONSE_BYTES) throw new IOException("ChromeML response limit exceeded");
        if (response.statusCode() != 200) throw new IOException("ChromeML HTTP status " + response.statusCode());
        try {
            String body = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bounded)).toString();
            return parseCompletion(body);
        } catch (CharacterCodingException | IllegalArgumentException ex) {
            throw new IOException("invalid ChromeML JSON response", ex);
        }
    }

    /** Deterministic strict request projection; public for offline recipe/contract tests. */
    public static String requestJson(String model, List<Message> messages, int maxTokens) {
        if (model == null || !model.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
            throw new IllegalArgumentException("model identifier");
        Objects.requireNonNull(messages, "messages");
        if (messages.isEmpty() || messages.size() > MAX_MESSAGES)
            throw new IllegalArgumentException("message count");
        if (maxTokens < 1 || maxTokens > 8_192) throw new IllegalArgumentException("maxTokens");
        StringBuilder json = new StringBuilder(128);
        json.append("{\"model\":").append(quote(model)).append(",\"stream\":false,\"max_tokens\":")
                .append(maxTokens).append(",\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            Message msg = Objects.requireNonNull(messages.get(i), "message");
            if (i != 0) json.append(',');
            json.append("{\"role\":").append(quote(msg.role())).append(",\"content\":")
                    .append(quote(msg.content())).append('}');
        }
        return json.append("]}").toString();
    }

    /** Parse only a successful OpenAI-compatible JSON message; never infer a success from error data. */
    public static Completion parseCompletion(String body) {
        if (body == null || body.getBytes(StandardCharsets.UTF_8).length > MAX_RESPONSE_BYTES)
            throw new IllegalArgumentException("JSON size");
        Object decoded = new Json(body).parse();
        Map<?, ?> root = expectMap(decoded, "root");
        String model = expectString(root.get("model"), "model");
        List<?> choices = expectList(root.get("choices"), "choices");
        if (choices.size() != 1) throw new IllegalArgumentException("expected exactly one choice");
        Map<?, ?> choice = expectMap(choices.get(0), "choice");
        Map<?, ?> message = expectMap(choice.get("message"), "message");
        if (!"assistant".equals(message.get("role"))) throw new IllegalArgumentException("assistant role");
        String content = expectString(message.get("content"), "content");
        String finish = choice.get("finish_reason") instanceof String value ? value : null;
        return new Completion(model, content, finish);
    }

    private static Map<?, ?> expectMap(Object value, String name) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException(name + " object");
        return map;
    }
    private static List<?> expectList(Object value, String name) {
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException(name + " array");
        return list;
    }
    private static String expectString(Object value, String name) {
        if (!(value instanceof String text)) throw new IllegalArgumentException(name + " string");
        return text;
    }
    private static String quote(String value) {
        validateUtf16(value);
        StringBuilder builder = new StringBuilder(value.length() + 8).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append("\\u");
                        builder.append("0123456789abcdef".charAt((c >> 12) & 15));
                        builder.append("0123456789abcdef".charAt((c >> 8) & 15));
                        builder.append("0123456789abcdef".charAt((c >> 4) & 15));
                        builder.append("0123456789abcdef".charAt(c & 15));
                    } else builder.append(c);
                }
            }
        }
        return builder.append('"').toString();
    }
    private static void validateUtf16(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isHighSurrogate(ch)) {
                if (++i == text.length() || !Character.isLowSurrogate(text.charAt(i)))
                    throw new IllegalArgumentException("unpaired UTF-16 surrogate");
            } else if (Character.isLowSurrogate(ch)) {
                throw new IllegalArgumentException("unpaired UTF-16 surrogate");
            }
        }
    }

    @Override public void close() { client.close(); }

    /** No model/native/runtime needed for this offline build oracle. */
    public static void main(String[] args) {
        if (args.length != 1 || !"--self-test".equals(args[0]))
            throw new IllegalArgumentException("--self-test");
        ChromeChat sample = new ChromeChat(11435);
        try {
            String request = requestJson("chrome-gemini-nano", List.of(new Message("user", "A\"\nB")), 32);
            if (!request.contains("A\\\"\\nB") || !request.contains("\"stream\":false"))
                throw new AssertionError("request projection");
            Completion completion = parseCompletion("{\"model\":\"chrome-gemini-nano\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\\n\\u263A\"},\"finish_reason\":\"stop\"}]}");
            if (!"ok\n☺".equals(completion.content())) throw new AssertionError("JSON parse");
            System.out.println("M3_JMOD_TEST_PASS chrome.chat");
        } finally { sample.close(); }
    }

    /** Strict bounded JSON reader; internal only because Java 21 has no standard JSON decoder. */
    private static final class Json {
        private final String data;
        private int index;
        Json(String data) { this.data = data; }
        Object parse() {
            Object value = read(0);
            ws();
            if (index != data.length()) throw bad();
            return value;
        }
        private Object read(int depth) {
            if (depth > MAX_DEPTH) throw bad();
            ws();
            if (index >= data.length()) throw bad();
            char ch = data.charAt(index);
            if (ch == '"') return string();
            if (ch == '{') {
                index++;
                Map<String, Object> map = new LinkedHashMap<>();
                ws();
                if (consume('}')) return map;
                do {
                    ws();
                    if (index >= data.length() || data.charAt(index) != '"') throw bad();
                    String key = string();
                    ws();
                    required(':');
                    Object next = read(depth + 1);
                    if (map.containsKey(key)) throw bad();
                    map.put(key, next);
                    ws();
                    if (consume('}')) return map;
                    required(',');
                } while (true);
            }
            if (ch == '[') {
                index++;
                List<Object> list = new ArrayList<>();
                ws();
                if (consume(']')) return list;
                do {
                    list.add(read(depth + 1));
                    ws();
                    if (consume(']')) return list;
                    required(',');
                } while (true);
            }
            if (data.startsWith("true", index)) { index += 4; return Boolean.TRUE; }
            if (data.startsWith("false", index)) { index += 5; return Boolean.FALSE; }
            if (data.startsWith("null", index)) { index += 4; return null; }
            if (ch == '-' || ch >= '0' && ch <= '9') {
                int start = index;
                if (consume('-') && index >= data.length()) throw bad();
                if (consume('0')) { /* one leading zero */ }
                else {
                    if (index >= data.length() || data.charAt(index) < '1' || data.charAt(index) > '9') throw bad();
                    while (index < data.length() && Character.isDigit(data.charAt(index))) index++;
                }
                if (consume('.')) {
                    int digits = index;
                    while (index < data.length() && Character.isDigit(data.charAt(index))) index++;
                    if (digits == index) throw bad();
                }
                if (consume('e') || consume('E')) {
                    if (!consume('+')) consume('-');
                    int digits = index;
                    while (index < data.length() && Character.isDigit(data.charAt(index))) index++;
                    if (digits == index) throw bad();
                }
                return data.substring(start, index);
            }
            throw bad();
        }
        private String string() {
            required('"');
            StringBuilder out = new StringBuilder();
            boolean terminated = false;
            while (index < data.length()) {
                char c = data.charAt(index++);
                if (c == '"') { terminated = true; break; }
                if (c == '\\') {
                    if (index >= data.length()) throw bad();
                    char escape = data.charAt(index++);
                    c = switch (escape) {
                        case '"', '\\', '/' -> escape;
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case 'u' -> {
                            if (index + 4 > data.length()) throw bad();
                            int value = 0;
                            for (int j = 0; j < 4; j++) {
                                int d = Character.digit(data.charAt(index++), 16);
                                if (d < 0) throw bad();
                                value = value * 16 + d;
                            }
                            yield (char) value;
                        }
                        default -> throw bad();
                    };
                } else if (c < 0x20) throw bad();
                out.append(c);
            }
            if (!terminated) throw bad();
            String text = out.toString();
            validateUtf16(text);
            return text;
        }
        private void ws() {
            while (index < data.length() && " \n\r\t".indexOf(data.charAt(index)) >= 0) index++;
        }
        private boolean consume(char c) {
            if (index < data.length() && data.charAt(index) == c) { index++; return true; }
            return false;
        }
        private void required(char c) { if (!consume(c)) throw bad(); }
        private IllegalArgumentException bad() { return new IllegalArgumentException("invalid JSON at " + index); }
    }
}
