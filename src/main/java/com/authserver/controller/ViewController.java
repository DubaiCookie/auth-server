package com.authserver.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@Tag(name = "뷰 컨트롤러", description = "HTML 페이지 및 정적 리소스 제공")
public class ViewController {

    /**
     * GET / -> /login으로 리다이렉트
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    /**
     * GET /login -> login.html 제공
     */
    @GetMapping("/login")
    @ResponseBody
    public ResponseEntity<String> login() throws IOException {
        return serveHtmlResource("web/login.html");
    }

    /**
     * GET /signup -> signup.html 제공
     */
    @GetMapping("/signup")
    @ResponseBody
    public ResponseEntity<String> signup() throws IOException {
        return serveHtmlResource("web/signup.html");
    }

    /**
     * GET /home -> 로그인 후 홈 페이지
     */
    @GetMapping("/home")
    @ResponseBody
    public ResponseEntity<String> homePage(HttpServletRequest request) {
        // JWT 인증 필터에서 검증된 사용자 ID 가져오기
        Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");

        // 간단한 홈 페이지 HTML 반환
        String html = """
                <html>
                <head>
                  <meta charset='utf-8'/>
                  <link rel='stylesheet' href='/static/style.css'/>
                  <title>Home</title>
                </head>
                <body>
                  <div class='container'>
                    <h1>Welcome!</h1>
                    <p>인증 서버의 /home 페이지입니다.</p>
                    <p>사용자 ID: %s</p>
                    <form method='POST' action='/api/logout'>
                      <button type='submit'>로그아웃</button>
                    </form>
                  </div>
                </body>
                </html>
                """.formatted(authenticatedUserId);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    /**
     * GET /static/{filename} -> CSS/JS 파일 제공
     */
    @GetMapping("/static/{filename:.+}")
    @ResponseBody
    public ResponseEntity<byte[]> serveStatic(@PathVariable String filename) throws IOException {
        String resourcePath = "web/" + filename;
        Resource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = StreamUtils.copyToByteArray(resource.getInputStream());

        // Content-Type 설정
        MediaType mediaType;
        if (filename.endsWith(".css")) {
            mediaType = MediaType.parseMediaType("text/css");
        } else if (filename.endsWith(".js")) {
            mediaType = MediaType.parseMediaType("application/javascript");
        } else {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(content);
    }

    /**
     * HTML 리소스 제공 헬퍼
     */
    private ResponseEntity<String> serveHtmlResource(String resourcePath) throws IOException {
        Resource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(content);
    }

    /**
     * 쿠키에서 세션 ID 추출
     */
    private String getSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if ("SESSION_ID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
