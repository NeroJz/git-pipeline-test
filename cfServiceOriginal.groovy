import jenkins.model.Jenkins
import groovy.json.*

def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    
    pipeline {
        agent 
        {
            label 'nodejs10x'
        }
        options { skipDefaultCheckout() }
        environment {
            jobName = "${env.JOB_NAME}".replace('%2F', '_')
    	    serviceName = "${config.serviceName}"
            servicePlan = "${config.servicePlan}"
            serviceInstanceName = "${config.serviceInstanceName}"
            jsonFileName = "${config.jsonFileName}"
            jsonResetName = 'xsSecurityReset.json'
            restageAppName = "${config.restageApp}"
            releaseTag = "${config.releaseTag}"

            startingIntgTag="0.1.0-intg"
            startingQaTag="0.1.0-qa"
            startingDemoTag="0.1.0-demo"
			startingPerfTag="0.1.0-perf"
			startingextDemoTag="0.1.0-extDemo"

            scmUrl = scm.getUserRemoteConfigs()[0].getUrl()
            JENKINS_API_CRED = credentials('JENKINS_API')
			CF_BUILD = credentials('CF_BUILD')

            envAwareJsonFile = "xs-security-env.json"
			envResetJsonfile = "xsSecurityReset.json"
            // envAwareIdentifier = "${config.envAwareIdentifier}"
            
            // Hard coded for now
            envAwareIdentifier = "xsappname"

            cfApi = "${env.CF2_API}"
            cfOrg =  "${env.CF_DEV_ORG}"
            cfSpace = "${env.CF2_DEV_SPACE}"
            intgTags = ""
            qaTags = ""
            demoTags = ""
        }
        
        stages {
            
            stage('Checkout')
            {
                steps{
                    
                    // Hardcoded for xsuaa services                    
                    echo "Starting Job with parameters: ${serviceName} ${servicePlan} ${serviceInstanceName} ${jsonFileName}"

                    script {
                        echo "Starting function to clone repository"
                        def promoteIntg = new com.accenture.newspage.PromoteIntg();
                        if(DEPLOY_ENV == "dev") { checkout scm }

                        if(DEPLOY_ENV == "extDemo") { checkout scm }
                        
                        if(DEPLOY_ENV == "intg") {
                            def specifiedTarget = params["Tag/Commit Hash"]
                            if(specifiedTarget == "" || specifiedTarget == null){
                                cloneRepository(scmUrl,"master")
                            }
                            else if(specifiedTarget.contains(".")){
                                echo "Tag specified: ${specifiedTarget}"
                                cloneRepository(scmUrl, specifiedTarget)
                            } else {
                                echo "Commit Hash Specified : ${specifiedTarget}"
                                cloneRepository(scmUrl,specifiedTarget)
                                echo "Checking to see if the last Dev Build was successful to promote intg"
                                promoteIntg.validateBuildStatus(jobName, "${env.JENKINS_API_URL}", "${JENKINS_API_CRED_USR}", "${JENKINS_API_CRED_PSW}", "${scmUrl}", "${specifiedTarget}", this)
                            }
                        }
                        
                        if(DEPLOY_ENV == "qa") {
                            def specifiedTarget = params["Tag"]
                            if(specifiedTarget == "" || specifiedTarget == null){
                                error "Promotion to qa must be from a specific tag"
                            }
                            else{
                                if(specifiedTarget.contains(".")) {
                                    echo "Tag specified: ${specifiedTarget}"
                                    cloneRepository(scmUrl,specifiedTarget)
                                } else{
                                    error "Promotion to qa must be from a specific tag"
                                }
                            }
                        }
                        
                        if(DEPLOY_ENV == "demo") {
                            def specifiedTarget = params["Tag"]
                            if(specifiedTarget == "" || specifiedTarget == null){
                                error "Promotion to demo must be from a specific tag"
                            }
                            else{
                                if(specifiedTarget.contains(".")) {
                                    echo "Tag specified: ${specifiedTarget}"
                                    cloneRepository(scmUrl,specifiedTarget)
                                } else{
                                    error "Promotion to demo must be from a specific tag"
                                }
                            }
                        }

                        if (jobName.endsWith('perf'))
                        {
                            def specifiedTarget = params["Tag"]
                            if(specifiedTarget == "" || specifiedTarget == null){
                                error "Promotion to Perf must be from a specific tag"
                            }
                            else{
                                if(specifiedTarget.contains(".")) {
                                    echo "Tag specified: ${specifiedTarget}"
                                    cloneRepository(scmUrl,specifiedTarget)
                                } else{
                                    error "Promotion to Perf must be from a specific tag"
                                }
                            }
                        }

                        echo "Starting function to generate security reset jason"
                        createResetJson("${jsonFileName}") //function call for creating reset json
                        def jsonContents = readJSON file: jsonFileName
                        def jsonReset = readJSON file: jsonResetName
                        def nonEnvAwareValue = jsonContents.xsappname
                        def nonEnvResetValue = jsonReset.xsappname
                        if (jsonContents.has('role-collections'))
						{
							def RoleCollecArray = jsonContents.'role-collections'.name
							def countlist=RoleCollecArray.size()  
							if (jobName.endsWith('qa'))
							{ 
								for (j=0 ; j< countlist;j++){
									jsonContents.'role-collections'[j].name =RoleCollecArray[j] +"-qa"
                                }
									 
							}
							if (jobName.endsWith('dev'))
							{
								for (j=0 ; j< countlist;j++){
									jsonContents.'role-collections'[j].name =RoleCollecArray[j] +"-dev"
								}
							}
							if (jobName.endsWith('demo'))
							{
								for (j=0 ; j< countlist;j++){
									jsonContents.'role-collections'[j].name =RoleCollecArray[j] +"-demo"
								}
							}
							if (jobName.endsWith('perf'))
							{
								for (j=0 ; j< countlist;j++){
									jsonContents.'role-collections'[j].name =RoleCollecArray[j] +"-perf"
								}
							}
							if (jobName.endsWith('extDemo'))
							{
								for (j=0 ; j< countlist;j++){
									jsonContents.'role-collections'[j].name =RoleCollecArray[j] +"-extDemo"
								}
							}
                            if (jobName.endsWith('intg'))
							{
								for (j=0 ; j< countlist;j++){
									jsonContents.'role-collections'[j].name =RoleCollecArray[j] +"-intg"
								}
							}
                        }
                        else 
                        {
							echo "Role -Collections not found"
						}
                        if (jobName.endsWith('dev'))
                        {
                            envAwareValue = nonEnvAwareValue +"-dev"
                            envResetValue = nonEnvResetValue +"-dev"
                        }
                        if (jobName.endsWith('intg'))
                        {
                            envAwareValue = nonEnvAwareValue +"-intg"
                            envResetValue = nonEnvResetValue +"-intg"
                        }
                        if (jobName.endsWith('qa'))
                        {
                            envAwareValue = nonEnvAwareValue +"-qa"
                            envResetValue = nonEnvResetValue +"-qa"
                        }
                        if (jobName.endsWith('demo'))
                        {
                            envAwareValue = nonEnvAwareValue +"-demo"
                            envResetValue = nonEnvResetValue +"-demo"
                        }
                        if (jobName.endsWith('perf'))
                        {
                            envAwareValue = nonEnvAwareValue +"-perf"
                            envResetValue = nonEnvResetValue +"-perf"
                        }
                        if (jobName.endsWith('extDemo'))
                        {
                            envAwareValue = nonEnvAwareValue +"-extDemo"
                            envResetValue = nonEnvResetValue +"-extDemo"
                        }
                        
                        echo "Updated environment aware xsappname: ${envAwareValue}"
                                                    
                        jsonContents.xsappname = envAwareValue
                        jsonReset.xsappname = envResetValue

                        sh "ls -l"
                        
                        echo "Start writing JSON file: ${envAwareJsonFile}\n"
                        writeJSON(file: envAwareJsonFile, json: jsonContents, pretty: 4)

                        echo "Start writing JSON file: ${envResetJsonfile}\n"
                        writeJSON(file: envResetJsonfile, json: jsonReset, pretty: 4)

                        sh 'cat xs-security-env.json'
                        sh 'cat xsSecurityReset.json'

                        if (jobName.endsWith('dev'))
                        {

                        }
                        else if (jobName.endsWith('intg'))
                        {
                            cfOrg =  "${env.CF_INTG_ORG}"
                            cfSpace = "${env.CF_INTG_SPACE}"
                            if (DEPLOY_ENV == null)
                            {
                                echo "WARN: Manually setting DEPLOY_ENV This needs to be updated in the Job"
                                DEPLOY_ENV = "intg"
                            }
                        }
                        else if (jobName.endsWith('qa'))
                        {
                            cfOrg =  "${env.CF_QA_ORG}"
                            cfSpace = "${env.CF_QA_SPACE}"
                            if (DEPLOY_ENV == null)
                            {
                                echo "WARN: Manually setting DEPLOY_ENV This needs to be updated in the Job"
                                DEPLOY_ENV = "qa"
                            }
                        }
                        else if (jobName.endsWith('demo'))
                        {
                            cfOrg =  "${env.CF_DEMO_ORG}"
                            cfSpace = "${env.CF_DEMO_SPACE}"
                            if (DEPLOY_ENV == null)
                            {
                                echo "WARN: Manually setting DEPLOY_ENV This needs to be updated in the Job"
                                DEPLOY_ENV = "demo"
                            }
                        }
                        else if (jobName.endsWith('perf'))
                        {
                            cfOrg =  "${env.CF_PERF_ORG}"
                            cfSpace = "${env.CF_PERF_SPACE}"
                            if (DEPLOY_ENV == null)
                            {
                                echo "WARN: Manually setting DEPLOY_ENV This needs to be updated in the Job"
                                DEPLOY_ENV = "perf"
                            }
                        }
                        else if (jobName.endsWith('extDemo'))
                        {
                            cfOrg =  "${env.CF_ExtDemo_ORG}"
                            cfSpace = "${env.CF_ExTDemo_SPACE}"
                            if (DEPLOY_ENV == null)
                            {
                                echo "WARN: Manually setting DEPLOY_ENV This needs to be updated in the Job"
                                DEPLOY_ENV = "extDemo"
                            }
                        }
                    }
                }
            }
            stage('Deploy to Dev')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'dev'   
                } 
                steps {
                    withCfCli( apiEndpoint: "${env.CF2_API}",
                        skipSslValidation: true, 
                        cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                        credentialsId: 'CF_BUILD', 
                        organization: "${env.CF_DEV_ORG}", 
                        space: "${env.CF2_DEV_SPACE}") {
                        script {
                            updateServiceReset("${serviceInstanceName}","${envResetJsonfile}")
                            updateOrCreateService("${serviceInstanceName}","${envAwareJsonFile}", "${serviceName}", "${servicePlan}")
                            
                            // Restage Apps
                            def serviceGuid  = sh(returnStdout:true, script: 'cf service ${serviceInstanceName} --guid').trim()
                            def boundAppsString = sh(returnStdout:true, script: 'cf curl /v2/service_instances/' + serviceGuid + "/service_bindings")
                            def boundAppsJson = readJSON text:boundAppsString
                            try { 
                                timeout(time:300, unit: 'SECONDS')
                                {
                                    restageAppEnv = "dev"
                                    restageApp(boundAppsJson['resources'], "${env.CF2_API}", "${env.CF_DEV_ORG}", "${env.CF2_DEV_SPACE}", "${restageAppName}", "${restageAppEnv}")
                                }
                            } catch(Exception e)
                            {
                                echo "Restage bound apps timed out - restage manually"
                            }
                        }
                    }
                }
            }
            stage('Deploy to INTG')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'intg'   
                } 
                steps {
                    echo "${cfOrg}"
                    echo "${cfSpace}"
                    echo "${cfApi}"
                    withCfCli( apiEndpoint: "${env.CF2_API}",
                        skipSslValidation: true, 
                        cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                        credentialsId: 'CF_BUILD', 
                        organization: "${env.CF_INTG_ORG}", 
                        space: "${env.CF_INTG_SPACE}") {
                        script {
                            updateServiceReset("${serviceInstanceName}","${envResetJsonfile}")
                            updateOrCreateService("${serviceInstanceName}","${envAwareJsonFile}", "${serviceName}", "${servicePlan}")
                            
                            // Restage Apps
                            def serviceGuid  = sh(returnStdout:true, script: 'cf service ${serviceInstanceName} --guid').trim()
                            def boundAppsString = sh(returnStdout:true, script: 'cf curl /v2/service_instances/' + serviceGuid + "/service_bindings")
                            def boundAppsJson = readJSON text:boundAppsString
                            try { 
                                timeout(time:300, unit: 'SECONDS')
                                {
                                    restageAppEnv = "intg"
                                    restageApp(boundAppsJson['resources'], "${env.CF2_API}", "${env.CF_INTG_ORG}", "${env.CF_INTG_SPACE}", "${restageAppName}", "${restageAppEnv}")
                                }
                            } catch(Exception e)
                            {
                                echo "Restage bound apps timed out - restage manually"
                            }
                        }
                    }
                }
            }
            stage('Deploy to QA')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'qa'   
                } 
                steps {
                    echo "${cfOrg}"
                    echo "${cfSpace}"
                    echo "${cfApi}"
                    withCfCli( apiEndpoint: "${env.CF2_API}",
                        skipSslValidation: true, 
                        cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                        credentialsId: 'CF_BUILD', 
                        organization: "${env.CF_QA_ORG}", 
                        space: "${env.CF_QA_SPACE}") {
                        script {
                            updateServiceReset("${serviceInstanceName}","${envResetJsonfile}")
                            updateOrCreateService("${serviceInstanceName}","${envAwareJsonFile}", "${serviceName}", "${servicePlan}")
                            
                            // Restage Apps
                            def serviceGuid  = sh(returnStdout:true, script: 'cf service ${serviceInstanceName} --guid').trim()
                            def boundAppsString = sh(returnStdout:true, script: 'cf curl /v2/service_instances/' + serviceGuid + "/service_bindings")
                            def boundAppsJson = readJSON text:boundAppsString
                            try { 
                                timeout(time:300, unit: 'SECONDS')
                                {
                                    restageAppEnv = "qa"
                                    restageApp(boundAppsJson['resources'], "${env.CF2_API}", "${env.CF_QA_ORG}", "${env.CF_QA_SPACE}", "${restageAppName}", "${restageAppEnv}")
                                }
                            } catch(Exception e)
                            {
                                echo "Restage bound apps timed out - restage manually"
                            } 
                        }
                    }
                }
            }
            stage('Deploy to Demo')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'demo'   
                } 
                steps {
                    withCfCli( apiEndpoint: "${env.CF2_API}",
                        skipSslValidation: true, 
                        cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                        credentialsId: 'CF_BUILD', 
                        organization: "${env.CF_DEMO_ORG}", 
                        space: "${env.CF_DEMO_SPACE}") {
                        script {
                            updateServiceReset("${serviceInstanceName}","${envResetJsonfile}")
                            updateOrCreateService("${serviceInstanceName}","${envAwareJsonFile}", "${serviceName}", "${servicePlan}")
                            
                            // Restage Apps
                            def serviceGuid  = sh(returnStdout:true, script: 'cf service ${serviceInstanceName} --guid').trim()
                            def boundAppsString = sh(returnStdout:true, script: 'cf curl /v2/service_instances/' + serviceGuid + "/service_bindings")
                            def boundAppsJson = readJSON text:boundAppsString
                            try { 
                                timeout(time:300, unit: 'SECONDS')
                                {
                                    restageAppEnv = "demo"
                                    restageApp(boundAppsJson['resources'], "${env.CF2_API}", "${env.CF_DEMO_ORG}", "${env.CF_DEMO_SPACE}", "${restageAppName}", "${restageAppEnv}")
                                }
                            } catch(Exception e)
                            {
                                echo "Restage bound apps timed out - restage manually"
                            }
                        }
                    }
                }
            }
			stage('Deploy to Perf')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'perf'   
                } 
                steps {
                    withCfCli( apiEndpoint: "${env.CF2_API}",
                        skipSslValidation: true, 
                        cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                        credentialsId: 'CF_BUILD', 
                        organization: "${env.CF_PERF_ORG}", 
                        space: "${env.CF_PERF_SPACE}") {
                        script {
                            updateServiceReset("${serviceInstanceName}","${envResetJsonfile}")
                            updateOrCreateService("${serviceInstanceName}","${envAwareJsonFile}", "${serviceName}", "${servicePlan}")
                            
                            // Restage Apps
                            def serviceGuid  = sh(returnStdout:true, script: 'cf service ${serviceInstanceName} --guid').trim()
                            def boundAppsString = sh(returnStdout:true, script: 'cf curl /v2/service_instances/' + serviceGuid + "/service_bindings")
                            def boundAppsJson = readJSON text:boundAppsString
                            try { 
                                timeout(time:300, unit: 'SECONDS')
                                {
                                    restageAppEnv = "perf"
                                    restageApp(boundAppsJson['resources'], "${env.CF2_API}", "${env.CF_PERF_ORG}", "${env.CF_PERF_SPACE}", "${restageAppName}", "${restageAppEnv}")
                                }
                            } catch(Exception e)
                            {
                                echo "Restage bound apps timed out - restage manually"
                            }
                        }
                    }
                }
            }
			stage('Deploy to extDemo')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'extDemo'   
                } 
                steps {
                    withCfCli( apiEndpoint: "${env.CF2_API}",
                        skipSslValidation: true, 
                        cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                        credentialsId: 'CF_BUILD', 
                        organization: "${env.CF_ExtDemo_ORG}", 
                        space: "${env.CF_ExtDemo_SPACE}") {
                        script {
                            updateServiceReset("${serviceInstanceName}","${envResetJsonfile}")
                            updateOrCreateService("${serviceInstanceName}","${envAwareJsonFile}", "${serviceName}", "${servicePlan}")
                            
                            // Restage Apps
                            def serviceGuid  = sh(returnStdout:true, script: 'cf service ${serviceInstanceName} --guid').trim()
                            def boundAppsString = sh(returnStdout:true, script: 'cf curl /v2/service_instances/' + serviceGuid + "/service_bindings")
                            def boundAppsJson = readJSON text:boundAppsString
                            try { 
                                timeout(time:300, unit: 'SECONDS')
                                {
                                    restageAppEnv = "extdemo"
                                    restageApp(boundAppsJson['resources'], "${env.CF2_API}", "${env.CF_ExtDemo_ORG}", "${env.CF_ExtDemo_SPACE}", "${restageAppName}", "${restageAppEnv}")
                                }
                            } catch(Exception e)
                            {
                                echo "Restage bound apps timed out - restage manually"
                            }
                        }
                    }
                }
            }

            stage('Tag Perf')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'perf'   
                }
                steps {
                    // Get the latest tag and increment minor. 
                    // TODO: Put into library called incrementMinorVersion
                    script {
                        def specifiedTarget = params["Tag"]
                        def perfTags = specifiedTarget
                        echo "Creating Tag for Perf Pipeline"
                        def tagPerf = new com.accenture.newspage.TagPerf();
                        if(!perfTags.endsWith("perf")) {
                            perfTags = tagPerf.calculateTag(startingPerfTag, env)
                            sshagent (['93ef04e3-e61c-41f5-83de-5ae8b0e65bad']) { 
                                sh "git push origin $perfTags"
                            }
                        }
                        echo "About to update tags on service instance"
                        withCfCli( apiEndpoint: "${env.CF2_API}",
                        skipSslValidation: true, 
                        cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                        credentialsId: 'CF_BUILD', 
                        organization: "${cfOrg}", 
                        space: "${cfSpace}") {
                            sh 'cf update-service ${serviceInstanceName} -t "$perfTags"'
                        }           
                    }
                }
            }

            stage('Tag Intg')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'intg'   
                }
                steps {
                    // Get the latest tag and increment minor. 
                    // TODO: Put into library called incrementMinorVersion
                    script {
                        echo "Creating Tag for Intg Pipeline"
                        def specifiedTarget = params["Tag/Commit Hash"]
                        intgTags = specifiedTarget
                        def tagIntg = new com.accenture.newspage.TagIntg();
                        if(!intgTags.endsWith("intg")){
                            intgTags = tagIntg.calculateTag(startingIntgTag, env)
                            sshagent (['93ef04e3-e61c-41f5-83de-5ae8b0e65bad']) { 
                            sh "git push origin $intgTags"
                            }
                        }
                        echo "About to update tags on service instance"
                        withCfCli( apiEndpoint: "${env.CF2_API}",
                        skipSslValidation: true, 
                        cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                        credentialsId: 'CF_BUILD', 
                        organization: "${cfOrg}", 
                        space: "${cfSpace}") {
                            sh 'cf update-service ${serviceInstanceName} -t "$intgTags"'
                        } 
                    }
                }
            }

            stage('Tag extDemo')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'extDemo'   
                } 
                steps {
                    // Get the latest tag and increment minor. 
                    // TODO: Put into library called incrementMinorVersion
                    script {
                        echo "Creating Tag for ExtDemo Pipeline"
                        def tagExtDemo = new com.accenture.newspage.TagEXTDemo();
                        extDemoTags = tagExtDemo.calculateTag(startingExtDemoTag, env)

                        echo "About to tag using ${extDemoTags}"
                        sh("git config user.email ${env.GIT_EMAIL}")
                        sh("git config user.name '${env.GIT_USER}'")
                        
                        def commandTag = $/ git tag -m "Tagging by CI Build ${jobName} ${env.BUILD_NUMBER}" -a $extDemoTags /$
                        sh(returnStdout:true, script: commandTag).trim()

                        sshagent (['93ef04e3-e61c-41f5-83de-5ae8b0e65bad']) { 
                            sh "git push origin $extDemoTags"
                        }
                    
                        echo "About to update tags on service instance"
                        withCfCli( apiEndpoint: "${env.CF2_API}",
                        skipSslValidation: true, 
                        cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                        credentialsId: 'CF_BUILD', 
                        organization: "${cfOrg}", 
                        space: "${cfSpace}") {

                            sh 'cf update-service ${serviceInstanceName} -t "$extDemoTags"'

                        }    
                    }
                }
            }

            stage('Tag QA')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'qa'   
                }
                steps {
                    // Get the latest tag and increment minor. 
                    // TODO: Put into library called incrementMinorVersion
                    script {
                        echo "Creating Tag for QA Pipeline"
                        def specifiedTarget = params["Tag"]
                        qaTags = specifiedTarget
                        def tagQA = new com.accenture.newspage.TagQA();
                        if(!qaTags.endsWith('qa')) {
                            qaTags = tagQA.calculateTag(startingQATag, env)
                            sshagent (['93ef04e3-e61c-41f5-83de-5ae8b0e65bad']) { 
                                sh "git push origin $qaTags"
                            }
                        }

                        echo "About to update tags on service instance"
                        withCfCli( apiEndpoint: "${env.CF2_API}",
                            skipSslValidation: true, 
                            cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                            credentialsId: 'CF_BUILD', 
                            organization: "${env.CF_QA_ORG}", 
                            space: "${env.CF_QA_SPACE}") {
                                sh 'cf update-service ${serviceInstanceName} -t "$qaTags"'
                        }    
                    }
                }
            }

            stage('Tag Demo')
            {
                when {
                    environment name: 'DEPLOY_ENV', value: 'demo'   
                }
                steps {
                    // Get the latest tag and increment minor. 
                    // TODO: Put into library called incrementMinorVersion
                    script {
                        echo "Creating Tag for Demo Pipeline"
                        def specifiedTarget = params["Tag"]
                        demoTags = specifiedTarget
                        def tagDemo = new com.accenture.newspage.TagDemo();
                        if(!demoTags.endsWith("demo")){
                            demoTags = tagDemo.calculateTag(startingDemoTag, env)
                            sshagent (['93ef04e3-e61c-41f5-83de-5ae8b0e65bad']) { 
                                sh "git push origin $demoTags"
                            }
                        }

                        echo "About to update tags on service instance"
                        withCfCli( apiEndpoint: "${env.CF2_API}",
                        skipSslValidation: true, 
                        cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                        credentialsId: 'CF_BUILD', 
                        organization: "${cfOrg}", 
                        space: "${cfSpace}") {
                            sh 'cf update-service ${serviceInstanceName} -t "$demoTags"'
                        }    
                    }
                }
            }

            stage('Publish Release Notes'){
                when {
                    anyOf{
                        environment name: 'DEPLOY_ENV', value: 'intg'   
                        environment name: 'DEPLOY_ENV', value: 'qa'   
                        environment name: 'DEPLOY_ENV', value: 'demo'   
                    }
                }
                steps{
                    script { 
                        def currentTag = ""
                        if(DEPLOY_ENV == "intg") { currentTag = intgTags }
                        if(DEPLOY_ENV == "qa") { currentTag = qaTags }
                        if(DEPLOY_ENV == "demo") { currentTag = demoTags }
                        
                        def releaseNote = new com.accenture.newspage.ReleaseNote();
                        def changelogString = releaseNote.publishReleaseNote(currentTag, DEPLOY_ENV, config, serviceInstanceName)
                        echo "${changelogString}"
                    }
                }
            }
        }

		post {
			failure {
				script{ 
					try {
						def emailSubject = "ERROR on Build -> ${env.JOB_NAME}";
						def emailBody = "Hi User \n\nProject: ${JOB_NAME} with Build Number: ${env.BUILD_NUMBER} has failed.Please check detailed logs at - ${env.BUILD_URL}.\n\nYou are receiving this email :\n1)You might have committed code in this repository after the last successful build revision in Jenkins.It might be failing because of your commit.\n2)You might have triggered the build in Jenkins.\n3)You might be added in the Jenkinsfile to receive build failure notifications.\nPlease check once.\n\nRegards\nNewsPage DevOps Team";

						def toEmail = new com.accenture.newspage.ReleaseNote();
						toEmail.sendEmail(config, emailSubject, emailBody);
					}
					catch (Exception e)
					{	
						echo "Release Notes fail to publish"
					}
				}
			}
		}
    }
}

