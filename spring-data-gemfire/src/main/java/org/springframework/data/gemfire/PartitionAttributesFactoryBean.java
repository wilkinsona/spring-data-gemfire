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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.gemstone.gemfire.cache.FixedPartitionAttributes;
import com.gemstone.gemfire.cache.PartitionAttributes;
import com.gemstone.gemfire.cache.PartitionResolver;
import com.gemstone.gemfire.cache.partition.PartitionListener;

/**
 * Spring-friendly bean for creating {@link PartitionAttributes}. Eliminates the
 * need of using a XML 'factory-method' tag and allows the attributes properties
 * to be set directly.
 * 
 * @author Costin Leau
 * @author David Turanski
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PartitionAttributesFactoryBean implements FactoryBean<PartitionAttributes>, InitializingBean {

	private final com.gemstone.gemfire.cache.PartitionAttributesFactory paf = new com.gemstone.gemfire.cache.PartitionAttributesFactory();

	private List<PartitionListener> listeners;

	@Override
	public PartitionAttributes getObject() throws Exception {
		return paf.create();
	}

	@Override
	public Class<?> getObjectType() {
		return PartitionAttributes.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	public void setColocatedWith(String colocatedRegionFullPath) {
		paf.setColocatedWith(colocatedRegionFullPath);
	}

	public void setFixedPartitionAttributes(List<FixedPartitionAttributes> fixedPartitionAttributes) {
		for (FixedPartitionAttributes fpa : fixedPartitionAttributes) {
			paf.addFixedPartitionAttributes(fpa);
		}
	}

	public void setLocalMaxMemory(int mb) {
		paf.setLocalMaxMemory(mb);
	}

	public void setPartitionResolver(PartitionResolver resolver) {
		paf.setPartitionResolver(resolver);
	}

	public void setPartitionListeners(List<PartitionListener> listeners) {
		this.listeners = listeners;
	}

	public void setRecoveryDelay(long recoveryDelay) {
		paf.setRecoveryDelay(recoveryDelay);
	}

	public void setRedundantCopies(int redundantCopies) {
		paf.setRedundantCopies(redundantCopies);
	}

	public void setStartupRecoveryDelay(long startupRecoveryDelay) {
		paf.setStartupRecoveryDelay(startupRecoveryDelay);
	}

	public void setTotalMaxMemory(long mb) {
		paf.setTotalMaxMemory(mb);
	}

	public void setTotalNumBuckets(int numBuckets) {
		paf.setTotalNumBuckets(numBuckets);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (listeners != null) {
			for (PartitionListener listener : listeners) {
				paf.addPartitionListener(listener);
			}
		}

	}
}