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
import com.amazon.creators.model.GetVariationsRequestContent;
import com.amazon.creators.model.GetVariationsResponseContent;
import com.amazon.creators.model.GetVariationsResource;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample class demonstrating how to use the CreatorsAPI Java SDK for GetVariations API
 * GetVariations operation retrieves variations of a product (like different colors, sizes, etc.)
 * along with detailed information including images, item info, offers, and other product data.
 */
public class SampleGetVariations {
    
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
            // Create GetVariations request
            GetVariationsRequestContent getVariationsRequest = new GetVariationsRequestContent();
            
            // Enter your partner tag (store/tracking id)
            getVariationsRequest.setPartnerTag("<YOUR PARTNER TAG>");
            
            // Choose ASIN for which to retrieve variations
            getVariationsRequest.setAsin("B0DLFMFBJW");
            
            /**
             * Choose resources you want from GetVariationsResource enum
             * For more details, refer: https://affiliate-program.amazon.com/creatorsapi/docs/en-us/api-reference/operations/get-variations#resources-parameter
             */
            List<GetVariationsResource> resources = new ArrayList<GetVariationsResource>();
            resources.add(GetVariationsResource.IMAGES_PRIMARY_MEDIUM);
            resources.add(GetVariationsResource.ITEM_INFO_TITLE);
            resources.add(GetVariationsResource.OFFERS_V2_LISTINGS_AVAILABILITY);
            resources.add(GetVariationsResource.OFFERS_V2_LISTINGS_CONDITION);
            resources.add(GetVariationsResource.OFFERS_V2_LISTINGS_DEAL_DETAILS);
            resources.add(GetVariationsResource.OFFERS_V2_LISTINGS_IS_BUY_BOX_WINNER);
            resources.add(GetVariationsResource.OFFERS_V2_LISTINGS_LOYALTY_POINTS);
            resources.add(GetVariationsResource.OFFERS_V2_LISTINGS_MERCHANT_INFO);
            resources.add(GetVariationsResource.OFFERS_V2_LISTINGS_PRICE);
            resources.add(GetVariationsResource.OFFERS_V2_LISTINGS_TYPE);
            resources.add(GetVariationsResource.VARIATION_SUMMARY_VARIATION_DIMENSION);
            getVariationsRequest.setResources(resources);

            // Call the GetVariations API
            GetVariationsResponseContent response = api.getVariations(marketplace, getVariationsRequest);

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
