package edu.mit.puzzle.cube.huntimpl.setec2017;

import com.fasterxml.jackson.databind.JsonNode;

import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.RestletTest;
import edu.mit.puzzle.cube.core.db.CubeJdbcRealm;
import edu.mit.puzzle.cube.core.model.HintRequest;
import edu.mit.puzzle.cube.core.model.HintRequestStatus;
import edu.mit.puzzle.cube.huntimpl.setec2017.Setec2017HuntDefinition.Character;

import org.apache.shiro.realm.Realm;
import org.junit.Test;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class Setec2017HuntRunTest extends RestletTest {
    protected static final ChallengeResponse TESTERTEAM_CREDENTIALS =
            new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "testerteam", "testerteampassword");

    @Override
    protected HuntDefinition createHuntDefinition() {
        return new Setec2017HuntDefinition();
    }

    @Override
    protected Realm createAuthenticationRealm() {
        CubeJdbcRealm realm = new CubeJdbcRealm();
        realm.setDataSource(serviceEnvironment.getConnectionFactory().getDataSource());
        return realm;
    }

    private int getGold() {
        JsonNode json = get("/teams/testerteam");
        return json.get("teamProperties").get("GoldProperty").get("gold").asInt();
    }

    private int getCharacterLevel(Character character) {
        JsonNode json = get("/teams/testerteam");
        JsonNode characterNode = json
                .get("teamProperties")
                .get("CharacterLevelsProperty")
                .get("levels")
                .get(character.name());
        if (characterNode != null) {
            return characterNode.asInt();
        } else {
            return 0;
        }
    }

    @Test
    public void testHintRun() throws IOException {
        postHuntStart();

        assertThat(getCharacterLevel(Character.FIGHTER)).isEqualTo(0);
        assertThat(getGold()).isEqualTo(0);

        JsonNode json = getVisibility("testerteam", "fighter");
        assertThat(json.get("status").asText()).isEqualTo("UNLOCKED");
        json = getVisibility("testerteam", "f1");
        assertThat(json.get("status").asText()).isEqualTo("UNLOCKED");
        json = getVisibility("testerteam", "f3");
        assertThat(json.get("status").asText()).isEqualTo("INVISIBLE");
        json = getVisibility("testerteam", "dynast");
        assertThat(json.get("status").asText()).isEqualTo("INVISIBLE");

        currentUserCredentials = TESTERTEAM_CREDENTIALS;
        postNewSubmission("testerteam", "f1", "FIGHTER1");
        currentUserCredentials = ADMIN_CREDENTIALS;
        postUpdateSubmission(1, "CORRECT");

        assertThat(getCharacterLevel(Character.FIGHTER)).isEqualTo(1);
        assertThat(getCharacterLevel(Character.WIZARD)).isEqualTo(0);
        assertThat(getGold()).isEqualTo(1);

        json = post(
                "/hintrequests",
                HintRequest.builder()
                        .setTeamId("testerteam")
                        .setPuzzleId("f2")
                        .setRequest("help")
                        .build()
        );
        assertThat(json.get("created").asBoolean()).isTrue();
        assertThat(getGold()).isEqualTo(0);

        // Reject the hint request. One gold should be credited back to the team.
        currentUserCredentials = ADMIN_CREDENTIALS;
        post(
                "/hintrequests/1",
                HintRequest.builder()
                        .setHintRequestId(1)
                        .setCallerUsername("adminuser")
                        .setStatus(HintRequestStatus.REJECTED)
                        .build()
        );
        assertThat(getGold()).isEqualTo(1);
    }
}
