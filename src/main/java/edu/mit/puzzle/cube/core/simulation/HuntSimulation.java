package edu.mit.puzzle.cube.core.simulation;

import com.beust.jcommander.internal.Maps;
import edu.mit.puzzle.cube.core.CubeComponent;
import edu.mit.puzzle.cube.core.CubeConfig;
import edu.mit.puzzle.cube.core.CubeConfigModule;
import edu.mit.puzzle.cube.core.DaggerCubeComponent;
import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.events.HuntStartEvent;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HuntSimulation {

    private enum PuzzleType {
        CHARACTER,
        CHARACTER_META,
        QUEST,
        QUEST_META,
        EVENT
    }

    private final EventProcessor<Event> eventProcessor;
    private final HuntStatusStore huntStatusStore;
    private Instant huntTime;
    private Map<String,Double> accumulatedEffort;

    public HuntSimulation(
            SimulatedTeam simulatedTeam
    ) {
        CubeConfig config = CubeConfig.readFromConfigJson();
        CubeComponent dagger = DaggerCubeComponent.builder()
                .cubeConfigModule(new CubeConfigModule(config))
                .build();

        HuntDefinition huntDefinition = dagger.getHuntDefinition();
        dagger.injectHuntDefinition(huntDefinition);
        huntDefinition.addToEventProcessor();

        huntStatusStore = huntDefinition.huntStatusStore;
        eventProcessor = huntDefinition.eventProcessor;
        huntTime = Instant.ofEpochSecond(1484330400);
        accumulatedEffort = Maps.newHashMap();

        System.out.println(getUnlockedPuzzleIdsForTeam(simulatedTeam.teamId()));
        eventProcessor.process(HuntStartEvent.builder().build());
        System.out.println(getUnlockedPuzzleIdsForTeam(simulatedTeam.teamId()));
    }

    private double getTargetEffortForPuzzle(String puzzleId) {
        return 0.0;
    }

    private String getTypeForPuzzle(String puzzleId) {

    }

    private Set<String> getUnlockedPuzzleIdsForTeam(String teamId) {
        return huntStatusStore.getVisibilitiesForTeam(teamId).stream()
                .filter(vis -> vis.getStatus().equalsIgnoreCase("unlocked"))
                .map(vis -> vis.getPuzzleId())
                .collect(Collectors.toSet());
    }

}
