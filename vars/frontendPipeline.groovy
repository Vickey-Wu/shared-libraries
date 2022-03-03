def call() {
    pipeline {
        agent { 
            node {
                label "vickey-test-jnlp"
            }
        }
        options {
            skipDefaultCheckout()  //删除隐式checkout scm语句
            timeout(time: 18, unit: 'MINUTES')
        }
        stages {
            stage('choose branch to build') {
                steps{
                    script{
                        properties([
                            parameters([
                                choice(defaultValue: 'sit', name: 'branchName',choices: ['sit', 'release', 'master'], description: '选择你要打包的git分支'),
                            ])
                        ])
                        def gitlab = new org.devops.gitlab()
                        gitlab.FrontendCheckout()
                        sh "echo build with branch -----------> ${env.branchName}"
                    }
                }
            }
            stage('Build and push') {
                steps {
                    script{
                        def build = new org.devops.build()
                        if (env.branchName == "master"){
                            // build staging and prod
                            sh "echo '------------------ now build for prod-----------------'"
                            build.FrontendBuildProd()
                            sh "echo 'insert build info into mongo'"
                            def je = new org.devops.jenkinsEcr()
                            je.InsertDbFrontend()
                            sh "echo '------------------ now build for staging-----------------'"
                            build.FrontendBuildStaging()
                        }
                        if (env.branchName == "release"){
                            // build staging
                            sh "echo '------------------ now build for staging-----------------'"
                            build.FrontendBuildStaging()
                            def je = new org.devops.jenkinsEcr()
                            je.InsertDbFrontend()
                        }
                        if (env.branchName == "sit"){
                            // build test 
                            sh "echo '------------------ now build for sit-----------------'"
                            build.FrontendBuildTest()
                        }
                        // sh "echo 'insert build vars into mongo'"
                        def je = new org.devops.jenkinsEcr()
                        je.InsertDbFrontend()
                    }
                }
            }
            stage('deploy to sit') {
                // when { equals expected: "sit", actual: env.branchName }
                when { 
                    anyOf {
                        environment name: 'branchName', value: 'sit'
                        // environment name: 'branchName', value: 'release'
                    }
                }
                steps{
                    timeout(time: 2, unit: 'MINUTES') {
                        script{
                            def deploy = new org.devops.deploy()
                            sh "echo '------------------ now deploy to sit-----------------'"
                            deploy.FrontendDeployToTest()
                        }
                    }
                }
            }
            stage('deploy to staging') {
                when { 
                    anyOf {
                        environment name: 'branchName', value: 'release'
                        environment name: 'branchName', value: 'master'
                    }
                }
                steps{
                    timeout(time: 2, unit: 'MINUTES') {
                        script{
                            env.IS_DEPLOY_STAGING = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到staging环境，选择跳过表示跳过发布staging环境但可以继续选择后面的发布到prod的环境阶段，选择abort将终止整条流水线！')],submitter: 'vickey-admin,admin'
                            if (env.IS_DEPLOY_STAGING == "发布"){
                                sh "echo '------------------ now deploy to staging-----------------'"
                                def deploy = new org.devops.deploy()
                                deploy.FrontendDeployToStaging()
                            }
                            // env.IS_DEPLOY_PROD = 'no'
                            // if (env.branchName == "master"){
                            //     //是否继续发布到生产
                            //     env.IS_DEPLOY_PROD = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['yes','no'], description: '', name: 'master代码已发布到staging，请验证后!!! 再决定是否继续发布到prod？')],submitter: 'vickey-admin,admin'
                            //     // if yes continue deploy prod stage next
                            //     sh "echo ${env.IS_DEPLOY_PROD}"
                            // }
                        }
                    }
                }
            }
            stage('deploy to prod') {
                // when { equals expected: "yes", actual: env.IS_DEPLOY_PROD }
                when { 
                    anyOf {
                        environment name: 'branchName', value: 'master'
                        // environment name: 'branchName', value: 'release'
                    }
                }
                steps{
                    timeout(time: 2, unit: 'MINUTES') {
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
}