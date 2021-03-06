/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Apache 2.0 License
 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
     http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 
See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */

/**
 * QueryODataWriter.java
 */
package com.microsoft.windowsazure.mobileservices.table.query;

import android.util.Pair;

import com.microsoft.windowsazure.mobileservices.table.MobileServiceTableSystemPropertiesProvider;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class QueryODataWriter {

    /**
     * Returns the OData string representation of the query
     */
    public static String getRowFilter(Query query) {
        QueryNodeODataWriter oDataWriter = new QueryNodeODataWriter();

        if (query != null && query.getQueryNode() != null) {
            query.getQueryNode().accept(oDataWriter);
        }

        return oDataWriter.getBuilder().toString();
    }

    /**
     * Returns the OData string representation of the rowset's modifiers
     *
     * @throws java.io.UnsupportedEncodingException
     */
    public static String getRowSetModifiers(Query query, MobileServiceTableSystemPropertiesProvider table) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();

        if (query != null) {
            if (query.hasInlineCount()) {
                sb.append("&$inlinecount=allpages");
            }

            if (query.getTop() > 0) {
                sb.append("&$top=");
                sb.append(query.getTop());
            }

            //Allow Skip=0 to work around a SQL ordering issue #571
            if (query.getSkip() >= 0) {
                sb.append("&$skip=");
                sb.append(query.getSkip());
            }

            if (query.getOrderBy().size() > 0) {
                sb.append("&$orderby=");

                boolean first = true;
                for (Pair<String, QueryOrder> order : query.getOrderBy()) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }

                    sb.append(QueryNodeODataWriter.percentEncode(order.first, "!$&'()*+,;=:@")); // odataIdentifier
                    sb.append("%20");
                    sb.append(order.second == QueryOrder.Ascending ? "asc" : "desc");

                }
            }

            if (query.hasDeleted()) {
                sb.append("&__includeDeleted=true");
            }
        }

        List<Pair<String, String>> parameters = table.addSystemProperties(table.getSystemProperties(), query != null ? query.getUserDefinedParameters() : null);

        for (Pair<String, String> parameter : parameters) {
            if (parameter.first != null) {
                sb.append("&");

                String key = parameter.first;
                String value = parameter.second;
                if (value == null)
                    value = "null";

                sb.append(QueryNodeODataWriter.percentEncode(key, "!$'()*,;:@")); // customName
                sb.append("=");
                sb.append(QueryNodeODataWriter.percentEncode(value, "!$'()*,;=:@")); // customValue
            }
        }

        if (query != null && query.getProjection() != null && query.getProjection().size() > 0) {
            sb.append("&$select=");

            boolean first = true;
            for (String field : query.getProjection()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }

                sb.append(QueryNodeODataWriter.percentEncode(field, "!$&'()*,;=:@")); // odataIdentifier
            }
        }

        return sb.toString();
    }
}
