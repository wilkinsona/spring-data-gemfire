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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.gemfire.cache.Cache;


/**
 * Integration test for declarable support (and GEF bean factory locator). 
 * 
 * @author Costin Leau
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "cache-with-declarable-ctx.xml" })
public class DeclarableSupportTest {

	@Autowired
	private BeanFactory ctx;

	@Test
	public void testUserObject() throws Exception {
		ctx.getBean(Cache.class);
		assertNotNull(UserObject.THIS);
		UserObject obj = UserObject.THIS;
		assertSame(ctx, obj.getBeanFactory());
		assertSame(ctx.getBean("bean"), obj.getProp2());
		assertEquals("Enescu", obj.getProp1());
	}
}
