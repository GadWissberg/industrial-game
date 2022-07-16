package com.gadarts.industrial.systems.character;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.animation.AnimationComponent;
import com.gadarts.industrial.components.cd.CharacterDecalComponent;
import com.gadarts.industrial.components.character.*;
import com.gadarts.industrial.components.player.PlayerComponent;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.shared.model.characters.enemies.WeaponsDefinitions;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.actions.CharacterCommandImplementation;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;
import com.gadarts.industrial.systems.projectiles.BulletSystemEventsSubscriber;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.utils.EntityBuilder;

import java.util.Map;

import static com.gadarts.industrial.shared.model.characters.SpriteType.*;

public class CharacterSystem extends GameSystem<CharacterSystemEventsSubscriber> implements
		PlayerSystemEventsSubscriber,
		RenderSystemEventsSubscriber,
		EnemySystemEventsSubscriber,
		BulletSystemEventsSubscriber {
	private static final int ROT_INTERVAL = 125;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final CharacterCommandContext auxCommand = new CharacterCommandContext();
	private static final long CHARACTER_PAIN_DURATION = 1000;
	private final static Vector3 auxVector3_3 = new Vector3();
	private final static Vector3 auxVector3_4 = new Vector3();
	private final Map<SpriteType, CharacterCommandsDefinitions> onFrameChangedEvents = Map.of(
			RUN, CharacterCommandsDefinitions.RUN,
			PICKUP, CharacterCommandsDefinitions.PICKUP,
			ATTACK_PRIMARY, CharacterCommandsDefinitions.ATTACK_PRIMARY
	);
	private ParticleEffect bloodSplatterEffect;
	private ImmutableArray<Entity> characters;

	public CharacterSystem(SystemsCommonData systemsCommonData,
						   GameAssetsManager assetsManager,
						   GameLifeCycleHandler lifeCycleHandler) {
		super(systemsCommonData, assetsManager, lifeCycleHandler);
	}

	@Override
	public void addedToEngine(Engine engine) {
		super.addedToEngine(engine);
		characters = getEngine().getEntitiesFor(Family.all(CharacterComponent.class).get());
	}

	@Override
	public Class<CharacterSystemEventsSubscriber> getEventsSubscriberClass( ) {
		return CharacterSystemEventsSubscriber.class;
	}

	@Override
	public void initializeData( ) {
		bloodSplatterEffect = getAssetsManager().getParticleEffect(Assets.ParticleEffects.BLOOD_SPLATTER);
	}

	/**
	 * Applies a given command on the given character.
	 *
	 * @param command
	 * @param character
	 */
	@SuppressWarnings("JavaDoc")
	public void applyCommand(final CharacterCommandContext command,
							 final Entity character) {
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		systemsCommonData.setCurrentCommand(command);
		CharacterCommandContext currentCommand = systemsCommonData.getCurrentCommand();
		currentCommand.setStarted(false);
		if (ComponentsMapper.character.get(character).getCharacterSpriteData().getSpriteType() != PAIN) {
			beginProcessingCommand(character, systemsCommonData, currentCommand);
		}
	}

	private void beginProcessingCommand(Entity character,
										SystemsCommonData systemsCommonData,
										CharacterCommandContext currentCommand) {
		currentCommand.setStarted(true);
		SpriteType spriteType = currentCommand.getDefinition().getSpriteType();
		ComponentsMapper.character.get(character).getCharacterSpriteData().setSpriteType(spriteType);
		currentCommand.getDefinition().getCharacterCommandImplementation().initialize(
				character,
				systemsCommonData,
				currentCommand.getAdditionalData(),
				getSubscribers());
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		CharacterCommandContext currentCommand = getSystemsCommonData().getCurrentCommand();
		if (currentCommand != null) {
			handleCurrentCommand(currentCommand);
		}
		for (int i = 0; i < characters.size(); i++) {
			handlePain(characters.get(i));
		}
	}

	private void handlePain(final Entity character) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		long lastDamage = characterComponent.getSkills().getHealthData().getLastDamage();
		CharacterSpriteData spriteData = characterComponent.getCharacterSpriteData();
		if (spriteData.getSpriteType() == PAIN && TimeUtils.timeSinceMillis(lastDamage) > CHARACTER_PAIN_DURATION) {
			painDone(character, spriteData);
		}
	}

	private void applyDamageToCharacter(final Entity attacked, final int damage) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(attacked);
		characterComponent.dealDamage(damage);
		handleDeath(attacked);
		Vector3 pos = ComponentsMapper.characterDecal.get(attacked).getNodePosition(auxVector3_1);
		float height = calculateSplatterEffectHeight(attacked, pos);
		addSplatterEffect(auxVector3_1.set(pos.x + 0.5F, height, pos.z + 0.5F));
	}

	private void addSplatterEffect(final Vector3 pos) {
		EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addParticleEffectComponent((PooledEngine) getEngine(), bloodSplatterEffect, pos)
				.finishAndAddToEngine();
	}

	private float calculateSplatterEffectHeight(final Entity attacked, final Vector3 pos) {
		float height = pos.y;
		if (ComponentsMapper.enemy.has(attacked)) {
			height += ComponentsMapper.enemy.get(attacked).getEnemyDefinition().getHeight() / 2F;
		} else if (ComponentsMapper.player.has(attacked)) {
			height += PlayerComponent.PLAYER_HEIGHT;
		}
		return height;
	}

	private void handleDeath(final Entity character) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		CharacterHealthData healthData = characterComponent.getSkills().getHealthData();
		CharacterSoundData soundData = characterComponent.getSoundData();
		if (healthData.getHp() <= 0) {
			characterDies(character, characterComponent, soundData);
		} else {
			characterInPain(character, characterComponent, soundData);
		}
	}

	private void characterInPain(Entity character,
								 CharacterComponent characterComponent,
								 CharacterSoundData soundData) {
		getSystemsCommonData().getSoundPlayer().playSound(soundData.getPainSound());
		characterComponent.getCharacterSpriteData().setSpriteType(PAIN);
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterGotDamage(character);
		}
	}

	private void characterDies(Entity character, CharacterComponent characterComponent, CharacterSoundData soundData) {
		CharacterSpriteData charSpriteData = characterComponent.getCharacterSpriteData();
		charSpriteData.setSpriteType(charSpriteData.isSingleDeathAnimation() ? LIGHT_DEATH_1 : randomLightDeath());
		if (ComponentsMapper.animation.has(character)) {
			ComponentsMapper.animation.get(character).resetStateTime();
		}
		getSystemsCommonData().getSoundPlayer().playSound(soundData.getDeathSound());
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterDies(character);
		}
	}

	private void painDone(Entity character, CharacterSpriteData spriteData) {
		spriteData.setSpriteType(IDLE);
		CharacterCommandContext currentCommand = getSystemsCommonData().getCurrentCommand();
		if (currentCommand != null && !currentCommand.isStarted()) {
			applyCommand(currentCommand, character);
		}
	}

	public void commandDone(final Entity character) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		characterComponent.getCharacterSpriteData().setSpriteType(SpriteType.IDLE);
		CharacterCommandContext lastCommand = getSystemsCommonData().getCurrentCommand();
		getSystemsCommonData().setCurrentCommand(null);
		characterComponent.getRotationData().setRotating(false);
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterCommandDone(character, lastCommand);
		}
	}

	private void handleModeWithNonLoopingAnimation(final Entity character) {
		AnimationComponent animationComponent = ComponentsMapper.animation.get(character);
		Animation<TextureAtlas.AtlasRegion> animation = animationComponent.getAnimation();
		if (animation.isAnimationFinished(animationComponent.getStateTime())) {
			animationEnded(character, animationComponent, animation);
		}
	}

	private void animationEnded(Entity character,
								AnimationComponent animationComponent,
								Animation<TextureAtlas.AtlasRegion> animation) {
		CharacterComponent characterComp = ComponentsMapper.character.get(character);
		SpriteType spriteType = characterComp.getCharacterSpriteData().getSpriteType();
		OnGoingAttack onGoingAttack = characterComp.getOnGoingAttack();
		if (spriteType.isAddReverse()) {
			handleAnimationReverse(character, animationComponent, animation, spriteType);
		} else if (onGoingAttack.getType() != null) {
			int primaryAttackHitFrameIndex = characterComp.getCharacterSpriteData().getPrimaryAttackHitFrameIndex();
			if (onGoingAttack.isDone()) {
				onGoingAttack.setType(null);
				animationComponent.setStateTime((primaryAttackHitFrameIndex - 1) * animation.getFrameDuration());
				animation.setPlayMode(Animation.PlayMode.REVERSED);
			} else {
				animationComponent.setStateTime((primaryAttackHitFrameIndex) * animation.getFrameDuration());
			}
		} else {
			animation.setPlayMode(Animation.PlayMode.NORMAL);
			commandDone(character);
		}
	}

	private void handleAnimationReverse(Entity character,
										AnimationComponent animationComponent,
										Animation<TextureAtlas.AtlasRegion> animation,
										SpriteType spriteType) {
		if (animationComponent.isDoingReverse()) {
			commandDone(character);
			animation.setPlayMode(Animation.PlayMode.NORMAL);
			animationComponent.setDoingReverse(false);
		} else {
			animation.setFrameDuration(spriteType.getAnimationDuration());
			applyAnimationToReverse(animationComponent);
		}
	}

	private void applyAnimationToReverse(AnimationComponent animationComponent) {
		animationComponent.getAnimation().setPlayMode(Animation.PlayMode.REVERSED);
		animationComponent.resetStateTime();
		animationComponent.setDoingReverse(true);
	}

	private void handleCurrentCommand(final CharacterCommandContext currentCommand) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(currentCommand.getCharacter());
		SpriteType spriteType = characterComponent.getCharacterSpriteData().getSpriteType();
		if (spriteType == PICKUP || spriteType == ATTACK_PRIMARY) {
			handleModeWithNonLoopingAnimation(currentCommand.getCharacter());
		} else {
			handleRotation(currentCommand, characterComponent);
		}
	}

	private Direction calculateDirectionToDestination(CharacterCommandContext currentCommand) {
		Entity character = currentCommand.getCharacter();
		Vector3 characterPos = auxVector3_1.set(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
		Vector2 destPos = currentCommand.getDestinationNode().getCenterPosition(auxVector2_2);
		Vector2 directionToDest = destPos.sub(characterPos.x, characterPos.z).nor();
		return Direction.findDirection(directionToDest);
	}


	private void handleRotation(CharacterCommandContext currentCommand,
								CharacterComponent charComponent) {
		if (charComponent.getCharacterSpriteData().getSpriteType() == PAIN) return;

		CharacterRotationData rotationData = charComponent.getRotationData();
		if (rotationData.isRotating() && TimeUtils.timeSinceMillis(rotationData.getLastRotation()) > ROT_INTERVAL) {
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onCharacterRotated(currentCommand.getCharacter());
			}
			rotationData.setLastRotation(TimeUtils.millis());
			Direction directionToDest = calculateDirectionToDestination(currentCommand);
			rotate(charComponent, rotationData, directionToDest);
		}
	}

	private void rotate(CharacterComponent charComponent,
						CharacterRotationData rotationData,
						Direction directionToDest) {
		if (charComponent.getCharacterSpriteData().getFacingDirection() != directionToDest) {
			CharacterSpriteData characterSpriteData = charComponent.getCharacterSpriteData();
			Vector2 currentDirVector = characterSpriteData.getFacingDirection().getDirection(auxVector2_1);
			float diff = directionToDest.getDirection(auxVector2_2).angleDeg() - currentDirVector.angleDeg();
			int side = auxVector2_3.set(1, 0).setAngleDeg(diff).angleDeg() > 180 ? -1 : 1;
			characterSpriteData.setFacingDirection(Direction.findDirection(currentDirVector.rotateDeg(45f * side)));
		} else {
			rotationDone(rotationData, charComponent.getCharacterSpriteData());
		}
	}

	private Direction calculateDirectionToTarget(final Entity character) {
		Vector3 pos = auxVector3_1.set(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		Entity target = characterComponent.getTarget();
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode targetNode = map.getNode(ComponentsMapper.characterDecal.get(target).getDecal().getPosition());
		Vector2 destPos = targetNode.getCenterPosition(auxVector2_2);
		Vector2 directionToDest = destPos.sub(pos.x, pos.z).nor();
		return Direction.findDirection(directionToDest);
	}

	private void rotationDone(CharacterRotationData rotationData,
							  CharacterSpriteData charSpriteData) {
		rotationData.setRotating(false);
		charSpriteData.setSpriteType(RUN);
	}

	private SpriteType applyPrimaryAttackForCharacter(CharacterCommandContext currentCommand) {
		SpriteType spriteType;
		spriteType = ATTACK_PRIMARY;
		CharacterComponent charComp = ComponentsMapper.character.get(currentCommand.getCharacter());
		WeaponsDefinitions primary = charComp.getPrimaryAttack();
		int bulletsToShoot = MathUtils.random(primary.getMinNumberOfBullets(), primary.getMaxNumberOfBullets());
		charComp.getOnGoingAttack().initialize(CharacterComponent.AttackType.PRIMARY, bulletsToShoot);
		return spriteType;
	}

	@Override
	public void dispose( ) {

	}

	@Override
	public void onPlayerPathCreated(MapGraphNode destination) {

	}

	@Override
	public void onEnemyAppliedCommand(CharacterCommandContext auxCommand, Entity enemy) {
		onCharacterAppliedCommand(auxCommand, enemy);
	}

	@Override
	public void onPlayerAppliedCommand(CharacterCommandContext command) {
		onCharacterAppliedCommand(command, getSystemsCommonData().getPlayer());
	}

	@Override
	public void onHitScanCollisionWithAnotherEntity(WeaponsDefinitions definition, Entity collidable) {
		if (ComponentsMapper.character.has(collidable)) {
			applyDamageToCharacter(collidable, definition.getDamage());
		}
	}

	private void onCharacterAppliedCommand(CharacterCommandContext command, Entity character) {
		auxCommand.init(command);
		applyCommand(auxCommand, character);
	}

	private void handlePickup(Entity character, TextureAtlas.AtlasRegion newFrame) {
		if (newFrame.index == 1 && ComponentsMapper.animation.get(character).isDoingReverse()) {
			CharacterCommandContext currentCommand = getSystemsCommonData().getCurrentCommand();
			Entity itemPickedUp = (Entity) currentCommand.getAdditionalData();
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onItemPickedUp(itemPickedUp);
			}
		}
	}

	@Override
	public void onProjectileCollisionWithAnotherEntity(final Entity bullet, final Entity collidable) {
		if (ComponentsMapper.character.has(collidable)) {
			applyDamageToCharacter(collidable, ComponentsMapper.bullet.get(bullet).getDamage());
		}
	}

	@Override
	public void onFrameChanged(final Entity character, final float deltaTime, final TextureAtlas.AtlasRegion newFrame) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		CharacterSpriteData characterSpriteData = characterComponent.getCharacterSpriteData();
		CharacterCommandsDefinitions commandDef = onFrameChangedEvents.get(characterSpriteData.getSpriteType());
		if (commandDef != null) {
			CharacterCommandImplementation impl = commandDef.getCharacterCommandImplementation();
			SystemsCommonData commonData = getSystemsCommonData();
			if (impl.reactToFrameChange(commonData, character, newFrame, subscribers, commonData.getCurrentCommand())) {
				commandDone(character);
			}
		}
	}

	private void engagePrimaryAttack(Entity character,
									 TextureAtlas.AtlasRegion newFrame) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		OnGoingAttack onGoingAttack = characterComponent.getOnGoingAttack();
		if (onGoingAttack.isDone()) return;

		if (newFrame.index == characterComponent.getCharacterSpriteData().getPrimaryAttackHitFrameIndex()) {
			CharacterDecalComponent charDecalComp = ComponentsMapper.characterDecal.get(character);
			MapGraphNode positionNode = getSystemsCommonData().getMap().getNode(charDecalComp.getDecal().getPosition());
			Vector3 positionNodeCenterPosition = positionNode.getCenterPosition(auxVector3_4);
			Vector3 direction = calculateDirectionToTarget(characterComponent, positionNodeCenterPosition);
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onCharacterEngagesPrimaryAttack(character, direction, positionNodeCenterPosition);
			}
			onGoingAttack.bulletShot();
		}
	}

	private Vector3 calculateDirectionToTarget(CharacterComponent characterComp, Vector3 positionNodeCenterPosition) {
		CharacterDecalComponent targetDecalComp = ComponentsMapper.characterDecal.get(characterComp.getTarget());
		MapGraphNode targetNode = getSystemsCommonData().getMap().getNode(targetDecalComp.getDecal().getPosition());
		Vector3 targetNodeCenterPosition = targetNode.getCenterPosition(auxVector3_3);
		targetNodeCenterPosition.y += 0.5f;
		return targetNodeCenterPosition.sub(positionNodeCenterPosition);
	}

	private void applyRunning(final Entity character,
							  final TextureAtlas.AtlasRegion newFrame) {
	}


}
