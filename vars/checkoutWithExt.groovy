import hudson.plugins.git.GitSCM

// Checkout step with custom behavior
//
// This is done to force Jenkins use value of "Author" field from git commits 
// in changelog instead of "Committer" field. 
// This way  we can get correct changelog after rebase or cherry-pick.
// Jenkins' documentation states that enabling this custom behavior disables 
// faster polling implementation (based on git ls-remote) however this is no 
// longer the case. For detail see  
// https://github.com/jenkinsci/git-plugin/commit/04152e98a19da6c43658a22c3f3340a26ac2fb49
//
// For details about how to customize checkoutWithExt behavior see
// https://support.cloudbees.com/hc/en-us/articles/226122247-How-to-Customize-Checkout-for-Pipeline-Multibranch-
//
// This is implemented as custom step in shared library to avoid whitelisting of access to scm.* attributes.
def call(GitSCM scm) {
    checkout([
            $class: 'GitSCM',
            branches: scm.branches,
            extensions: scm.extensions + [[$class: 'AuthorInChangelog']],
            userRemoteConfigs: scm.userRemoteConfigs,
            doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
    ])
}
