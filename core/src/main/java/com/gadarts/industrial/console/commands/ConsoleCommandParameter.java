package com.gadarts.industrial.console.commands;

import com.gadarts.industrial.console.Console;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ConsoleCommandParameter {
	private final String description;
	private final String alias;
	private boolean parameterValue;

	public ConsoleCommandParameter(final String description, final String alias) {
		this.description = description;
		this.alias = alias;
	}

	public void defineParameterValue(final String value,
									 final Console console,
									 final String messageOnParameterActivation,
									 final String messageOnParameterDeactivation) {
		boolean result;
		try {
			result = Integer.parseInt(value) == 1;
		} catch (NumberFormatException e) {
			result = false;
		}
		String msg = String.format(result ? messageOnParameterActivation : messageOnParameterDeactivation, alias);
		console.insertNewLog(msg, false);
		setParameterValue(result);
	}

	public abstract void run(String value, Console console);

	public boolean getParameterValue( ) {
		return parameterValue;
	}
}
