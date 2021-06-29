import jenkins.model.Jenkins
import groovy.json.*

  def call(body) {
    def config = [: ]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
      agent {
        label 'nodejs10x'
      }
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
        JENKINS_API_CRED = credentials('JENKINS_API')
        CF_BUILD = credentials('CF_BUILD')

        envAwareJsonFile = "xs-security-env.json"
        envResetJsonfile = "xsSecurityReset.json"
        // envAwareIdentifier = "${config.envAwareIdentifier}"

        // Hard coded for now
        envAwareIdentifier = "xsappname"

        cfApi = "${env.CF2_API}"
        cfOrg = "${env.CF_DEV_ORG}"
        cfSpace = "${env.CF2_DEV_SPACE}"
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
              def promoteIntg = new com.accenture.newspage.PromoteIntg();
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
                  promoteIntg.validateBuildStatus(jobName, "${env.JENKINS_API_URL}", "${JENKINS_API_CRED_USR}", "${JENKINS_API_CRED_PSW}", "${scmUrl}", "${specifiedTarget}", this)
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

def restageApp(resources, api, org, space, restageAppName, restageAppEnv) {
  echo "Starting function to restage app"
  restageAppName = restageAppName.toLowerCase()
  restageAppNameList = restageAppName.split(',')
  restageAppEnvName = restageAppEnv
  if (restageAppNameList[0].equals("null")) {
    restageAppNameList[0] = "none"
  }
  if (restageAppNameList.length == 1 && restageAppNameList[0].equals("all")) {
    echo "Restage All App"
    script {
      for (int i = 0; i < resources.size(); i++) {

        def appguid = resources[i].entity.app_guid
        withCfCli(apiEndpoint: api,
          skipSslValidation: true,
          cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0',
          credentialsId: 'CF_BUILD',
          organization: org,
          space: space) {
          // Find app name from guid
          script {
            try {
              def boundAppString = sh(returnStdout: true, script: 'cf curl /v2/apps/' + appguid)
              def boundAppJson = readJSON text: boundAppString
              def boundAppName = boundAppJson.entity.name
              echo "boundAppName: ${boundAppName}"
              sh(returnStdout: true, script: 'cf restage ' + boundAppName)
            } catch (Exception e) {
              echo "Error restaging ${appguid}. Restage manually"
              // TODO Set Warn             
            }
          }
        }
      }
    }
  }

  if (restageAppNameList.length == 1 && (restageAppNameList[0].equals("") || restageAppNameList[0].equals("none"))) {
    echo "No app will be restaged"
  }

  if (restageAppNameList.length >= 1 && !((restageAppNameList[0].equals("all") || restageAppNameList[0].equals("") || restageAppNameList[0].equals("none")))) {
    echo "restaging ${restageAppName} app"
    script {
      String[] boundApp = restageAppName.split(",");
      for (String s: boundApp) {
        try {
          def boundAppName = "${s}"
          echo "boundAppName: ${boundAppName}"
          boundAppName = boundAppNameEnv("${boundAppName}", "${restageAppEnvName}")
          sh(returnStdout: true, script: 'cf restage ' + boundAppName)
        } catch (Exception e) {
          echo "Error restaging Restage ${boundAppName} manually"
          // TODO Set Warn             
        }

      }
    }
  }
}

def boundAppNameEnv(boundAppName, restageAppEnvName) {

  if ("${restageAppEnvName}" == "dev") {
    return ("${boundAppName}" + "-dev")
  }
  if ("${restageAppEnvName}" == "qa") {
    return ("${boundAppName}" + "-qa")
  }
  if ("${restageAppEnvName}" == "demo") {
    return ("${boundAppName}" + "-demo")
  }
  if ("${restageAppEnvName}" == "perf") {
    return ("${boundAppName}" + "-perf")
  }
  if ("${restageAppEnvName}" == "extDemo") {
    return ("${boundAppName}" + "-extDemo")
  }
  if ("${restageAppEnvName}" == "intg") {
    return ("${boundAppName}" + "-intg")
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

def updateServiceReset(serviceInstanceName, envResetJsonfile) {

  def outfile = 'stdout.out'
  def status = sh(script: "cf update-service ${serviceInstanceName} -c ${envResetJsonfile} >${outfile} 2>&1", returnStatus: true)
  def output = readFile(outfile).trim()
  echo "Response recorded for reset update-service: ${output}"
  def messageServicePresent = "Service instance ${serviceInstanceName} not found"

  if (status == 0) {
    echo "updated sucessfully with reset"
  } else if (output.contains("status code: 502")) {
    echo "issue while updating ${serviceInstanceName} : Status Code: 502 : Response Recorded : ${output}"
    retryUpdate("${serviceInstanceName}", "${envResetJsonfile}")
  } else if (output.contains(messageServicePresent)) {
    echo "Service instance ${serviceInstanceName} not found So skiping the reset passing the control to create service"
  } else {
    throw new Exception("Terminating build as update-service using reset json failed")
  }
}

def updateOrCreateService(serviceInstanceName, envAwareJsonFile, serviceName, servicePlan) {

  def outfile = 'stdout.out'
  def status = sh(script: "cf update-service ${serviceInstanceName} -c ${envAwareJsonFile} >${outfile} 2>&1", returnStatus: true)
  def output = readFile(outfile).trim()
  echo "Response recorded for update-service: ${output}"

  if (status == 0) {
    echo "updated sucessfully"
  } else if (output.contains("status code: 502")) {
    echo "issue while updating ${serviceInstanceName} : Status Code: 502 : Response Recorded : ${output}"
    retryUpdate("${serviceInstanceName}", "${envAwareJsonFile}")
  } else {
    echo "Service can't be updated, trying to create"
    def outfileCreate = 'stdout.out'
    def statusCreate = sh(script: "cf create-service ${serviceName} ${servicePlan} ${serviceInstanceName} -c ${envAwareJsonFile} >${outfileCreate} 2>&1", returnStatus: true)
    def outputCreate = readFile(outfileCreate).trim()
    echo "Response recorded for create-service: ${outputCreate}"
    if (statusCreate == 0 && !(output.contains("already exists"))) {
      echo "Service ${serviceName} Created successfully"
    } else {
      throw new Exception("Terminating build as both update service and create service command failed")
    }
  }
}

def retryUpdate(serviceInstanceName, envJsonFile) {

  echo "Now retrying update command as failed due to Server error"
  def set = ""
  def count = 1
  for (i = 3; i > 0; i--) {
    count++
    echo "update attempt ${count}"
    def outfileRetry = 'stdout.out'
    def statusRetry = sh(script: "cf update-service ${serviceInstanceName} -c ${envJsonFile} >${outfileRetry} 2>&1", returnStatus: true)
    def outputRetry = readFile(outfileRetry).trim()
    echo "Response recorded for update-service: ${outputRetry}"
    if (statusRetry == 0) {
      echo "Updated sucessfully on ${count} update attempt"
      set = "passed"
      break
    } else {
      set = "failed"
    }
  }
  if (set.contains("failed")) {
    throw new Exception("Terminating build as update service command failed even after retying for four times")
  }
}

def cloneRepository(def gitUrl, def releaseTag) {
  try {
    checkout([
      $class: 'GitSCM',
      branches: [
        [
          name: releaseTag
        ]
      ],
      doGenerateSubmoduleConfigurations: false,
      extensions: [
        [
          $class: 'LocalBranch', localBranch: "**"
        ]
      ],
      submoduleCfg: [],
      userRemoteConfigs: [
        [
          credentialsId: '93ef04e3-e61c-41f5-83de-5ae8b0e65bad',
          url: gitUrl
        ]
      ]
    ])
  } catch (Exception ex) {
    String sStackTrace = ex.getMessage()
    if (sStackTrace.contains("Couldn't find any revision to build")) {
      error "Please enter a valid tag or commit hash(eg: d8234c75153c6524c72806a3d5d36f76716fe30b): Verify the repository and branch configuration : for getting valid commit hash use mentioned command git log -n 1 --pretty=format:'%H' tag/commitID"
    } else if (sStackTrace.contains("Error cloning remote repo")) {
      error "Please Check the Input repo URL ${gitUrl} : Requested repository does not exist, or you do not have permission to access it"
    } else {
      error "Job Failed while cloning due to Issue : ${sStackTrace}"
    }
  }
}