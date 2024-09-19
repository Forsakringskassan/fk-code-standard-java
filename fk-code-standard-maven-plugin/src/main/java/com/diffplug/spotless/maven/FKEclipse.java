package com.diffplug.spotless.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;
import com.diffplug.spotless.extra.P2Mirror;
import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.diffplug.spotless.maven.java.Eclipse;

public class FKEclipse extends Eclipse {
	public static String ECLIPSE_VERSION = "4.29";
	public static final List<P2Mirror> P2_MIRROR = List.of(new P2Mirror() {
		@Override
		public String getPrefix() {
			return "https://download.eclipse.org/eclipse/updates/" + ECLIPSE_VERSION + "/";
		}

		@Override
		public String getUrl() {
			return "https://repo1.maven.org/maven2/Eclipse_" + ECLIPSE_VERSION + "/";
		}
	});

	private final File buildDir;
	private final Log log;

	public FKEclipse(final File buildDir, final Log log) {
		this.buildDir = buildDir;
		this.log = log;
	}

	@Override
	public FormatterStep newFormatterStep(final FormatterStepConfig stepConfig) {

		final String file = this.getLocalCodeStandard(this.buildDir, this.log).getAbsolutePath();
		// final List<P2Mirror> p2Mirrors = P2_MIRROR;
		final List<P2Mirror> p2Mirrors = new ArrayList<P2Mirror>();

		final File settingsFile = stepConfig.getFileLocator().locateFile(file);

		final EquoBasedStepBuilder eclipseConfig = EclipseJdtFormatterStep.createBuilder(stepConfig.getProvisioner());
		eclipseConfig.setVersion(ECLIPSE_VERSION);
		eclipseConfig.setPreferences(Arrays.asList(settingsFile));
		eclipseConfig.setP2Mirrors(p2Mirrors);

		return eclipseConfig.build();
	}

	private File getLocalCodeStandard(final File buildDir, final Log log) {
		final String codeStandardResourceName = "code-standard-java-eclipse.xml";
		try {
			final InputStream codeStandardResource = FKEclipse.class
					.getResourceAsStream("/" + codeStandardResourceName);
			if (codeStandardResource == null) {
				throw new RuntimeException("Cannot read resource " + codeStandardResourceName);
			}
			buildDir.mkdir();
			final File localFile = buildDir.toPath().resolve(codeStandardResourceName).toFile();
			final String localFileName = localFile.getPath();
			log.info("Saving code standard to local " + localFileName);
			codeStandardResource.transferTo(new FileOutputStream(localFileName));
			return localFile;
		} catch (final Exception e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException("Unable to copy " + codeStandardResourceName + " to build dir", e);
		}
	}
}