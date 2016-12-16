package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import com.google.common.collect.Maps;
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

    @Get
    public Puzzles handleGet() {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));
        Optional<String> puzzleId = Optional.ofNullable(getQueryValue("puzzleId"));
        if (teamId.isPresent()) {
            SecurityUtils.getSubject().checkPermission(
                    new TeamsPermission(teamId.get(), PermissionAction.READ));

            if (!puzzleId.isPresent()) {
                Map<String, Puzzle> unfilteredPuzzles = puzzleStore.getPuzzles();

                Map<String, Visibility> retrievedVisibilities = huntStatusStore.getVisibilitiesForTeam(teamId.get()).stream()
                        .collect(Collectors.toMap(
                                Visibility::getPuzzleId,
                                Function.identity()
                        ));

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
                return Puzzles.builder()
                        .setPuzzles(puzzles)
                        .build();
            } else {
                List<String> puzzleIds = Splitter.on(",").splitToList(puzzleId.get());
                Map<String, Puzzle> unfilteredPuzzles = Maps.toMap(puzzleIds, puzzleStore::getPuzzle);
                Map<String, Visibility> retrievedVisibilities = Maps.toMap(puzzleIds, pid -> huntStatusStore.getVisibility(teamId.get(), pid));
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
                return Puzzles.builder()
                        .setPuzzles(puzzles)
                        .build();
            }
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
