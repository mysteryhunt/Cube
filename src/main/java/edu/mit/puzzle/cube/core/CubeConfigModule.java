package edu.mit.puzzle.cube.core;

import edu.mit.puzzle.cube.core.environments.DevelopmentEnvironment;
import edu.mit.puzzle.cube.core.environments.ProductionEnvironment;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class CubeConfigModule {
    private static Logger LOGGER = LoggerFactory.getLogger(CubeConfigModule.class);

    private final CubeConfig config;

    public CubeConfigModule(CubeConfig config) {
        this.config = config;
    }

    @Provides
    @Singleton
    HuntDefinition provideHuntDefinition() {
        return HuntDefinition.forClassName(config.getHuntDefinitionClassName());
    }

    @Provides
    @Singleton
    ServiceEnvironment provideServiceEnvironment(HuntDefinition huntDefinition) {
        switch (config.getServiceEnvironment()) {
        case DEVELOPMENT:
            return new DevelopmentEnvironment(huntDefinition);
        case PRODUCTION:
            return new ProductionEnvironment(config);
        default:
            LOGGER.error("Unimplemented service environment: " + config.getServiceEnvironment());
            System.exit(1);
            return null;
        }
    }
}
