package com.gadarts.industrial.components.enemy;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.components.GameComponent;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.declarations.enemies.EnemyDeclaration;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.systems.enemy.ai.EnemyAiStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnemyComponent implements GameComponent {
	public static final int ROAM_SOUND_INTERVAL_MINIMUM = 5000;
	public static final int ROAM_SOUND_INTERVAL_MAXIMUM = 10000;

	private MapGraphNode targetLastVisibleNode;
	private int engineEnergy;
	private long nextRoamSound;
	private EnemyDeclaration enemyDeclaration;
	private EnemyAiStatus aiStatus;
	private Direction initialSearchingLookingDirection;
	private int wonderingCounter;
	private long wonderingPrevTurnId;
	private EnemyTimeStamps timeStamps = new EnemyTimeStamps();
	private Animation<TextureAtlas.AtlasRegion> bulletAnimation;
	private long iconDisplayInFlowerTimeStamp;

	public void init(final EnemyDeclaration enemyDeclaration,
					 final Animation<TextureAtlas.AtlasRegion> bulletAnimation) {
		calculateNextRoamSound();
		this.enemyDeclaration = enemyDeclaration;
		this.bulletAnimation = bulletAnimation;
		timeStamps.reset();
		targetLastVisibleNode = null;
		aiStatus = EnemyAiStatus.IDLE;
		iconDisplayInFlowerTimeStamp = 0;
		engineEnergy = enemyDeclaration.engine();
	}

	@Override
	public void reset( ) {

	}

	public void calculateNextRoamSound( ) {
		nextRoamSound = TimeUtils.millis() + MathUtils.random(ROAM_SOUND_INTERVAL_MINIMUM, ROAM_SOUND_INTERVAL_MAXIMUM);
	}

}
