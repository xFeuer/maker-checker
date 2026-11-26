package io.github.xfeuer.makerchecker.chassis.testapplication;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/** Sends a request to the server byte for byte. A client library refuses to send what some tests need. */
final class RawHttp {

    /** A response, with the names of its headers in any case. */
    record Reply(int status, Map<String, String> headers, String body) {}

    private RawHttp() {}

    /** Sends the request and reads until the server closes the connection, so the request has to ask for that. */
    static Reply send(int port, String request) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(10_000);
            socket.getOutputStream().write(request.getBytes(StandardCharsets.ISO_8859_1));
            socket.getOutputStream().flush();
            String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            int endOfHead = response.indexOf("\r\n\r\n");
            String[] head = response.substring(0, endOfHead).split("\r\n");
            Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (int line = 1; line < head.length; line++) {
                int colon = head[line].indexOf(':');
                headers.put(
                        head[line].substring(0, colon),
                        head[line].substring(colon + 1).strip());
            }
            String body = response.substring(endOfHead + 4);
            if ("chunked".equalsIgnoreCase(headers.get("Transfer-Encoding"))) {
                body = withoutChunks(body);
            }
            return new Reply(Integer.parseInt(head[0].split(" ")[1]), headers, body);
        }
    }

    private static String withoutChunks(String chunked) {
        StringBuilder body = new StringBuilder();
        int position = 0;
        while (true) {
            int endOfSize = chunked.indexOf("\r\n", position);
            int size = Integer.parseInt(chunked.substring(position, endOfSize).strip(), 16);
            if (size == 0) {
                return body.toString();
            }
            // The bodies of these tests are ASCII, so a character is a byte.
            body.append(chunked, endOfSize + 2, endOfSize + 2 + size);
            position = endOfSize + 2 + size + 2;
        }
    }
}
