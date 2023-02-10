package com.gadarts.industrial.systems.ui.window;

import com.gadarts.industrial.shared.assets.GameAssetManager;
import com.gadarts.industrial.systems.player.PlayerStorage;

public interface OnEvent {
	boolean execute(WindowEventParameters windowEventParameters, GameAssetManager assetsManager, PlayerStorage storage);
}
