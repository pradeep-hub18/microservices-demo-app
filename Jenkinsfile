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

def sendAlert(String subject, String body) {
  if (params.ALERT_EMAIL?.trim()) {
    emailext(
      to: params.ALERT_EMAIL.trim(),
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
  if (params.AWS_CREDENTIALS_ID?.trim()) {
    withCredentials([[
      $class: 'AmazonWebServicesCredentialsBinding',
      credentialsId: params.AWS_CREDENTIALS_ID.trim()
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

  parameters {
    string(name: 'SONARQUBE_SERVER', defaultValue: 'SonarQube', description: 'Jenkins SonarQube server name from Manage Jenkins > System.')
    string(name: 'SONAR_SCANNER_TOOL', defaultValue: 'SonarScanner', description: 'Jenkins SonarQube Scanner tool name.')
    string(name: 'SONAR_PROJECT_PREFIX', defaultValue: 'microservices-demo-app', description: 'Prefix used for the two SonarQube project keys.')
    string(name: 'QUALITY_THRESHOLD', defaultValue: '80', description: 'Minimum JaCoCo line coverage percentage required for each service.')
    string(name: 'ALERT_EMAIL', defaultValue: '', description: 'Email address for quality/vulnerability/deployment alerts.')

    string(name: 'AWS_REGION', defaultValue: 'ap-south-1', description: 'AWS region for ECR and EKS.')
    string(name: 'AWS_ACCOUNT_ID', defaultValue: '', description: 'AWS account ID that owns the target ECR repositories.')
    string(name: 'AWS_CREDENTIALS_ID', defaultValue: '', description: 'Optional Jenkins AWS credentials ID. Leave empty when the agent already has AWS permissions.')
    string(name: 'AUTH_ECR_REPOSITORY', defaultValue: 'microapps/auth-service', description: 'ECR repository name for auth-service.')
    string(name: 'CATALOG_ECR_REPOSITORY', defaultValue: 'microapps/catalog-service', description: 'ECR repository name for catalog-service.')
    booleanParam(name: 'CREATE_ECR_REPOS', defaultValue: true, description: 'Create ECR repositories if they do not already exist.')
    string(name: 'IMAGE_TAG', defaultValue: '', description: 'Optional image tag. If empty Jenkins uses BUILD_NUMBER-gitsha.')
    string(name: 'TRIVY_SEVERITY', defaultValue: 'HIGH,CRITICAL', description: 'Trivy severities that fail the build.')

    booleanParam(name: 'DEPLOY_TO_EKS', defaultValue: false, description: 'Update existing Kubernetes deployments after pushing images.')
    string(name: 'EKS_CLUSTER_NAME', defaultValue: 'demo-eks-DEV-eks-cluster', description: 'EKS cluster name used when DEPLOY_TO_EKS is true.')
    string(name: 'K8S_NAMESPACE', defaultValue: 'default', description: 'Kubernetes namespace containing the app deployments.')
    string(name: 'AUTH_K8S_DEPLOYMENT', defaultValue: 'auth-service', description: 'Existing Kubernetes Deployment name for auth-service.')
    string(name: 'CATALOG_K8S_DEPLOYMENT', defaultValue: 'catalog-service', description: 'Existing Kubernetes Deployment name for catalog-service.')
    string(name: 'AUTH_K8S_CONTAINER', defaultValue: 'auth-service', description: 'Container name inside the auth-service Deployment.')
    string(name: 'CATALOG_K8S_CONTAINER', defaultValue: 'catalog-service', description: 'Container name inside the catalog-service Deployment.')
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
          env.IMAGE_TAG_VALUE = params.IMAGE_TAG?.trim()
            ? params.IMAGE_TAG.trim()
            : "${env.BUILD_NUMBER}-${env.GIT_SHORT_SHA}"
          env.ECR_REGISTRY = "${params.AWS_ACCOUNT_ID}.dkr.ecr.${params.AWS_REGION}.amazonaws.com"
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

            if (coverage < params.QUALITY_THRESHOLD.toDouble()) {
              def message = "${service.name} line coverage is ${String.format('%.2f', coverage)}%, below required ${params.QUALITY_THRESHOLD}%."
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
          def scannerHome = tool params.SONAR_SCANNER_TOOL

          services.each { service ->
            withSonarQubeEnv(params.SONARQUBE_SERVER) {
              sh """
                ${scannerHome}/bin/sonar-scanner \\
                  -Dsonar.projectKey=${params.SONAR_PROJECT_PREFIX}-${service.name} \\
                  -Dsonar.projectName=${params.SONAR_PROJECT_PREFIX}-${service.name} \\
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
                --severity ${params.TRIVY_SEVERITY} \\
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
                --severity ${params.TRIVY_SEVERITY} \\
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
          if (!params.AWS_ACCOUNT_ID?.trim()) {
            error('AWS_ACCOUNT_ID is required to push images to ECR.')
          }

          runWithAwsCredentials {
            sh """
              aws ecr get-login-password --region ${params.AWS_REGION} \\
                | docker login --username AWS --password-stdin ${env.ECR_REGISTRY}
            """

            services.each { service ->
              def repoName = params[service.ecrRepositoryParam]
              def localImage = env["${service.name.replace('-', '_').toUpperCase()}_LOCAL_IMAGE"]
              def remoteImage = "${env.ECR_REGISTRY}/${repoName}:${env.IMAGE_TAG_VALUE}"

              if (params.CREATE_ECR_REPOS) {
                sh """
                  aws ecr describe-repositories --region ${params.AWS_REGION} --repository-names ${repoName} >/dev/null 2>&1 \\
                    || aws ecr create-repository --region ${params.AWS_REGION} --repository-name ${repoName} >/dev/null
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
        expression { return params.DEPLOY_TO_EKS }
      }
      steps {
        script {
          runWithAwsCredentials {
            sh "aws eks update-kubeconfig --region ${params.AWS_REGION} --name ${params.EKS_CLUSTER_NAME}"

            services.each { service ->
              def remoteImage = env["${service.name.replace('-', '_').toUpperCase()}_REMOTE_IMAGE"]
              def deployment = params[service.deploymentParam]
              def container = params[service.containerParam]

              sh """
                kubectl -n ${params.K8S_NAMESPACE} set image deployment/${deployment} ${container}=${remoteImage}
                kubectl -n ${params.K8S_NAMESPACE} rollout status deployment/${deployment} --timeout=180s
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
