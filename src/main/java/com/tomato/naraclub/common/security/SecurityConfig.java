package com.tomato.naraclub.common.security;

import com.tomato.naraclub.admin.security.AdminUserDetailsService;
import com.tomato.naraclub.application.security.MemberUserDetailsService;
import com.tomato.naraclub.common.config.CorsConfig;
import com.tomato.naraclub.common.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final MemberUserDetailsService userDetailsService;
    private final AdminUserDetailsService adminUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CorsConfig corsConfig;
    private final CookieUtil cookieUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 회원·관리자용 UserDetailsService 를 각각 등록한 ProviderManager(=AuthenticationManager) 생성
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider memberProvider = new DaoAuthenticationProvider();
        memberProvider.setUserDetailsService(userDetailsService);
        memberProvider.setPasswordEncoder(passwordEncoder());

        DaoAuthenticationProvider adminProvider = new DaoAuthenticationProvider();
        adminProvider.setUserDetailsService(adminUserDetailsService);
        adminProvider.setPasswordEncoder(passwordEncoder());

        // 순서대로 시도 → adminCredentials 로 adminProvider, memberCredentials 로 memberProvider 가 처리됩니다.
        return new ProviderManager(memberProvider, adminProvider);
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
            .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
            .requestMatchers("/uploads/**");
    }

    @Bean
    public JwtAuthenticationFilter jwtFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService,
            adminUserDetailsService, cookieUtil);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
//            .cors(cors -> cors
//                .configurationSource(request -> new CorsConfiguration().applyPermitDefaultValues())
//            )
//            .csrf(AbstractHttpConfigurer::disable)
            .addFilterBefore(corsConfig.corsFilter(), UsernamePasswordAuthenticationFilter.class)
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationManager(authenticationManager())
            .authorizeHttpRequests(auth -> auth
                //1) 회원만 가능 리스트
                .requestMatchers("/admin", "/admin/auth/login", "/admin/auth/logout",
                    "/admin/auth/check/username", "/admin/auth/register").permitAll()

                .requestMatchers("/admin/**").hasAnyRole("SUPER_ADMIN", "OPERATOR", "UPLOADER")

                .requestMatchers("/api/auth/validate", "/api/auth/delete").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/board/posts", "/api/vote/posts/*/options/*")
                .authenticated()
                // 2) 로그인·리프레시는 누구나 (토큰 없어도) 허용
                .requestMatchers(
                    "/api/auth/login", "/api/auth/refresh", "/api/board/**",
                    "/api/vote/**", "/api/videos/**", "/api/news/**",
                    "/share/**", "twitter/callback", "/file/profile/**",
                    "/api/subscription/confirm", "/api/subscription/recurring",
                    "/api/auth/pass/callback", "/api/search/**"
                ).permitAll()

                // 3) swagger, 정적 리소스 등
                .requestMatchers("/", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**")
                .permitAll()
                .requestMatchers(
                    "/login/**", "/main/**", "/board/**", "/components/**",
                    "/bootstrap/**", "/css/**", "/js/**", "/images/**",
                    "/favicon.ico", "/uploads/**", "/vote/**", "/original/**",
                    "/assets/**", "/side/**", "/mypage/**"
                ).permitAll()

                // 4) 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            // JWT 필터 (위에서 인증이 필요한 경로에는 이 필터가 동작)
            .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class)

            // 예외 처리
            .exceptionHandling(ex -> ex
                // 인증 실패시 401
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                // 인가 실패시 403
                .accessDeniedHandler((req, res, denied) ->
                    res.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden"))
            );

        return http.build();
    }
}
