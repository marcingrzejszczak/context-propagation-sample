/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.contextpropagation.spi;

import com.example.contextpropagation.holder.MdcThreadLocalHolder;
import reactor.netty.observability.contextpropagation.ContextContainer;
import reactor.netty.observability.contextpropagation.ThreadLocalAccessor;

public class MdcThreadLocalAccessor implements ThreadLocalAccessor {

	public static final String KEY = MdcThreadLocalAccessor.class.getName();

	@Override
	public void captureValues(ContextContainer container) {
		String value = MdcThreadLocalHolder.get();
		if (value != null) {
			container.put(KEY, value);
		}
	}

	@Override
	public void restoreValues(ContextContainer container) {
		if (container.containsKey(KEY)) {
			MdcThreadLocalHolder.set(KEY);
		}
	}

	@Override
	public void resetValues(ContextContainer container) {
		MdcThreadLocalHolder.reset();
	}

}
