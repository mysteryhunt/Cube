package edu.mit.puzzle.cube.huntimpl.setec2017;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Setec2017HuntDefinition implements HuntDefinition {
    private static final VisibilityStatusSet VISIBILITY_STATUS_SET = new StandardVisibilityStatusSet();

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
    abstract static class UnlockConstraint {
        @AutoValue.Builder
        abstract static class Builder {
            abstract Builder setAutomaticUnlock(boolean automaticUnlock);
            abstract ImmutableMap.Builder<ImmutableSet<Character>, Integer> sumConstraintsBuilder();
            abstract ImmutableMap.Builder<ImmutableSet<Character>, Integer> maxConstraintsBuilder();

            Builder addSumConstraint(int levels, Character... characters) {
                sumConstraintsBuilder().put(ImmutableSet.copyOf(characters), levels);
                return this;
            }

            Builder addMaxConstraint(int levels, Character... characters) {
                maxConstraintsBuilder().put(ImmutableSet.copyOf(characters), levels);
                return this;
            }

            abstract UnlockConstraint build();
        }

        static Builder builder() {
            return new AutoValue_Setec2017HuntDefinition_UnlockConstraint.Builder()
                    .setAutomaticUnlock(true);
        }

        abstract boolean getAutomaticUnlock();
        abstract ImmutableMap<ImmutableSet<Character>, Integer> getSumConstraints();
        abstract ImmutableMap<ImmutableSet<Character>, Integer> getMaxConstraints();

        boolean isSatisfied(CharacterLevelsProperty characterLevels) {
            if (!getAutomaticUnlock()) {
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
        static Setec2017Puzzle create(
                Puzzle puzzle,
                UnlockConstraint unlockConstraint,
                SolveReward solveReward
        ) {
            return new AutoValue_Setec2017HuntDefinition_Setec2017Puzzle(
                    puzzle,
                    unlockConstraint,
                    solveReward
            );
        }

        abstract Puzzle getPuzzle();
        abstract UnlockConstraint getUnlockConstraint();
        abstract SolveReward getSolveReward();
    }

    @Override
    public List<Puzzle> getPuzzles() {
        return Setec2017Puzzles.PUZZLES.entrySet().stream()
                .map(entry -> entry.getValue().getPuzzle())
                .collect(Collectors.toList());
    }

    private static void unlockPuzzles(HuntStatusStore huntStatusStore, String teamId) {
        CharacterLevelsProperty characterLevels = huntStatusStore
                .getTeam(teamId)
                .getTeamProperty(CharacterLevelsProperty.class);
        for (Setec2017Puzzle puzzle : Setec2017Puzzles.PUZZLES.values()) {
            if (puzzle.getUnlockConstraint().isSatisfied(characterLevels)) {
                huntStatusStore.setVisibility(
                        teamId,
                        puzzle.getPuzzle().getPuzzleId(),
                        "UNLOCKED",
                        false
                );
            }
        }
    }

    @Override
    public void addToEventProcessor(CompositeEventProcessor eventProcessor, HuntStatusStore huntStatusStore) {
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
                    unlockPuzzles(huntStatusStore, teamId);
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
                    unlockPuzzles(huntStatusStore, visibility.getTeamId());
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
