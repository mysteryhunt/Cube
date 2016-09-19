package edu.mit.puzzle.cube.core;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(subcomponents = CubeResourceComponent.class)
public class CubeModule {
    @Provides
    @Singleton
    ConnectionFactory provideConnectionFactory(ServiceEnvironment serviceEnvironment) {
        return serviceEnvironment.getConnectionFactory();
    }

    @Provides
    @Singleton
    CompositeEventProcessor provideCompositeEventProcessor(HuntDefinition huntDefinition) {
        return huntDefinition.generateCompositeEventProcessor();
    }

    @Provides
    EventProcessor<Event> provideEventProcessor(CompositeEventProcessor compositeEventProcessor) {
        return compositeEventProcessor;
    }
}
