package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
import java.util.function.Function;
import java.util.stream.Collectors;

public class PuzzlesResource extends AbstractCubeResource {

    @Get
    public Puzzles handleGet() {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));
        if (teamId.isPresent()) {
            SecurityUtils.getSubject().checkPermission(
                    new TeamsPermission(teamId.get(), PermissionAction.READ));

            Map<String, Puzzle> unfilteredPuzzles = puzzleStore.getPuzzles();
            Map<String, Visibility> retrievedVisibilities = huntStatusStore.getVisibilitiesForTeam(teamId.get()).stream()
                    .collect(Collectors.toMap(
                            Visibility::getPuzzleId,
                            Function.identity()
                    ));
            Map<String, Visibility> visibilities = Maps.asMap(
                    unfilteredPuzzles.keySet(),
                    puzzleId -> {
                        Visibility visibility = retrievedVisibilities.get(puzzleId);
                        if (visibility == null) {
                            visibility = Visibility.builder()
                                    .setTeamId(teamId.get())
                                    .setPuzzleId(puzzleId)
                                    .setStatus(huntStatusStore.getVisibilityStatusSet().getDefaultVisibilityStatus())
                                    .build();
                        }
                        return visibility;
                    });
            List<Puzzle> puzzles = unfilteredPuzzles.entrySet().stream()
                    .filter(entry -> !huntStatusStore.getVisibilityStatusSet().getInvisibleStatuses().contains(
                            visibilities.get(entry.getKey()).getStatus()))
                    .map(entry -> entry.getValue().strip(visibilities.get(entry.getKey())))
                    .collect(Collectors.toList());
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