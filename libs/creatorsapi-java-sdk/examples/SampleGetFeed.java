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
import com.amazon.creators.model.GetFeedRequestContent;
import com.amazon.creators.model.GetFeedResponseContent;

/**
 * Sample class demonstrating how to use the CreatorsAPI Java SDK for GetFeed API
 * GetFeed operation retrieves a specific product feed file that your store has access to.
 */
public class SampleGetFeed {
    
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
            // Create GetFeed request
            GetFeedRequestContent getFeedRequest = new GetFeedRequestContent();
            
            // Add Feed name (can be found from ListFeeds API response)
            getFeedRequest.setFeedName("<YOUR FILENAME>");

            // Call the GetFeed API
            GetFeedResponseContent response = api.getFeed(marketplace, getFeedRequest);

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
