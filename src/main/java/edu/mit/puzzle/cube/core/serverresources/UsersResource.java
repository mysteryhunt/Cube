package edu.mit.puzzle.cube.core.serverresources;

import com.google.common.base.Preconditions;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.User;
import edu.mit.puzzle.cube.core.permissions.PermissionAction;
import edu.mit.puzzle.cube.core.permissions.UsersPermission;

import org.apache.shiro.SecurityUtils;
import org.restlet.resource.Post;

public class UsersResource extends AbstractCubeResource {

    @Post
    public PostResult handlePost(User user) {
        SecurityUtils.getSubject().checkPermission(
                new UsersPermission(user.getUsername(), PermissionAction.CREATE));
        Preconditions.checkArgument(
                !user.getUsername().isEmpty(),
                "The username must be non-empty"
        );
        Preconditions.checkArgument(
                user.getPassword() != null && !user.getPassword().isEmpty(),
                "A password must be provided when creating a user"
        );
        userStore.addUser(user);
        return PostResult.builder().setCreated(true).build();
    }
}
