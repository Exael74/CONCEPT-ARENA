package com.conceptarena.auth.app.bus;

import com.conceptarena.kernel.command.Command;

public interface CommandBus {
    <T> T dispatch(Command<T> command);
}
