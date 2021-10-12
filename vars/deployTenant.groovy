def call(Closure body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def scmUrl = scm.getUserRemoteConfigs()[0].getUrl()
  def appName = ""

  def isSkipped = false

  // if(config.tenantname_dev == null) {
  //   error 'Error: tenantname_dev must be provided from Jenkins!'
  // }
  
  def tenantName = "${config.tenantname_dev}".trim()
  String[] tenants = tenantName.split(",");

  def jobName = "${env.JOB_NAME}".replace('%2F', '_')

  echo "Config: ${config.tenantname_intg?.trim()}"
  echo "Before condition"
  echo "Tenants: ${tenants.toString()}"

  if(jobName.endsWith('dev')) {
    echo "\tInside Dev job"
    tenantName = "${config.tenantname_dev}".trim()
    tenants = tenantName.split(",");
  } else if(jobName.endsWith('intg') && config.tenantname_intg != null) {
    echo "\tInside Intg job"
    tenantName = "${config.tenantname_intg}".trim()
    tenants = tenantName.split(",");
  }

  echo "IS NULL: ${tenantName == 'null'}"

  echo "After condition"
  echo "Tenants: ${tenants.toString()}"

  node() {
    stage('Checkout') {
      checkout scm

      appName = scmUrl.substring(scmUrl.lastIndexOf('/') + 1,scmUrl.length() - 4)

      echo "Appname: $appName";

      if(tenantName == 'null') {
        isSkipped = false
        echo 'Tenant name is empty. Skip the Checkout Stage.'
        return
      }

      echo "Processing Checkout"
    }
    stage('Backup Existing App') {
      sh "cp -r ${appName} ${appName}-bkp"

      sh "cp -r ${appName}/src/tenant tenant"

      sh "rm -r ${appName}/src/tenant"

      sh "ls -la"
    }
    stage('Copy tenants and ') {
      
    }
    stage('Test Tenantname Based on Job Name') {
      echo "$env.JOB_NAME"
    }
  }
}


def getReplacedTenantName(String tenant) {
  def replacedTenant = tenant.trim().replaceAll(/(-dev|-intg)/, "")

  return replacedTenant
}


def getAllFileCommand() {
  return $/ find . -maxdepth 5 -type f -not -path '*/.*' | sort /$
}