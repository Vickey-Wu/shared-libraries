def call() {
    pipeline {
        agent { 
            node {
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
                                choice(defaultValue: 'hotfix', name: 'branchName',choices: ['hotfix','master'], description: '选择你要打包的git分支'),
                            ])
                        ])
                        def repositoryUrl = scm.userRemoteConfigs[0].url
                        checkout([$class: 'GitSCM', branches: [[name: "${env.branchName}"]], 
                                doGenerateSubmoduleConfigurations: false, 
                                extensions: [], 
                                submoduleCfg: [], 
                                userRemoteConfigs: [[credentialsId: 'vickey-jenkins', url: "${repositoryUrl}"]]])

                        // env.CONTEXT_PATH = sh(returnStdout: true, script: "grep 'context-path' ./src/main/resources/bootstrap.yml | awk -F':' '{print \$2}'|awk -F' ' '{print \$1}'").trim()
                        env.GIT_BRANCH_NAME = sh(returnStdout: true, script: "echo ${env.branchName}").trim()


                        env.GIT_REPO_NAME = input message: 'User input required', ok: 'continue', parameters: [choice(choices: ['vickey-admin', 'vickey-search','vickey-search','vickey-index','vickey-search','vickey-sync'], description: '', name: '选择你要打包的项目')]   
                        if (env.GIT_REPO_NAME == "vickey-search" || env.GIT_REPO_NAME == "vickey-admin"){
                            env.CONFIG_FILE = 'bootstrap'
                            env.CONFIG_SUFFIX = 'yml'
                        } else {
                            env.CONFIG_FILE = 'application'
                            env.CONFIG_SUFFIX = 'properties'
                        }
                        // get nacos service name by different config file type
                        if (env.CONFIG_FILE == "bootstrap"){
                            env.NACOS_SERVICE_NAME = sh(returnStdout: true, script: "grep -w 'name:' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX} | grep -v '\\\$' | awk -F':' '{print \$2}'|awk -F' ' '{print \$1}'").trim()
                            sh "echo bootstrap: ${env.NACOS_SERVICE_NAME}"
                        }else{
                            env.NACOS_SERVICE_NAME = sh(returnStdout: true, script: "grep -w 'spring.application.name' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX} | grep -v '\\\$'  | awk -F'=' '{print \$2}'|awk -F' ' '{print \$1}'").trim()
                            sh "echo application: ${env.NACOS_SERVICE_NAME}"
                        }
                        env.TAG_TIMESTAMP = sh(returnStdout: true, script: "date +%Y%m%d%H%M%S").trim()
                        env.GIT_REPO_VERSION = sh(returnStdout: true, script: "head  ./$GIT_REPO_NAME/pom.xml|grep '<version>.*</version>' |awk -F'[<>]' '{print \$3}'").trim()
                        env.IMAGE_TAG = sh(returnStdout: true, script: "echo ${GIT_REPO_VERSION}_${TAG_TIMESTAMP}").trim()
                        env.IMAGE_NAME = sh(returnStdout: true, script: "echo $AWS_ECR_URL/vickey/$GIT_REPO_NAME:$IMAGE_TAG").trim()
                    }
                }
            }
            stage('Build stage') {
                steps{
                    script{
                        // if (env.GIT_REPO_NAME == "vickey-search" || env.GIT_REPO_NAME == "vickey-admin"){
                        //     sh "cp ./$GIT_REPO_NAME/src/main/resources/application.properties ./$GIT_REPO_NAME/src/main/resources/application-staging.properties"
                        //     sh "cp ./$GIT_REPO_NAME/src/main/resources/application.properties ./$GIT_REPO_NAME/src/main/resources/application-prod.properties"
                        // }
                        // cp config file for different env
                        sh "cp ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX} ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-staging.${CONFIG_SUFFIX}"
                        sh "cp ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX} ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-prod.${CONFIG_SUFFIX}"
                        // sh "cp k8s-test.yaml k8s-staging.yaml"
                        // sh "cp k8s-test.yaml k8s-prod.yaml"

                        // change config for prod env
                        withCredentials([usernamePassword(credentialsId: 'nacosProd', passwordVariable: 'nacosProdPassword', usernameVariable: 'nacosProdUser')]) {
                        sh "sed -i 's#nacos.namespace=.*#nacos.namespace=prod#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-prod.${CONFIG_SUFFIX}"
                        sh "sed -i 's#nacos.host=.*#nacos.host=${NACOS_PROD_SERVER_ISEARCH}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-prod.${CONFIG_SUFFIX}"
                        sh "sed -i 's#nacos.username=.*#nacos.username=${nacosProdUser}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-prod.${CONFIG_SUFFIX}"
                        sh "sed -i 's#nacos.password=.*#nacos.password=${nacosProdPassword}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-prod.${CONFIG_SUFFIX}"
                        sh "sed -i 's#namespace:.*#namespace: prod#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-prod.${CONFIG_SUFFIX}"
                        sh "sed -i 's#host:.*#host: ${NACOS_PROD_SERVER_ISEARCH}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-prod.${CONFIG_SUFFIX}"
                        sh "sed -i 's#username:.*#username: ${nacosProdUser}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-prod.${CONFIG_SUFFIX}"
                        sh "sed -i 's#password:.*#password: ${nacosProdPassword}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-prod.${CONFIG_SUFFIX}"
                        sh "cat ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-prod.${CONFIG_SUFFIX}"
                        }

                        // change config for staging env
                        withCredentials([usernamePassword(credentialsId: 'nacosStaging', passwordVariable: 'nacosStagingPassword', usernameVariable: 'nacosStagingUser')]) {
                        sh "sed -i 's#nacos.namespace=.*#nacos.namespace=release#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-staging.${CONFIG_SUFFIX}"
                        sh "sed -i 's#nacos.host=.*#nacos.host=${NACOS_STAGING_SERVER_ISEARCH}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-staging.${CONFIG_SUFFIX}"
                        sh "sed -i 's#nacos.username=.*#nacos.username=${nacosStagingUser}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-staging.${CONFIG_SUFFIX}"
                        sh "sed -i 's#nacos.password=.*#nacos.password=${nacosStagingPassword}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-staging.${CONFIG_SUFFIX}"
                        sh "sed -i 's#namespace:.*#namespace: release#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-staging.${CONFIG_SUFFIX}"
                        sh "sed -i 's#host:.*#host: ${NACOS_STAGING_SERVER_ISEARCH}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-staging.${CONFIG_SUFFIX}"
                        sh "sed -i 's#username:.*#username: ${nacosStagingUser}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-staging.${CONFIG_SUFFIX}"
                        sh "sed -i 's#password:.*#password: ${nacosStagingPassword}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-staging.${CONFIG_SUFFIX}"
                        sh "cat ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}-staging.${CONFIG_SUFFIX}"
                        }

                        // change config for sit env
                        withCredentials([usernamePassword(credentialsId: 'nacosTest', passwordVariable: 'nacosTestPassword', usernameVariable: 'nacosTestUser')]) {
                        sh "aws ecr get-login-password --region ap-east-1 | docker login --username AWS --password-stdin $AWS_ECR_URL"
                        sh "sed -i 's#nacos.namespace=.*#nacos.namespace=sit#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX}"
                        sh "sed -i 's#nacos.host=.*#nacos.host=${NACOS_TEST_SERVER}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX}"
                        sh "sed -i 's#nacos.username=.*#nacos.username=${nacosTestUser}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX}"
                        sh "sed -i 's#nacos.password=.*#nacos.password=${nacosTestPassword}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX}"
                        sh "sed -i 's#namespace:.*#namespace: sit#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX}"
                        sh "sed -i 's#host:.*#host: ${NACOS_TEST_SERVER}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX}"
                        sh "sed -i 's#username:.*#username: ${nacosTestUser}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX}"
                        sh "sed -i 's#password:.*#password: ${nacosTestPassword}#g' ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX}"
                        sh "cat ./$GIT_REPO_NAME/src/main/resources/${CONFIG_FILE}.${CONFIG_SUFFIX}"

                        // sh "mvn -pl $GIT_REPO_NAME,vickey-api,vickey-common package -amd"
                        sh "mvn clean package -pl $GIT_REPO_NAME -am"

                        sh "sed -i 's#FROM #FROM $AWS_ECR_URL/#' ./$GIT_REPO_NAME/Dockerfile"
                        sh "sed -i 's#CI_PROJECT_NAME#$GIT_REPO_NAME#' ./$GIT_REPO_NAME/Dockerfile"
                        sh "sed -i 's#1.0-SNAPSHOT#$GIT_REPO_VERSION#' ./$GIT_REPO_NAME/Dockerfile"
                        sh "cat ./$GIT_REPO_NAME/Dockerfile"

                        sh "docker build -t $IMAGE_NAME -f ./$GIT_REPO_NAME/Dockerfile ./$GIT_REPO_NAME"
                        sh "docker push $IMAGE_NAME"
                        sh "docker rmi $IMAGE_NAME"
                        sh "echo 'insert build vars into mongo'"
                        def je = new org.devops.jenkinsEcr()
                        je.InsertDb()
                        }
                    }
                }
            }
            stage('Deploy to sit') {
                steps{
                    timeout(time: 2, unit: 'MINUTES') {
                        script{
                            env.IS_DEPLOY_SIT = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到staging环境，选择跳过表示跳过发布staging环境但可以继续选择后面的发布到prod的环境阶段，选择abort将终止整条流水线！')]
                            // env.IS_DEPLOY_STAGING = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到staging环境，选择跳过表示跳过发布staging环境但可以继续选择后面的发布到prod的环境阶段，选择abort将终止整条流水线！')],submitter: 'vickey-admin,admin'
                            if (env.IS_DEPLOY_SIT == "发布"){
                                sh "cp k8s-test.yaml k8s-staging.yaml"
                                sh "cp k8s-test.yaml k8s-prod.yaml"
                                sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}#' k8s-test.yaml"
                                sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' k8s-test.yaml"
                                sh "sed -i 's#GIT_REPO_VERSION#${GIT_REPO_VERSION}#' k8s-test.yaml"
                                sh "sed -i 's#POD_NUM#2#' k8s-test.yaml"
                                sh "cat k8s-test.yaml"
                                sh "kubectl apply -f k8s-test.yaml -n default --record"
                            }
                        }
                    }
                }
            }
            stage('Deploy to staging') {
                steps{
                    timeout(time: 2, unit: 'MINUTES') {
                        script{
                            env.IS_DEPLOY_STAGING = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到staging环境，选择跳过表示跳过发布staging环境但可以继续选择后面的发布到prod的环境阶段，选择abort将终止整条流水线！')],submitter: 'vickey-admin,admin'
                            // env.IS_DEPLOY_STAGING = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '选择发布将发布到staging环境，选择跳过表示跳过发布staging环境但可以继续选择后面的发布到prod的环境阶段，选择abort将终止整条流水线！')],submitter: 'vickey-admin,admin'
                            if (env.IS_DEPLOY_STAGING == "发布"){
                                withKubeConfig(credentialsId: 'staging-configfile', serverUrl: 'https://vickeywu.staging.ap-east-1.eks.amazonaws.com') {
                                // --- replace k8s prestop nacos api vars --- start
                                sh "sed -i 's#NACOS_SERVER#${NACOS_STAGING_SERVER}#' k8s-staging.yaml"
                                sh "sed -i 's#NACOS_SERVICE_NAME#${NACOS_SERVICE_NAME}#' k8s-staging.yaml"
                                sh "sed -i 's#NACOS_NAMESPACE#release#' k8s-staging.yaml"
                                // --- replace k8s prestop nacos api vars --- end
                                // --- replace k8s template usual vars --- start
                                sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}#' k8s-staging.yaml"
                                sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' k8s-staging.yaml"
                                sh "sed -i 's#GIT_REPO_VERSION#${GIT_REPO_VERSION}#' k8s-staging.yaml"
                                sh "sed -i 's#POD_NUM#2#' k8s-staging.yaml"
                                sh "sed -i 's#AWS-ENV-NAME#staging#' k8s-staging.yaml"
                                // --- replace k8s template usual vars --- end
                                sh "cat k8s-staging.yaml"
                                sh "kubectl apply -f k8s-staging.yaml -n default --record"
                                }
                            }
                        }
                    }
                }                
            }
            stage('SonarQube analysis') {
                steps{
                    script{
                        withSonarQubeEnv('vickey-sonarqube') {
                        sh 'mvn clean package sonar:sonar'
                        }
                    }
                }

            }
            stage('Deploy to prod') {
                // when { 
                //     anyOf {
                //         environment name: 'branchName', value: 'master'
                //         // environment name: 'branchName', value: 'release'
                //     }
                // }
                steps{
                    timeout(time: 1, unit: 'MINUTES') {
                        script{
                            withKubeConfig(credentialsId: 'prod-configfile', serverUrl: 'https://vickeywu.prod.ap-east-1.eks.amazonaws.com') {
                                // --- replace k8s prestop nacos api vars --- start
                                sh "sed -i 's#NACOS_SERVER#${NACOS_PROD_SERVER}#' k8s-prod.yaml"
                                sh "sed -i 's#NACOS_SERVICE_NAME#${NACOS_SERVICE_NAME}#' k8s-prod.yaml"
                                sh "sed -i 's#NACOS_NAMESPACE#prod#' k8s-prod.yaml"
                                // --- replace k8s prestop nacos api vars --- end
                                // --- replace k8s template usual vars --- start
                                sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}#' k8s-prod.yaml"
                                sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' k8s-prod.yaml"
                                sh "sed -i 's#GIT_REPO_VERSION#${GIT_REPO_VERSION}#' k8s-prod.yaml"
                                sh "sed -i 's#AWS-ENV-NAME#prod#' k8s-prod.yaml"
                                sh "sed -i 's#POD_NUM#2#' k8s-prod.yaml"
                                // --- replace k8s template usual vars --- start
                                sh "cat k8s-prod.yaml"
                                env.IS_DEPLOY_PROD = input message: 'User input required', ok: 'continue!', parameters: [choice(choices: ['发布','跳过'], description: '', name: '是否继续发布到生产！！！')],submitter: 'vickey-admin,admin'
                                if (env.IS_DEPLOY_PROD == "发布"){
                                    sh "echo 'deploy to prod now'"
                                    sh "kubectl apply -f k8s-prod.yaml -n default --record"
                                } 
                            }
                        }
                    }
                }
            }
        }
    }
}