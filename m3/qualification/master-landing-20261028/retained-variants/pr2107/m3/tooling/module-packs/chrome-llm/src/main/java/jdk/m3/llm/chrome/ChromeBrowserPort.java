/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Synexia-owned bridge for the mechanical ChromeSummarizerPort/ChromeProtocol
 * at 2ab439480ecb0643615e85b42def954dbf3b5627, cpullm-opdag/java/chrome-ai-mechanical,
 * and aix-chrome-ai-studio/mechanical/browser/chrome-ai-api.mjs. Reviewed template; recipe materialized.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.ChromeRequest;
import static jdk.m3.llm.chrome.MechanicalModel.ChromeResult;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import jdk.m3.llm.internal.Json;

/**
 * Candidate-only connection to an explicitly prepared Chrome browser adapter.
 * The host owns transport routing, authentication, deadlines and cancellation.
 * This class neither initializes Chrome nor opens a network connection.
 */
public final class ChromeBrowserPort implements ChromeSummarizerPort {
    public static final String PROTOCOL = "M3_CHROME_BRIDGE_V1";
    private static final String REQUEST_DOMAIN = "M3_CHROME_BRIDGE_REQUEST_V1";
    private static final String RESULT_DOMAIN = "M3_CHROME_BRIDGE_RESULT_V1";

    /** Relay the JSON string unchanged to the browser adapter's exchange method. */
    @FunctionalInterface
    public interface Transport {
        String exchange(String requestJson) throws Exception;
    }

    /** A separate Prompt API job session; each prompt is independent of earlier jobs. */
    @SuppressWarnings("try") // Caller-owned transports may propagate interruption on close.
    public interface PromptSession extends AutoCloseable {
        ChromeResult prompt(ChromeRequest request) throws Exception;
        @Override void close() throws Exception;
    }

    /** Bounds may be lowered from the browser adapter defaults. Units are UTF-8 bytes. */
    public record Limits(int maxInputBytes, int maxOutputBytes, int maxSourceRefs,
            int maxMetadataBytes, int maxModelCalls) {
        public Limits {
            bound(maxInputBytes, 4 * 1024 * 1024, "maxInputBytes");
            bound(maxOutputBytes, 64 * 1024, "maxOutputBytes");
            bound(maxSourceRefs, 4096, "maxSourceRefs");
            bound(maxMetadataBytes, 64 * 1024, "maxMetadataBytes");
            bound(maxModelCalls, 4096, "maxModelCalls");
        }
        public static Limits defaults() {
            return new Limits(4 * 1024 * 1024, 64 * 1024, 4096, 64 * 1024, 4096);
        }
        private int packetCharacters() {
            return 8192 + 6 * (maxInputBytes + maxOutputBytes + maxMetadataBytes) + 3 * maxSourceRefs;
        }
    }

    private final Transport transport;
    private final Limits limits;
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile BrowserSession active;

    public ChromeBrowserPort(Transport transport) {
        this(transport, Limits.defaults());
    }

    public ChromeBrowserPort(Transport transport, Limits limits) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    @Override public Session open() throws Exception { return openOperation("summarize"); }

    public PromptSession openPrompt() throws Exception { return openOperation("prompt"); }

