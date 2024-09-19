package com.diffplug.spotless.maven;

import java.io.File;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

/**
 * To avoid: <code>
 * Caused by: java.lang.IllegalArgumentException: Spotless plugin absent from the project: MavenProject: ....
    at com.diffplug.spotless.maven.incremental.PluginFingerprint.findSpotlessPlugin (PluginFingerprint.java:100)
    at com.diffplug.spotless.maven.incremental.PluginFingerprint.from (PluginFingerprint.java:47)
    at com.diffplug.spotless.maven.incremental.IndexBasedChecker.create (IndexBasedChecker.java:43)
    at com.diffplug.spotless.maven.incremental.UpToDateChecker.forProject (UpToDateChecker.java:39)
 * </code>
 * 
 */
public class FakeProject extends MavenProject {
	private final MavenProject real;

	public FakeProject(final MavenProject real) {
		this.real = real;
	}

	@Override
	public Plugin getPlugin(final String pluginKey) {
		return new Plugin() {
			private static final long serialVersionUID = -2660609503449848888L;

			@Override
			public String getVersion() {
				return "0.0.1";
			}
		};
	}

	@Override
	public File getBasedir() {
		return this.real.getBasedir();
	}
}
