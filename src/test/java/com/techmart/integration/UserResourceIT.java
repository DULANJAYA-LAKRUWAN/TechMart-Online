package com.techmart.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserResourceIT extends AbstractIntegrationTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createBaseDeployment("techmart-users-it.war");
    }

    private Long registerTestUser() {
        String uniqueUser = "ituser_" + System.currentTimeMillis();
        Map<String, String> body = new HashMap<>();
        body.put("username", uniqueUser);
        body.put("email", uniqueUser + "@test.com");
        body.put("password", "Test1234!");
        body.put("firstName", "Integration");
        body.put("lastName", "TestUser");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(body)
            .when().post(getBaseUri() + "/api/users/register")
            .then().statusCode(201)
            .body("success", equalTo(true))
            .body("data.username", equalTo(uniqueUser))
            .extract().response();

        Long id = response.path("data.id");
        assertNotNull(id);
        return id;
    }

    @Test
    void testRegisterUser() {
        registerTestUser();
    }

    @Test
    void testRegisterUser_MissingFields() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "incomplete");

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when().post(getBaseUri() + "/api/users/register")
            .then().statusCode(400)
            .body("success", equalTo(false))
            .body("errorCode", equalTo("MISSING_FIELDS"));
    }

    @Test
    void testGetUserById() {
        Long id = registerTestUser();

        given()
            .when().get(getBaseUri() + "/api/users/" + id)
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.id", equalTo(id.intValue()));
    }

    @Test
    void testGetUserById_NotFound() {
        given()
            .when().get(getBaseUri() + "/api/users/999999")
            .then().statusCode(404)
            .body("success", equalTo(false))
            .body("errorCode", equalTo("USER_NOT_FOUND"));
    }

    @Test
    void testGetUserByEmail() {
        String uniqueUser = "ituser_email_" + System.currentTimeMillis();
        String email = uniqueUser + "@test.com";
        Map<String, String> body = new HashMap<>();
        body.put("username", uniqueUser);
        body.put("email", email);
        body.put("password", "Test1234!");
        body.put("firstName", "Email");
        body.put("lastName", "Test");

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when().post(getBaseUri() + "/api/users/register")
            .then().statusCode(201);

        given()
            .when().get(getBaseUri() + "/api/users/email/" + email)
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.email", equalTo(email));
    }

    @Test
    void testGetAllUsers() {
        given()
            .when().get(getBaseUri() + "/api/users")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }

    @Test
    void testAuthenticate() {
        String uniqueUser = "ituser_auth_" + System.currentTimeMillis();
        String password = "Pass1234!";
        Map<String, String> body = new HashMap<>();
        body.put("username", uniqueUser);
        body.put("email", uniqueUser + "@test.com");
        body.put("password", password);
        body.put("firstName", "Auth");
        body.put("lastName", "Test");

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when().post(getBaseUri() + "/api/users/register")
            .then().statusCode(201);

        Map<String, String> authBody = new HashMap<>();
        authBody.put("username", uniqueUser);
        authBody.put("password", password);

        given()
            .contentType(ContentType.JSON)
            .body(authBody)
            .when().post(getBaseUri() + "/api/users/authenticate")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.username", equalTo(uniqueUser));
    }

    @Test
    void testUpdateUser() {
        Long id = registerTestUser();

        Map<String, String> updateBody = new HashMap<>();
        updateBody.put("firstName", "UpdatedFirst");
        updateBody.put("lastName", "UpdatedLast");
        updateBody.put("phone", "555-0100");

        given()
            .contentType(ContentType.JSON)
            .body(updateBody)
            .when().put(getBaseUri() + "/api/users/" + id)
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.firstName", equalTo("UpdatedFirst"));
    }

    @Test
    void testAssignRole() {
        Long id = registerTestUser();

        Map<String, String> roleBody = new HashMap<>();
        roleBody.put("role", "ADMIN");

        given()
            .contentType(ContentType.JSON)
            .body(roleBody)
            .when().post(getBaseUri() + "/api/users/" + id + "/roles")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("message", equalTo("Role assigned"));
    }

    @Test
    void testRemoveRole() {
        Long id = registerTestUser();

        Map<String, String> roleBody = new HashMap<>();
        roleBody.put("role", "ADMIN");

        given()
            .contentType(ContentType.JSON)
            .body(roleBody)
            .when().post(getBaseUri() + "/api/users/" + id + "/roles")
            .then().statusCode(200);

        given()
            .when().delete(getBaseUri() + "/api/users/" + id + "/roles/ADMIN")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("message", equalTo("Role removed"));
    }

    @Test
    void testChangePassword() {
        Long id = registerTestUser();

        Map<String, String> pwBody = new HashMap<>();
        pwBody.put("oldPassword", "Test1234!");
        pwBody.put("newPassword", "NewPass1234!");

        given()
            .contentType(ContentType.JSON)
            .body(pwBody)
            .when().post(getBaseUri() + "/api/users/" + id + "/change-password")
            .then().statusCode(200)
            .body("success", equalTo(true));
    }

    @Test
    void testDeactivateUser() {
        Long id = registerTestUser();

        given()
            .when().delete(getBaseUri() + "/api/users/" + id)
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("message", equalTo("User deactivated"));
    }

    @Test
    void testCountUsers() {
        given()
            .when().get(getBaseUri() + "/api/users/count")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }
}
