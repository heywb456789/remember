package com.tomato.remember.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.List;
import java.util.Map;

/**
 * Swagger(OpenAPI) 설정 클래스
 * JWT 인증을 지원하는 토마토리멤버 API 문서화
 */
@Configuration
@SecuritySchemes({
    @SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT 인증 토큰 (예: Bearer eyJhbGciOiJIUzI1NiJ9...)"
    ),
    @SecurityScheme(
        name = "AdminAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "관리자 JWT 인증 토큰"
    )
})
public class OpenApiConfig {

    @Value("${server.port:8096}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    /**
     * 전체 OpenAPI 정보 + 전역 보안 스킴 설정
     */
    @Bean
    public OpenAPI baseOpenAPI() {
        return new OpenAPI()
            .info(createApiInfo())
            .servers(createServers())
            .components(createComponents())
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }

    /**
     * API 정보 생성
     */
    private Info createApiInfo() {
        return new Info()
            .title("🍅 토마토리멤버 API")
            .version("v1.0.0")
            .description("""
                ## 토마토리멤버 Spring Boot API 문서
                
                ### 🔐 인증 방법
                1. **회원 로그인**: `POST /api/auth/login`으로 JWT 토큰 발급
                2. **관리자 로그인**: `POST /admin/api/auth/login`으로 관리자 토큰 발급
                3. 상단의 **'Authorize'** 버튼 클릭
                4. `Bearer {토큰}` 형식으로 입력 (Bearer 뒤 공백 필수)
                
                ### 📚 API 그룹 안내
                - **🔐 Auth API**: 로그인, 회원가입, 토큰 갱신
                - **👨‍👩‍👧‍👦 Family API**: 가족 구성원 관리, 초대, 권한 설정
                - **💭 Memorial API**: 메모리얼 생성/관리, 파일 업로드
                - **📱 Mobile Web API**: 모바일 화면용 통합 API
                - **🛠️ Admin API**: 관리자 전용 API
                
                ### 🎯 주요 기능
                - JWT 기반 인증/인가
                - 가족 초대 및 권한 관리
                - 메모리얼 생성 및 파일 관리
                - 모바일 최적화 API
                
                ### 🔧 개발 환경
                - **프로필**: `{activeProfile}`
                - **포트**: `{serverPort}`
                - **컨텍스트**: `{contextPath}`
                """.replace("{activeProfile}", activeProfile)
                   .replace("{serverPort}", serverPort)
                   .replace("{contextPath}", contextPath))
            .contact(new Contact()
                .name("토마토리멤버 개발팀")
                .email("tomatoai@etomato.com")
                .url("https://remember.newstomato.com"))
            .license(new License()
                .name("토마토리멤버 이용약관")
                .url("https://remember.newstomato.com/terms"));
    }

    /**
     * 서버 목록 생성
     */
    private List<Server> createServers() {
        return List.of(
            new Server()
                .url("http://localhost:" + serverPort + contextPath)
                .description("🏠 로컬 개발 서버"),
            new Server()
                .url("http://114.31.52.64:" + serverPort + contextPath)
                .description("🧪 개발 서버"),
            new Server()
                .url("https://api.remember.newstomato.com" + contextPath)
                .description("🚀 운영 서버")
        );
    }

    /**
     * OpenAPI 컴포넌트 생성
     */
    private io.swagger.v3.oas.models.Components createComponents() {
        return new io.swagger.v3.oas.models.Components()
            .addSecuritySchemes(
                "BearerAuth",
                new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("회원용 JWT 인증 토큰")
            )
            .addSecuritySchemes(
                "AdminAuth",
                new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("관리자용 JWT 인증 토큰")
            )
            .addResponses("UnauthorizedError", new ApiResponse()
                .description("인증 실패")
                .content(new Content()
                    .addMediaType("application/json", new MediaType()
                        .addExamples("default", new io.swagger.v3.oas.models.examples.Example()
                            .value(Map.of(
                                "status", Map.of(
                                    "code", "AUTH_4001",
                                    "message", "인증이 필요합니다."
                                )
                            ))
                        )
                    )
                )
            )
            .addResponses("ForbiddenError", new ApiResponse()
                .description("권한 부족")
                .content(new Content()
                    .addMediaType("application/json", new MediaType()
                        .addExamples("default", new io.swagger.v3.oas.models.examples.Example()
                            .value(Map.of(
                                "status", Map.of(
                                    "code", "AUTH_4003",
                                    "message", "접근 권한이 없습니다."
                                )
                            ))
                        )
                    )
                )
            )
            .addResponses("NotFoundError", new ApiResponse()
                .description("리소스를 찾을 수 없음")
                .content(new Content()
                    .addMediaType("application/json", new MediaType()
                        .addExamples("default", new io.swagger.v3.oas.models.examples.Example()
                            .value(Map.of(
                                "status", Map.of(
                                    "code", "COMMON_4004",
                                    "message", "요청한 리소스를 찾을 수 없습니다."
                                )
                            ))
                        )
                    )
                )
            )
            .addResponses("ValidationError", new ApiResponse()
                .description("입력값 검증 실패")
                .content(new Content()
                    .addMediaType("application/json", new MediaType()
                        .addExamples("default", new io.swagger.v3.oas.models.examples.Example()
                            .value(Map.of(
                                "status", Map.of(
                                    "code", "COMMON_4000",
                                    "message", "입력값이 올바르지 않습니다."
                                ),
                                "errors", List.of(
                                    Map.of("field", "name", "message", "이름은 필수입니다."),
                                    Map.of("field", "email", "message", "올바른 이메일 형식이 아닙니다.")
                                )
                            ))
                        )
                    )
                )
            );
    }

    /**
     * 🔐 인증 관련 API 그룹
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("01-auth")
            .displayName("🔐 Auth API")
            .pathsToMatch("/api/auth/**")
            .build();
    }

    /**
     * 👨‍👩‍👧‍👦 가족 관리 API 그룹
     */
    @Bean
    public GroupedOpenApi familyApi() {
        return GroupedOpenApi.builder()
            .group("02-family")
            .displayName("👨‍👩‍👧‍👦 Family API")
            .pathsToMatch("/api/family/**")
            .build();
    }

    /**
     * 💭 메모리얼 API 그룹
     */
    @Bean
    public GroupedOpenApi memorialApi() {
        return GroupedOpenApi.builder()
            .group("03-memorial")
            .displayName("💭 Memorial API")
            .pathsToMatch("/api/memorial/**", "/api/memorials/**")
            .build();
    }

    /**
     * 📱 모바일 웹 API 그룹
     */
    @Bean
    public GroupedOpenApi mobileApi() {
        return GroupedOpenApi.builder()
            .group("04-mobile")
            .displayName("📱 Mobile Web API")
            .pathsToMatch("/api/**")
            .pathsToExclude(
                "/api/auth/**",
                "/api/family/**",
                "/api/memorial/**",
                "/api/memorials/**",
                "/admin/**"
            )
            .build();
    }

    /**
     * 🛠️ 관리자 API 그룹
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("05-admin")
            .displayName("🛠️ Admin API")
            .pathsToMatch("/admin/**")
            .build();
    }

    /**
     * 🔍 전체 API 그룹 (개발용)
     */
    @Bean
    @Profile({"local", "dev"})
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
            .group("99-all")
            .displayName("🔍 All APIs (개발용)")
            .pathsToMatch("/**")
            .pathsToExclude("/error", "/actuator/**")
            .build();
    }

    /**
     * 전역 Operation 커스터마이저
     */
    @Bean
    public OperationCustomizer globalOperationCustomizer() {
        return (operation, handlerMethod) -> {
            // 공통 응답 코드 추가
            ApiResponses responses = operation.getResponses();

            // 인증이 필요한 API에 401, 403 응답 추가
            if (operation.getSecurity() != null && !operation.getSecurity().isEmpty()) {
                if (!responses.containsKey("401")) {
                    responses.addApiResponse("401", new ApiResponse().$ref("#/components/responses/UnauthorizedError"));
                }
                if (!responses.containsKey("403")) {
                    responses.addApiResponse("403", new ApiResponse().$ref("#/components/responses/ForbiddenError"));
                }
            }

            // 모든 API에 500 응답 추가
            if (!responses.containsKey("500")) {
                responses.addApiResponse("500", new ApiResponse()
                    .description("서버 내부 오류")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .addExamples("default", new io.swagger.v3.oas.models.examples.Example()
                                .value(Map.of(
                                    "status", Map.of(
                                        "code", "COMMON_5000",
                                        "message", "서버 내부 오류가 발생했습니다."
                                    )
                                ))
                            )
                        )
                    )
                );
            }

            return operation;
        };
    }
}