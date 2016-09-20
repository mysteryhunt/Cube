package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.model.HintRequestStore;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.PuzzleStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.model.UserStore;

import org.restlet.resource.ServerResource;

import javax.inject.Inject;

public abstract class AbstractCubeResource extends ServerResource {
    @Inject EventProcessor<Event> eventProcessor;
    @Inject HintRequestStore hintRequestStore;
    @Inject HuntStatusStore huntStatusStore;
    @Inject PuzzleStore puzzleStore;
    @Inject SubmissionStore submissionStore;
    @Inject UserStore userStore;
}
