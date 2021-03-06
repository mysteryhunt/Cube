package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.InMemoryConnectionFactory;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.restlet.resource.ResourceException;

import java.sql.SQLException;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class UserStoreTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private ConnectionFactory connectionFactory;
    private UserStore userStore;

    private static String TEST_TEAM_ID = "testerteam";
    private static String TEST_PUZZLE_ID = "a_test_puzzle";

    @Before
    public void setup() throws SQLException {
        connectionFactory = new InMemoryConnectionFactory(
                new StandardVisibilityStatusSet(),
                Lists.newArrayList(TEST_TEAM_ID),
                Lists.newArrayList(Puzzle.create(TEST_PUZZLE_ID, "ANSWER")),
                ImmutableList.<User>of());
        userStore = new UserStore(connectionFactory);
    }

    @Test
    public void addAndGetUser() {
        User user = User.builder()
                .setUsername("testuser")
                .setPassword("testpassword")
                .setRoles(ImmutableList.of("writingteam"))
                .build();
        userStore.addUser(user);

        User readUser = userStore.getUser("testuser");
        assertThat(user.getUsername()).isEqualTo(readUser.getUsername());
        assertThat(readUser.getPassword()).isNull();
        assertThat(readUser.getRoles()).containsExactly("writingteam");
    }

    @Test
    public void getUser_doesNotExist() {
        exception.expect(ResourceException.class);
        userStore.getUser("testuser");
    }

    @Test
    public void addUser_roleDoesNotExist() {
        exception.expect(ResourceException.class);
        userStore.addUser(User.builder()
                .setUsername("testuser")
                .setPassword("testpassword")
                .setRoles(ImmutableList.of("nonexistentrole"))
                .build());
    }

    @Test
    public void updateUser_noChange() {
        User user = User.builder()
                .setUsername("testuser")
                .setPassword("testpassword")
                .setRoles(ImmutableList.of("writingteam"))
                .build();
        userStore.addUser(user);

        user = user.toBuilder()
                .setPassword(null)
                .build();
        assertThat(userStore.updateUser(user)).isFalse();
    }

    @Test
    public void updateUser_changePassword() {
        User user = User.builder()
                .setUsername("testuser")
                .setPassword("testpassword")
                .setRoles(ImmutableList.of("writingteam"))
                .build();
        userStore.addUser(user);

        user = user.toBuilder()
                .setPassword("newpassword")
                .build();
        assertThat(userStore.updateUser(user)).isTrue();

        // Trying to update the new password again should fail (since there's no actual password change).
        exception.expect(ResourceException.class);
        userStore.updateUser(user);
    }

    @Test
    public void updateUser_changeRole() {
        User user = User.builder()
                .setUsername("testuser")
                .setPassword("testpassword")
                .setRoles(ImmutableList.of("writingteam"))
                .build();
        userStore.addUser(user);

        user = user.toBuilder()
                .setPassword(null)
                .setRoles(ImmutableList.of("admin"))
                .build();
        assertThat(userStore.updateUser(user)).isTrue();

        User readUser = userStore.getUser("testuser");
        assertThat(readUser.getRoles()).containsExactly("admin");
    }

    @Test
    public void getAllUsers() {
        User user = User.builder()
                .setUsername("writinguser")
                .setPassword("testpassword")
                .setRoles(ImmutableList.of("writingteam"))
                .build();
        userStore.addUser(user);

        user = User.builder()
                .setUsername("adminuser")
                .setPassword("testpassword")
                .setRoles(ImmutableList.of("admin"))
                .build();
        userStore.addUser(user);

        user = User.builder()
                .setUsername(TEST_TEAM_ID)
                .setPassword("testpassword")
                .setTeamId(TEST_TEAM_ID)
                .build();
        userStore.addUser(user);

        List<User> allUsers = userStore.getAllUsers();
        assertThat(allUsers).containsExactly(
                User.builder()
                        .setUsername("writinguser")
                        .setRoles(ImmutableList.of("writingteam"))
                        .build(),
                User.builder()
                        .setUsername("adminuser")
                        .setRoles(ImmutableList.of("admin"))
                        .build(),
                User.builder()
                        .setUsername(TEST_TEAM_ID)
                        .setTeamId(TEST_TEAM_ID)
                        .setRoles(ImmutableList.of("solvingteam"))
                        .build()
        );
    }
}
