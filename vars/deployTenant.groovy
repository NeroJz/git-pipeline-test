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
      prepareFolderStructure(appName)
    }
    stage('Copy and Replace for Each Tenant') {

      def multiTenant = new com.accenture.newspage.MultiTenant()

      for(String tenant: tenants) {
        echo "Processing Tenant--->$tenant"
        def tenantNameElement = "${tenant}"

        def tenantNameWithoutEnv = replacedTenantName(tenantNameElement)

        def existsTenant = fileExists "tenant/${tenantNameWithoutEnv}"

        echo "Tenant Folder (${tenantNameElement}): ${existsTenant}"

        if(existsTenant && tenantNameWithoutEnv == 'kara') {
          echo "Processing Copy and Replace..."
          try {
            def replacedTenantFolderName = "/tenant/${tenantNameWithoutEnv}"
            multiTenant.processTenantFromFolder(appName, replacedTenantFolderName, tenantNameElement)
          } catch (Exception ex) {
            error "Processing Copy and Replace For tenant failed: ${tenantNameElement}"
          }
        } else {
          echo "Perform normal process"
        }
      }
    }
    stage('Test Tenantname Based on Job Name') {
      echo "$env.JOB_NAME"
    }
  }
}

def prepareFolderStructure(String appName) {
  sh "cp -r ${appName} ${appName}-original-bkp"

  sh "cp -r ${appName}/src/tenant tenant"

  sh "rm -r ${appName}/src/tenant"

  sh "ls -la"
}


def replacedTenantName(String tenant) {
  /*
    The function remove -dev or -intg from the
    tenant using regular expression
  */

  def replacedTenant = tenant.trim().replaceAll(/(-dev|-intg)/, "")

  return replacedTenant
}


def getAllFileCommand() {
  // return $/ find . -maxdepth 5 -type f -not -path */.* | sort /$
}