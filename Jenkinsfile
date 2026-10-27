pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 30, unit: 'MINUTES')
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    environment {
        IMAGE_REPO = 'khinmyohan91/finsecure-pay-api'
        AWS_REGION = 'ap-southeast-1'
        EKS_CLUSTER = 'finsecure-pay-eks'
        KUBE_CONTEXT = 'arn:aws:eks:ap-southeast-1:989813815626:cluster/finsecure-pay-eks'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm

                script {
                    def commit = bat(
                        script: '@git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()

                    env.IMAGE_TAG = "build-${env.BUILD_NUMBER}-${commit}"
                }
            }
        }

        stage('Build') {
            steps {
                bat 'call mvnw.cmd -B clean compile'
            }
        }

        stage('Test') {
            steps {
                bat 'call mvnw.cmd -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/TEST-*.xml',
                          allowEmptyResults: false
                }
            }
        }

        stage('Package') {
            steps {
                bat 'call mvnw.cmd -B package -DskipTests'
            }
        }

        stage('Container Build') {
            steps {
                bat '''
                    @echo off
                    docker build --pull -t %IMAGE_REPO%:%IMAGE_TAG% .
                    if errorlevel 1 exit /b 1
                '''
            }
        }

               stage('Push') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'dockerhub-auth-config',
                        variable: 'DOCKER_AUTH_FILE'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = 'Stop'
                        $configDirectory = Join-Path $env:TEMP (
                            'docker-auth-' + [guid]::NewGuid()
                        )

                        New-Item -ItemType Directory -Path $configDirectory |
                            Out-Null

                        try {
                            Copy-Item $env:DOCKER_AUTH_FILE (
                                Join-Path $configDirectory 'config.json'
                            )

                            docker --config "$configDirectory" push `
                                "$($env:IMAGE_REPO):$($env:IMAGE_TAG)"

                            if ($LASTEXITCODE -ne 0) {
                                throw 'Docker push failed'
                            }
                        }
                        finally {
                            Remove-Item -Recurse -Force $configDirectory
                        }
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                bat '''
                    @echo off
                    aws eks update-kubeconfig --name %EKS_CLUSTER% --region %AWS_REGION%
                    if errorlevel 1 exit /b 1

                    kubectl --context "%KUBE_CONTEXT%" -n finsecure set image deployment/finsecure-pay-api finsecure-pay-api=%IMAGE_REPO%:%IMAGE_TAG%
                    if errorlevel 1 exit /b 1

                    kubectl --context "%KUBE_CONTEXT%" -n finsecure rollout status deployment/finsecure-pay-api --timeout=180s
                    if errorlevel 1 exit /b 1

                    kubectl --context "%KUBE_CONTEXT%" -n finsecure get deployment finsecure-pay-api
                    if errorlevel 1 exit /b 1

                    kubectl --context "%KUBE_CONTEXT%" -n finsecure get pods -o wide
                    if errorlevel 1 exit /b 1
                '''
            }
        }
    }

    post {
        success {
            echo "Deployed ${env.IMAGE_REPO}:${env.IMAGE_TAG}"
        }
        failure {
            echo 'Pipeline failed. Review the first failing stage.'
        }
    }
}