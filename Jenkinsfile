pipeline {
  agent any

  environment {
    IMAGE_TAG = "${env.BUILD_NUMBER}"
  }

  parameters {
    string(name: 'IMAGE_REGISTRY', defaultValue: 'replace-with-registry', description: 'Container registry path, for example account-id.dkr.ecr.ap-south-1.amazonaws.com')
    string(name: 'IMAGE_NAMESPACE', defaultValue: 'microapps', description: 'Image namespace or repository prefix')
  }

  stages {
    stage('Build JARs') {
      steps {
        sh 'mvn -f auth-service/pom.xml clean package'
        sh 'mvn -f catalog-service/pom.xml clean package'
      }
    }

    stage('Build Docker Images') {
      steps {
        sh 'docker build -t ${IMAGE_REGISTRY}/${IMAGE_NAMESPACE}/auth-service:${IMAGE_TAG} auth-service'
        sh 'docker build -t ${IMAGE_REGISTRY}/${IMAGE_NAMESPACE}/catalog-service:${IMAGE_TAG} catalog-service'
      }
    }

    stage('Push Docker Images') {
      steps {
        sh 'docker push ${IMAGE_REGISTRY}/${IMAGE_NAMESPACE}/auth-service:${IMAGE_TAG}'
        sh 'docker push ${IMAGE_REGISTRY}/${IMAGE_NAMESPACE}/catalog-service:${IMAGE_TAG}'
      }
    }
  }
}