def restageApp(resources, api, org, space, restageAppName, restageAppEnv)
{   
    echo "Starting function to restage app"
    restageAppName = restageAppName.toLowerCase()
    restageAppNameList = restageAppName.split(',')
    restageAppEnvName = restageAppEnv
    if (restageAppNameList[0].equals("null")){
        restageAppNameList[0] = "none"
    }
    if (restageAppNameList.length == 1 && restageAppNameList[0].equals("all")){
        echo "Restage All App"
        script {
            for (int i = 0; i<resources.size(); i++){
                
                def appguid = resources[i].entity.app_guid
                withCfCli( apiEndpoint: api,
                skipSslValidation: true, 
                cloudFoundryCliVersion: 'CloudFoundry CLI 6.40.0', 
                credentialsId: 'CF_BUILD', 
                organization: org, 
                space: space) {
                // Find app name from guid
                    script {
                        try {
                            def boundAppString = sh(returnStdout:true, script: 'cf curl /v2/apps/' + appguid)
                            def boundAppJson = readJSON text:boundAppString
                            def boundAppName = boundAppJson.entity.name
                            echo "boundAppName: ${boundAppName}"
                            sh(returnStdout:true, script: 'cf restage ' + boundAppName)
                        } catch(Exception e){
                            echo "Error restaging ${appguid}. Restage manually"       
                        // TODO Set Warn             
                        }
                    }
                }
            }
        }   
    }
    
    if (restageAppNameList.length == 1 && (restageAppNameList[0].equals("") || restageAppNameList[0].equals("none"))){
        echo "No app will be restaged"
    }

    if (restageAppNameList.length >= 1 && !((restageAppNameList[0].equals("all") || restageAppNameList[0].equals("") || restageAppNameList[0].equals("none")))){
        echo "restaging ${restageAppName} app"
        script {
            String[] boundApp = restageAppName.split(",");
            for (String s: boundApp) {
                try {
                    def boundAppName = "${s}"
                    echo "boundAppName: ${boundAppName}"
                    boundAppName = boundAppNameEnv ("${boundAppName}", "${restageAppEnvName}")
                    sh(returnStdout:true, script: 'cf restage ' + boundAppName)
                }catch(Exception e)
                {
                    echo "Error restaging Restage ${boundAppName} manually"       
                            // TODO Set Warn             
                }

            }
        }
    }
}

