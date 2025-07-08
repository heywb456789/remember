package com.tomato.naraclub.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TomcatPostSizeLogger implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
            connector.setMaxPostSize(524288000); // 500MB (바이트 단위)
            int maxPostSize = connector.getMaxPostSize(); // 기본값: 2097152
            log.info("🚀 Tomcat maxPostSize (bytes): {}" , maxPostSize);
        });
    }
}
