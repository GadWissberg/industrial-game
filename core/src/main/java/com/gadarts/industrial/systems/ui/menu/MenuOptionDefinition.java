package com.gadarts.industrial.systems.ui.menu;

public interface MenuOptionDefinition {
	String getLabel();

	MenuOptionAction getAction();

	MenuOptionDefinition[] getSubOptions();

}
