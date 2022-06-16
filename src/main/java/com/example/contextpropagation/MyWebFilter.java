package com.example.contextpropagation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

@Component
@Profile("server")
class MyWebFilter implements WebFilter {

	private static final Log logger = LogFactory.getLog(MyWebFilter.class);


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return chain.filter(exchange)
				.contextWrite(context -> {
					logger.debug("Setting Reactor Context value " + MdcThreadLocalAccessor.KEY + "=123");
					return context.put(MdcThreadLocalAccessor.KEY, "123");
				});
	}

}
