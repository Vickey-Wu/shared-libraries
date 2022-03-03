package org.devops

def MavenBuild(){
    sh "cp ./src/main/resources/bootstrap.yml ./src/main/resources/bootstrap-prod.yml"
    withCredentials([usernamePassword(credentialsId: 'nacosProd', passwordVariable: 'nacosProdPassword', usernameVariable: 'nacosProdUser')]) {
      sh "sed -i 's#default\$#prod#g' ./src/main/resources/bootstrap-prod.yml"
      sh "sed -i 's#NACOS_PROD_SERVER#${NACOS_PROD_SERVER}#g' ./src/main/resources/bootstrap-prod.yml"
      sh "sed -i 's#nacosProdUser\$#${nacosProdUser}#g' ./src/main/resources/bootstrap-prod.yml"
      sh "sed -i 's#nacosProdPassword#${nacosProdPassword}#g' ./src/main/resources/bootstrap-prod.yml"
      //sh "cat ./src/main/resources/bootstrap-prod.yml"
    }
    sh "cp ./src/main/resources/bootstrap.yml ./src/main/resources/bootstrap-staging.yml"
    withCredentials([usernamePassword(credentialsId: 'nacosStaging', passwordVariable: 'nacosStagingPassword', usernameVariable: 'nacosStagingUser')]) {
      sh "sed -i 's#default\$#release#g' ./src/main/resources/bootstrap-staging.yml"
      sh "sed -i 's#NACOS_PROD_SERVER#${NACOS_STAGING_SERVER}#g' ./src/main/resources/bootstrap-staging.yml"
      sh "sed -i 's#nacosProdUser\$#${nacosStagingUser}#g' ./src/main/resources/bootstrap-staging.yml"
      sh "sed -i 's#nacosProdPassword#${nacosStagingPassword}#g' ./src/main/resources/bootstrap-staging.yml"
    }
    withCredentials([usernamePassword(credentialsId: 'nacosTest', passwordVariable: 'nacosTestPassword', usernameVariable: 'nacosTestUser')]) {
      sh "aws ecr get-login-password --region ap-east-1 | docker login --username AWS --password-stdin $AWS_ECR_URL"
      //// 如果是master则nacos变量 prod namespace
      sh "sed -i 's#default\$#sit#g' ./src/main/resources/bootstrap.yml"
      sh "sed -i 's#NACOS_PROD_SERVER#${NACOS_TEST_SERVER}#g' ./src/main/resources/bootstrap.yml"
      sh "sed -i 's#nacosProdUser\$#${nacosTestUser}#g' ./src/main/resources/bootstrap.yml"
      sh "sed -i 's#nacosProdPassword#${nacosTestPassword}#g' ./src/main/resources/bootstrap.yml"
      sh "mvn clean install -U"
      sh "sed -i 's#FROM #FROM $AWS_ECR_URL/#' Dockerfile"
      sh "sed -i 's#CI_PROJECT_NAME#$GIT_REPO_NAME#' ./Dockerfile"
      sh "sed -i 's#1.0-SNAPSHOT#$GIT_REPO_VERSION#' ./Dockerfile"
      if ( "${GIT_REPO_NAME}" == 'vickey-service-websocket'){
        sh "sed -i 's#EXPOSE 8080#EXPOSE 8080 8081 8082#' ./Dockerfile"
      }else{
        sh "sed -i 's#EXPOSE 8080#EXPOSE 8080 8081#' ./Dockerfile"
      }
      // sh "docker rmi vickeywu/openjdk:8-jdk-alpine-shtimezone"
      sh "cat Dockerfile"
      sh "docker build -t $IMAGE_NAME ."
      sh "docker push $IMAGE_NAME"
      sh "docker rmi $IMAGE_NAME"
    }
}

