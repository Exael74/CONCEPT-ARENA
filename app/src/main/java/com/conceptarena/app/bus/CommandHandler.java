package com.conceptarena.app.bus;

import com.conceptarena.core.shared.command.Command;

@FunctionalInterface
public interface CommandHandler<T extends Command<R>, R> {
    R handle(T command);
}
