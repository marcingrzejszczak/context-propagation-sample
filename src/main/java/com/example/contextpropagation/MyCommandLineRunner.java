package com.example.contextpropagation;

import java.util.function.Consumer;

import io.micrometer.context.ContextSnapshot;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Profile("client")
class MyCommandLineRunner implements CommandLineRunner {

	private static final Log logger = LogFactory.getLog(MyCommandLineRunner.class);


	private final MyReactiveService service = new MyReactiveService();


	@Override
	public void run(String... args) {

		MDC.put(MdcThreadLocalAccessor.KEY, "123");
		logger.debug("Set MDC ThreadLocal value to 123");

		ContextSnapshot snapshot = ContextSnapshot.forContextAndThreadLocalValues();
		logger.debug("Created " + snapshot);

		String fooValue = this.service.retrieveFoo()
				.contextWrite(snapshot::updateContext)
				.block();

		logger.debug("fooValue=" + fooValue);
	}


	private static class MyReactiveService {

		public Mono<String> retrieveFoo() {
			return WebClient.builder().baseUrl("http://localhost:8080").build()
					.get().uri("/foo")
					.retrieve()
					.bodyToMono(String.class)
					.transformDeferredContextual((stringMono, contextView) ->
							stringMono.doOnNext(instrumentConsumer(contextView, value -> {
								logger.debug("MDC ThreadLocal value is " + MDC.get(MdcThreadLocalAccessor.KEY));
							})));
		}

		private <T> Consumer<T> instrumentConsumer(ContextView context, Consumer<T> consumer) {
			return ContextSnapshot.forContextAndThreadLocalValues(context).instrumentConsumer(consumer);
		}
	}

}
