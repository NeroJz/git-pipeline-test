import com.cdancy.jenkins.rest.JenkinsClient

def call(Map config) {
  node() {
    stage('Test') {
      echo 'Hello from callRestApi'

      JenkinsClient client = JenkinsClient.builder()
        .endPoint("http://127.0.0.1:5000/api/service/hello")
        .build()
      
      def systemInfo = client.api().systemApi().systemInfo()

      echo "${systemInfo}"
     }
}