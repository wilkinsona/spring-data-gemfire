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

import java.lang.reflect.Field;

/**
 * @author Costin Leau
 */
public abstract class TestUtils {

	@SuppressWarnings("unchecked")
	public static <T> T readField(String name, Object target) throws Exception {
		Field field = null;
		Class<?> clazz = target.getClass();
		do {
			try {
				field = clazz.getDeclaredField(name);
			} catch (Exception ex) {
			}

			clazz = clazz.getSuperclass();
		} while (field == null && !clazz.equals(Object.class));

		if (field == null)
			throw new IllegalArgumentException("Cannot find field '" + name + "' in the class hierarchy of "
					+ target.getClass());
		field.setAccessible(true);
		return (T) field.get(target);
	}

	public static void cleanBeanFactoryStaticReference() {
		try {
			Field field = GemfireBeanFactoryLocator.class.getDeclaredField("canUseDefaultBeanFactory");
			field.setAccessible(true);
			field.set(null, true);

			field = GemfireBeanFactoryLocator.class.getDeclaredField("defaultFactory");
			field.setAccessible(true);
			field.set(null, null);

		} catch (Exception ex) {
		}
	}
}
