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

import java.util.Arrays;

import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.gemfire.function.config.TypeFilterParser.Type;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author David Turanski
 *
 */
class XmlFunctionExecutionConfigurationSource extends AbstractFunctionExecutionConfigurationSource {
	private static final String BASE_PACKAGE = "base-package";
	private Element element;
	private ParserContext context;
	private Iterable<TypeFilter> includeFilters;
	private Iterable<TypeFilter> excludeFilters;

	XmlFunctionExecutionConfigurationSource(Element element, ParserContext context) {
		Assert.notNull(element);
		Assert.notNull(context);

		this.element = element;
		this.context = context;

		TypeFilterParser parser = new TypeFilterParser(context.getReaderContext());
		this.includeFilters = parser.parseTypeFilters(element, Type.INCLUDE);
		this.excludeFilters = parser.parseTypeFilters(element, Type.EXCLUDE);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.gemfire.function.config.FunctionExecutionConfigurationSource#getSource()
	 */
	@Override
	public Object getSource() {
		return context.extractSource(element);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.gemfire.function.config.FunctionExecutionConfigurationSource#getBasePackages()
	 */
	@Override
	public Iterable<String> getBasePackages() {
		String attribute = element.getAttribute(BASE_PACKAGE);
		return Arrays.asList(StringUtils.delimitedListToStringArray(attribute, ",", " "));
	}

	 
	/* (non-Javadoc)
	 * @see org.springframework.data.gemfire.function.config.FunctionExecutionConfigurationSource#getIncludeFilters()
	 */
	@Override
	public Iterable<TypeFilter> getIncludeFilters() {
		return includeFilters;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.gemfire.function.config.FunctionExecutionConfigurationSource#getExcludeFilters()
	 */
	@Override
	public Iterable<TypeFilter> getExcludeFilters() {
		return excludeFilters;
	}

}
