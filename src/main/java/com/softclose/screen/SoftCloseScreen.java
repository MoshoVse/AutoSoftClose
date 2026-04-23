package com.softclose.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SoftCloseScreen extends Screen {

    // Тонкая полоса над GUI контейнера
    private List<Integer> getContainerSlotNumbers(HandledScreen<?> screen) {
        // 1. Получаем хендлер
        var handler = screen.getScreenHandler();

        // 2. Считаем только слоты контейнера (минус 36 слотов игрока)
        int containerSlotsCount = handler.slots.size() - 36;

        // Защита: если слотов вдруг меньше 1 (например, пустой экран)
        if (containerSlotsCount <= 0) return List.of(0);

        // 3. Генерируем список от 1 до N
        return IntStream.rangeClosed(1, containerSlotsCount)
                .boxed()
                .collect(Collectors.toList());
    }
    private static final int BAR_HEIGHT     = 22;
    private static final int TEXT_COLOR     = 0xFFFFFFFF;
    private static final int BUTTON_WIDTH   = 90;
    private static final int BUTTON_HEIGHT  = 14;

    private final String message;
    private final String buttonLabel;
    private final HandledScreen<?> parentHandledScreen;
    private final Runnable onConfirm;

    private ButtonWidget confirmButton;

    public SoftCloseScreen(String message, String buttonLabel,
                           HandledScreen<?> parentHandledScreen, Runnable onConfirm) {
        super(Text.literal("SoftClose"));
        this.message             = message;
        this.buttonLabel         = buttonLabel;
        this.parentHandledScreen = parentHandledScreen;
        this.onConfirm           = onConfirm;
    }



    /**
     * [x, y, width] полосы над GUI контейнера.
     * Стандартный контейнер 176px шириной, верх GUI ≈ (height-166)/2.
     */
    private int[] barRect() {
        int guiWidth = 200;
        int barX     = (this.width - guiWidth) / 2;
        int guiTop   = (this.height - 166) / 2;
        int barY     = guiTop - BAR_HEIGHT - 2;
        if (barY < 2) barY = 2;
        return new int[]{barX, barY, guiWidth};
    }
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим, контейнер, как обычно — никакого затемнения
        parentHandledScreen.render(context, mouseX, mouseY, delta);
        int[] bar    = barRect();
        int barX     = bar[0];
        int barY     = bar[1];
        int textY = barY + (BAR_HEIGHT - 50) / 2;

        context.drawText(this.textRenderer, Text.literal(message),barX + 14, textY, TEXT_COLOR, false);
        super.render(context, mouseX, mouseY, delta);
    }
    @Override
    protected void init() {
        int[] bar = barRect();
        int bx = bar[0] + bar[2] - BUTTON_WIDTH - 14;
        int barY     = bar[1];
        //int textY = barY + (BAR_HEIGHT - 50) / 2;
        int by = barY + ((BAR_HEIGHT - 50) / 2) - 17;

        confirmButton = ButtonWidget.builder(Text.literal(buttonLabel), btn -> onConfirm.run())
                .dimensions(bx, by, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        this.addDrawableChild(confirmButton);
    }
    @Override public boolean shouldPause()        { return false; }
    @Override public boolean shouldCloseOnEsc()   { return false; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (confirmButton.isMouseOver(mouseX, mouseY))
            return super.mouseClicked(mouseX, mouseY, button);
        return parentHandledScreen.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return parentHandledScreen.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return parentHandledScreen.mouseReleased(mouseX, mouseY, button);
    }

    public HandledScreen<?> getParentHandledScreen() {
        return parentHandledScreen;
    }
}
