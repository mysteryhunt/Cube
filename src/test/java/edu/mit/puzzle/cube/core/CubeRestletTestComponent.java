package edu.mit.puzzle.cube.core;

import com.amazonaws.services.sns.AmazonSNSAsync;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;

import java.util.Optional;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = {CubeModule.class, CubeRestletTestModule.class})
@Singleton
public interface CubeRestletTestComponent {
    HuntDefinition getHuntDefinition();
    ServiceEnvironment getServiceEnvironment();
    ConnectionFactory getConnectionFactory();
    CompositeEventProcessor getCompositeEventProcessor();
    Optional<AmazonSNSAsync> getAmazonSNSAsync();

    void injectHuntDefinition(HuntDefinition huntDefinition);

    CubeResourceComponent.Builder getCubeResourceComponentBuilder();
}
