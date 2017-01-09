package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
@JsonDeserialize(builder = AutoValue_HuntSpecificEvent.Builder.class)
@JsonTypeName("HuntSpecificEvent")
public abstract class HuntSpecificEvent extends Event {
    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("huntSpecificType") public abstract Builder setHuntSpecificType(String huntSpecificType);
        @JsonProperty("specification") public abstract Builder setSpecification(Map<String,Object> specification);
        public abstract HuntSpecificEvent build();
    }

    public static Builder builder() {
        return new AutoValue_HuntSpecificEvent.Builder();
    }

    @JsonProperty("huntSpecificType") public abstract String getHuntSpecificType();
    @JsonProperty("specification") public abstract Map<String,Object> getSpecification();
}
