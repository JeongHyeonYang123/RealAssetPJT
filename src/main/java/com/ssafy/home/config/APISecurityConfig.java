package com.ssafy.home.config;

import com.ssafy.home.filter.JWTAuthenticationFilter;
import com.ssafy.home.filter.JWTVerificationFilter;
import com.ssafy.home.filter.SecurityExceptionHandlingFilter;
import com.ssafy.home.service.CustomUserDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
public class APISecurityConfig {

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfig,
            CustomUserDetailService userDetailsService,
            JWTAuthenticationFilter authFilter,
            JWTVerificationFilter jwtVerifyFilter,
            SecurityExceptionHandlingFilter exceptionFilter,
            PasswordEncoder passwordEncoder)
            throws Exception {

        http.securityMatcher("/api/**")
                .cors(t -> t.configurationSource(corsConfig))
                .userDetailsService(userDetailsService)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider(userDetailsService, passwordEncoder));

        // HTTP 메서드별 세분화된 권한 설정
        http.authorizeHttpRequests(auth -> auth
                // === 기본 인증 관련 API ===
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                .requestMatchers("/api/v1/users/email/**").permitAll()
                .requestMatchers("/api/v1/users/verify").permitAll()
                .requestMatchers("/api/v1/users/find-password").permitAll()
                .requestMatchers("/api/v1/users/reset-password").permitAll()
                .requestMatchers("/api/v1/users/refresh").permitAll()

                .requestMatchers("/api/v1/house/**").permitAll()

                // === 게시글 관련 API - 조회는 누구나, 작성/수정/삭제는 인증 필요 ===
                .requestMatchers(HttpMethod.GET, "/api/v1/posts").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/posts").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/posts/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/*").authenticated()

                // === 댓글 관련 API - 조회는 누구나, 작성/수정/삭제는 인증 필요 ===
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/*/comments").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/comments").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/posts/comments/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/comments/*").authenticated()

                // === 좋아요/싫어요 - 로그인 필요 ===
                .requestMatchers(HttpMethod.POST, "/api/v1/posts/*/reaction").authenticated()

                // === 기타 공개 API ===
                .requestMatchers("/api/v1/adm/**").permitAll()
                .requestMatchers("/api/v1/transactions/**").permitAll()
                .requestMatchers("/api/v1/commercial-areas/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/bitcoin/**").permitAll()
                .requestMatchers("/api/v1/house/**").permitAll()

                // === 관심지역 API - 인증 필요 ===
                .requestMatchers("/api/v1/interest-areas/**").authenticated()

                // === 챗봇 API - 인증 없이 사용 ===
                .requestMatchers("/api/v1/chatbot/**").permitAll()

                // === Swagger 문서 ===
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/v1/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()

                // === 관리자 전용 ===
                .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/management/**").hasAuthority("ADMIN")

                // === 나머지는 인증 필요 ===
                .anyRequest().authenticated()
        );

        // 필터 순서 설정
        http.addFilterBefore(exceptionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(authFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtVerifyFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ 수정된 부분: allowedOriginPatterns로 모든 origin 허용
        configuration.setAllowedOriginPatterns(List.of("*"));  // 개발 환경에서 모든 도메인 허용
        // 운영 환경에서는: configuration.setAllowedOriginPatterns(List.of("https://yourdomain.com", "http://localhost:3000"));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
