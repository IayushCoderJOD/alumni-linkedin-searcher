package com.alumni.searcher.client.phantombuster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record ResultObjectResponse(String resultObject) {
}
