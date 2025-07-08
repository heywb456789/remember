package com.tomato.naraclub.application.file;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.file
 * @fileName : FileViewController
 * @date : 2025-05-19
 * @description :
 * @AUTHOR : MinjaeKim
 */
@RestController
@RequestMapping("/file/profile")
public class FileViewController {

    private final WebClient webClient;

    public FileViewController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://api.otongtong.net:28080").build();
    }

    @GetMapping("/{userkey}/{folder}/{filename:.+}")
    public Mono<ResponseEntity<byte[]>> proxy(
            @PathVariable String userkey,
            @PathVariable String folder,
            @PathVariable String filename) {
        String remotePath = String.format("/file/profile/%s/%s/%s", userkey,folder, filename);

        return webClient.get()
                .uri(remotePath)
                .accept(MediaType.ALL)
                .retrieve()
                .toEntity(byte[].class)
                .map(resp -> {
                    // 원본 Content-Type 가져오기
                    MediaType contentType = resp.getHeaders().getContentType();
                    return ResponseEntity.status(resp.getStatusCode())
                            .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                            .contentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM)
                            .body(resp.getBody());
                });
    }
}
