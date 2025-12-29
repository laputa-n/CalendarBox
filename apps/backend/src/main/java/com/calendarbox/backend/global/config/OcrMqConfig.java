package com.calendarbox.backend.global.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrMqConfig {
    public static final String EXCHANGE = "ocr.exchange";

    public static final String OCR_QUEUE = "ocr.queue";
    public static final String RK_RUN = "ocr.run";
    public static final String RK_RETRY_10S = "ocr.retry.10s";
    public static final String RK_RETRY_1M  = "ocr.retry.1m";
    public static final String RK_RETRY_10M = "ocr.retry.10m";
    public static final String RK_DLQ = "ocr.dlq";

    @Bean
    public DirectExchange ocrExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue ocrQueue() {
        return QueueBuilder.durable(OCR_QUEUE).build();
    }

    @Bean
    public Binding bindOcrQueue(DirectExchange ex) {
        return BindingBuilder.bind(ocrQueue()).to(ex).with(RK_RUN);
    }

    private Queue retryQueue(String name, int ttlMs) {
        return QueueBuilder.durable(name)
                .ttl(ttlMs)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(RK_RUN)
                .build();
    }

    @Bean public Queue retry10sQueue() { return retryQueue("ocr.retry.10s.queue", 10_000); }
    @Bean public Queue retry1mQueue()  { return retryQueue("ocr.retry.1m.queue", 60_000); }
    @Bean public Queue retry10mQueue() { return retryQueue("ocr.retry.10m.queue", 600_000); }

    @Bean public Binding bindRetry10s(DirectExchange ex) {
        return BindingBuilder.bind(retry10sQueue()).to(ex).with(RK_RETRY_10S);
    }
    @Bean public Binding bindRetry1m(DirectExchange ex) {
        return BindingBuilder.bind(retry1mQueue()).to(ex).with(RK_RETRY_1M);
    }
    @Bean public Binding bindRetry10m(DirectExchange ex) {
        return BindingBuilder.bind(retry10mQueue()).to(ex).with(RK_RETRY_10M);
    }

    @Bean public Queue dlqQueue() { return QueueBuilder.durable("ocr.dlq.queue").build(); }

    @Bean public Binding bindDlq(DirectExchange ex) {
        return BindingBuilder.bind(dlqQueue()).to(ex).with(RK_DLQ);
    }
}

