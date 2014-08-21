/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.data.gemfire.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.gemstone.gemfire.cache.CacheListener;
import com.gemstone.gemfire.cache.CacheLoader;
import com.gemstone.gemfire.cache.CacheWriter;
import com.gemstone.gemfire.cache.CustomExpiry;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.EvictionAttributes;
import com.gemstone.gemfire.cache.ExpirationAttributes;
import com.gemstone.gemfire.cache.MembershipAttributes;
import com.gemstone.gemfire.cache.PartitionAttributes;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.RegionFactory;
import com.gemstone.gemfire.cache.RegionService;
import com.gemstone.gemfire.cache.Scope;
import com.gemstone.gemfire.cache.SubscriptionAttributes;
import com.gemstone.gemfire.cache.query.QueryService;

/**
 * @author David Turanski
 * @author John Blum
 */
@SuppressWarnings("deprecation")
public class MockRegionFactory<K,V>   {

	private static QueryService queryService = mock(QueryService.class);
	private static RegionService regionService = mock(RegionService.class);

	private com.gemstone.gemfire.cache.AttributesFactory<K,V> attributesFactory;

  	private RegionAttributes regionAttributes;

	private final StubCache cache;

	public MockRegionFactory(StubCache cache) {
		this.cache = cache;
	}

	public RegionFactory<K, V> createMockRegionFactory() {
		return createMockRegionFactory(null);
	}

