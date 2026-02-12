package com.authserver.config;

import com.authserver.filter.JwtAuthenticationFilter;
import com.authserver.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 필터 설정
 * JWT 인증 필터를 등록하고 보호된 엔드포인트를 설정합니다.
 */
@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter() {
        FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();

        // 필터 인스턴스 생성
        registrationBean.setFilter(new JwtAuthenticationFilter(jwtUtil));

        // 필터 적용 URL 패턴 (인증 필요)
        // Note: /* matches single level, /** is not standard servlet pattern but Spring supports it
        registrationBean.addUrlPatterns("/api/tickets/*");
        registrationBean.addUrlPatterns("/api/tickets/products/*");
        registrationBean.addUrlPatterns("/api/ticket-management/*");
        registrationBean.addUrlPatterns("/api/rides/*");
        registrationBean.addUrlPatterns("/api/ride-usages/*");
        registrationBean.addUrlPatterns("/api/logout");
        registrationBean.addUrlPatterns("/home");
        // NOTE: /api/refresh is excluded because it needs to work with expired Access Tokens

        // 필터 순서 설정 (낮을수록 먼저 실행)
        registrationBean.setOrder(1);

        return registrationBean;
    }
}
