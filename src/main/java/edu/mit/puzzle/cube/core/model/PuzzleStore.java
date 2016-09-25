package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;

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
import java.util.*;
import java.util.Map.Entry;
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
                        "INSERT INTO puzzle_properties (puzzleId, propertyKey, propertyValue) VALUES (?,?,?)")
        ) {
            for (Puzzle puzzle : puzzles) {
                insertPuzzleStatement.setString(1, puzzle.getPuzzleId());
                insertPuzzleStatement.executeUpdate();

                insertPuzzlePropertyStatement.setString(1, puzzle.getPuzzleId());
                for (Entry<String, Puzzle.Property> entry : puzzle.getPuzzleProperties().entrySet()) {
                    insertPuzzlePropertyStatement.setString(2, entry.getKey());
                    try {
                        insertPuzzlePropertyStatement.setString(
                                3,
                                OBJECT_MAPPER.writeValueAsString(entry.getValue())
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    insertPuzzlePropertyStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Map<String, Puzzle.Property>> deserializePuzzleProperties(
            Table<Integer, String, Object> puzzlePropertiesResults
    ) {
        Map<String, Map<String, Puzzle.Property>> allPuzzleProperties = new HashMap<>();
        for (Map<String, Object> rowMap : puzzlePropertiesResults.rowMap().values()) {
            String puzzleId = (String) rowMap.get("puzzleId");
            Map<String, Puzzle.Property> puzzleProperties = allPuzzleProperties.get(puzzleId);
            if (puzzleProperties == null) {
                puzzleProperties = new HashMap<>();
                allPuzzleProperties.put(puzzleId, puzzleProperties);
            }

            String key = (String) rowMap.get("propertyKey");
            String value = (String) rowMap.get("propertyValue");
            Class<? extends Puzzle.Property> propertyClass = Puzzle.Property.getClass(key);
            try {
                Puzzle.Property property = OBJECT_MAPPER.readValue(value, propertyClass);
                puzzleProperties.put(key, property);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return allPuzzleProperties;
    }

    public Puzzle getPuzzle(String puzzleId) {
        Puzzle puzzle;
        try {
            puzzle = Iterables.getOnlyElement(DatabaseHelper.query(
                    connectionFactory,
                    "SELECT * FROM puzzles WHERE puzzleId = ?",
                    Lists.newArrayList(puzzleId),
                    Puzzle.class
            ));
        } catch (NoSuchElementException e) {
            Optional<String> unaliasedPuzzleId = getPuzzleIdForAlias(puzzleId);
            if (unaliasedPuzzleId.isPresent()) {
                return getPuzzle(unaliasedPuzzleId.get());
            }
            throw new ResourceException(
                    Status.CLIENT_ERROR_NOT_FOUND.getCode(),
                    String.format("Failed to access puzzle id %s: %s", puzzleId, e));
        } catch (Exception e) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_NOT_FOUND.getCode(),
                    String.format("Failed to access puzzle id %s: %s", puzzleId, e));
        }

        Table<Integer, String, Object> puzzlePropertiesResults = DatabaseHelper.query(
                connectionFactory,
                "SELECT puzzleId, propertyKey, propertyValue FROM puzzle_properties " +
                        "WHERE puzzleId = ?",
                Lists.newArrayList(puzzleId)
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

    private Optional<String> getPuzzleIdForAlias(String possiblePuzzleAlias) {
        Set<String> puzzleIds = getAllPuzzleProperties().entrySet().stream()
                .filter(entry -> entry.getValue().containsKey("AliasesProperty"))
                .filter(entry -> ((Puzzle.AliasesProperty) entry.getValue().get("AliasesProperty"))
                        .getAliases().contains(possiblePuzzleAlias))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (puzzleIds.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Ordering.natural().leastOf(puzzleIds, 1).get(0));
    }

    public Map<String, Puzzle> getPuzzles() {
        List<Puzzle> puzzles = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM puzzles",
                Lists.newArrayList(),
                Puzzle.class
        );
        Table<Integer, String, Object> puzzlePropertiesResults = DatabaseHelper.query(
                connectionFactory,
                "SELECT puzzleId, propertyKey, propertyValue FROM puzzle_properties",
                Lists.newArrayList()
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

    private Map<String, Map<String, Puzzle.Property>> getAllPuzzleProperties() {
        Table<Integer, String, Object> puzzlePropertiesResults = DatabaseHelper.query(
                connectionFactory,
                "SELECT puzzleId, propertyKey, propertyValue FROM puzzle_properties",
                Lists.newArrayList()
        );
        return deserializePuzzleProperties(puzzlePropertiesResults);
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

        Optional<Integer> generatedId = DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO puzzle_properties (puzzleId, propertyKey, propertyValue) SELECT ?, ?, ? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM puzzle_properties WHERE puzzleId = ? AND propertyKey = ?)",
                Lists.newArrayList(puzzleId, propertyKey, propertyValue, puzzleId, propertyKey));
        if (generatedId.isPresent()) {
            changed = true;
        } else {
            int updates = DatabaseHelper.update(
                    connectionFactory,
                    "UPDATE puzzle_properties SET propertyValue = ? " +
                            "WHERE puzzleId = ? AND propertyKey = ?",
                            Lists.newArrayList(propertyValue, puzzleId, propertyKey)
            );
            changed = updates > 0;
        }

        if (changed) {
            eventProcessor.process(PuzzlePropertyChangeEvent.builder()
                    .setPuzzle(getPuzzle(puzzleId))
                    .build());
        }

        return changed;
    }
}
