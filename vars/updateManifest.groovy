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

          def originalManifest = readYaml file: "manifest.yml"

          sh(stdout: true, command: "cat ${originalManifest}")
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