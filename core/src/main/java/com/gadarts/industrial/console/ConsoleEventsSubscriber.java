package com.gadarts.industrial.console;

import com.gadarts.industrial.EventsSubscriber;
import com.gadarts.industrial.console.commands.ConsoleCommandParameter;
import com.gadarts.industrial.console.commands.ConsoleCommandResult;
import com.gadarts.industrial.console.commands.ConsoleCommands;

public interface ConsoleEventsSubscriber extends EventsSubscriber {
	default void onConsoleActivated( ) {

	}

	default boolean onCommandRun(ConsoleCommands command, ConsoleCommandResult consoleCommandResult) {
		return false;
	}

	default boolean onCommandRun(ConsoleCommands command,
								 ConsoleCommandResult consoleCommandResult,
								 ConsoleCommandParameter parameter) {
		return false;
	}


	default void onConsoleInitialized(Console console){

	}
}
