package com.conceptarena.conceptbank.infra.bus;

import com.conceptarena.conceptbank.app.bus.CommandBus;
import com.conceptarena.conceptbank.app.bus.CommandHandler;
import com.conceptarena.kernel.command.Command;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryCommandBus implements CommandBus {

    private final Map<Class<?>, CommandHandler<?, ?>> handlers = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T dispatch(Command<T> command) {
        CommandHandler<Command<T>, T> handler = (CommandHandler<Command<T>, T>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for: " + command.getClass().getName());
        }
        return handler.handle(command);
    }

    public <C extends Command<R>, R> void register(Class<C> commandClass, CommandHandler<C, R> handler) {
        handlers.put(commandClass, handler);
    }
}
