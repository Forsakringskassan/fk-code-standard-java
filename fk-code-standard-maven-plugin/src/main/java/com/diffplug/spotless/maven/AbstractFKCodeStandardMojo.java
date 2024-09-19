package com.diffplug.spotless.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.maven.incremental.UpToDateChecker;
import com.diffplug.spotless.maven.incremental.UpToDateChecking;
import com.diffplug.spotless.maven.java.Eclipse;
import com.diffplug.spotless.maven.java.Java;

/**
 * Very much inspired by:
 * https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/AbstractSpotlessMojo.java
 */
public abstract class AbstractFKCodeStandardMojo extends AbstractMojo {
	private static final String DEFAULT_INDEX_FILE_NAME = "spotless-index";
	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final String DEFAULT_LINE_ENDINGS = "GIT_ATTRIBUTES_FAST_ALLSAME";

	@Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
	File baseDir;
	@Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
	private File buildDir;

	@Parameter(defaultValue = DEFAULT_ENCODING)
	private String encoding;

	@Parameter(defaultValue = DEFAULT_LINE_ENDINGS)
	private LineEnding lineEndings;

	@Parameter(defaultValue = "${mojoExecution.goal}", required = true, readonly = true)
	private String goal;

	@Component
	BuildContext buildContext;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Component
	private RepositorySystem repositorySystem;
	@Component
	private ResourceManager resourceManager;

	@Parameter(defaultValue = "${project.remotePluginRepositories}", required = true, readonly = true)
	private List<RemoteRepository> repositories;

	@Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
	private RepositorySystemSession repositorySystemSession;

	@Parameter
	private final UpToDateChecking upToDateChecking = UpToDateChecking.enabled();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.shouldSkip()) {
			this.getLog().info(String.format("Spotless %s skipped", this.goal));
			return;
		}

		final List<FormatterFactory> formatterFactories = this.getFormatterFactories();
		final FormatterConfig config = this.getFormatterConfig();

		final Map<FormatterFactory, Supplier<Iterable<File>>> formatterFactoryToFiles = new LinkedHashMap<>();
		for (final FormatterFactory formatterFactory : formatterFactories) {
			final Supplier<Iterable<File>> filesToFormat = () -> this.collectFiles();
			formatterFactoryToFiles.put(formatterFactory, filesToFormat);
		}

		try (FormattersHolder formattersHolder = FormattersHolder.create(formatterFactoryToFiles, config);
				UpToDateChecker upToDateChecker = this.createUpToDateChecker(formattersHolder.getFormatters())) {
			for (final Entry<Formatter, Supplier<Iterable<File>>> entry : formattersHolder.getFormattersWithFiles()
					.entrySet()) {
				final Formatter formatter = entry.getKey();
				final Iterable<File> files = entry.getValue().get();
				this.process(files, formatter, upToDateChecker);
			}
		} catch (final PluginException e) {
			throw e.asMojoExecutionException();
		}
	}

	private List<FormatterFactory> getFormatterFactories() {
		final Eclipse eclipseFormatter = new FKEclipse(this.buildDir, this.getLog());

		final Java javaFormatter = new Java();
		javaFormatter.addEclipse(eclipseFormatter);

		return List.of(javaFormatter);
	}

	private FormatterConfig getFormatterConfig() {
		final ArtifactResolver resolver = new ArtifactResolver(this.repositorySystem, this.repositorySystemSession,
				this.repositories, this.getLog());
		final Provisioner provisioner = MavenProvisioner.create(resolver);
		final List<FormatterStepFactory> formatterStepFactories = new ArrayList<FormatterStepFactory>();
		final FileLocator fileLocator = this.getFileLocator();
		final Optional<String> optionalRatchetFrom = Optional.empty();
		return new FormatterConfig(this.baseDir, this.encoding, this.lineEndings, optionalRatchetFrom, provisioner,
				fileLocator, formatterStepFactories, Optional.empty());
	}

	private FileLocator getFileLocator() {
		this.resourceManager.addSearchPath(FileResourceLoader.ID, this.baseDir.getAbsolutePath());
		this.resourceManager.addSearchPath("url", "");
		this.resourceManager.setOutputDirectory(this.buildDir);
		return new FileLocator(this.resourceManager, this.baseDir, this.buildDir);
	}

	protected abstract void process(Iterable<File> files, Formatter formatter, UpToDateChecker upToDateChecker)
			throws MojoExecutionException;

	Iterable<File> collectFiles() {
		final String includesString = "**/*.java";
		final String excludesString = "**/gen/**, **/generated/**, .mvn/**/*";
		try {
			return FileUtils.getFiles(this.baseDir, includesString, excludesString);
		} catch (final IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private UpToDateChecker createUpToDateChecker(final Iterable<Formatter> formatters) {
		Path indexFile = this.upToDateChecking == null ? null : this.upToDateChecking.getIndexFile();
		if (indexFile == null) {
			final Path targetDir = this.project.getBasedir().toPath().resolve(this.project.getBuild().getDirectory());
			indexFile = targetDir.resolve(DEFAULT_INDEX_FILE_NAME);
		}
		final UpToDateChecker checker;
		if (this.upToDateChecking != null && this.upToDateChecking.isEnabled()) {

			checker = UpToDateChecker.forProject(new FakeProject(this.project), indexFile, formatters, this.getLog());
		} else {
			this.getLog().info("Up-to-date checking disabled");
			checker = UpToDateChecker.noop(this.project, indexFile, this.getLog());
		}
		return UpToDateChecker.wrapWithBuildContext(checker, this.buildContext);
	}

	protected abstract boolean shouldSkip();

}
