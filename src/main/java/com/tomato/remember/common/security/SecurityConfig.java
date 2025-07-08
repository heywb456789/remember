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
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

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
     * 1순위: 관리자 뷰 - 세션 기반 인증
     * URL: /admin/view/**
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminViewFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/view/**")
            .csrf(csrf -> csrf.disable()) // 필요시 활성화 가능
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
     * 2순위: 관리자 API - JWT Bearer 토큰
     * URL: /admin/api/**
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
            .addFilterBefore(adminApiJwtFilter(), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, denied) ->
                    res.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden"))
            );

        return http.build();
    }

    /**
     * 3순위: 모바일 뷰 - JWT 쿠키 기반
     * URL: /mobile/**
     */
    @Bean
    @Order(3)
    public SecurityFilterChain mobileViewFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/mobile/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/mobile/login", "/mobile/register", "/mobile/auth/**").permitAll()
                .requestMatchers("/mobile/**").authenticated()
            )
            .addFilterBefore(mobileJwtFilter(), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendRedirect("/mobile/login"))
            );

        return http.build();
    }

    /**
     * 4순위: 모바일/앱 공용 API - JWT Bearer 토큰
     * URL: /api/**
     */
    @Bean
    @Order(4)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/register").permitAll()
                .requestMatchers("/api/memorial/public/**", "/api/videos/public/**").permitAll()
                .requestMatchers("/api/**").authenticated()
            )
            .addFilterBefore(apiJwtFilter(), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, denied) ->
                    res.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden"))
            );

        return http.build();
    }

    /**
     * 5순위: 기본 웹 페이지 (홈, 로그인 등)
     */
    @Bean
    @Order(5)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home", "/about", "/contact").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }

    // 필터 빈 생성
    @Bean
    public AdminApiJwtFilter adminApiJwtFilter() {
        return new AdminApiJwtFilter(jwtTokenProvider, adminUserDetailsService);
    }

    @Bean
    public MobileJwtFilter mobileJwtFilter() {
        return new MobileJwtFilter(jwtTokenProvider, memberUserDetailsService, cookieUtil);
    }

    @Bean
    public ApiJwtFilter apiJwtFilter() {
        return new ApiJwtFilter(jwtTokenProvider, memberUserDetailsService);
    }
}