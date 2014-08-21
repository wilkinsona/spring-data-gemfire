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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.gemstone.gemfire.cache.RegionAttributes;

/**
 * Spring-friendly bean for creating {@link RegionAttributes}. Eliminates the need of using
 * a XML 'factory-method' tag.
 *
 * @author Costin Leau
 * @author John Blum
 * @see org.springframework.beans.factory.FactoryBean
 * @see org.springframework.beans.factory.InitializingBean
 * @see com.gemstone.gemfire.cache.AttributesFactory
 * @see com.gemstone.gemfire.cache.RegionAttributes
 */
@SuppressWarnings("deprecation")
public class RegionAttributesFactoryBean extends com.gemstone.gemfire.cache.AttributesFactory
		implements FactoryBean<RegionAttributes>, InitializingBean {

	private RegionAttributes attributes;

	@Override
	public RegionAttributes getObject() throws Exception {
		return attributes;
	}

	@Override
	public Class<?> getObjectType() {
		return (attributes != null ? attributes.getClass() : RegionAttributes.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		attributes = super.create();
	}

}
