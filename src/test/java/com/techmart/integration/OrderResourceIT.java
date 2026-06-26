package com.techmart.integration;

import io.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class OrderResourceIT extends AbstractIntegrationTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createBaseDeployment("techmart-orders-it.war");
    }

    private Long registerTestUser() {
        String uniqueUser = "orduser_" + System.currentTimeMillis();
        Map<String, String> body = new HashMap<>();
        body.put("username", uniqueUser);
        body.put("email", uniqueUser + "@test.com");
        body.put("password", "Test1234!");
        body.put("firstName", "Order");
        body.put("lastName", "TestUser");

        return given()
            .contentType(ContentType.JSON)
            .body(body)
            .when().post(getBaseUri() + "/api/users/register")
            .then().statusCode(201)
            .extract().path("data.id");
    }

    @Test
    void testPlaceOrder() {
        Long userId = registerTestUser();

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("userId", userId);
        orderRequest.put("shippingAddress", "123 Test Street, Test City");
        orderRequest.put("billingAddress", "123 Test Street, Test City");
        orderRequest.put("paymentMethod", "CREDIT_CARD");
        orderRequest.put("notes", "Integration test order");

        Map<String, Object> item = new HashMap<>();
        item.put("productId", 1);
        item.put("quantity", 2);

        orderRequest.put("items", List.of(item));

        given()
            .contentType(ContentType.JSON)
            .body(orderRequest)
            .when().post(getBaseUri() + "/api/orders")
            .then().statusCode(201)
            .body("success", equalTo(true))
            .body("data.orderNumber", notNullValue())
            .body("data.status", equalTo("PENDING"));
    }

    @Test
    void testPlaceOrder_EmptyOrder() {
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("userId", 1);
        orderRequest.put("shippingAddress", "123 Test St");
        orderRequest.put("paymentMethod", "CREDIT_CARD");
        orderRequest.put("items", List.of());

        given()
            .contentType(ContentType.JSON)
            .body(orderRequest)
            .when().post(getBaseUri() + "/api/orders")
            .then().statusCode(400)
            .body("success", equalTo(false))
            .body("errorCode", equalTo("EMPTY_ORDER"));
    }

    @Test
    void testGetAllOrders() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 20)
            .when().get(getBaseUri() + "/api/orders")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }

    @Test
    void testGetOrderById_NotFound() {
        given()
            .when().get(getBaseUri() + "/api/orders/999999")
            .then().statusCode(404)
            .body("success", equalTo(false))
            .body("errorCode", equalTo("ORDER_NOT_FOUND"));
    }

    @Test
    void testGetOrdersByStatus() {
        given()
            .when().get(getBaseUri() + "/api/orders/status/PENDING")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }

    @Test
    void testGetOrdersByStatus_Invalid() {
        given()
            .when().get(getBaseUri() + "/api/orders/status/INVALID_STATUS_XYZ")
            .then().statusCode(400)
            .body("success", equalTo(false))
            .body("errorCode", equalTo("INVALID_STATUS"));
    }

    @Test
    void testGetOrdersByUser() {
        Long userId = registerTestUser();

        given()
            .when().get(getBaseUri() + "/api/orders/user/" + userId)
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }

    @Test
    void testCancelOrder() {
        Long userId = registerTestUser();

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("userId", userId);
        orderRequest.put("shippingAddress", "123 Test St");
        orderRequest.put("paymentMethod", "CREDIT_CARD");

        Map<String, Object> item = new HashMap<>();
        item.put("productId", 1);
        item.put("quantity", 1);
        orderRequest.put("items", List.of(item));

        Long orderId = given()
            .contentType(ContentType.JSON)
            .body(orderRequest)
            .when().post(getBaseUri() + "/api/orders")
            .then().statusCode(201)
            .extract().path("data.id");

        Map<String, String> cancelBody = new HashMap<>();
        cancelBody.put("reason", "Integration test cancellation");

        given()
            .contentType(ContentType.JSON)
            .body(cancelBody)
            .when().put(getBaseUri() + "/api/orders/" + orderId + "/cancel")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("CANCELLED"));
    }

    @Test
    void testUpdateOrderStatus() {
        Long userId = registerTestUser();

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("userId", userId);
        orderRequest.put("shippingAddress", "123 Test St");
        orderRequest.put("paymentMethod", "CREDIT_CARD");

        Map<String, Object> item = new HashMap<>();
        item.put("productId", 1);
        item.put("quantity", 1);
        orderRequest.put("items", List.of(item));

        Long orderId = given()
            .contentType(ContentType.JSON)
            .body(orderRequest)
            .when().post(getBaseUri() + "/api/orders")
            .then().statusCode(201)
            .extract().path("data.id");

        Map<String, String> statusBody = new HashMap<>();
        statusBody.put("status", "CONFIRMED");

        given()
            .contentType(ContentType.JSON)
            .body(statusBody)
            .when().put(getBaseUri() + "/api/orders/" + orderId + "/status")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("CONFIRMED"));
    }

    @Test
    void testGetTodayRevenue() {
        given()
            .when().get(getBaseUri() + "/api/orders/revenue/today")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }

    @Test
    void testCountByStatus() {
        given()
            .when().get(getBaseUri() + "/api/orders/count/PENDING")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }
}
