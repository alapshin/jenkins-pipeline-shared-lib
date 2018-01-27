// Checkout step with custom behavior
//
// This is done to make Jenkins use value of "Author" field from git commits in build changelog instead of "Committer"
// field. This way  we get correct changelog after rebase or cherry-pick.
// Jenkins' documentation states that enabling this custom behavior disables faster polling implementation (based on
// git ls-remote) but since we don't use polling this should be acceptable trade-off.
//
// For details about how to customize checkout behavior see
// https://support.cloudbees.com/hc/en-us/articles/226122247-How-to-Customize-Checkout-for-Pipeline-Multibranch-
//
// To avoid manual whitelisting of access to scm.* attributes this is implemented as custom step in shared library.

def call(scm) {
    checkout([
            $class: 'GitSCM',
            branches: scm.branches,
            extensions: scm.extensions + [[$class: 'AuthorInChangelog']],
            userRemoteConfigs: scm.userRemoteConfigs,
            doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
    ])
}
