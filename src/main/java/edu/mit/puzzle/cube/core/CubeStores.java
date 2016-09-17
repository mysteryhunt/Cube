package edu.mit.puzzle.cube.core;

import com.google.auto.value.AutoValue;

import edu.mit.puzzle.cube.core.model.HintRequestStore;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.PuzzleStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.model.UserStore;

@AutoValue
public abstract class CubeStores {
    public static CubeStores create(
            HintRequestStore hintRequestStore,
            HuntStatusStore huntStatusStore,
            PuzzleStore puzzleStore,
            SubmissionStore submissionStore,
            UserStore userStore
    ) {
        return new AutoValue_CubeStores(
                hintRequestStore,
                huntStatusStore,
                puzzleStore,
                submissionStore,
                userStore
        );
    }

    public abstract HintRequestStore getHintRequestStore();
    public abstract HuntStatusStore getHuntStatusStore();
    public abstract PuzzleStore getPuzzleStore();
    public abstract SubmissionStore getSubmissionStore();
    public abstract UserStore getUserStore();
}
