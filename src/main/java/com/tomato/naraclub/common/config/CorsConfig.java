// src/main/java/com/tomato/naraclub/common/config/CorsConfig.java
package com.tomato.naraclub.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        var config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 허용할 Origin 목록 (앱에서 호출하는 도메인 혹은 '*'로 전체 허용)
        config.addAllowedOriginPattern("*");
        // 허용할 HTTP 메서드
        config.addAllowedMethod("*");
        // 허용할 헤더
        config.addAllowedHeader("*");
        // 노출할 응답 헤더
        config.addExposedHeader("Authorization");
        
        var source = new UrlBasedCorsConfigurationSource();
        // 모든 경로에 대해 위 설정 적용
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
