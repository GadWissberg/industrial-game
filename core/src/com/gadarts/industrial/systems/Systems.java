package com.gadarts.industrial.systems;

import com.gadarts.industrial.systems.camera.CameraSystem;
import com.gadarts.industrial.systems.character.CharacterSystem;
import com.gadarts.industrial.systems.enemy.EnemySystem;
import com.gadarts.industrial.systems.input.InputSystem;
import com.gadarts.industrial.systems.player.PlayerSystem;
import com.gadarts.industrial.systems.projectiles.BulletSystem;
import com.gadarts.industrial.systems.render.RenderSystem;
import com.gadarts.industrial.systems.turns.TurnsSystem;
import com.gadarts.industrial.systems.ui.UserInterfaceSystem;
import lombok.Getter;

@Getter
public enum Systems {
	CAMERA(CameraSystem.class),
	INPUT(InputSystem.class),
	PLAYER(PlayerSystem.class),
	RENDER(RenderSystem.class),
	USER_INTERFACE(UserInterfaceSystem.class),
	PROFILING(ProfilingSystem.class),
	PICKUP(PickupSystem.class),
	TURNS(TurnsSystem.class),
	ENEMY(EnemySystem.class),
	PROJECTILE(BulletSystem.class),
	PARTICLE_EFFECTS(ParticleEffectsSystem.class),
	CHARACTER(CharacterSystem.class),
	AMB(AmbSystem.class);

	private final Class<? extends GameSystem<? extends SystemEventsSubscriber>> systemClass;

	Systems(Class<? extends GameSystem<? extends SystemEventsSubscriber>> systemClass) {
		this.systemClass = systemClass;
	}
}