def MavenBuildProd(){
    withCredentials([usernamePassword(credentialsId: 'nacosProd', passwordVariable: 'nacosProdPassword', usernameVariable: 'nacosProdUser')]) {
      sh "cp ./src/main/resources/bootstrap.yml ./src/main/resources/bootstrap-prod.yml"
      sh "sed -i 's#default\$#prod#g' ./src/main/resources/bootstrap-prod.yml"
      sh "sed -i 's#NACOS_PROD_SERVER#${NACOS_PROD_SERVER}#g' ./src/main/resources/bootstrap-prod.yml"
      sh "sed -i 's#nacosProdUser\$#${nacosProdUser}#g' ./src/main/resources/bootstrap-prod.yml"
      sh "sed -i 's#nacosProdPassword#${nacosProdPassword}#g' ./src/main/resources/bootstrap-prod.yml"
      sh "mvn clean install -U"
      sh "sed -i 's#FROM #FROM $AWS_ECR_URL/#' Dockerfile"
      sh "sed -i 's#CI_PROJECT_NAME#$GIT_REPO_NAME#' ./Dockerfile"
      sh "sed -i 's#1.0-SNAPSHOT#$GIT_REPO_VERSION#' ./Dockerfile"
      sh "cat Dockerfile"
      sh "docker build -t $IMAGE_NAME ."
      sh "docker push $IMAGE_NAME"
      sh "docker rmi $IMAGE_NAME"
    }
}
def MavenBuildStaging(){
    withCredentials([usernamePassword(credentialsId: 'nacosStaging', passwordVariable: 'nacosStagingPassword', usernameVariable: 'nacosStagingUser')]) {
      sh "cp ./src/main/resources/bootstrap.yml ./src/main/resources/bootstrap-staging.yml"
      sh "sed -i 's#default\$#release#g' ./src/main/resources/bootstrap-staging.yml"
      sh "sed -i 's#NACOS_PROD_SERVER#${NACOS_STAGING_SERVER}#g' ./src/main/resources/bootstrap-staging.yml"
      sh "sed -i 's#nacosProdUser\$#${nacosStagingUser}#g' ./src/main/resources/bootstrap-staging.yml"
      sh "sed -i 's#nacosProdPassword#${nacosStagingPassword}#g' ./src/main/resources/bootstrap-staging.yml"
      sh "mvn clean install -U"
      sh "sed -i 's#FROM #FROM $AWS_ECR_URL/#' Dockerfile"
      sh "sed -i 's#CI_PROJECT_NAME#$GIT_REPO_NAME#' ./Dockerfile"
      sh "sed -i 's#1.0-SNAPSHOT#$GIT_REPO_VERSION#' ./Dockerfile"
      sh "cat Dockerfile"
      sh "docker build -t $IMAGE_NAME ."
      sh "docker push $IMAGE_NAME"
      sh "docker rmi $IMAGE_NAME"
    }
}
def MavenBuildTest(){
    withCredentials([usernamePassword(credentialsId: 'nacosTest', passwordVariable: 'nacosTestPassword', usernameVariable: 'nacosTestUser')]) {
      sh "aws ecr get-login-password --region ap-east-1 | docker login --username AWS --password-stdin $AWS_ECR_URL"
      //// 如果是master则nacos变量 prod namespace
      sh "sed -i 's#default\$#sit#g' ./src/main/resources/bootstrap.yml"
      sh "sed -i 's#NACOS_PROD_SERVER#${NACOS_TEST_SERVER}#g' ./src/main/resources/bootstrap.yml"
      sh "sed -i 's#nacosProdUser\$#${nacosTestUser}#g' ./src/main/resources/bootstrap.yml"
      sh "sed -i 's#nacosProdPassword#${nacosTestPassword}#g' ./src/main/resources/bootstrap.yml"
      sh "mvn clean install -U"
      sh "sed -i 's#FROM #FROM $AWS_ECR_URL/#' Dockerfile"
      sh "sed -i 's#CI_PROJECT_NAME#$GIT_REPO_NAME#' ./Dockerfile"
      sh "sed -i 's#1.0-SNAPSHOT#$GIT_REPO_VERSION#' ./Dockerfile"
      sh "cat Dockerfile"
      sh "docker build -t $IMAGE_NAME ."
      sh "docker push $IMAGE_NAME"
      sh "docker rmi $IMAGE_NAME"
    }
}

def FrontendBuildTest(){
  sh "aws ecr get-login-password --region ap-east-1 | docker login --username AWS --password-stdin $AWS_ECR_URL"
  //// 如果是master则nacos变量 prod namespace
  sh "cp Dockerfile Dockerfile-test"
  // sh "cp k8s-test.yaml k8s-test.yaml"
  sh "sed -i 's#FROM #FROM $AWS_ECR_URL/#' Dockerfile-test"
  sh "sed -i 's#ENV_NAME#test#' ./Dockerfile-test"
  sh "sed -i 's#RUN npm config set proxy http://proxyserver:proxyport##' Dockerfile-test"
  sh "cat Dockerfile-test"
  sh "docker build -f Dockerfile-test --network host -t $IMAGE_NAME ."
  sh "docker push $IMAGE_NAME"
  sh "docker rmi $IMAGE_NAME"
}

def FrontendBuildStaging(){
  sh "cp Dockerfile Dockerfile-staging"
  // sh "cp k8s-test.yaml k8s-staging.yaml"
  sh "aws ecr get-login-password --region ap-east-1 | docker login --username AWS --password-stdin $AWS_ECR_URL"
  //// 如果是master则nacos变量 prod namespace
  sh "sed -i 's#FROM #FROM $AWS_ECR_URL/#' Dockerfile-staging"
  sh "sed -i 's#ENV_NAME#uat#' Dockerfile-staging"
  sh "sed -i 's#RUN npm config set proxy http://proxyserver:proxyport##' Dockerfile-staging"
  sh "cat Dockerfile-staging"
  sh "docker build -f Dockerfile-staging --network host -t ${IMAGE_NAME}-staging ."
  sh "docker push ${IMAGE_NAME}-staging"
  sh "docker rmi ${IMAGE_NAME}-staging"
}

def FrontendBuildProd(){
  sh "cp Dockerfile Dockerfile-prod"
  // sh "cp k8s-test.yaml k8s-prod.yaml"
  sh "aws ecr get-login-password --region ap-east-1 | docker login --username AWS --password-stdin $AWS_ECR_URL"
  //// 如果是master则nacos变量 prod namespace
  sh "sed -i 's#FROM #FROM $AWS_ECR_URL/#' Dockerfile-prod"
  sh "sed -i 's#ENV_NAME#production#' Dockerfile-prod"
  sh "sed -i 's#RUN npm config set proxy http://proxyserver:proxyport##' Dockerfile-prod"
  sh "cat Dockerfile-prod"
  sh "docker build -f Dockerfile-prod --network host -t ${IMAGE_NAME}-prod ."
  sh "docker push ${IMAGE_NAME}-prod"
  sh "docker rmi ${IMAGE_NAME}-prod"
}