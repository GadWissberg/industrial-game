package com.gadarts.industrial.console.commands;

import java.util.HashMap;
import java.util.Map;

public class CommandInvoke {
	private final ConsoleCommandsList command;
	private final Map<String, String> parameters = new HashMap<>();

	public CommandInvoke(final ConsoleCommandsList command) {
		this.command = command;
	}

	public ConsoleCommandsList getCommand( ) {
		return command;
	}

	public void addParameter(final String parameter, final String value) {
		parameters.put(parameter, value);
	}

	public Map<String, String> getParameters( ) {
		return parameters;
	}
}
