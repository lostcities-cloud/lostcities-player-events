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
        stage('Build') { 
            steps {
                withGradle {
                    sh './gradlew build'
                }
            }
        }
        stage('Test') { 
            steps {
                withGradle {
                    sh './gradlew test'
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
}