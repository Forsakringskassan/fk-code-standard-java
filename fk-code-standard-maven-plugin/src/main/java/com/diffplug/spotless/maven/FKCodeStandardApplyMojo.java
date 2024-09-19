package com.diffplug.spotless.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;
import com.diffplug.spotless.maven.incremental.UpToDateChecker;

/**
 * Very much inspired by:
 * https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/SpotlessApplyMojo.java
 */
@Mojo(name = "spotlessApply", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class FKCodeStandardApplyMojo extends AbstractFKCodeStandardMojo {
	@Parameter(property = "skip-automatic-fk-code-standard-apply", defaultValue = "false")
	private boolean applySkip;

	@Override
	protected void process(final Iterable<File> files, final Formatter formatter, final UpToDateChecker upToDateChecker)
			throws MojoExecutionException {
		final ImpactedFilesTracker counter = new ImpactedFilesTracker();

		for (final File file : files) {
			if (upToDateChecker.isUpToDate(file.toPath())) {
				counter.skippedAsCleanCache();
				if (this.getLog().isDebugEnabled()) {
					this.getLog().debug("Spotless will not format an up-to-date file: " + file);
				}
				continue;
			}

			try {
				final PaddedCell.DirtyState dirtyState = PaddedCell.calculateDirtyState(formatter, file);
				if (!dirtyState.isClean() && !dirtyState.didNotConverge()) {
					this.getLog().info(String.format("Writing clean file: %s", file));
					dirtyState.writeCanonicalTo(file);
					this.buildContext.refresh(file);
					counter.cleaned();
				} else {
					counter.checkedButAlreadyClean();
				}
			} catch (IOException | RuntimeException e) {
				throw new MojoExecutionException("Unable to format file " + file, e);
			}

			upToDateChecker.setUpToDate(file.toPath());
		}

		// We print the number of considered files which is useful when ratchetFrom is
		// setup
		if (counter.getTotal() > 0) {
			this.getLog().info(String.format(
					"Spotless.%s is keeping %s files clean - %s were changed to be clean, %s were already clean, %s were skipped because caching determined they were already clean",
					formatter.getName(), counter.getTotal(), counter.getCleaned(), counter.getCheckedButAlreadyClean(),
					counter.getSkippedAsCleanCache()));
		} else {
			this.getLog().debug(String.format(
					"Spotless.%s has no target files. Examine your `<includes>`: https://github.com/diffplug/spotless/tree/main/plugin-maven#quickstart",
					formatter.getName()));
		}
	}

	@Override
	protected boolean shouldSkip() {
		return this.applySkip;
	}
}
