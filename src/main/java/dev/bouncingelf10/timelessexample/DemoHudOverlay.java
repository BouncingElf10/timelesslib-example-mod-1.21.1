package dev.bouncingelf10.timelessexample;

import dev.bouncingelf10.timelesslib.TimelessLibClient;
import dev.bouncingelf10.timelesslib.api.animation.AnimationTimeline;
import dev.bouncingelf10.timelesslib.api.animation.Easing;
import dev.bouncingelf10.timelesslib.api.animation.Interpolation;
import dev.bouncingelf10.timelesslib.api.clock.TimeSources;
import dev.bouncingelf10.timelesslib.api.cooldown.ClientCooldownManager;
import dev.bouncingelf10.timelesslib.api.countdown.Countdown;
import dev.bouncingelf10.timelesslib.api.scheduler.Scheduler;
import dev.bouncingelf10.timelesslib.api.scheduler.TaskHandle;
import dev.bouncingelf10.timelesslib.api.time.Duration;
import dev.bouncingelf10.timelesslib.api.time.TimeAnchor;
import dev.bouncingelf10.timelesslib.api.time.TimeFormat;
import dev.bouncingelf10.timelesslib.api.time.TimeFormatter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class DemoHudOverlay implements HudRenderCallback {
    private static double linearGametimeOffset;
    private static double easeGametimeOffset;
    private static double linearRealtimeOffset;
    private static double easeRealtimeOffset;

    private static Vec3 movingaboutVec3 = Vec3.ZERO;
    private static Vec3 movingaboutVec3Hermit = Vec3.ZERO;

    public static String chatParsedTimeString;

    private static final TimeAnchor timeAnchorGame = TimeAnchor.createGameTime();
    private static final TimeAnchor timeAnchorReal = TimeAnchor.createRealTime();
    private static Duration snapshotGame = timeAnchorGame.snapshot();
    private static final TimeAnchor pausedAnchor = TimeAnchor.createRealTime();

    private static final TaskHandle schedulerRepeatingGT;
    private static final TaskHandle schedulerDelayedRT;
    private static final TaskHandle schedulerFixedRate;
    private static int cycleInt = 1;
    private static String stringHasPassed = "60 seconds have NOT passed yet!";

    private static Countdown countdown;
    private static Countdown countdownRealtime;

    private static double countdownProgressOffset;
    private static double countdownRealtimeProgressOffset;
    private static String countdownState = "Running...";
    private static String realtimeCountdownState = "Running...";

    private static final Duration COUNTDOWN_TOTAL = Duration.ofSeconds(5);
    private static final Duration COUNTDOWN_RT_TOTAL = Duration.ofSeconds(3);

    public static final ClientCooldownManager<Minecraft> cooldownManager = TimelessLibClient.getClientCooldownManager();
    private static final String DEMO_ATTACK_KEY = "demo_attack";
    private static final String DEMO_DASH_KEY = "demo_dash";

    private static final Duration CD_ATTACK = Duration.ofSeconds(4);
    private static final Duration CD_DASH = Duration.ofSeconds(2);

    private static final AnimationTimeline GUI_TIMELINE_GAMETIME = TimelessLibClient.getClientAnimationManager()
            .createTimeline("gui_gametime")
            .loop(true)
            .setTimeSource(TimeSources.GAME_TIME);

    private static final AnimationTimeline GUI_TIMELINE_REALTIME = TimelessLibClient.getClientAnimationManager()
            .createTimeline("gui_realtime")
            .loop(true).pingPong(true)
            .setTimeSource(TimeSources.REAL_TIME);

    static {
        Scheduler<Minecraft> scheduler = TimelessLibClient.getClientScheduler();

        schedulerRepeatingGT = scheduler.every(Duration.SECOND, mc -> {});
        schedulerDelayedRT = scheduler.after(Duration.ofSeconds(60), mc -> {
            stringHasPassed = "60 seconds have passed!";
        });
        schedulerFixedRate = scheduler.everyFixedRate(Duration.ofTicks(20), mc -> {
            cycleInt *= -1;
        });

        GUI_TIMELINE_GAMETIME.channelDouble("linear_gametime")
                .keyframe(0,  0)
                .keyframe(5,  100)
                .keyframe(10, 0)
                .defaultEasing(Easing.LINEAR)
                .defaultInterpolation(Interpolation.EASE)
                .bind(value -> linearGametimeOffset = value);

        GUI_TIMELINE_GAMETIME.channelDouble("ease_gametime")
                .keyframe(0,  0)
                .keyframe(5,  100)
                .keyframe(10, 0)
                .defaultEasing(Easing.EASE_OUT_QUAD)
                .defaultInterpolation(Interpolation.EASE)
                .bind(value -> easeGametimeOffset = value);

        GUI_TIMELINE_REALTIME.channelDouble("linear_realtime")
                .keyframe(0,  0)
                .keyframe(5,  100)
                .defaultEasing(Easing.LINEAR)
                .defaultInterpolation(Interpolation.EASE)
                .bind(value -> linearRealtimeOffset = value);

        GUI_TIMELINE_REALTIME.channelDouble("ease_realtime")
                .keyframe(0,  0)
                .keyframe(5,  100)
                .defaultEasing(Easing.EASE_OUT_QUAD)
                .defaultInterpolation(Interpolation.EASE)
                .bind(value -> easeRealtimeOffset = value);

        GUI_TIMELINE_GAMETIME.channelVec3("movingabout_linear")
                .keyframe(0, new Vec3(0, 0, 0))
                .keyframe(2, new Vec3(100, 0, 0))
                .keyframe(4, new Vec3(100, 50, 0))
                .keyframe(6, new Vec3(0, 50, 0))
                .keyframe(8, new Vec3(0, 0, 0))
                .defaultEasing(Easing.LINEAR)
                .defaultInterpolation(Interpolation.LINEAR)
                .bind(value -> movingaboutVec3 = value);

        GUI_TIMELINE_GAMETIME.channelVec3("movingabout_hermit")
                .keyframe(0, new Vec3(0, 0, 0))
                .keyframe(2, new Vec3(100, 0, 0))
                .keyframe(4, new Vec3(100, 50, 0))
                .keyframe(6, new Vec3(0, 50, 0))
                .keyframe(8, new Vec3(0, 0, 0))
                .defaultEasing(Easing.EASE_IN_OUT_SINE)
                .defaultInterpolation(Interpolation.CATMULL)
                .bind(value -> movingaboutVec3Hermit = value);

        startCountdowns();

        cooldownManager.start(DEMO_ATTACK_KEY, CD_ATTACK);
        cooldownManager.startRealtime(DEMO_DASH_KEY, CD_DASH);
    }

    public static void startCountdowns() {
        countdownState = "Running...";
        realtimeCountdownState = "Running...";

        if (countdown != null) countdown.cancel();
        if (countdownRealtime != null) countdownRealtime.cancel();

        countdown = TimelessLibClient.getClientCountdownManager()
                .start(COUNTDOWN_TOTAL)
                .onTick((mc, remaining) -> {
                    double pct = 1.0 - (remaining.toMillis() / COUNTDOWN_TOTAL.toMillis());
                    countdownProgressOffset = pct * 100.0;
                })
                .onThreshold(Duration.ofSeconds(3), mc -> countdownState = "Hit 3s Threshold!")
                .onFinish(mc -> countdownState = "Finished!");

        countdownRealtime = TimelessLibClient.getClientCountdownManager()
                .startRealtime(COUNTDOWN_RT_TOTAL)
                .onTick((mc, remaining) -> {
                    double pct = 1.0 - (remaining.toMillis() / COUNTDOWN_RT_TOTAL.toMillis());
                    countdownRealtimeProgressOffset = pct * 100.0;
                })
                .onThreshold(Duration.ofSeconds(2), mc -> realtimeCountdownState = "Hit 2s Threshold!")
                .onFinish(mc -> realtimeCountdownState = "Finished!");
    }

    public static void playOrReset() {
        GUI_TIMELINE_GAMETIME.playOrReset();
        GUI_TIMELINE_REALTIME.playOrReset();
        timeAnchorGame.playOrReset();
        timeAnchorReal.playOrReset();
        pausedAnchor.playOrReset();
        startCountdowns();

        cooldownManager.start(DEMO_ATTACK_KEY, CD_ATTACK);
        cooldownManager.startRealtime(DEMO_DASH_KEY, CD_DASH);
    }

    public static void pauseOrUnpause() {
        GUI_TIMELINE_GAMETIME.pauseOrUnpause();
        GUI_TIMELINE_REALTIME.pauseOrUnpause();
        timeAnchorGame.pauseOrUnpause();
        timeAnchorReal.pauseOrUnpause();
        pausedAnchor.pauseOrUnpause();
        snapshotGame = timeAnchorGame.snapshot();
        schedulerRepeatingGT.pauseOrUnpause();
        schedulerDelayedRT.pauseOrUnpause();
        schedulerFixedRate.pauseOrUnpause();
        countdown.pauseOrUnpause();
        countdownRealtime.pauseOrUnpause();
        cooldownManager.reset(DEMO_ATTACK_KEY);
        cooldownManager.reset(DEMO_DASH_KEY);
    }

    @Override
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        guiGraphics.drawString(font, "Linear Game Time", 10, 10, 0xFFFFFF, false);
        guiGraphics.renderItem(new ItemStack(Items.CLOCK), (int)(10 + linearGametimeOffset), 20);
        guiGraphics.drawString(font, "Ease Game Time", 10, 26, 0xFFFFFF, false);
        guiGraphics.renderItem(new ItemStack(Items.CLOCK), (int)(10 + easeGametimeOffset), 36);
        guiGraphics.drawString(font, "Linear Real Time", 10, 42, 0xFFFFFF, false);
        guiGraphics.renderItem(new ItemStack(Items.CLOCK), (int)(10 + linearRealtimeOffset), 52);
        guiGraphics.drawString(font, "Ease Real Time", 10, 58, 0xFFFFFF, false);
        guiGraphics.renderItem(new ItemStack(Items.CLOCK), (int)(10 + easeRealtimeOffset), 68);

        guiGraphics.drawString(font, "Moving About", 10, 80, 0xFFFFFF, false);
        guiGraphics.renderItem(new ItemStack(Items.REDSTONE), 10, 90);
        guiGraphics.renderItem(new ItemStack(Items.REDSTONE), 10 + 100, 90);
        guiGraphics.renderItem(new ItemStack(Items.REDSTONE), 10 + 100, 90 + 50);
        guiGraphics.renderItem(new ItemStack(Items.REDSTONE), 10, 90 + 50);
        guiGraphics.renderItem(new ItemStack(Items.CLOCK), (int) (10 + movingaboutVec3.x), (int) (90 + movingaboutVec3.y));
        guiGraphics.renderItem(new ItemStack(Items.CLOCK), (int) (10 + movingaboutVec3Hermit.x), (int) (90 + movingaboutVec3Hermit.y));

        guiGraphics.drawString(font, "Chat Parsed Time:", 10, 160, 0xFFFFFF, false);
        guiGraphics.drawString(font, chatParsedTimeString, 10, 170, 0xFFFFFF, false);

        guiGraphics.drawString(font, "TimeAnchor (Game Time)", 130, 10, 0xFFFFFF, false);
        guiGraphics.renderItem(new ItemStack(Items.CLOCK), 130 + (int)(timeAnchorGame.elapsedSeconds() * 10), 20);
        guiGraphics.drawString(font, "TimeAnchor (Real Time)", 130, 36, 0xFFFFFF, false);
        guiGraphics.renderItem(new ItemStack(Items.CLOCK), 130 + (int)(timeAnchorReal.elapsedSeconds() * 10), 46);
        guiGraphics.drawString(font, "Snapshot Δ (ms): " + timeAnchorGame.elapsedSince(snapshotGame).toMillis(),
                130, 62, 0xAAAAFF, false);
        guiGraphics.drawString(font, "Paused Anchor: " + (pausedAnchor.isPaused() ? "PAUSED" : "RUNNING"),
                130, 72, pausedAnchor.isPaused() ? 0xFF8080 : 0x80FF80, false);

        guiGraphics.drawString(font, "Scheduler Tests", 130, 82, 0xFFFFFF, false);
        guiGraphics.drawString(font, "Repeating Next In:", 130, 92 + 14, 0xFFFFFF, false);
        guiGraphics.renderItem(new ItemStack(Items.CLOCK),
                130 + (int)(schedulerRepeatingGT.getRemainingDelay().orElse(Duration.ZERO).toMillis() / 5), 102);
        guiGraphics.drawString(font, "Delayed Remaining:", 130, 118, 0xFFFFFF, false);
        guiGraphics.drawString(font, stringHasPassed, 130, 128, 0xFFFFFF, false);
        schedulerDelayedRT.getRemainingDelay().ifPresent(delay -> {
            guiGraphics.renderItem(new ItemStack(Items.REDSTONE), 120 + (int)(delay.toMillis() / 1000), 128);
        });
        guiGraphics.drawString(font, "FixedRate:", 130, 144, 0xFFFFFF, false);
        if (cycleInt == 1) guiGraphics.renderItem(new ItemStack(Items.GOLD_NUGGET), 130, 154);
        else guiGraphics.renderItem(new ItemStack(Items.IRON_NUGGET), 130, 154);

        int cy = 180;
        guiGraphics.drawString(font, "Countdown (Game Time 5s)", 10, cy, 0xFFFFFF, false);
        guiGraphics.drawString(font, "State: " + countdownState, 10, cy + 10, 0xFFFFFF, false);
        guiGraphics.renderItem(new ItemStack(Items.CLOCK), 10 + (int) countdownProgressOffset, cy + 20);

        guiGraphics.drawString(font, "Remaining: " + countdown.remaining().toString(TimeFormat.COMPACT),
                10, cy + 36, 0xAAAAFF, false);

        guiGraphics.drawString(font, "Countdown (Real Time 3s)", 150, cy, 0xFFFFFF, false);
        guiGraphics.drawString(font, "State: " + realtimeCountdownState, 150, cy + 10, 0xFFFFFF, false);
        guiGraphics.renderItem(new ItemStack(Items.CLOCK), 150 + (int) countdownRealtimeProgressOffset, cy + 20);
        guiGraphics.drawString(font, "Remaining: " + countdownRealtime.remaining().toString(TimeFormat.COMPACT),
                150, cy + 36, 0xAAAAFF, false);

        int cdX = 280;
        int cdY = 10;

        guiGraphics.drawString(font, "Cooldown Tests", cdX, cdY, 0xFFFFFF, false);
        cdY += 12;

        boolean attackReady = cooldownManager.isReady(DEMO_ATTACK_KEY);

        guiGraphics.drawString(font, "Attack:", cdX, cdY, 0xFFFFFF, false);
        cdY += 12;

        if (attackReady) {
            guiGraphics.drawString(font, "READY", cdX + 10, cdY, 0x80FF80, false);
        } else {
            Duration r = cooldownManager.remaining(DEMO_ATTACK_KEY);
            guiGraphics.drawString(font, "Remaining: " + r.toString(TimeFormat.COMPACT),
                    cdX + 10, cdY, 0xFF8080, false);
        }
        cdY += 20;

        guiGraphics.renderItem(new ItemStack(Items.IRON_SWORD), cdX - 18, cdY - 20);

        boolean dashReady = cooldownManager.isReady(DEMO_DASH_KEY);

        guiGraphics.drawString(font, "Dash:", cdX, cdY, 0xFFFFFF, false);
        cdY += 12;

        if (dashReady) {
            guiGraphics.drawString(font, "READY", cdX + 10, cdY, 0x80FF80, false);
        } else {
            Duration r = cooldownManager.remaining(DEMO_DASH_KEY);
            guiGraphics.drawString(font, "Remaining: " + r.toString(TimeFormat.COMPACT),
                    cdX + 10, cdY, 0xFF8080, false);
        }
        cdY += 20;
        guiGraphics.renderItem(new ItemStack(Items.FEATHER), cdX - 18, cdY - 20);
    }
}