def boundAppNameEnv (boundAppName, restageAppEnvName){
    
    if("${restageAppEnvName}" == "dev"){
        return ("${boundAppName}" + "-dev")
    }
    if("${restageAppEnvName}" == "qa"){
        return ("${boundAppName}" + "-qa")
    }
    if("${restageAppEnvName}" == "demo"){
        return ("${boundAppName}" + "-demo")
    }
    if("${restageAppEnvName}" == "perf"){
        return ("${boundAppName}" + "-perf")
    }
    if("${restageAppEnvName}" == "extDemo"){
        return ("${boundAppName}" + "-extDemo")
    }
    if("${restageAppEnvName}" == "intg"){
        return ("${boundAppName}" + "-intg")
    }
}

def createResetJson (jsonFileName){

    script{
        def jsonContentsReset = readJSON file: jsonFileName
        if (jsonContentsReset.has('role-collections')){
            jsonContentsReset.remove('role-collections')
            def jsonStr = JsonOutput.prettyPrint(JsonOutput.toJson(jsonContentsReset))
            writeFile(file: 'xsSecurityReset.json', text: jsonStr)
        }
        else {
            throw new Exception("Build failed as role-collections not found under ${jsonFileName}")
        }
    }
}

def updateServiceReset (serviceInstanceName, envResetJsonfile){

    def outfile = 'stdout.out'
    def status = sh(script:"cf update-service ${serviceInstanceName} -c ${envResetJsonfile} >${outfile} 2>&1", returnStatus:true)
    def output = readFile(outfile).trim()
    echo "Response recorded for reset update-service: ${output}"
    def messageServicePresent = "Service instance ${serviceInstanceName} not found"

    if (status == 0 ) {
      echo "updated sucessfully with reset"
    } else if(output.contains("status code: 502")) {
        echo "issue while updating ${serviceInstanceName} : Status Code: 502 : Response Recorded : ${output}"
        retryUpdate("${serviceInstanceName}", "${envResetJsonfile}")
    } else if(output.contains(messageServicePresent)) {
        echo "Service instance ${serviceInstanceName} not found So skiping the reset passing the control to create service"
    } else {
        throw new Exception("Terminating build as update-service using reset json failed")
    }
}

