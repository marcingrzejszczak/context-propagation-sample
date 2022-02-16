package com.example.contextpropagation.spi;

import reactor.netty.observability.contextpropagation.ContextContainer;
import reactor.netty.observability.contextpropagation.ReactorContextAccessor;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

public class ReactorKeyReactorContextAccessor implements ReactorContextAccessor {

	public static final String key = "REACTOR-KEY";

	@Override
	public void captureValues(ContextView view, ContextContainer container) {
		container.put(key, view.get(key));
	}

	@Override
	public Context restoreValues(Context context, ContextContainer container) {
		return context.put(key, context.get(key));
	}
}