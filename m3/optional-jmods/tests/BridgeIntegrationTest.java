// SPDX-License-Identifier: Apache-2.0
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.synexia.m3.chrome.chat.ChromeChat;
import com.synexia.m3.compute.vector.VectorCompute;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class BridgeIntegrationTest {
    static int checks;
    static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    static void rejects(Runnable action, String label) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException ok) { return; }
        throw new AssertionError(label);
    }
    static void ioRejects(ChromeChat chat, String label) {
        checks++;
        try { chat.complete("chrome-gemini-nano", List.of(new ChromeChat.Message("user", "Hello")), 12); }
        catch (IOException ok) { return; }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
        throw new AssertionError(label);
    }
    public static void main(String[] args) throws Exception {
        rejects(() -> new ChromeChat(0), "zero port");
        rejects(() -> new ChromeChat(65536), "oversized port");
        rejects(() -> new ChromeChat.Message("sys", "x"), "role");
        rejects(() -> new ChromeChat.Message("user", "\uD800"), "surrogate");
        rejects(() -> ChromeChat.requestJson("../escape", List.of(new ChromeChat.Message("user","hi")), 5), "model");
        rejects(() -> ChromeChat.parseCompletion("{\"model\":\"x\",\"choices\":[]}"), "wrong choices");
        rejects(() -> ChromeChat.parseCompletion("{\"model\":\"x\",\"choices\":null}"), "wrong type");
        rejects(() -> ChromeChat.parseCompletion("{\"model\":\"x\",\"model\":\"z\",\"choices\":[]}"), "duplicate key");
        rejects(() -> ChromeChat.parseCompletion("{\"model\":null}"), "bad model");
        rejects(() -> ChromeChat.parseCompletion("{\"model\":\"x\",\"choices\":[{\"message\":{\"role\":\"user\",\"content\":\"x\"}}]}"), "role mismatch");
        check(VectorCompute.add(new int[0], new int[0]).values().length == 0, "empty vector");
        rejects(() -> VectorCompute.add(new int[1], new int[2]), "wrong length");
        rejects(() -> VectorCompute.addWithBackend(new int[2], new int[2], "missing-gpu"), "missing provider");
        rejects(() -> VectorCompute.addVerified(new int[]{1}, new int[]{2}, new VectorCompute.Provider() {
            public String id() { return "mutating-test"; }
            public int[] add(int[] a, int[] b) { a[0] = 99; return new int[]{3}; }
        }), "mutated inputs");
        check(Arrays.equals(VectorCompute.add(new int[]{-1, Integer.MAX_VALUE}, new int[]{1, 1}).values(),
            new int[]{0, Integer.MIN_VALUE}), "overflow parity");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", BridgeIntegrationTest::reply);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        try (ChromeChat chat = new ChromeChat(server.getAddress().getPort())) {
            var completion = chat.complete("chrome-gemini-nano", List.of(new ChromeChat.Message("user", "Hello")), 25);
            check(completion.content().equals("Hello back ☺"), "HTTP JSON completion");
            check(completion.finishReason().equals("stop"), "finish reason");
            ioRejects(chat, "bad HTTP response rejected");
        } finally { server.stop(0); }
        System.out.println("M3_BRIDGE_INTEGRATION_PASS checks="+checks);
    }
    static int requests;
    private static void reply(HttpExchange exchange) throws IOException {
        String incoming = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (!incoming.contains("\"stream\":false")) throw new AssertionError("expected non-streaming");
        byte[] data;
        int status;
        if (++requests == 1) {
            data = "{\"model\":\"chrome-gemini-nano\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello back \\u263a\"},\"finish_reason\":\"stop\"}]}".getBytes(StandardCharsets.UTF_8);
            status=200;
        } else {
            data="{\"error\":\"failed\"}".getBytes(StandardCharsets.UTF_8);
            status=500;
        }
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, data.length);
        try (var output = exchange.getResponseBody()) { output.write(data); }
    }
}
