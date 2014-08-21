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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

import com.gemstone.gemfire.GemFireCheckedException;
import com.gemstone.gemfire.GemFireException;
import com.gemstone.gemfire.cache.Region;

/**
 * Base class for GemfireTemplate and GemfireInterceptor, defining common properties such as {@link Region}. 
 *
 * Not intended to be used directly.
 * 
 * @author Costin Leau
 * @author John Blum
 * @see org.springframework.beans.factory.InitializingBean
 * @see com.gemstone.gemfire.cache.Region
 */
public class GemfireAccessor implements InitializingBean {

	protected final Log log = LogFactory.getLog(getClass());

	private Region region;

	/**
	 * Returns the template GemFire Cache Region.
	 *
	 * @param <K> the Region key class type.
	 * @param <V> the Region value class type.
	 * @return the GemFire Cache Region.
	 * @see com.gemstone.gemfire.cache.Region
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Region<K, V> getRegion() {
		return region;
	}

	/**
	 * Sets the template GemFire Cache Region.
	 *
	 * @param region the GemFire Cache Region used by this template.
	 * @see com.gemstone.gemfire.cache.Region
	 */
	public void setRegion(Region<?, ?> region) {
		this.region = region;
	}

	public void afterPropertiesSet() {
		Assert.notNull(getRegion(), "The GemFire Cache Region is required.");
	}

	/**
	 * Converts the given {@link GemFireCheckedException} to an appropriate exception from the
	 * <code>org.springframework.dao</code> hierarchy.
	 * May be overridden in subclasses.
	 * @param ex GemFireCheckedException that occurred
	 * @return the corresponding DataAccessException instance
	 */
	public DataAccessException convertGemFireAccessException(GemFireCheckedException ex) {
		return GemfireCacheUtils.convertGemfireAccessException(ex);
	}

	/**
	 * Converts the given {@link GemFireException} to an appropriate exception from the
	 * <code>org.springframework.dao</code> hierarchy.
	 * May be overridden in subclasses.
	 * @param ex GemFireException that occurred
	 * @return the corresponding DataAccessException instance
	 */
	public DataAccessException convertGemFireAccessException(GemFireException ex) {
		return GemfireCacheUtils.convertGemfireAccessException(ex);
	}

	/**
	 * Converts the given GemFire exception to an appropriate exception from the
	 * <code>org.springframework.dao</code> hierarchy. Note that this particular implementation
	 * is called only for GemFire querying exception that do <b>NOT</b> extend from GemFire exception.
	 * May be overridden in subclasses.
	 * 
	 * @param ex GemFireException that occurred
	 * @return the corresponding DataAccessException instance
	 */
	public DataAccessException convertGemFireQueryException(RuntimeException ex) {
		return GemfireCacheUtils.convertQueryExceptions(ex);
	}

}
