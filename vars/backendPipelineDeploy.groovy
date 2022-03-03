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
            stage('选择你要发布的项目') {
                steps{
                    script{
                        env.GIT_REPO_NAME = input message: 'User input required', ok: 'continue', parameters: [choice(choices: ['vickey-web'], description: '', name: '选择你要发布的项目')]
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
                        env.NACOS_SERVICE_NAME = je.GetRepoNacosInfo()
                        sh "echo NACOS_SERVICE_NAME -----------> ${env.NACOS_SERVICE_NAME}"
                        if (repo_tag == 'false' && "${env.NACOS_SERVICE_NAME}" != ''){
                            sh "echo ${env.GIT_REPO_NAME}不存在${env.REPO_JOB_ID}这个job id，请重新运行并输入正确的job id"
                            sh "exit 1"
                        }
                    }
                }
            }
            // stage('选择要发布的环境') {
            //     steps{
            //         script{
            //             env.DEPLOY_ENV = input message: 'User input required', ok: 'continue', parameters: [choice(choices: ['sit', 'staging', 'prod'], description: '', name: '选择你要发布的环境')]
            //             if (env.DEPLOY_ENV == "sit"){
            //                 sh "echo 'deploy to sit now'"
            //                 def deploy = new org.devops.deploy()
            //                 deploy.DeployToTest()
            //             }
            //             if (env.DEPLOY_ENV == "staging"){
            //                 sh "echo 'deploy to staging now'"
            //                 def deploy = new org.devops.deploy()
            //                 deploy.DeployToStaging()
            //             }
            //             // env.IS_DEPLOY_PROD = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '是否继续发布到生产！！！')],submitter: 'vickey-admin,admin'
            //             // if (env.DEPLOY_ENV == "prod" && env.IS_DEPLOY_PROD == "发布"){
            //             //     sh "echo 'deploy to prod now'"
            //             //     def deploy = new org.devops.deploy()
            //             //     // deploy.DeployToProduct()
            //             // }                        
            //         }
            //     }
            // }
            
            // import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
            // stage('I am skipped') {
            //     Utils.markStageSkippedForConditional(STAGE_NAME)
            // }

            // stage('发布到sit环境') {
            //     steps{
            //         script{
            //             env.IS_DEPLOY_SIT = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到sit环境，选择跳过表示跳过发布sit环境但可以继续选择后面的发布到staging的环境阶段，选择abort将终止整条流水线！')]
            //             if (env.IS_DEPLOY_SIT == "发布"){
            //                 sh "echo 'deploy to sit now'"
            //                 def deploy = new org.devops.deploy()
            //                 deploy.DeployToTest()
            //             }
            //         }
            //     }
            // }
            // stage('发布到staging环境') {
            //     steps{
            //         script{
            //             env.IS_DEPLOY_STAGING = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到staging环境，选择跳过表示跳过发布staging环境但可以继续选择后面的发布到prod的环境阶段，选择abort将终止整条流水线！')],submitter: 'vickey-admin,admin'
            //             if (env.IS_DEPLOY_STAGING == "发布"){
            //                 sh "echo 'deploy to staging now'"
            //                 def deploy = new org.devops.deploy()
            //                 deploy.DeployToStaging()
            //             }
            //         }
            //     }
            // }
            stage('发布到prod环境') {
                steps{
                    script{
                        env.IS_DEPLOY_PROD = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '是否继续发布到生产！！！')],submitter: 'vickey-admin,admin'
                        if (env.IS_DEPLOY_PROD == "发布"){
                            sh "echo 'deploy to prod now'"
                            def deploy = new org.devops.deploy()
                            deploy.DeployToProduct()
                        }
                    }
                }
            }
        }
    }
}