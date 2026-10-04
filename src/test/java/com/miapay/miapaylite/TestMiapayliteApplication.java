package com.miapay.miapaylite;

import org.springframework.boot.SpringApplication;

public class TestMiapayliteApplication {

	public static void main(String[] args) {
		SpringApplication.from(MiapayliteApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
