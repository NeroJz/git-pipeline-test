import jenkins.model.Jenkins
import groovy.json.*

  def call(body) {
    def config = [: ]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
      agent any
      options {
        skipDefaultCheckout()
      }
      environment {
        jobName = "${env.JOB_NAME}".replace('%2F', '_')
        serviceName = "${config.serviceName}"
        servicePlan = "${config.servicePlan}"
        serviceInstanceName = "${config.serviceInstanceName}"
        jsonFileName = "${config.jsonFileName}"
        jsonResetName = 'xsSecurityReset.json'
        restageAppName = "${config.restageApp}"
        releaseTag = "${config.releaseTag}"

        startingIntgTag = "0.1.0-intg"
        startingQaTag = "0.1.0-qa"
        startingDemoTag = "0.1.0-demo"
        startingPerfTag = "0.1.0-perf"
        startingextDemoTag = "0.1.0-extDemo"

        scmUrl = scm.getUserRemoteConfigs()[0].getUrl()

        envAwareJsonFile = "xs-security-env.json"
        envResetJsonfile = "xsSecurityReset.json"
        // envAwareIdentifier = "${config.envAwareIdentifier}"

        // Hard coded for now
        envAwareIdentifier = "xsappname"
        intgTags = ""
        qaTags = ""
        demoTags = ""
      }

      stages {

        stage('Checkout') {
          steps {

            // Hardcoded for xsuaa services                    
            echo "Starting Job with parameters: ${serviceName} ${servicePlan} ${serviceInstanceName} ${jsonFileName}"

            script {
              echo "Starting function to clone repository"
              // def promoteIntg = new com.accenture.newspage.PromoteIntg();
              if (DEPLOY_ENV == "dev") {
                checkout scm
              }

              if (DEPLOY_ENV == "extDemo") {
                checkout scm
              }

              if (DEPLOY_ENV == "intg") {
                def specifiedTarget = params["Tag/Commit Hash"]
                if (specifiedTarget == "" || specifiedTarget == null) {
                  cloneRepository(scmUrl, "master")
                } else if (specifiedTarget.contains(".")) {
                  echo "Tag specified: ${specifiedTarget}"
                  cloneRepository(scmUrl, specifiedTarget)
                } else {
                  echo "Commit Hash Specified : ${specifiedTarget}"
                  cloneRepository(scmUrl, specifiedTarget)
                  echo "Checking to see if the last Dev Build was successful to promote intg"
                  // promoteIntg.validateBuildStatus(jobName, "${env.JENKINS_API_URL}", "${JENKINS_API_CRED_USR}", "${JENKINS_API_CRED_PSW}", "${scmUrl}", "${specifiedTarget}", this)
                }
              }

              if (DEPLOY_ENV == "qa") {
                def specifiedTarget = params["Tag"]
                if (specifiedTarget == "" || specifiedTarget == null) {
                  error "Promotion to qa must be from a specific tag"
                } else {
                  if (specifiedTarget.contains(".")) {
                    echo "Tag specified: ${specifiedTarget}"
                    cloneRepository(scmUrl, specifiedTarget)
                  } else {
                    error "Promotion to qa must be from a specific tag"
                  }
                }
              }

              if (DEPLOY_ENV == "demo") {
                def specifiedTarget = params["Tag"]
                if (specifiedTarget == "" || specifiedTarget == null) {
                  error "Promotion to demo must be from a specific tag"
                } else {
                  if (specifiedTarget.contains(".")) {
                    echo "Tag specified: ${specifiedTarget}"
                    cloneRepository(scmUrl, specifiedTarget)
                  } else {
                    error "Promotion to demo must be from a specific tag"
                  }
                }
              }

              if (jobName.endsWith('perf')) {
                def specifiedTarget = params["Tag"]
                if (specifiedTarget == "" || specifiedTarget == null) {
                  error "Promotion to Perf must be from a specific tag"
                } else {
                  if (specifiedTarget.contains(".")) {
                    echo "Tag specified: ${specifiedTarget}"
                    cloneRepository(scmUrl, specifiedTarget)
                  } else {
                    error "Promotion to Perf must be from a specific tag"
                  }
                }
              }

              echo "Starting function to generate security reset jason"
              createResetJson("${jsonFileName}") //function call for creating reset json
              def jsonContents = readJSON file: jsonFileName
              def jsonReset = readJSON file: jsonResetName
              def nonEnvAwareValue = jsonContents.xsappname
              def nonEnvResetValue = jsonReset.xsappname
              if (jsonContents.has('role-collections')) {
                def RoleCollecArray = jsonContents.
                'role-collections'.name
                def countlist = RoleCollecArray.size()
                if (jobName.endsWith('qa')) {
                  for (j = 0; j < countlist; j++) {
                    jsonContents.
                    'role-collections' [j].name = RoleCollecArray[j] + "-qa"
                  }

                }
                if (jobName.endsWith('dev')) {
                  for (j = 0; j < countlist; j++) {
                    jsonContents.
                    'role-collections' [j].name = RoleCollecArray[j] + "-dev"
                  }
                }
                if (jobName.endsWith('demo')) {
                  for (j = 0; j < countlist; j++) {
                    jsonContents.
                    'role-collections' [j].name = RoleCollecArray[j] + "-demo"
                  }
                }
                if (jobName.endsWith('perf')) {
                  for (j = 0; j < countlist; j++) {
                    jsonContents.
                    'role-collections' [j].name = RoleCollecArray[j] + "-perf"
                  }
                }
                if (jobName.endsWith('extDemo')) {
                  for (j = 0; j < countlist; j++) {
                    jsonContents.
                    'role-collections' [j].name = RoleCollecArray[j] + "-extDemo"
                  }
                }
                if (jobName.endsWith('intg')) {
                  for (j = 0; j < countlist; j++) {
                    jsonContents.
                    'role-collections' [j].name = RoleCollecArray[j] + "-intg"
                  }
                }
              } else {
                echo "Role -Collections not found"
              }
              if (jobName.endsWith('dev')) {
                envAwareValue = nonEnvAwareValue + "-dev"
                envResetValue = nonEnvResetValue + "-dev"
              }
              if (jobName.endsWith('intg')) {
                envAwareValue = nonEnvAwareValue + "-intg"
                envResetValue = nonEnvResetValue + "-intg"
              }
              if (jobName.endsWith('qa')) {
                envAwareValue = nonEnvAwareValue + "-qa"
                envResetValue = nonEnvResetValue + "-qa"
              }
              if (jobName.endsWith('demo')) {
                envAwareValue = nonEnvAwareValue + "-demo"
                envResetValue = nonEnvResetValue + "-demo"
              }
              if (jobName.endsWith('perf')) {
                envAwareValue = nonEnvAwareValue + "-perf"
                envResetValue = nonEnvResetValue + "-perf"
              }
              if (jobName.endsWith('extDemo')) {
                envAwareValue = nonEnvAwareValue + "-extDemo"
                envResetValue = nonEnvResetValue + "-extDemo"
              }

              echo "Updated environment aware xsappname: ${envAwareValue}"

              jsonContents.xsappname = envAwareValue
              jsonReset.xsappname = envResetValue

              sh "ls -l"

              Jenkins.instance.pluginManager.plugins.each{
                plugin ->
                  println("${plugin.getDisplayName()} (${plugin.getShortName()}): ${plugin.getVersion()}")
              }

              echo "Start writing JSON file: ${envAwareJsonFile}\n"
              writeJSON(file: envAwareJsonFile, json: jsonContents, pretty: 4)

              echo "Start writing JSON file: ${envResetJsonfile}\n"
              writeJSON(file: envResetJsonfile, json: jsonReset, pretty: 4)

              sh 'cat xs-security-env.json'
              sh 'cat xsSecurityReset.json'

              if (jobName.endsWith('dev')) {

              } else if (jobName.endsWith('intg')) {
                cfOrg = "${env.CF_INTG_ORG}"
                cfSpace = "${env.CF_INTG_SPACE}"
                if (DEPLOY_ENV == null) {
                  echo "WARN: Manually setting DEPLOY_ENV This needs to be updated in the Job"
                  DEPLOY_ENV = "intg"
                }
              } else if (jobName.endsWith('qa')) {
                cfOrg = "${env.CF_QA_ORG}"
                cfSpace = "${env.CF_QA_SPACE}"
                if (DEPLOY_ENV == null) {
                  echo "WARN: Manually setting DEPLOY_ENV This needs to be updated in the Job"
                  DEPLOY_ENV = "qa"
                }
              } else if (jobName.endsWith('demo')) {
                cfOrg = "${env.CF_DEMO_ORG}"
                cfSpace = "${env.CF_DEMO_SPACE}"
                if (DEPLOY_ENV == null) {
                  echo "WARN: Manually setting DEPLOY_ENV This needs to be updated in the Job"
                  DEPLOY_ENV = "demo"
                }
              } else if (jobName.endsWith('perf')) {
                cfOrg = "${env.CF_PERF_ORG}"
                cfSpace = "${env.CF_PERF_SPACE}"
                if (DEPLOY_ENV == null) {
                  echo "WARN: Manually setting DEPLOY_ENV This needs to be updated in the Job"
                  DEPLOY_ENV = "perf"
                }
              } else if (jobName.endsWith('extDemo')) {
                cfOrg = "${env.CF_ExtDemo_ORG}"
                cfSpace = "${env.CF_ExTDemo_SPACE}"
                if (DEPLOY_ENV == null) {
                  echo "WARN: Manually setting DEPLOY_ENV This needs to be updated in the Job"
                  DEPLOY_ENV = "extDemo"
                }
              }
            }
          }
        }
    }
  }
}

def createResetJson(jsonFileName) {

  script {
    def jsonContentsReset = readJSON file: jsonFileName
    if (jsonContentsReset.has('role-collections')) {
      jsonContentsReset.remove('role-collections')
      def jsonStr = JsonOutput.prettyPrint(JsonOutput.toJson(jsonContentsReset))
      writeFile(file: 'xsSecurityReset.json', text: jsonStr)
    } else {
      throw new Exception("Build failed as role-collections not found under ${jsonFileName}")
    }
  }
}