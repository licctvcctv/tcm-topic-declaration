package com.project.declaration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.project.declaration.mapper")
public class TopicDeclarationApplication {

	public static void main(String[] args) {
		SpringApplication.run(TopicDeclarationApplication.class, args);
	}

}
