#!/usr/bin/env groovy

@Library('github.com/ForsytheHostingSolutions/jenkins-pipeline-library@master') _

podTemplate(label: 'mypod', containers: [
    containerTemplate(
        name: 'maven', 
        image: 'maven:3.3.9-jdk-8-alpine', 
        envVars: [envVar(key: 'MAVEN_SETTINGS_PATH', value: '/root/.m2/settings.xml')], 
        ttyEnabled: true, 
        command: 'cat'),
    containerTemplate(image: 'docker', name: 'docker', command: 'cat', ttyEnabled: true),
    containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.0', command: 'cat', ttyEnabled: true),
  ], volumes: [
    secretVolume(mountPath: '/root/.m2/', secretName: 'jenkins-maven-settings'),
    secretVolume(mountPath: '/home/jenkins/.docker', secretName: 'regsecret'),
    hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
    persistentVolumeClaim(claimName: 'nfs', mountPath: '/root/.m2nrepo')
  ], imagePullSecrets: [ 'regsecret' ]) {

    node('mypod') {
        checkout scm
        def jobName = "${env.JOB_NAME}".tokenize('/').last()
        def projectNamespace = "${env.JOB_NAME}".tokenize('/')[0]

        def pullRequest = false
        if (jobName.startsWith("PR-")) {
            pullRequest = true
        }

        if (!pullRequest) {
            container('kubectl') {
                stage('Configure Kubernetes') {
                    createNamespace(projectNamespace)
                }
            }
        }

        container('maven') {
            stage('Build a project') {
                sh 'mvn clean install -DskipTests=true'
            }

            stage('Run tests') {
                try {
                    sh 'mvn clean install test'
                } finally {
                    junit 'target/surefire-reports/*.xml'
                }
            }

            stage('SonarQube Analysis') {
                sonarQubeScanner(){}
            }

            if (!pullRequest) {
                stage('Deploy project to Nexus') {
                    sh 'mvn -DskipTests=true package deploy'
                    archiveArtifacts artifacts: 'target/*.jar'
                }
            }
        }

        if (!pullRequest) {
            container('docker') {
                stage('Docker build') {
                    sh 'docker build -t greetings-service .'
                    sh 'docker tag greetings-service quay.io/zotovsa/greetings-service'
                    sh 'docker push quay.io/zotovsa/greetings-service'
                }
            }

            container('kubectl') {
                stage('Deploy MicroService') {
                   sh "kubectl delete deployment greetings-service -n ${projectNamespace} --ignore-not-found=true"
                   sh "kubectl delete service greetings-service -n ${projectNamespace} --ignore-not-found=true"
                   sh "kubectl create -f ./deployment/deployment.yml -n ${projectNamespace}"
                   sh "kubectl create -f ./deployment/service.yml -n ${projectNamespace}"
                   waitForRunningState(projectNamespace)
                }
            }

            container('kubectl') {
                timeout(time: 3, unit: 'MINUTES') {
                    printEndpoint(namespace: projectNamespace, serviceId: "greetings-service",
                        serviceName: "Greetings Service", port: "8080")
                    input message: "Deploy to Production?"
                }
            }

            container('kubectl') {
               sh "kubectl create namespace prod-${projectNamespace} || true"
               sh "kubectl delete deployment greetings-service -n prod-${projectNamespace} --ignore-not-found=true"
               sh "kubectl delete service greetings-service -n prod-${projectNamespace} --ignore-not-found=true"
               sh "kubectl create -f ./deployment/deployment.yml -n prod-${projectNamespace}"
               sh "kubectl create -f ./deployment/service.yml -n prod-${projectNamespace}"
               waitForRunningState("prod-${projectNamespace}")
               printEndpoint(namespace: "prod-${projectNamespace}", serviceId: "greetings-service",
                                   serviceName: "Greetings Service", port: "8080")
            }
        }
    }
}
