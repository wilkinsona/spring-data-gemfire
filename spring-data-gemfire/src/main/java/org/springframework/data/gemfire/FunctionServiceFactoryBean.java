/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.gemfire;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import com.gemstone.gemfire.cache.execute.Function;
import com.gemstone.gemfire.cache.execute.FunctionService;

/**
 * @author David Turanski
 * 
 */
public class FunctionServiceFactoryBean implements FactoryBean<FunctionService>, InitializingBean {
	private static FunctionService functionService;
	private static Log logger = LogFactory.getLog(FunctionServiceFactoryBean.class);
	private List<Function> functions;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!CollectionUtils.isEmpty(functions)) {
			for (Function function : functions) {
				if (logger.isInfoEnabled()) {
					logger.info("registering function with ID: " + function.getId());
				}
				FunctionService.registerFunction(function);
			}
		}
	}

	public void setFunctions(List<Function> functions) {
		this.functions = functions;
	}

	@Override
	public FunctionService getObject() throws Exception {
		return functionService;
	}

	@Override
	public Class<?> getObjectType() {
		return FunctionService.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
