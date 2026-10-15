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
import com.amazon.creators.model.ListReportsResponseContent;

/**
 * Sample class demonstrating how to use the CreatorsAPI Java SDK for ListReports API
 * ListReports operation retrieves a list of available reports for your store.
 */
public class SampleListReports {
    
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
            // Call the ListReports API
            ListReportsResponseContent response = api.listReports(marketplace);

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
