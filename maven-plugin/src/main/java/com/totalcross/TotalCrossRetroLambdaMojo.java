/*
 * Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import com.totalcross.tooling.compatibility.JavaCompatibilityPolicy;

@Mojo(name = "retrolambda", requiresDependencyResolution = ResolutionScope.COMPILE)
public class TotalCrossRetroLambdaMojo extends AbstractMojo {
	private static final int MAX_JAVA_11_CLASS_FILE_VERSION = 55;

	@Component
	private MavenProject mavenProject;

	@Component
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;

	@Parameter
	private String jdkPath;

	public void execute() throws MojoExecutionException, MojoFailureException {
		Artifact sdk = totalCrossSdk();
		boolean legacySdk = sdk == null;
		try {
			legacySdk = sdk == null || JavaCompatibilityPolicy.usesJdk11(sdk.getVersion());
		} catch (IllegalArgumentException error) {
			throw new MojoExecutionException(error.getMessage(), error);
		}
		if (sdk != null && !legacySdk) {
			getLog().info("Skipping Retrolambda because TotalCross SDK " + sdk.getVersion()
					+ " uses the Java 17 compatibility policy.");
			return;
		}
		if (jdkPath == null) {
			JavaJDKManager javaJDKManager = new JavaJDKManager();
			try {
				javaJDKManager.init();
			} catch (IOException e) {
				throw new MojoExecutionException(e.getMessage(), e);
			}

			jdkPath = javaJDKManager.getPath().getAbsolutePath();
		}

		executeMojo(
				plugin(groupId("net.orfjackal.retrolambda"), artifactId("retrolambda-maven-plugin"), version("2.5.7")),
				goal("process-main"), configuration(element("java8home", jdkPath), element("fork", "true")),
				executionEnvironment(mavenProject, mavenSession, pluginManager));
	}

	private Artifact totalCrossSdk() {
		Set<Artifact> artifacts = mavenProject.getArtifacts();
		for (Artifact artifact : artifacts) {
			if ("com.totalcross".equals(artifact.getGroupId()) && "totalcross-sdk".equals(artifact.getArtifactId())) {
				return artifact;
			}
		}
		return null;
	}

	static boolean hasModernBytecode(File artifact) throws MojoExecutionException {
		if (artifact == null || !artifact.exists()) return false;
		try {
			if (artifact.isDirectory()) {
				return classFileVersion(new File(artifact, "totalcross/ui/Container.class")) > MAX_JAVA_11_CLASS_FILE_VERSION;
			}
			try (JarFile jar = new JarFile(artifact)) {
				for (java.util.Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
					JarEntry entry = entries.nextElement();
					if (!entry.isDirectory() && entry.getName().startsWith("totalcross/") && entry.getName().endsWith(".class")) {
						try (InputStream input = jar.getInputStream(entry)) {
							if (classFileVersion(input) > MAX_JAVA_11_CLASS_FILE_VERSION) return true;
						}
					}
				}
			}
			return false;
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to inspect TotalCross SDK bytecode", e);
		}
	}

	private static int classFileVersion(File file) throws IOException {
		if (!file.isFile()) return -1;
		try (InputStream input = new java.io.FileInputStream(file)) {
			return classFileVersion(input);
		}
	}

	private static int classFileVersion(InputStream input) throws IOException {
		byte[] header = new byte[8];
		int offset = 0;
		while (offset < header.length) {
			int read = input.read(header, offset, header.length - offset);
			if (read < 0) return -1;
			offset += read;
		}
		if (header[0] != (byte) 0xca || header[1] != (byte) 0xfe || header[2] != (byte) 0xba || header[3] != (byte) 0xbe) return -1;
		return ((header[6] & 0xff) << 8) | (header[7] & 0xff);
	}
}
