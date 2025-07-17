package com.tomato.remember.common.security;

import com.tomato.remember.admin.security.AdminUserDetailsService;
import com.tomato.remember.application.security.MemberUserDetailsService;
import com.tomato.remember.common.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final MemberUserDetailsService memberUserDetailsService;
    private final AdminUserDetailsService adminUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider memberProvider = new DaoAuthenticationProvider();
        memberProvider.setUserDetailsService(memberUserDetailsService);
        memberProvider.setPasswordEncoder(passwordEncoder());

        DaoAuthenticationProvider adminProvider = new DaoAuthenticationProvider();
        adminProvider.setUserDetailsService(adminUserDetailsService);
        adminProvider.setPasswordEncoder(passwordEncoder());

        return new ProviderManager(memberProvider, adminProvider);
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .requestMatchers("/uploads/**", "/css/**", "/js/**", "/images/**", "/favicon.ico");
    }

    /**
     * 1순위: 관리자 뷰 - 세션 기반 인증 URL: /admin/view/**
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminViewFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/view/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/view/login", "/admin/view/register").permitAll()
                        .requestMatchers("/admin/view/**").hasAnyRole("SUPER_ADMIN", "OPERATOR", "UPLOADER")
                )
                .formLogin(form -> form
                        .loginPage("/admin/view/login")
                        .loginProcessingUrl("/admin/view/login")
                        .defaultSuccessUrl("/admin/view/dashboard", true)
                        .failureUrl("/admin/view/login?error=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/view/logout")
                        .logoutSuccessUrl("/admin/view/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect("/admin/view/login"))
                );

        return http.build();
    }

    /**
     * 2순위: 관리자 API - JWT Bearer 토큰 URL: /admin/api/**
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminApiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/api/auth/login", "/admin/api/auth/refresh").permitAll()
                        .requestMatchers("/admin/api/**").hasAnyRole("SUPER_ADMIN", "OPERATOR", "UPLOADER")
                )
                .addFilterBefore(new AdminApiJwtFilter(jwtTokenProvider, adminUserDetailsService), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, denied) ->
                                res.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden"))
                );

        return http.build();
    }

    /**
     * 3순위: 모바일 뷰 통합 - 선택적 인증 (토큰 있으면 인증, 없으면 비회원)
     * 인증이 필요한 경로와 선택적 인증 경로를 필터 내에서 구분 처리
     */
    @Bean
    @Order(3)
    public SecurityFilterChain mobileViewFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/mobile/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 인증이 필요한 경로들
                        .requestMatchers(
                                "/mobile/dashboard/**",
                                "/mobile/memorial/create",
                                "/mobile/memorial/*/edit",
                                "/mobile/mypage/**"
                        ).authenticated()
                        // 나머지 모든 모바일 경로는 허용 (선택적 인증)
                        .requestMatchers("/mobile/**").permitAll()
                )
                .addFilterBefore(new MobileJwtFilter(jwtTokenProvider, memberUserDetailsService, cookieUtil), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendRedirect("/mobile/login");
                        })
                );

        return http.build();
    }

    /**
     * 4순위: API 공개 경로 - 인증 불필요
     */
    @Bean
    @Order(4)
    public SecurityFilterChain publicApiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/register",
                        "/api/memorial/public/**",
                        "/api/videos/public/**",
                        "/api/auth/smsCert/**",
                        "/api/video/**"
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * 5순위: 모바일/앱 공용 API - JWT Bearer 토큰 URL: /api/**
     */
    @Bean
    @Order(5)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(new ApiJwtFilter(jwtTokenProvider, memberUserDetailsService), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, denied) ->
                                res.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden"))
                );

        return http.build();
    }

    /**
     * 6순위: 루트 경로 리다이렉션 - 나머지 모든 요청
     */
    @Bean
    @Order(6)
    public SecurityFilterChain rootRedirectFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}