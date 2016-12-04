package edu.mit.puzzle.cube.core.serverresources;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.model.Puzzle;
import edu.mit.puzzle.cube.core.model.Puzzles;
import edu.mit.puzzle.cube.core.model.Visibility;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.TeamsPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Get;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PuzzlesResource extends AbstractCubeResource {

    private static Timer getPuzzlesTimer;
    private Timer getPuzzlesTimer() {
        if (getPuzzlesTimer == null) {
            getPuzzlesTimer = metricRegistry.timer(MetricRegistry.name(this.getClass(), "getPuzzles"));
        }
        return getPuzzlesTimer;
    }

    private static Timer getVisibilitiesTimer;
    private Timer getVisibilitiesTimer() {
        if (getVisibilitiesTimer == null) {
            getVisibilitiesTimer = metricRegistry.timer(MetricRegistry.name(this.getClass(), "getVisibilities"));
        }
        return getVisibilitiesTimer;
    }

    private static Timer postProcessingTimer;
    private Timer postProcessingTimer() {
        if (postProcessingTimer == null) {
            postProcessingTimer = metricRegistry.timer(MetricRegistry.name(this.getClass(), "postProcessing"));
        }
        return postProcessingTimer;
    }

    @Get
    public Puzzles handleGet() {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));
        if (teamId.isPresent()) {
            SecurityUtils.getSubject().checkPermission(
                    new TeamsPermission(teamId.get(), PermissionAction.READ));

            Timer.Context timerContext = getPuzzlesTimer().time();
            Map<String, Puzzle> unfilteredPuzzles = puzzleStore.getPuzzles();
            timerContext.stop();

            timerContext = getVisibilitiesTimer().time();
            Map<String, Visibility> retrievedVisibilities = huntStatusStore.getVisibilitiesForTeam(teamId.get()).stream()
                    .collect(Collectors.toMap(
                            Visibility::getPuzzleId,
                            Function.identity()
                    ));
            timerContext.stop();

            timerContext = postProcessingTimer().time();
            final Set<String> invisibleStatuses = huntStatusStore.getVisibilityStatusSet().getInvisibleStatuses();
            List<Puzzle> puzzles = unfilteredPuzzles.entrySet().stream()
                    .filter(entry -> {
                        Visibility visibility = retrievedVisibilities.get(entry.getKey());
                        if (visibility == null) {
                            return false;
                        }
                        return !invisibleStatuses.contains(visibility.getStatus());
                    })
                    .map(entry -> entry.getValue().strip(retrievedVisibilities.get(entry.getKey())))
                    .collect(Collectors.toList());
            timerContext.stop();
            return Puzzles.builder()
                    .setPuzzles(puzzles)
                    .build();
        } else {
            SecurityUtils.getSubject().checkPermission(
                    new TeamsPermission("*", PermissionAction.READ));

            Map<String, Puzzle> unfilteredPuzzles = puzzleStore.getPuzzles();
            return Puzzles.builder()
                    .setPuzzles(ImmutableList.copyOf(unfilteredPuzzles.values()))
                    .build();
        }

    }

}
