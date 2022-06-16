package com.example.contextpropagation;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.reactor.ReactorContextAccessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ContextPropagationApplication {

	public static void main(String[] args) {

		ContextRegistry.getInstance().registerThreadLocalAccessor(new MdcThreadLocalAccessor());
		ContextRegistry.getInstance().registerContextAccessor(new ReactorContextAccessor());

		SpringApplication.run(ContextPropagationApplication.class, args);
	}

}


