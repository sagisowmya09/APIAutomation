pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    options {
        timestamps()
    }

    stages {
        stage('Verify tools') {
            steps {
                sh 'java -version'
                sh 'mvn -version'
            }
        }

        stage('Run tests') {
            steps {
                sh 'mvn -B clean test'
            }
        }
    }

    post {
        always {
            junit testResults: 'target/failure-logs/TEST-*.xml',
                  allowEmptyResults: true
            archiveArtifacts artifacts: 'target/failure-logs/**',
                             allowEmptyArchive: true
        }
    }
}
