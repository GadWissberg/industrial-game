package com.gadarts.industrial.systems.ui.window;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GameWindowEvent extends ChangeListener.ChangeEvent {

	private GameWindowEventType type;


	public GameWindowEvent(final Actor target, final GameWindowEventType type) {
		setTarget(target);
		this.type = type;
	}

	@Override
	public void reset() {
		super.reset();
		type = null;
	}
}
