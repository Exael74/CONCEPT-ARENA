package com.conceptarena.game.app.bus;

import com.conceptarena.kernel.command.Command;

public interface CommandBus {
    <T> T dispatch(Command<T> command);
}
