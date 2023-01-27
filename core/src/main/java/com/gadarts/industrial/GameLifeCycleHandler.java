package com.gadarts.industrial;

public interface GameLifeCycleHandler {
	boolean isInGame( );

	void raiseFlagToRestartGame( );
}
