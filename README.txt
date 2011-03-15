This plugin is a handy wrapper for the Maven and PGP plugins. It is
handy at least for projects that work with Sonatype's OSS repository.

Single point for configuring pom with pomConfigurator().

External username/authentication properties file.

The plugin adds three tasks: mavenOssPluginJavadoc creates javadocs;
task packageJavadoc creates the javadoc jar from them, and adds it to
archives artifacts; packageSources does likewise with the source jar.

