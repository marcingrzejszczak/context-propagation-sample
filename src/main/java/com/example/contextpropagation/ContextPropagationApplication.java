package com.example.contextpropagation;

import java.awt.Container;

import com.example.contextpropagation.holder.MdcThreadLocalHolder;
import com.example.contextpropagation.spi.MdcThreadLocalAccessor;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.netty.observability.contextpropagation.ContextContainer;
import reactor.netty.observability.contextpropagation.ReactorContextUtils;
import reactor.netty.observability.contextpropagation.propagator.ContainerUtils;
import reactor.util.context.Context;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class ContextPropagationApplication {

	public static void main(String[] args) {
		MdcThreadLocalHolder.set("MDC-VALUE");
		SpringApplication.run(ContextPropagationApplication.class, args);
	}

}


@Component
@Profile("client")
class MyCommandLineRunner implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {

		ContextContainer container = ContextContainer.create().captureThreadLocalValues();

		WebClient.builder().baseUrl("http://localhost:8080").build()
				.get().uri("/foo")
				.retrieve()
				.bodyToMono(String.class)
				.transformDeferredContextual((stringMono, contextView) -> stringMono.doOnNext(s -> {
					// Retrieve the container from context
					ContextContainer restoredContainer = ContainerUtils.restoreContainer(contextView);
					// Put container values back in thread local
					try (ContextContainer.Scope scope = container.restoreThreadLocalValues()) {
						// Thread local
						String result = MDC.get("MDC-KEY");
						Assert.isTrue(result.equals("MDC-VALUE"), "Context propagation is not working");
					}
				}))
				.contextWrite(context -> ContainerUtils.saveContainer(context, container));
	}
}



@RestController
@Profile("server")
class MyWebFlux {

	@GetMapping("/foo")
	Mono<String> foo() {
		return Mono.just("bar")
				.transformDeferredContextual((stringMono, contextView) -> stringMono.doOnNext(s -> {
						// Retrieve the container from context
					ContextContainer container = ContainerUtils.restoreContainer(contextView);

					Assert.isTrue("REACTOR-VALUE".equals(container.get("REACTOR-KEY")), "Context propagation is not working");
					// Put container values back in thread local
					try (ContextContainer.Scope scope = container.restoreThreadLocalValues()) {
							// Thread local
							String result = MDC.get("MDC-KEY");
							Assert.isTrue(result.equals("MDC-VALUE"), "Context propagation is not working");
						}
					}))
				.contextWrite(context -> {
					// We put a new value
					Context newContext = context.put("REACTOR-KEY", "REACTOR-VALUE");

					// Capture thread local
					ContextContainer contextContainer = ReactorContextUtils.create().captureThreadLocalValues();

					// TODO: This should populate the MDC-KEY in the MdcThreadLocalHolder
					// TODO: Some public API
					// contextContainer.put("MDC-KEY", "alksdjsalkda");
					MdcThreadLocalAccessor accessor = contextContainer.get(MdcThreadLocalAccessor.KEY);

					// Copy from context to container
					ReactorContextUtils.captureReactorContext(context, contextContainer);
					// Copy the container to the context
					return ContainerUtils.saveContainer(newContext, contextContainer);
				});
	}
}

// TODO: Add a webfilter that interacts with a MDC key
