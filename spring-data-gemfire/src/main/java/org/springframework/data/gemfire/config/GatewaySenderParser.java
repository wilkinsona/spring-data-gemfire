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
package org.springframework.data.gemfire.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.gemfire.wan.GatewaySenderFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * @author David Turanski
 * @author John Blum
 * @see org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser
 */
class GatewaySenderParser extends AbstractSimpleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return GatewaySenderFactoryBean.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String cacheRef = element.getAttribute("cache-ref");

		builder.addConstructorArgReference((StringUtils.hasText(cacheRef) ? cacheRef
			: GemfireConstants.DEFAULT_GEMFIRE_CACHE_NAME));

		ParsingUtils.setPropertyValue(element, builder, NAME_ATTRIBUTE);
		ParsingUtils.setPropertyValue(element, builder, "alert-threshold");
		ParsingUtils.setPropertyValue(element, builder, "batch-size");
		ParsingUtils.setPropertyValue(element, builder, "batch-time-interval");
		ParsingUtils.setPropertyValue(element, builder, "disk-store-ref");
		ParsingUtils.setPropertyValue(element, builder, "disk-synchronous");
		ParsingUtils.setPropertyValue(element, builder, "dispatcher-threads");
		ParsingUtils.setPropertyValue(element, builder, "enable-batch-conflation");
		ParsingUtils.setPropertyValue(element, builder, "manual-start");
		ParsingUtils.setPropertyValue(element, builder, "maximum-queue-memory");
		ParsingUtils.setPropertyValue(element, builder, "order-policy");
		ParsingUtils.setPropertyValue(element, builder, "parallel");
		ParsingUtils.setPropertyValue(element, builder, "persistent");
		ParsingUtils.setPropertyValue(element, builder, "remote-distributed-system-id");
		ParsingUtils.setPropertyValue(element, builder, "socket-buffer-size");
		ParsingUtils.setPropertyValue(element, builder, "socket-read-timeout");

		Element eventFilterElement = DomUtils.getChildElementByTagName(element, "event-filter");

		if (eventFilterElement != null) {
			builder.addPropertyValue("eventFilters", ParsingUtils.parseRefOrNestedBeanDeclaration(
				parserContext, eventFilterElement, builder));
		}

		ParsingUtils.parseTransportFilters(element, parserContext, builder);

		 // set the name for the GatewaySender as an inner bean
		if (!StringUtils.hasText(element.getAttribute(NAME_ATTRIBUTE))) {
			if (element.getParentNode().getNodeName().endsWith("region")) {
				Element region = (Element) element.getParentNode();

				String regionName = (StringUtils.hasText(region.getAttribute(NAME_ATTRIBUTE))
					? region.getAttribute(NAME_ATTRIBUTE) : region.getAttribute(ID_ATTRIBUTE));

				int number = 0;

				String gatewaySenderName = (regionName + ".gatewaySender#" + number);

				while (parserContext.getRegistry().isBeanNameInUse(gatewaySenderName)) {
					gatewaySenderName = (regionName + ".gatewaySender#" + (++number));
				}

				builder.addPropertyValue("name", gatewaySenderName);
			}
		}
	}

}
