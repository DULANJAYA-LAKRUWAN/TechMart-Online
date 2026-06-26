package com.techmart.integration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.net.URL;

@ExtendWith(ArquillianExtension.class)
@RunAsClient
public abstract class AbstractIntegrationTest {

    @ArquillianResource
    private URL baseURL;

    protected String getBaseUri() {
        String url = baseURL.toString();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    protected static WebArchive createBaseDeployment(String archiveName) {
        return ShrinkWrap.create(WebArchive.class, archiveName)
            .addPackages(true, "com.techmart.entity")
            .addPackages(true, "com.techmart.ejb")
            .addPackages(true, "com.techmart.rest")
            .addPackages(true, "com.techmart.service")
            .addPackages(true, "com.techmart.dto")
            .addPackages(true, "com.techmart.cdi")
            .addPackages(true, "com.techmart.jms")
            .addPackages(true, "com.techmart.exception")
            .addAsResource("META-INF/persistence.xml")
            .addAsWebInfResource(new File("src/main/webapp/WEB-INF/beans.xml"), "beans.xml")
            .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"), "web.xml")
            .addAsWebInfResource(new File("src/main/webapp/WEB-INF/jboss-web.xml"), "jboss-web.xml");
    }
}
