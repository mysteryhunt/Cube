package edu.mit.puzzle.cube.core.simulation;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SimulatedTeam {

    public abstract String teamId();

    public static Builder builder() {
        return new AutoValue_SimulatedTeam.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setTeamId(String teamId);

        public abstract SimulatedTeam build();
    }

}
