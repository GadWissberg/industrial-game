package com.gadarts.industrial.systems.ui.storage;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.gadarts.industrial.shared.model.pickups.ItemDefinition;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.systems.ui.window.GameWindowEvent;
import com.gadarts.industrial.systems.ui.window.GameWindowEventType;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ItemDisplay extends Image {

	public static final float FLICKER_DURATION = 0.2f;
	private static final Vector2 auxVector = new Vector2();
	private final Item item;
	private final ItemSelectionHandler itemSelectionHandler;

	@Setter
	private Class<? extends Table> locatedIn;

	public ItemDisplay(final Item item,
					   final ItemSelectionHandler itemSelectionHandler,
					   final Class<? extends Table> locatedIn) {
		super(item.getImage());
		this.item = item;
		this.itemSelectionHandler = itemSelectionHandler;
		this.locatedIn = locatedIn;
		addListener(new InputListener() {
			@Override
			public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer, final int button) {
				boolean result = false;
				if (button == Input.Buttons.LEFT) {
					result = onLeftClick(event, x, y);
				} else if (button == Input.Buttons.RIGHT) {
					onRightClick();
					result = true;
				}
				return result;
			}
		});
	}

	@Override
	public void clearActions() {
		super.clearActions();
		setColor(Color.WHITE);
	}

	@Override
	public Actor hit(final float x, final float y, final boolean touchable) {
		if (!isVisible() || (touchable && getTouchable() != Touchable.enabled) || x < 0 || y < 0) return null;
		ItemDefinition def = item.getDefinition();
		int col = (int) MathUtils.map(0, getPrefWidth(), 0, def.getWidth(), x);
		int row = ((int) MathUtils.map(0, getPrefHeight(), 0, def.getSymbolHeight(), y));
		float cellIndex = def.getWidth() * row + col;
		if (cellIndex < 0 || cellIndex >= def.getWidth() * def.getSymbolHeight()) return null;
		return def.getMask()[(int) cellIndex] == 1 ? this : null;
	}

	protected void onRightClick() {
		fire(new GameWindowEvent(this, GameWindowEventType.CLICK_RIGHT));
	}

	private boolean onLeftClick(final InputEvent event, final float x, final float y) {
		boolean result = false;
		if (itemSelectionHandler.getSelection() == null) {
			fire(new GameWindowEvent(ItemDisplay.this, GameWindowEventType.ITEM_SELECTED));
			result = true;
		} else {
			passClickToBehind(event, x, y);
		}
		return result;
	}

	private void passClickToBehind(final InputEvent event, final float x, final float y) {
		Vector2 stageCoordinates = localToStageCoordinates(auxVector.set(x, y));
		setTouchable(Touchable.disabled);
		Actor behind = getStage().hit(stageCoordinates.x, stageCoordinates.y, true);
		if (behind != null) {
			behind.notify(event, false);
		}
		setTouchable(Touchable.enabled);
	}

	public void applyFlickerAction() {
		addAction(
				Actions.forever(
						Actions.sequence(
								Actions.color(Color.BLACK, FLICKER_DURATION, Interpolation.smooth2),
								Actions.color(Color.WHITE, FLICKER_DURATION, Interpolation.smooth2)
						)
				)
		);
	}

	public boolean isWeapon() {
		return item.isWeapon();
	}
}
