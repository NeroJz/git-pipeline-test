import groovy.json.JsonSlurperClassic

def stopBuild() {
  currentBuild.getRawBuild().getExecutor().interrupt(Result.SUCCESS)
  return
}

def call(Closure body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  // if(config.tenantname_dev == null) {
  //   error 'Error: tenantname_dev must be provided from Jenkins!'
  // }
  
  def tenantName = "${config.tenantname_dev}".trim()
  String[] tenants = tenantName.split(",");

  def jobName = "${env.JOB_NAME}".replace('%2F', '_')

  echo "Config: ${config.tenantname_intg?.trim()}"
  echo "Before condition"
  echo "Tenants: ${tenants.toString()}"

  if(jobName.endsWith('dev')) {
    echo "\tInside Dev job"
    tenantName = "${config.tenantname_dev}".trim()
    tenants = tenantName.split(",");
  } else if(jobName.endsWith('intg') && config.tenantname_intg != null) {
    echo "\tInside Intg job"
    tenantName = "${config.tenantname_intg}".trim()
    tenants = tenantName.split(",");
  }

  echo "IS NULL: ${tenantName == 'null'}"

  echo "After condition"
  echo "Tenants: ${tenants.toString()}"

  node() {
    stage('Checkout') {
      checkout scm

      if(tenantName == 'null') {
        echo 'Tenant name is empty. Skip the rest of the pipeline.'
        stopBuild()
      }

      echo "Processing Checkout"

      // sh '''
      //   pwd;ls -l
      //   Pattern='project": "projects/'
      //   grep -F "${Pattern}" ./angular.json |awk -F "/" {'print $2'} > ./list.txt
      //   cat ./list.txt
      //   mkdir ./projects
      //   echo 'Checkout started for Projects'
      // '''
    }
    stage('Step 2') {
      if(tenantName == 'null') {
        echo 'Step 2 Tenant name is empty. Skip the rest of the pipeline.'
        stopBuild()
      }

      echo "Processing Step 2"
    }
    stage('Step 3') {
      if(tenantName == 'null') {
        echo 'Step 3 Tenant name is empty. Skip the rest of the pipeline.'
        stopBuild()
      }

      echo "Processing Step 3"
    }
    stage('Test Tenantname Based on Job Name') {
      echo "$env.JOB_NAME"
    }
    // stage('Test Directory') {
    //   stdout = sh(returnStdout: true, script: "ls -l")
    //   echo "${stdout.trim()}"

    //   for(String tenant : tenants) {
    //     String tenantElement = "${tenant}"
    //     echo "Starting Process for Tenant: ${tenantElement}"

    //     def replacedTenant = getReplacedTenantName(tenantElement)

    //     echo "\tgetReplacedTenantName ---> ${replacedTenant}"

    //     def exists = fileExists replacedTenant

    //     echo "\tFile ${tenantElement}: ${exists.toString()}"

    //   }
    // }
    // stage('Patch') {
    //   withCredentials([usernamePassword(credentialsId: 'PRTG_CREDENTIAL', 
    //     usernameVariable: 'PRTG_USR', 
    //     passwordVariable: 'PRTG_PWD')]) {
    //     // echo PRTG_USR
    //     // echo PRTG_PWD

    //     for(String tenant : tenants) {
    //       def data = '{"prtg.user":"' + PRTG_USR + '","prtg.password":"' + PRTG_PWD +'", "tenant":"'+ tenant.trim() +'"}'
    //       def command = 'curl -d \''+ data + '\' -H "Content-Type: application/json" -X POST http://host.docker.internal:5000/api/service/hello'

    //       echo command
          
    //       def stdout = sh(returnStdout: true, script: command)

    //       def jsonObj = new JsonSlurperClassic().parseText(stdout.trim())

    //       echo "${jsonObj.status}"

    //       echo "${stdout.trim()}"
    //     }
    //   }
    // }
  }
}


def getReplacedTenantName(String tenant) {
  // String[] envs = ['-dev', '-intg']

  // for(String pattern : envs) {
  //   def folder_name = tenant.minus(pattern)

  //   def folderExist = fileExists folder_name.trim()

  //   if(folderExist) {
  //     return folder_name
  //   }
  // }

  // return ""

  def replacedTenant = tenant.trim().replaceAll(/(-dev|-intg)/, "")

  return replacedTenant
}