package com.gadarts.industrial.systems.ui.storage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.SoundPlayer;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.systems.ui.UserInterfaceSystemEventsSubscriber;
import com.gadarts.industrial.systems.ui.window.GameWindow;
import com.gadarts.industrial.systems.ui.window.GameWindowEvent;
import com.gadarts.industrial.systems.ui.window.WindowEventParameters;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.List;
import java.util.stream.IntStream;

import static com.gadarts.industrial.systems.ui.storage.StorageGrid.GRID_CELL_SIZE;
import static com.gadarts.industrial.systems.ui.storage.StorageGrid.GRID_SIZE;
import static com.gadarts.industrial.systems.ui.storage.StorageWindowOnEvents.*;

/**
 * The player's storage management GUI.
 */
public class StorageWindow extends GameWindow {

	/**
	 * Window identifier.
	 */
	public static final String NAME = "storage";
	private static final int PLAYER_LAYOUT_PADDING = 40;
	private static final WindowEventParameters auxWindowEventParameters = new WindowEventParameters();
	@Getter
	private final ItemSelectionHandler selectedItem = new ItemSelectionHandler();
	private final Texture gridTexture;
	private final Texture gridCellTexture;
	@Getter(AccessLevel.PACKAGE)
	private StorageGrid storageGrid;
	@Getter(AccessLevel.PACKAGE)
	private PlayerLayout playerLayout;

	public StorageWindow(WindowStyle windowStyle,
						 GameAssetsManager assetsManager,
						 SoundPlayer soundPlayer,
						 SystemsCommonData systemsCommonData,
						 List<UserInterfaceSystemEventsSubscriber> subscribers) {
		super(StorageWindow.NAME, windowStyle, assetsManager);
		this.gridTexture = createGridTexture();
		this.gridCellTexture = createGridCellTexture();
		addPlayerLayout(assetsManager, systemsCommonData, subscribers);
		setTouchable(Touchable.enabled);
		addStorageGrid(systemsCommonData);
		initializeListeners(soundPlayer, systemsCommonData, subscribers);
	}

	/**
	 * Initializes the storage grid.
	 */
	public void initialize() {
		storageGrid.initialize();
	}

	@Override
	public void draw(final Batch batch, final float parentAlpha) {
		super.draw(batch, parentAlpha);
		if (selectedItem.getSelection() != null) {
			drawSelectedItemOnCursor(batch);
		}
	}

	void applySelectedItem(final ItemDisplay itemDisplay) {
		if (itemDisplay != null) {
			itemDisplay.clearActions();
		}
		selectedItem.setSelection(itemDisplay);
		if (itemDisplay != null) {
			itemDisplay.applyFlickerAction();
		}
		closeButton.setDisabled(true);
	}

	void clearSelectedItem() {
		if (selectedItem.getSelection() != null) {
			closeButton.setDisabled(false);
			selectedItem.setSelection(null);
		}
	}

	public void onItemAddedToStorage(Item item) {
		storageGrid.onItemAddedToStorage(item);
	}

	boolean onRightClick() {
		boolean result = false;
		if (selectedItem.getSelection() != null) {
			clearSelectedItem();
			result = true;
		}
		return result;
	}

