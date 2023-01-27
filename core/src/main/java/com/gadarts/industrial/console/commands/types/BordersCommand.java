package com.gadarts.industrial.console.commands.types;

import com.gadarts.industrial.console.commands.ConsoleCommand;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;

public class BordersCommand extends ConsoleCommand {
	@Override
	protected ConsoleCommandsList getCommandEnumValue() {
		return ConsoleCommandsList.BORDERS;
	}
}
