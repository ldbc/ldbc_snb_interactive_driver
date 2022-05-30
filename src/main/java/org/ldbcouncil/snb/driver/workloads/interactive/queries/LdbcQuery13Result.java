package org.ldbcouncil.snb.driver.workloads.interactive.queries;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LdbcQuery13Result {
    private final int shortestPathLength;

    public LdbcQuery13Result(
        @JsonProperty("shortestPathLength") int shortestPathLength
    )
    {
        this.shortestPathLength = shortestPathLength;
    }

    public int getShortestPathLength() {
        return shortestPathLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LdbcQuery13Result that = (LdbcQuery13Result) o;

        if (shortestPathLength != that.shortestPathLength) return false;

        return true;
    }

    @Override
    public String toString() {
        return "LdbcQuery13Result{" +
                "shortestPathLength=" + shortestPathLength +
                '}';
    }
}
