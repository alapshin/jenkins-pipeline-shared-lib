import com.alapshin.jenkins.BuildUtil
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

def call(RunWrapper build, String bucket) {
    return BuildUtil.generateMessage(build, bucket)
}

