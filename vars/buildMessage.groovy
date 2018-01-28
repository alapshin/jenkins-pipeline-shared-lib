import com.alapshin.jenkins.BuildUtil
import hudson.model.Result
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

def call(Map args) {
    Map<String, String> scm = args.scmInfo
    String bucket = args.bucket
    RunWrapper build = args.build

    if (!build.result) {
        return BuildUtil.generateProgressMessage(build, scm.GIT_BRANCH)
    } else if (build.result == Result.SUCCESS.toString()) {
        return BuildUtil.generateSuccessMessage(build, scm.GIT_BRANCH, bucket)
    } else {
        return BuildUtil.generateFailureOrAbortedMessage(build, scm.GIT_BRANCH)
    }
}

