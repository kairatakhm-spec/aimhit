package com.example.aimhit;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Optional;

public class AimHitClient implements ClientModInitializer {
    private static final double RANGE = 4.5;
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("aimhit", "main"));
    private static KeyBinding attackKey;

    @Override
    public void onInitializeClient() {
        attackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aimhit.attack",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) return;
        if (client.currentScreen != null || !attackKey.isPressed()) return;

        Optional<PlayerEntity> target = client.world
                .getEntitiesByClass(PlayerEntity.class,
                        player.getBoundingBox().expand(RANGE),
                        e -> e != player && !e.isSpectator() && e.isAlive() && player.canSee(e) && player.distanceTo(e) <= RANGE)
                .stream()
                .min(Comparator.comparingDouble(player::distanceTo));

        if (target.isEmpty()) return;
        PlayerEntity mob = target.get();

        // Наводим камеру на глаза/центр моба
        Vec3d from = player.getEyePos();
        Vec3d to = mob.getBoundingBox().getCenter();
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        player.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        player.setPitch((float) -Math.toDegrees(Math.atan2(dy, horizontal)));

        // Бьём, когда оружие перезарядилось
        if (player.getAttackCooldownProgress(0.5f) >= 1.0f) {
            client.interactionManager.attackEntity(player, mob);
            player.swingHand(Hand.MAIN_HAND);
        }
    }
}
