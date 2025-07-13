package com.tomato.remember.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.path:/uploads}")
    private String uploadPath;


//    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//        // "/" 요청이 들어오면 내부적으로 /main/main.html 을 forward
//        registry.addViewController("/").setViewName("forward:/main/main.html");
//        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
//    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // OS별 경로 처리
        String resourceLocation;

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // 윈도우: file:/// 프로토콜 + 드라이브 문자 처리
            resourceLocation = "file:///" + uploadPath.replace("\\", "/") + "/";
        } else {
            // 리눅스/유닉스: file:// 프로토콜
            resourceLocation = "file://" + uploadPath + "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);

        log.info("정적 리소스 핸들러 등록 - OS: {}, 경로: {}",
                System.getProperty("os.name"), resourceLocation);

        //origianla
        // /uploads/** 요청을 실제 파일 시스템 위치로 매핑
//        registry.addResourceHandler("/uploads/**")
//                .addResourceLocations("file:" + uploadRoot + "/");
    }
}
