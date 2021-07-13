def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  pipeline {
    agent any
    stages {
      stage('Checkout') {
        steps {
          checkout scm

          echo "Checkout done..."
        }
      }
      stage('Read Manifest') {
        steps {
          echo "Read manifest..."
        }
      }
      stage('Update Manifest') {
        steps {
          echo "Update manifest..."
        }
      }
    }
  }
}