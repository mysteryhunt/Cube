package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.collect.ImmutableList;

import edu.mit.puzzle.cube.core.model.Visibilities;
import edu.mit.puzzle.cube.core.model.Visibility;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.VisibilitiesPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import java.util.List;
import java.util.Optional;

public class VisibilitiesResource extends AbstractCubeResource {

    @Get
    public Visibilities handleGet() {
        Optional<String> teamId = Optional.ofNullable(getQueryValue("teamId"));
        if (teamId.isPresent()) {
            SecurityUtils.getSubject().checkPermission(
                    new VisibilitiesPermission(teamId.get(), PermissionAction.READ));
        } else {
            throw new ResourceException(
                    Status.CLIENT_ERROR_BAD_REQUEST,
                    "A team id must be specified");
        }
        Optional<String> puzzleId = Optional.ofNullable(getQueryValue("puzzleId"));
        puzzleId = puzzleId.map(puzzleStore::getCanonicalPuzzleId);

        List<Visibility> visibilities;

        if (puzzleId.isPresent()) {
            visibilities = ImmutableList.of(huntStatusStore.getVisibility(teamId.get(), puzzleId.get()));
        } else {
            visibilities = huntStatusStore.getVisibilitiesForTeam(teamId.get());
        }

        return Visibilities.builder()
                .setVisibilities(visibilities)
                .build();
    }
}
