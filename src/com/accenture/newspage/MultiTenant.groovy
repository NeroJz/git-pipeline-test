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
    
  }
}