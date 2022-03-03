package org.devops

def BackendStages(){
    def build = new org.devops.build()
    def deploy = new org.devops.deploy()
    def sonarqube = new org.devops.sonarqube()
    build.MavenBuild()
    sonarqube.SonarqubeScan()
    deploy.DeployToTest()
    deploy.DeployToStaging()
    deploy.DeployToProduct()
}