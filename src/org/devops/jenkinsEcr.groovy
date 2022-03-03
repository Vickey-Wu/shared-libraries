package org.devops
import groovy.json.JsonSlurper

def InsertDb(){
    //raw example: curl -d '{"project_name": "vickey-req", "job_id": "89", "ecr_tag": "2021-1017"}' -H 'Content-Type: application/json' http://jenkins-ecr-service.kube-ops:5000/api/v1/project/jenkins/jobs
    sh """
curl -d '{"project_name": "${GIT_REPO_NAME}", "nacos_service_name": "${NACOS_SERVICE_NAME}", "job_id": "${BUILD_ID}", "ecr_tag": "${IMAGE_TAG}"}' -H 'Content-Type: application/json' http://jenkins-ecr-service.kube-ops:5000/api/v1/project/jenkins/jobs
"""
}

def InsertDbFrontend(){
    sh """
curl -d '{"project_name": "${GIT_REPO_NAME}", "job_id": "${BUILD_ID}", "ecr_tag": "${IMAGE_TAG}"}' -H 'Content-Type: application/json' http://jenkins-ecr-service.kube-ops:5000/api/v1/project/jenkins/jobs
"""
}

def GetAllRepos(){
    // sh "curl http://jenkins-ecr-service.kube-ops:5000/api/v1/ecr/repostories/all"
    // env.ALL_REPOS = sh(returnStdout: true, script: "curl http://jenkins-ecr-service.kube-ops:5000/api/v1/ecr/repostories/all").trim()
    def all_repos = sh(returnStdout: true, script: "curl http://jenkins-ecr-service.kube-ops:5000/api/v1/ecr/repostories/all").trim()
    def list = new JsonSlurper().parseText(all_repos)
    // list.each { println it }
    return list
}

def GetRepoJenkinsJobId(){
    //split to get GIT_REPO_VERSION
    String repo_tag = sh(returnStdout: true, script: "curl http://jenkins-ecr-service.kube-ops:5000/api/v1/tag/${env.GIT_REPO_NAME}/${env.REPO_JOB_ID}").trim()
    // String repo_tag = '1.0-SNAPSHOT_2021-1028-171045'
    println(repo_tag);
    String[] tmp_str;
    tmp_str = repo_tag.split('_');
    // for( String values : tmp_str )
    // println(values);
    // println(tmp_str[0])
    env.GIT_REPO_VERSION = tmp_str[0]
    env.IMAGE_NAME = "vickeywu/${GIT_REPO_NAME}:${repo_tag}"
    println(IMAGE_NAME);
    return repo_tag
}

def GetRepoNacosInfo(){
    //get var NACOS_SERVICE_NAME
    String nacos_service_name = sh(returnStdout: true, script: "curl http://jenkins-ecr-service.kube-ops:5000/api/v1/nacos/${env.GIT_REPO_NAME}/${env.REPO_JOB_ID}").trim()
    println(nacos_service_name);
    return nacos_service_name
}