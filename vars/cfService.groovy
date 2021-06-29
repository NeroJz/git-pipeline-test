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

      envAwareJsonFile = "xs-security-env.json"
			envResetJsonfile = "xsSecurityReset.json"
    }
    stages {
      stage('Checkout') {
        steps {
          echo "Starting function to clone repository"

          checkout scm

          echo "The json filename: ${jsonFileName}"

          script {
            echo "Starting function to generate security reset json"
            createResetJson("${jsonFileName}") //function call for creating reset json
            def jsonContents = readJSON file: jsonFileName
            def jsonReset = readJSON file: jsonResetName

            def nonEnvAwareValue = jsonContents.xsappname
            def nonEnvResetValue = jsonReset.xsappname

            envAwareValue = nonEnvAwareValue +"-dev"
            envResetValue = nonEnvResetValue +"-dev"

            echo "Updated environment aware xsappname: ${envAwareValue}"
            jsonContents.xsappname = envAwareValue
            jsonReset.xsappname = envResetValue

            stdout = sh(returnStdout: true, script: "cat $jsonContents")

            echo "$stdout"

            writeJSON(file: envAwareJsonFile, json: jsonContents, pretty: 4)
            writeJSON(file: envResetJsonfile, json: jsonReset, pretty: 4)

            echo "Content: xs-security-env.json\n"
            sh 'cat xs-security-env.json'
            // echo "\nContent: xsSecurityReset.json\n"
            // sh 'cat xsSecurityReset.json'
          }
        }
      }
    }
  }
}

def createResetJson (jsonFileName){
  script{
    sh 'ls -l'
      def jsonContentsReset = readJSON file: jsonFileName
      if (jsonContentsReset.has('role-collections')){
          jsonContentsReset.remove('role-collections')
          def jsonStr = JsonOutput.prettyPrint(JsonOutput.toJson(jsonContentsReset))
          writeFile(file: 'xsSecurityReset.json', text: jsonStr)

          // sh 'cat xsSecurityReset.json'
      }
      else {
          throw new Exception("Build failed as role-collections not found under ${jsonFileName}")
      }
  }
}