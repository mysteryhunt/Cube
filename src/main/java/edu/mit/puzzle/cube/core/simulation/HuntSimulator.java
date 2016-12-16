package edu.mit.puzzle.cube.core.simulation;

import edu.mit.puzzle.cube.core.CubeComponent;
import edu.mit.puzzle.cube.core.CubeConfig;
import edu.mit.puzzle.cube.core.CubeConfigModule;
import edu.mit.puzzle.cube.core.DaggerCubeComponent;
import edu.mit.puzzle.cube.core.HuntDefinition;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;

import javax.inject.Inject;

public class HuntSimulator {

    public static void main(String[] args) {
        HuntSimulation sim = new HuntSimulation(
                SimulatedTeam.builder().setTeamId("testerteam10").build()
        );
    }

}
