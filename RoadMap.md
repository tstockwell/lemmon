# Version 0.1 #

  * GAE implementation of OSGi Logging API.<div />Packaged as separate bundle.<div />Simply delegates to java.util.Logger (since java.util.Logger logs to Google Admin Console).

  * GAE implementation of OSGi Servlet API.<div />Exposes registered servlets underneath the felix4gae servlet's namespace.<div />Packaged as separate bundle.

  * All bundles embedded in .war file are installed when GAE is started.

  * Simple bundle cache implementation that does not store any state.<div />That means that each time GAE application is started in a JVM that only autostart bundles listed in felix.properties file are started.  Also, any bundles that are dynamically installed by other bundles will not be available in other instances of felix4gae that are started on other servers.

# Version 0.2 #

  * Enhance bundle cache implementation to save its state in Google Datastore.<div />When bundles are dynamically installed then the bundles are stored in Google Datastore.  All installed bundles are available from the BundleContext.getBundles method on all instances of felix4gae on all servers.

  * When a bundle is started in once instance of felix4gae then the same bundle should be started in all instances of felix4gae.  Starting a bundle should fail if the bundle cannot be started on all instances.