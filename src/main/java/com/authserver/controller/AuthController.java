package com.authserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.authserver.dto.LoginRequest;
import com.authserver.dto.SignupRequest;
import com.authserver.entity.User;
import com.authserver.service.AuthService;
import com.authserver.util.JwtUtil;
import com.authserver.exception.InvalidTokenException;
import com.authserver.exception.ExpiredTokenException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "회원가입, 로그인, 로그아웃 등 인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Value("${frontend.url:http://localhost:3001}")
    private String frontendUrl;

    // 세션 저장소 (실제 서비스에서는 Redis나 Spring Session 사용 권장)
    private static final Map<String, User> SESSION_STORE = new ConcurrentHashMap<>();
    private static final String SESSION_COOKIE_NAME = "SESSION_ID";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

    /**
     * POST /api/signup - 회원가입
     */
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다. JSON 본문으로만 요청을 받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 값 누락 또는 중복된 사용자명)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {

        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username and password are required"));
        }

        try {
            User user = authService.signUp(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(Map.of(
                    "message", "Signup successful",
                    "userId", user.getId(),
                    "username", user.getUsername()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "username already exists"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "signup error"));
        }
    }

    /**
     * POST /api/login - 로그인
     */
    @Operation(summary = "로그인", description = "사용자 인증 후 세션 및 JWT 토큰을 발급합니다. JSON 본문으로만 요청을 받습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 값 누락)"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (잘못된 사용자명 또는 비밀번호)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {

        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username and password are required"));
        }

        try {
            User user = authService.login(request.getUsername(), request.getPassword());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid credentials"));
            }

            // 1) 세션 쿠키 생성
            String sessionId = UUID.randomUUID().toString();
            SESSION_STORE.put(sessionId, user);

            Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
            sessionCookie.setPath("/");
            sessionCookie.setHttpOnly(true);
            sessionCookie.setAttribute("SameSite", "Lax");
            response.addCookie(sessionCookie);

            // 2) Access Token 쿠키 생성
            String accessToken = jwtUtil.createAccessToken(user);
            Cookie accessTokenCookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, accessToken);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setMaxAge(60 * 60); // 1시간
            accessTokenCookie.setAttribute("SameSite", "Lax");
            response.addCookie(accessTokenCookie);

            // 3) Refresh Token 생성 및 저장
            String refreshToken = jwtUtil.createRefreshToken(user);
            authService.saveRefreshToken(user.getId(), refreshToken);

            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
            refreshTokenCookie.setAttribute("SameSite", "Lax");
            response.addCookie(refreshTokenCookie);

            // 4) 프론트엔드에서 로그인 여부 판단용 쿠키 (JS에서 읽기 가능)
            Cookie appAuthCookie = new Cookie("APP_AUTH", "1");
            appAuthCookie.setPath("/");
            appAuthCookie.setAttribute("SameSite", "Lax");
            response.addCookie(appAuthCookie);

            // 5) 로그인 성공 응답
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "userId", user.getId(),
                    "username", user.getUsername()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "login error"));
        }
    }

    /**
     * POST /api/refresh - Access Token 갱신
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (만료되었거나 유효하지 않은 Refresh Token)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // 1. 쿠키에서 Refresh Token 추출
            String oldRefreshToken = getRefreshTokenFromCookie(request);
            if (oldRefreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token not found");
            }

            // 2. Refresh Token 검증 및 User 반환
            User user = authService.refreshAccessToken(oldRefreshToken);

            // 3. Token Rotation: 새로운 Refresh Token 생성
            String newRefreshToken = jwtUtil.createRefreshToken(user);
            authService.saveRefreshToken(user.getId(), newRefreshToken);

            // 4. 새로운 Access Token 생성
            String newAccessToken = jwtUtil.createAccessToken(user);

            // 5. Access Token 쿠키 업데이트
            addCookie(response, ACCESS_TOKEN_COOKIE_NAME, newAccessToken, 60 * 60);

            // 6. Refresh Token 쿠키 업데이트 (Token Rotation)
            addCookie(response, REFRESH_TOKEN_COOKIE_NAME, newRefreshToken, 30 * 24 * 60 * 60);

            return ResponseEntity.ok("Tokens refreshed successfully");

        } catch (ExpiredTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token expired. Please login again.");
        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid refresh token");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Token refresh error");
        }
    }

    /**
     * POST /api/logout - 로그아웃
     */
    @Operation(summary = "로그아웃", description = "세션 및 쿠키를 삭제하여 로그아웃 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 인증된 사용자 ID 가져오기
        Long authenticatedUserId = (Long) request.getAttribute("authenticatedUserId");

        // DB Refresh Token 무효화
        if (authenticatedUserId != null) {
            authService.logout(authenticatedUserId);
        }

        // 세션 제거
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId != null) {
            SESSION_STORE.remove(sessionId);
        }

        // 쿠키 삭제
        deleteCookie(response, SESSION_COOKIE_NAME);
        deleteCookie(response, ACCESS_TOKEN_COOKIE_NAME);
        deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
        deleteCookie(response, "APP_AUTH");

        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    /**
     * 쿠키에서 세션 ID 추출
     */
    private String getSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 쿠키에서 Refresh Token 추출
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 쿠키 추가 헬퍼 메서드
     */
    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /**
     * 쿠키 삭제 헬퍼 메서드
     */
    private void deleteCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
