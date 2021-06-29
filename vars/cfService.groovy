
def call(Closure body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  pipeline {
    agent {
      label 'nodejs10x'
    }
    stages {
      stage('Checkout') {
        steps {
          echo "Starting function to clone repository"

          checkout scm
        }
      }
    }
  }
}