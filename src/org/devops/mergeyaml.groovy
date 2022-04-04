package org.devops

// Map merge(Map... maps) {
//     Map result = [:]
//     maps.each { map ->
//         map.each { k, v ->
//             result[k] = result[k] instanceof Map ? merge(result[k], v) : v
//         }
//     }
//     result
// }

def MergeSvcYaml(){
    fileData = readYaml text: """
apiVersion: v1
kind: Service
metadata:
  name: GIT_REPO_NAME
  labels:
    app: GIT_REPO_NAME
  annotations:
    boot.spring.io/actuator: http://:8081/actuator
    prometheus.io/scrape: 'true'
    prometheus.io.scrape/springboot: 'true'
    prometheus.io/path: '/actuator/prometheus'
    prometheus.io/port: '8081'
spec:
  selector:
    app: GIT_REPO_NAME
  type: ClusterIP
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  - name: manageport
    port: 8081
    targetPort: 8081
"""
    writeYaml file: "service.yaml", data: fileData
}

def MergeDeploymentYaml(){
    deployment_main = """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: GIT_REPO_NAME
spec:
  selector:
    matchLabels:
      app: GIT_REPO_NAME
  replicas: POD_NUM
"""
    deployment_strategy = """
  strategy:
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 1
    type: RollingUpdate
"""
    deployment_tpl_meta = """
  template:
    metadata:
      labels:
        app: GIT_REPO_NAME
        version: GIT_REPO_VERSION
      annotations:
        prometheus.io.scrape/springboot: 'true'
        prometheus.io/path: '/actuator/prometheus'
        prometheus.io/port: '8081'
"""
    deployment_tpl_spec_init = """
    spec:
      volumes:
        - name: skywalking-agent
          emptyDir: { }
      initContainers:
      - name: agent-container
        image: vickeywu.ecr.ap-east-1.amazonaws.com/vickeywu/skywalking-java-agent:8.7.0-alpine
        volumeMounts:
          - name: skywalking-agent
            mountPath: /agent
        command: [ "/bin/sh" ]
        args: [ "-c", "cp -R /skywalking/agent /agent/" ]
"""
    deployment_tpl_spec_main = """
      terminationGracePeriodSeconds: 40
      containers:
      - image: IMAGE_NAME
        imagePullPolicy: IfNotPresent
        name: GIT_REPO_NAME
        ports:
        - containerPort: 8080
          name: project-port
        - containerPort: 8081
          name: manageport
        volumeMounts:
          - name: skywalking-agent
            mountPath: /skywalking
"""
    deployment_tpl_spec_main_life = """
        lifecycle:
          preStop:
            exec:
              command: ['/bin/sh','-c','HI=`hostname -i`; curl -X PUT "NACOS_SERVER/nacos/v1/ns/instance?namespaceId=NACOS_NAMESPACE&groupName=DEFAULT_GROUP&ip=\${HI}&port=8080&serviceName=NACOS_SERVICE_NAME&clusterName=DEFAULT&enabled=false"; sleep 35']
"""
    deployment_env = """
        env:
          - name: ENV_NAME
            value: AWS-ENV-NAME
          - name: JAVA_TOOL_OPTIONS
            value: "-javaagent:/skywalking/agent/skywalking-agent.jar"
          - name: SW_AGENT_COLLECTOR_BACKEND_SERVICES
            value: skywalking-oap.kube-ops.svc:11800
          - name: SW_GRPC_LOG_SERVER_HOST
            value: skywalking-oap.kube-ops.svc
          - name: SW_AGENT_NAME
            value: GIT_REPO_NAME
"""
    deployment_probe = """
        readinessProbe:
          tcpSocket:
            port: 8080
          initialDelaySeconds: 45
          periodSeconds: 6
          failureThreshold: 10
        livenessProbe:
          tcpSocket:
            port: 8080
          initialDelaySeconds: 50
          periodSeconds: 6
          failureThreshold: 10
"""
    deployment_resources = """
        resources:
          requests:
            memory: "700Mi"
            cpu: "100m"
          limits:
            memory: "2000Mi"
            cpu: "2000m"
"""
    // int[] array = [deployment_strategy, deployment_tpl_spec_init]; 
    // for(int i in array) {
    //   deployment_file_all = deployment_main.concat(i)
    //   println(deployment_file_all);
    // } 

    deployment_file_all = deployment_main.concat(deployment_strategy).concat(deployment_tpl_meta).concat(deployment_tpl_spec_init).concat(deployment_tpl_spec_main).concat(deployment_tpl_spec_main_life).concat(deployment_env).concat(deployment_probe).concat(deployment_resources)
    // readYaml writeYaml should be use at the same time
    fileData = readYaml text: deployment_file_all
    writeYaml file: "deployment.yaml", data: fileData
    sh "cat deployment.yaml"
    // Write to file
    // writeYaml file: 'dev-config.yaml', data: merge(config6,result)
    // sh "cat dev-config.yaml"
}
