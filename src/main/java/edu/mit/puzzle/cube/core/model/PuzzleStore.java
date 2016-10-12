package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.google.common.collect.Table;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.events.PuzzlePropertyChangeEvent;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A repository for all puzzle metadata, including the answers.
 */
@Singleton
public class PuzzleStore {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new GuavaModule());

    private final ConnectionFactory connectionFactory;
    private final EventProcessor<Event> eventProcessor;

    @Inject
    public PuzzleStore(
            ConnectionFactory connectionFactory,
            EventProcessor<Event> eventProcessor
    ) {
        this.connectionFactory = connectionFactory;
        this.eventProcessor = eventProcessor;
    }

    public void initializePuzzles(List<Puzzle> puzzles) {
        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement insertPuzzleStatement = connection.prepareStatement(
                        "INSERT INTO puzzles (puzzleId) VALUES (?)");
                PreparedStatement insertPuzzlePropertyStatement = connection.prepareStatement(
                        "INSERT INTO puzzle_properties (puzzleId, propertyKey, propertyValue) VALUES (?,?,?)");
                PreparedStatement insertPuzzleIndexablePropertyStatement = connection.prepareStatement(
                        "INSERT INTO puzzle_indexable_properties (puzzleId, propertyKey, propertyValue) VALUES (?,?,?)")
        ) {
            for (Puzzle puzzle : puzzles) {
                insertPuzzleStatement.setString(1, puzzle.getPuzzleId());
                insertPuzzleStatement.executeUpdate();

                insertPuzzlePropertyStatement.setString(1, puzzle.getPuzzleId());
                insertPuzzleIndexablePropertyStatement.setString(1, puzzle.getPuzzleId());
                for (Entry<String, Puzzle.Property> entry : puzzle.getPuzzleProperties().entrySet()) {
                    insertPuzzlePropertyStatement.setString(2, entry.getKey());
                    insertPuzzleIndexablePropertyStatement.setString(2, entry.getKey());
                    try {
                        insertPuzzlePropertyStatement.setString(
                                3,
                                OBJECT_MAPPER.writeValueAsString(entry.getValue())
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    insertPuzzlePropertyStatement.executeUpdate();
                    Optional<String> indexableValue = entry.getValue().getIndexableValue();
                    if (indexableValue.isPresent()) {
                        insertPuzzleIndexablePropertyStatement.setString(3, indexableValue.get());
                        insertPuzzleIndexablePropertyStatement.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @AutoValue
    @JsonDeserialize(builder = AutoValue_PuzzleStore_PuzzlePropertiesRow.Builder.class)
    abstract static class PuzzlePropertiesRow {
        @AutoValue.Builder
        abstract static class Builder {
            @JsonProperty("puzzleId") abstract Builder setPuzzleId(String puzzleId);
            @JsonProperty("propertyKey") abstract Builder setPropertyKey(String propertyKey);
            @JsonProperty("propertyValue") abstract Builder setPropertyValue(String propertyValue);
            abstract PuzzlePropertiesRow build();
        }
        @JsonProperty("puzzleId") abstract String getPuzzleId();
        @JsonProperty("propertyKey") abstract String getPropertyKey();
        @JsonProperty("propertyValue") abstract String getPropertyValue();
    }

    private Map<String, Map<String, Puzzle.Property>> deserializePuzzleProperties(
            List<PuzzlePropertiesRow> puzzlePropertiesResults
    ) {
        Map<String, Map<String, Puzzle.Property>> allPuzzleProperties = new HashMap<>();
        for (PuzzlePropertiesRow row : puzzlePropertiesResults) {
            Map<String, Puzzle.Property> puzzleProperties = allPuzzleProperties.get(row.getPuzzleId());
            if (puzzleProperties == null) {
                puzzleProperties = new HashMap<>();
                allPuzzleProperties.put(row.getPuzzleId(), puzzleProperties);
            }

            Class<? extends Puzzle.Property> propertyClass = Puzzle.Property.getClass(row.getPropertyKey());
            if (propertyClass == null) {
                throw new RuntimeException(String.format("Unknown puzzle property class '%s'", row.getPropertyKey()));
            }
            try {
                Puzzle.Property property = OBJECT_MAPPER.readValue(row.getPropertyValue(), propertyClass);
                puzzleProperties.put(row.getPropertyKey(), property);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return allPuzzleProperties;
    }

    public Puzzle getPuzzle(String puzzleId) {
        Puzzle puzzle;
        try {
            List<Puzzle> retrievedPuzzles = DatabaseHelper.query(
                    connectionFactory,
                    "SELECT * FROM puzzles WHERE puzzleId = ?",
                    Lists.newArrayList(puzzleId),
                    Puzzle.class
            );
            if (retrievedPuzzles.isEmpty()) {
                Table<Integer, String, Object> displayIdResult = DatabaseHelper.query(
                        connectionFactory,
                        "SELECT * FROM puzzle_indexable_properties WHERE propertyValue = ? AND propertyKey = 'DisplayIdProperty'",
                        Lists.newArrayList(puzzleId)
                );

                if (!displayIdResult.isEmpty()) {
                    String aliasedPuzzleId = (String) Iterables.getOnlyElement(displayIdResult.rowMap().values()).get("puzzleId");
                    return getPuzzle(aliasedPuzzleId);
                }
            }
            //This will throw an exception if retrievedPuzzles is empty and the process got to this point
            puzzle = Iterables.getOnlyElement(retrievedPuzzles);
        } catch (Exception e) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_NOT_FOUND.getCode(),
                    String.format("Failed to access puzzle id %s: %s", puzzleId, e));
        }

        List<PuzzlePropertiesRow> puzzlePropertiesResults = DatabaseHelper.query(
                connectionFactory,
                "SELECT puzzleId, propertyKey, propertyValue FROM puzzle_properties " +
                        "WHERE puzzleId = ?",
                Lists.newArrayList(puzzleId),
                PuzzlePropertiesRow.class
        );
        Map<String, Puzzle.Property> puzzleProperties =
                deserializePuzzleProperties(puzzlePropertiesResults).get(puzzleId);
        if (puzzleProperties != null && !puzzleProperties.isEmpty()) {
            puzzle = puzzle.toBuilder()
                    .setPuzzleProperties(puzzleProperties)
                    .build();
        }

        return puzzle;
    }

    public Map<String, Puzzle> getPuzzles() {
        List<Puzzle> puzzles = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM puzzles",
                Lists.newArrayList(),
                Puzzle.class
        );
        List<PuzzlePropertiesRow> puzzlePropertiesResults = DatabaseHelper.query(
                connectionFactory,
                "SELECT puzzleId, propertyKey, propertyValue FROM puzzle_properties",
                Lists.newArrayList(),
                PuzzlePropertiesRow.class
        );
        Map<String, Map<String, Puzzle.Property>> allPuzzleProperties =
                deserializePuzzleProperties(puzzlePropertiesResults);
        return puzzles.stream()
                .map(puzzle -> {
                    Map<String, Puzzle.Property> puzzleProperties =
                            allPuzzleProperties.get(puzzle.getPuzzleId());
                    if (puzzleProperties != null && !puzzleProperties.isEmpty()) {
                        return puzzle.toBuilder()
                                .setPuzzleProperties(puzzleProperties)
                                .build();
                    }
                    return puzzle;
                })
                .collect(Collectors.toMap(Puzzle::getPuzzleId, Function.identity()));
    }

    public boolean setPuzzleProperty(
            String puzzleId,
            Class<? extends Puzzle.Property> propertyClass,
            Puzzle.Property property) {
        String propertyKey = propertyClass.getSimpleName();
        Preconditions.checkArgument(
                propertyClass.isInstance(property),
                "Puzzle property is not an instance of %s",
                propertyKey);
        String propertyValue;
        try {
            propertyValue = OBJECT_MAPPER.writeValueAsString(property);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        boolean changed = false;
        Optional<String> indexablePropertyValue = property.getIndexableValue();

        Optional<Integer> generatedId = DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO puzzle_properties (puzzleId, propertyKey, propertyValue) SELECT ?, ?, ? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM puzzle_properties WHERE puzzleId = ? AND propertyKey = ?)",
                Lists.newArrayList(puzzleId, propertyKey, propertyValue, puzzleId, propertyKey));
        if (generatedId.isPresent()) {
            changed = true;
            if (indexablePropertyValue.isPresent()) {
                DatabaseHelper.insert(
                        connectionFactory,
                        "INSERT INTO puzzle_indexable_properties (puzzleId, propertyKey, propertyValue) SELECT ?, ?, ? " +
                                "WHERE NOT EXISTS (SELECT 1 FROM puzzle_properties WHERE puzzleId = ? AND propertyKey = ?)",
                        Lists.newArrayList(puzzleId, propertyKey, indexablePropertyValue.get(), puzzleId, propertyKey));
            }
        } else {
            int updates = DatabaseHelper.update(
                    connectionFactory,
                    "UPDATE puzzle_properties SET propertyValue = ? " +
                            "WHERE puzzleId = ? AND propertyKey = ?",
                            Lists.newArrayList(propertyValue, puzzleId, propertyKey)
            );
            changed = updates > 0;
            if (changed && indexablePropertyValue.isPresent()) {
                DatabaseHelper.update(
                        connectionFactory,
                        "UPDATE puzzle_indexable_properties SET propertyValue = ? " +
                                "WHERE puzzleId = ? AND propertyKey = ?",
                        Lists.newArrayList(indexablePropertyValue.get(), puzzleId, propertyKey)
                );
            }
        }

        if (changed) {
            eventProcessor.process(PuzzlePropertyChangeEvent.builder()
                    .setPuzzle(getPuzzle(puzzleId))
                    .build());
        }

        return changed;
    }
}
