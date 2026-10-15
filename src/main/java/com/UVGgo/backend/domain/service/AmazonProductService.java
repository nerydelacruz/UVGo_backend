package com.UVGgo.backend.domain.service;

import com.UVGgo.backend.config.AmazonProperties;
import com.UVGgo.backend.domain.ProductoAmazon;
import com.amazon.creators.ApiException;
import com.amazon.creators.api.DefaultApi;
import com.amazon.creators.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class AmazonProductService {
    private final DefaultApi api;
    private final AmazonProperties props;

    public AmazonProductService(DefaultApi api, AmazonProperties props) {
        this.api = api;
        this.props = props;
    }

    public List<ProductoAmazon> buscar(String palabras, int cantidad) {
        SearchItemsRequestContent req = new SearchItemsRequestContent();
        req.setPartnerTag(props.partnerTag());
        req.setKeywords(palabras);
        req.setItemCount(BigDecimal.valueOf(cantidad));
        req.setResources(List.of(
                SearchItemsResource.ITEM_INFO_TITLE,
                SearchItemsResource.IMAGES_PRIMARY_MEDIUM,
                SearchItemsResource.OFFERS_V2_LISTINGS_PRICE));
        try {
            SearchItemsResponseContent res = api.searchItems(props.marketplace(), req);
            if (res.getSearchResult() == null || res.getSearchResult().getItems() == null) return List.of();
            return res.getSearchResult().getItems().stream().map(this::aProducto).toList();
        } catch (ApiException e) {
            throw error(e);
        }
    }

    public List<ProductoAmazon> porAsin(List<String> asins) {
        GetItemsRequestContent req = new GetItemsRequestContent();
        req.setPartnerTag(props.partnerTag());
        req.setItemIds(asins);
        req.setResources(List.of(
                GetItemsResource.ITEM_INFO_TITLE,
                GetItemsResource.IMAGES_PRIMARY_MEDIUM,
                GetItemsResource.OFFERS_V2_LISTINGS_PRICE));
        try {
            GetItemsResponseContent res = api.getItems(props.marketplace(), req);
            if (res.getItemsResult() == null || res.getItemsResult().getItems() == null) return List.of();
            return res.getItemsResult().getItems().stream().map(this::aProducto).toList();
        } catch (ApiException e) {
            throw error(e);
        }
    }

    private ProductoAmazon aProducto(Item item) {
        String titulo = item.getItemInfo() != null && item.getItemInfo().getTitle() != null
                ? item.getItemInfo().getTitle().getDisplayValue() : null;
        String imagen = item.getImages() != null && item.getImages().getPrimary() != null
                && item.getImages().getPrimary().getMedium() != null
                ? item.getImages().getPrimary().getMedium().getUrl() : null;
        Money precio = null;
        if (item.getOffersV2() != null && item.getOffersV2().getListings() != null) {
            precio = item.getOffersV2().getListings().stream()
                    .map(OfferListingV2::getPrice)
                    .filter(p -> p != null && p.getMoney() != null)
                    .map(OfferPriceV2::getMoney)
                    .findFirst().orElse(null);
        }
        return new ProductoAmazon(item.getAsin(), titulo, imagen, item.getDetailPageURL(),
                precio != null ? precio.getAmount() : null,
                precio != null ? precio.getCurrency() : null);
    }

    private ResponseStatusException error(ApiException e) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al consultar Amazon: " + e.getMessage(), e);
    }
}
