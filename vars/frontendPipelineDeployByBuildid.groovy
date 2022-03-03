def call() {
    pipeline{
        agent { 
            node {
                label "vickey-test-jnlp"
            }
        }
        options {
            skipDefaultCheckout()  //删除隐式checkout scm语句
            timeout(time: 8, unit: 'MINUTES')
        }
        stages {
            stage('选择你要发布的项目') {
                steps{
                    script{
                        env.GIT_REPO_NAME = input message: 'User input required', ok: 'continue', parameters: [choice(choices: ['vickey-admin','vickey-mobile','vickey-supplier','vickey'], description: '', name: '选择你要发布的项目')]
                        sh "echo GIT_REPO_NAME -----------> ${env.GIT_REPO_NAME}"
                    }
                }
            }
            stage('输入该项目打包要发生产的job id') {
                steps{
                    script{
                        env.REPO_JOB_ID = input message: 'User input required', ok: 'continue', parameters: [text(defualtValue: 'jenkins job num', description: '输入jenkins job number id: 如 88', name: '输入该项目打包要发生产的job id')]   
                        sh "echo REPO_JOB_ID -----------> ${env.REPO_JOB_ID}"
                        def je = new org.devops.jenkinsEcr()
                        repo_tag = je.GetRepoJenkinsJobId()
                        sh "echo repo_tag -----------> ${repo_tag}"
                        if (repo_tag == 'false'){
                            sh "echo ${env.GIT_REPO_NAME}不存在${env.REPO_JOB_ID}这个job id，请重新运行并输入正确的job id"
                            sh "exit 1"
                        }
                    }
                }
            }
            stage('发布到生产') {
                steps{
                    script{
                        env.IS_DEPLOY_PROD = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '是否继续发布到生产！！！')],submitter: 'vickey-admin,admin'
                        if (env.IS_DEPLOY_PROD == "发布"){
                            sh "echo 'deploy to prod now'"
                            def deploy = new org.devops.deploy()
                            deploy.FrontendDeployToProd()
                        }
                    }
                }
            }
        }
    }
}