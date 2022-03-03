def call() {
    pipeline{
        agent { 
            node {
                label "vickey-test-jnlp"
            }
        }
        options {
            skipDefaultCheckout()  //删除隐式checkout scm语句
            timeout(time: 5, unit: 'MINUTES')
        }
        stages {
            stage('pod status check') {
                steps{
                    script{
                        def gitlab = new org.devops.gitlab()
                        gitlab.CheckoutOnly()
                        withKubeConfig(credentialsId: 'prod-configfile', serverUrl: 'https://vickeywu.prod.ap-east-1.eks.amazonaws.com') {
                            POD_STATUS = sh(returnStdout: true, script: "kubectl get pod |grep -v Running |grep -v Terminating |grep -v ContainerCreating").trim()
                            sh "echo $POD_STATUS"
                            if ("$POD_STATUS" == "1") {
                                sh "send email"
                            }
                        }
                    }
                }
            }
        }
    }
}