    private BrowserSession openOperation(String operation) throws Exception {
        enter();
        try {
            if (active != null) throw new IllegalStateException("a browser session is already attached");
            String sessionId = UUID.randomUUID().toString();
            BrowserSession session = new BrowserSession(operation, sessionId);
            Map<String, Object> packet = base("open", operation, sessionId, 0);
            packet.put("limits", limitsMap(limits));
            try {
                Map<String, Object> response = exchange(packet);
                exact(response, "protocol", "action", "operation", "sessionId", "sequence", "limits",
                        "candidateOnly", "admitted", "modelCalls", "directNetworkCalls");
                envelope(response, "opened", operation, sessionId, 0);
                if (!limits.equals(readLimits(response.get("limits"))) || !bool(response, "candidateOnly")
                        || bool(response, "admitted") || integer(response, "modelCalls") != 0
                        || integer(response, "directNetworkCalls") != 0) {
                    throw new IllegalArgumentException("invalid browser session acknowledgement");
                }
                active = session;
                return session;
            } catch (Exception failure) {
                // The caller chose the identifier, so an uncertain open can still be closed.
                try { exchange(base("close", operation, sessionId, 0)); }
                catch (Exception cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
        } finally { busy.set(false); }
    }

    @SuppressWarnings("try") // Both inherited session close signatures retain checked exceptions.
    private final class BrowserSession implements Session, PromptSession {
        private final String operation;
        private final String sessionId;
        private int sequence;
        private int acknowledgedCalls;
        private boolean closed;
        private boolean failed;

        private BrowserSession(String operation, String sessionId) {
            this.operation = operation;
            this.sessionId = sessionId;
        }

        @Override public ChromeResult summarize(ChromeRequest request) throws Exception {
            if (!operation.equals("summarize")) throw new IllegalStateException("not a Summarizer session");
            return execute(request);
        }

        @Override public ChromeResult prompt(ChromeRequest request) throws Exception {
            if (!operation.equals("prompt")) throw new IllegalStateException("not a Prompt session");
            return execute(request);
        }

        private ChromeResult execute(ChromeRequest request) throws Exception {
            enter();
            boolean sent = false;
            try {
                if (closed || active != this || failed) throw new IllegalStateException("browser session is closed or failed");
                validateRequest(request, limits);
                if (sequence >= limits.maxModelCalls()) throw new IllegalStateException("model call budget exceeded");
                int next = sequence + 1;
                String requestRoot = requestPacketId(operation, sessionId, next, request);
                Map<String, Object> packet = base("execute", operation, sessionId, next);
                packet.put("packetId", requestRoot);
                packet.put("request", requestMap(request));
                sequence = next;
                sent = true;
                Map<String, Object> response = exchange(packet);
                exact(response, "protocol", "action", "operation", "sessionId", "sequence", "packetId",
                        "resultPacketId", "result");
                envelope(response, "result", operation, sessionId, next);
                if (!requestRoot.equals(string(response, "packetId"))) throw new IllegalArgumentException("request packet binding mismatch");
                ChromeResult result = readResult(response.get("result"), limits);
                ChromeProtocol.verify(request, result);
                if (!result.status().equals("CANDIDATE_ONLY") || result.modelCalls() != 1) {
                    throw new IllegalArgumentException("browser must return one native candidate");
                }
                String root = resultPacketId(operation, sessionId, next, requestRoot, result);
                if (!root.equals(string(response, "resultPacketId"))) throw new IllegalArgumentException("result packet identity mismatch");
                acknowledgedCalls += result.modelCalls();
                return result;
            } catch (Exception failure) {
                if (sent) failed = true; // Transport failures cannot safely be retried as a new model call.
                throw failure;
            } finally { busy.set(false); }
        }

        @Override public void close() throws Exception {
            enter();
            try {
                if (closed) return;
                closed = true;
                if (active != this) return;
                try {
                    Map<String, Object> response = exchange(base("close", operation, sessionId, sequence));
                    exact(response, "protocol", "action", "operation", "sessionId", "sequence",
                            "modelCalls", "directNetworkCalls");
                    envelope(response, "closed", operation, sessionId, sequence);
                    int calls = integer(response, "modelCalls");
                    if (calls < acknowledgedCalls || calls > sequence || integer(response, "directNetworkCalls") != 0) {
                        throw new IllegalArgumentException("invalid browser close counters");
                    }
                } finally { active = null; }
            } finally { busy.set(false); }
        }
    }

    /** Unambiguous length-framed identity in addition to the preserved donor request identity. */
    public static String requestPacketId(String operation, String sessionId, int sequence, ChromeRequest request) {
        operation(operation); sessionId(sessionId); bound(sequence, 4096, "sequence");
        Objects.requireNonNull(request, "request");
        List<Object> fields = new ArrayList<>();
        fields.add(operation); fields.add(sessionId); fields.add(sequence);
        fields.add(request.requestId()); fields.add(request.nodeId()); fields.add(request.inputSha256());
        fields.add(request.sourceRefs().size()); fields.addAll(request.sourceRefs());
        fields.add(request.authority()); fields.add(request.candidateOnly());
        fields.add(request.modelCalls()); fields.add(request.directNetworkCalls());
        return Hashing.hash(REQUEST_DOMAIN, fields.toArray());
    }

    /** Result identity binds the operation, full request packet and exact reference array. */
    public static String resultPacketId(String operation, String sessionId, int sequence,
            String requestPacketId, ChromeResult result) {
        operation(operation); sessionId(sessionId); bound(sequence, 4096, "sequence");
        if (!Hashing.isSha256(requestPacketId)) throw new IllegalArgumentException("invalid request packet identity");
        Objects.requireNonNull(result, "result");
        List<Object> fields = new ArrayList<>();
        fields.add(operation); fields.add(sessionId); fields.add(sequence); fields.add(requestPacketId);
        fields.add(result.resultId()); fields.add(result.status()); fields.add(result.sourceRequestId());
        fields.add(result.sourceRefs().size()); fields.addAll(result.sourceRefs());
        fields.add(result.text()); fields.add(result.authority()); fields.add(result.candidateOnly());
        fields.add(result.admitted()); fields.add(result.modelCalls()); fields.add(result.directNetworkCalls());
        return Hashing.hash(RESULT_DOMAIN, fields.toArray());
    }

    private void enter() {
        if (!busy.compareAndSet(false, true)) throw new IllegalStateException("one browser operation at a time");
    }

    private Map<String, Object> exchange(Map<String, Object> packet) throws Exception {
        String request = Json.write(packet);
        if (request.length() > limits.packetCharacters()) throw new IllegalArgumentException("request packet budget exceeded");
        String response = Objects.requireNonNull(transport.exchange(request), "browser response");
        Object parsed = Json.parse(response, limits.packetCharacters());
        if (!response.equals(Json.write(parsed))) throw new IllegalArgumentException("noncanonical browser JSON");
        return object(parsed);
    }

    private static void validateRequest(ChromeRequest request, Limits limits) {
        Objects.requireNonNull(request, "request");
        bytes(request.input(), limits.maxInputBytes(), "input");
        if (request.input().isBlank()) throw new IllegalArgumentException("input must not be blank");
        metadata(request.nodeId(), request.sourceRefs(), limits);
        if (!request.equals(ChromeProtocol.request(request.nodeId(), request.input(), request.sourceRefs()))) {
            throw new IllegalArgumentException("request identity or authority mismatch");
        }
    }

    private static ChromeResult readResult(Object value, Limits limits) {
        Map<String, Object> result = object(value);
        exact(result, "status", "resultId", "sourceRequestId", "sourceRefs", "text", "authority",
                "candidateOnly", "admitted", "modelCalls", "directNetworkCalls");
        List<String> refs = strings(result.get("sourceRefs"), limits.maxSourceRefs());
        metadata("", refs, limits);
        String output = string(result, "text");
        bytes(output, limits.maxOutputBytes(), "output");
        return new ChromeResult(string(result, "status"), string(result, "resultId"),
                string(result, "sourceRequestId"), refs, output, string(result, "authority"),
                bool(result, "candidateOnly"), bool(result, "admitted"),
                integer(result, "modelCalls"), integer(result, "directNetworkCalls"));
    }

    private static Map<String, Object> requestMap(ChromeRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("requestId", request.requestId()); out.put("nodeId", request.nodeId());
        out.put("input", request.input()); out.put("inputSha256", request.inputSha256());
        out.put("sourceRefs", request.sourceRefs()); out.put("authority", request.authority());
        out.put("candidateOnly", request.candidateOnly()); out.put("modelCalls", request.modelCalls());
        out.put("directNetworkCalls", request.directNetworkCalls());
        return out;
    }

    private static Map<String, Object> base(String action, String operation, String sessionId, int sequence) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("protocol", PROTOCOL); out.put("action", action); out.put("operation", operation);
        out.put("sessionId", sessionId); out.put("sequence", sequence);
        return out;
    }

    private static Map<String, Object> limitsMap(Limits limits) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("maxInputBytes", limits.maxInputBytes()); out.put("maxOutputBytes", limits.maxOutputBytes());
        out.put("maxSourceRefs", limits.maxSourceRefs()); out.put("maxMetadataBytes", limits.maxMetadataBytes());
        out.put("maxModelCalls", limits.maxModelCalls()); return out;
    }

