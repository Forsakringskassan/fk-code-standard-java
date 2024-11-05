package se.fk.fkcodestandard.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.logging.LogLevel;

import com.diffplug.gradle.spotless.JavaExtension.EclipseConfig;
import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessExtensionImpl;

public class FKCodeStandardPlugin implements Plugin<Project> {
	private static final String PROP_SKIP_APPLY = "skip-automatic-fk-code-standard-apply";

	@Override
	public void apply(final Project project) {
		project.getRepositories().maven(new Action<MavenArtifactRepository>() {
			@Override
			public void execute(final MavenArtifactRepository t) {
				t.setName("java-all");
				t.setUrl("https://repo1.maven.org/maven2/");
			}
		});

		final SpotlessExtension spotlessExtension = new SpotlessExtensionImpl(project);

		spotlessExtension.java(spotlessJavaExtension -> {
			spotlessJavaExtension.target("**/*.java");
			spotlessJavaExtension.targetExclude("**/gen/**", "**/generated/**");

			final String eclipseVersion = "4.29";
			final EclipseConfig eclipse = spotlessJavaExtension.eclipse(eclipseVersion);
			eclipse.configFile(this.getLocalCodeStandard(project));
//			eclipse.withP2Mirrors(Map.of("https://download.eclipse.org/eclipse/updates/" + eclipseVersion + "/",
//					"https://repo1.maven.org/maven2/Eclipse_" + eclipseVersion + "/"));
		});

		project.getExtensions().add("fkSpotlessExtension", spotlessExtension);

		project.getTasksByName("check", true).forEach(checkTask -> {
			final boolean shouldFormat = project.getProperties().get(PROP_SKIP_APPLY) == null;
			if (shouldFormat) {
				project.getLogger()
						.info("Automatic formatting, can be turned of with " + PROP_SKIP_APPLY + " property.");
				project.getTasksByName("spotlessApply", true).forEach(spotlessApplyTask -> {
					checkTask.dependsOn(spotlessApplyTask);
				});
			} else {
				project.getLogger().info("Skipping automatic formatting because of " + PROP_SKIP_APPLY + " property.");
			}
		});
	}

	private File getLocalCodeStandard(final Project project) {
		final String codeStandardResourceName = "code-standard-java-eclipse.xml";
		try {
			final InputStream codeStandardResource = FKCodeStandardPlugin.class
					.getResourceAsStream("/" + codeStandardResourceName);
			if (codeStandardResource == null) {
				throw new GradleException("Cannot read resource " + codeStandardResourceName);
			}
			final File buildDir = project.getBuildDir();
			buildDir.mkdir();
			final File localFile = buildDir.toPath().resolve(codeStandardResourceName).toFile();
			final String localFileName = localFile.getPath();
			project.getLogger().log(LogLevel.INFO, "Saving code standard to local " + localFileName);
			codeStandardResource.transferTo(new FileOutputStream(localFileName));
			return localFile;
		} catch (final Exception e) {
			project.getLogger().log(LogLevel.ERROR, e.getMessage(), e);
			throw new GradleException("Unable to copy " + codeStandardResourceName + " to build dir", e);
		}
	}
}
