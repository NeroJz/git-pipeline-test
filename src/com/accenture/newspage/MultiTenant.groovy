package com.accenture.newspage;


def processTenantFromFolder(String appName, String replacedTenantFolderName, String tenantNameElement) 
{
  sh "cp -r ${appName} ${appName}-bkp"
  sh "ls -la"
  //sh "cp -Rv ${tenantName}/* ${appName}"
  pwd = sh(returnStdout:true,script : 'pwd').replaceAll("[\\n ]", "")
  dir (replacedTenantFolderName) {
    sh 'ls l'
  }
}