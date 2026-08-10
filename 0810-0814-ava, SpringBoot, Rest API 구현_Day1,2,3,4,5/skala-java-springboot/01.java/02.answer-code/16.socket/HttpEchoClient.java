import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 * 간단한 HTTP 클라이언트 에뮬레이션
 *
 * [동작 순서]
 * 1. TCP 소켓으로 서버에 연결
 * 2. HTTP GET 요청 텍스트를 직접 작성하여 전송
 * 3. 서버의 HTTP 응답을 읽어서 출력
 * 4. 상태 코드와 JSON 본문을 파싱하여 표시
 */
public class HttpEchoClient {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        // 테스트할 경로 목록
        String[] paths = {"/", "/users", "/time", "/unknown"};

        for (String path : paths) {
            System.out.println("\n" + "=".repeat(50));
            sendGetRequest(host, port, path);
        }
    }

    private static void sendGetRequest(String host, int port, String path) {
        System.out.println("▶ GET " + path + " 요청");

        try {
            InetAddress serverAddr = InetAddress.getByName(host);

            try (
                Socket socket = new Socket(serverAddr, port);
                PrintWriter writer = new PrintWriter(
                    socket.getOutputStream(), true
                );
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                )
            ) {
                // ── 1단계: HTTP GET 요청 전송 ─────────────────────────────────
                //
                // HTTP 요청 형식 (반드시 \r\n 사용):
                //   GET /path HTTP/1.1\r\n
                //   Host: localhost\r\n
                //   Connection: close\r\n
                //   \r\n         ← 헤더 끝을 알리는 빈 줄 (필수!)
                //
                writer.print("GET " + path + " HTTP/1.1\r\n");
                writer.print("Host: " + host + "\r\n");
                writer.print("Connection: close\r\n");
                writer.print("\r\n");  // 헤더 끝 빈 줄
                writer.flush();

                System.out.println("[전송 요청]");
                System.out.println("  GET " + path + " HTTP/1.1");
                System.out.println("  Host: " + host);
                System.out.println("  Connection: close");
                System.out.println("  (빈 줄 - 헤더 끝)");

                // ── 2단계: HTTP 응답 수신 ─────────────────────────────────────
                // 첫 번째 줄: 상태 라인 (예: "HTTP/1.1 200 OK")
                String statusLine = reader.readLine();
                System.out.println("\n[수신 응답]");
                System.out.println("  상태 라인: " + statusLine);

                // ── 3단계: 응답 헤더 읽기 + Content-Length 추출 ──────────────
                // 서버가 보내는 응답 헤더:
                //   Content-Type: application/json
                //   Content-Length: 59   ← 이 값을 추출
                //   Connection: close
                int contentLength = 0;
                String headerLine;
                while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                    System.out.println("  헤더: " + headerLine);
                    if (headerLine.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                    }
                }
                System.out.println("  (빈 줄 - 헤더 끝)");

                // ── 4단계: 본문을 Content-Length 만큼만 읽기 ─────────────────
                // Connection: close 방식: readLine() == null 이 될 때까지 읽음
                //   → 서버가 연결을 끊어야 null 이 옴
                //
                // Content-Length 방식: 정확히 N 바이트만 읽고 즉시 리턴
                //   → 서버 연결 종료를 기다릴 필요 없음
                char[] bodyBuf = new char[contentLength];
                reader.read(bodyBuf, 0, contentLength);
                String body = new String(bodyBuf);
                System.out.println("  본문(JSON): " + body);

                // ── 4단계: 상태 코드 추출 및 결과 표시 ──────────────────────
                // "HTTP/1.1 200 OK" → "200"
                if (statusLine != null) {
                    String[] statusParts = statusLine.split(" ");
                    int statusCode = Integer.parseInt(statusParts[1]);
                    System.out.println("\n[결과] 상태코드=" + statusCode
                        + (statusCode == 200 ? " ✓ 성공" : " ✗ 실패"));
                }
            }

        } catch (IOException e) {
            System.err.println("[오류] " + e.getMessage());
        }
    }
}
