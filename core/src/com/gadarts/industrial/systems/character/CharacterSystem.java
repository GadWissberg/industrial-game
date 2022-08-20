package com.gadarts.industrial.systems.character;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.animation.AnimationComponent;
import com.gadarts.industrial.components.character.*;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
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
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.enemy.EnemyAiStatus;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;
import com.gadarts.industrial.systems.projectiles.AttackSystemEventsSubscriber;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.utils.EntityBuilder;
import com.gadarts.industrial.utils.GameUtils;

import java.util.Map;

import static com.gadarts.industrial.components.ComponentsMapper.character;
import static com.gadarts.industrial.components.player.PlayerComponent.*;
import static com.gadarts.industrial.shared.model.characters.Direction.*;
import static com.gadarts.industrial.shared.model.characters.SpriteType.*;

public class CharacterSystem extends GameSystem<CharacterSystemEventsSubscriber> implements
		PlayerSystemEventsSubscriber,
		RenderSystemEventsSubscriber,
		EnemySystemEventsSubscriber,
		AttackSystemEventsSubscriber,
		CharacterSystemEventsSubscriber {
	private static final int ROTATION_INTERVAL = 125;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector3 auxVector3_2 = new Vector3();
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final long CHARACTER_PAIN_DURATION = 1000L;
	private static final long MIN_IDLE_ANIMATION_INTERVAL = 2000L;
	public static final long MAX_IDLE_ANIMATION_INTERVAL = 10000L;
	private final Map<SpriteType, CharacterCommandsDefinitions> onFrameChangedEvents = Map.of(
			RUN, CharacterCommandsDefinitions.RUN,
			PICKUP, CharacterCommandsDefinitions.PICKUP,
			ATTACK_PRIMARY, CharacterCommandsDefinitions.ATTACK_PRIMARY
	);
	private ParticleEffect bloodSplatterEffect;
	private ImmutableArray<Entity> characters;
	private ParticleEffect smallExpEffect;

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
		smallExpEffect = getAssetsManager().getParticleEffect(Assets.ParticleEffects.SMALL_EXP);
	}

	/**
	 * Applies a given command on the given character.
	 *
	 * @param command
	 * @param character
	 */
	@SuppressWarnings("JavaDoc")
	public void applyCommand(final CharacterCommand command,
							 final Entity character) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		characterComponent.setCommand(command);
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		Entity currentTurn = getSystemsCommonData().getTurnsQueue().first();
		CharacterCommand currentCommand = ComponentsMapper.character.get(currentTurn).getCommand();
		currentCommand.setStarted(false);
		if (characterComponent.getCharacterSpriteData().getSpriteType() != PAIN) {
			beginProcessingCommand(character, systemsCommonData, currentCommand);
		}
	}

	private void beginProcessingCommand(Entity character,
										SystemsCommonData systemsCommonData,
										CharacterCommand currentCommand) {
		currentCommand.setStarted(true);
		ComponentsMapper.character.get(character).getRotationData().setRotating(true);
		currentCommand.initialize(
				character,
				systemsCommonData,
				currentCommand.getAdditionalData(),
				getSubscribers());
		subscribers.forEach(CharacterSystemEventsSubscriber::onCommandInitialized);
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		CharacterCommand currentCommand = character.get(getSystemsCommonData().getTurnsQueue().first()).getCommand();
		if (currentCommand != null) {
			handleCurrentCommand(currentCommand);
		}
		updateCharacters();
	}

	private void updateCharacters( ) {
		long now = TimeUtils.millis();
		for (int i = 0; i < characters.size(); i++) {
			Entity character = characters.get(i);
			handleIdle(character, now);
			handlePain(character);
		}
	}

	private void handleIdle(Entity character, long now) {
		CharacterSpriteData spriteData = ComponentsMapper.character.get(character).getCharacterSpriteData();
		SpriteType spriteType = spriteData.getSpriteType();
		if (spriteType == IDLE && spriteData.getNextIdleAnimationPlay() < now) {
			long random = MathUtils.random(MAX_IDLE_ANIMATION_INTERVAL - MIN_IDLE_ANIMATION_INTERVAL);
			spriteData.setNextIdleAnimationPlay(now + MIN_IDLE_ANIMATION_INTERVAL + random);
			ComponentsMapper.animation.get(character).getAnimation().setFrameDuration(spriteType.getFrameDuration());
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

	private void applyDamageToCharacter(Entity attacked,
										int damage) {
		applyDamageToCharacter(attacked, damage, null);
	}

	private void applyDamageToCharacter(Entity attacked,
										int damage,
										ModelInstanceComponent bulletModelInstanceComponent) {
		CharacterComponent characterComponent = character.get(attacked);
		characterComponent.dealDamage(damage);
		handleDeath(attacked);
		if (ComponentsMapper.player.has(attacked)) {
			addSplatterEffect(bulletModelInstanceComponent);
		}
	}

	private void addSplatterEffect(final ModelInstanceComponent bulletModelInstanceComponent) {
		Vector3 position = positionBloodSplatter(bulletModelInstanceComponent);
		EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addParticleEffectComponent(bloodSplatterEffect, position)
				.finishAndAddToEngine();
	}

	private Vector3 positionBloodSplatter(ModelInstanceComponent bulletModelInstanceComponent) {
		Vector3 position;
		if (bulletModelInstanceComponent != null) {
			position = bulletModelInstanceComponent.getModelInstance().transform.getTranslation(auxVector3_1);
		} else {
			Decal decal = ComponentsMapper.characterDecal.get(getSystemsCommonData().getPlayer()).getDecal();
			MapGraph map = getSystemsCommonData().getMap();
			position = map.getNode(decal.getPosition()).getCenterPosition(auxVector3_2).add(0F, PLAYER_HEIGHT, 0F);
		}
		return position;
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
		charSpriteData.setSpriteType(LIGHT_DEATH);
		if (ComponentsMapper.animation.has(character)) {
			ComponentsMapper.animation.get(character).resetStateTime();
		}
		createExplosionOnCharacterDeath(character);
		getSystemsCommonData().getSoundPlayer().playSound(soundData.getDeathSound());
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterDies(character);
		}
	}

	private void createExplosionOnCharacterDeath(Entity character) {
		if (ComponentsMapper.enemy.has(character)) {
			Decal decal = ComponentsMapper.characterDecal.get(character).getDecal();
			MapGraphNode node = getSystemsCommonData().getMap().getNode(decal.getPosition());
			float height = ComponentsMapper.enemy.get(character).getEnemyDefinition().getHeight();
			Vector3 pos = node.getCenterPosition(auxVector3_1).add(0F, height / 4F, 0F);
			EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
					.addParticleEffectComponent(smallExpEffect, pos)
					.finishAndAddToEngine();
		}
	}

	@Override
	public void onMeleeAttackAppliedOnTarget(Entity character, Entity target, WeaponsDefinitions primaryAttack) {
		applyDamageToCharacter(target, primaryAttack.getDamage());
	}

	public void commandDone(final Entity character) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		characterComponent.getCharacterSpriteData().setSpriteType(SpriteType.IDLE);
		CharacterCommand lastCommand = characterComponent.getCommand();
		characterComponent.getRotationData().setRotating(false);
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterCommandDone(character, lastCommand);
		}
		CharacterCommand prevCommand = characterComponent.getCommand();
		if (prevCommand != null) {
			prevCommand.free();
		}
		characterComponent.setCommand(null);
		if (characterComponent.getTurnTimeLeft() > 0) {
			subscribers.forEach(subscriber -> subscriber.onCharacterStillHasTime(character));
		}
	}

	private void painDone(Entity character, CharacterSpriteData spriteData) {
		spriteData.setSpriteType(IDLE);
		Entity currentTurn = getSystemsCommonData().getTurnsQueue().first();
		CharacterCommand currentCommand = ComponentsMapper.character.get(currentTurn).getCommand();
		if (currentCommand != null && !currentCommand.isStarted()) {
			applyCommand(currentCommand, character);
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
			SystemsCommonData commonData = getSystemsCommonData();
			int primaryAttackHitFrameIndex = GameUtils.getPrimaryAttackHitFrameIndexForCharacter(character, commonData);
			if (onGoingAttack.isDone()) {
				onGoingAttack.setType(null);
				if (!characterComp.getPrimaryAttack().isMelee()) {
					animationComponent.setStateTime(0);
					animation.setPlayMode(Animation.PlayMode.REVERSED);
				}
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
			animation.setFrameDuration(spriteType.getFrameDuration());
			applyAnimationToReverse(animationComponent);
		}
	}

	private void applyAnimationToReverse(AnimationComponent animationComponent) {
		animationComponent.getAnimation().setPlayMode(Animation.PlayMode.REVERSED);
		animationComponent.resetStateTime();
		animationComponent.setDoingReverse(true);
	}

	private void handleCurrentCommand(final CharacterCommand currentCommand) {
		CharacterComponent characterComponent = character.get(currentCommand.getCharacter());
		SpriteType spriteType = characterComponent.getCharacterSpriteData().getSpriteType();
		if (spriteType == PICKUP || spriteType == ATTACK_PRIMARY) {
			handleModeWithNonLoopingAnimation(currentCommand.getCharacter());
		} else {
			handleRotation(currentCommand, characterComponent);
		}
	}

	private Direction calculateDirectionToDestination(CharacterCommand currentCommand) {
		Entity character = currentCommand.getCharacter();
		Vector3 characterPos = auxVector3_1.set(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
		Vector2 destPos = currentCommand.getDestinationNode().getCenterPosition(auxVector2_2);
		Vector2 directionToDest = destPos.sub(characterPos.x, characterPos.z).nor();
		return findDirection(directionToDest);
	}


	private void handleRotation(CharacterCommand currentCommand,
								CharacterComponent charComponent) {
		if (charComponent.getCharacterSpriteData().getSpriteType() == PAIN) return;

		CharacterRotationData rotationData = charComponent.getRotationData();
		if (rotationData.isRotating() && TimeUtils.timeSinceMillis(rotationData.getLastRotation()) > ROTATION_INTERVAL) {
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onCharacterRotated(currentCommand.getCharacter());
			}
			rotationData.setLastRotation(TimeUtils.millis());
			Direction directionToDest = calculateDirectionToDestination(currentCommand);
			rotate(charComponent, rotationData, directionToDest, currentCommand.getCharacter());
		}
	}

	private void rotate(CharacterComponent charComponent,
						CharacterRotationData rotationData,
						Direction directionToDest,
						Entity character) {
		if (charComponent.getCharacterSpriteData().getFacingDirection() != directionToDest) {
			CharacterSpriteData characterSpriteData = charComponent.getCharacterSpriteData();
			Vector2 currentDirVec = characterSpriteData.getFacingDirection().getDirection(auxVector2_1);
			float diff = directionToDest.getDirection(auxVector2_2).angleDeg() - currentDirVec.angleDeg();
			int side = auxVector2_3.set(1, 0).setAngleDeg(diff).angleDeg() > 180 ? -1 : 1;
			Vector3 charPos = ComponentsMapper.characterDecal.get(character).getDecal().getPosition();
			Entity floorEntity = getSystemsCommonData().getMap().getNode(charPos).getEntity();
			int fogOfWarSig = ComponentsMapper.floor.get(floorEntity).getFogOfWarSignature();
			boolean isNodeHidden = (fogOfWarSig & 16) == 16;
			Direction newDir = isNodeHidden ? directionToDest : findDirection(currentDirVec.rotateDeg(45f * side));
			characterSpriteData.setFacingDirection(newDir);
		} else {
			rotationDone(rotationData, charComponent.getCharacterSpriteData());
		}
	}

	@Override
	public void onCharacterNodeChanged(Entity entity, MapGraphNode oldNode, MapGraphNode newNode) {
		character.get(entity).getRotationData().setRotating(true);
	}

	private Direction calculateDirectionToTarget(final Entity character) {
		Vector3 pos = auxVector3_1.set(ComponentsMapper.characterDecal.get(character).getDecal().getPosition());
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		Entity target = characterComponent.getTarget();
		MapGraph map = getSystemsCommonData().getMap();
		MapGraphNode targetNode = map.getNode(ComponentsMapper.characterDecal.get(target).getDecal().getPosition());
		Vector2 destPos = targetNode.getCenterPosition(auxVector2_2);
		Vector2 directionToDest = destPos.sub(pos.x, pos.z).nor();
		return findDirection(directionToDest);
	}

	private void rotationDone(CharacterRotationData rotationData,
							  CharacterSpriteData charSpriteData) {
		rotationData.setRotating(false);
		CharacterCommand command = character.get(getSystemsCommonData().getTurnsQueue().first()).getCommand();
		charSpriteData.setSpriteType(command.getDefinition().getSpriteType());
	}

	@Override
	public void dispose( ) {

	}

	@Override
	public void onPlayerPathCreated(MapGraphNode destination) {

	}

	@Override
	public void onEnemyAppliedCommand(CharacterCommand auxCommand, Entity enemy) {
		onCharacterAppliedCommand(auxCommand, enemy);
	}

	@Override
	public void onPlayerAppliedCommand(CharacterCommand command) {
		onCharacterAppliedCommand(command, getSystemsCommonData().getPlayer());
	}

	private void onCharacterAppliedCommand(CharacterCommand command, Entity character) {
		applyCommand(command, character);
	}

	private void handlePickup(Entity character, TextureAtlas.AtlasRegion newFrame) {
		if (newFrame.index == 1 && ComponentsMapper.animation.get(character).isDoingReverse()) {
			Entity currentTurn = getSystemsCommonData().getTurnsQueue().first();
			CharacterCommand currentCommand = ComponentsMapper.character.get(currentTurn).getCommand();
			Entity itemPickedUp = (Entity) currentCommand.getAdditionalData();
			for (CharacterSystemEventsSubscriber subscriber : subscribers) {
				subscriber.onItemPickedUp(itemPickedUp);
			}
		}
	}

	@Override
	public void onProjectileCollisionWithAnotherEntity(final Entity bullet, final Entity collidable) {
		if (character.has(collidable)) {
			ModelInstanceComponent modelInstanceComponent = ComponentsMapper.modelInstance.get(bullet);
			applyDamageToCharacter(collidable, ComponentsMapper.bullet.get(bullet).getDamage(), modelInstanceComponent);
		}
	}

	@Override
	public void onFrameChanged(final Entity character, final float deltaTime, final TextureAtlas.AtlasRegion newFrame) {
		CharacterSpriteData characterSpriteData = ComponentsMapper.character.get(character).getCharacterSpriteData();
		CharacterCommandsDefinitions commandDef = onFrameChangedEvents.get(characterSpriteData.getSpriteType());
		if (commandDef != null) {
			SystemsCommonData commonData = getSystemsCommonData();
			CharacterComponent characterComp = ComponentsMapper.character.get(character);
			CharacterCommand currentCommand = characterComp.getCommand();
			Entity turn = commonData.getTurnsQueue().first();
			if (turn == character) {
				if (currentCommand.reactToFrameChange(commonData, character, newFrame, subscribers)) {
					commandDone(character);
				}
				if (characterComp.getTurnTimeLeft() <= 0) {
					subscribers.forEach(subscriber -> subscriber.onCharacterFinishedTurn(character));
				}
			}
		}
	}

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus) {
		if (prevAiStatus == EnemyAiStatus.IDLE) {
			commandDone(getSystemsCommonData().getPlayer());
		}
	}
}
