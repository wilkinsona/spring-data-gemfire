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
package org.springframework.data.gemfire.function.config;

import org.springframework.core.type.filter.TypeFilter;

/**
 * Interface for function execution configuration sources (e.g., annotation or XML configuration) to configure 
 * classpath scanning of annotated interfaces to implement proxies that invoke Gemfire functions
 * 
 * @author David Turanski
 *
 */
interface FunctionExecutionConfigurationSource {
	/**
	 * Returns the actual source object that the configuration originated from. Will be used by the tooling to give visual
	 * feedback on where the repository instances actually come from.
	 * 
	 * @return must not be {@literal null}.
	 */
	Object getSource();

	/**
	 * Returns the base packages the repository interfaces shall be found under.
	 * 
	 * @return must not be {@literal null}.
	 */
	Iterable<String> getBasePackages();
 
	
	/**
	 * Returns configured {@link TypeFilter}s
	 * @return include filters
	 */
	Iterable<TypeFilter> getIncludeFilters();
	
	/**
	 * Returns configured {@link TypeFilter}s
	 * @return exclude filters
	 */
	Iterable<TypeFilter> getExcludeFilters();
	
}
