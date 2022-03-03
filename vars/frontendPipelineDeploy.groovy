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
            stage('发布到sit') {
                steps {
                    script{
                        env.IS_DEPLOY_STAGING = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到sit环境，选择跳过表示跳过发布sit环境但可以继续选择后面的发布到staging的环境阶段，选择abort将终止整条流水线！')]
                        if (env.IS_DEPLOY_STAGING == "发布"){
                            def deploy = new org.devops.deploy()
                            sh "echo '------------------ now deploy to sit-----------------'"
                            deploy.FrontendDeployToTest()
                        }
                    }
                }
            }
            stage('发布到staging') {
                steps {
                    script{
                        env.IS_DEPLOY_STAGING = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到staging环境，选择跳过表示跳过发布staging环境但可以继续选择后面的发布到prod的环境阶段，选择abort将终止整条流水线！')]
                        if (env.IS_DEPLOY_STAGING == "发布"){
                            sh "echo '------------------ now defploy to staging-----------------'"
                            def deploy = new org.devops.deploy()
                            deploy.FrontendDeployToStaging()
                        }
                    }
                }
            }
            stage('发布到prod') {
                steps{
                    script{
                        env.IS_DEPLOY_PROD = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '是否继续发布到生产！！！')],submitter: 'vickey-admin,admin'
                        if (env.IS_DEPLOY_PROD == "发布"){
                            def deploy = new org.devops.deploy()
                            sh "echo '------------------ now deploy to prod-----------------'"
                            deploy.FrontendDeployToProd()
                        }
                    }
                }
            }
        }
    }
}