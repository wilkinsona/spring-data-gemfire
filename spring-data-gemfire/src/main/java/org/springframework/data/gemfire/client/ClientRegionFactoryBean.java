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

package org.springframework.data.gemfire.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.DataPolicyConverter;
import org.springframework.data.gemfire.RegionLookupFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.gemstone.gemfire.cache.CacheClosedException;
import com.gemstone.gemfire.cache.CacheListener;
import com.gemstone.gemfire.cache.CacheLoader;
import com.gemstone.gemfire.cache.CacheWriter;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.GemFireCache;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientRegionFactory;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;
import com.gemstone.gemfire.cache.client.Pool;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;

/**
 * Client extension for GemFire Regions.
 *
 * @author Costin Leau
 * @author David Turanski
 * @author John Blum
 */
@SuppressWarnings("unused")
public class ClientRegionFactoryBean<K, V> extends RegionLookupFactoryBean<K, V> implements BeanFactoryAware,
		DisposableBean {

	private static final Log log = LogFactory.getLog(ClientRegionFactoryBean.class);

	private boolean close = false;
	private boolean destroy = false;

	private BeanFactory beanFactory;

	private Boolean persistent;

	private CacheListener<K, V>[] cacheListeners;

	private CacheLoader<K, V> cacheLoader;

	private CacheWriter<K, V> cacheWriter;

	private ClientRegionShortcut shortcut = null;

	private DataPolicy dataPolicy;

	private Interest<K>[] interests;

	private RegionAttributes<K, V> attributes;

	private Resource snapshot;

	private String diskStoreName;
	private String poolName;

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		postProcess(getRegion());
	}

	@Override
	@SuppressWarnings("deprecation")
	protected Region<K, V> lookupFallback(GemFireCache cache, String regionName) throws Exception {
		Assert.isTrue(cache instanceof ClientCache, String.format("Unable to create regions from %1$s", cache));

		// TODO reference to an internal GemFire class!
		if (cache instanceof GemFireCacheImpl) {
			Assert.isTrue(((GemFireCacheImpl) cache).isClient(), "A client-cache instance is required.");
		}

		ClientCache clientCache = (ClientCache) cache;

		ClientRegionFactory<K, V> factory = clientCache.createClientRegionFactory(resolveClientRegionShortcut());

		// map region attributes onto the client region factory
		if (attributes != null) {
			factory.setCloningEnabled(attributes.getCloningEnabled());
			factory.setConcurrencyLevel(attributes.getConcurrencyLevel());
			factory.setCustomEntryIdleTimeout(attributes.getCustomEntryIdleTimeout());
			factory.setCustomEntryTimeToLive(attributes.getCustomEntryTimeToLive());
			factory.setDiskStoreName(attributes.getDiskStoreName());
			factory.setDiskSynchronous(attributes.isDiskSynchronous());
			factory.setEntryIdleTimeout(attributes.getEntryIdleTimeout());
			factory.setEntryTimeToLive(attributes.getEntryTimeToLive());
			factory.setEvictionAttributes(attributes.getEvictionAttributes());
			factory.setInitialCapacity(attributes.getInitialCapacity());
			factory.setKeyConstraint(attributes.getKeyConstraint());
			factory.setLoadFactor(attributes.getLoadFactor());
			factory.setPoolName(attributes.getPoolName());
			factory.setRegionIdleTimeout(attributes.getRegionIdleTimeout());
			factory.setRegionTimeToLive(attributes.getRegionTimeToLive());
			factory.setStatisticsEnabled(attributes.getStatisticsEnabled());
			factory.setValueConstraint(attributes.getValueConstraint());
		}

		addCacheListeners(factory);

		if (StringUtils.hasText(poolName)) {
			// try to eagerly initialize the pool name, if defined as a bean
			if (beanFactory.isTypeMatch(poolName, Pool.class)) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Found bean definition for pool '%1$s'. Eagerly initializing...", poolName));
				}
				beanFactory.getBean(poolName, Pool.class);
			}
			factory.setPoolName(poolName);
		}
		else {
			Pool pool = beanFactory.getBean(Pool.class);
			factory.setPoolName(pool.getName());
		}

		if (diskStoreName != null) {
			factory.setDiskStoreName(diskStoreName);
		}

		Region<K, V> clientRegion = (getParent() != null ? factory.createSubregion(getParent(), regionName)
			: factory.create(regionName));

		if (log.isInfoEnabled()) {
			if (getParent() != null) {
				log.info(String.format("Created new Client Cache sub-Region [%1$s] under parent Region [%2$s].",
					regionName, getParent().getName()));
			}
			else {
				log.info(String.format("Created new Client Cache Region [%1$s].", regionName));
			}
		}

		if (snapshot != null) {
			clientRegion.loadSnapshot(snapshot.getInputStream());
		}

		return clientRegion;
	}

	protected ClientRegionShortcut resolveClientRegionShortcut() {
		ClientRegionShortcut resolvedShortcut = this.shortcut;

		if (resolvedShortcut == null) {
			if (this.dataPolicy != null) {
				assertDataPolicyAndPersistentAttributeAreCompatible(this.dataPolicy);

				if (DataPolicy.EMPTY.equals(this.dataPolicy)) {
					resolvedShortcut = ClientRegionShortcut.PROXY;
				}
				else if (DataPolicy.NORMAL.equals(this.dataPolicy)) {
					resolvedShortcut = ClientRegionShortcut.CACHING_PROXY;
				}
				else if (DataPolicy.PERSISTENT_REPLICATE.equals(this.dataPolicy)) {
					resolvedShortcut = ClientRegionShortcut.LOCAL_PERSISTENT;
				}
				else {
					// NOTE the Data Policy validation is based on the ClientRegionShortcut initialization logic
					// in com.gemstone.gemfire.internal.cache.GemFireCacheImpl.initializeClientRegionShortcuts
					throw new IllegalArgumentException(String.format(
						"Data Policy '%1$s' is invalid for Client Regions.", this.dataPolicy));
				}
			}
			else {
				resolvedShortcut = (isPersistent() ? ClientRegionShortcut.LOCAL_PERSISTENT
					: ClientRegionShortcut.LOCAL);
			}

		}

		// NOTE the ClientRegionShortcut and Persistent attribute will be compatible if the shortcut was derived from
		// the Data Policy.
		assertClientRegionShortcutAndPersistentAttributeAreCompatible(resolvedShortcut);

		return resolvedShortcut;
	}

	/**
	 * Validates that the settings for ClientRegionShortcut and the 'persistent' attribute in &lt;gfe:*-region&gt; elements
	 * are compatible.
	 *
	 * @param resolvedShortcut the GemFire ClientRegionShortcut resolved form the Spring GemFire XML namespace
	 * configuration meta-data.
	 * @see #isPersistent()
	 * @see #isNotPersistent()
	 * @see com.gemstone.gemfire.cache.client.ClientRegionShortcut
	 */
	protected void assertClientRegionShortcutAndPersistentAttributeAreCompatible(final ClientRegionShortcut resolvedShortcut) {
		final boolean persistentNotSpecified = (this.persistent == null);

		if (ClientRegionShortcut.LOCAL_PERSISTENT.equals(resolvedShortcut)
				|| ClientRegionShortcut.LOCAL_PERSISTENT_OVERFLOW.equals(resolvedShortcut)) {
			Assert.isTrue(persistentNotSpecified || isPersistent(), String.format(
				"Client Region Shortcut '%1$s' is invalid when persistent is false.", resolvedShortcut));
		}
		else {
			Assert.isTrue(persistentNotSpecified || isNotPersistent(), String.format(
				"Client Region Shortcut '%1$s' is invalid when persistent is true.", resolvedShortcut));
		}
	}

	/**
	 * Validates that the settings for Data Policy and the 'persistent' attribute in &lt;gfe:*-region&gt; elements
	 * are compatible.
	 *
	 * @param resolvedDataPolicy the GemFire Data Policy resolved form the Spring GemFire XML namespace configuration
	 * meta-data.
	 * @see #isPersistent()
	 * @see #isNotPersistent()
	 * @see com.gemstone.gemfire.cache.DataPolicy
	 */
	protected void assertDataPolicyAndPersistentAttributeAreCompatible(final DataPolicy resolvedDataPolicy) {
		if (resolvedDataPolicy.withPersistence()) {
			Assert.isTrue(isPersistentUnspecified() || isPersistent(), String.format(
				"Data Policy '%1$s' is invalid when persistent is false.", resolvedDataPolicy));
		}
		else {
			// NOTE otherwise, the Data Policy is without persistence, so...
			Assert.isTrue(isPersistentUnspecified() || isNotPersistent(), String.format(
				"Data Policy '%1$s' is invalid when persistent is true.", resolvedDataPolicy));
		}
	}

	private void addCacheListeners(ClientRegionFactory<K, V> factory) {
		if (attributes != null) {
			CacheListener<K, V>[] cacheListeners = attributes.getCacheListeners();

			if (!ObjectUtils.isEmpty(cacheListeners)) {
				for (CacheListener<K, V> cacheListener : cacheListeners) {
					factory.addCacheListener(cacheListener);
				}
			}
		}

		if (!ObjectUtils.isEmpty(cacheListeners)) {
			for (CacheListener<K, V> cacheListener : cacheListeners) {
				factory.addCacheListener(cacheListener);
			}
		}
	}

	protected void postProcess(final Region<K, V> region) {
		registerInterests(region);
		setCacheLoader(region);
		setCacheWriter(region);
	}

	private void registerInterests(final Region<K, V> region) {
		if (!ObjectUtils.isEmpty(interests)) {
			for (Interest<K> interest : interests) {
				if (interest instanceof RegexInterest) {
					region.registerInterestRegex((String) interest.getKey(), interest.getPolicy(),
						interest.isDurable(), interest.isReceiveValues());
				}
				else {
					region.registerInterest(interest.getKey(), interest.getPolicy(), interest.isDurable(),
						interest.isReceiveValues());
				}
			}
		}
	}

	private void setCacheLoader(final Region<K, V> region) {
		if (cacheLoader != null) {
			region.getAttributesMutator().setCacheLoader(this.cacheLoader);
		}
	}

	private void setCacheWriter(final Region<K, V> region) {
		if (cacheWriter != null) {
			region.getAttributesMutator().setCacheWriter(this.cacheWriter);
		}
	}

	@Override
	public void destroy() throws Exception {
		Region<K, V> region = getObject();

		try {
			if (region != null && !ObjectUtils.isEmpty(interests)) {
				for (Interest<K> interest : interests) {
					if (interest instanceof RegexInterest) {
						region.unregisterInterestRegex((String) interest.getKey());
					}
					else {
						region.unregisterInterest(interest.getKey());
					}
				}
			}
		}
		// NOTE AdminRegion, LocalDataSet, Proxy Region and RegionCreation all throw UnsupportedOperationException;
		// however, should not really happen since Interests are validated at start/registration
		catch (UnsupportedOperationException ex) {
			log.warn("Cannot unregister cache interests", ex);
		}

		if (region != null) {
			if (close) {
				if (!region.getRegionService().isClosed()) {
					try {
						region.close();
					}
					catch (CacheClosedException ignore) {
					}
				}
			}
			// TODO I think Region.close and Region.destroyRegion are mutually exclusive; thus, 1 operation (e.g. close)
			// does not cancel the other (i.e. destroy). This should just be if, not else if.
			else if (destroy) {
				region.destroyRegion();
			}
		}
	}

	/**
	 * Sets the region attributes used for the region used by this factory.
	 * Allows maximum control in specifying the region settings. Used only when
	 * a new region is created. Note that using this method allows for advanced
	 * customization of the region - while it provides a lot of flexibility,
	 * note that it's quite easy to create misconfigured regions (especially in
	 * a client/server scenario).
	 *
	 * @param attributes the attributes to set on a newly created region
	 */
	public void setAttributes(RegionAttributes<K, V> attributes) {
		this.attributes = attributes;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Set the interests for this client region. Both key and regex interest are
	 * supported.
	 * 
	 * @param interests the interests to set
	 */
	public void setInterests(Interest<K>[] interests) {
		this.interests = interests;
	}

	/**
	 * @return the interests
	 */
	Interest<K>[] getInterests() {
		return interests;
	}

	/**
	 * Sets the pool used by this client.
	 *
	 * @param pool the GemFire client pool.
	 */
	public void setPool(Pool pool) {
		Assert.notNull(pool, "pool cannot be null");
		setPoolName(pool.getName());
	}

	/**
	 * Sets the pool name used by this client.
	 *
	 * @param poolName a String specify the name of the GemFire client pool.
	 */
	public void setPoolName(String poolName) {
		Assert.hasText(poolName, "pool name is required");
		this.poolName = poolName;
	}

	final boolean isClose() {
		return close;
	}

	/**
	 * Indicates whether the region referred by this factory bean, will be
	 * closed on shutdown (default true). Note: destroy and close are mutually
	 * exclusive. Enabling one will automatically disable the other.
	 *
	 * @param close whether to close or not the region
	 * @see #setDestroy(boolean)
	 */
	public void setClose(boolean close) {
		this.close = close;
		this.destroy = (this.destroy && !close); // retain previous value iff close is false.
	}

	final boolean isDestroy() {
		return destroy;
	}

	/**
	 * Indicates whether the region referred by this factory bean will be
	 * destroyed on shutdown (default false). Note: destroy and close are
	 * mutually exclusive. Enabling one will automatically disable the other.
	 *
	 * @param destroy whether or not to destroy the region
	 * @see #setClose(boolean)
	 */
	public void setDestroy(boolean destroy) {
		this.destroy = destroy;
		this.close = (this.close && !destroy); // retain previous value iff destroy is false;
	}

	/**
	 * Sets the cache listeners used for the region used by this factory. Used
	 * only when a new region is created.Overrides the settings specified
	 * through {@link #setAttributes(RegionAttributes)}.
	 *
	 * @param cacheListeners the cacheListeners to set on a newly created region
	 */
	public void setCacheListeners(CacheListener<K, V>[] cacheListeners) {
		this.cacheListeners = cacheListeners;
	}

	/**
	 * Sets the CacheLoader used to load data local to the client's Region on cache misses.
	 *
	 * @param cacheLoader a GemFire CacheLoader used to load data into the client Region.
	 * @see com.gemstone.gemfire.cache.CacheLoader
	 */
	public void setCacheLoader(CacheLoader<K, V> cacheLoader) {
		this.cacheLoader = cacheLoader;
	}

	/**
	 * Sets the CacheWriter used to perform a synchronous write-behind when data is put into the client's Region.
	 *
	 * @param cacheWriter the GemFire CacheWriter used to perform synchronous write-behinds on put ops.
	 * @see com.gemstone.gemfire.cache.CacheWriter
	 */
	public void setCacheWriter(CacheWriter<K, V> cacheWriter) {
		this.cacheWriter = cacheWriter;
	}

	/**
	 * Sets the Data Policy. Used only when a new Region is created.
	 *
	 * @param dataPolicy the client Region's Data Policy.
	 * @see com.gemstone.gemfire.cache.DataPolicy
	 */
	public void setDataPolicy(DataPolicy dataPolicy) {
		this.dataPolicy = dataPolicy;
	}

	/**
	 * Sets the name of disk store to use for overflow and persistence
	 *
	 * @param diskStoreName a String specifying the 'name' of the client Region Disk Store.
	 */
	public void setDiskStoreName(String diskStoreName) {
		this.diskStoreName = diskStoreName;
	}

	/**
	 * An alternate way to set the Data Policy, using the String name of the enumerated value.
	 *
	 * @param dataPolicyName the enumerated value String name of the Data Policy.
	 * @see com.gemstone.gemfire.cache.DataPolicy
	 * @see #setDataPolicy(com.gemstone.gemfire.cache.DataPolicy)
	 * @deprecated use setDataPolicy(:DataPolicy) instead.
	 */
	public void setDataPolicyName(String dataPolicyName) {
		final DataPolicy resolvedDataPolicy = new DataPolicyConverter().convert(dataPolicyName);
		Assert.notNull(resolvedDataPolicy, String.format("Data Policy '%1$s' is invalid.", dataPolicyName));
		setDataPolicy(resolvedDataPolicy);
	}

	protected boolean isPersistentUnspecified() {
		return (persistent == null);
	}

	protected boolean isPersistent() {
		return Boolean.TRUE.equals(persistent);
	}

	protected boolean isNotPersistent() {
		return Boolean.FALSE.equals(persistent);
	}

	public void setPersistent(final boolean persistent) {
		this.persistent = persistent;
	}

	/**
	 * Initializes the client using a GemFire {@link ClientRegionShortcut}. The
	 * recommended way for creating clients since it covers all the major
	 * scenarios with minimal configuration.
	 *
	 * @param shortcut the ClientRegionShortcut to use.
	 */
	public void setShortcut(ClientRegionShortcut shortcut) {
		this.shortcut = shortcut;
	}

	/**
	 * Sets the snapshots used for loading a newly <i>created</i> region. That
	 * is, the snapshot will be used <i>only</i> when a new region is created -
	 * if the region already exists, no loading will be performed.
	 *
	 * @param snapshot the snapshot to set
	 * @see #setName(String)
	 */
	public void setSnapshot(Resource snapshot) {
		this.snapshot = snapshot;
	}

}
