package org.folio.services.impl;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.folio.kafka.KafkaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterators.cycle;
import static org.folio.rest.RestVerticle.MODULE_SPECIFIC_ARGS;

@Component
public class KafkaProducersBuilder {

  // number of producers to be created is equal to allocated thread pool
  private static final int NUMBER_OF_PRODUCERS =
    Integer.parseInt(MODULE_SPECIFIC_ARGS.getOrDefault("event.publishing.thread.pool.size", "20"));
  private Iterator<KafkaProducer<String, String>> producerIterator;

  public KafkaProducersBuilder(@Autowired Vertx vertx, @Autowired KafkaConfig config) {
    List<KafkaProducer<String, String>> producers =
      Stream.generate(() -> KafkaProducer.<String, String>create(vertx, config.getProducerProps()))
        .limit(NUMBER_OF_PRODUCERS)
        .collect(Collectors.toList());
    producerIterator = cycle(new UnmodifiableList<>(producers));
  }

  public KafkaProducer<String, String> getKafkaProducer() {
    return Stream.generate(producerIterator::next)
      .filter(producer -> !producer.writeQueueFull())
      .findAny()
      .orElseThrow(() -> new RuntimeException("No eligible Kafka Producer available"));
  }
}
