package com.tomato.naraclub.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger(OpenAPI) 설정 클래스
 */
@Configuration
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "JWT 토큰을 입력하세요"
)
public class OpenApiConfig {

    @Value("${server.port:8032}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * 전체 OpenAPI 정보 + 전역 보안 스킴 설정
     */
    @Bean
    public OpenAPI baseOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NaraClub API")
                .version("v1.0.0")
                .description("""
                    ## NaraClub Spring Boot API 문서
                    
                    ### 인증 방법
                    1. 로그인 API를 통해 JWT 토큰을 발급받습니다
                    2. 상단의 'Authorize' 버튼을 클릭합니다
                    3. 'Bearer {토큰}' 형식으로 입력합니다 (Bearer 뒤에 공백 필수)
                    
                    ### API 그룹
                    - **Admin API**: 관리자 전용 API
                    - **Mobile API**: 모바일 웹뷰용 API
                    - **Auth API**: 인증 API
                    """)
                .contact(new Contact()
                    .name("NaraClub Team")
                    .email("admin@naraclub.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server()
                    .url("http://114.31.52.64:" + serverPort + contextPath)
                    .description("로컬 개발 서버"),
                new Server()
                    .url("https://club1.newstomato.com")
                    .description("운영 서버")
            ))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(
                new io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        "BearerAuth",
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                            .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT 인증 토큰")
                    )
            );
    }

    /**
     * 관리자 API 그룹
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("01-admin")
            .displayName("Admin API")
            .pathsToMatch("/admin/**")
            .pathsToExclude("/api/**")
            .build();
    }


    /**
     * 인증 관련 API 그룹
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("02-auth")
            .displayName("Auth API")
            .pathsToMatch("/api/auth/**")
            .pathsToExclude(
                "/admin/**","/api/news/**",
                "/api/board/**", "/api/vote/**",
                "/api/videos/**", "/api/subscription/**",
                "/api/share/**", "/api/search/**",
                "/api/members/**", "/api/user/**"
            )
            .build();
    }

    /**
     * 전체 API 그룹
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
            .group("03-mobile-web")
            .displayName("Mobile Web API")
            .pathsToMatch("/api/**")
            .pathsToExclude("/api/auth/**", "/admin/**")
            .build();
    }
}