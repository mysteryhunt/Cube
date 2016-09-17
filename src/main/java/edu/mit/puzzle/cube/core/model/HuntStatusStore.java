package edu.mit.puzzle.cube.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.DatabaseHelper;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.events.VisibilityChangeEvent;

import org.apache.commons.lang3.tuple.Pair;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class HuntStatusStore {
    private static Logger LOGGER = LoggerFactory.getLogger(HuntStatusStore.class);

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new GuavaModule());

    private final ConnectionFactory connectionFactory;
    private final Clock clock;
    private final VisibilityStatusSet visibilityStatusSet;
    private final EventProcessor<Event> eventProcessor;

    public HuntStatusStore(
        ConnectionFactory connectionFactory,
        VisibilityStatusSet visibilityStatusSet,
        EventProcessor<Event> eventProcessor
    ) {
        this(connectionFactory, Clock.systemUTC(), visibilityStatusSet, eventProcessor);
    }

    public HuntStatusStore(
            ConnectionFactory connectionFactory,
            Clock clock,
            VisibilityStatusSet visibilityStatusSet,
            EventProcessor<Event> eventProcessor
    ) {
        this.connectionFactory = checkNotNull(connectionFactory);
        this.clock = checkNotNull(clock);
        this.visibilityStatusSet = checkNotNull(visibilityStatusSet);
        this.eventProcessor = checkNotNull(eventProcessor);
    }

    public VisibilityStatusSet getVisibilityStatusSet() {
        return this.visibilityStatusSet;
    }

    public Visibility getVisibility(String teamId, String puzzleId) {
        return getExplicitVisibility(teamId, puzzleId)
                .orElse(Visibility.builder()
                        .setTeamId(teamId)
                        .setPuzzleId(puzzleId)
                        .setStatus(visibilityStatusSet.getDefaultVisibilityStatus())
                        .build()
                );
    }

    public List<Visibility> getExplicitVisibilities(
            Optional<String> teamId,
            Optional<String> puzzleId
    ) {
        String whereClause = "";
        List<Object> parameters = Lists.newArrayList();
        if (teamId.isPresent() && puzzleId.isPresent()) {
            whereClause = " WHERE teamId = ? AND puzzleId = ?";
            parameters.add(teamId.get());
            parameters.add(puzzleId.get());
        } else if (teamId.isPresent()) {
            whereClause = " WHERE teamId = ?";
            parameters.add(teamId.get());
        } else if (puzzleId.isPresent()) {
            whereClause = " WHERE puzzleId = ?";
            parameters.add(puzzleId.get());
        }

        String visibilitiesQuery = "SELECT teamId, puzzleId, status FROM visibilities";
        visibilitiesQuery += whereClause;
        List<Visibility> visibilities = DatabaseHelper.query(
                connectionFactory,
                visibilitiesQuery,
                parameters,
                Visibility.class
        );

        String submissionsQuery = "SELECT teamId, puzzleId, canonicalAnswer FROM submissions";
        if (whereClause.isEmpty()) {
            submissionsQuery += " WHERE ";
        } else {
            submissionsQuery += whereClause + " AND ";
        }
        submissionsQuery += "canonicalAnswer IS NOT NULL";
        List<Submission> submissions = DatabaseHelper.query(
                connectionFactory,
                submissionsQuery,
                parameters,
                Submission.class
        );
        ImmutableListMultimap<Object, Submission> submissionIndex = Multimaps.index(
                submissions,
                submission -> Pair.of(submission.getTeamId(), submission.getPuzzleId())
        );

        visibilities = visibilities.stream().map(visibility -> {
            List<Submission> visibilitySubmissions = submissionIndex.get(
                    Pair.of(visibility.getTeamId(), visibility.getPuzzleId()));
            if (!visibilitySubmissions.isEmpty()) {
                List<String> solvedAnswers = visibilitySubmissions.stream()
                        .map(Submission::getCanonicalAnswer)
                        .collect(Collectors.toList());
                return visibility.toBuilder()
                        .setSolvedAnswers(solvedAnswers)
                        .build();
            }
            return visibility;
        }).collect(Collectors.toList());

        return visibilities;
    }

    public List<Visibility> getVisibilitiesForTeam(String teamId) {
        List<Visibility> visibilities = DatabaseHelper.query(
                connectionFactory,
                "SELECT " +
                "  ? AS teamId, " +
                "  puzzles.puzzleId AS puzzleId, " +
                "  CASE WHEN visibilities.status IS NOT NULL " +
                "    THEN visibilities.status " +
                "    ELSE ? " +
                "  END AS status " +
                "FROM puzzles " +
                "LEFT JOIN visibilities ON " +
                "  puzzles.puzzleId = visibilities.puzzleId AND visibilities.teamId = ?",
                Lists.newArrayList(teamId, visibilityStatusSet.getDefaultVisibilityStatus(), teamId),
                Visibility.class
        );

        String submissionsQuery = "SELECT puzzleId, canonicalAnswer FROM submissions " +
                "WHERE teamId = ? AND canonicalAnswer IS NOT NULL";
        List<Submission> submissions = DatabaseHelper.query(
                connectionFactory,
                submissionsQuery,
                ImmutableList.of(teamId),
                Submission.class
        );
        ImmutableListMultimap<String, Submission> submissionIndex = Multimaps.index(
                submissions, Submission::getPuzzleId
        );

        visibilities = visibilities.stream().map(visibility -> {
            List<Submission> visibilitySubmissions = submissionIndex.get(visibility.getPuzzleId());
            if (!visibilitySubmissions.isEmpty()) {
                List<String> solvedAnswers = visibilitySubmissions.stream()
                        .map(Submission::getCanonicalAnswer)
                        .collect(Collectors.toList());
                return visibility.toBuilder()
                        .setSolvedAnswers(solvedAnswers)
                        .build();
            }
            return visibility;
        }).collect(Collectors.toList());

        return visibilities;
    }

    public boolean recordHuntRunStart() {
        Integer updates = DatabaseHelper.update(
                connectionFactory,
                "UPDATE run SET startTimestamp = ? WHERE startTimestamp IS NULL",
                Lists.newArrayList(Timestamp.from(clock.instant()))
        );
        return updates > 0;
    }

    public Optional<Run> getHuntRunProperties() {
        List<Run> runs = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM run",
                Lists.newArrayList(),
                Run.class
        );

        if (runs.size() == 1) {
            return Optional.of(runs.get(0));
        } else {
            return Optional.empty();
        }
    }

    public Set<String> getTeamIds() {
        List<Team> teams = DatabaseHelper.query(
                connectionFactory,
                "SELECT teamId FROM teams",
                Lists.newArrayList(),
                Team.class
        );

        return teams.stream().map(Team::getTeamId).collect(Collectors.toSet());
    }

    private Map<String, Map<String, Team.Property>> deserializeTeamProperties(
            Table<Integer, String, Object> teamPropertiesResults
    ) {
        Map<String, Map<String, Team.Property>> allTeamProperties = new HashMap<>();
        for (Map<String, Object> rowMap : teamPropertiesResults.rowMap().values()) {
            String teamId = (String) rowMap.get("teamId");
            Map<String, Team.Property> teamProperties = allTeamProperties.get(teamId);
            if (teamProperties == null) {
                teamProperties = new HashMap<>();
                allTeamProperties.put(teamId, teamProperties);
            }

            String key = (String) rowMap.get("propertyKey");
            String value = (String) rowMap.get("propertyValue");
            Class<? extends Team.Property> propertyClass = Team.Property.getClass(key);
            try {
                Team.Property property = OBJECT_MAPPER.readValue(value, propertyClass);
                teamProperties.put(key, property);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return allTeamProperties;
    }

    public Team getTeam(String teamId) {
        Table<Integer, String, Object> teamPropertiesResults = DatabaseHelper.query(
                connectionFactory,
                "SELECT teamId, propertyKey, propertyValue FROM team_properties " +
                        "WHERE teamId = ?",
                Lists.newArrayList(teamId)
        );
        Map<String, Team.Property> teamProperties =
                deserializeTeamProperties(teamPropertiesResults).get(teamId);

        List<Team> teams = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM teams WHERE teamId = ?",
                Lists.newArrayList(teamId),
                Team.class
        );
        Team team = Iterables.getOnlyElement(teams);

        return team.toBuilder()
                .setTeamProperties(teamProperties)
                .build();
    }

    public List<Team> getTeams() {
        Table<Integer, String, Object> teamPropertiesResults = DatabaseHelper.query(
                connectionFactory,
                "SELECT teamId, propertyKey, propertyValue FROM team_properties",
                ImmutableList.of()
        );
        Map<String, Map<String, Team.Property>> allTeamProperties =
                deserializeTeamProperties(teamPropertiesResults);

        List<Team> teams = DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM teams",
                ImmutableList.of(),
                Team.class
        );

        return teams.stream()
                .map(team -> team.toBuilder().setTeamProperties(
                        allTeamProperties.get(team.getTeamId())).build())
                .collect(Collectors.toList());
    }

    public boolean setTeamProperty(
            String teamId,
            Class<? extends Team.Property> propertyClass,
            Team.Property property) {
        String propertyKey = propertyClass.getSimpleName();
        Preconditions.checkArgument(
                propertyClass.isInstance(property),
                "Team property is not an instance of %s",
                propertyKey);
        String propertyValue;
        try {
            propertyValue = OBJECT_MAPPER.writeValueAsString(property);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Optional<Integer> generatedId = DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO team_properties (teamId, propertyKey, propertyValue) SELECT ?, ?, ? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM team_properties WHERE teamId = ? AND propertyKey = ?)",
                Lists.newArrayList(teamId, propertyKey, propertyValue, teamId, propertyKey));
        if (generatedId.isPresent()) {
            return true;
        }

        int updates = DatabaseHelper.update(
                connectionFactory,
                "UPDATE team_properties SET propertyValue = ? " +
                        "WHERE teamId = ? AND propertyKey = ?",
                Lists.newArrayList(propertyValue, teamId, propertyKey)
        );
        return updates > 0;
    }

    public <P extends Team.Property> boolean mutateTeamProperty(
            String teamId,
            Class<P> propertyClass,
            Function<P, P> mutator) {
        String propertyKey = propertyClass.getSimpleName();
        int retryCount = 0;
        while (true) {
            try (
                    Connection connection = connectionFactory.getConnection();
                    PreparedStatement getPropertyStatement = connection.prepareStatement(
                            "SELECT propertyValue FROM team_properties WHERE teamId = ? AND propertyKey = ?");
                    PreparedStatement updatePropertyStatement = connection.prepareStatement(
                            "UPDATE team_properties SET propertyValue = ? WHERE teamId = ? AND propertyKey = ?")
            ) {
                connection.setAutoCommit(false);

                getPropertyStatement.setString(1, teamId);
                getPropertyStatement.setString(2, propertyKey);
                ResultSet resultSet = getPropertyStatement.executeQuery();
                if (!resultSet.next()) {
                    throw new RuntimeException("failed to read team property from database");
                }
                P property = OBJECT_MAPPER.readValue(resultSet.getString(1), propertyClass);

                P mutatedProperty = mutator.apply(property);
                String mutatedPropertyJson = OBJECT_MAPPER.writeValueAsString(mutatedProperty);

                updatePropertyStatement.setString(1, mutatedPropertyJson);
                updatePropertyStatement.setString(2, teamId);
                updatePropertyStatement.setString(3, propertyKey);
                boolean updated = updatePropertyStatement.executeUpdate() > 0;

                connection.commit();

                return updated;
            } catch (SQLException e) {
                // 40001 is the SQLSTATE error for a serialization failure.
                if (e.getSQLState().equals("40001")) {
                    ++retryCount;
                    if (retryCount > 3) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addTeam(Team team) {
        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement insertTeamStatement = connection.prepareStatement(
                        "INSERT INTO teams (teamId, email, primaryPhone, secondaryPhone) VALUES (?,?,?,?)")
        ) {
            insertTeamStatement.setString(1, team.getTeamId());
            insertTeamStatement.setString(2, team.getEmail());
            insertTeamStatement.setString(3, team.getPrimaryPhone());
            insertTeamStatement.setString(4, team.getSecondaryPhone());
            insertTeamStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                    e,
                    "Failed to add team to the database");
        }
    }

    public boolean updateTeam(Team team) {
        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement insertTeamStatement = connection.prepareStatement(
                        "UPDATE teams SET email = ?, primaryPhone = ?, secondaryPhone = ? " +
                        "WHERE teamId = ?")
        ) {
            insertTeamStatement.setString(1, team.getEmail());
            insertTeamStatement.setString(2, team.getPrimaryPhone());
            insertTeamStatement.setString(3, team.getSecondaryPhone());
            insertTeamStatement.setString(4, team.getTeamId());
            return insertTeamStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                    e,
                    "Failed to update team in the database");
        }
    }

    private Optional<Visibility> getExplicitVisibility(String teamId, String puzzleId) {
        List<Visibility> visibilities = getExplicitVisibilities(Optional.of(teamId), Optional.of(puzzleId));
        if (visibilities.size() == 1) {
            return Optional.of(visibilities.get(0));
        } else if (visibilities.isEmpty()) {
            return Optional.empty();
        } else {
            throw new RuntimeException("Primary key violation in application layer");
        }
    }

    private boolean createExplicitDefaultVisibility(String teamId, String puzzleId) {
        Optional<Integer> generatedId = DatabaseHelper.insert(
                connectionFactory,
                "INSERT INTO visibilities (teamId, puzzleId) SELECT ?, ? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM visibilities WHERE teamId = ? AND puzzleId = ?)",
                Lists.newArrayList(teamId, puzzleId, teamId, puzzleId));
        return generatedId.isPresent();
    }

    private void createExplicitDefaultVisibilities(Multimap<String,String> teamToPuzzles) {
        List<List<Object>> parametersList = teamToPuzzles.entries().stream()
                .map(entry -> Lists.<Object>newArrayList(
                        entry.getKey(), entry.getValue(), entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        DatabaseHelper.insertBatch(
                connectionFactory,
                "INSERT INTO visibilities (teamId, puzzleId) SELECT ?, ? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM visibilities WHERE teamId = ? AND puzzleId = ?)",
                parametersList
        );
    }

    public boolean setVisibility(
            String teamId,
            String puzzleId,
            String status
    ) {
        return internalSetVisibilityBatch(ImmutableTable.of(teamId, puzzleId, status), true);
    }

    public boolean setVisibilityWithoutWorkflowValidation(
            String teamId,
            String puzzleId,
            String status
    ) {
        return internalSetVisibilityBatch(ImmutableTable.of(teamId, puzzleId, status), false);
    }

    public boolean setVisibilityBatch(
            Table<String,String,String> teamPuzzleStatusTable
    ) {
        return internalSetVisibilityBatch(teamPuzzleStatusTable, true);
    }

    public boolean setVisibilityBatchWithoutWorkflowValidation(
            Table<String,String,String> teamPuzzleStatusTable
    ) {
        return internalSetVisibilityBatch(teamPuzzleStatusTable, false);
    }

    private boolean internalSetVisibilityBatch(
            Table<String,String,String> teamPuzzleStatusTable,
            boolean useWorkflowValidation
    ) {
        Set<String> statuses = Sets.newHashSet(teamPuzzleStatusTable.values());

        Set<String> disallowedStatuses = Sets.filter(statuses, status -> !visibilityStatusSet.isAllowedStatus(status));
        if (!disallowedStatuses.isEmpty()) {
            LOGGER.error("Attempted to set visibilities to invalid status(es): " + Joiner.on(", ").join(disallowedStatuses));
            statuses.removeAll(disallowedStatuses);
        }

        if (useWorkflowValidation) {
            Set<String> unsettableStatuses = Sets.filter(statuses, status -> visibilityStatusSet.getAllowedAntecedents(status).isEmpty());
            if (!unsettableStatuses.isEmpty()) {
                LOGGER.warn("Attempted to set visibilities to unsettable status(es): " + Joiner.on(", ").join(unsettableStatuses));
                statuses.removeAll(unsettableStatuses);
            }
        }

        List<Visibility> updatedVisibilities = Lists.newArrayList();

        for (String status : statuses) {
            Multimap<String,String> teamToPuzzles = HashMultimap.create();
            teamPuzzleStatusTable.cellSet().stream()
                    .filter(cell -> cell.getValue().equals(status))
                    .forEach(cell -> teamToPuzzles.put(cell.getRowKey(), cell.getColumnKey()));
            createExplicitDefaultVisibilities(teamToPuzzles);

            String preparedUpdateSql = "UPDATE visibilities SET status = ? " +
                    "WHERE teamId = ? AND puzzleId = ?";
            Stream<ImmutableList.Builder<Object>> builderStream = teamToPuzzles.entries().stream()
                    .map(entry -> new ImmutableList.Builder<Object>()
                            .add(status)
                            .add(entry.getKey())
                            .add(entry.getValue()));
            if (useWorkflowValidation) {
                Set<String> allowedCurrentStatuses = visibilityStatusSet.getAllowedAntecedents(status);
                preparedUpdateSql += " AND (" +
                        Joiner.on(" OR ").join(allowedCurrentStatuses.stream()
                                .map(s -> "status = ?")
                                .collect(Collectors.toList())) +
                        ")";
                builderStream = builderStream.map(builder -> builder.addAll(allowedCurrentStatuses));
            }

            List<List<Object>> parametersList = builderStream
                    .map(ImmutableList.Builder::build)
                    .collect(Collectors.toList());

            List<Integer> updateCounts = DatabaseHelper.updateBatch(
                    connectionFactory,
                    preparedUpdateSql,
                    parametersList
            );

            List<Visibility> updatedVisibilitiesForStatus = IntStream.range(0, parametersList.size())
                    .filter(index -> updateCounts.get(index) > 0)
                    .mapToObj(index -> Visibility.builder()
                            .setStatus((String) parametersList.get(index).get(0))
                            .setTeamId((String) parametersList.get(index).get(1))
                            .setPuzzleId((String) parametersList.get(index).get(2))
                            .build())
                    .collect(Collectors.toList());

            updatedVisibilities.addAll(updatedVisibilitiesForStatus);
        }

        Timestamp timestamp = Timestamp.from(clock.instant());
        DatabaseHelper.insertBatch(
                connectionFactory,
                "INSERT INTO visibility_history (teamId, puzzleId, status, timestamp) VALUES (?, ?, ?, ?)",
                updatedVisibilities.stream()
                        .map(v -> Lists.<Object>newArrayList(
                                v.getTeamId(),
                                v.getPuzzleId(),
                                v.getStatus(),
                                timestamp
                        )).collect(Collectors.toList())
        );

        List<VisibilityChangeEvent> changeEvents = updatedVisibilities.stream()
                .map(v -> VisibilityChangeEvent.builder().setVisibility(v).build())
                .collect(Collectors.toList());

        if (!changeEvents.isEmpty()) {
            eventProcessor.processBatch(changeEvents);
        }
        return !changeEvents.isEmpty();
    }


    public List<VisibilityChange> getVisibilityHistory(String teamId, String puzzleId) {
        return DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM visibility_history WHERE " +
                        "teamId = ? AND puzzleId = ? ORDER BY timestamp ASC",
                Lists.newArrayList(teamId, puzzleId),
                VisibilityChange.class
        );
    }

    // TODO: introduce some filtering and/or pagination on this API - always reading all
    // visibility changes may not scale.
    public List<VisibilityChange> getVisibilityChanges() {
        return DatabaseHelper.query(
                connectionFactory,
                "SELECT * FROM visibility_history",
                ImmutableList.<Object>of(),
                VisibilityChange.class
        );
    }
}
