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
import com.amazon.creators.model.GetItemsRequestContent;
import com.amazon.creators.model.GetItemsResponseContent;
import com.amazon.creators.model.GetItemsResource;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample class demonstrating how to use the CreatorsAPI Java SDK for GetItems API
 * GetItems operation retrieves item information for specified ASINs including
 * images, item info, offers, and other detailed product data.
 */
public class SampleGetItems {
    
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
            // Create GetItems request
            GetItemsRequestContent getItemsRequest = new GetItemsRequestContent();
            
            // Enter your partner tag (store/tracking id)
            getItemsRequest.setPartnerTag("<YOUR PARTNER TAG>");
            
            // Choose item id(s) - ASINs to retrieve
            List<String> itemIds = new ArrayList<String>();
            itemIds.add("B0DLFMFBJW");
            itemIds.add("B0BFC7WQ6R");
            itemIds.add("B00ZV9RDKK");
            getItemsRequest.setItemIds(itemIds);
            
            /**
             * Choose resources you want from GetItemsResource enum
             * For more details, refer: https://affiliate-program.amazon.com/creatorsapi/docs/en-us/api-reference/operations/get-items#resources-parameter
             */
            List<GetItemsResource> resources = new ArrayList<GetItemsResource>();
            resources.add(GetItemsResource.IMAGES_PRIMARY_MEDIUM);
            resources.add(GetItemsResource.ITEM_INFO_TITLE);
            resources.add(GetItemsResource.ITEM_INFO_FEATURES);
            resources.add(GetItemsResource.OFFERS_V2_LISTINGS_PRICE);
            resources.add(GetItemsResource.OFFERS_V2_LISTINGS_AVAILABILITY);
            resources.add(GetItemsResource.OFFERS_V2_LISTINGS_CONDITION);
            resources.add(GetItemsResource.OFFERS_V2_LISTINGS_MERCHANT_INFO);
            getItemsRequest.setResources(resources);

            // Call the GetItems API
            GetItemsResponseContent response = api.getItems(marketplace, getItemsRequest);

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
