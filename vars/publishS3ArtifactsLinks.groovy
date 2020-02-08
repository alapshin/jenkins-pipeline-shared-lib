import com.alapshin.jenkins.BuildUtil
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

def call(RunWrapper build, String bucket) {
    rtp(parserName: 'HTML', nullAction: '1', stableText: BuildUtil.generateArtifactsHtmlMessage(build, bucket))
}
