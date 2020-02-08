package com.alapshin.jenkins

import groovy.json.JsonOutput
import com.cloudbees.groovy.cps.NonCPS
import groovy.xml.MarkupBuilder
import hudson.model.Result
import hudson.scm.ChangeLogSet
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

import java.nio.charset.StandardCharsets

class BuildUtil implements Serializable {
    // Number of entries from changelog per single attachments.
    // Because Slack's has limit on attachments' value size we have to
    // split changelog to multiple chunks if we want to send it all.
    private static final int CHANGELOG_CHUNK_SIZE = 12
    // Number of changelog chunks that will be sent with message.
    // In some cases changelog could be very long (for example after rebase of
    // long lived branch) and sending it all will flood the channel.
    // To avoid this limit total number of chunks being sent to sane amount.
    private static final int CHANGELOG_CHUNK_COUNT = 2
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
    static Map generateBaseMessage(RunWrapper build, String bucket) {
        String color = "#000000"
        String branch = URLDecoder.decode(build.projectName)
        if (!build.result) {
            color = "#2196F3"
        } else if (build.result == Result.SUCCESS.toString()) {
            color = "#4CAF50"
        } else if (build.result == Result.ABORTED.toString()) {
            color = "#9E9E9E"
        } else if (build.result == Result.FAILURE.toString()) {
            color = "#F44336"
        }
        String status;
        if (!build.result) {
            if (!isArtifactsReady(build)) {
                status = "Started"
            } else {
                status = "In Progress"
            }
        } else {
            status = build.result.toLowerCase().capitalize()
        }

        return [
                "color": color,
                "mrkdwn_in": [
                        "text",
                        "pretext",
                        "fields"
                ],
                "fields": [
                        [
                                "title": "Build",
                                "value": build.id
                        ],
                        [
                                "title": "Status",
                                "value": status
                        ],
                        [
                                "title": "Branch",
                                "value": branch
                        ],
                ]
        ]
    }

    @NonCPS
    static String generateStartMessage(RunWrapper build, String bucket) {
        def message = generateBaseMessage(build, bucket)
        return JsonOutput.toJson([message])
    }

    @NonCPS
    static String generateResultMessage(RunWrapper build, String bucket) {
        def message = generateBaseMessage(build, bucket)
        message.fields += [
                "title": "Details",
                "value": build.absoluteUrl
        ]

        return JsonOutput.toJson([message])
    }

    @NonCPS
    static String generateArtifactsMessage(RunWrapper build, String bucket) {
        // By default Slack attachment's field length is limited to 2048 bytes.
        // See https://github.com/jenkinsci/slack-plugin/pull/274#issuecomment-268710977
        //
        // To avoid changelog truncation we split it into multiple fields.
        // Assuming that each changelog entry is 160 symbols (80 symbols for commit
        // message + 30 symbols for date + 50 symbols for author name + other text)
        // we could fit approximately 12 entries into single field (calculated as 2048 / 160 = 12.8)
        def changelog = getChangeLog(build)
                // Split changelog to chunks
                .collate(CHANGELOG_CHUNK_SIZE)
                // Limit total number of chunks
                .take(CHANGELOG_CHUNK_COUNT)
                // For every sublist generate field entry
                .collect {[ "value" : "```${it.join('\n')}```" ]}

        def message = generateBaseMessage(build, bucket)
        if (changelog) {
            message.fields += [
                    "title": "Changes"
            ]
            message.fields += changelog
        }

        def artifacts = getArtifactsUrls(build, bucket)
                .join("\n")
        if (artifacts) {
            message.fields += [
                    "title": "Artifacts",
                    "value": artifacts
            ]
        }

        return JsonOutput.toJson([message])
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

