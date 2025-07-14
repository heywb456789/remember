package com.tomato.remember.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring MVC 설정 - 정적 리소스 핸들링
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir:/uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        // 업로드 디렉토리 생성
        createUploadDirectoryIfNotExists();
        log.info("파일 업로드 디렉토리 초기화: {}", uploadDir);
    }

    //    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//        // "/" 요청이 들어오면 내부적으로 /main/main.html 을 forward
//        registry.addViewController("/").setViewName("forward:/main/main.html");
//        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
//    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 업로드된 파일 정적 리소스 설정
        String resourceLocation = getResourceLocation();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);

        log.info("정적 리소스 핸들러 등록 - OS: {}, 경로: {}",
                System.getProperty("os.name"), resourceLocation);

        // 기본 정적 리소스
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");
    }

    /**
     * OS별 리소스 경로 생성
     */
    private String getResourceLocation() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // 윈도우: file:/// + 경로
            return "file:///" + uploadDir.replace("\\", "/") + "/";
        } else {
            // 리눅스/유닉스: file:// + 경로
            return "file://" + uploadDir + "/";
        }
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
            }

        } catch (Exception e) {
            log.error("업로드 디렉토리 생성 실패: {}", uploadDir, e);
            throw new RuntimeException("업로드 디렉토리 초기화에 실패했습니다.", e);
        }
    }
}