package com.gadarts.industrial.screens;

import com.gadarts.industrial.GameStates;

public interface GameLifeCycleManager {
	void startNewGame(String mapName);

	void pauseGame( );

	GameStates getGameState( );

	void resumeGame( );

	GameStates getPrevGameState( );
}
