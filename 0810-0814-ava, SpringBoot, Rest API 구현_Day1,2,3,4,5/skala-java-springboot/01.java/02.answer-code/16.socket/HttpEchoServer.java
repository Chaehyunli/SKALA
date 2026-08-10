import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 간단한 HTTP 서버 에뮬레이션
 *
 * [HTTP 요청 구조]
 * ┌─────────────────────────────┐
 * │ GET /path HTTP/1.1          │ ← 요청 라인 (메서드 + 경로 + 버전)
 * │ Host: localhost             │ ← 헤더
 * │ Connection: close           │ ← 헤더
 * │                             │ ← 빈 줄 (헤더 끝 표시)
 * └─────────────────────────────┘
 *
 * [HTTP 응답 구조]
 * ┌─────────────────────────────┐
 * │ HTTP/1.1 200 OK             │ ← 상태 라인 (버전 + 상태코드 + 메시지)
 * │ Content-Type: application/json │ ← 헤더
 * │ Content-Length: 27          │ ← 헤더
 * │                             │ ← 빈 줄 (헤더 끝 표시)
 * │ {"message":"hello"}         │ ← 본문 (Body)
 * └─────────────────────────────┘
 */
public class HttpEchoServer {

    public static void main(String[] args) {
        int port = 8080;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("HTTP Echo Server started on port " + port);
            System.out.println("접속 경로 예시:");
            System.out.println("  GET /         → 서버 정보");
            System.out.println("  GET /users    → 사용자 목록");
            System.out.println("  GET /time     → 현재 시간");
            System.out.println("  GET /other    → 404 Not Found");
            System.out.println("=".repeat(40));

            while (true) {
                // 클라이언트 연결 수락
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[클라이언트 연결] " + clientSocket.getInetAddress());

                handleRequest(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
            );
            PrintWriter writer = new PrintWriter(
                clientSocket.getOutputStream(), true
            )
        ) {
            // ── 1단계: HTTP 요청 읽기 ──────────────────────────────────────────
            // 첫 번째 줄: 요청 라인 (예: "GET /users HTTP/1.1")
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }
            System.out.println("[요청 라인] " + requestLine);

            // 나머지 헤더 읽기 (빈 줄이 나올 때까지)
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                System.out.println("[요청 헤더] " + headerLine);
            }
            System.out.println("[요청 헤더] (빈 줄 - 헤더 끝)");

            // ── 2단계: 요청 라인에서 경로 추출 ────────────────────────────────
            // "GET /users HTTP/1.1" → 공백 기준으로 분리
            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "UNKNOWN"; // GET
            String path   = parts.length > 1 ? parts[1] : "/";       // /users
            System.out.println("[파싱 결과] 메서드=" + method + ", 경로=" + path);

            // ── 3단계: GET 메서드만 처리 ──────────────────────────────────────
            if (!"GET".equals(method)) {
                sendResponse(writer, 405, "Method Not Allowed",
                    "{\"error\":\"GET 메서드만 지원합니다\"}");
                return;
            }

            // ── 4단계: 경로별 JSON 응답 생성 ──────────────────────────────────
            switch (path) {
                case "/" -> sendResponse(writer, 200, "OK",
                    "{\"service\":\"HttpEchoServer\",\"status\":\"running\",\"port\":8080}");

                case "/users" -> sendResponse(writer, 200, "OK",
                    "{\"users\":[" +
                    "{\"id\":1,\"name\":\"Alice\"}," +
                    "{\"id\":2,\"name\":\"Bob\"}," +
                    "{\"id\":3,\"name\":\"Charlie\"}" +
                    "]}");

                case "/time" -> sendResponse(writer, 200, "OK",
                    "{\"time\":\"" + java.time.LocalDateTime.now() + "\"}");

                default -> sendResponse(writer, 404, "Not Found",
                    "{\"error\":\"경로를 찾을 수 없습니다\",\"path\":\"" + path + "\"}");
            }

        } catch (IOException e) {
            System.err.println("[오류] " + e.getMessage());
        }
    }

    /**
     * HTTP 응답 전송
     *
     * 응답 형식:
     *   HTTP/1.1 {statusCode} {statusMessage}\r\n
     *   Content-Type: application/json\r\n
     *   Content-Length: {길이}\r\n
     *   Connection: close\r\n
     *   \r\n
     *   {body}
     */
    private static void sendResponse(PrintWriter writer, int statusCode,
                                     String statusMessage, String body) {
        System.out.println("[응답 상태] " + statusCode + " " + statusMessage);
        System.out.println("[응답 본문] " + body);

        // 상태 라인
        writer.print("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");

        // 응답 헤더
        writer.print("Content-Type: application/json; charset=UTF-8\r\n");
        writer.print("Content-Length: " + body.getBytes().length + "\r\n");
        writer.print("Connection: close\r\n");

        // 헤더와 본문 사이의 빈 줄 (필수!)
        writer.print("\r\n");

        // 응답 본문 (Body)
        writer.print(body);
        writer.flush();
    }
}
