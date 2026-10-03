package com.ecommerce.notification_service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationStrategyFactory {

    private final List<NotificationStrategy> strategies;

    public NotificationStrategy getStrategy(String channel) {
        return strategies.stream()
                .filter(s -> s.getChannel().equalsIgnoreCase(channel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported notification channel: " + channel));
    }
}
