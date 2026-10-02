package com.hybridash.durabilityalert;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Warns you (sound + action bar message) when the tool in your hand or a piece of armor is about to break.
 * One warning when it gets low, a louder one when it's nearly gone, and a reminder every few seconds
 * while you keep using a nearly-broken tool.
 */
public class DurabilityAlertClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("DurabilityAlert");

	private enum Level { OK, LOW, CRITICAL }

	private static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private static AlertConfig config;
	private static final Map<EquipmentSlot, Level> lastLevel = new EnumMap<>(EquipmentSlot.class);
	private static final Map<EquipmentSlot, ItemStack> lastStack = new EnumMap<>(EquipmentSlot.class);
	private static int reminderTicks = 0;

	@Override
	public void onInitializeClient() {
		config = AlertConfig.load();
		ClientTickEvents.END_CLIENT_TICK.register(DurabilityAlertClient::tick);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("durability").executes(ctx -> {
					config.enabled = !config.enabled;
					config.save();
					ctx.getSource().sendFeedback(Text.literal("Durability alerts " + (config.enabled ? "on" : "off"))
							.formatted(config.enabled ? Formatting.GREEN : Formatting.GRAY));
					return 1;
				})));
	}

	private static void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || !config.enabled || player.isCreative()) return;

		for (EquipmentSlot slot : SLOTS) {
			ItemStack stack = player.getEquippedStack(slot);
			Level level = levelOf(stack);
			ItemStack before = lastStack.get(slot);
			Level beforeLevel = lastLevel.getOrDefault(slot, Level.OK);
			boolean sameItem = before != null && ItemStack.areItemsEqual(before, stack);

			// Only alert when the item we already had gets worse, not when you switch to an old damaged tool
			if (sameItem && level.ordinal() > beforeLevel.ordinal()) {
				alert(client, stack, slot, level);
			}
			lastStack.put(slot, stack.copy());
			lastLevel.put(slot, level);
		}

		// Keep reminding while you use a nearly broken tool
		ItemStack hand = player.getMainHandStack();
		if (levelOf(hand) == Level.CRITICAL && client.options.attackKey.isPressed()) {
			if (++reminderTicks >= 100) {
				reminderTicks = 0;
				alert(client, hand, EquipmentSlot.MAINHAND, Level.CRITICAL);
			}
		} else {
			reminderTicks = Math.min(reminderTicks, 60);
		}
	}

	private static Level levelOf(ItemStack stack) {
		if (stack.isEmpty() || !stack.isDamageable()) return Level.OK;
		int max = stack.getMaxDamage();
		int left = max - stack.getDamage();
		if (left <= Math.max(config.criticalUses, max * config.criticalPercent / 100.0)) return Level.CRITICAL;
		if (left <= Math.max(config.lowUses, max * config.lowPercent / 100.0)) return Level.LOW;
		return Level.OK;
	}

	private static void alert(MinecraftClient client, ItemStack stack, EquipmentSlot slot, Level level) {
		int left = stack.getMaxDamage() - stack.getDamage();
		String where = slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? " (armor)" : "";
		Text msg = Text.literal("⚠ ").append(stack.getName()).append(Text.literal(where + ": " + left + (left == 1 ? " use left!" : " uses left!")))
				.formatted(level == Level.CRITICAL ? Formatting.RED : Formatting.GOLD, Formatting.BOLD);
		client.inGameHud.setOverlayMessage(msg, false);

		if (config.sound) {
			if (level == Level.CRITICAL) {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.6f, 1.0f));
			} else {
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.6f, 0.8f));
			}
		}
	}
}
