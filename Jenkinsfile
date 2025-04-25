def VERSION

pipeline {
    agent any
    environment {
        SILLY = ''
    }
    stages {
        stage('Give Permission') {
            steps {
                script {
                    sh 'chmod +x gradlew'
                }
            }
        }
        stage('Version') {
            steps {
                script {
                    if (env.TAG_NAME) {
                        VERSION = "${TAG_NAME}"
                    } else if (env.BRANCH_NAME == 'master') {
                        VERSION = "${BUILD_NUMBER}"
                    } else {
                        VERSION = "${BRANCH_NAME}-${BUILD_NUMBER}"
                    }
                }
            }
        }
        stage('Build') {
            steps {
                script {
                    sh "./gradlew -Pversion=${VERSION} clean build"
                    archiveArtifacts artifacts: "build/libs/*.jar", fingerprint: true
                }
            }
        }
    }
}