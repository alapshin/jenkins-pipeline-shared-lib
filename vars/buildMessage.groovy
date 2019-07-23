import com.alapshin.jenkins.BuildUtil
import hudson.model.Result
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

def call(Map args) {
    Map<String, String> scm = args.scmInfo
    String bucket = args.bucket
    RunWrapper build = args.build

    return BuildUtil.generateMessage(build, scm.GIT_BRANCH, bucket)
}

