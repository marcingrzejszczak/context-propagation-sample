package com.example.contextpropagation;

import java.util.function.Consumer;

import io.micrometer.context.ContextSnapshot;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("server")
class MyController {

	private static final Log logger = LogFactory.getLog(MyController.class);


	@GetMapping("/foo")
	Mono<String> foo() {
		return Mono.just("bar")
				.transformDeferredContextual((fooMono, contextView) ->
						fooMono.doOnNext(instrumentConsumer(contextView, s ->
								logger.debug("MDC ThreadLocal value is " + MDC.get(MdcThreadLocalAccessor.KEY)))));
	}

	private <T> Consumer<T> instrumentConsumer(ContextView context, Consumer<T> consumer) {
		return ContextSnapshot.forContextAndThreadLocalValues(context).instrumentConsumer(consumer);
	}

}
