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
          
          script {
            echo "Starting function to clone repository"

            echo "Starting function to generate security reset jason"
            createResetJson("${jsonFileName}") //function call for creating reset json
            def jsonContents = readJSON file: jsonFileName
            def jsonReset = readJSON file: jsonResetName
            def nonEnvAwareValue = jsonContents.xsappname
            def nonEnvResetValue = jsonReset.xsappname

            if(jsonContents.has('role-collections')) {
              def RoleCollecArray = jsonContents.'role-collections'.name
							def countlist=RoleCollecArray.size()

              for (j=0 ; j< countlist;j++){
                jsonContents.'role-collections'[j].name =RoleCollecArray[j] +"-dev"
              }
            } else {
              echo "Role -Collections not found"
            }

            envAwareValue = nonEnvAwareValue +"-dev"
            envResetValue = nonEnvResetValue +"-dev"

            echo "Updated environment aware xsappname: ${envAwareValue}"
                                                    
            jsonContents.xsappname = envAwareValue
            jsonReset.xsappname = envResetValue

            echo "Start writing JSON file: ${envAwareJsonFile}\n"
            writeJSON(file: envAwareJsonFile, json: jsonContents, pretty: 4)

            echo "Start writing JSON file: ${envResetJsonfile}\n"
            writeJSON(file: envResetJsonfile, json: jsonReset, pretty: 4)
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