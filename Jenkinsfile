def services = [
  [
    name: 'auth-service',
    directory: 'auth-service',
    ecrRepositoryParam: 'AUTH_ECR_REPOSITORY',
    deploymentParam: 'AUTH_K8S_DEPLOYMENT',
    containerParam: 'AUTH_K8S_CONTAINER'
  ],
  [
    name: 'catalog-service',
    directory: 'catalog-service',
    ecrRepositoryParam: 'CATALOG_ECR_REPOSITORY',
    deploymentParam: 'CATALOG_K8S_DEPLOYMENT',
    containerParam: 'CATALOG_K8S_CONTAINER'
  ]
]

def defaultConfig(String name) {
  switch (name) {
    case 'SONARQUBE_SERVER':
      return 'SonarQube'
    case 'SONAR_SCANNER_TOOL':
      return 'SonarScanner'
    case 'SONAR_PROJECT_PREFIX':
      return 'microservices-demo-app'
    case 'QUALITY_THRESHOLD':
      return '80'
    case 'ALERT_EMAIL':
      return ''
    case 'AWS_REGION':
      return 'ap-south-1'
    case 'AWS_ACCOUNT_ID':
      return '152406482015'
    case 'AWS_CREDENTIALS_ID':
      return 'aws-ecr-eks-creds'
    case 'AUTH_ECR_REPOSITORY':
      return 'microapps/auth-service'
    case 'CATALOG_ECR_REPOSITORY':
      return 'microapps/catalog-service'
    case 'CREATE_ECR_REPOS':
      return 'true'
    case 'IMAGE_TAG':
      return ''
    case 'TRIVY_SEVERITY':
      return 'HIGH,CRITICAL'
    case 'DEPLOY_TO_EKS':
      return 'false'
    case 'EKS_CLUSTER_NAME':
      return 'demo-eks-DEV-eks-cluster'
    case 'K8S_NAMESPACE':
      return 'default'
    case 'AUTH_K8S_DEPLOYMENT':
      return 'auth-service'
    case 'CATALOG_K8S_DEPLOYMENT':
      return 'catalog-service'
    case 'AUTH_K8S_CONTAINER':
      return 'auth-service'
    case 'CATALOG_K8S_CONTAINER':
      return 'catalog-service'
    default:
      return ''
  }
}

def configValue(String name) {
  def value = env[name]
  if (value?.trim()) {
    return value.trim()
  }
  return defaultConfig(name)
}

def configBoolean(String name) {
  return configValue(name).toBoolean()
}

def sendAlert(String subject, String body) {
  def alertEmail = configValue('ALERT_EMAIL')
  if (alertEmail) {
    emailext(
      to: alertEmail,
      subject: subject,
      body: body
    )
  }
}

def jacocoLineCoverage(String reportPath) {
  def report = readFile(reportPath)
  def matches = report =~ /<counter type="LINE" missed="([0-9]+)" covered="([0-9]+)"/
  def missed = 0
  def covered = 0

  matches.each { match ->
    missed = match[1] as Integer
    covered = match[2] as Integer
  }

  def total = missed + covered
  if (total == 0) {
    return 0.0
  }
  return (covered * 100.0) / total
}

def runWithAwsCredentials(Closure body) {
  def awsCredentialsId = configValue('AWS_CREDENTIALS_ID')
  if (awsCredentialsId) {
    withCredentials([[
      $class: 'AmazonWebServicesCredentialsBinding',
      credentialsId: awsCredentialsId
    ]]) {
      body()
    }
  } else {
    body()
  }
}

pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  environment {
    MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
  }

  stages {
    stage('SCM Checkout') {
      steps {
        checkout scm
        script {
          env.GIT_SHORT_SHA = sh(
            script: 'git rev-parse --short=7 HEAD',
            returnStdout: true
          ).trim()
          env.IMAGE_TAG_VALUE = configValue('IMAGE_TAG')
            ? configValue('IMAGE_TAG')
            : "${env.BUILD_NUMBER}-${env.GIT_SHORT_SHA}"
          env.ECR_REGISTRY = "${configValue('AWS_ACCOUNT_ID')}.dkr.ecr.${configValue('AWS_REGION')}.amazonaws.com"
        }
      }
    }

    stage('Validate Tooling') {
      steps {
        sh '''
          set -e
          java -version
          mvn -version
          docker --version
          trivy --version
          aws --version
          kubectl version --client=true
        '''
      }
    }

    stage('Unit Test and JaCoCo') {
      steps {
        script {
          services.each { service ->
            sh "mvn -f ${service.directory}/pom.xml clean verify"
            junit "${service.directory}/target/surefire-reports/*.xml"
            archiveArtifacts artifacts: "${service.directory}/target/site/jacoco/**", allowEmptyArchive: false

            def coverage = jacocoLineCoverage("${service.directory}/target/site/jacoco/jacoco.xml")
            echo "${service.name} JaCoCo line coverage: ${String.format('%.2f', coverage)}%"

            if (coverage < configValue('QUALITY_THRESHOLD').toDouble()) {
              def message = "${service.name} line coverage is ${String.format('%.2f', coverage)}%, below required ${configValue('QUALITY_THRESHOLD')}%."
              sendAlert("Jenkins quality alert: ${service.name}", message)
              error(message)
            }
          }
        }
      }
    }

    stage('SonarQube Scan and Quality Gate') {
      steps {
        script {
          def scannerHome = tool configValue('SONAR_SCANNER_TOOL')

          services.each { service ->
            withSonarQubeEnv(configValue('SONARQUBE_SERVER')) {
              sh """
                ${scannerHome}/bin/sonar-scanner \\
                  -Dsonar.projectKey=${configValue('SONAR_PROJECT_PREFIX')}-${service.name} \\
                  -Dsonar.projectName=${configValue('SONAR_PROJECT_PREFIX')}-${service.name} \\
                  -Dsonar.sources=${service.directory}/src/main/java,${service.directory}/src/main/resources/static \\
                  -Dsonar.tests=${service.directory}/src/test/java \\
                  -Dsonar.java.binaries=${service.directory}/target/classes \\
                  -Dsonar.junit.reportPaths=${service.directory}/target/surefire-reports \\
                  -Dsonar.coverage.jacoco.xmlReportPaths=${service.directory}/target/site/jacoco/jacoco.xml \\
                  -Dsonar.sourceEncoding=UTF-8
              """
            }

            timeout(time: 5, unit: 'MINUTES') {
              def qualityGate = waitForQualityGate abortPipeline: false
              if (qualityGate.status != 'OK') {
                def message = "${service.name} failed SonarQube quality gate with status ${qualityGate.status}."
                sendAlert("Jenkins SonarQube alert: ${service.name}", message)
                error(message)
              }
            }
          }
        }
      }
    }

    stage('Trivy Filesystem Scan') {
      steps {
        script {
          services.each { service ->
            sh """
              trivy fs \\
                --exit-code 1 \\
                --severity ${configValue('TRIVY_SEVERITY')} \\
                --scanners vuln,secret,misconfig \\
                --ignore-unfixed \\
                ${service.directory}
            """
          }
        }
      }
    }

    stage('Build Docker Images') {
      steps {
        script {
          services.each { service ->
            def localImage = "${service.name}:${env.IMAGE_TAG_VALUE}"
            sh "docker build -t ${localImage} ${service.directory}"
            env["${service.name.replace('-', '_').toUpperCase()}_LOCAL_IMAGE"] = localImage
          }
        }
      }
    }

    stage('Trivy Image Scan') {
      steps {
        script {
          services.each { service ->
            def localImage = env["${service.name.replace('-', '_').toUpperCase()}_LOCAL_IMAGE"]
            sh """
              trivy image \\
                --exit-code 1 \\
                --severity ${configValue('TRIVY_SEVERITY')} \\
                --ignore-unfixed \\
                ${localImage}
            """
          }
        }
      }
    }

    stage('Push Images to ECR') {
      steps {
        script {
          if (!configValue('AWS_ACCOUNT_ID')) {
            error('AWS_ACCOUNT_ID is required to push images to ECR.')
          }

          runWithAwsCredentials {
            sh """
              aws ecr get-login-password --region ${configValue('AWS_REGION')} \\
                | docker login --username AWS --password-stdin ${env.ECR_REGISTRY}
            """

            services.each { service ->
              def repoName = configValue(service.ecrRepositoryParam)
              def localImage = env["${service.name.replace('-', '_').toUpperCase()}_LOCAL_IMAGE"]
              def remoteImage = "${env.ECR_REGISTRY}/${repoName}:${env.IMAGE_TAG_VALUE}"

              if (configBoolean('CREATE_ECR_REPOS')) {
                sh """
                  aws ecr describe-repositories --region ${configValue('AWS_REGION')} --repository-names ${repoName} >/dev/null 2>&1 \\
                    || aws ecr create-repository --region ${configValue('AWS_REGION')} --repository-name ${repoName} >/dev/null
                """
              }

              sh """
                docker tag ${localImage} ${remoteImage}
                docker push ${remoteImage}
              """
              env["${service.name.replace('-', '_').toUpperCase()}_REMOTE_IMAGE"] = remoteImage
            }
          }
        }
      }
    }

    stage('Deploy to EKS') {
      when {
        expression { return configBoolean('DEPLOY_TO_EKS') }
      }
      steps {
        script {
          runWithAwsCredentials {
            sh "aws eks update-kubeconfig --region ${configValue('AWS_REGION')} --name ${configValue('EKS_CLUSTER_NAME')}"

            services.each { service ->
              def remoteImage = env["${service.name.replace('-', '_').toUpperCase()}_REMOTE_IMAGE"]
              def deployment = configValue(service.deploymentParam)
              def container = configValue(service.containerParam)

              sh """
                kubectl -n ${configValue('K8S_NAMESPACE')} set image deployment/${deployment} ${container}=${remoteImage}
                kubectl -n ${configValue('K8S_NAMESPACE')} rollout status deployment/${deployment} --timeout=180s
              """
            }
          }
        }
      }
    }
  }

  post {
    success {
      echo "Pipeline completed successfully. Images pushed with tag ${env.IMAGE_TAG_VALUE}."
    }
    failure {
      script {
        sendAlert(
          "Jenkins pipeline failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
          "Build URL: ${env.BUILD_URL}\nBranch: ${env.BRANCH_NAME ?: 'main'}\nCommit: ${env.GIT_SHORT_SHA ?: 'unknown'}"
        )
      }
    }
    always {
      cleanWs(deleteDirs: true, notFailBuild: true)
    }
  }
}
