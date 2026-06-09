package com.alfahospital.alfa_auth_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE                    = "alfa.exchange";
    public static final String PASSWORD_RESET_QUEUE        = "password.reset.queue";
    public static final String PASSWORD_RESET_ROUTING_KEY  = "password.reset.routing.key";
    public static final String WELCOME_GUEST_QUEUE         = "welcome.guest.queue";
    public static final String WELCOME_GUEST_ROUTING_KEY   = "welcome.guest.routing.key";
    public static final String USER_REGISTERED_QUEUE       = "user.registered.queue";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered.routing.key";

    @Bean
    public Queue passwordResetQueue() {
        return new Queue(PASSWORD_RESET_QUEUE, true);
    }

    @Bean
    public Queue welcomeGuestQueue() {
        return new Queue(WELCOME_GUEST_QUEUE, true);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding bindingPasswordReset(Queue passwordResetQueue, DirectExchange exchange) {
        return BindingBuilder.bind(passwordResetQueue).to(exchange).with(PASSWORD_RESET_ROUTING_KEY);
    }

    @Bean
    public Binding bindingWelcomeGuest(Queue welcomeGuestQueue, DirectExchange exchange) {
        return BindingBuilder.bind(welcomeGuestQueue).to(exchange).with(WELCOME_GUEST_ROUTING_KEY);
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

