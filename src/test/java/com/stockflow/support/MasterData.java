package com.stockflow.support;

import io.restassured.response.ValidatableResponse;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

/**
 * Helper pembuatan master data pada test lewat REST API.
 */
public final class MasterData {

    private MasterData() {
    }

    public static UUID createProduct() {
        ValidatableResponse response = given()
                .contentType(JSON)
                .body("""
                        {
                          "sku": "SKU-%s",
                          "name": "Produk %s",
                          "description": "Produk untuk pengujian",
                          "unit": "PCS"
                        }
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when().post("/api/v1/products")
                .then().statusCode(201);

        return UUID.fromString(response.extract().jsonPath().getString("data.id"));
    }

    public static UUID createWarehouse() {
        ValidatableResponse response = given()
                .contentType(JSON)
                .body("""
                        {
                          "code": "WH-%s",
                          "name": "Gudang %s",
                          "location": "Jakarta"
                        }
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when().post("/api/v1/warehouses")
                .then().statusCode(201);

        return UUID.fromString(response.extract().jsonPath().getString("data.id"));
    }
}
