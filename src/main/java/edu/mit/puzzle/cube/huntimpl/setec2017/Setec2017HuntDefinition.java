package edu.mit.puzzle.cube.huntimpl.setec2017;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.FullReleaseEvent;
import edu.mit.puzzle.cube.core.events.HintCompleteEvent;
import edu.mit.puzzle.cube.core.events.HuntStartEvent;
import edu.mit.puzzle.cube.core.events.SubmissionCompleteEvent;
import edu.mit.puzzle.cube.core.events.VisibilityChangeEvent;
import edu.mit.puzzle.cube.core.model.HintRequest;
import edu.mit.puzzle.cube.core.model.HintRequestStatus;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.Puzzle;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;
import edu.mit.puzzle.cube.core.model.Team;
import edu.mit.puzzle.cube.core.model.Visibility;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Setec2017HuntDefinition implements HuntDefinition {
    private static final VisibilityStatusSet VISIBILITY_STATUS_SET = new StandardVisibilityStatusSet();

    private HuntStatusStore huntStatusStore;

    @Override
    public VisibilityStatusSet getVisibilityStatusSet() {
        return VISIBILITY_STATUS_SET;
    }

    public enum Character {
        FIGHTER,
        WIZARD,
        CLERIC,
        LINGUIST,
        ECONOMIST,
        CHEMIST;
    }

    @AutoValue
    public abstract static class CharacterLevelsProperty extends Team.Property {
        static {
            registerClass(CharacterLevelsProperty.class);
        }

        @JsonCreator
        public static CharacterLevelsProperty create(
                @JsonProperty("levels") ImmutableMap<Character, Integer> levels) {
            return new AutoValue_Setec2017HuntDefinition_CharacterLevelsProperty(levels);
        }

        @JsonProperty("levels")
        public abstract ImmutableMap<Character, Integer> getLevels();
    }

    @AutoValue
    public abstract static class GoldProperty extends Team.Property {
        static {
            registerClass(GoldProperty.class);
        }

        @JsonCreator
        public static GoldProperty create(@JsonProperty("gold") int gold) {
            return new AutoValue_Setec2017HuntDefinition_GoldProperty(gold);
        }

        @JsonProperty("gold") public abstract int getGold();
    }

    // TODO: create a team property for inventory items

    @AutoValue
    abstract static class VisibilityConstraint {
        private static final ImmutableList<String> VISIBILITY_STATUS_ORDER = ImmutableList.of(
                "INVISIBLE", "VISIBLE", "UNLOCKED", "SOLVED");

        @AutoValue.Builder
        abstract static class Builder {
            // If true, then this visibility constraint will never be satisfied. All other
            // constraints in this object will be ignored.
            abstract Builder setRequiresManualAction(boolean requiresManualAction);

            // A sum constraint is satisfied when the sum of the levels of a set of characters is
            // greater than or equal to a number. All sum constraints must be satisfied for this
            // visibility constraint to be satisfied.
            abstract ImmutableMap.Builder<ImmutableSet<Character>, Integer> sumConstraintsBuilder();

            // A max constraint is satisfied when the largest of the levels of a set of characters
            // is greater than or equal to a number. All max constraints must be satisfied for this
            // visibility constraint to be satisfied.
            abstract ImmutableMap.Builder<ImmutableSet<Character>, Integer> maxConstraintsBuilder();

            // A puzzle visibility status constraint is satisfied when a puzzle has the given
            // visibility status, or has a visibility status that follows the given visibility
            // status in the visibility status order. All puzzle visibility constraints must be
            // satisfied for this visibility constraint to be satisfied.
            abstract ImmutableMap.Builder<String, String> puzzleVisibilityStatusConstraintsBuilder();

            Builder addSumConstraint(int levels, Character... characters) {
                sumConstraintsBuilder().put(ImmutableSet.copyOf(characters), levels);
                return this;
            }

            Builder addMaxConstraint(int levels, Character... characters) {
                maxConstraintsBuilder().put(ImmutableSet.copyOf(characters), levels);
                return this;
            }

            Builder addPuzzleVisibilityStatusConstraint(String puzzleId, String visibility) {
                puzzleVisibilityStatusConstraintsBuilder().put(puzzleId, visibility);
                return this;
            }

            abstract VisibilityConstraint build();
        }

        static Builder builder() {
            return new AutoValue_Setec2017HuntDefinition_VisibilityConstraint.Builder()
                    .setRequiresManualAction(false);
        }

        abstract boolean getRequiresManualAction();
        abstract ImmutableMap<ImmutableSet<Character>, Integer> getSumConstraints();
        abstract ImmutableMap<ImmutableSet<Character>, Integer> getMaxConstraints();
        abstract ImmutableMap<String, String> getPuzzleVisibilityStatusConstraints();

        boolean isSatisfied(
                CharacterLevelsProperty characterLevels,
                Map<String, String> puzzleIdToVisibilityStatus
        ) {
            if (getRequiresManualAction()) {
                return false;
            }

            for (Map.Entry<ImmutableSet<Character>, Integer> entry : getSumConstraints().entrySet()) {
                ImmutableSet<Character> characters = entry.getKey();
                Integer minimumLevelSum = entry.getValue();
                int sum = 0;
                for (Character character : characters) {
                    Integer level = characterLevels.getLevels().get(character);
                    if (level != null) {
                        sum += level;
                    }
                }
                if (sum < minimumLevelSum) {
                    return false;
                }
            }

            for (Map.Entry<ImmutableSet<Character>, Integer> entry : getMaxConstraints().entrySet()) {
                ImmutableSet<Character> characters = entry.getKey();
                Integer minimumLevel = entry.getValue();
                int max = 0;
                for (Character character : characters) {
                    Integer level = characterLevels.getLevels().get(character);
                    if (level != null && level > max) {
                        max = level;
                    }
                }
                if (max < minimumLevel) {
                    return false;
                }
            }

            for (Map.Entry<String, String> entry : getPuzzleVisibilityStatusConstraints().entrySet()) {
                String puzzleId = entry.getKey();
                String constraintStatus = entry.getValue();
                String status = puzzleIdToVisibilityStatus.get(puzzleId);
                if (status == null) {
                    status = VISIBILITY_STATUS_SET.getDefaultVisibilityStatus();
                }
                if (VISIBILITY_STATUS_ORDER.indexOf(status) < VISIBILITY_STATUS_ORDER.indexOf(constraintStatus)) {
                    return false;
                }
            }

            return true;
        }
    }

    @AutoValue
    abstract static class SolveReward {
        @AutoValue.Builder
        abstract static class Builder {
            abstract Builder setGold(int gold);
            abstract ImmutableMap.Builder<Character, Integer> characterLevelsBuilder();

            Builder addCharacterLevels(Character character, int levels) {
                characterLevelsBuilder().put(character, levels);
                return this;
            }

            abstract SolveReward build();
        }

        static Builder builder() {
            return new AutoValue_Setec2017HuntDefinition_SolveReward.Builder()
                    .setGold(0);
        }

        abstract int getGold();
        abstract ImmutableMap<Character, Integer> getCharacterLevels();
        // TODO: add inventory items
    }

    @AutoValue
    abstract static class Setec2017Puzzle {
        @AutoValue.Builder
        abstract static class Builder {
            abstract Builder setPuzzle(Puzzle puzzle);
            abstract Builder setVisibleConstraint(VisibilityConstraint visibleConstraint);
            abstract Builder setUnlockedConstraint(VisibilityConstraint unlockedConstraint);
            abstract Builder setSolveReward(SolveReward solveReward);
            abstract Setec2017Puzzle build();
        }

        static Builder builder() {
            return new AutoValue_Setec2017HuntDefinition_Setec2017Puzzle.Builder()
                    .setVisibleConstraint(VisibilityConstraint.builder().build())
                    .setUnlockedConstraint(VisibilityConstraint.builder().build())
                    .setSolveReward(SolveReward.builder().build());
        }

        abstract Puzzle getPuzzle();
        abstract VisibilityConstraint getVisibleConstraint();
        abstract VisibilityConstraint getUnlockedConstraint();
        abstract SolveReward getSolveReward();
    }

    @Override
    public List<Puzzle> getPuzzles() {
        return Setec2017Puzzles.PUZZLES.entrySet().stream()
                .map(entry -> entry.getValue().getPuzzle())
                .collect(Collectors.toList());
    }

    private class Setec2017CompositeEventProcessor extends CompositeEventProcessor {
        private Set<String> getVisibilityAffectedTeams(Event event) {
            if (event instanceof HuntStartEvent || event instanceof FullReleaseEvent) {
                return ImmutableSet.copyOf(huntStatusStore.getTeamIds());
            }
            if (event instanceof VisibilityChangeEvent) {
                VisibilityChangeEvent visibilityChangeEvent = (VisibilityChangeEvent) event;
                return ImmutableSet.of(visibilityChangeEvent.getVisibility().getTeamId());
            }
            return ImmutableSet.of();
        }

        @Override
        public void process(Event event) {
            super.process(event);
            for (String teamId : getVisibilityAffectedTeams(event)) {
                updateVisibility(teamId);
            }
        }

        @Override
        public void processBatch(List<? extends Event> events) {
            Set<String> affectedTeams = new HashSet<>();
            for (Event event : events) {
                super.process(event);
                affectedTeams.addAll(getVisibilityAffectedTeams(event));
            }
            for (String teamId : affectedTeams) {
                updateVisibility(teamId);
            }
        }
    }

    @Override
    public CompositeEventProcessor generateCompositeEventProcessor() {
        return new Setec2017CompositeEventProcessor();
    }

    private void updateVisibility(String teamId) {
        CharacterLevelsProperty characterLevels = huntStatusStore
                .getTeam(teamId)
                .getTeamProperty(CharacterLevelsProperty.class);

        Map<String, String> puzzleIdToVisibilityStatus =
                huntStatusStore.getVisibilitiesForTeam(teamId).stream()
                .collect(Collectors.toMap(Visibility::getPuzzleId, Visibility::getStatus));

        Table<String, String, String> teamPuzzleStatusTable = HashBasedTable.create();

        for (Setec2017Puzzle puzzle : Setec2017Puzzles.PUZZLES.values()) {
            if (puzzle.getUnlockedConstraint().isSatisfied(
                    characterLevels,
                    puzzleIdToVisibilityStatus
            )) {
                teamPuzzleStatusTable.put(teamId, puzzle.getPuzzle().getPuzzleId(), "UNLOCKED");
            } else if (puzzle.getVisibleConstraint().isSatisfied(
                    characterLevels,
                    puzzleIdToVisibilityStatus
            )) {
                teamPuzzleStatusTable.put(teamId, puzzle.getPuzzle().getPuzzleId(), "VISIBLE");
            }
        }

        huntStatusStore.setVisibilityBatch(teamPuzzleStatusTable, false);
    }

    @Override
    public void addToEventProcessor(CompositeEventProcessor eventProcessor, HuntStatusStore huntStatusStore) {
        this.huntStatusStore = huntStatusStore;

        eventProcessor.addEventProcessor(HuntStartEvent.class, event -> {
            boolean changed = huntStatusStore.recordHuntRunStart();
            if (changed) {
                for (String teamId : huntStatusStore.getTeamIds()) {
                    huntStatusStore.setTeamProperty(
                            teamId,
                            GoldProperty.class,
                            GoldProperty.create(0)
                    );
                    huntStatusStore.setTeamProperty(
                            teamId,
                            CharacterLevelsProperty.class,
                            CharacterLevelsProperty.create(ImmutableMap.of())
                    );
                }
            }
        });

        eventProcessor.addEventProcessor(SubmissionCompleteEvent.class, event -> {
            Submission submission = event.getSubmission();
            if (submission.getStatus().equals(SubmissionStatus.CORRECT)) {
                huntStatusStore.setVisibility(
                        submission.getTeamId(),
                        submission.getPuzzleId(),
                        "SOLVED",
                        false
                );
            }
        });

        eventProcessor.addEventProcessor(VisibilityChangeEvent.class, event -> {
            Visibility visibility = event.getVisibility();
            if (visibility.getStatus().equals("SOLVED")) {
                Setec2017Puzzle puzzle = Setec2017Puzzles.PUZZLES.get(visibility.getPuzzleId());
                SolveReward solveReward = puzzle.getSolveReward();
                if (solveReward.getGold() != 0) {
                    huntStatusStore.mutateTeamProperty(
                            visibility.getTeamId(),
                            GoldProperty.class,
                            gold -> GoldProperty.create(gold.getGold() + solveReward.getGold())
                    );
                }
                if (!solveReward.getCharacterLevels().isEmpty()) {
                    huntStatusStore.mutateTeamProperty(
                            visibility.getTeamId(),
                            CharacterLevelsProperty.class,
                            characterLevels -> {
                                Map<Character, Integer> newLevels = new HashMap<>(characterLevels.getLevels());
                                for (Map.Entry<Character, Integer> entry : solveReward.getCharacterLevels().entrySet()) {
                                    Character character = entry.getKey();
                                    Integer levelsToAdd = entry.getValue();
                                    if (characterLevels.getLevels().containsKey(character)) {
                                        newLevels.put(character, characterLevels.getLevels().get(character) + levelsToAdd);
                                    } else {
                                        newLevels.put(character, levelsToAdd);
                                    }
                                }
                                return CharacterLevelsProperty.create(ImmutableMap.copyOf(newLevels));
                            }
                    );
                }
            }
        });

        eventProcessor.addEventProcessor(FullReleaseEvent.class, event -> {
            for (String teamId : huntStatusStore.getTeamIds()) {
                huntStatusStore.setVisibility(
                        teamId,
                        event.getPuzzleId(),
                        "UNLOCKED",
                        false
                );
            }
        });

        eventProcessor.addEventProcessor(HintCompleteEvent.class, event -> {
            String teamId = event.getHintRequest().getTeamId();
            HintRequestStatus hintRequestStatus = event.getHintRequest().getStatus();
            if (hintRequestStatus == HintRequestStatus.REJECTED) {
                // Refund the team their gold back.
                huntStatusStore.mutateTeamProperty(
                        teamId,
                        GoldProperty.class,
                        goldProperty -> GoldProperty.create(goldProperty.getGold() + 1)
                );
            }
        });
    }

    @Override
    public boolean handleHintRequest(HintRequest hintRequest, HuntStatusStore huntStatusStore) {
        AtomicBoolean deductedGold = new AtomicBoolean(false);
        huntStatusStore.mutateTeamProperty(
                hintRequest.getTeamId(),
                GoldProperty.class,
                goldProperty -> {
                    if (goldProperty.getGold() > 0) {
                        deductedGold.set(true);
                        return GoldProperty.create(goldProperty.getGold() - 1);
                    }
                    return goldProperty;
                }
        );
        return deductedGold.get();
    }
}
