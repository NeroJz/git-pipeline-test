def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def originalManifest;

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
          script {
            echo "Read manifest..."

            // try {
            //   originalManifest = readYaml file: 'manifest.yml'
            // } catch (Exception e){
            //   error "Execption on reading manifest: ${e}"
            // }

            // sh(returnStdout: true, script: "cat ${originalManifest}")
            if(originalManifest != null) {
              echo "${originalManifest}"
            }

          }
        }
      }
      stage('Manipulate Manifest') {
        steps {
          script {
            echo "Manipulate manifest..."

            if(originalManifest != null) {
              // Step 1 - Backup original manifest
              echo "\t\tPerform manipulation"

              // Step 2 - Replace the buildpack from env

              // Step 3 - Save manifest from originalManifest
            }
          }
        }
      }
    }
  }
}