	@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
	public RegionFactory<K, V> createMockRegionFactory(RegionAttributes<K,V> attributes) {
		attributesFactory = (attributes != null ? new com.gemstone.gemfire.cache.AttributesFactory<K,V>(attributes)
			: new com.gemstone.gemfire.cache.AttributesFactory<K,V>());

		// Workaround for GemFire bug
		// TODO ?!?!?!
		if (attributes !=null) {
			attributesFactory.setLockGrantor(attributes.isLockGrantor());
		}

		final RegionFactory<K, V> regionFactory = mock(RegionFactory.class);

		when(regionFactory.create(anyString())).thenAnswer(new Answer<Region>() {
			@Override public Region answer(InvocationOnMock invocation) throws Throwable {
				String name = (String) invocation.getArguments()[0];
				Region region = mockRegion(name);

				cache.allRegions().put(name, region);

				return region;
			}
		});

		when(regionFactory.createSubregion(any(Region.class), anyString())).thenAnswer(new Answer<Region>() {
			@Override public Region answer(InvocationOnMock invocation) throws Throwable {
				Region parent = (Region) invocation.getArguments()[0];
				String name = (String) invocation.getArguments()[1];
				String parentRegionName = null;

				for (String key: cache.allRegions().keySet()) {
					if (cache.allRegions().get(key).equals(parent)) {
						parentRegionName = key;
					}
				}

				assert parentRegionName != null : "The parent Region name was null!";

				String subRegionName = (parentRegionName.startsWith("/") ? parentRegionName+"/"+name
					: "/"+parentRegionName+"/"+ name);

				Region subRegion = mockRegion(subRegionName);

				cache.allRegions().put(subRegionName, subRegion);

				return subRegion;
			}
		});

		when(regionFactory.setCacheLoader(any(CacheLoader.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				CacheLoader cacheLoader = (CacheLoader) invocation.getArguments()[0];
				attributesFactory.setCacheLoader(cacheLoader);
				return regionFactory;
			}
		});

		when(regionFactory.setCacheWriter(any(CacheWriter.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				CacheWriter cacheWriter = (CacheWriter) invocation.getArguments()[0];
				attributesFactory.setCacheWriter(cacheWriter);
				return regionFactory;
			}
		});

		when(regionFactory.setCloningEnabled(anyBoolean())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean cloningEnabled = (Boolean) invocation.getArguments()[0];
				attributesFactory.setCloningEnabled(cloningEnabled);
				return regionFactory;
			}
		});

		when(regionFactory.setConcurrencyChecksEnabled(anyBoolean())).thenAnswer(new Answer<RegionFactory>() {
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean concurrencyChecksEnabled = (Boolean) invocation.getArguments()[0];
				attributesFactory.setConcurrencyChecksEnabled(concurrencyChecksEnabled);
				return regionFactory;
			}
		});

		when(regionFactory.setConcurrencyLevel(anyInt())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				int concurrencyLevel = (Integer) invocation.getArguments()[0];
				attributesFactory.setConcurrencyLevel(concurrencyLevel);
				return regionFactory;
			}
		});

		when(regionFactory.setDataPolicy(any(DataPolicy.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				DataPolicy dataPolicy = (DataPolicy) invocation.getArguments()[0];
				attributesFactory.setDataPolicy(dataPolicy);
				return regionFactory;
			}
		});

		when(regionFactory.setDiskDirs(any(File[].class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				File[] diskDirectories = (File[]) invocation.getArguments()[0];
				attributesFactory.setDiskDirs(diskDirectories);
				return regionFactory;
			}
		});

		when(regionFactory.setDiskDirsAndSizes(any(File[].class), any(int[].class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				File[] diskDirectories = (File[]) invocation.getArguments()[0];
				int[] diskSizes = (int[]) invocation.getArguments()[1];
				attributesFactory.setDiskDirsAndSizes(diskDirectories, diskSizes);
				return regionFactory;
			}
		});

		when(regionFactory.setDiskStoreName(anyString())).thenAnswer(new Answer<RegionFactory>() {
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				String diskStoreName = (String) invocation.getArguments()[0];
				attributesFactory.setDiskStoreName(diskStoreName);
				return regionFactory;
			}
		});

		when(regionFactory.setDiskWriteAttributes(any(com.gemstone.gemfire.cache.DiskWriteAttributes.class)))
			.thenAnswer(new Answer<RegionFactory>() {
				@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
					com.gemstone.gemfire.cache.DiskWriteAttributes diskWriteAttributes =
						(com.gemstone.gemfire.cache.DiskWriteAttributes) invocation.getArguments()[0];
					attributesFactory.setDiskWriteAttributes(diskWriteAttributes);
					return regionFactory;
				}
			});

		when(regionFactory.setDiskSynchronous(anyBoolean())).thenAnswer(new Answer<RegionFactory>() {
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean diskSynchronous = (Boolean) invocation.getArguments()[0];
				attributesFactory.setDiskSynchronous(diskSynchronous);
				return regionFactory;
			}
		});

		when(regionFactory.setEarlyAck(anyBoolean())).thenAnswer(new Answer<RegionFactory>() {
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean earlyAck = (Boolean) invocation.getArguments()[0];
				attributesFactory.setEarlyAck(earlyAck);
				return regionFactory;
			}
		});

		when(regionFactory.setEnableAsyncConflation(anyBoolean())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean enableAsyncConflation = (Boolean) invocation.getArguments()[0];
				attributesFactory.setEnableAsyncConflation(enableAsyncConflation);
				return regionFactory;
			}
		});

		when(regionFactory.setEnableGateway(anyBoolean())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean enableGateway = (Boolean) invocation.getArguments()[0];
				attributesFactory.setEnableGateway(enableGateway);
				return regionFactory;
			}
		});

		when(regionFactory.setEnableSubscriptionConflation(anyBoolean())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean enableSubscriptionConflation = (Boolean) invocation.getArguments()[0];
				attributesFactory.setEnableSubscriptionConflation(enableSubscriptionConflation);
				return regionFactory;
			}
		});

		when(regionFactory.setEntryIdleTimeout(any(ExpirationAttributes.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				ExpirationAttributes entryIdleTimeout = (ExpirationAttributes) invocation.getArguments()[0];
				attributesFactory.setEntryIdleTimeout(entryIdleTimeout);
				return regionFactory;
			}
		});

		when(regionFactory.setCustomEntryIdleTimeout(any(CustomExpiry.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				CustomExpiry customEntryIdleTimeout = (CustomExpiry) invocation.getArguments()[0];
				attributesFactory.setCustomEntryIdleTimeout(customEntryIdleTimeout);
				return regionFactory;
			}
		});

		when(regionFactory.setEntryTimeToLive(any(ExpirationAttributes.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				ExpirationAttributes entryTimeToLive = (ExpirationAttributes) invocation.getArguments()[0];
				attributesFactory.setEntryTimeToLive(entryTimeToLive);
				return regionFactory;
			}
		});

		when(regionFactory.setCustomEntryTimeToLive(any(CustomExpiry.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				CustomExpiry customEntryTimeToLive = (CustomExpiry) invocation.getArguments()[0];
				attributesFactory.setCustomEntryTimeToLive(customEntryTimeToLive);
				return regionFactory;
			}
		});

		when(regionFactory.setEvictionAttributes(any(EvictionAttributes.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				EvictionAttributes evictionAttributes = (EvictionAttributes) invocation.getArguments()[0];
				attributesFactory.setEvictionAttributes(evictionAttributes);
				return regionFactory;
			}
		});

		when(regionFactory.setGatewayHubId(anyString())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				String gatewayHubId = (String) invocation.getArguments()[0];
				attributesFactory.setGatewayHubId(gatewayHubId);
				return regionFactory;
			}
		});

		when(regionFactory.setIgnoreJTA(anyBoolean())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean ignoreJta = (Boolean) invocation.getArguments()[0];
				attributesFactory.setIgnoreJTA(ignoreJta);
				return regionFactory;
			}
		});

		when(regionFactory.setIndexMaintenanceSynchronous(anyBoolean())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean indexMaintenanceSynchronous = (Boolean) invocation.getArguments()[0];
				attributesFactory.setIndexMaintenanceSynchronous(indexMaintenanceSynchronous);
				return regionFactory;
			}
		});

		when(regionFactory.setInitialCapacity(anyInt())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				int initialCapacity = (Integer) invocation.getArguments()[0];
				attributesFactory.setInitialCapacity(initialCapacity);
				return regionFactory;
			}
		});

		when(regionFactory.setKeyConstraint(any(Class.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				Class keyConstraint = (Class) invocation.getArguments()[0];
				attributesFactory.setKeyConstraint(keyConstraint);
				return regionFactory;
			}
		});

		when(regionFactory.setLoadFactor(anyFloat())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				float loadFactor = (Float) invocation.getArguments()[0];
				attributesFactory.setLoadFactor(loadFactor);
				return regionFactory;
			}
		});

		when(regionFactory.setLockGrantor(anyBoolean())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean lockGrantor = (Boolean) invocation.getArguments()[0];
				attributesFactory.setLockGrantor(lockGrantor);
				return regionFactory;
			}
		});

		when(regionFactory.setMembershipAttributes(any(MembershipAttributes.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				MembershipAttributes membershipAttributes = (MembershipAttributes) invocation.getArguments()[0];
				attributesFactory.setMembershipAttributes(membershipAttributes);
				return regionFactory;
			}
		});

		when(regionFactory.setMulticastEnabled(anyBoolean())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean multicastEnabled = (Boolean) invocation.getArguments()[0];
				attributesFactory.setMulticastEnabled(multicastEnabled);
				return regionFactory;
			}
		});

		when(regionFactory.setPartitionAttributes(any(PartitionAttributes.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				PartitionAttributes partitionAttributes = (PartitionAttributes) invocation.getArguments()[0];
				attributesFactory.setPartitionAttributes(partitionAttributes);
				return regionFactory;
			}
		});

		when(regionFactory.setPoolName(anyString())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				String poolName = (String) invocation.getArguments()[0];
				attributesFactory.setPoolName(poolName);
				return regionFactory;
			}
		});

		when(regionFactory.setRegionIdleTimeout(any(ExpirationAttributes.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				ExpirationAttributes regionIdleTimeout = (ExpirationAttributes) invocation.getArguments()[0];
				attributesFactory.setRegionIdleTimeout(regionIdleTimeout);
				return regionFactory;
			}
		});

		when(regionFactory.setRegionTimeToLive(any(ExpirationAttributes.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				ExpirationAttributes regionTimeToLive = (ExpirationAttributes) invocation.getArguments()[0];
				attributesFactory.setRegionTimeToLive(regionTimeToLive);
				return regionFactory;
			}
		});

		when(regionFactory.setScope(any(Scope.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				Scope scope = (Scope) invocation.getArguments()[0];
				attributesFactory.setScope(scope);
				return regionFactory;
			}
		});

		when(regionFactory.setStatisticsEnabled(anyBoolean())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				boolean statisticsEnabled = (Boolean) invocation.getArguments()[0];
				attributesFactory.setStatisticsEnabled(statisticsEnabled);
				return regionFactory;
			}
		});

		when(regionFactory.setSubscriptionAttributes(any(SubscriptionAttributes.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				SubscriptionAttributes subscriptionAttributes = (SubscriptionAttributes) invocation.getArguments()[0];
				attributesFactory.setSubscriptionAttributes(subscriptionAttributes);
				return regionFactory;
			}
		});

		when(regionFactory.setValueConstraint(any(Class.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				Class valueConstraint = (Class) invocation.getArguments()[0];
				attributesFactory.setValueConstraint(valueConstraint);
				return regionFactory;
			}
		});

		when(regionFactory.addAsyncEventQueueId(anyString())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				String asyncEventQueueId = (String) invocation.getArguments()[0];
				attributesFactory.addAsyncEventQueueId(asyncEventQueueId);
				return regionFactory;
			}
		});

		when(regionFactory.addCacheListener(any(CacheListener.class))).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				CacheListener cacheListener = (CacheListener) invocation.getArguments()[0];
				attributesFactory.addCacheListener(cacheListener);
				return regionFactory;
			}
		});

		when(regionFactory.addGatewaySenderId(anyString())).thenAnswer(new Answer<RegionFactory>(){
			@Override public RegionFactory answer(InvocationOnMock invocation) throws Throwable {
				String gatewaySenderId = (String) invocation.getArguments()[0];
				attributesFactory.addGatewaySenderId(gatewaySenderId);
				return regionFactory;
			}
		});

		return regionFactory;
	}

	@SuppressWarnings("rawtypes")
	RegionFactory createRegionFactory() {
		return createMockRegionFactory();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Region mockRegion(String name) {
		regionAttributes = attributesFactory.create();

		RegionService mockRegionService = mockRegionService();
		Region region = mock(Region.class);

		when(region.getAttributes()).thenAnswer(new Answer<RegionAttributes>() {
			@Override public RegionAttributes answer(InvocationOnMock invocation) throws Throwable {
				return regionAttributes;
			}
		});

		String fullPath = (name.startsWith(Region.SEPARATOR) ? name : Region.SEPARATOR + name);

		when(region.getFullPath()).thenReturn(fullPath);
		when(region.getName()).thenReturn(name);
    	when(region.getRegionService()).thenReturn(mockRegionService);

    	when(region.getSubregion(anyString())).thenAnswer(new Answer<Region>() {
			@Override
			public Region answer(InvocationOnMock invocation) throws Throwable {
				Region parent = (Region) invocation.getMock();

				String parentRegionName = parent.getName();
				String subRegionName = (String) invocation.getArguments()[0];
				String subRegionPath = (parentRegionName.startsWith("/") ? parentRegionName+"/"+subRegionName
					: "/"+parentRegionName+"/"+subRegionName);

				return cache.getRegion(subRegionPath);
			}
		});

		when(region.createSubregion(anyString(), any(RegionAttributes.class))).thenAnswer(new Answer<Region>() {
			@Override
			public Region answer(InvocationOnMock invocation) throws Throwable {
				String name = (String) invocation.getArguments()[0];
				RegionAttributes attributes = (RegionAttributes) invocation.getArguments()[1];

				Region parent = (Region) invocation.getMock();
				String parentName = parent.getName();
				String regionName = parentName.startsWith("/") ? parentName+"/"+name : "/"+parentName+"/"+ name;

				Region subRegion = new MockRegionFactory(cache).createMockRegionFactory(attributes).create(regionName);
				when(subRegion.getFullPath()).thenReturn(regionName);

				cache.allRegions().put(regionName, subRegion);

				return subRegion;
			}
		});

		return region;
	}

	public static RegionService mockRegionService() {
		when(regionService.getQueryService()).thenReturn(mockQueryService());
		return regionService;
	}

	public static QueryService mockQueryService() {
		return queryService;
	}


}
