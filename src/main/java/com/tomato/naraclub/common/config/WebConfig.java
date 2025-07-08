package com.tomato.naraclub.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${spring.app.upload.root}")
    private String uploadRoot;


    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // "/" 요청이 들어오면 내부적으로 /main/main.html 을 forward
        registry.addViewController("/").setViewName("forward:/main/main.html");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** 요청을 실제 파일 시스템 위치로 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadRoot + "/");
    }
}
