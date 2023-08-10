package fuzs.puzzleslib.mixin.client;

import fuzs.puzzleslib.api.client.event.v1.FabricClientEvents;
import fuzs.puzzleslib.api.client.event.v1.FabricScreenEvents;
import fuzs.puzzleslib.api.event.v1.core.EventResult;
import fuzs.puzzleslib.api.event.v1.data.DefaultedValue;
import fuzs.puzzleslib.mixin.client.accessor.KeyMappingFabricAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Minecraft.class)
public abstract class MinecraftFabricMixin {
    @Shadow
    @Nullable
    public ClientLevel level;
    @Shadow
    @Nullable
    public LocalPlayer player;
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;
    @Shadow
    @Final
    public Options options;
    @Shadow
    @Nullable
    public Screen screen;
    @Shadow
    protected int missTime;
    @Unique
    private DefaultedValue<Screen> puzzleslib$newScreen;
    @Unique
    private boolean puzzleslib$attackCancelled;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    public void setScreen$0(@Nullable Screen newScreen, CallbackInfo callback) {
        // we handle this callback at head to avoid having to block Screen#remove
        newScreen = this.puzzleslib$handleEmptyScreen(newScreen);
        this.puzzleslib$newScreen = DefaultedValue.fromValue(newScreen);
        EventResult result = FabricScreenEvents.SCREEN_OPENING.invoker().onScreenOpening(this.screen, this.puzzleslib$newScreen);
        if (result.isInterrupt() || this.puzzleslib$newScreen.getAsOptional().filter(screen -> screen == this.screen).isPresent()) callback.cancel();
    }

    @Unique
    @Nullable
    private Screen puzzleslib$handleEmptyScreen(@Nullable Screen newScreen) {
        // copy this vanilla functionality, it runs after the screen is removed, but we need it before that to potentially block the removal
        if (newScreen == null && this.level == null) {
            return new TitleScreen();
        } else if (newScreen == null && this.player.isDeadOrDying()) {
            if (this.player.shouldShowDeathScreen()) {
                return new DeathScreen(null, this.level.getLevelData().isHardcore());
            } else {
                this.player.respawn();
            }
        }
        return newScreen;
    }

    @ModifyVariable(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", shift = At.Shift.BEFORE, ordinal = 0), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;respawn()V")), ordinal = 0)
    public Screen setScreen$1(@Nullable Screen newScreen) {
        Objects.requireNonNull(this.puzzleslib$newScreen, "new screen is null");
        // problematic for our own title / death screen instances, in case event listeners depend on the exact reference
        // but probably better our implementation doesn't work perfectly than breaking other mods which would be hard to trace
        newScreen = this.puzzleslib$newScreen.getAsOptional().orElse(newScreen);
        this.puzzleslib$newScreen = null;
        return newScreen;
    }

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;resetData()V", shift = At.Shift.AFTER))
    public void clearLevel(Screen screen, CallbackInfo callback) {
        if (this.player == null || this.gameMode == null) return;
        Connection connection = this.player.connection.getConnection();
        Objects.requireNonNull(connection, "connection is null");
        FabricClientEvents.PLAYER_LOGGED_OUT.invoker().onLoggedOut(this.player, this.gameMode, connection);
    }

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0))
    private void handleKeybinds(CallbackInfo ci) {
        int attackKeyPressCount = ((KeyMappingFabricAccessor) this.options.keyAttack).puzzleslib$getClickCount();
        if (this.options.keyAttack.isDown() || attackKeyPressCount != 0) {
            this.puzzleslib$attackCancelled = this.puzzleslib$onClientPlayerPreAttack(Minecraft.class.cast(this), this.player, attackKeyPressCount, this.missTime);
        } else {
            this.puzzleslib$attackCancelled = false;
        }
    }

    @Unique
    private boolean puzzleslib$onClientPlayerPreAttack(Minecraft client, LocalPlayer player, int clickCount, int missTime) {
        if (missTime <= 0 && client.hitResult != null) {
            if (clickCount != 0) {
                if (!player.isHandsBusy()) {
                    return FabricClientEvents.ATTACK_INTERACTION_INPUT.invoker().onAttackInteraction(client, player).isInterrupt();
                }
            } else {
                if (!player.isUsingItem() && client.hitResult.getType() == HitResult.Type.BLOCK) {
                    if (!client.level.isEmptyBlock(((BlockHitResult) client.hitResult).getBlockPos())) {
                        return FabricClientEvents.ATTACK_INTERACTION_INPUT.invoker().onAttackInteraction(client, player).isInterrupt();
                    }
                }
            }
        }
        return false;
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void startAttack(CallbackInfoReturnable<Boolean> callback) {
        if (this.puzzleslib$attackCancelled) callback.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void continueAttack(boolean leftClick, CallbackInfo callback) {
        if (this.puzzleslib$attackCancelled) {
            if (this.gameMode != null) this.gameMode.stopDestroyBlock();
            callback.cancel();
        }
    }
}
