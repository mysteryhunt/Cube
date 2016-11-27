package edu.mit.puzzle.cube.core;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.common.base.Optional;

import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.environments.ServiceEnvironment;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventProcessor;

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
}
