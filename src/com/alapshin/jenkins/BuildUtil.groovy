package com.alapshin.jenkins

import groovy.json.JsonOutput
import com.cloudbees.groovy.cps.NonCPS
import groovy.xml.MarkupBuilder
import hudson.model.Result
import hudson.scm.ChangeLogSet
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

import java.nio.charset.StandardCharsets

class BuildUtil implements Serializable {
    private static final String ARTIFACT_URL_TEMPLATE = "https://%s.s3.amazonaws.com/%s/%s"

    @NonCPS
    static boolean isArtifactsReady(RunWrapper build) {
        return !build.rawBuild.artifacts.empty
    }

    @NonCPS
    static List<String> getChangeLog(RunWrapper build) {
        RunWrapper prevBuild = build
        List<ChangeLogSet.Entry> entries = []

        while (prevBuild != null) {
            for (changeset in prevBuild.changeSets) {
                for (entry in changeset.items) {
                    entries += entry
                }
            }
            prevBuild = prevBuild.previousBuild
            // If previous build was successful stop changelog generation
            if (prevBuild == null || prevBuild.result == Result.SUCCESS.toString()) {
                break
            }
        }

        return entries
        // Show recent entries first
                .reverse()
        // Remove entries with same message to avoid duplicates after rebase
                .toUnique {entry -> entry.msg }
        // Transform entry to formatted message
                .collect { entry -> "By ${entry.author} on ${new Date(entry.timestamp)}: ${entry.msg}" }
    }

    // Get artifacts urls
    @NonCPS
    static List<String> getArtifactsUrls(RunWrapper build, String bucket) {
        String fullProjectName = URLDecoder.decode(build.fullProjectName, StandardCharsets.UTF_8) .replace("#", "")
        return build.rawBuild.artifacts.collect { artifact ->
            String.format(ARTIFACT_URL_TEMPLATE, bucket, fullProjectName, artifact.fileName)
        }
    }

    @NonCPS 
    static String generateMessage(RunWrapper build, String bucket) {
        if (!build.result) {
            if (!isArtifactsReady(build)) {
                return generateStartMessage(build, bucket)
            } else {
                return generateArtifactsMessage(build, bucket)
            }
        } else {
            generateResultMessage(build, bucket)
        }
    }

    @NonCPS
    static String generateBaseMessage(RunWrapper build, String bucket) {
        String status;
        String branch = URLDecoder.decode(build.projectName)
        if (!build.result) {
            if (!isArtifactsReady(build)) {
                status = "Started"
            } else {
                status = "In Progress"
            }
        } else {
            status = build.result.toLowerCase().capitalize()
        }

        return """
            **Build**
            ${build.id}

            **Status**
            ${status}

            **Branch**
            ${branch}
        """.stripIndent()
    }

    @NonCPS
    static String generateStartMessage(RunWrapper build, String bucket) {
        return generateBaseMessage(build, bucket)
    }

    @NonCPS
    static String generateResultMessage(RunWrapper build, String bucket) {
        String details = """
            **Details**
            ${build.absoluteUrl}
        """.stripIndent()
        return generateBaseMessage(build, bucket) + details
    }

    @NonCPS
    static String generateArtifactsMessage(RunWrapper build, String bucket) {
        String message = generateBaseMessage(build, bucket)

        String changelog = getChangeLog(build).join("\n")
        if (changelog) {
            message += """
                **Changes**
                ${changelog}
            """.stripIndent()
        }

        String artifacts = getArtifactsUrls(build, bucket).join("\n")
        if (artifacts) {
            message += """
                **Artifacts**
                ${artifacts}
            """.stripIndent()
        }

        return message
    }

    @NonCPS
    static String generateArtifactsHtmlMessage(RunWrapper build, String bucket) {
        StringWriter writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)
        List<String> artifactsUrls = getArtifactsUrls(build, bucket)

        builder.p {
            p("Remote artifacts")
            p {
                ul {
                    artifactsUrls.each { url ->
                        li {
                            a href: url, url
                        }
                    }
                }
            }
        }

        return writer.toString()
    }
}

