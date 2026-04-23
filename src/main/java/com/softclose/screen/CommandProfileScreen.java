package com.softclose.screen;

import com.softclose.SoftCloseMod;
import com.softclose.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class CommandProfileScreen extends Screen {

    private final Screen parent;
    private final ConfigManager configManager;

    private TextFieldWidget profileNameField;
    private TextFieldWidget onOpenField;
    private TextFieldWidget afterPickupField;
    private TextFieldWidget beforeCloseField;

    private int selectedProfileIndex = 0;
    private String statusMessage = "";
    private int statusTimer = 0;

    private static final int ACCENT = 0xFF5A5AFF;
    private static final int TEXT   = 0xFFEEEEEE;
    private static final int MUTED  = 0xFF888899;

    public CommandProfileScreen(Screen parent) {
        super(Text.translatable("softclose.screen.profiles.title"));
        this.parent = parent;
        this.configManager = SoftCloseMod.getInstance().getConfigManager();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int startY = 80;
        int fieldW = 260;
        int fieldH = 18;
        int gap = 30;

        profileNameField = new TextFieldWidget(this.textRenderer,
                cx - fieldW / 2, startY, fieldW, fieldH,
                Text.translatable("softclose.screen.profiles.placeholder.name"));
        profileNameField.setMaxLength(64);
        this.addSelectableChild(profileNameField);

        onOpenField = new TextFieldWidget(this.textRenderer,
                cx - fieldW / 2, startY + gap, fieldW, fieldH,
                Text.translatable("softclose.screen.profiles.placeholder.on_open"));
        onOpenField.setMaxLength(256);
        this.addSelectableChild(onOpenField);

        afterPickupField = new TextFieldWidget(this.textRenderer,
                cx - fieldW / 2, startY + gap * 2, fieldW, fieldH,
                Text.translatable("softclose.screen.profiles.placeholder.after_pickup"));
        afterPickupField.setMaxLength(256);
        this.addSelectableChild(afterPickupField);

        beforeCloseField = new TextFieldWidget(this.textRenderer,
                cx - fieldW / 2, startY + gap * 3, fieldW, fieldH,
                Text.translatable("softclose.screen.profiles.placeholder.before_close"));
        beforeCloseField.setMaxLength(256);
        this.addSelectableChild(beforeCloseField);

        loadSelectedProfile();

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("softclose.screen.profiles.btn.save"), btn -> saveProfile())
                .dimensions(cx - 130, startY + gap * 4 + 10, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("softclose.screen.profiles.btn.new"), btn -> newProfile())
                .dimensions(cx + 10, startY + gap * 4 + 10, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("softclose.screen.profiles.btn.delete"), btn -> deleteProfile())
                .dimensions(cx - 60, startY + gap * 4 + 36, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("softclose.screen.profiles.btn.back"), btn -> close())
                .dimensions(10, this.height - 30, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("◀"), btn -> navigateProfile(-1))
                .dimensions(cx - 120, 48, 20, 16).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("▶"), btn -> navigateProfile(1))
                .dimensions(cx + 100, 48, 20, 16).build());
    }

    private void loadSelectedProfile() {
        List<ConfigManager.CommandProfile> profiles = configManager.getProfiles();
        if (profiles.isEmpty()) return;
        if (selectedProfileIndex >= profiles.size()) selectedProfileIndex = 0;

        ConfigManager.CommandProfile p = profiles.get(selectedProfileIndex);
        profileNameField.setText(p.name);
        onOpenField.setText(p.onOpenCommand);
        afterPickupField.setText(p.afterPickupCommand);
        beforeCloseField.setText(p.beforeCloseCommand);
    }

    private void saveProfile() {
        List<ConfigManager.CommandProfile> profiles = configManager.getProfiles();
        ConfigManager.CommandProfile p;

        if (profiles.isEmpty() || selectedProfileIndex >= profiles.size()) {
            p = new ConfigManager.CommandProfile();
            profiles.add(p);
            selectedProfileIndex = profiles.size() - 1;
        } else {
            p = profiles.get(selectedProfileIndex);
        }

        p.name = profileNameField.getText();
        p.onOpenCommand = onOpenField.getText();
        p.afterPickupCommand = afterPickupField.getText();
        p.beforeCloseCommand = beforeCloseField.getText();

        configManager.saveProfiles();
        showStatus(Text.translatable("softclose.screen.profiles.status.saved").getString());
    }

    private void newProfile() {
        ConfigManager.CommandProfile p = new ConfigManager.CommandProfile();
        p.name = "New Profile " + (configManager.getProfiles().size() + 1);
        configManager.addProfile(p);
        selectedProfileIndex = configManager.getProfiles().size() - 1;
        loadSelectedProfile();
        showStatus(Text.translatable("softclose.screen.profiles.status.created").getString());
    }

    private void deleteProfile() {
        List<ConfigManager.CommandProfile> profiles = configManager.getProfiles();
        if (profiles.isEmpty()) return;
        String name = profiles.get(selectedProfileIndex).name;
        configManager.removeProfile(name);
        selectedProfileIndex = Math.max(0, selectedProfileIndex - 1);
        loadSelectedProfile();
        showStatus(Text.translatable("softclose.screen.profiles.status.deleted").getString());
    }

    private void navigateProfile(int delta) {
        int size = configManager.getProfiles().size();
        if (size == 0) return;
        selectedProfileIndex = (selectedProfileIndex + delta + size) % size;
        loadSelectedProfile();
    }

    private void showStatus(String msg) {
        statusMessage = msg;
        statusTimer = 60;
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) statusTimer--;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int startY = 80;
        int fieldW = 260;
        int gap = 30;

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("softclose.screen.profiles.manage"), cx, 12, ACCENT);

        List<ConfigManager.CommandProfile> profiles = configManager.getProfiles();
        String profileLabel = profiles.isEmpty()
                ? Text.translatable("softclose.screen.profiles.none").getString()
                : profiles.get(selectedProfileIndex).name
                  + " [" + (selectedProfileIndex + 1) + "/" + profiles.size() + "]";
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("softclose.screen.profiles.current", profileLabel), cx, 52, TEXT);

        renderLabel(context, Text.translatable("softclose.screen.profiles.field.name").getString(),         cx - fieldW / 2, startY - 10);
        renderLabel(context, Text.translatable("softclose.screen.profiles.field.on_open").getString(),      cx - fieldW / 2, startY + gap - 10);
        renderLabel(context, Text.translatable("softclose.screen.profiles.field.after_pickup").getString(), cx - fieldW / 2, startY + gap * 2 - 10);
        renderLabel(context, Text.translatable("softclose.screen.profiles.field.before_close").getString(), cx - fieldW / 2, startY + gap * 3 - 10);

        profileNameField.render(context, mouseX, mouseY, delta);
        onOpenField.render(context, mouseX, mouseY, delta);
        afterPickupField.render(context, mouseX, mouseY, delta);
        beforeCloseField.render(context, mouseX, mouseY, delta);

        if (statusTimer > 0) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(statusMessage), cx, startY + gap * 4 + 62, 0xFF44FF88);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderLabel(DrawContext context, String label, int x, int y) {
        context.drawText(this.textRenderer, Text.literal(label), x, y, MUTED, false);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
