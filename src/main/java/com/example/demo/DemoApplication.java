package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Spring Boot 應用程式啟動類
 */
@SpringBootApplication
@EnableRedisRepositories("com.example.demo.shared.infrastructure.repository.redis")
@EnableR2dbcRepositories("com.example.demo.shared.infrastructure.repository.r2dbc")
public class DemoApplication {

	/**
	 * 應用程式主入口點
	 * @param args 命令列參數
	 */
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
