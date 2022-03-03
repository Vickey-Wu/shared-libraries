def call() {
    pipeline{
        agent { 
            node {
                // should be the same as slave label in jenkins plugin kubernetes config(node config->cloud)
                label "vickey-test-jnlp"
            }
        }
        options {
            skipDefaultCheckout()  //删除隐式checkout scm语句
            timeout(time: 15, unit: 'MINUTES')
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
                        gitlab.CheckoutNew()
                        sh "echo build with branch -----------> ${env.branchName}"
                    }
                }
            }
            stage('Build') {
                steps{
                    script{
                        sh "echo 'build now'"
                        def build = new org.devops.build()
                        build.MavenBuild()
                        sh "echo 'insert build vars into mongo'"
                        def je = new org.devops.jenkinsEcr()
                        je.InsertDb()
                        // //always skip deploy stage
                        // env.IS_DEPLOY = "no"
                    }
                }
            }
            stage('Deploy to test') {
                // when { equals expected: "yes", actual: env.IS_DEPLOY }
                when { 
                    anyOf {
                        environment name: 'branchName', value: 'sit'
                        // environment name: 'branchName', value: 'release'
                    }
                }
                steps{
                    timeout(time: 2, unit: 'MINUTES') {
                        script{
                            // env.IS_DEPLOY_SIT = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到sit环境，选择跳过表示跳过发布sit环境但可以继续选择后面的发布到staging的环境阶段，选择abort将终止整条流水线！')],submitter: 'vickey-admin,admin'
                            // if (env.IS_DEPLOY_SIT == "发布"){
                            //     sh "echo 'deploy to sit now'"
                            //     def deploy = new org.devops.deploy()
                            //     deploy.DeployToTest()
                            // }
                            // sh "echo 'deploy to sit now'"
                            def deploy = new org.devops.deploy()
                            deploy.DeployToTest()
                        }
                    }
                }
            }
            stage('Deploy to staging') {
                when { 
                    anyOf {
                        environment name: 'branchName', value: 'master'
                        environment name: 'branchName', value: 'release'
                    }
                }
                // when { equals expected: "yes", actual: env.IS_DEPLOY }
                steps{
                    script{
                        env.IS_DEPLOY_STAGING = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到staging环境，选择跳过表示跳过发布staging环境但可以继续选择后面的发布到prod的环境阶段，选择abort将终止整条流水线！')],submitter: 'vickey-admin,admin'
                        // env.IS_DEPLOY_STAGING = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到staging环境，选择跳过表示跳过发布staging环境但可以继续选择后面的发布到prod的环境阶段，选择abort将终止整条流水线！')],submitter: 'vickey-admin,admin'
                        if (env.IS_DEPLOY_STAGING == "发布"){
                            sh "echo 'deploy to staging now'"
                            def deploy = new org.devops.deploy()
                            deploy.DeployToStaging()
                        }

                        // if (env.branchName == "master"){
                        //     //是否继续发布到生产
                        //     env.IS_DEPLOY_PROD = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['yes','no'], description: '', name: '已发布到staging环境，请验证后再决定是否继续发布到生产！！！')],submitter: 'vickey-admin,admin'
                        //     // if yes continue deploy prod stage next
                        //     sh "echo ${env.IS_DEPLOY_PROD}"
                        // }
                    }
                }
            }
            stage('SonarQube analysis') {
                steps{
                    script{
                        def sonarqube = new org.devops.sonarqube()
                        sonarqube.SonarqubeScan()
                    }
                }
            }
            stage('Run test plan') {
                when { 
                    anyOf {
                        // environment name: 'branchName', value: 'master'
                        environment name: 'branchName', value: 'release'
                    }
                }
                steps{
                    timeout(time: 5, unit: 'MINUTES') {
                        script{
                            if (env.IS_DEPLOY_STAGING == "发布"){
                                // sh "echo 'wait for service start after 1min'"
                                // sh "sleep 50"
                                
                                //// wether execute test plan after deploy staging
                                def ms = new org.devops.metersphere()
                                testPlanId = ms.GetRepoMetersphereInfo()
                                sh "echo testPlanId -----------> ${testPlanId}"
                                if (testPlanId != 'false'){
                                    sh "echo '执行${env.GIT_REPO_NAME}的测试计划'"
                                    if ("${env.GIT_REPO_NAME}" != "vickey-web"){
                                        meterSphere environmentId: '',
                                        method: 'testPlan',
                                        mode: 'parallel',
                                        // mode: 'serial',
                                        msAccessKey: 'msAccessKey',
                                        msEndpoint: 'http://metersphere:port/',
                                        msSecretKey: 'msSecretKey',
                                        projectId: 'xxxxxxxx',
                                        // result: 'jenkins',
                                        result: 'MeterSphere',
                                        runEnvironmentId: 'xxxxxxxx',
                                        testCaseId: '',
                                        // search
                                        // testPlanId: "xxxxxxxxxxx",
                                        testPlanId: "${testPlanId}",
                                        workspaceId: 'xxxxxxxxxxxxx'
                                    }
                                }
                            }
                        }
                    }
                }
            }
            stage('Deploy to product') {
                // when { equals expected: "yes", actual: env.IS_DEPLOY_PROD }
                // when { equals expected: "yes", actual: env.IS_DEPLOY }
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
}