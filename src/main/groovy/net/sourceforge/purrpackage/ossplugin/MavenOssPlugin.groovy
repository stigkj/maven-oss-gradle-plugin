package net.sourceforge.purrpackage.ossplugin

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.testng.TestNGOptions;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.LogLevel;
import de.huxhorn.gradle.pgp.PgpPlugin;
import org.gradle.api.plugins.MavenPlugin;

class MavenOssPlugin implements Plugin<Project> {

      PgpPlugin pgp = new PgpPlugin();
      MavenPlugin maven = new MavenPlugin();
      Properties secrets = new Properties();

      public void apply( Project p ) {
         def conventionObject = new MavenOssPluginConvention();
         p.convention.plugins.mavenSecret = conventionObject;
         maven.apply( p )
         pgp.apply( p );

         def javadocTask = p.task( [type: Javadoc], "mavenOssPluginJavadoc" )
         javadocTask.configure( {
              source project.sourceSets.main.allJava 
              destinationDir = new File( p.buildDir, "javadoc" );
         } )

         def packageJavadoc = p.task( [type: Jar], "packageJavadoc" );
         packageJavadoc.configure( {
              dependsOn( javadocTask )
              from javadocTask.destinationDir 
              classifier = 'javadoc' 
         } );

         def packageSources = p.task( [type: Jar], "packageSources" );        
         packageSources.configure( {
           classifier = 'sources'
           from p.sourceSets.main.allSource
         } )
         p.artifacts.archives packageSources
         p.artifacts.archives packageJavadoc
         String home = System.properties["user.home"];
         File f = p.file( "${home}/maven.secret.properties" )
         FileInputStream fis = new FileInputStream( f );
         try {
             secrets.load( fis );
         }
         finally {
             fis.close();
         }
         def x = p.convention.plugins.pgp;
         x.pgp {
           secretKeyRingFile = new File("${home}/.gnupg/secring.gpg")
           keyId = secrets["gpgKeyId"];
           password = secrets["gpgKeyPassword"];
         }
         
         def uploadTask = p.getByName( "uploadArchives" )
         def installTask = p.getByName( "install" );
         installTask.dependsOn( packageSources );
         def repUrl = secrets["repositoryUrl"]
         def repUser = secrets["repositoryUser"]
         def repPw = secrets["repositoryPassword"]
         def mavenDeployer = uploadTask.repositories.mavenDeployer { 
           repository(url: repUrl) {
             authentication(userName: repUser, password: repPw)
           }
         }
         conventionObject.mavenDeployer = mavenDeployer;
         conventionObject.mavenInstaller = installTask.repositories.mavenInstaller;
      } 

      class MavenOssPluginConvention {

          def pomConfigurator = new PomConfigurator();

          def mavenDeployer;
          def mavenInstaller;

          def myDevelopers = []
          def myLicenses = []

          class PomConfigurator {
            def configurePom( Closure c ) {
                  
                def targets = [mavenInstaller, mavenDeployer]
                targets.each { 
                   if ( myDevelopers.size() > 0 ) {
                      it.pom { project { developers { 
                        myDevelopers.each( {d ->
                          developer { 
                            name d[0]
                            email d[1] }  } )
                        } } } }
                   if ( myLicenses.size() > 0 ) {
                      it.pom { project { licenses { 
                        myLicenses.each( {lic ->
                          license { 
                            name lic[0]
                            url lic[1] 
                            distribution lic[2]
                          }  } )
                        } } } }                       
                   c.call( it ) };
            }

           def gnuLicense() {
             myLicenses.add( [ 
                        "GNU Public License, Version 2", 
                        "http://www.gnu.org/licenses/gpl.txt",
                        "repo"
                       ] );
           }
           def apacheLicense() {
             myLicenses.add( [
                        "The Apache Public License, Version 2",
                        "http://www.apache.org/licenses/LICENSE-2.0.txt",
                        "repo"] );
           }

           def addDeveloper( devName, devEmail ) {
              myDevelopers.add( [devName, devEmail] );
           }

      }
   }
}
