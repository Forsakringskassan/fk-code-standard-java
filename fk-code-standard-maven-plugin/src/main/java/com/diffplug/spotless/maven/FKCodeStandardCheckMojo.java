package com.diffplug.spotless.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;
import com.diffplug.spotless.maven.incremental.UpToDateChecker;

/**
 * Very much inspired by:
 * https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/SpotlessCheckMojo.java
 */
@Mojo(name = "spotlessCheck", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class FKCodeStandardCheckMojo extends AbstractFKCodeStandardMojo {

	@Override
	protected void process(final Iterable<File> files, final Formatter formatter, final UpToDateChecker upToDateChecker)
			throws MojoExecutionException {
		final ImpactedFilesTracker counter = new ImpactedFilesTracker();

		final List<File> problemFiles = new ArrayList<>();
		for (final File file : files) {
			if (upToDateChecker.isUpToDate(file.toPath())) {
				counter.skippedAsCleanCache();
				if (this.getLog().isDebugEnabled()) {
					this.getLog().debug("Spotless will not check an up-to-date file: " + file);
				}
				continue;
			}

			try {
				final PaddedCell.DirtyState dirtyState = PaddedCell.calculateDirtyState(formatter, file);
				if (!dirtyState.isClean() && !dirtyState.didNotConverge()) {
					problemFiles.add(file);
					if (this.buildContext.isIncremental()) {
						final Map.Entry<Integer, String> diffEntry = DiffMessageFormatter.diff(formatter, file);
						this.buildContext.addMessage(file, diffEntry.getKey() + 1, 0, diffEntry.getValue(),
								BuildContext.SEVERITY_ERROR, null);
					}
					counter.cleaned();
				} else {
					counter.checkedButAlreadyClean();
					upToDateChecker.setUpToDate(file.toPath());
				}
			} catch (IOException | RuntimeException e) {
				throw new MojoExecutionException("Unable to check file " + file, e);
			}
		}

		if (!problemFiles.isEmpty()) {
			throw new MojoExecutionException(
					DiffMessageFormatter.builder().runToFix("Run './mvnw se.fk.codestandard:fk-code-standard-maven-plugin:spotlessApply' to fix these violations.")
							.formatter(formatter).problemFiles(problemFiles).getMessage());
		}
	}

	@Override
	protected boolean shouldSkip() {
		return false;
	}
}
