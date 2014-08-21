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

package org.springframework.data.gemfire.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.data.gemfire.test.support.FileSystemUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The ProcessExecutor class is a utility class for launching and running Java processes.
 *
 * @author John Blum
 * @see java.lang.Process
 * @see java.lang.ProcessBuilder
 * @see org.springframework.data.gemfire.process.ProcessConfiguration
 * @see org.springframework.data.gemfire.process.ProcessWrapper
 * @since 1.5.0
 */
@SuppressWarnings("unused")
public abstract class ProcessExecutor {

	protected static final File JAVA_EXE = new File(new File(FileSystemUtils.JAVA_HOME, "bin"), "java");

	protected static final String JAVA_CLASSPATH = System.getProperty("java.class.path");

	protected static final String SPRING_GEMFIRE_SYSTEM_PROPERTY_PREFIX = "spring.gemfire.";

	public static ProcessWrapper launch(final Class<?> type, final String... args) throws IOException {
		return launch(FileSystemUtils.WORKING_DIRECTORY, type, args);
	}

	public static ProcessWrapper launch(final File workingDirectory, final Class<?> type, final String... args)
			throws IOException
	{
		ProcessBuilder processBuilder = new ProcessBuilder()
			.command(buildCommand(type, args))
			.directory(validateDirectory(workingDirectory))
			.redirectErrorStream(true);

		Process process = processBuilder.start();

		ProcessWrapper processWrapper = new ProcessWrapper(process, ProcessConfiguration.create(processBuilder));

		processWrapper.register(new ProcessInputStreamListener() {
			@Override public void onInput(final String input) {
				System.err.printf("[FORK-OUT] - %1$s%n", input);
			}
		});

		return processWrapper;
	}

	protected static String[] buildCommand(final Class<?> type, final String... args) {
		Assert.notNull(type != null, "The main Class to launch must not be null!");

		List<String> command = new ArrayList<String>();
		List<String> programArgs = Collections.emptyList();

		command.add(JAVA_EXE.getAbsolutePath());
		command.add("-server");
		command.add("-classpath");
		command.add(JAVA_CLASSPATH);
		command.addAll(getSpringGemFireSystemProperties());

		if (args != null) {
			programArgs = new ArrayList<String>(args.length);

			for (String arg : args) {
				if (isJvmOption(arg)) {
					command.add(arg);
				}
				else if (!StringUtils.isEmpty(arg)) {
					programArgs.add(arg);
				}
			}
		}

		command.add(type.getName());
		command.addAll(programArgs);

		return command.toArray(new String[command.size()]);
	}

	protected static Collection<? extends String> getSpringGemFireSystemProperties() {
		List<String> springGemfireSystemProperties = new ArrayList<String>();

		for (String property : System.getProperties().stringPropertyNames()) {
			if (property.startsWith(SPRING_GEMFIRE_SYSTEM_PROPERTY_PREFIX)) {
				springGemfireSystemProperties.add(String.format("-D%1$s=%2$s", property, System.getProperty(property)));
			}
		}

		return springGemfireSystemProperties;
	}

	protected static boolean isJvmOption(final String option) {
		return (!StringUtils.isEmpty(option) && (option.startsWith("-D") || option.startsWith("-X")));
	}

	protected static File validateDirectory(final File workingDirectory) {
		Assert.isTrue(workingDirectory != null && (workingDirectory.isDirectory() || workingDirectory.mkdirs()));
		return workingDirectory;
	}

}