def updateOrCreateService (serviceInstanceName, envAwareJsonFile, serviceName, servicePlan){

    def outfile = 'stdout.out'
    def status = sh(script:"cf update-service ${serviceInstanceName} -c ${envAwareJsonFile} >${outfile} 2>&1", returnStatus:true)
    def output = readFile(outfile).trim()
    echo "Response recorded for update-service: ${output}"
    
    if (status == 0 ) {
      echo "updated sucessfully"
    }else if(output.contains("status code: 502")) {
        echo "issue while updating ${serviceInstanceName} : Status Code: 502 : Response Recorded : ${output}"
        retryUpdate("${serviceInstanceName}", "${envAwareJsonFile}")
    }else {
        echo "Service can't be updated, trying to create"
        def outfileCreate = 'stdout.out'
        def statusCreate = sh(script:"cf create-service ${serviceName} ${servicePlan} ${serviceInstanceName} -c ${envAwareJsonFile} >${outfileCreate} 2>&1", returnStatus:true)
        def outputCreate = readFile(outfileCreate).trim()
        echo "Response recorded for create-service: ${outputCreate}"
        if (statusCreate == 0 && !(output.contains("already exists"))) {
            echo "Service ${serviceName} Created successfully"
        }
        else {
            throw new Exception("Terminating build as both update service and create service command failed")
        }
    }
}

