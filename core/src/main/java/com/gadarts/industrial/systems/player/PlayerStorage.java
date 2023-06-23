package com.gadarts.industrial.systems.player;

import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.shared.assets.declarations.weapons.PlayerWeaponDeclaration;
import com.gadarts.industrial.shared.assets.declarations.weapons.PlayerWeaponsDeclarations;
import com.gadarts.industrial.shared.assets.declarations.weapons.WeaponDeclaration;
import com.gadarts.industrial.shared.model.ItemDeclaration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.IntStream;

@Getter
public class PlayerStorage {

	public static final int WIDTH = 8;
	public static final int HEIGHT = 8;
	public static final int SIZE = WIDTH * HEIGHT;

	private final Set<Item> items = new LinkedHashSet<>();
	private final int[] storageMap = new int[SIZE];
	@Getter(AccessLevel.NONE)
	private final int[] storageMapSketch = new int[SIZE];
	private final HashMap<WeaponDeclaration, PlayerWeaponDeclaration> playerWeaponsDeclarations = new HashMap<>();
	private final HashMap<ItemDeclaration, Integer> indices;
	private final Set<ItemDeclaration> pickupHistory = new HashSet<>();
	@Setter(AccessLevel.PACKAGE)
	private Weapon selectedWeapon;

	public PlayerStorage(GameAssetManager assetsManager) {
		PlayerWeaponsDeclarations declarations = (PlayerWeaponsDeclarations) assetsManager.getDeclaration(Assets.Declarations.PLAYER_WEAPONS);
		declarations.playerWeaponsDeclarations().forEach(d -> playerWeaponsDeclarations.put(d.declaration(), d));
		final int[] counter = {0};
		final HashMap<ItemDeclaration, Integer> indices = new HashMap<>();
		playerWeaponsDeclarations.forEach((declaration, playerWeaponDeclaration) -> indices.put(declaration, counter[0]++));
		this.indices = indices;
	}

	public void clear( ) {
		items.clear();
		pickupHistory.clear();
		IntStream.range(0, storageMap.length).forEach(i -> storageMap[i] = 0);
	}

	public boolean addItem(final Item item) {
		initializeStorageArray(storageMap, storageMapSketch);
		int index = 0;
		boolean result = false;
		while (index < SIZE) {
			if (tryToFillItemArea(index, item.getDeclaration())) {
				applyItemAddition(item, index);
				result = true;
				break;
			} else {
				index++;
			}
		}
		if (result) {
			pickupHistory.add(item.getDeclaration());
		}
		return result;
	}

	private void applyItemAddition(final Item item, final int index) {
		initializeStorageArray(storageMapSketch, storageMap);
		item.setRow(index / WIDTH);
		item.setCol(index % WIDTH);
		items.add(item);
	}

	private boolean tryToFillItemArea(int index, ItemDeclaration definition) {
		for (int row = 0; row < definition.getSymbolHeight(); row++) {
			if (index % (WIDTH) + definition.getWidth() < WIDTH) {
				if (!tryToFillRow(index, definition, row)) {
					initializeStorageArray(storageMap, storageMapSketch);
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	private boolean tryToFillRow(int index, ItemDeclaration definition, int row) {
		int leftMost = index % (WIDTH);
		int rightMost = leftMost + definition.getWidth();
		for (int col = leftMost; col < rightMost; col++) {
			if (!tryToFillCell(definition, row, col, leftMost)) {
				initializeStorageArray(storageMap, storageMapSketch);
				return false;
			}
		}
		return true;
	}

	private boolean tryToFillCell(ItemDeclaration definition, int row, int col, int leftMost) {
		if (definition.getMask()[row * (definition.getWidth()) + (col - leftMost)] == 1) {
			int currentCellInStorage = row * WIDTH + col;
			if (storageMap[currentCellInStorage] == 0) {
				storageMapSketch[currentCellInStorage] = indices.get(((PlayerWeaponDeclaration) definition).declaration());
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	private void initializeStorageArray(final int[] source, final int[] destination) {
		System.arraycopy(source, 0, destination, 0, source.length);
	}

	public void removeItem(final int itemId) {
		for (int row = 0; row < WIDTH; row++) {
			for (int col = 0; col < HEIGHT; col++) {
				int i = row * WIDTH + col;
				if (storageMap[i] == itemId) {
					storageMap[i] = 0;
				}
			}
		}
	}

	public boolean isFirstTimePickup(Item item) {
		return pickupHistory.contains(item);
	}
}
