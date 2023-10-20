package com.gadarts.industrial.console;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ConsoleScrollPane extends ScrollPane {
	private boolean scrollToEnd = true;

	public ConsoleScrollPane(Actor actor) {
		super(actor);
	}
}
