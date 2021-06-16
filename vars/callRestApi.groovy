def call(Map config) {
  def tenantName = "${config.tenantname}".trim()
  String[] tenants = tenantName.split(",");
  node() {
    stage('Checkout') {
      checkout scm
    }
    stage('Test Directory') {
      stdout = sh(returnStdout: true, script: "ls -l")
      echo "${stdout.trim()}"

      for(String tenant : tenants) {
        String tenantElement = "${tenant.trim()}"
        echo "Starting Process for Tenant: ${tenantElement}"

        def exists = fileExists tenantElement

        echo "\tFile ${tenantElement}: ${exists.toString()}"

      }
    }
    stage('Patch') {
      withCredentials([usernamePassword(credentialsId: 'PRTG_CREDENTIAL', 
        usernameVariable: 'PRTG_USR', 
        passwordVariable: 'PRTG_PWD')]) {
        // echo PRTG_USR
        // echo PRTG_PWD

        def data = '{"prtg.user"="' + PRTG_USR + '","prtg.password"="' + PRTG_PWD +'"}'

        echo data

        def command = "curl -d ${data} -H 'Content-Type: application/json' -X POST http://host.docker.internal:5000/api/service/hello"

        echo command
        
        def stdout = sh(returnStdout: true, script: command)

        echo "${stdout.trim()}"
      }
    }
  }
}