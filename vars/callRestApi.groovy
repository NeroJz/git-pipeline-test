def call(Map config) {
  def tenantName = "${config.tenantname}".trim()
  String[] tenants = tenantName.split(",");
  node() {
    stage('Checkout') {
      checkout scm
    }
    stage('Test') {
      echo 'Hello from callRestApi'

      // stdout = bat(returnStdout: true, script: 'curl -X GET http://127.0.0.1:5000/api/service/hello')

      // echo "${stdout.trim()}"

      stdout = sh(returnStdout: true, script: "ls -l")
      echo "${stdout.trim()}"

      for(String tenant : tenants) {
        def tenantElement = "${tenant}"
        echo "Starting Process for Tenant: ${tenantElement}"

        def folder_path = "./${tenantElement}"

        existsTenant = fileExists "${tenantElement}"

        echo "\tFile ${tenantElement}: ${existsTenant.toString()}"

      }

      // JenkinsClient client = JenkinsClient.builder()
      //   .endPoint("http://127.0.0.1:5000/api/service/hello")
      //   .build()
      
      // def systemInfo = client.api().systemApi().systemInfo()

      // echo "${systemInfo}"
    }
  }
}