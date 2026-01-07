package com.calendarbox.backend.global.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingMqConfig {
    public static final String EXCHANGE = "embedding.exchange";

    public static final String EMBEDDING_QUEUE = "embedding.queue";
    public static final String RK_RUN = "embedding.run";
    public static final String RK_RETRY_10S = "embedding.retry.10s";
    public static final String RK_RETRY_1M  = "embedding.retry.1m";
    public static final String RK_RETRY_10M = "embedding.retry.10m";
    public static final String RK_DLQ = "embedding.dlq";

    @Bean
    public DirectExchange embeddingExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue embeddingQueue() {
        return QueueBuilder.durable(EMBEDDING_QUEUE).build();
    }

    @Bean
    public Binding bindEmbeddingQueue(@Qualifier("embeddingExchange") DirectExchange ex) {
        return BindingBuilder.bind(embeddingQueue()).to(ex).with(RK_RUN);
    }

    private Queue retryQueue(String name, int ttlMs) {
        return QueueBuilder.durable(name)
                .ttl(ttlMs)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(RK_RUN)
                .build();
    }

    @Bean(name = "embeddingRetry10sQueue")
    public Queue retry10sQueue() { return retryQueue("embedding.retry.10s.queue", 10_000); }

    @Bean(name = "embeddingRetry1mQueue")
    public Queue retry1mQueue()  { return retryQueue("embedding.retry.1m.queue", 60_000); }

    @Bean(name = "embeddingRetry10mQueue")
    public Queue retry10mQueue() { return retryQueue("embedding.retry.10m.queue", 600_000); }

    @Bean(name = "embeddingBindRetry10s")
    public Binding bindRetry10s(@Qualifier("embeddingExchange") DirectExchange ex) {
        return BindingBuilder.bind(retry10sQueue()).to(ex).with(RK_RETRY_10S);
    }
    @Bean(name = "embeddingBindRetry1m")
    public Binding bindRetry1m(@Qualifier("embeddingExchange") DirectExchange ex) {
        return BindingBuilder.bind(retry1mQueue()).to(ex).with(RK_RETRY_1M);
    }
    @Bean(name = "embeddingBindRetry10m")
    public Binding bindRetry10m(@Qualifier("embeddingExchange") DirectExchange ex) {
        return BindingBuilder.bind(retry10mQueue()).to(ex).with(RK_RETRY_10M);
    }

    @Bean(name = "embeddingDlqQueue")
    public Queue dlqQueue() { return QueueBuilder.durable("embedding.dlq.queue").build(); }

    @Bean(name = "embeddingBindDlq")
    public Binding bindDlq(@Qualifier("embeddingExchange") DirectExchange ex) {
        return BindingBuilder.bind(dlqQueue()).to(ex).with(RK_DLQ);
    }
}
