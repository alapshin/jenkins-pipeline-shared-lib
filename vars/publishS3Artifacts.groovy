import com.alapshin.jenkins.BuildUtil
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

def call(Map args) {
    String bucket = args.bucket
    RunWrapper build = args.build

    rtp(parserName: 'HTML', nullAction: '1', stableText: BuildUtil.generateArtifactsHtmlMessage(build, bucket))
}
