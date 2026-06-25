package com.alfahospital.alfa_auth_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ──────────────────────────────────────────────────────────────────
    // IMPORTANTE: Este servicio es SOLO PRODUCTOR para las queues de
    // password.reset y welcome.guest. Las queues ya fueron declaradas
    // por alfa-notificaciones-service con los argumentos DLQ correctos.
    // Redeclararlas aquí con argumentos distintos causa PRECONDITION_FAILED.
    // Solo declaramos el exchange y la queue user.registered (propia).
    // ──────────────────────────────────────────────────────────────────

    public static final String EXCHANGE                    = "alfa.exchange";
    public static final String PASSWORD_RESET_ROUTING_KEY  = "password.reset.routing.key";
    public static final String WELCOME_GUEST_ROUTING_KEY   = "welcome.guest.routing.key";
    public static final String USER_REGISTERED_QUEUE       = "user.registered.queue";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered.routing.key";

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return new Queue(USER_REGISTERED_QUEUE, true);
    }

    @Bean
    public Binding bindingUserRegistered(Queue userRegisteredQueue, DirectExchange exchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(exchange).with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

