pipeline {
    agent any

    stages {
        
        stage('Verify Tools') {
            steps {
                // Verifies that Java and Maven from the system PATH are working
                bat 'java -version'
                bat 'mvn -version'
            }
        }
        
        stage('Checkout') {
            steps {
                git url: 'https://github.com/AlejandroAlberca/java-chess-app.git', branch: 'main'
            }
        }

        stage('Compile and Package') {
            steps {
                // Runs Maven clean package skipping tests for a faster build
                bat 'mvn clean package -DskipTests'
            }
        }
    }

    post {
        success {
            echo 'The .jar file has been successfully generated in the target/ directory.'
        }
        failure {
            echo 'Build or packaging failed.'
        }
    }
}
