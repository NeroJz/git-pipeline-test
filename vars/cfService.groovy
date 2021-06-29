import jenkins.model.Jenkins
import groovy.json.*


def call(Closure body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  pipeline {
    agent any
    environment {
      jsonFileName = "${config.jsonFileName}"
      jsonResetName = 'xsSecurityReset.json'
    }
    stages {
      stage('Checkout') {
        steps {
          echo "Starting function to clone repository"

          checkout scm

          echo "The json filename: ${jsonFileName}"

          echo "Starting function to generate security reset jason"
          createResetJson("${jsonFileName}") //function call for creating reset json
          def jsonContents = readJSON file: jsonFileName
          def jsonReset = readJSON file: jsonResetName

          def nonEnvAwareValue = jsonContents.xsappname
          def nonEnvResetValue = jsonReset.xsappname

          echo "${nonEnvAwareValue}"
          echo "\n"
          echo "${nonEnvResetValue}"
        }
      }
    }
  }
}

def createResetJson (jsonFileName){

  script{
      def jsonContentsReset = readJSON file: jsonFileName
      if (jsonContentsReset.has('role-collections')){
          jsonContentsReset.remove('role-collections')
          def jsonStr = JsonOutput.prettyPrint(JsonOutput.toJson(jsonContentsReset))
          writeFile(file: 'xsSecurityReset.json', text: jsonStr)

          sh 'cat xsSecurityReset.json'
      }
      else {
          throw new Exception("Build failed as role-collections not found under ${jsonFileName}")
      }
  }
}