    private static Limits readLimits(Object value) {
        Map<String, Object> map = object(value);
        exact(map, "maxInputBytes", "maxOutputBytes", "maxSourceRefs", "maxMetadataBytes", "maxModelCalls");
        return new Limits(integer(map, "maxInputBytes"), integer(map, "maxOutputBytes"),
                integer(map, "maxSourceRefs"), integer(map, "maxMetadataBytes"), integer(map, "maxModelCalls"));
    }

    private static void envelope(Map<String, Object> packet, String action, String operation, String sessionId, int sequence) {
        if (!PROTOCOL.equals(string(packet, "protocol")) || !action.equals(string(packet, "action"))
                || !operation.equals(string(packet, "operation")) || !sessionId.equals(string(packet, "sessionId"))
                || sequence != integer(packet, "sequence")) throw new IllegalArgumentException("browser envelope binding mismatch");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map) || map.keySet().stream().anyMatch(key -> !(key instanceof String))) {
            throw new IllegalArgumentException("expected JSON object");
        }
        return (Map<String, Object>) map;
    }

    private static void exact(Map<String, Object> value, String... keys) {
        if (!value.keySet().equals(Set.of(keys))) throw new IllegalArgumentException("unknown or missing packet fields");
    }

    private static String string(Map<String, Object> map, String key) {
        if (!(map.get(key) instanceof String value)) throw new IllegalArgumentException("expected text field " + key);
        Hashing.utf8(value); return value;
    }

    private static boolean bool(Map<String, Object> map, String key) {
        if (!(map.get(key) instanceof Boolean value)) throw new IllegalArgumentException("expected boolean field " + key);
        return value;
    }

    private static int integer(Map<String, Object> map, String key) {
        if (!(map.get(key) instanceof BigInteger value)) throw new IllegalArgumentException("expected integer field " + key);
        try { return value.intValueExact(); }
        catch (ArithmeticException error) { throw new IllegalArgumentException("integer out of range: " + key, error); }
    }

    private static List<String> strings(Object value, int maximum) {
        if (!(value instanceof List<?> list) || list.size() > maximum) throw new IllegalArgumentException("source reference budget exceeded");
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String text)) throw new IllegalArgumentException("source reference must be text");
            out.add(text);
        }
        return List.copyOf(out);
    }

    private static void metadata(String nodeId, List<String> refs, Limits limits) {
        if (refs.size() > limits.maxSourceRefs()) throw new IllegalArgumentException("source reference budget exceeded");
        int used = bytes(nodeId, limits.maxMetadataBytes(), "nodeId");
        for (String ref : refs) {
            used += bytes(ref, limits.maxMetadataBytes(), "source reference");
            if (used > limits.maxMetadataBytes()) throw new IllegalArgumentException("metadata byte budget exceeded");
        }
    }

    private static int bytes(String value, int maximum, String label) {
        Objects.requireNonNull(value, label);
        if (value.length() > maximum) throw new IllegalArgumentException(label + " byte budget exceeded");
        int count = Hashing.utf8(value).length;
        if (count > maximum) throw new IllegalArgumentException(label + " byte budget exceeded");
        return count;
    }

    private static void operation(String operation) {
        if (!"summarize".equals(operation) && !"prompt".equals(operation)) throw new IllegalArgumentException("unsupported browser operation");
    }

    private static void sessionId(String sessionId) {
        if (sessionId == null || !sessionId.matches("[A-Za-z0-9._-]{1,128}")) throw new IllegalArgumentException("invalid session identifier");
    }

    private static void bound(int value, int maximum, String name) {
        if (value < 1 || value > maximum) throw new IllegalArgumentException("invalid " + name);
    }
}
