@Grab(group='com.cdancy', module='jenkins-rest', version='0.0.18')
import com.cdancy.jenkins.rest.JenkinsClient

def call(Map config) {
  def tenantName = "${config.tenantname}".trim()
  String[] tenants = tenantName.split(",");
  node() {
    stage('Test') {
      echo 'Hello from callRestApi'

      // stdout = bat(returnStdout: true, script: 'curl -X GET http://127.0.0.1:5000/api/service/hello')

      // echo "${stdout.trim()}"

      sh "ls -l"

      for(String tenant : tenants) {
        def tenantElement = "${tenant}"
        echo "Starting Process for Tenant: ${tenantElement}"

        def existsTenant = fileExists tenantElement

        echo "\tFile ${tenantElement}: ${existsTenant}"
      }

      // JenkinsClient client = JenkinsClient.builder()
      //   .endPoint("http://127.0.0.1:5000/api/service/hello")
      //   .build()
      
      // def systemInfo = client.api().systemApi().systemInfo()

      // echo "${systemInfo}"
    }
  }
}