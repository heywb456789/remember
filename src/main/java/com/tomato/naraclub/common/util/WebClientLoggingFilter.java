// src/main/java/com/tomato/naraclub/common/util/WebClientLoggingFilter.java
package com.tomato.naraclub.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

public class WebClientLoggingFilter {

    private static final Logger log = LoggerFactory.getLogger(WebClientLoggingFilter.class);

    /** 요청(Request) 메타데이터(메서드·URL·헤더) 로깅 */
    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("[WebClient][Request] {} {}", request.method(), request.url());
            request.headers()
                   .forEach((name, values) ->
                       values.forEach(v -> log.debug("[WebClient][Request] {}={}", name, v))
                   );
            return Mono.just(request);
        });
    }

    /** 응답(Response) 메타데이터(상태·헤더) 로깅 */
    public static ExchangeFilterFunction logResponseBody() {
        return ExchangeFilterFunction.ofResponseProcessor(response ->
            response.bodyToMono(String.class)
                    .defaultIfEmpty("")                   // 빈 바디도 대비
                    .flatMap(body -> {
                        log.debug("[WebClient][Response][Body] {}", body);

                        // 바디를 재사용할 수 있도록, 새로운 ClientResponse 생성
                        ClientResponse copy = ClientResponse.create(response.statusCode())
                            .headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
                            .body(body)
                            .build();

                        return Mono.just(copy);
                    })
        );
    }

    /**
     * (선택) 응답 바디까지 보고 싶다면 주석 해제하고 사용하세요.
     * 주의: 바디를 읽으면 스트림이 소모되므로, 이후 다시 읽을 수 있도록
     * 새 ClientResponse를 만들어야 합니다.
     */
    /*
    public static ExchangeFilterFunction logResponseBody() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> 
            resp.bodyToMono(String.class)
                .flatMap(body -> {
                    log.debug("[WebClient][Response][Body] {}", body);
                    return Mono.just(
                        ClientResponse.create(resp.statusCode())
                                      .headers(h -> h.addAll(resp.headers().asHttpHeaders()))
                                      .body(body)
                                      .build()
                    );
                })
        );
    }
    */
}
