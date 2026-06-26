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

class ProductResourceIT extends AbstractIntegrationTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return createBaseDeployment("techmart-products-it.war");
    }

    private Long createTestProduct() {
        String uniqueSku = "IT-SKU-" + System.currentTimeMillis();
        Map<String, String> body = new HashMap<>();
        body.put("name", "Integration Test Product");
        body.put("sku", uniqueSku);
        body.put("description", "Created during integration test");
        body.put("price", "29.99");
        body.put("categoryId", "1");
        body.put("brand", "TestBrand");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(body)
            .when().post(getBaseUri() + "/api/products")
            .then().statusCode(201)
            .body("success", equalTo(true))
            .body("data.name", equalTo("Integration Test Product"))
            .body("data.sku", equalTo(uniqueSku))
            .extract().response();

        Long id = response.path("data.id");
        assertNotNull(id);
        return id;
    }

    @Test
    void testGetAllProducts() {
        given()
            .when().get(getBaseUri() + "/api/products")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }

    @Test
    void testGetProductById() {
        Long id = createTestProduct();

        given()
            .when().get(getBaseUri() + "/api/products/" + id)
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.id", equalTo(id.intValue()))
            .body("data.name", equalTo("Integration Test Product"));
    }

    @Test
    void testGetProductById_NotFound() {
        given()
            .when().get(getBaseUri() + "/api/products/999999")
            .then().statusCode(404)
            .body("success", equalTo(false))
            .body("errorCode", equalTo("PRODUCT_NOT_FOUND"));
    }

    @Test
    void testGetProductBySku() {
        String uniqueSku = "IT-SKU-BYSKU-" + System.currentTimeMillis();
        Map<String, String> body = new HashMap<>();
        body.put("name", "SKU Test Product");
        body.put("sku", uniqueSku);
        body.put("description", "Testing SKU lookup");
        body.put("price", "15.99");
        body.put("categoryId", "1");
        body.put("brand", "TestBrand");

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when().post(getBaseUri() + "/api/products")
            .then().statusCode(201);

        given()
            .when().get(getBaseUri() + "/api/products/sku/" + uniqueSku)
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.sku", equalTo(uniqueSku));
    }

    @Test
    void testSearchProducts() {
        createTestProduct();

        given()
            .queryParam("q", "Integration")
            .queryParam("page", 0)
            .queryParam("size", 20)
            .when().get(getBaseUri() + "/api/products/search")
            .then().statusCode(200)
            .body("success", equalTo(true));
    }

    @Test
    void testSearchProducts_NoKeyword() {
        given()
            .when().get(getBaseUri() + "/api/products/search")
            .then().statusCode(400)
            .body("success", equalTo(false))
            .body("errorCode", equalTo("MISSING_KEYWORD"));
    }

    @Test
    void testGetProductsByCategory() {
        given()
            .when().get(getBaseUri() + "/api/products/category/1")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }

    @Test
    void testGetFeaturedProducts() {
        given()
            .when().get(getBaseUri() + "/api/products/featured")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }

    @Test
    void testGetByPriceRange() {
        given()
            .queryParam("min", 10)
            .queryParam("max", 100)
            .when().get(getBaseUri() + "/api/products/price-range")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }

    @Test
    void testUpdateProduct() {
        Long id = createTestProduct();

        Map<String, String> updateBody = new HashMap<>();
        updateBody.put("name", "Updated Product Name");
        updateBody.put("description", "Updated description");
        updateBody.put("price", "49.99");
        updateBody.put("featured", "true");

        given()
            .contentType(ContentType.JSON)
            .body(updateBody)
            .when().put(getBaseUri() + "/api/products/" + id)
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data.name", equalTo("Updated Product Name"));
    }

    @Test
    void testDeleteProduct() {
        Long id = createTestProduct();

        given()
            .when().delete(getBaseUri() + "/api/products/" + id)
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("message", equalTo("Product deactivated"));
    }

    @Test
    void testCountProducts() {
        given()
            .when().get(getBaseUri() + "/api/products/count")
            .then().statusCode(200)
            .body("success", equalTo(true))
            .body("data", notNullValue());
    }
}
