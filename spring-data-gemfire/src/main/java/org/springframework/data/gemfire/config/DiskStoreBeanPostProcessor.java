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

package org.springframework.data.gemfire.config;

import java.io.File;
import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.gemfire.DiskStoreFactoryBean.DiskDir;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * The DiskStoreBeanPostProcessor class post processes any GemFire Disk Store DiskDir Spring beans defined
 * in the application context to ensure that the Disk Store directory location (disk-dir) actually exists
 * before creating the Disk Store.
 *
 * @author John Blum
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.data.gemfire.DiskStoreFactoryBean.DiskDir
 * @since 1.5.0
 */
@SuppressWarnings("unused")
public class DiskStoreBeanPostProcessor implements BeanPostProcessor {

	protected final Log log = LogFactory.getLog(getClass());

	@Override
	public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Post Processing Bean (%1$s) of Type (%2$s) with Name (%3$s)...%n", bean,
				ObjectUtils.nullSafeClassName(bean), beanName));
		}

		if (bean instanceof DiskDir) {
			createIfNotExists((DiskDir) bean);
		}

		return bean;
	}

	private void createIfNotExists(final DiskDir diskDirectory) {
		String location = readField(diskDirectory, "location");

		File diskDirectoryFile = new File(location);

		Assert.isTrue(diskDirectoryFile.isDirectory() || diskDirectoryFile.mkdirs(),
			String.format("Failed to create Disk Directory (%1$s)%n!", location));

		if (log.isInfoEnabled()) {
			log.info(String.format("The Disk Directory either exists or was created @ Location (%1$s).%n", location));
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T readField(final Object obj, final String fieldName) {
		try {
			Class type = obj.getClass();
			Field field;

			do {
				field = type.getDeclaredField(fieldName);
				type = type.getSuperclass(); // traverse up the object class hierarchy
			}
			while (field == null && !Object.class.equals(type));

			if (field == null) {
				throw new NoSuchFieldException(String.format("Field (%1$s) does not exist on Object of type (%2$s)!",
					fieldName, ObjectUtils.nullSafeClassName(obj)));
			}

			field.setAccessible(true);

			return (T) field.get(obj);
		}
		catch (Exception e) {
			throw new RuntimeException(String.format("Failed to read field (%1$s) on Object of type (%2$s)!",
				fieldName, ObjectUtils.nullSafeClassName(obj)), e);
		}
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		return bean;
	}

}
