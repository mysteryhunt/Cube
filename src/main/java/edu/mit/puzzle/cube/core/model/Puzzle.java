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
 * contain only one answer. It is possible that some puzzles may be partially solvable and require
 * multiple distinct answers to be entered, in which case the answers property will contain
 * multiple answers. It is also possible that solving a puzzle is determined by an external
 * interaction, not by entering an answer, in which case the answers property may not exist or be
 * empty.
 *
 * The answers property will be filtered to only contain answers a team has solved when returning
 * puzzle metadata to solving teams.
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

    @AutoValue
    public static abstract class AnswersProperty extends Puzzle.Property {
        static {
            registerClass(AnswersProperty.class);
        }

        @JsonCreator
        public static AnswersProperty create(
                @JsonProperty("answers") List<Answer> answers
        ) {
            return new AutoValue_Puzzle_AnswersProperty(ImmutableList.copyOf(answers));
        }

        @JsonProperty("answers") public abstract ImmutableList<Answer> getAnswers();
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

        @JsonDeserialize(using=PuzzlePropertiesDeserializer.class)
        @JsonProperty("puzzleProperties")
        public abstract Builder setPuzzleProperties(@Nullable Map<String, Property> puzzleProperties);

        public abstract ImmutableMap.Builder<String, Property> puzzlePropertiesBuilder();

        public <T extends Property> Builder addPuzzleProperty(Class<T> propertyClass, T value) {
            puzzlePropertiesBuilder().put(propertyClass.getSimpleName(), value);
            return this;
        }

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

    public static Builder builder(String puzzleId, String answer) {
        return new AutoValue_Puzzle.Builder()
                .setPuzzleId(puzzleId)
                .addPuzzleProperty(
                        AnswersProperty.class,
                        AnswersProperty.create(Answer.createSingle(answer))
                );
    }

    public abstract Builder toBuilder();

    public static Puzzle create(String puzzleId, String answer) {
        return builder(puzzleId, answer).build();
    }

    @SuppressWarnings("unchecked")
    @Nullable
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
    @JsonProperty("puzzleProperties")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract ImmutableMap<String, Property> getPuzzleProperties();

    @Nullable
    @JsonIgnore
    public String getDisplayName() {
        DisplayNameProperty displayNameProperty = getPuzzleProperty(DisplayNameProperty.class);
        if (displayNameProperty == null) {
            return null;
        }
        return displayNameProperty.getDisplayName();
    }

    @Nullable
    @JsonIgnore
    public List<Answer> getAnswers() {
        AnswersProperty answersProperty = getPuzzleProperty(AnswersProperty.class);
        if (answersProperty == null) {
            return null;
        }
        return answersProperty.getAnswers();
    }

    // Return a copy of this puzzle with properties that should not be visible to the current
    // solving team removed.
    public Puzzle strip(Visibility visibility) {
        ImmutableMap.Builder<String, Property> puzzlePropertiesBuilder = ImmutableMap.builder();
        if (getPuzzleProperties() != null) {
            for (Map.Entry<String, Property> entry : getPuzzleProperties().entrySet()) {
                if (entry.getValue() instanceof AnswersProperty) {
                    if (SecurityUtils.getSubject().isPermitted(new AnswersPermission())) {
                        puzzlePropertiesBuilder.put(entry.getKey(), entry.getValue());
                    } else {
                        AnswersProperty answersProperty = (AnswersProperty) entry.getValue();
                        puzzlePropertiesBuilder.put(
                                entry.getKey(),
                                AnswersProperty.create(answersProperty.getAnswers().stream()
                                        .filter(a -> visibility.getSolvedAnswers().contains(a.getCanonicalAnswer()))
                                        .collect(Collectors.toList()))
                        );
                    }
                } else {
                    if (entry.getValue().getVisibilityRequirement().contains(visibility.getStatus())) {
                        puzzlePropertiesBuilder.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return toBuilder().setPuzzleProperties(puzzlePropertiesBuilder.build()).build();
    }
}
