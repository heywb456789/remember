package com.tomato.remember;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableScheduling
public class RememberApplication {

	public static void main(String[] args) {
		SpringApplication.run(RememberApplication.class, args);
	}

}
