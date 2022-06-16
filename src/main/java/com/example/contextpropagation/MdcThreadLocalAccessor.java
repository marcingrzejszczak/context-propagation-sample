package com.example.contextpropagation;

import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;

public class MdcThreadLocalAccessor implements ThreadLocalAccessor<String> {

	public static final String KEY = "MDC.ID";

	@Override
	public Object key() {
		return KEY;
	}

	@Override
	public String getValue() {
		return MDC.get(KEY);
	}

	@Override
	public void setValue(String value) {
		MDC.put(KEY, value);
	}

	@Override
	public void reset() {
		MDC.remove(KEY);
	}

}
