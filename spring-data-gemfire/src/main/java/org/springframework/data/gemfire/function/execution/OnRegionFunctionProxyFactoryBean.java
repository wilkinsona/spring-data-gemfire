/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.data.gemfire.function.execution;


import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.data.gemfire.util.ArrayUtils;

/**
 * @author David Turanski
 *
 */
public class OnRegionFunctionProxyFactoryBean extends GemfireFunctionProxyFactoryBean {

	private OnRegionExecutionMethodMetadata methodMetadata;

	/**
	 * @param serviceInterface the Service class interface specifying the operations to proxy.
	 * @param gemfireOnRegionOperations an {@link GemfireOnRegionOperations} instance
	 */
	public OnRegionFunctionProxyFactoryBean(Class<?> serviceInterface, GemfireOnRegionOperations gemfireOnRegionOperations) {
		super(serviceInterface, gemfireOnRegionOperations);
		methodMetadata = new OnRegionExecutionMethodMetadata(serviceInterface);
	}

	@Override
	protected Iterable<?> invokeFunction(Method method, Object[] args) {
		
		Set<?> filter = null;
		
		Iterable<?> results = null;
		
		GemfireOnRegionOperations gemfireOnRegionOperations = (GemfireOnRegionOperations) this.gemfireFunctionOperations;
		
		OnRegionMethodMetadata ormmd = methodMetadata.getMethodMetadata(method);
		
		int filterArgPosition = ormmd.getFilterArgPosition();
		
		String functionId = ormmd.getFunctionId();
		
		/*
		 * extract filter from args if necessary
		 */
		if (filterArgPosition >=0 ) {
			filter = (Set<?>)args[filterArgPosition];
			args = ArrayUtils.remove(args, filterArgPosition);
		}
		
		if (filter == null) {
			results =  gemfireOnRegionOperations.execute(functionId, args);
		} else {
			results =  gemfireOnRegionOperations.execute(functionId, filter, args);
		}
 
		return results;
	}
}
