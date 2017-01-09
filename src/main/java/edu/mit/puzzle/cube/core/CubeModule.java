package edu.mit.puzzle.cube.core;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

import edu.mit.puzzle.cube.core.CubeConfig.AmazonSNSConfig;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
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

    @Provides
    @Singleton
    MetricRegistry provideMetricRegistry(
            Optional<Graphite> graphite,
            @Named("graphitePrefix") Optional<String> graphitePrefix
    ) {
        MetricRegistry metricRegistry = new MetricRegistry();
        if (graphite.isPresent() && graphitePrefix.isPresent()) {
            GraphiteReporter reporter = GraphiteReporter
                    .forRegistry(metricRegistry)
                    .prefixedWith(graphitePrefix.get())
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL)
                    .build(graphite.get());
            reporter.start(15, TimeUnit.SECONDS);
        }
        return metricRegistry;
    }

    @Provides
    @Singleton
    Optional<AmazonSNSAsync> provideAmazonSNSAsync(Optional<AmazonSNSConfig> amazonSNSConfig) {
        if (!amazonSNSConfig.isPresent()) {
            return Optional.empty();
        }
        AWSCredentials awsCredentials = new BasicAWSCredentials(
                amazonSNSConfig.get().getAccessKey(),
                amazonSNSConfig.get().getSecretKey()
        );
        return Optional.of(new AmazonSNSAsyncClient(awsCredentials));
    }

    @Provides
    @Named("amazonSNSTopicArn")
    Optional<String> provideAmazonSNSTopicArn(Optional<AmazonSNSConfig> amazonSNSConfig) {
        if (!amazonSNSConfig.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(amazonSNSConfig.get().getTopicArn());
    }
}
