package com.transport;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));  // 서버 시간대(EC2는 UTC)와 상관없이 LocalDateTime.now()가 한국 시간
		SpringApplication.run(BackendApplication.class, args);
	}

}
