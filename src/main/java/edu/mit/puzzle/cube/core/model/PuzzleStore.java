package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.events.PuzzlePropertyChangeEvent;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.util.*;
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
    private final Map<String, Puzzle> puzzles;

    @Inject
    public PuzzleStore(
            ConnectionFactory connectionFactory,
            EventProcessor<Event> eventProcessor,
            HuntDefinition huntDefinition
    ) {
        this(connectionFactory, eventProcessor, huntDefinition.getPuzzles());
    }

    public PuzzleStore(
            ConnectionFactory connectionFactory,
            EventProcessor<Event> eventProcessor,
            List<Puzzle> puzzleList
    ) {
        this.connectionFactory = connectionFactory;
        this.eventProcessor = eventProcessor;
        puzzles = puzzleList.stream().collect(
                Collectors.toMap(Puzzle::getPuzzleId, Function.identity())
        );
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
        Puzzle puzzle = puzzles.get(puzzleId);
        if (puzzle == null) {
            Optional<String> aliasedId = getPuzzleIdForAlias(puzzleId);
            if (aliasedId.isPresent()) {
                puzzleId = aliasedId.get();
                puzzle = puzzles.get(puzzleId);
            }
            if (puzzle == null) {
                throw new ResourceException(
                        Status.CLIENT_ERROR_NOT_FOUND.getCode(),
                        String.format("Unknown puzzle id %s", puzzleId));
            }
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
        //Search in in-memory stored properties
        Set<String> inMemoryPuzzleIds = puzzles.entrySet().stream()
                .filter(entry -> entry.getValue().getPuzzleProperty(Puzzle.AliasesProperty.class) != null)
                .filter(entry -> entry.getValue().getPuzzleProperty(Puzzle.AliasesProperty.class)
                        .getAliases().contains(possiblePuzzleAlias))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());


        //Search in database-stored properties
        Set<String> databasePuzzleIds = getAllPuzzleProperties().entrySet().stream()
                .filter(entry -> entry.getValue().containsKey("AliasesProperty"))
                .filter(entry -> ((Puzzle.AliasesProperty) entry.getValue().get("AliasesProperty"))
                        .getAliases().contains(possiblePuzzleAlias))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Set<String> puzzleIds = Sets.union(inMemoryPuzzleIds, databasePuzzleIds);
        if (puzzleIds.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Ordering.natural().leastOf(puzzleIds, 1).get(0));
    }

    public Map<String, Puzzle> getPuzzles() {
        Map<String, Map<String, Puzzle.Property>> allPuzzleProperties = getAllPuzzleProperties();
        Map<String, Puzzle> puzzlesCopy = ImmutableMap.copyOf(puzzles);
        puzzlesCopy = Maps.transformEntries(puzzlesCopy, (puzzleId, puzzle) -> {
            Map<String, Puzzle.Property> puzzleProperties = allPuzzleProperties.get(puzzleId);
            if (puzzleProperties != null && !puzzleProperties.isEmpty()) {
                puzzle = puzzle.toBuilder()
                        .setPuzzleProperties(puzzleProperties)
                        .build();
            }
            return puzzle;
        });
        return puzzlesCopy;
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
