import com.alapshin.jenkins.BuildUtil
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

def call(Map args) {
    String bucket = args.bucket
    RunWrapper build = args.build

    return BuildUtil.generateMessage(build, bucket)
}

