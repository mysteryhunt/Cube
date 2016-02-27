package edu.mit.puzzle.cube.huntimpl.linearexample;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.events.*;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.SubmissionStatus;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;

import java.util.List;
import java.util.Map;

public class LinearExampleHuntDefinition implements HuntDefinition {

    private static final VisibilityStatusSet VISIBILITY_STATUS_SET = new StandardVisibilityStatusSet();

    @Override
    public VisibilityStatusSet getVisibilityStatusSet() {
        return VISIBILITY_STATUS_SET;
    }

    private static final List<String> PUZZLES;
    static {
        ImmutableList.Builder<String> puzzleBuilder = ImmutableList.builder();
        for (int i = 1; i <= 7 ; ++i) {
            puzzleBuilder.add("puzzle" + i);
        }
        PUZZLES = puzzleBuilder.build();
    }

    @Override
    public List<String> getPuzzleList() {
        return PUZZLES;
    }

    private static final Map<String,String> DIRECT_UNLOCK_PREREQS;
    static {
        ImmutableMap.Builder<String,String> directPrereqBuilder = ImmutableMap.builder();
        for (int i = 1; i <= 6; ++i) {
            directPrereqBuilder.put("puzzle" + i, "puzzle" + (i+1));
        }
        DIRECT_UNLOCK_PREREQS = directPrereqBuilder.build();
    }

    @Override
    public void addToEventProcessor(
            CompositeEventProcessor eventProcessor,
            HuntStatusStore huntStatusStore
    ) {
        eventProcessor.addEventProcessor(event -> {
            if (!event.getEventType().equals("SubmissionComplete")) {
                return;
            }
            Submission submission = (Submission) event.getAttribute("submission");
            if (submission.getStatus().equals(SubmissionStatus.CORRECT)) {
                huntStatusStore.setVisibility(
                        submission.getTeamId(),
                        submission.getPuzzleId(),
                        "SOLVED",
                        false
                );
            }
        });

        eventProcessor.addEventProcessor(event -> {
            if (!event.getEventType().equals("FullRelease")) {
                return;
            }
            String runId = (String) event.getAttribute("runId");
            String puzzleId = (String) event.getAttribute("puzzleId");
            for (String teamId : huntStatusStore.getTeamIds(runId)) {
                huntStatusStore.setVisibility(
                        teamId,
                        puzzleId,
                        "UNLOCKED",
                        false
                );
            }
        });

        eventProcessor.addEventProcessor(event -> {
            if (!event.getEventType().equals("HuntStart")) {
                return;
            }
            String runId = (String) event.getAttribute("runId");
            boolean changed = huntStatusStore.recordHuntRunStart(runId);
            if (changed) {
                for (String teamId : huntStatusStore.getTeamIds(runId)) {
                    huntStatusStore.setVisibility(teamId, "puzzle1", "UNLOCKED", false);
                }
            }
        });

        for (Map.Entry<String,String> directPrereqEntry : DIRECT_UNLOCK_PREREQS.entrySet()) {
            eventProcessor.addEventProcessor(event -> {
                if (!event.getEventType().equals("VisibilityChange")) {
                    return;
                }
                String teamId = (String) event.getAttribute("teamId");
                String puzzleId = (String) event.getAttribute("puzzleId");
                String status = (String) event.getAttribute("status");

                if (status.equals("SOLVED") && puzzleId.equals(directPrereqEntry.getKey())) {
                    huntStatusStore.setVisibility(teamId, directPrereqEntry.getValue(),
                            "UNLOCKED", false);
                }
            });
        }
    }
}
