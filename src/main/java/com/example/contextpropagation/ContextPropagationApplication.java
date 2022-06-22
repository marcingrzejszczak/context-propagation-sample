package com.example.contextpropagation;

import io.micrometer.context.ContextRegistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ContextPropagationApplication {

	public static void main(String[] args) {

		ContextRegistry.getInstance().registerThreadLocalAccessor(new MdcThreadLocalAccessor());

		SpringApplication.run(ContextPropagationApplication.class, args);
	}

}


