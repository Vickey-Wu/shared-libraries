package org.devops

def SonarqubeScan(){
    withSonarQubeEnv('vickey-sonarqube') {
        sh 'mvn clean package sonar:sonar'
    }
}