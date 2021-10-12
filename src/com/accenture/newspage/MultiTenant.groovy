package com.accenture.newspage;


def processTenantFromFolder(String appName, String replacedTenantFolderName, String tenantNameElement) 
{
  sh "cp -r ${appName} ${appName}-bkp"
  sh "ls -la"
  //sh "cp -Rv ${tenantName}/* ${appName}"
  pwd = sh(returnStdout:true,script : 'pwd').replaceAll("[\\n ]", "")
  dir (replacedTenantFolderName) {
    echo "\t${tenantNameElement} found. Running within ${pwd}"

    /* Hide for temporary */
    command =  $/ find . -maxdepth 5 -type f -not -path '*/.*' | sort /$ 
    
    listFiles = sh(returnStdout:true, script: command)
    echo listFiles
    listArray = listFiles.split('\n')
    size = listArray.size()
    if ({listArray.length >= 1}){
      for (int i = 0; i<size; i++){
        //For .csv files 
        def path = (listArray[i] - (listArray[i].substring(listArray[i].lastIndexOf('/') + 1).replaceAll("[\\n ]", ""))).substring(1)
        
        // Original logic move to copy_and_merge function

        // Test Copy
        fileName = listArray[i].substring(listArray[i].lastIndexOf('/') + 1).replaceAll("[\\n ]", "")
        path = "../../../${appName}/src${path}"
        exists = sh (returnStatus:true, script: "find ${path} -name $fileName | grep .")
        if ( exists != 0 ){
          sh (returnStdout:true, script: "cp ${listArray[i]} ${path}")
          sh "ls -l $path"
        }

      }
    }
  }
}