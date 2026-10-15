package com.UVGgo.backend.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Registro tal como lo entrega BrightData. Cada scraper o dataset nombra
 * distinto sus campos, por eso se aceptan los nombres más comunes.
 * El precio puede venir como número, texto ("$1,299.90") u objeto {value, currency}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoBrightData(
        @JsonAlias({"title", "product_name", "name"}) String titulo,
        @JsonAlias({"description", "product_description"}) String descripcion,
        @JsonAlias({"image_url", "image", "main_image"}) String imagen,
        @JsonAlias({"final_price", "price", "initial_price"}) Object precio,
        @JsonAlias("currency") String moneda,
        @JsonAlias({"seller_name", "brand", "source"}) String tienda,
        @JsonAlias({"url", "product_url", "product_page_url"}) String url,
        @JsonAlias({"asin", "product_id", "sku", "id"}) String idExterno,
        @JsonAlias({"categories", "category"})
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<String> categorias) {
}
