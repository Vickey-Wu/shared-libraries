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

def MergeYamlFileFunc(){
    svc_main = """
apiVersion: v1
kind: Service
metadata:
  name: tmptest
  labels:
    app: tmptest
  annotations:
    boot.spring.io/actuator: http://:8081/actuator
    prometheus.io/scrape: 'true'
    prometheus.io.scrape/springboot: 'true'
    prometheus.io/path: /actuator/prometheus
    prometheus.io/port: '8081'
spec:
  selector:
    app: tmptest
  type: ClusterIP
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  - name: manageport
    port: 8081
    targetPort: 8081
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
            memory: "500Mi"
            cpu: "100m"
          limits:
            memory: "2000Mi"
            cpu: "2000m"
"""
    deployment_env = """
        env:
        - name: ENV_NAME
          value: staging
        - name: JAVA_TOOL_OPTIONS
          value: -javaagent:/skywalking/agent/skywalking-agent.jar
        - name: SW_AGENT_COLLECTOR_BACKEND_SERVICES
          value: skywalking-oap.kube-ops.svc:11800
        - name: SW_GRPC_LOG_SERVER_HOST
          value: skywalking-oap.kube-ops.svc
        - name: SW_AGENT_NAME
          value: tmptest
"""
    deployment_main = """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tmptest
spec:
  selector:
    matchLabels:
      app: tmptest
  replicas: 1
  template:
    metadata:
      labels:
        app: tmptest
        version: GIT_REPO_VERSION
      annotations:
        prometheus.io.scrape/springboot: 'true'
        prometheus.io/path: /actuator/prometheus
        prometheus.io/port: '8081'
    spec:
      volumes:
      - name: skywalking-agent
        emptyDir: {}
      initContainers:
      - name: agent-container
        image: vickeywu/skywalking-java-agent:8.7.0-alpine
        volumeMounts:
        - name: skywalking-agent
          mountPath: /agent
        command:
        - /bin/sh
        args:
        - -c
        - cp -R /skywalking/agent /agent/
      containers:
      - image: vickeywu/coupon:1.0-SNAPSHOT_20220303
        imagePullPolicy: IfNotPresent
        name: tmptest
        ports:
        - containerPort: 8080
          name: project-port
        - containerPort: 8081
          name: manageport
        volumeMounts:
        - name: skywalking-agent
          mountPath: /skywalking
"""
    // Showcasing what the above code does:
    // println "merge(config, configOverrides): " + merge(config, configOverrides)
    // => [config:[num_instances:3, instance_size:small]]
    // println "merge(config, config2, config3): " + merge(config, config2, config3)merge(config, config2, config3)
    // => [config:[instance_size:large, num_instances:3]]
    result = deployment_main.concat(deployment_resources).concat(deployment_env).concat(deployment_probe)
    // result = config6.concat(config5).concat(config4).concat(' ')
    println result
    // Write to file
    // writeYaml file: 'dev-config.yaml', data: merge(config6,result)
    // sh "cat dev-config.yaml"
}