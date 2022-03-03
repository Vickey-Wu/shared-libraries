package org.devops

def DeployToTest(){
    if("${GIT_BRANCH_NAME}" == "sit"){
        def template = new org.devops.loadK8sTemplate()
        template.BackendLoadSvcTemplateNoPlugin()
        template.BackendLoadDeploymentTemplateNoPlugin()
    }else{
        def template = new org.devops.loadK8sTemplate()
        template.BackendLoadSvcTemplate()
        template.BackendLoadDeploymentTemplate()
    }
    sh "cat service.yaml"
    sh "cat deployment.yaml"
    // sh "sed -i 's#/actuator/prometheus#${CONTEXT_PATH}/actuator/prometheus#' service.yaml"
    // sh "sed -i 's#/actuator/prometheus#${CONTEXT_PATH}/actuator/prometheus#' deployment.yaml"
    sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' service.yaml"
    sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' deployment.yaml"
    sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}#' deployment.yaml"
    sh "sed -i 's#GIT_REPO_VERSION#${GIT_REPO_VERSION}#' deployment.yaml"
    sh "sed -i 's#POD_NUM#1#' deployment.yaml"
    sh "cat service.yaml"
    sh "cat deployment.yaml"
    sh "kubectl apply -f service.yaml -n default --record"
    sh "kubectl apply -f deployment.yaml -n default --record"
    // must clean temp yaml files otherwise the next step would be failed
    sh "rm service.yaml deployment.yaml"
}
def DeployToStaging(){
    def template = new org.devops.loadK8sTemplate()
    template.BackendLoadSvcTemplate()
    template.BackendLoadDeploymentTemplate()
    withKubeConfig(credentialsId: 'staging-configfile', serverUrl: 'https://vickeywu.staging.ap-east-1.eks.amazonaws.com') {
        // sh "cat service.yaml"
        // sh "cat deployment.yaml"
        // sh "sed -i 's#/actuator/prometheus#${CONTEXT_PATH}/actuator/prometheus#' service.yaml"
        // sh "sed -i 's#/actuator/prometheus#${CONTEXT_PATH}/actuator/prometheus#' deployment.yaml"
        // --- replace k8s prestop nacos api vars --- start
        sh "sed -i 's#NACOS_SERVER#${NACOS_STAGING_SERVER}#' deployment.yaml"
        sh "sed -i 's#NACOS_SERVICE_NAME#${NACOS_SERVICE_NAME}#' deployment.yaml"
        sh "sed -i 's#NACOS_NAMESPACE#release#' deployment.yaml"
        // --- replace k8s prestop nacos api vars --- end
        // --- replace k8s template usual vars --- start
        sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' service.yaml"
        sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' deployment.yaml"
        sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}#' deployment.yaml"
        sh "sed -i 's#AWS-ENV-NAME#staging#' deployment.yaml"
        sh "sed -i 's#POD_NUM#1#' deployment.yaml"
        // --- replace k8s template usual vars --- end
        sh "cat service.yaml"
        sh "cat deployment.yaml"
        sh "kubectl apply -f service.yaml -n default --record"
        sh "kubectl apply -f deployment.yaml -n default --record"
        // must clean temp yaml files otherwise the next step would be failed
        sh "rm service.yaml deployment.yaml"
    }
}
def DeployToProduct(){
    def template = new org.devops.loadK8sTemplate()
    template.BackendLoadSvcTemplate()
    template.BackendLoadDeploymentTemplate()
    withKubeConfig(credentialsId: 'prod-configfile', serverUrl: 'https://vickeywu.prod.ap-east-1.eks.amazonaws.com') {
        // input id: 'Deploy_to_prod', message: '是否发布到生产？！', ok: '是！', parameters: [choice(choices: ['yes', 'no'], description: '', name: '发布到生产')], submitter: 'vickey-admin,admin'
        // sh "cat service.yaml"
        // sh "cat deployment.yaml"
        // sh "sed -i 's#/actuator/prometheus#${CONTEXT_PATH}/actuator/prometheus#' service.yaml"
        // sh "sed -i 's#/actuator/prometheus#${CONTEXT_PATH}/actuator/prometheus#' deployment.yaml"
        // --- replace k8s prestop nacos api vars --- start
        sh "sed -i 's#NACOS_SERVER#${NACOS_PROD_SERVER}#' deployment.yaml"
        sh "sed -i 's#NACOS_SERVICE_NAME#${NACOS_SERVICE_NAME}#' deployment.yaml"
        sh "sed -i 's#NACOS_NAMESPACE#prod#' deployment.yaml"
        // --- replace k8s prestop nacos api vars --- end
        // --- replace k8s template usual vars --- start
        sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' service.yaml"
        sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' deployment.yaml"
        sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}#' deployment.yaml"
        sh "sed -i 's#AWS-ENV-NAME#prod#' deployment.yaml"
        // --- replace k8s template usual vars --- end
        if ("${GIT_REPO_NAME}" == 'vickey-h5' || "${GIT_REPO_NAME}" == 'vickey-contract'){
            sh "sed -i 's#POD_NUM#3#' deployment.yaml"
        }else{
            sh "sed -i 's#POD_NUM#2#' deployment.yaml"
        }
        sh "cat service.yaml"
        sh "cat deployment.yaml"
        sh "kubectl apply -f service.yaml -n default --record"
        sh "kubectl apply -f deployment.yaml -n default --record"
        // //must clean temp yaml files otherwise the next step would be failed
        sh "rm service.yaml deployment.yaml"
    }
}


