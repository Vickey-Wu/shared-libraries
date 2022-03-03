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
            stage('read json') {
                steps{
                    script{
                        def mm = new org.devops.mergeyaml()
                        mm.MergeYamlFileFunc()
                        // env.GIT_REPO_NAME = "vickey-web"
                        // env.IS_EXECUSE_TEST = "yes"
                        // def ms = new org.devops.metersphere()
                        // testPlanId = ms.GetRepoMetersphereInfo()
                        // sh "echo testPlanId -----------> ${testPlanId}"
                        // if (testPlanId != 'false'){
                        //     sh "echo '执行${env.GIT_REPO_NAME}的测试计划'"
                        //     if ("${env.GIT_REPO_NAME}" == "vickey-web"){
                        //         meterSphere environmentId: '',
                        //         method: 'testPlan',
                        //         mode: 'parallel',
                        //         // mode: 'serial',
                        //         msAccessKey: 'msAccessKey',
                        //         msEndpoint: 'http://metersphere:port/',
                        //         msSecretKey: 'msSecretKey',
                        //         projectId: 'xxxxxxxx',
                        //         // result: 'jenkins',
                        //         result: 'MeterSphere',
                        //         runEnvironmentId: 'xxxxxxxx',
                        //         testCaseId: '',
                        //         // search
                        //         // testPlanId: "xxxxxxxxxx",
                        //         testPlanId: "${testPlanId}",
                        //         workspaceId: 'xxxxxxxxxxx'
                        //     }
                        // }
                    }
                }
            }
        }
    }
}