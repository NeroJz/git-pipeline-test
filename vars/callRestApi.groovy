import groovy.json.JsonSlurperClassic

def call(Closure config) {
  config()
  def tenantName = "${config.tenantname_dev}".trim()
  String[] tenants = tenantName.split(",");

  def jobName = "${env.JOB_NAME}".replace('%2F', '_')

  if(jobName.endsWith('dev')) {
    tenantName = "${config.tenantname_dev}".trim()
    tenants = tenantName.split(",");
  } else if(jobName.endsWith('intg') && config.tenantname_intg != null) {
    tenantName = "${config.tenantname_intg}".trim()
    tenants = tenantName.split(",");
  }

  echo "${tenants.toString()}"

  node() {
    stage('Checkout') {
      checkout scm

      // sh '''
      //   pwd;ls -l
      //   Pattern='project": "projects/'
      //   grep -F "${Pattern}" ./angular.json |awk -F "/" {'print $2'} > ./list.txt
      //   cat ./list.txt
      //   mkdir ./projects
      //   echo 'Checkout started for Projects'
      // '''
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