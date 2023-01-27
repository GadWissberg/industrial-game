package com.gadarts.industrial.console.commands.types;

import com.gadarts.industrial.console.commands.ConsoleCommand;
import com.gadarts.industrial.console.commands.ConsoleCommandsList;

public class MusicCommand extends ConsoleCommand {
	@Override
	protected ConsoleCommandsList getCommandEnumValue() {
		return ConsoleCommandsList.MELODY;
	}
}