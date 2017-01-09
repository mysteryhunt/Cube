package edu.mit.puzzle.cube.core;

import com.amazonaws.services.sns.AmazonSNSAsync;
import com.codahale.metrics.MetricRegistry;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;

import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;

@Component(modules = {CubeModule.class, CubeConfigModule.class})
@Singleton
public interface CubeComponent {
    HuntDefinition getHuntDefinition();
    ServiceEnvironment getServiceEnvironment();
    ConnectionFactory getConnectionFactory();
    CompositeEventProcessor getCompositeEventProcessor();
    MetricRegistry getMetricRegistry();

    Optional<AmazonSNSAsync> getAmazonSNSAsync();
    @Named("amazonSNSTopicArn") Optional<String> getAmazonSNSTopicArn();

    void injectHuntDefinition(HuntDefinition huntDefinition);

    CubeResourceComponent.Builder getCubeResourceComponentBuilder();
}
