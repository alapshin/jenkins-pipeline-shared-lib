package com.alapshin.jenkins

import groovy.json.JsonOutput
import com.cloudbees.groovy.cps.NonCPS
import hudson.model.Result
import hudson.scm.ChangeLogSet
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

class BuildUtil implements Serializable {
    private static final String ARTIFACT_URL_TEMPLATE = "https://%s.s3.amazonaws.com/%s/%s/release/%s"

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

    // Get artifacts file names
    @NonCPS
    static List<String> getArtifactsUrls(RunWrapper build, String branch, String bucket) {
        return build.rawBuild.artifacts.collect { artifact ->
            String.format(ARTIFACT_URL_TEMPLATE, bucket, branch, build.id, artifact.fileName)
        }
    }

    @NonCPS
    static Map generateBaseMessage(RunWrapper build, String branch) {
        String color = "#000000"
        if (!build.result) {
            color = "#2196F3"
        } else if (build.result == "SUCCESS") {
            color = "#4CAF50"
        } else if (build.result == "ABORTED") {
            color = "#9E9E9E"
        } else if (build.result == "FAILURE") {
            color = "#F44336"
        }
        String status;
        if (!build.result) {
            status = "In Progress"
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
    static String generateProgressMessage(RunWrapper build, String branch) {
        def message = generateBaseMessage(build, branch)
        return JsonOutput.toJson([message])
    }

    @NonCPS
    static String generateFailureOrAbortedMessage(RunWrapper build, String branch) {
        def message = generateBaseMessage(build, branch)
        message.fields += [
                "title": "Details",
                "value": build.absoluteUrl
        ]

        return JsonOutput.toJson([message])
    }

    @NonCPS
    static String generateSuccessMessage(RunWrapper build, String branch, String bucket) {
        def artifacts = getArtifactsUrls(build, branch, bucket)
                .join("\n")

        // By default Slack attachment's field length is limited to 2048 bytes.
        // See https://github.com/jenkinsci/slack-plugin/pull/274#issuecomment-268710977
        //
        // To avoid changelog truncation we split it into multiple fields.
        // Assuming that each changelog entry is 160 symbols (80 symbols for commit
        // message + 30 symbols for date + 50 symbols for author name + other text)
        // we could fit approximately 12 entries into single field (calculated as 2048 / 160 = 12.8)
        def changelog = getChangeLog(build)
        // Split list to 12 sublists
                .collate(12)
        // For every sublist generate field entry
                .collect {[ "value" : "```${it.join('\n')}```" ]}

        def message = generateBaseMessage(build, branch)
        message.fields += [
                "title": "Changes"
        ]
        message.fields += changelog
        message.fields += [
                "title": "Artifacts",
                "value": artifacts
        ]

        return JsonOutput.toJson([message])
    }
}

