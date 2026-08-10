import java.io.*;
import java.net.Socket;

/**
 * HttpWebServer 테스트 클라이언트
 *
 * GET / POST 요청을 모두 테스트합니다.
 */
public class HttpWebClient {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        // GET 요청 테스트
        sendGet(host, port, "/users");
        sendGet(host, port, "/users/1");
        sendGet(host, port, "/notfound");

        // POST 요청 테스트 (body의 name/role/email이 User 객체로 변환됨)
        sendPost(host, port, "/users",
            "{\"name\":\"Dave\",\"role\":\"user\",\"email\":\"dave@example.com\"}");
    }

    // ── GET 요청 전송 ─────────────────────────────────────────────────────
    static void sendGet(String host, int port, String path) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("▶ GET " + path);

        try (
            Socket socket = new Socket(host, port);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            )
        ) {
            // HTTP GET 요청 전송
            writer.print("GET " + path + " HTTP/1.1\r\n");
            writer.print("Host: " + host + "\r\n");
            writer.print("Connection: close\r\n");
            writer.print("\r\n");
            writer.flush();

            printResponse(reader);
        } catch (IOException e) {
            System.err.println("[오류] " + e.getMessage());
        }
    }

    // ── POST 요청 전송 ────────────────────────────────────────────────────
    static void sendPost(String host, int port, String path, String body) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("▶ POST " + path + "  body=" + body);

        try (
            Socket socket = new Socket(host, port);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            )
        ) {
            // HTTP POST 요청 전송 (Content-Length 필수)
            writer.print("POST " + path + " HTTP/1.1\r\n");
            writer.print("Host: " + host + "\r\n");
            writer.print("Content-Type: application/json\r\n");
            writer.print("Content-Length: " + body.getBytes().length + "\r\n");
            writer.print("Connection: close\r\n");
            writer.print("\r\n");   // 헤더 끝
            writer.print(body);     // Body 전송
            writer.flush();

            printResponse(reader);
        } catch (IOException e) {
            System.err.println("[오류] " + e.getMessage());
        }
    }

    // ── 응답 출력 ─────────────────────────────────────────────────────────
    static void printResponse(BufferedReader reader) throws IOException {
        // 상태 라인
        String statusLine = reader.readLine();
        System.out.println("[상태] " + statusLine);

        // 헤더 (빈 줄까지)
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            System.out.println("[헤더] " + headerLine);
        }

        // 본문
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        System.out.println("[JSON] " + body);
    }
}
