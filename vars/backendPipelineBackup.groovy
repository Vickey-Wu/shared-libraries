def call() {
    pipeline{
        agent { 
            node {
                label "vickey-test-jnlp"
            }
        }
        options {
            skipDefaultCheckout()  //删除隐式checkout scm语句
            timeout(time: 15, unit: 'MINUTES')
        }
        stages {
            stage('Prepare') {
                steps{
                    script{
                        def gitlab = new org.devops.gitlab()
                        gitlab.Checkout()
                    }
                }
            }
            stage('Build') {
                steps{
                    script{
                        def build = new org.devops.build()
                        build.MavenBuild()
                    }
                }
            }
            stage('Deploy to test') {
                steps{
                    script{
                        def deploy = new org.devops.deploy()
                        deploy.DeployToTest()
                    }
                }
            }
            stage('Deploy to staging') {
                steps{
                    script{
                        def deploy = new org.devops.deploy()
                        deploy.DeployToStaging()
                    }
                }
            }
            // stage('Deploy to product') {
            //     steps{
            //         script{
            //             def deploy = new org.devops.deploy()
            //             deploy.DeployToProduct()
            //         }
            //     }
            // }
            stage('SonarQube analysis') {
                steps{
                    script{
                        def sonarqube = new org.devops.sonarqube()
                        sonarqube.SonarqubeScan()
                    }
                }
            }
        }
    }
}
