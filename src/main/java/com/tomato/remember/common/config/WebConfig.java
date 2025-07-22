package com.tomato.remember.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring MVC 설정 - 정적 리소스 핸들링 (수정됨)
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir:./uploads/local}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        // 업로드 디렉토리 생성
        createUploadDirectoryIfNotExists();
        log.info("파일 업로드 디렉토리 초기화: {}", uploadDir);

        // 절대 경로 로그 출력
        try {
            Path absolutePath = Paths.get(uploadDir).toAbsolutePath();
            log.info("업로드 디렉토리 절대 경로: {}", absolutePath);
        } catch (Exception e) {
            log.error("절대 경로 확인 실패", e);
        }
    }

    //    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//        // "/" 요청이 들어오면 내부적으로 /main/main.html 을 forward
//        registry.addViewController("/").setViewName("forward:/main/main.html");
//        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
//    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 절대 경로로 변환
        String absoluteUploadDir = getAbsoluteUploadDir();
        String resourceLocation = getResourceLocation(absoluteUploadDir);

        // /uploads/** → ./uploads/local/** 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(0); // 캐시 비활성화 (개발용)

        log.info("정적 리소스 핸들러 등록:");
        log.info("  - URL 패턴: /uploads/**");
        log.info("  - 리소스 위치: {}", resourceLocation);
        log.info("  - OS: {}", System.getProperty("os.name"));

        // 기본 정적 리소스
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");
    }

    /**
     * 절대 경로로 변환
     */
    private String getAbsoluteUploadDir() {
        try {
            Path path = Paths.get(uploadDir);
            String absolutePath = path.toAbsolutePath().normalize().toString();
            log.info("업로드 디렉토리 절대 경로 변환: {} → {}", uploadDir, absolutePath);
            return absolutePath;
        } catch (Exception e) {
            log.error("절대 경로 변환 실패: {}", uploadDir, e);
            return uploadDir;
        }
    }

    /**
     * OS별 리소스 경로 생성 (수정됨)
     */
    private String getResourceLocation(String absolutePath) {
        String osName = System.getProperty("os.name").toLowerCase();
        String resourceLocation;

        if (osName.contains("win")) {
            // 윈도우: file:///C:/path/to/uploads/
            resourceLocation = "file:///" + absolutePath.replace("\\", "/") + "/";
        } else {
            // 리눅스/유닉스: file:///path/to/uploads/
            resourceLocation = "file://" + absolutePath + "/";
        }

        log.info("리소스 위치 생성: OS={}, 경로={}", osName, resourceLocation);
        return resourceLocation;
    }

    /**
     * 업로드 디렉토리 생성
     */
    private void createUploadDirectoryIfNotExists() {
        try {
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("업로드 디렉토리 생성: {}", uploadPath.toAbsolutePath());
            } else {
                log.info("업로드 디렉토리 존재 확인: {}", uploadPath.toAbsolutePath());
            }

            // 하위 디렉토리도 미리 생성
            Path profilePath = uploadPath.resolve("profile");
            if (!Files.exists(profilePath)) {
                Files.createDirectories(profilePath);
                log.info("프로필 디렉토리 생성: {}", profilePath.toAbsolutePath());
            }

        } catch (Exception e) {
            log.error("업로드 디렉토리 생성 실패: {}", uploadDir, e);
            throw new RuntimeException("업로드 디렉토리 초기화에 실패했습니다.", e);
        }
    }
}