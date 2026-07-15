package com.conceptarena.conceptbank.app.bus;

import com.conceptarena.kernel.command.Command;

@FunctionalInterface
public interface CommandHandler<T extends Command<R>, R> {
    R handle(T command);
}
