import com.alapshin.jenkins.BuildUtil
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

def call(RunWrapper build, String bucket) {
    def message = BuildUtil.generateArtifactsHtmlMessage(
            build, bucket)
    rtp(parserName: 'HTML', nullAction: '1', stableText: message)
}
