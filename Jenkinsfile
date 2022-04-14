pipeline {
    agent any

    options {
        ansiColor('xterm')
    }

    environment {
        GH_ACTOR = credentials('GITHUB_ACTOR')
        GH_TOKEN = credentials('GITHUB_TOKEN')
    }

    tools {
       jdk "openjdk-17"
    }

    stages {
        stage('ktlint') {
            steps {
                withGradle {
                    sh './gradlew ktlintCheck'
                }
            }
        }
        stage('Assemble') {
            steps {
                withGradle {
                    sh './gradlew clean assemble'
                }
            }
        }
        stage('Test') {
            steps {
                withGradle {
                    sh './gradlew check'
                }
            }
            post {
                always {
                    junit 'build/test-results/test/*.xml'
                }
            }
        }
        stage('Publish Docker') {
            steps {
                withGradle {
                    sh './gradlew jib'
                }
            }
        }
    }

}
