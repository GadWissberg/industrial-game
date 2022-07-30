package com.gadarts.industrial.systems.character;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import com.gadarts.industrial.GameLifeCycleHandler;
import com.gadarts.industrial.components.ComponentsMapper;
import com.gadarts.industrial.components.animation.AnimationComponent;
import com.gadarts.industrial.components.character.*;
import com.gadarts.industrial.components.mi.ModelInstanceComponent;
import com.gadarts.industrial.components.player.Weapon;
import com.gadarts.industrial.map.MapGraph;
import com.gadarts.industrial.map.MapGraphNode;
import com.gadarts.industrial.shared.assets.Assets;
import com.gadarts.industrial.shared.assets.GameAssetsManager;
import com.gadarts.industrial.shared.model.characters.Direction;
import com.gadarts.industrial.shared.model.characters.SpriteType;
import com.gadarts.industrial.shared.model.pickups.PlayerWeaponsDefinitions;
import com.gadarts.industrial.systems.GameSystem;
import com.gadarts.industrial.systems.SystemsCommonData;
import com.gadarts.industrial.systems.character.commands.CharacterCommand;
import com.gadarts.industrial.systems.character.commands.CharacterCommandsDefinitions;
import com.gadarts.industrial.systems.enemy.EnemyAiStatus;
import com.gadarts.industrial.systems.enemy.EnemySystemEventsSubscriber;
import com.gadarts.industrial.systems.player.PlayerSystemEventsSubscriber;
import com.gadarts.industrial.systems.projectiles.BulletSystemEventsSubscriber;
import com.gadarts.industrial.systems.render.RenderSystemEventsSubscriber;
import com.gadarts.industrial.utils.EntityBuilder;
import com.gadarts.industrial.utils.GameUtils;

import java.util.Map;

import static com.gadarts.industrial.components.ComponentsMapper.character;
import static com.gadarts.industrial.shared.model.characters.SpriteType.*;

public class CharacterSystem extends GameSystem<CharacterSystemEventsSubscriber> implements
		PlayerSystemEventsSubscriber,
		RenderSystemEventsSubscriber,
		EnemySystemEventsSubscriber,
		BulletSystemEventsSubscriber {
	private static final int ROTATION_INTERVAL = 125;
	private static final Vector3 auxVector3_1 = new Vector3();
	private static final Vector2 auxVector2_1 = new Vector2();
	private static final Vector2 auxVector2_2 = new Vector2();
	private static final Vector2 auxVector2_3 = new Vector2();
	private static final long CHARACTER_PAIN_DURATION = 1000;
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
		SystemsCommonData systemsCommonData = getSystemsCommonData();
		systemsCommonData.setCurrentCommandContext(command);
		CharacterCommand currentCommand = systemsCommonData.getCurrentCommandContext();
		currentCommand.setStarted(false);
		if (ComponentsMapper.character.get(character).getCharacterSpriteData().getSpriteType() != PAIN) {
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
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		CharacterCommand currentCommand = getSystemsCommonData().getCurrentCommandContext();
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

	private void applyDamageToCharacter(Entity attacked,
										int damage,
										ModelInstanceComponent bulletModelInstanceComponent) {
		CharacterComponent characterComponent = character.get(attacked);
		characterComponent.dealDamage(damage);
		handleDeath(attacked);
		if (ComponentsMapper.player.has(attacked)) {
			addSplatterEffect(bulletModelInstanceComponent.getModelInstance().transform.getTranslation(auxVector3_1));
		}
	}

	private void addSplatterEffect(final Vector3 pos) {
		EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
				.addParticleEffectComponent(bloodSplatterEffect, pos)
				.finishAndAddToEngine();
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
		createExplosionOnCharacterDeath(character);
		getSystemsCommonData().getSoundPlayer().playSound(soundData.getDeathSound());
		for (CharacterSystemEventsSubscriber subscriber : subscribers) {
			subscriber.onCharacterDies(character);
		}
	}

	private void createExplosionOnCharacterDeath(Entity character) {
		if (ComponentsMapper.enemy.has(character)) {
			Vector3 position = ComponentsMapper.characterDecal.get(character).getDecal().getPosition();
			float height = ComponentsMapper.enemy.get(character).getEnemyDefinition().getHeight();
			EntityBuilder.beginBuildingEntity((PooledEngine) getEngine())
					.addParticleEffectComponent(smallExpEffect,
							auxVector3_1.set(position).add(0F, height / 4F, 0F))
					.finishAndAddToEngine();
		}
	}

	private void painDone(Entity character, CharacterSpriteData spriteData) {
		spriteData.setSpriteType(IDLE);
		CharacterCommand currentCommand = getSystemsCommonData().getCurrentCommandContext();
		if (currentCommand != null && !currentCommand.isStarted()) {
			applyCommand(currentCommand, character);
		}
	}

	@Override
	public void onEnemyAwaken(Entity enemy, EnemyAiStatus prevAiStatus) {
		getSystemsCommonData().getCurrentCommandContext().onEnemyAwaken(enemy, prevAiStatus);
	}

	public void commandDone(final Entity character) {
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		characterComponent.getCharacterSpriteData().setSpriteType(SpriteType.IDLE);
		CharacterCommand lastCommand = getSystemsCommonData().getCurrentCommandContext();
		getSystemsCommonData().setCurrentCommandContext(null);
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
			Weapon selectedWeapon = getSystemsCommonData().getStorage().getSelectedWeapon();
			PlayerWeaponsDefinitions definition = (PlayerWeaponsDefinitions) (selectedWeapon.getDefinition());
			int primaryAttackHitFrameIndex = GameUtils.getPrimaryAttackHitFrameIndexForCharacter(character, definition);
			if (onGoingAttack.isDone()) {
				onGoingAttack.setType(null);
				animationComponent.setStateTime(0);
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
		return Direction.findDirection(directionToDest);
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
		charSpriteData.setSpriteType(getSystemsCommonData().getCurrentCommandContext().getDefinition().getSpriteType());
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
			CharacterCommand currentCommand = getSystemsCommonData().getCurrentCommandContext();
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
		CharacterComponent characterComponent = ComponentsMapper.character.get(character);
		CharacterSpriteData characterSpriteData = characterComponent.getCharacterSpriteData();
		CharacterCommandsDefinitions commandDef = onFrameChangedEvents.get(characterSpriteData.getSpriteType());
		if (commandDef != null) {
			SystemsCommonData commonData = getSystemsCommonData();
			if (commonData.getCurrentCommandContext().reactToFrameChange(commonData, character, newFrame, subscribers)) {
				commandDone(character);
			}
		}
	}


}
