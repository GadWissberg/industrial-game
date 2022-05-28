package com.gadarts.industrial.systems.player;

import com.gadarts.industrial.shared.model.pickups.ItemDefinition;
import com.gadarts.industrial.components.player.Item;
import com.gadarts.industrial.components.player.Weapon;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;
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

	@Setter(AccessLevel.PACKAGE)
	private Weapon selectedWeapon;


	public void clear() {
		items.clear();
		IntStream.range(0, storageMap.length).forEach(i -> storageMap[i] = 0);
	}

	public boolean addItem(final Item item) {
		initializeStorageArray(storageMap, storageMapSketch);
		int index = 0;
		boolean result = false;
		while (index < SIZE) {
			if (tryToFillItemArea(index, item.getDefinition())) {
				applyItemAddition(item, index);
				result = true;
				break;
			} else {
				index++;
			}
		}
		return result;
	}

	private void applyItemAddition(final Item item, final int index) {
		initializeStorageArray(storageMapSketch, storageMap);
		item.setRow(index / WIDTH);
		item.setCol(index % WIDTH);
		items.add(item);
	}

	private boolean tryToFillItemArea(final int index, final ItemDefinition definition) {
		for (int row = 0; row < definition.getHeight(); row++) {
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

	private boolean tryToFillRow(final int index, final ItemDefinition definition, final int row) {
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

	private boolean tryToFillCell(final ItemDefinition definition, final int row, final int col, final int leftMost) {
		if (definition.getMask()[row * (definition.getWidth()) + (col - leftMost)] == 1) {
			int currentCellInStorage = row * WIDTH + col;
			if (storageMap[currentCellInStorage] == 0) {
				storageMapSketch[currentCellInStorage] = definition.getId();
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	private void initializeStorageArray(final int[] source, final int[] destination) {
		for (int i = 0; i < source.length; i++) {
			destination[i] = source[i];
		}
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
}
