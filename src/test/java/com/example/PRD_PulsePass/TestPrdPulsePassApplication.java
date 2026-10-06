package com.example.PRD_PulsePass;

import org.springframework.boot.SpringApplication;

public class TestPrdPulsePassApplication {

	public static void main(String[] args) {
		SpringApplication.from(PrdPulsePassApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
