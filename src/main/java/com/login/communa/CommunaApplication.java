package com.login.communa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.login.communa")
public class CommunaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommunaApplication.class, args);
	}

}
