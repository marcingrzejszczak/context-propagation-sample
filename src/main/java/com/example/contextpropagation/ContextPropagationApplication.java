package com.example.contextpropagation;

import java.awt.Container;
import java.util.function.BiFunction;

import com.example.contextpropagation.holder.MdcThreadLocalHolder;
import com.example.contextpropagation.spi.MdcThreadLocalAccessor;
import io.micrometer.contextpropagation.ContextContainer;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

@SpringBootApplication
public class ContextPropagationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContextPropagationApplication.class, args);
	}

}


@Component
@Profile("client")
class MyCommandLineRunner implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {

		MdcThreadLocalHolder.set("MDC-VALUE");

		ContextContainer container = ContextContainer.create().captureThreadLocalValues();

		String string = WebClient.builder().baseUrl("http://localhost:8080").build()
				.get().uri("/foo")
				.retrieve()
				.bodyToMono(String.class)
				.transformDeferredContextual(Foo.wrap((stringMono, restoredContainer) -> stringMono.doOnNext(s -> {
					try (ContextContainer.Scope scope = restoredContainer.restoreThreadLocalValues()) {
						// Thread local
						String result = MDC.get(MdcThreadLocalHolder.key);
						Assert.isTrue("MDC-VALUE".equals(result), "Context propagation is not working");
					}
				})))
				.contextWrite(container::save)
				.block();

		Assert.isTrue("bar".equals(string), "Boom!");
		System.out.println("Everything is working fine!!");
	}

}

class Foo {

	public static BiFunction<Mono<String>, ContextView, Publisher<String>> wrap(BiFunction<Mono<String>, ContextContainer, Publisher<String>> arg) {
		return (stringMono, contextView) -> {
			ContextContainer restoredContainer = ContextContainer.restore(contextView);
			return arg.apply(stringMono, restoredContainer);
		};
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
					ContextContainer container = ContextContainer.restore(contextView);

					// TODO: why not container.capture(contextView) ? we wouldn't need any utils
					// TODO: We could have an SPI (?) to plug in various mechanisms - or just utils for everything
					container.captureContext(contextView);

					Assert.isTrue("REACTOR-VALUE".equals(container.get("REACTOR-KEY")), "Context propagation is not working for reactor accessors");
					// Put container values back in thread local
					try (ContextContainer.Scope scope = container.restoreThreadLocalValues()) {
							// Thread local
							String result = MDC.get("MDC-KEY");
							 Assert.isTrue("THIS IS SET IN REACTOR BUT WILL BE RESOLVED AS THREAD LOCAL".equals(result), "Context propagation is not working for thread local");
						}
					}));
	}
}

@Component
@Profile("server")
class MyWebFilter implements WebFilter {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return Mono.deferContextual(contextView -> {
			ContextContainer container = ContextContainer.create();
			container.put(MdcThreadLocalAccessor.KEY, "THIS IS SET IN REACTOR BUT WILL BE RESOLVED AS THREAD LOCAL");
			return chain.filter(exchange)
					.contextWrite(context -> container.save(context.put("REACTOR-KEY", "REACTOR-VALUE")));
		});
	}
}
