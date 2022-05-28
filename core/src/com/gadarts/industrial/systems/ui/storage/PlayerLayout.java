package com.gadarts.industrial.systems.ui.storage;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import com.gadarts.industrial.systems.ui.window.GameWindowEvent;
import com.gadarts.industrial.systems.ui.window.GameWindowEventType;
import lombok.Getter;

import java.util.List;

public class PlayerLayout extends ItemsTable {
	public static final int WEAPON_POSITION_PARENT_X = 100;
	public static final int WEAPON_POSITION_PARENT_Y = 200;
	public static final String NAME = "player_layout";
	private static final Vector2 auxVector = new Vector2();
	private static final float SPOT_RADIUS = 25;

	@Getter
	private ItemDisplay weaponChoice;

	public PlayerLayout(Texture texture,
						Weapon weaponChoice,
						ItemSelectionHandler itemSelectionHandler,
						SystemsCommonData systemsCommonData,
						List<UserInterfaceSystemEventsSubscriber> subscribers) {
		super(itemSelectionHandler);
		this.weaponChoice = new ItemDisplay(weaponChoice, this.itemSelectionHandler, PlayerLayout.class);
		setTouchable(Touchable.enabled);
		setName(NAME);
		setBackground(new TextureRegionDrawable(texture));
		addListener(event -> {
			boolean result = false;
			if (event instanceof GameWindowEvent) {
				result = onGameWindowEvent(event, systemsCommonData, subscribers);
			}
			return result;
		});
		addListener(new ClickListener() {
			@Override
			public boolean touchDown(final InputEvent event,
									 final float x,
									 final float y,
									 final int pointer,
									 final int button) {
				super.touchDown(event, x, y, pointer, button);
				boolean result;
				result = onClick(x, y, button);
				return result;
			}

		});
	}

	private boolean onClick(final float x, final float y, final int button) {
		boolean result = false;
		if (button == Input.Buttons.RIGHT) {
			onRightClick();
			result = true;
		} else if (button == Input.Buttons.LEFT) {
			result = onLeftClick(x, y);
		}
		return result;
	}

	private boolean onLeftClick(final float x, final float y) {
		Vector2 local = auxVector.set(WEAPON_POSITION_PARENT_X, WEAPON_POSITION_PARENT_Y);
		float distance = parentToLocalCoordinates(local).dst(x, y);
		boolean result = false;
		if (itemSelectionHandler != null && PlayerLayout.this.weaponChoice == null && distance < SPOT_RADIUS) {
			PlayerLayout.this.fire(new GameWindowEvent(PlayerLayout.this, GameWindowEventType.ITEM_PLACED));
			result = true;
		}
		return result;
	}

	private boolean onGameWindowEvent(Event event,
									  SystemsCommonData systemsCommonData,
									  List<UserInterfaceSystemEventsSubscriber> subscribers) {
		GameWindowEventType type = ((GameWindowEvent) event).getType();
		boolean result = false;
		if (type == GameWindowEventType.ITEM_PLACED) {
			ItemsTable itemsTable = (ItemsTable) event.getTarget();
			ItemDisplay selection = itemSelectionHandler.getSelection();
			if (event.getTarget() instanceof PlayerLayout) {
				applySelectionToSelectedWeapon(itemsTable, selection, subscribers);
			} else {
				if (selection == weaponChoice) {
					removeItem(selection);
				}
			}
			result = true;
		}
		return result;
	}

	void applySelectionToSelectedWeapon(ItemsTable itemsTableToRemoveFrom,
										ItemDisplay selection,
										List<UserInterfaceSystemEventsSubscriber> subscribers) {
		itemsTableToRemoveFrom.removeItem(selection);
		PlayerLayout.this.weaponChoice = selection;
		subscribers.forEach(sub -> sub.onSelectedWeaponChanged((Weapon) weaponChoice.getItem()));
		selection.setLocatedIn(PlayerLayout.class);
		placeWeapon();
	}

	@Override
	public void draw(final Batch batch, final float parentAlpha) {
		super.draw(batch, parentAlpha);
		ItemDisplay selection = itemSelectionHandler.getSelection();
		if (selection != null && selection.isWeapon() && weaponChoice == null) {
			Texture image = selection.getItem().getImage();
			batch.setColor(1f, 1f, 1f, 0.5f);
			float x = WEAPON_POSITION_PARENT_X - image.getWidth() / 2f;
			float y = WEAPON_POSITION_PARENT_Y - image.getHeight() / 2f;
			batch.draw(image, x, y);
			batch.setColor(1f, 1f, 1f, 1f);
		}
	}

	@Override
	protected void setParent(final Group parent) {
		super.setParent(parent);
		getParent().addActor(this.weaponChoice);
		placeWeapon();
	}

	private void placeWeapon() {
		Texture weaponImage = this.weaponChoice.getItem().getImage();
		float weaponX = WEAPON_POSITION_PARENT_X - weaponImage.getWidth() / 2f;
		float weaponY = WEAPON_POSITION_PARENT_Y - weaponImage.getHeight() / 2f;
		this.weaponChoice.setPosition(weaponX, weaponY);
	}


	@Override
	public void removeItem(final ItemDisplay item) {
		if (weaponChoice == item) {
			weaponChoice = null;
		}
	}
}
