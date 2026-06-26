package com.techmart.integration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class MonitoringResourceIT extends AbstractIntegrationTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createBaseDeployment("techmart-monitor-it.war");
    }

    @Test
    void testHealthCheck() {
        given()
            .when().get(getBaseUri() + "/api/monitor/health")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("UP"))
            .body("data.app", equalTo("TechMart Online"))
            .body("data.version", equalTo("1.0.0"));
    }

    @Test
    void testGetMetrics() {
        given()
            .when().get(getBaseUri() + "/api/monitor/metrics")
            .then().statusCode(200)
            .body("success", equalTo(true));
    }

    @Test
    void testGetSlowOperations() {
        given()
            .queryParam("threshold", 500)
            .when().get(getBaseUri() + "/api/monitor/slow")
            .then().statusCode(200)
            .body("success", equalTo(true));
    }
}
