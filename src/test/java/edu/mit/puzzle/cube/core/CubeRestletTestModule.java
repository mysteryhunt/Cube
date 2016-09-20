package edu.mit.puzzle.cube.core;

import edu.mit.puzzle.cube.core.environments.DevelopmentEnvironment;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class CubeRestletTestModule {
    private final HuntDefinition huntDefinition;

    CubeRestletTestModule(HuntDefinition huntDefinition) {
        this.huntDefinition = huntDefinition;
    }

    @Provides
    @Singleton
    HuntDefinition provideHuntDefinition() {
        return huntDefinition;
    }

    @Provides
    @Singleton
    ServiceEnvironment provideServiceEnvironment(HuntDefinition huntDefinition) {
        return new DevelopmentEnvironment(huntDefinition);
    }
}