def FrontendDeployToTest(){
    def template = new org.devops.loadK8sTemplate()
    template.FrontendLoadSvcTemplate()
    template.FrontendLoadDeploymentTemplate()
    sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}#' deployment.yaml"
    sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' service.yaml"
    sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' deployment.yaml"
    sh "sed -i 's#POD_NUM#1#' deployment.yaml"
    if ( "${GIT_REPO_NAME}" == 'vickey-admin' || "${GIT_REPO_NAME}" == 'vickey-supplier'){
        sh "sed -i 's#POD_PORT#80#' service.yaml"
    }else{
        sh "sed -i 's#POD_PORT#3000#' service.yaml"
    }
    sh "cat service.yaml"
    sh "cat deployment.yaml"
    sh "kubectl apply -f service.yaml -n default --record"
    sh "kubectl apply -f deployment.yaml -n default --record"
    // //must clean temp yaml files otherwise the next step would be failed
    sh "rm service.yaml deployment.yaml"
}

def FrontendDeployToStaging(){
    def template = new org.devops.loadK8sTemplate()
    template.FrontendLoadSvcTemplate()
    template.FrontendLoadDeploymentTemplate()
    withKubeConfig(credentialsId: 'staging-configfile', serverUrl: 'https://vickeywu.staging.ap-east-1.eks.amazonaws.com') {
        sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}-staging#' deployment.yaml"
        sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}-staging#' service.yaml"
        sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}-staging#' deployment.yaml"
        sh "sed -i 's#POD_NUM#1#' deployment.yaml"
        if ( "${GIT_REPO_NAME}" == 'vickey-admin' || "${GIT_REPO_NAME}" == 'vickey-supplier'){
            sh "sed -i 's#POD_PORT#80#' service.yaml"
        }else{
            sh "sed -i 's#POD_PORT#3000#' service.yaml"
        }
        sh "cat service.yaml"
        sh "cat deployment.yaml"
        sh "kubectl apply -f service.yaml -n default --record"
        sh "kubectl apply -f deployment.yaml -n default --record"
        // //must clean temp yaml files otherwise the next step would be failed
        sh "rm service.yaml deployment.yaml"
    }
}

def FrontendDeployToProd(){
    def template = new org.devops.loadK8sTemplate()
    template.FrontendLoadSvcTemplate()
    template.FrontendLoadDeploymentTemplate()
    withKubeConfig(credentialsId: 'prod-configfile', serverUrl: 'https://vickeywu.prod.ap-east-1.eks.amazonaws.com') {
        sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}-prod#' deployment.yaml"
        sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}-prod#' service.yaml"
        sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}-prod#' deployment.yaml"
        if ( "${GIT_REPO_NAME}" == 'vickey-admin' || "${GIT_REPO_NAME}" == 'vickey-supplier'){
            sh "sed -i 's#POD_PORT#80#' service.yaml"
            sh "sed -i 's#POD_NUM#2#' deployment.yaml"
        }else{
            sh "sed -i 's#POD_PORT#3000#' service.yaml"
            sh "sed -i 's#POD_NUM#5#' deployment.yaml"
        }
        sh "cat service.yaml"
        sh "cat deployment.yaml"
        sh "kubectl apply -f service.yaml -n default --record"
        sh "kubectl apply -f deployment.yaml -n default --record"
        // //must clean temp yaml files otherwise the next step would be failed
        sh "rm service.yaml deployment.yaml"
    }
}