def retryUpdate (serviceInstanceName, envJsonFile){

    echo "Now retrying update command as failed due to Server error"
    def set = ""
    def count = 1
    for (i=3; i > 0; i--){
        count++
        echo "update attempt ${count}"
        def outfileRetry = 'stdout.out'
        def statusRetry = sh(script:"cf update-service ${serviceInstanceName} -c ${envJsonFile} >${outfileRetry} 2>&1", returnStatus:true)
        def outputRetry = readFile(outfileRetry).trim()
        echo "Response recorded for update-service: ${outputRetry}"
        if (statusRetry == 0 ) {
            echo "Updated sucessfully on ${count} update attempt"
            set = "passed"
                break
        }
        else{
            set = "failed"
        }
    }
    if (set.contains("failed")){
        throw new Exception("Terminating build as update service command failed even after retying for four times")
    }
}

def cloneRepository(def gitUrl, def releaseTag) {
	try {
		checkout([
			$class: 'GitSCM', 
			branches: [[
			name: releaseTag
			]], 
			doGenerateSubmoduleConfigurations: false, 
			extensions: [[
			$class:'LocalBranch', localBranch: "**"
			]], 
			submoduleCfg: [], 
			userRemoteConfigs: [[
			credentialsId: '93ef04e3-e61c-41f5-83de-5ae8b0e65bad', 
			url: gitUrl
			]]
		])
	}
	catch(Exception ex){
        String sStackTrace = ex.getMessage()
		if (sStackTrace.contains("Couldn't find any revision to build")){
			error "Please enter a valid tag or commit hash(eg: d8234c75153c6524c72806a3d5d36f76716fe30b): Verify the repository and branch configuration : for getting valid commit hash use mentioned command git log -n 1 --pretty=format:'%H' tag/commitID"
		}else if(sStackTrace.contains("Error cloning remote repo")) {
			error "Please Check the Input repo URL ${gitUrl} : Requested repository does not exist, or you do not have permission to access it"
    	}else {
			error "Job Failed while cloning due to Issue : ${sStackTrace}"
		}
	}
}