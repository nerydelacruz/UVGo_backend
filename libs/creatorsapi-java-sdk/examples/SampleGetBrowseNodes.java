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
import com.amazon.creators.model.GetBrowseNodesRequestContent;
import com.amazon.creators.model.GetBrowseNodesResponseContent;
import com.amazon.creators.model.GetBrowseNodesResource;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample class demonstrating how to use the CreatorsAPI Java SDK for GetBrowseNodes API
 * GetBrowseNodes operation retrieves information about browse nodes (product categories)
 * including their hierarchical structure, ancestor/child relationships, and descriptive names.
 */
public class SampleGetBrowseNodes {
    
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
            // Create GetBrowseNodes request
            GetBrowseNodesRequestContent getBrowseNodesRequest = new GetBrowseNodesRequestContent();
            
            // Enter your partner tag (store/tracking id)
            getBrowseNodesRequest.setPartnerTag("<YOUR PARTNER TAG>");
            
            // Specify browse node id(s)
            List<String> browseNodeIds = new ArrayList<String>();
            browseNodeIds.add("3040");
            browseNodeIds.add("1");
            browseNodeIds.add("3045");
            getBrowseNodesRequest.setBrowseNodeIds(browseNodeIds);
            
            /**
             * Choose resources you want from GetBrowseNodesResource enum
             * For more details, refer: https://affiliate-program.amazon.com/creatorsapi/docs/en-us/api-reference/operations/get-browse-nodes#resources-parameter
             */
            List<GetBrowseNodesResource> resources = new ArrayList<GetBrowseNodesResource>();
            resources.add(GetBrowseNodesResource.ANCESTOR);
            resources.add(GetBrowseNodesResource.CHILDREN);
            getBrowseNodesRequest.setResources(resources);

            // Call the GetBrowseNodes API
            GetBrowseNodesResponseContent response = api.getBrowseNodes(marketplace, getBrowseNodesRequest);

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
