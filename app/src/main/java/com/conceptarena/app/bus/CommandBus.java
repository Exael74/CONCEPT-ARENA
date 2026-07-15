package com.conceptarena.app.bus;

import com.conceptarena.core.shared.command.Command;

public interface CommandBus {
    <T> T dispatch(Command<T> command);
}
