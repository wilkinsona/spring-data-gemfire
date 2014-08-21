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

import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.RegionService;
import com.gemstone.gemfire.cache.execute.Execution;
import com.gemstone.gemfire.cache.execute.FunctionService;

/**
 * Creates a GemFire {@link Execution} using {code}FunctionService.onServer(RegionService regionService){code}
 * @author David Turanski
 *
 */
class ServerFunctionExecution extends AbstractFunctionExecution {
	

	private RegionService regionService;

  
	public ServerFunctionExecution(RegionService regionService) {
		super();
		Assert.notNull(regionService,"regionService cannot be null");
		this.regionService = regionService; 
	}
	
 
	@Override
	protected Execution getExecution() {
		return FunctionService.onServer(this.regionService);
	}
}
