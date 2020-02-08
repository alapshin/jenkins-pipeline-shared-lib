import java.nio.charset.StandardCharsets

import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

// Get full project name
def call(RunWrapper build) {
    // Jenkins uses URL encoding to store project names.
    // As a result slashes in branch names are represented as `%2F` and we have to use URL decoding to reverse it.
    return URLDecoder.decode(build.fullProjectName, StandardCharsets.UTF_8)
}