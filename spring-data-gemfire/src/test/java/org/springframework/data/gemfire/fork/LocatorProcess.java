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

package org.springframework.data.gemfire.fork;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.springframework.data.gemfire.process.support.ProcessUtils;
import org.springframework.data.gemfire.test.support.FileSystemUtils;
import org.springframework.data.gemfire.test.support.ThreadUtils;

import com.gemstone.gemfire.distributed.LocatorLauncher;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;
import com.gemstone.gemfire.distributed.internal.InternalLocator;
import com.gemstone.gemfire.distributed.internal.SharedConfiguration;

/**
 * The LocatorProcess class is a main Java class that is used fork and launch a GemFire Locator process using the
 * LocatorLauncher class.
 *
 * @author John Blum
 * @see com.gemstone.gemfire.distributed.LocatorLauncher
 * @since 1.5.0
 */
public class LocatorProcess {

	public static final int DEFAULT_LOCATOR_PORT = 20668;

	public static final String DEFAULT_GEMFIRE_MEMBER_NAME = "SpringDataGemFire-Locator";
	public static final String DEFAULT_HOSTNAME_FOR_CLIENTS = "localhost";
	public static final String DEFAULT_HTTP_SERVICE_PORT = "0";
	public static final String DEFAULT_LOG_LEVEL = "config";

	public static void main(final String... args) throws IOException {
		//runLocator();
		runInternalLocator();

		registerShutdownHook();

		waitForLocatorStart(TimeUnit.SECONDS.toMillis(30));

		ProcessUtils.writePid(new File(FileSystemUtils.WORKING_DIRECTORY, getLocatorProcessControlFilename()),
			ProcessUtils.currentPid());

		ProcessUtils.waitForStopSignal();
	}

	public static String getLocatorProcessControlFilename() {
		return LocatorProcess.class.getSimpleName().toLowerCase().concat(".pid");
	}

	@SuppressWarnings("unused")
	private static InternalLocator runInternalLocator() throws IOException {
		String hostnameForClients = System.getProperty("spring.gemfire.hostname-for-clients",
			DEFAULT_HOSTNAME_FOR_CLIENTS);

		int locatorPort = Integer.getInteger("spring.gemfire.locator-port", DEFAULT_LOCATOR_PORT);

		boolean loadClusterConfigurationFromDirectory = Boolean.getBoolean("spring.gemfire.load-cluster-configuration");

		Properties distributedSystemProperties = new Properties();

		distributedSystemProperties.setProperty(DistributionConfig.ENABLE_CLUSTER_CONFIGURATION_NAME,
			String.valueOf(Boolean.getBoolean("spring.gemfire.enable-cluster-configuration")));
		distributedSystemProperties.setProperty(DistributionConfig.HTTP_SERVICE_PORT_NAME,
			System.getProperty("spring.gemfire.http-service-port", DEFAULT_HTTP_SERVICE_PORT));
		distributedSystemProperties.setProperty(DistributionConfig.JMX_MANAGER_NAME,
			System.getProperty("spring.gemfire.jmx-manager", Boolean.TRUE.toString()));
		distributedSystemProperties.setProperty(DistributionConfig.JMX_MANAGER_START_NAME,
			System.getProperty("spring.gemfire.jmx-manager-start", Boolean.FALSE.toString()));
		distributedSystemProperties.setProperty(DistributionConfig.LOAD_CLUSTER_CONFIG_FROM_DIR_NAME,
			String.valueOf(loadClusterConfigurationFromDirectory));
		distributedSystemProperties.setProperty(DistributionConfig.LOG_LEVEL_NAME,
			System.getProperty("spring.gemfire.log-level", DEFAULT_LOG_LEVEL));

		return InternalLocator.startLocator(locatorPort, null, null, null, null, null, distributedSystemProperties,
			true, true, hostnameForClients, loadClusterConfigurationFromDirectory);
	}

	@SuppressWarnings("unused")
	private static LocatorLauncher runLocator() {
		LocatorLauncher locatorLauncher = buildLocatorLauncher();

		// start the GemFire Locator process...
		locatorLauncher.start();

		return locatorLauncher;
	}

	private static LocatorLauncher buildLocatorLauncher() {
		return new LocatorLauncher.Builder()
			.setMemberName(DEFAULT_GEMFIRE_MEMBER_NAME)
			.setHostnameForClients(System.getProperty("spring.gemfire.hostname-for-clients",
				DEFAULT_HOSTNAME_FOR_CLIENTS))
			.setPort(Integer.getInteger("spring.gemfire.locator-port", DEFAULT_LOCATOR_PORT))
			.setRedirectOutput(false)
			.set(DistributionConfig.ENABLE_CLUSTER_CONFIGURATION_NAME, String.valueOf(Boolean.getBoolean(
				"spring.gemfire.enable-cluster-configuration")))
			.set(DistributionConfig.HTTP_SERVICE_PORT_NAME, System.getProperty("spring.gemfire.http-service-port",
				DEFAULT_HTTP_SERVICE_PORT))
			.set(DistributionConfig.JMX_MANAGER_NAME, String.valueOf(Boolean.TRUE))
			.set(DistributionConfig.JMX_MANAGER_START_NAME, String.valueOf(Boolean.FALSE))
			.set(DistributionConfig.LOAD_CLUSTER_CONFIG_FROM_DIR_NAME, String.valueOf(Boolean.getBoolean(
				"spring.gemfire.load-cluster-configuration")))
			.set(DistributionConfig.LOG_LEVEL_NAME, System.getProperty("spring.gemfire.log-level", DEFAULT_LOG_LEVEL))
			.build();
	}

	private static boolean isClusterConfigurationEnabled(final InternalLocator locator) {
		return (locator != null && Boolean.valueOf(locator.getDistributedSystem().getProperties().getProperty(
			DistributionConfig.ENABLE_CLUSTER_CONFIGURATION_NAME)));
	}

	private static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override public void run() {
				stopLocator(stopSharedConfigurationService(InternalLocator.getLocator()));
			}

			private void stopLocator(final InternalLocator locator) {
				if (locator != null) {
					locator.stop();
				}
			}

			private InternalLocator stopSharedConfigurationService(final InternalLocator locator) {
				if (isClusterConfigurationEnabled(locator)) {
					SharedConfiguration sharedConfiguration = locator.getSharedConfiguration();

					if (sharedConfiguration != null) {
						if (Boolean.valueOf(System.getProperty("spring.gemfire.fork.clean", Boolean.TRUE.toString()))) {
							sharedConfiguration.destroySharedConfiguration();
						}
					}
				}

				return locator;
			}
		}));
	}

	private static void waitForLocatorStart(final long milliseconds) {
		final InternalLocator locator = InternalLocator.getLocator();

		if (isClusterConfigurationEnabled(locator)) {
			ThreadUtils.timedWait(milliseconds, 500, new ThreadUtils.WaitCondition() {
				@Override public boolean waiting() {
					return !locator.isSharedConfigurationRunning();
				}
			});
		}
		else {
			LocatorLauncher.getInstance().waitOnStatusResponse(milliseconds, Math.min(500, milliseconds),
				TimeUnit.MILLISECONDS);
		}
	}

}
