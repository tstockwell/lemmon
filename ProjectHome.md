Lemmon is an [OSGi](http://en.wikipedia.org/wiki/OSGi) kernel implementation....
  * that enables developers to run OSGi-based applications in Java servlet containers, especially the Google App Engine.
  * that supports some OSGi 'containerisms' such as Eclipse buddy classloading.
  * provides custom OSGi service implementations geared toward server environments.

Some of Lemmon's features include...
  * can be used in Google App Engine (and other servlet containers) via the LemmonServlet servlet.
  * can install and start bundles cached in associated .war file.
  * provides a custom OSGi HTTP service that exposes registered servlets underneath the LemmonServlet servlet's namespace
  * provides a custom OSGi Logging service that logs messages using Java Logging API.
  * provides an implementation of the OSGi RFC 138 API (Composite Frameworks) (Composite frameworks are used with lemmon to implement multi-tenant web applications whose features may be customized for individual users by including additional sets of bundles).

Lemmon was originally forked from the [Apache Felix](http://felix.apache.org/site/index.html) project since the set of features planned for Lemmon conflicted with Felix's goals of being a completely compliant, pure OSGi implementation with a small footprint.  Lemmon abandons those goals in favor of supporting upcoming cloud environments and supporting Java server development in general.

Status...this project was built, tested, and used in another project.