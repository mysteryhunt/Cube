package edu.mit.puzzle.cube.core;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = {CubeModule.class, CubeConfigModule.class})
@Singleton
public interface CubeComponent {
    HuntDefinition getHuntDefinition();
    ServiceEnvironment getServiceEnvironment();
    ConnectionFactory getConnectionFactory();
    CompositeEventProcessor getCompositeEventProcessor();

    void injectHuntDefinition(HuntDefinition huntDefinition);

    CubeResourceComponent.Builder getCubeResourceComponentBuilder();
}