	private void initializeListeners(SoundPlayer soundPlayer,
									 SystemsCommonData commonData,
									 List<UserInterfaceSystemEventsSubscriber> subscribers) {
		addListener(event -> {
			boolean result = false;
			if (event instanceof GameWindowEvent windowEvent) {
				initializeWindowEventParameters(soundPlayer, commonData, subscribers, windowEvent);
				result = execute(auxWindowEventParameters);
			}
			return result;
		});
		addListener(new InputListener() {
			@Override
			public void enter(final InputEvent event, final float x, final float y, final int pointer, final Actor fromActor) {
				super.enter(event, x, y, pointer, fromActor);
				Actor target = event.getTarget();
				if (target instanceof ItemDisplay) {
					if (selectedItem.getSelection() == null) {
						ItemDisplay item = (ItemDisplay) target;
						item.clearActions();
						item.applyFlickerAction();
					}
				}
			}

			@Override
			public void exit(final InputEvent event, final float x, final float y, final int pointer, final Actor toActor) {
				super.exit(event, x, y, pointer, toActor);
				Actor target = event.getTarget();
				if (target instanceof ItemDisplay) {
					if (selectedItem.getSelection() == null) {
						ItemDisplay item = (ItemDisplay) target;
						item.clearActions();
						item.addAction(Actions.color(Color.WHITE, ItemDisplay.FLICKER_DURATION, Interpolation.smooth2));
					}
				}
			}
		});
		addListener(new ClickListener() {
			@Override
			public boolean touchDown(final InputEvent event,
									 final float x,
									 final float y,
									 final int pointer,
									 final int button) {
				boolean result = super.touchDown(event, x, y, pointer, button);
				if (button == Input.Buttons.RIGHT) {
					result = onRightClick();
				}
				return result;
			}

		});
	}

	private void initializeWindowEventParameters(SoundPlayer soundPlayer,
												 SystemsCommonData commonData,
												 List<UserInterfaceSystemEventsSubscriber> subscribers,
												 GameWindowEvent windowEvent) {
		auxWindowEventParameters.setWindowEvent(windowEvent);
		auxWindowEventParameters.setSoundPlayer(soundPlayer);
		auxWindowEventParameters.setSelectedItem(selectedItem);
		auxWindowEventParameters.setTarget(StorageWindow.this);
		auxWindowEventParameters.setSystemsCommonData(commonData);
		auxWindowEventParameters.setSubscribers(subscribers);
	}

	private void addPlayerLayout(GameAssetsManager assetsManager,
								 SystemsCommonData systemsCommonData,
								 List<UserInterfaceSystemEventsSubscriber> subscribers) {
		Texture texture = assetsManager.getTexture(Assets.UiTextures.PLAYER_LAYOUT);
		Weapon selectedWeapon = systemsCommonData.getStorage().getSelectedWeapon();
		playerLayout = new PlayerLayout(texture, selectedWeapon, selectedItem, systemsCommonData, subscribers);
		add(playerLayout).pad(PLAYER_LAYOUT_PADDING);
	}

	private Texture createGridTexture() {
		Pixmap gridPixmap = new Pixmap(GRID_SIZE, GRID_SIZE, Pixmap.Format.RGBA8888);
		paintGrid(gridPixmap);
		Texture gridTexture = new Texture(gridPixmap);
		gridPixmap.dispose();
		return gridTexture;
	}

	private void paintGrid(final Pixmap gridPixmap) {
		gridPixmap.setColor(Color.BLACK);
		gridPixmap.drawRectangle(0, 0, GRID_SIZE, GRID_SIZE);
		IntStream.range(0, GRID_SIZE / GRID_CELL_SIZE).forEach(i -> {
			int division = i * GRID_CELL_SIZE;
			gridPixmap.drawLine(division, 0, division, GRID_SIZE);
			gridPixmap.drawLine(0, division, GRID_SIZE, division);
		});
	}

	private Texture createGridCellTexture() {
		int size = GRID_CELL_SIZE;
		Pixmap gridPixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
		gridPixmap.setColor(Color.WHITE);
		gridPixmap.fill();
		Texture gridTexture = new Texture(gridPixmap);
		gridPixmap.dispose();
		return gridTexture;
	}


	private void drawSelectedItemOnCursor(final Batch batch) {
		Texture image = selectedItem.getSelection().getItem().getImage();
		float x = Gdx.input.getX(0) - image.getWidth() / 2f;
		float y = getStage().getHeight() - Gdx.input.getY(0) - image.getHeight() / 2f;
		batch.setColor(1f, 1f, 1f, 0.5f);
		batch.draw(image, x, y);
		batch.setColor(1f, 1f, 1f, 1f);
	}

	private void addStorageGrid(SystemsCommonData systemsCommonData) {
		storageGrid = new StorageGrid(gridTexture, systemsCommonData, gridCellTexture, selectedItem);
		add(storageGrid);
	}
}

