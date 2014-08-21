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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.data.gemfire.SimplePartitionResolver;
import org.springframework.data.gemfire.TestUtils;
import org.springframework.data.gemfire.test.GemfireTestApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ObjectUtils;

import com.gemstone.gemfire.cache.CacheListener;
import com.gemstone.gemfire.cache.FixedPartitionAttributes;
import com.gemstone.gemfire.cache.PartitionAttributes;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.partition.PartitionListener;

/**
 * @author Costin Leau
 * @author David Turanski
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "partitioned-ns.xml", initializers = GemfireTestApplicationContextInitializer.class)
public class PartitionedRegionNamespaceTest {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testBasicPartition() throws Exception {
		assertTrue(context.containsBean("simple"));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testPartitionOptions() throws Exception {
		assertTrue(context.containsBean("options"));
		RegionFactoryBean fb = context.getBean("&options", RegionFactoryBean.class);
		assertTrue(fb instanceof PartitionedRegionFactoryBean);
		assertEquals(null, TestUtils.readField("scope", fb));
		assertEquals("redundant", TestUtils.readField("name", fb));

		RegionAttributes attrs = TestUtils.readField("attributes", fb);
		assertTrue(attrs.getStatisticsEnabled());

		PartitionAttributes pAttr = attrs.getPartitionAttributes();

		assertEquals(1, pAttr.getRedundantCopies());
		assertEquals(4, pAttr.getTotalNumBuckets());
		assertSame(SimplePartitionResolver.class, pAttr.getPartitionResolver().getClass());
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testComplexPartition() throws Exception {
		assertTrue(context.containsBean("complex"));
		RegionFactoryBean fb = context.getBean("&complex", RegionFactoryBean.class);
		CacheListener[] listeners = TestUtils.readField("cacheListeners", fb);
		assertFalse(ObjectUtils.isEmpty(listeners));
		assertEquals(2, listeners.length);
		assertSame(listeners[0], context.getBean("c-listener"));

		assertSame(context.getBean("c-loader"), TestUtils.readField("cacheLoader", fb));
		assertSame(context.getBean("c-writer"), TestUtils.readField("cacheWriter", fb));

		RegionAttributes attrs = TestUtils.readField("attributes", fb);
		PartitionAttributes pAttr = attrs.getPartitionAttributes();
		assertEquals(20, pAttr.getLocalMaxMemory());

		assertNotNull(pAttr.getPartitionListeners());
		assertEquals(1, pAttr.getPartitionListeners().length);
		assertTrue(pAttr.getPartitionListeners()[0] instanceof TestPartitionListener);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testFixedPartition() throws Exception {
		RegionFactoryBean fb = context.getBean("&fixed", RegionFactoryBean.class);
		RegionAttributes attrs = TestUtils.readField("attributes", fb);
		PartitionAttributes pAttr = attrs.getPartitionAttributes();
		assertNotNull(pAttr.getFixedPartitionAttributes());
		assertEquals(3, pAttr.getFixedPartitionAttributes().size());

		FixedPartitionAttributes fpa = (FixedPartitionAttributes) pAttr.getFixedPartitionAttributes().get(0);
		assertEquals(3, fpa.getNumBuckets());
		assertTrue(fpa.isPrimary());

	}

	public static class TestPartitionListener implements PartitionListener {

		@Override
		public void afterBucketCreated(int arg0, Iterable<?> arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void afterBucketRemoved(int arg0, Iterable<?> arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void afterPrimary(int arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void afterRegionCreate(Region<?, ?> arg0) {
			// TODO Auto-generated method stub

		}

	}
}