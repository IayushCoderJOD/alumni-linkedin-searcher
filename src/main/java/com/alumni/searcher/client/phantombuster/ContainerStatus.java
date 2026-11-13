package com.alumni.searcher.client.phantombuster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record ContainerStatus(String status, Integer exitCode) {

    boolean isFinished() {
        return "finished".equalsIgnoreCase(status);
    }

    boolean isFailed() {
        return exitCode != null && exitCode != 0;
    }
}
