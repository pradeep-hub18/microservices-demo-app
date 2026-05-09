def services = [
  [
    name: 'auth-service',
    directory: 'auth-service',
    ecrRepositoryParam: 'AUTH_ECR_REPOSITORY',
    helmValuesKey: 'authService'
  ],
  [
    name: 'catalog-service',
    directory: 'catalog-service',
    ecrRepositoryParam: 'CATALOG_ECR_REPOSITORY',
    helmValuesKey: 'catalogService'
  ]
]

def localImages = [:]
def remoteImages = [:]

def defaultConfig(String name) {
  switch (name) {
    case 'GIT_REPOSITORY_URL':
      return 'https://github.com/pradeep-hub18/microservices-demo-app.git'
    case 'GIT_BRANCH':
      return 'main'
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
    case 'UPDATE_HELM_VALUES':
      return 'true'
    case 'HELM_VALUES_FILE':
      return 'helm/microservices-demo/values-dev.yaml'
    case 'GITOPS_GIT_CREDENTIALS_ID':
      return 'GitHub-pat'
    case 'GITOPS_GIT_USER_NAME':
      return 'jenkins'
    case 'GITOPS_GIT_USER_EMAIL':
      return 'jenkins@local'
    default:
      return ''
  }
}

def configValue(String name) {
  def value = env.getProperty(name)
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

def runWithGitCredentials(Closure body) {
  def gitCredentialsId = configValue('GITOPS_GIT_CREDENTIALS_ID')
  if (gitCredentialsId) {
    withCredentials([usernamePassword(
      credentialsId: gitCredentialsId,
      usernameVariable: 'GITOPS_USERNAME',
      passwordVariable: 'GITOPS_PASSWORD'
    )]) {
      body(true)
    }
  } else {
    body(false)
  }
}

def checkoutSource() {
  try {
    checkout scm
  } catch (ignored) {
    echo 'No SCM context is available for checkout scm. Falling back to explicit Git checkout.'
    git branch: configValue('GIT_BRANCH'),
      credentialsId: configValue('GITOPS_GIT_CREDENTIALS_ID'),
      url: configValue('GIT_REPOSITORY_URL')
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
        script {
          checkoutSource()
        }
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
            localImages.put(service.name, localImage)
          }
        }
      }
    }

    stage('Trivy Image Scan') {
      steps {
        script {
          services.each { service ->
            def localImage = localImages.get(service.name)
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
              def localImage = localImages.get(service.name)
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
              remoteImages.put(service.name, remoteImage)
            }
          }
        }
      }
    }

    stage('Update Helm Image Tags') {
      when {
        expression { return configBoolean('UPDATE_HELM_VALUES') }
      }
      steps {
        script {
          def valuesFile = configValue('HELM_VALUES_FILE')

          withEnv([
            "HELM_VALUES_FILE=${valuesFile}",
            "AUTH_IMAGE_TAG=${env.IMAGE_TAG_VALUE}",
            "CATALOG_IMAGE_TAG=${env.IMAGE_TAG_VALUE}"
          ]) {
            sh '''
              set -e
              python3 <<'PY'
from pathlib import Path
import os
import re

path = Path(os.environ["HELM_VALUES_FILE"])
lines = path.read_text().splitlines()
updates = {
    "authService": os.environ["AUTH_IMAGE_TAG"],
    "catalogService": os.environ["CATALOG_IMAGE_TAG"],
}

current_section = None
inside_image = False

for index, line in enumerate(lines):
    stripped = line.strip()
    if line and not line.startswith(" ") and stripped.endswith(":"):
        current_section = stripped[:-1]
        inside_image = False
        continue

    if current_section in updates and re.match(r"^\\s{2}image:\\s*$", line):
        inside_image = True
        continue

    if current_section in updates and inside_image and re.match(r"^\\s{4}tag:", line):
        indent = line[: len(line) - len(line.lstrip())]
        lines[index] = f'{indent}tag: "{updates[current_section]}"'

path.write_text("\\n".join(lines) + "\\n")
PY
            '''
          }

          def hasChanges = sh(
            script: "git diff --quiet -- ${valuesFile}",
            returnStatus: true
          ) != 0

          if (hasChanges) {
            sh """
              git config user.name '${configValue('GITOPS_GIT_USER_NAME')}'
              git config user.email '${configValue('GITOPS_GIT_USER_EMAIL')}'
              git add ${valuesFile}
              git commit -m 'Update Helm image tags to ${env.IMAGE_TAG_VALUE} [skip ci]'
            """

            runWithGitCredentials { hasGitCredentials ->
              if (hasGitCredentials) {
                sh '''
                  set -e
                  remote_url="$(git config --get remote.origin.url)"
                  askpass_file="$(mktemp)"
                  trap 'rm -f "$askpass_file"' EXIT
                  cat > "$askpass_file" <<'EOF'
#!/bin/sh
case "$1" in
  *Username*) printf '%s\n' "$GITOPS_USERNAME" ;;
  *Password*) printf '%s\n' "$GITOPS_PASSWORD" ;;
esac
EOF
                  chmod 700 "$askpass_file"
                  GIT_ASKPASS="$askpass_file" GIT_TERMINAL_PROMPT=0 git push "$remote_url" "HEAD:${BRANCH_NAME:-main}"
                '''
              } else {
                sh 'git push origin "HEAD:${BRANCH_NAME:-main}"'
              }
            }
          } else {
            echo "No Helm image tag changes detected in ${valuesFile}."
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
