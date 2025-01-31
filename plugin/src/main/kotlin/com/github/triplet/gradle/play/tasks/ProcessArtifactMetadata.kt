package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.tasks.internal.PlayPublishEditTaskBase
import com.github.triplet.gradle.play.tasks.internal.buildPublisher
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class ProcessArtifactMetadata @Inject constructor(
        extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PlayPublishEditTaskBase(extension, variant) {
    init {
        // Always out-of-date since we don't know what's changed on the network
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun process() {
        val maxVersionCode = extension.config.buildPublisher().edits().tracks()
                .list(variant.applicationId, editId).execute().tracks
                ?.flatMap { it.releases.orEmpty() }
                ?.flatMap { it.versionCodes.orEmpty() }
                ?.max() ?: 1

        val outputs = variant.outputs.filterIsInstance<ApkVariantOutput>()
        val smallestVersionCode = outputs.map { it.versionCode }.min() ?: 1

        val patch = maxVersionCode - smallestVersionCode + 1
        for ((i, output) in outputs.withIndex()) {
            if (patch > 0) output.versionCodeOverride = output.versionCode + patch.toInt() + i
            extension.config.outputProcessor?.execute(output)
        }
    }
}
