pipeline {
    agent any

    options {
        ansiColor('xterm')
    }

    environment {
        GH_ACTOR = credentials('GITHUB_ACTOR')
        GH_TOKEN = credentials('GITHUB_TOKEN')
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
        }
        stage('Publish') {
            steps {
                withGradle {
                    sh './gradlew jib'
                }
            }
        }
    }

    post {
        always {
            junit 'build/test-results/test/**/*.xml'
        }
    }
}
