/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package examples;

import com.amazon.creators.ApiClient;
import com.amazon.creators.ApiException;
import com.amazon.creators.api.DefaultApi;
import com.amazon.creators.model.SearchItemsRequestContent;
import com.amazon.creators.model.SearchItemsResponseContent;
import com.amazon.creators.model.SearchItemsResource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample class demonstrating how to use the CreatorsAPI Java SDK for SearchItems API
 * SearchItems operation searches for items on Amazon based on search criteria like keywords
 * and returns matching products with detailed information including images, pricing, and availability.
 */
public class SampleSearchItems {
    
    public static void main(String[] args) {
        // Initialize API client
        ApiClient apiClient = new ApiClient();

        // Add credential details
        apiClient.setCredentialId("<YOUR CREDENTIAL ID>");
        apiClient.setCredentialSecret("<YOUR CREDENTIAL SECRET>");
        apiClient.setVersion("<YOUR CREDENTIAL VERSION>");
        
        // Initialize API
        DefaultApi api = new DefaultApi(apiClient);

        /**
         * Add marketplace. For more details, refer: https://affiliate-program.amazon.com/creatorsapi/docs/en-us/api-reference/common-request-headers-and-parameters#marketplace-locale-reference
         */
        String marketplace = "<YOUR MARKETPLACE>";

        try {
            // Create SearchItems request
            SearchItemsRequestContent searchItemsRequest = new SearchItemsRequestContent();
            
            // Enter your partner tag (store/tracking id)
            searchItemsRequest.setPartnerTag("<YOUR PARTNER TAG>");
            
            // Setup Request
            searchItemsRequest.setKeywords("Harry Potter");
            searchItemsRequest.setSearchIndex("Books");
            searchItemsRequest.setItemCount(BigDecimal.valueOf(2));
            
            /**
             * Choose resources you want from SearchItemsResource enum
             * For more details, refer: https://affiliate-program.amazon.com/creatorsapi/docs/en-us/api-reference/operations/search-items#resources-parameter
             */
            List<SearchItemsResource> resources = new ArrayList<SearchItemsResource>();
            resources.add(SearchItemsResource.IMAGES_PRIMARY_MEDIUM);
            resources.add(SearchItemsResource.ITEM_INFO_TITLE);
            resources.add(SearchItemsResource.OFFERS_V2_LISTINGS_AVAILABILITY);
            resources.add(SearchItemsResource.OFFERS_V2_LISTINGS_CONDITION);
            resources.add(SearchItemsResource.OFFERS_V2_LISTINGS_DEAL_DETAILS);
            resources.add(SearchItemsResource.OFFERS_V2_LISTINGS_IS_BUY_BOX_WINNER);
            resources.add(SearchItemsResource.OFFERS_V2_LISTINGS_LOYALTY_POINTS);
            resources.add(SearchItemsResource.OFFERS_V2_LISTINGS_MERCHANT_INFO);
            resources.add(SearchItemsResource.OFFERS_V2_LISTINGS_PRICE);
            resources.add(SearchItemsResource.OFFERS_V2_LISTINGS_TYPE);
            searchItemsRequest.setResources(resources);

            // Call the SearchItems API
            SearchItemsResponseContent response = api.searchItems(marketplace, searchItemsRequest);

            System.out.println("API called successfully.");
            System.out.println("Complete Response: " + response);
            
        } catch (ApiException exception) {
            System.out.println("Error calling Creators API!");
            System.out.println("Status: " + exception.getCode() + ", Message: " + exception.getMessage() + ", Response: " + exception.getResponseBody());
        } catch (Exception exception) {
            System.out.println("Unexpected error: " + exception.getMessage());
        }
    }
}
