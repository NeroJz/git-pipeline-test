def call(Map config) {
  node() {
    stage('Test') {
      steps {
        echo 'Hello from callRestApi'
      }
    }
  }
}