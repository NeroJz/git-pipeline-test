def call(Map config) {
  node() {
    stage('Test') {
      echo 'Hello from callRestApi'
    }
  }
}