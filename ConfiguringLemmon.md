# NOTES #

lemmon uses the Apache VFS API to support different file systems.<br />
Therefore properties that specify the location of files or folders MUST include the file system protocol.

  * The felix configuration properties file must be located at WEB-INF/conf/config.properties inside your GAE .WAR file

  * The **org.osgi.framework.system.packages.extra** property should include the following packages:
    * javax.servlet;version="2.5.0"
    * org.osgi.service.http
> In the future it will not be required to include these packages in config.properties, lemmon will programatically set them internally.

  * The **org.osgi.framework.storage** property must be set in WEB-INF/conf/config.properties to a path that includes the the file system protocol.
    * Use org.osgi.framework.storage=gds:///felix-cache for the Google Datastore backed persistent bundle cache.
    * Use org.osgi.framework.storage=ram:///felix-cache for the non-persistent bundle cache.
> <font color='red'><b>The gds:// protocol is not implemented yet.</b></font>


  * The **felix.auto.start** property MUST also include the file system protocol.
    * Use the 'platform' protocol to tell lemmon to load a bundle from the WEB-INF folder in the GAE .war file.<br /> For example:
```
   felix.auto.start.1= \
     platform:///bundle/org.apache.felix.shell-1.2.0.jar \
     platform:///bundle/org.apache.felix.shell.tui-1.2.0.jar \
     platform:///bundle/org.apache.felix.bundlerepository-1.4.0.jar 
```