package com.softclose.config;

import com.softclose.SoftCloseMod;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClothConfigScreen {

    private static final Map<String, Integer> KEY_NAME_TO_CODE = new LinkedHashMap<>();
    private static final Map<Integer, String> KEY_CODE_TO_NAME = new LinkedHashMap<>();

    static {
        register("F1",  GLFW.GLFW_KEY_F1);  register("F2",  GLFW.GLFW_KEY_F2);
        register("F3",  GLFW.GLFW_KEY_F3);  register("F4",  GLFW.GLFW_KEY_F4);
        register("F5",  GLFW.GLFW_KEY_F5);  register("F6",  GLFW.GLFW_KEY_F6);
        register("F7",  GLFW.GLFW_KEY_F7);  register("F8",  GLFW.GLFW_KEY_F8);
        register("F9",  GLFW.GLFW_KEY_F9);  register("F10", GLFW.GLFW_KEY_F10);
        register("F11", GLFW.GLFW_KEY_F11); register("F12", GLFW.GLFW_KEY_F12);
        register("INSERT",       GLFW.GLFW_KEY_INSERT);
        register("DELETE",       GLFW.GLFW_KEY_DELETE);
        register("HOME",         GLFW.GLFW_KEY_HOME);
        register("END",          GLFW.GLFW_KEY_END);
        register("PAGE_UP",      GLFW.GLFW_KEY_PAGE_UP);
        register("PAGE_DOWN",    GLFW.GLFW_KEY_PAGE_DOWN);
        register("UP",           GLFW.GLFW_KEY_UP);
        register("DOWN",         GLFW.GLFW_KEY_DOWN);
        register("LEFT",         GLFW.GLFW_KEY_LEFT);
        register("RIGHT",        GLFW.GLFW_KEY_RIGHT);
        for (char c = 'A'; c <= 'Z'; c++) {
            register(String.valueOf(c), GLFW.GLFW_KEY_A + (c - 'A'));
        }
        for (int d = 0; d <= 9; d++) {
            register(String.valueOf(d), GLFW.GLFW_KEY_0 + d);
        }
        for (int n = 0; n <= 9; n++) {
            register("KP_" + n, GLFW.GLFW_KEY_KP_0 + n);
        }
        register("SPACE",         GLFW.GLFW_KEY_SPACE);
        register("ENTER",         GLFW.GLFW_KEY_ENTER);
        register("TAB",           GLFW.GLFW_KEY_TAB);
        register("BACKSPACE",     GLFW.GLFW_KEY_BACKSPACE);
        register("ESCAPE",        GLFW.GLFW_KEY_ESCAPE);
        register("GRAVE",         GLFW.GLFW_KEY_GRAVE_ACCENT);
        register("MINUS",         GLFW.GLFW_KEY_MINUS);
        register("EQUAL",         GLFW.GLFW_KEY_EQUAL);
        register("SEMICOLON",     GLFW.GLFW_KEY_SEMICOLON);
        register("APOSTROPHE",    GLFW.GLFW_KEY_APOSTROPHE);
        register("COMMA",         GLFW.GLFW_KEY_COMMA);
        register("PERIOD",        GLFW.GLFW_KEY_PERIOD);
        register("SLASH",         GLFW.GLFW_KEY_SLASH);
        register("BACKSLASH",     GLFW.GLFW_KEY_BACKSLASH);
        register("LEFT_BRACKET",  GLFW.GLFW_KEY_LEFT_BRACKET);
        register("RIGHT_BRACKET", GLFW.GLFW_KEY_RIGHT_BRACKET);
    }

    private static void register(String name, int code) {
        KEY_NAME_TO_CODE.put(name, code);
        KEY_CODE_TO_NAME.put(code, name);
    }

    public static String keyName(int code) {
        return KEY_CODE_TO_NAME.getOrDefault(code, "KEY_" + code);
    }

    public static int keyCode(String name) {
        return KEY_NAME_TO_CODE.getOrDefault(name.toUpperCase().trim(), -1);
    }

    private static String keyListHint() {
        return String.join(", ", KEY_NAME_TO_CODE.keySet());
    }

    public static Screen build(Screen parent) {
        ConfigManager manager = SoftCloseMod.getInstance().getConfigManager();
        ConfigManager.SoftCloseConfig cfg = manager.getConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("softclose.config.title"))
                .setSavingRunnable(() -> {
                    manager.saveMainConfig();
                    SoftCloseMod.getInstance().getKeybindManager().applyFromConfig(cfg);
                    SoftCloseMod.LOGGER.info("[SoftClose] Config saved");
                });

        ConfigEntryBuilder eb = builder.entryBuilder();

        // ── General ──────────────────────────────────────────────────────────
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("softclose.config.category.general"));

        general.addEntry(eb.startBooleanToggle(Text.translatable("softclose.config.auto_cycle"), cfg.autoCycle)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("softclose.config.auto_cycle.tooltip"))
                .setSaveConsumer(v -> cfg.autoCycle = v)
                .build());

        general.addEntry(eb.startIntSlider(Text.translatable("softclose.config.cycle_delay"), cfg.cycleDelayMs, 100, 5000)
                .setDefaultValue(500)
                .setTooltip(Text.translatable("softclose.config.cycle_delay.tooltip"))
                .setSaveConsumer(v -> cfg.cycleDelayMs = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Text.translatable("softclose.config.manual_confirm"), cfg.manualConfirm)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("softclose.config.manual_confirm.tooltip"))
                .setSaveConsumer(v -> cfg.manualConfirm = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Text.translatable("softclose.config.block_click_mode"), cfg.blockClickMode)
                .setDefaultValue(false)
                .setTooltip(
                        Text.translatable("softclose.config.block_click_mode.tooltip1"),
                        Text.translatable("softclose.config.block_click_mode.tooltip2")
                )
                .setSaveConsumer(v -> cfg.blockClickMode = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Text.translatable("softclose.config.drop_if_full"), cfg.dropIfFull)
                .setDefaultValue(false)
                .setTooltip(
                        Text.translatable("softclose.config.drop_if_full.tooltip1"),
                        Text.translatable("softclose.config.drop_if_full.tooltip2")
                )
                .setSaveConsumer(v -> cfg.dropIfFull = v)
                .build());

        // ── Commands (only when Block Click Mode is OFF) ──────────────────────
        if (!cfg.blockClickMode) {
            ConfigCategory commands = builder.getOrCreateCategory(Text.translatable("softclose.config.category.commands"));

            commands.addEntry(eb.startStrField(Text.translatable("softclose.config.gui_open_command"), cfg.guiOpenCommand)
                    .setDefaultValue("/say open")
                    .setTooltip(Text.translatable("softclose.config.gui_open_command.tooltip"))
                    .setSaveConsumer(v -> cfg.guiOpenCommand = v)
                    .build());

            commands.addEntry(eb.startStrField(Text.translatable("softclose.config.active_profile"), cfg.activeProfileName)
                    .setDefaultValue("Default")
                    .setTooltip(Text.translatable("softclose.config.active_profile.tooltip"))
                    .setSaveConsumer(v -> cfg.activeProfileName = v)
                    .build());
        }

        // ── Block Click Mode (only when Block Click Mode is ON) ───────────────
        if (cfg.blockClickMode) {
            ConfigCategory blockClick = builder.getOrCreateCategory(Text.translatable("softclose.config.category.blockclick"));

            blockClick.addEntry(eb.startBooleanToggle(Text.translatable("softclose.config.target_block_set"), cfg.targetBlockSet)
                    .setDefaultValue(false)
                    .setTooltip(
                            Text.translatable("softclose.config.target_block_set.tooltip1"),
                            Text.translatable("softclose.config.target_block_set.tooltip2")
                    )
                    .setSaveConsumer(v -> {
                        cfg.targetBlockSet = v;
                        if (!v) SoftCloseMod.LOGGER.info("[SoftClose] Target block cleared via config");
                    })
                    .build());

            blockClick.addEntry(eb.startIntField(Text.translatable("softclose.config.target_block_x"), cfg.targetBlockX)
                    .setDefaultValue(0)
                    .setTooltip(Text.translatable("softclose.config.target_block_x.tooltip"))
                    .setSaveConsumer(v -> cfg.targetBlockX = v)
                    .build());

            blockClick.addEntry(eb.startIntField(Text.translatable("softclose.config.target_block_y"), cfg.targetBlockY)
                    .setDefaultValue(64)
                    .setTooltip(Text.translatable("softclose.config.target_block_y.tooltip"))
                    .setSaveConsumer(v -> cfg.targetBlockY = v)
                    .build());

            blockClick.addEntry(eb.startIntField(Text.translatable("softclose.config.target_block_z"), cfg.targetBlockZ)
                    .setDefaultValue(0)
                    .setTooltip(Text.translatable("softclose.config.target_block_z.tooltip"))
                    .setSaveConsumer(v -> cfg.targetBlockZ = v)
                    .build());

            blockClick.addEntry(eb.startStrField(Text.translatable("softclose.config.target_block_dim"), cfg.targetBlockDimension)
                    .setDefaultValue("minecraft:overworld")
                    .setTooltip(Text.translatable("softclose.config.target_block_dim.tooltip"))
                    .setSaveConsumer(v -> cfg.targetBlockDimension = v)
                    .build());

            blockClick.addEntry(eb.startTextDescription(
                    Text.translatable("softclose.config.block_click_hint")
            ).build());
        }

        // ── Slot Pickup ───────────────────────────────────────────────────────
        ConfigCategory slots = builder.getOrCreateCategory(Text.translatable("softclose.config.category.slots"));

        String slotsString = cfg.pickupSlots.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        slots.addEntry(eb.startStrField(Text.translatable("softclose.config.pickup_slots"), slotsString)
                .setDefaultValue("0,1,2,3,4,5,6,7,8")
                .setTooltip(Text.translatable("softclose.config.pickup_slots.tooltip"))
                .setSaveConsumer(v -> {
                    List<Integer> parsed = new ArrayList<>();
                    for (String part : v.split(",")) {
                        try {
                            String t = part.trim();
                            if (t.contains("-")) {
                                String[] range = t.split("-");
                                int from = Integer.parseInt(range[0].trim());
                                int to   = Integer.parseInt(range[1].trim());
                                for (int i = from; i <= to; i++) parsed.add(i);
                            } else {
                                parsed.add(Integer.parseInt(t));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                    cfg.pickupSlots = parsed;
                })
                .build());

        // ── Keybinds ──────────────────────────────────────────────────────────
        ConfigCategory keybinds = builder.getOrCreateCategory(Text.translatable("softclose.config.category.keybinds"));

        keybinds.addEntry(eb.startTextDescription(
                Text.translatable("softclose.config.keybinds.hint")
        ).build());

        keybinds.addEntry(eb.startStrField(
                Text.translatable("softclose.config.keybind.start"), keyName(cfg.keybindStart))
                .setDefaultValue("F7")
                .setTooltip(
                        Text.translatable("softclose.config.keybind.start.tooltip"),
                        Text.translatable("softclose.config.keybind.available", keyListHint())
                )
                .setSaveConsumer(v -> {
                    int code = keyCode(v);
                    if (code != -1) cfg.keybindStart = code;
                    else SoftCloseMod.LOGGER.warn("[SoftClose] Unknown key name for start: {}", v);
                })
                .build());

        keybinds.addEntry(eb.startStrField(
                Text.translatable("softclose.config.keybind.stop"), keyName(cfg.keybindStop))
                .setDefaultValue("END")
                .setTooltip(
                        Text.translatable("softclose.config.keybind.stop.tooltip"),
                        Text.translatable("softclose.config.keybind.available", keyListHint())
                )
                .setSaveConsumer(v -> {
                    int code = keyCode(v);
                    if (code != -1) cfg.keybindStop = code;
                    else SoftCloseMod.LOGGER.warn("[SoftClose] Unknown key name for stop: {}", v);
                })
                .build());

        keybinds.addEntry(eb.startStrField(
                Text.translatable("softclose.config.keybind.config"), keyName(cfg.keybindConfig))
                .setDefaultValue("F8")
                .setTooltip(
                        Text.translatable("softclose.config.keybind.config.tooltip"),
                        Text.translatable("softclose.config.keybind.available", keyListHint())
                )
                .setSaveConsumer(v -> {
                    int code = keyCode(v);
                    if (code != -1) cfg.keybindConfig = code;
                    else SoftCloseMod.LOGGER.warn("[SoftClose] Unknown key name for config: {}", v);
                })
                .build());

        return builder.build();
    }
}
