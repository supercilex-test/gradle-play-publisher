package com.github.triplet.gradle.play

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class CompatibilityTest(
        private val agpVersion: String,
        private val gradleVersion: String
) {
    private val pluginBinaryDir = File("build/libs")
    private val pluginVersionName = System.getProperty("VERSION_NAME")

    private lateinit var testProject: Project

    @Before
    fun setup() {
        testProject = ProjectBuilder.builder()
                .withProjectDir(File("src/test/fixtures/android_app"))
                .build()
    }

    @Test
    fun pluginIsCompatible() {
        assert(pluginTest())
    }

    private fun pluginTest(): Boolean {
        val pluginJar = pluginBinaryDir
                .listFiles()
                .first { it.name.endsWith("$pluginVersionName.jar") }
                .absoluteFile
                .invariantSeparatorsPath

        // language=gradle
        File(testProject.projectDir, "build.gradle").writeText("""
        buildscript {
            repositories {
                google()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:$agpVersion'
                classpath files("$pluginJar")

                // Manually define transitive dependencies for our plugin since we don't have the
                // POM to fetch them for us
                classpath('com.google.apis:google-api-services-androidpublisher:v3-rev46-1.25.0')
            }
        }

        apply plugin: 'com.android.application'
        apply plugin: 'com.github.triplet.play'

        android {
            compileSdkVersion 28

            defaultConfig {
                applicationId "com.github.triplet.gradle.play.test"
                minSdkVersion 21
                targetSdkVersion 28
                versionCode 1
                versionName "1.0"
            }
        }

        play {
            serviceAccountCredentials = file('some-file.json')
        }
        """)

        GradleRunner.create()
                .withPluginClasspath()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProject.projectDir)
                .withArguments("checkReleaseManifest")
                .build()
        GradleRunner.create()
                .withPluginClasspath()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProject.projectDir)
                .withArguments("clean")
                .build()

        return true
    }

    @After
    fun cleanup() {
        File(testProject.projectDir, "build.gradle").delete()
    }

    private companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "agpVersion: {0}, gradleVersion {1}")
        fun parameters() = listOf(
                arrayOf("3.5.0-beta05", "5.4.1"), // Oldest supported
                arrayOf("3.5.0-beta05", "5.4.1"), // Latest stable
                arrayOf("3.6.0-alpha05", "5.5.1") // Latest
        )
    }
}
