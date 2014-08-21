/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.gemfire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.test.GemfireTestApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.gemfire.cache.DiskStore;

/**
 * The DiskStoreBeanUsingPropertyPlaceholdersIntegrationTest class is a test suite of integration tests testing the use
 * of Spring PropertyPlaceholders to configure and initialize a Disk Store bean's properties using property placeholders
 * in the SDG XML namespace &lt;disk-store&gt; bean definition attributes.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.springframework.data.gemfire.DiskStoreFactoryBean
 * @see org.springframework.data.gemfire.test.GemfireTestApplicationContextInitializer
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @see com.gemstone.gemfire.cache.DiskStore
 * @link https://jira.springsource.org/browse/SGF-249
 * @since 1.3.4
 */
@ContextConfiguration(locations = "diskstore-using-propertyplaceholders-config.xml",
	initializers = GemfireTestApplicationContextInitializer.class)
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("unused")
public class DiskStoreBeanUsingPropertyPlaceholdersIntegrationTest {

	@Autowired
	private DiskStore testDataStore;

	@Resource(name="diskStoreConfiguration")
	private Map<String, Object> diskStoreConfiguration;

	@SuppressWarnings("unchecked")
	protected Object getExpectedValue(final String propertyPlaceholderName) {
		return diskStoreConfiguration.get(propertyPlaceholderName);
	}

	@Test
	public void testDiskStoreBeanWithPropertyPlaceholderConfiguration() {
		System.out.printf("Disk Store Configuration: %1$s%n", diskStoreConfiguration);
		assertNotNull("The Disk Store was not configured and initialized!", testDataStore);
		assertEquals(getExpectedValue("allowForceCompaction"), testDataStore.getAllowForceCompaction());
		assertEquals(getExpectedValue("autoCompact"), testDataStore.getAutoCompact());
		assertEquals(getExpectedValue("compactionThreshold"), testDataStore.getCompactionThreshold());
		assertEquals(getExpectedValue("maxOplogSize"), testDataStore.getMaxOplogSize());
		assertEquals("TestDataStore", testDataStore.getName());
		assertEquals(getExpectedValue("queueSize"), testDataStore.getQueueSize());
		assertEquals(getExpectedValue("timeInterval"), testDataStore.getTimeInterval());
		assertEquals(getExpectedValue("writeBufferSize"), testDataStore.getWriteBufferSize());
	}

}
