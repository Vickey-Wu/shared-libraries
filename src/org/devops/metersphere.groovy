package org.devops

def GetRepoMetersphereInfo(){
    //get var metersphere testPlanId
    String testPlanId = sh(returnStdout: true, script: "curl http://jenkins-ecr-service.kube-ops:5000/api/v1/metersphere/${env.GIT_REPO_NAME}").trim()
    println(testPlanId);
    return testPlanId
}