package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.mit.puzzle.cube.core.permissions.AnswersPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * Model for something that's solvable by a team. This could be a normal puzzle, or a metapuzzle,
 * or a live event, etc.
 *
 * Usually a puzzle is solved by entering a single answer, so the answers property will usually
 * have a length of 1. It is possible that some puzzles may be partially solvable and require
 * multiple distinct answers to be entered, in which case the answers property will have a length
 * greater than 1. It is also possible that solving a puzzle is determined by an external
 * interaction, not by entering an answer, in which case the answers property may be empty.
 *
 * The answers property will be omitted when returning puzzle metadata to solving teams.
 */
@AutoValue
@JsonDeserialize(builder = AutoValue_Puzzle.Builder.class)
public abstract class Puzzle {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new GuavaModule());

    public static abstract class Property {
        private static Map<String, Class<? extends Property>> propertyClasses = new HashMap<>();

        protected static void registerClass(Class<? extends Property> propertyClass) {
            propertyClasses.put(propertyClass.getSimpleName(), propertyClass);
        }

        public static Class<? extends Property> getClass(String propertyClassName) {
            return propertyClasses.get(propertyClassName);
        }

        // A solving team will only be shown this property if the solving team's current visibility
        // for this puzzle is in the visibilityRequirement() set. By default, puzzle properties are
        // never shown to solving teams.
        @JsonIgnore
        public Set<String> getVisibilityRequirement() {
            return ImmutableSet.of();
        }
    }

    @AutoValue
    public static abstract class DisplayNameProperty extends Puzzle.Property {

        static {
            registerClass(DisplayNameProperty.class);
        }

        @JsonCreator
        public static DisplayNameProperty create(
                @JsonProperty("displayName") String displayName,
                @JsonProperty("visibilityRequirement") Set<String> visibilityRequirement
        ) {
            return new AutoValue_Puzzle_DisplayNameProperty(
                    displayName, ImmutableSet.copyOf(visibilityRequirement));
        }

        @JsonProperty("displayName") public abstract String getDisplayName();

        @JsonIgnore(false)
        @JsonProperty("visibilityRequirement")
        public abstract Set<String> getVisibilityRequirement();
    }

    public static class PuzzlePropertiesDeserializer extends StdDeserializer<Map<String, Property>> {
        private static final long serialVersionUID = 1L;

        public PuzzlePropertiesDeserializer() {
            this(null);
        }

        public PuzzlePropertiesDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Map<String, Property> deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);
            ImmutableMap.Builder<String, Property> properties = ImmutableMap.builder();
            node.fields().forEachRemaining(entry -> {
                String propertyClassName = entry.getKey();
                Class<? extends Property> propertyClass = Property.getClass(propertyClassName);
                if (propertyClass == null) {
                    throw new ResourceException(
                            Status.CLIENT_ERROR_BAD_REQUEST,
                            String.format("Unknown puzzle property type '%s'", entry.getKey()));
                }
                try {
                    String propertyValue = OBJECT_MAPPER.writeValueAsString(entry.getValue());
                    properties.put(entry.getKey(), OBJECT_MAPPER.readValue(propertyValue, propertyClass));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return properties.build();
        }
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty("puzzleId")
        public abstract Builder setPuzzleId(String puzzleId);

        @JsonProperty("answers")
        public abstract Builder setAnswers(@Nullable List<Answer> answers);

        @JsonDeserialize(using=PuzzlePropertiesDeserializer.class)
        @JsonProperty("puzzleProperties")
        public abstract Builder setPuzzleProperties(@Nullable Map<String, Property> puzzleProperties);

        abstract Puzzle autoBuild();

        public Puzzle build() {
            Puzzle puzzle = autoBuild();
            if (puzzle.getPuzzleProperties() != null) {
                for (Map.Entry<String, Property> entry : puzzle.getPuzzleProperties().entrySet()) {
                    Class<? extends Property> propertyClass = Property.getClass(entry.getKey());
                    Preconditions.checkNotNull(
                            propertyClass,
                            "Puzzle property class %s is not registered",
                            entry.getKey());
                    Preconditions.checkState(
                            propertyClass.isInstance(entry.getValue()),
                            "Puzzle property object %s has wrong type",
                            entry.getKey());
                }
            }
            return puzzle;
        }
    }

    public static Builder builder() {
        return new AutoValue_Puzzle.Builder();
    }

    public abstract Builder toBuilder();

    public static Puzzle create(String puzzleId, String answer) {
        return builder()
                .setPuzzleId(puzzleId)
                .setAnswers(Answer.createSingle(answer))
                .build();
    }

    @SuppressWarnings("unchecked")
    public <T extends Property> T getPuzzleProperty(Class<T> propertyClass) {
        if (getPuzzleProperties() == null) {
            return null;
        }
        Property property = getPuzzleProperties().get(propertyClass.getSimpleName());
        if (property != null) {
            return (T) property;
        }
        return null;
    }

    @JsonProperty("puzzleId")
    public abstract String getPuzzleId();

    @Nullable
    @JsonProperty("answers")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract List<Answer> getAnswers();

    @Nullable
    @JsonProperty("puzzleProperties")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract Map<String, Property> getPuzzleProperties();

    // Return a copy of this puzzle with properties that should not be visible to the current
    // solving team removed.
    public Puzzle strip(Visibility visibility) {
        Puzzle.Builder builder = toBuilder();
        if (!SecurityUtils.getSubject().isPermitted(new AnswersPermission())) {
            builder.setAnswers(ImmutableList.copyOf(
                    Iterables.filter(getAnswers(), a -> visibility.getSolvedAnswers().contains(a.getCanonicalAnswer()))));
        }
        if (getPuzzleProperties() != null) {
            builder.setPuzzleProperties(getPuzzleProperties().entrySet().stream()
                    .filter(entry -> {
                        Puzzle.Property property = entry.getValue();
                        return property.getVisibilityRequirement().contains(visibility.getStatus());
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        return builder.build();
    }
}
