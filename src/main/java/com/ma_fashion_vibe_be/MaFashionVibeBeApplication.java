package com.ma_fashion_vibe_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // BẬT TÍNH NĂNG CHẠY NGẦM LÊN
public class MaFashionVibeBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(MaFashionVibeBeApplication.class, args);
	}

}
