def call() {
    pipeline {
        agent { 
            node {
                label "vickey-test-jnlp"
            }
        }
        options {
            skipDefaultCheckout()  //删除隐式checkout scm语句
            timeout(time: 30, unit: 'MINUTES')
        }
        stages {
            stage('Prepare') {
                steps{
                    script{
                        checkout scm
                        def repositoryUrl = scm.userRemoteConfigs[0].url
                        def gitBranchName = scm.branches[0].name
                        //// '$'代表Jenkins里设置的变量和Jenkinsfile里设置的变量时，调用则直接使用'$VARIABLE_NAME'，如果是属于shell命令里面的'$'则需要写成'\$VARIABLE_NAME'，
                        //// 注意：shell里面调用Jenkins的变量也是不用直接使用'$VARIABLE_NAME'而不是'\$VARIABLE_NAME'
                        GIT_BRANCH_NAME = sh(returnStdout: true, script: "echo $gitBranchName |awk -F'/' '{print \$NF}'").trim()
                        GIT_REPO_NAME = sh(returnStdout: true, script: "echo $repositoryUrl |awk -F'/' '{print \$NF}'|awk -F'.' '{print \$1}'").trim()
                        IMAGE_TAG = sh(returnStdout: true, script: "date +%Y%m%d%H%M%S").trim()
                        //// 如果是master则IMAGE_NAME为GIT_REPO_NAME-prod：IMAGE_TAG
                        IMAGE_NAME = sh(returnStdout: true, script: "echo $AWS_ECR_URL/vickey/$GIT_REPO_NAME:${IMAGE_TAG}").trim()
                        sh "cp Dockerfile Dockerfile-staging"
                        sh "cp Dockerfile Dockerfile-prod"
                        sh "cp k8s-test.yaml k8s-staging.yaml"
                        sh "cp k8s-test.yaml k8s-prod.yaml"
                    }
                }
            }
            stage('parallel build') {
                parallel {
                    stage('sit') {
                        stages {
                            stage('Build and push') {
                                steps {
                                    script{
                                        sh "aws ecr get-login-password --region ap-east-1 | docker login --username AWS --password-stdin $AWS_ECR_URL"
                                        //// 如果是master则nacos变量 prod namespace
                                        sh "sed -i 's#FROM #FROM $AWS_ECR_URL/#' Dockerfile"
                                        sh "sed -i 's#ENV_NAME#test#' ./Dockerfile"
                                        sh "sed -i 's#RUN npm config set proxy http://proxyserver:proxyport##' Dockerfile"
                                        sh "cat Dockerfile"
                                        sh "docker build -f Dockerfile --network host -t $IMAGE_NAME ."
                                        sh "docker push $IMAGE_NAME"
                                        sh "docker rmi $IMAGE_NAME"
                                    }
                                }
                            }
                            stage('deploy to sit') {
                                steps {
                                    script{
                                        sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}#' k8s-test.yaml"
                                        sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}#' k8s-test.yaml"
                                        sh "cat k8s-test.yaml"
                                        sh "kubectl apply -f k8s-test.yaml -n default --record"
                                    }
                                }
                            }
                        }
                    }
                    stage('staging') {
                        stages {
                            stage('Build and push') {
                                steps {
                                    script{
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
                                }
                            }
                            stage('deploy to staging') {
                                steps {
                                    script{
                                        withKubeConfig(credentialsId: 'staging-configfile', serverUrl: 'https://vickeywu.staging.ap-east-1.eks.amazonaws.com') {
                                            sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}-staging#' k8s-staging.yaml"
                                            sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}-staging#' k8s-staging.yaml"
                                            sh "cat k8s-staging.yaml"
                                            sh "kubectl apply -f k8s-staging.yaml -n default --record"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    stage('prod') {
                        stages {
                            stage('Build and push') {
                                steps {
                                    script{
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
                                }
                            }
                            // stage('deploy to prod') {
                            //     steps {
                            //         script{
                            //             withKubeConfig(credentialsId: 'prod-configfile', serverUrl: 'https://vickeywu.prod.ap-east-1.eks.amazonaws.com') {
                            //                 sh "sed -i 's#IMAGE_NAME#${IMAGE_NAME}-prod#' k8s-prod.yaml"
                            //                 sh "sed -i 's#GIT_REPO_NAME#${GIT_REPO_NAME}-prod#' k8s-prod.yaml"
                            //                 sh "cat k8s-prod.yaml"
                            //                 sh "kubectl apply -f k8s-prod.yaml -n default --record"
                            //             }
                            //         }
                            //     }
                            // }
                        }
                    }
                }
            }
        }
    }
}