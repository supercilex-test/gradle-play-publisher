package com.github.triplet.gradle.play.tasks

import com.android.build.gradle.api.ApplicationVariant
import com.github.triplet.gradle.play.PlayPublisherExtension
import com.github.triplet.gradle.play.internal.MIME_TYPE_STREAM
import com.github.triplet.gradle.play.internal.orNull
import com.github.triplet.gradle.play.tasks.internal.ArtifactExtensionOptions
import com.github.triplet.gradle.play.tasks.internal.PlayPublishTaskBase
import com.github.triplet.gradle.play.tasks.internal.PlayWorkerBase
import com.github.triplet.gradle.play.tasks.internal.findBundleFile
import com.github.triplet.gradle.play.tasks.internal.paramsForBase
import com.google.api.client.http.FileContent
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

abstract class PublishInternalSharingBundle @Inject constructor(
        @get:Nested override val extension: PlayPublisherExtension,
        variant: ApplicationVariant
) : PlayPublishTaskBase(extension, variant), ArtifactExtensionOptions {
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    val bundle: File?
        get() = findBundleFile()
    @Suppress("MemberVisibilityCanBePrivate", "unused") // Used by Gradle
    @get:OutputDirectory
    protected val outputDir by lazy {
        File(project.buildDir, "outputs/internal-sharing/bundle/${variant.name}")
    }

    @TaskAction
    fun publishBundle() {
        val bundleFile = bundle?.orNull() ?: return
        project.serviceOf<WorkerExecutor>().submit(BundleUploader::class) {
            paramsForBase(this, BundleUploader.Params(bundleFile, outputDir))
        }
    }

    private class BundleUploader @Inject constructor(
            private val p: Params,
            data: PlayPublishingParams
    ) : PlayWorkerBase(data) {
        override fun run() {
            val bundle = publisher.internalappsharingartifacts()
                    .uploadbundle(appId, FileContent(MIME_TYPE_STREAM, p.bundleFile))
                    .trackUploadProgress("App Bundle", p.bundleFile)
                    .execute()

            File(p.outputDir, "${System.currentTimeMillis()}.json")
                    .writeText(bundle.toPrettyString())
            println("Upload successful: ${bundle.downloadUrl}")
        }

        data class Params(val bundleFile: File, val outputDir: File) : Serializable
    }
}
