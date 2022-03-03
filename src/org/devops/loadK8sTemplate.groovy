package org.devops

def BackendLoadSvcTemplate(){
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

def BackendLoadSvcTemplateNoPlugin(){
    fileData = readYaml text: """
apiVersion: v1
kind: Service
metadata:
  name: GIT_REPO_NAME
  labels:
    app: GIT_REPO_NAME
spec:
  selector:
    app: GIT_REPO_NAME
  type: ClusterIP
  ports:
  - name: http
    port: 8080
    targetPort: 8080
"""
    writeYaml file: "service.yaml", data: fileData
}

def BackendLoadDeploymentTemplate(){
    fileData = readYaml text: """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: GIT_REPO_NAME
spec:
  selector:
    matchLabels:
      app: GIT_REPO_NAME
  replicas: POD_NUM
  strategy:
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 1
    type: RollingUpdate
  template:
    metadata:
      labels:
        app: GIT_REPO_NAME
        version: GIT_REPO_VERSION
      annotations:
        prometheus.io.scrape/springboot: 'true'
        prometheus.io/path: '/actuator/prometheus'
        prometheus.io/port: '8081'
    spec:
      volumes:
        - name: skywalking-agent
          emptyDir: { }
      initContainers:
      - name: agent-container
        image: vickeywu/skywalking-java-agent:8.7.0-alpine
        volumeMounts:
          - name: skywalking-agent
            mountPath: /agent
        command: [ "/bin/sh" ]
        args: [ "-c", "cp -R /skywalking/agent /agent/" ]
      containers:
      - image: IMAGE_NAME
        lifecycle:
          preStop:
            exec:
              command: ['/bin/sh','-c','HI=`hostname -i`; curl -X PUT "NACOS_SERVER/nacos/v1/ns/instance?namespaceId=NACOS_NAMESPACE&groupName=DEFAULT_GROUP&ip=\${HI}&port=8080&serviceName=NACOS_SERVICE_NAME&clusterName=DEFAULT&enabled=false"; sleep 35']
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
        # resources:
        #   requests:
        #     memory: "800Mi"
        #     cpu: "200m"
        #   limits:
        #     memory: "2000Mi"
        #     cpu: "500m"
      ## default 30s
      terminationGracePeriodSeconds: 40
"""
    writeYaml file: "deployment.yaml", data: fileData
}

def BackendLoadDeploymentTemplateNoPlugin(){
    fileData = readYaml text: """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: GIT_REPO_NAME
spec:
  selector:
    matchLabels:
      app: GIT_REPO_NAME
  replicas: POD_NUM
  template:
    metadata:
      labels:
        app: GIT_REPO_NAME
        version: GIT_REPO_VERSION
    spec:
      containers:
      - image: IMAGE_NAME
        imagePullPolicy: IfNotPresent
        name: GIT_REPO_NAME
        ports:
        - containerPort: 8080
          name: project-port
        env:
          - name: ENV_NAME
            value: AWS-ENV-NAME
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
        # resources:
        #   requests:
        #     memory: "300Mi"
        #     cpu: "100m"
        #   limits:
        #     memory: "100Mi"
        #     cpu: "500m"
"""
    writeYaml file: "deployment.yaml", data: fileData
}

def FrontendLoadSvcTemplate(){
    fileData = readYaml text: """
apiVersion: v1
kind: Service
metadata:
  name: GIT_REPO_NAME
  labels:
    app: GIT_REPO_NAME
spec:
  selector:
    app: GIT_REPO_NAME
  type: ClusterIP
  ports:
  - name: http
    port: POD_PORT
    targetPort: POD_PORT
"""
    writeYaml file: "service.yaml", data: fileData
}

def FrontendLoadDeploymentTemplate(){
    fileData = readYaml text: """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: GIT_REPO_NAME
spec:
  selector:
    matchLabels:
      app: GIT_REPO_NAME
  replicas: POD_NUM
  template:
    metadata:
      labels:
        app: GIT_REPO_NAME
        version: GIT_REPO_VERSION
    spec:
      containers:
      - image: IMAGE_NAME
        imagePullPolicy: IfNotPresent
        name: GIT_REPO_NAME
"""
    writeYaml file: "deployment.yaml", data: fileData
}