package edu.mit.puzzle.cube.core.serverresources;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import edu.mit.puzzle.cube.core.model.HintRequestStore;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.InteractionRequestStore;
import edu.mit.puzzle.cube.core.model.PuzzleStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.model.UserStore;

import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ServerResource;

import javax.inject.Inject;

public abstract class AbstractCubeResource extends ServerResource {
    @Inject EventProcessor<Event> eventProcessor;
    @Inject HintRequestStore hintRequestStore;
    @Inject HuntStatusStore huntStatusStore;
    @Inject InteractionRequestStore interactionRequestStore;
    @Inject PuzzleStore puzzleStore;
    @Inject SubmissionStore submissionStore;
    @Inject UserStore userStore;

    @Inject MetricRegistry metricRegistry;

    private static Timer latencyTimer;

    private Timer getLatencyTimer() {
        if (latencyTimer == null) {
            latencyTimer = metricRegistry.timer(MetricRegistry.name(this.getClass(), "latency"));
        }
        return latencyTimer;
    }

    @Override
    protected Representation doNegotiatedHandle() {
        Timer.Context timerContext = getLatencyTimer().time();
        try {
            return super.doNegotiatedHandle();
        } finally {
            timerContext.stop();
        }
    }
}
