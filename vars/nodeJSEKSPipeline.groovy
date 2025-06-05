def call(Map configMap){   // 
    pipeline {
        agent { label 'jenkins-agent' }
        environment { 
            PROJECT = configMap.get('project')
            COMPONENT = configMap.get('component')
            appVersion = ''
            ACC_ID = '010526266250'
        }
        options {
            disableConcurrentBuilds()
            timeout(time: 30, unit: 'MINUTES')
        }
        parameters{
            booleanParam(name: 'deploy', defaultValue: false, description: 'Toggle this value')
        }
        stages {
            stage('Read Version') {
                steps {
                    script{
                        def packageJson = readJSON file: 'package.json'
                        appVersion = packageJson.version
                        echo "Version is: $appVersion"
                    }
                }
            }
            stage('Install Dependencies') {
                steps {
                    script { 
                        sh 'npm install'
                    }
                }
            }

            // stage('unit test') {
            //     steps {
            //         script{ 
            //             sh """
            //                 echo 'this will run when the developers will created'
            //             """
            //         }
            //     }
            // }
            // stage('Run Sonarqube') {
            //     environment {
            //         scannerHome = tool 'sonar-scanner-7.1';
            //     }
            //     steps {
            //     withSonarQubeEnv('sonar-scanner-7.1') {
            //         sh "${scannerHome}/bin/sonar-scanner"
            //         // This is generic command works for any language
            //     }
            //     }
            // }
            // stage("Quality Gate") {
            //     steps {
            //     timeout(time: 1, unit: 'HOURS') {
            //         waitForQualityGate abortPipeline: true
            //     }
            //     }
            // }
            stage('Docker Build') {
                steps {
                script{
                    withAWS(region: 'us-east-1', credentials: 'aws') {
                        sh """
                        aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com

                        docker build -t  ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appVersion} .

                        docker push ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appVersion}
                        """
                    }
                    
                }
                }
            }
            // stage('Trigger Deploy'){
            //     when { 
            //         expression { params.deploy }
            //     }
            //     steps{
            //         build job: 'helm', parameters: [string(name: 'version', value: "${appVersion}")], wait: true
            //     }
            // }
        }
        post { 
            always { 
                echo 'I will always say Hello again!'
                deleteDir()
            }
            failure { 
                echo 'I will run when pipeline is failed'
            }
            success { 
                echo 'I will run when pipeline is success'
            }
        }
    }
}