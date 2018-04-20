node("x2go") {
  deleteDir()
  checkout scm
  stage("Create Certificates") {
      sh "mkdir -p client/java/certificate"
      sh "keytool -genkey -keystore client/java/certificate/keystore -alias ulteo -dname \"CN=manconsulting.co.uk, OU=MAN, O=MAN, L=UK, S=UK, C=UK\"   -storepass 123456  -keypass 123456"
      sh "keytool -selfcert -keystore client/java/certificate/keystore -alias ulteo -storepass 123456 -keypass 123456"
  }
  stage("Build") {
    dir("client/java/") {
        sh "./autogen"
        sh "ant ovdNativeClient.jar"
        sh "ant ovdIntegratedLauncher.jar"
    }
    dir("client/java/jars") {
        sh "mv ../../../openjdk/* ."
        sh "/usr/bin/unzip java-1.8.0-openjdk-1.8.0.161-3.b14.el6_9.x86_64.zip"
        archiveArtifacts '*.jar'
    }
    
    dir("client/OVDIntegratedLauncher"){
        sh "./autogen"
        sh "make"
        sh "mv -f UlteoOVDIntegratedLauncher ADSIntegratedLauncher"
        archiveArtifacts 'ADSIntegratedLauncher'
    }
    
   dir("client/java/jars") {
   parallel (
     "Linux64" : {
       sh "java-1.8.0-openjdk-1.8.0.161-3.b14.el6_9.x86_64/bin/java -jar packr.jar --platform linux64 --jdk openjdk-1.7.0-u80-unofficial-linux-amd64-installer.zip --executable OVDNativeClient --classpath OVDNativeClient.jar --mainclass org.ulteo.ovd.client.NativeClient --output ADSNativeClient_linux64"
       sh "cp -r ../../OVDIntegratedLauncher/ADSIntegratedLauncher ."
       sh "zip -r ADSNativeClient_linux64.zip ADSNativeClient_linux64"
     },
     "Linux32" : {
       sh "java-1.8.0-openjdk-1.8.0.161-3.b14.el6_9.x86_64/bin/java -jar packr.jar --platform linux32 --jdk openjdk-1.7.0-u80-unofficial-linux-i586-image.zip --executable OVDNativeClient --classpath OVDNativeClient.jar --mainclass org.ulteo.ovd.client.NativeClient --output ADSNativeClient_linux32"
       sh "cp -r ../../OVDIntegratedLauncher/ADSIntegratedLauncher ."
       sh "zip -r ADSNativeClient_linux32.zip ADSNativeClient_linux32"

       },
     "Windows64" : {
       sh "java-1.8.0-openjdk-1.8.0.161-3.b14.el6_9.x86_64/bin/java -jar packr.jar --platform windows64 --jdk openjdk-1.7.0-u80-unofficial-windows-amd64-image.zip --executable OVDNativeClient --classpath OVDNativeClient.jar --mainclass org.ulteo.ovd.client.NativeClient --output ADSNativeClient_Windows64"
       sh "cp -r ../windlls.zip ADSNativeClient_Windows64"
       dir("ADSNativeClient_Windows64") {
         sh "unzip windlls.zip && rm -rf windlls.zip"
       }
       sh "zip -r ADSNativeClient_Windows64.zip ADSNativeClient_Windows64"
     },
     "Windows32" : {
       sh "java-1.8.0-openjdk-1.8.0.161-3.b14.el6_9.x86_64/bin/java -jar packr.jar --platform windows32 --jdk openjdk-1.7.0-u80-unofficial-windows-i586-image.zip --executable OVDNativeClient --classpath OVDNativeClient.jar --mainclass org.ulteo.ovd.client.NativeClient --output ADSNativeClient_Windows32"
       sh "cp -r ../windlls.zip ADSNativeClient_Windows32"
       dir("ADSNativeClient_Windows32") {
         sh "unzip windlls.zip && rm -rf windlls.zip"
       }
       sh "zip -r ADSNativeClient_Windows32.zip ADSNativeClient_Windows32"
       },
     "Mac" : {
       sh "java-1.8.0-openjdk-1.8.0.161-3.b14.el6_9.x86_64/bin/java -jar packr.jar --platform mac --jdk openjdk-1.7.0-u80-unofficial-macosx-x86_64-image.zip --executable OVDNativeClient --classpath OVDNativeClient.jar --mainclass org.ulteo.ovd.client.NativeClient --output ADSNativeClient_mac.app"
       sh "zip -r ADSNativeClient_mac.zip ADSNativeClient_mac.app"

     }
   )
   }
   dir("client/java/jars") {
       archiveArtifacts 'ADSNativeClient_*.zip'
   }
   stage("Installers & Package") {
   dir("client/java/jars") {
   parallel (
     "Windows64 Installer" : {
     },
     "Windows32 Installer" : {
       }
   )
   }
   }

    

  }
}
