package org.devops

def Checkout(){
    checkout scm
    def repositoryUrl = scm.userRemoteConfigs[0].url
    def gitBranchName = scm.branches[0].name
    //// '$'代表Jenkins里设置的变量和Jenkinsfile里设置的变量时，调用则直接使用'$VARIABLE_NAME'，如果是属于shell命令里面的'$'则需要写成'\$VARIABLE_NAME'，
    //// 注意：shell里面调用Jenkins的变量也是不能直接使用'$VARIABLE_NAME'而不是'\$VARIABLE_NAME'
    // env.CONTEXT_PATH = sh(returnStdout: true, script: "grep 'context-path' ./src/main/resources/bootstrap.yml | awk -F':' '{print \$2}'|awk -F' ' '{print \$1}'").trim()
    env.NACOS_SERVICE_NAME = sh(returnStdout: true, script: "grep -w 'name:' ./src/main/resources/bootstrap.yml | grep -v '\\\$' | awk -F':' '{print \$2}'|awk -F' ' '{print \$1}'").trim()
    sh(returnStdout: true, script: "echo NACOS_SERVICE_NAME: ${env.NACOS_SERVICE_NAME}").trim()
    env.GIT_BRANCH_NAME = sh(returnStdout: true, script: "echo $gitBranchName |awk -F'/' '{print \$NF}'").trim()
    env.GIT_REPO_NAME = sh(returnStdout: true, script: "echo $repositoryUrl |awk -F'/' '{print \$NF}'|awk -F'.' '{print \$1}'").trim()
    env.TAG_TIMESTAMP = sh(returnStdout: true, script: "date +%Y%m%d%H%M%S").trim()
    env.GIT_REPO_VERSION = sh(returnStdout: true, script: "head pom.xml|grep '<version>.*</version>' |awk -F'[<>]' '{print \$3}'").trim()
    env.IMAGE_TAG = sh(returnStdout: true, script: "echo ${GIT_REPO_VERSION}_${TAG_TIMESTAMP}").trim()
    env.IMAGE_NAME = sh(returnStdout: true, script: "echo $AWS_ECR_URL/vickey/$GIT_REPO_NAME:${IMAGE_TAG}").trim()
}

def CheckoutNew(){
    //env.BRANCH_NAME = input message: 'User input required', ok: 'Release!', parameters: [choice(choices: ['sit', 'release', 'master'], description: '', name: '选择你git分支')]   
    def repositoryUrl = scm.userRemoteConfigs[0].url
    def gitBranchName = scm.branches[0].name
    //sh "echo ${repositoryUrl}"
    checkout([$class: 'GitSCM', branches: [[name: "${env.branchName}"]], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: 'vickey-jenkins', url: "${repositoryUrl}"]]])

    // env.CONTEXT_PATH = sh(returnStdout: true, script: "grep 'context-path' ./src/main/resources/bootstrap.yml | awk -F':' '{print \$2}'|awk -F' ' '{print \$1}'").trim()
    env.NACOS_SERVICE_NAME = sh(returnStdout: true, script: "grep -w 'name:' ./src/main/resources/bootstrap.yml | grep -v '\\\$'  | awk -F':' '{print \$2}'|awk -F' ' '{print \$1}'").trim()
    sh(returnStdout: true, script: "echo NACOS_SERVICE_NAME: ${env.NACOS_SERVICE_NAME}").trim()
    env.GIT_BRANCH_NAME = sh(returnStdout: true, script: "echo ${env.branchName}").trim()
    env.GIT_REPO_NAME = sh(returnStdout: true, script: "echo $repositoryUrl |awk -F'/' '{print \$NF}'|awk -F'.' '{print \$1}'").trim()
    env.TAG_TIMESTAMP = sh(returnStdout: true, script: "date +%Y%m%d%H%M%S").trim()
    env.GIT_REPO_VERSION = sh(returnStdout: true, script: "head pom.xml|grep '<version>.*</version>' |awk -F'[<>]' '{print \$3}'").trim()
    env.IMAGE_TAG = sh(returnStdout: true, script: "echo ${GIT_REPO_VERSION}_${TAG_TIMESTAMP}").trim()
    env.IMAGE_NAME = sh(returnStdout: true, script: "echo $AWS_ECR_URL/vickey/$GIT_REPO_NAME:${IMAGE_TAG}").trim()
}


def FrontendCheckout(){
    //env.BRANCH_NAME = input message: 'User input required', ok: 'Release!', parameters: [choice(choices: ['sit', 'release', 'master'], description: '', name: '选择你git分支')]   
    def repositoryUrl = scm.userRemoteConfigs[0].url
    def gitBranchName = scm.branches[0].name
    //sh "echo ${repositoryUrl}"
    checkout([$class: 'GitSCM', branches: [[name: "${env.branchName}"]], 
            doGenerateSubmoduleConfigurations: false, 
            extensions: [], 
            submoduleCfg: [], 
            userRemoteConfigs: [[credentialsId: 'vickey-jenkins', url: "${repositoryUrl}"]]])

    env.GIT_BRANCH_NAME = sh(returnStdout: true, script: "echo ${env.branchName}").trim()
    env.GIT_REPO_NAME = sh(returnStdout: true, script: "echo $repositoryUrl |awk -F'/' '{print \$NF}'|awk -F'.' '{print \$1}'").trim()
    env.IMAGE_TAG = sh(returnStdout: true, script: "date +%Y%m%d%H%M%S").trim()
    //// 如果是master则IMAGE_NAME为GIT_REPO_NAME-prod：IMAGE_TAG
    env.IMAGE_NAME = sh(returnStdout: true, script: "echo $AWS_ECR_URL/vickey/$GIT_REPO_NAME:${IMAGE_TAG}").trim()
}


def CheckoutOnly(){
    checkout scm
}