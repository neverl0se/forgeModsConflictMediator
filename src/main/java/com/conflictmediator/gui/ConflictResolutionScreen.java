package com.conflictmediator.gui;

import com.conflictmediator.ConflictMediator;
import com.conflictmediator.analysis.DetectedConflict;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * GUI экран для разрешения конфликтов
 */
public class ConflictResolutionScreen extends Screen {
    private final List<DetectedConflict> conflicts;
    private final Throwable error;
    private final Runnable onResolved;
    
    private final List<ConflictOption> options = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 8;
    
    public ConflictResolutionScreen(List<DetectedConflict> conflicts, Throwable error, Runnable onResolved) {
        super(Component.literal("Разрешение конфликтов модов"));
        this.conflicts = conflicts;
        this.error = error;
        this.onResolved = onResolved;
        
        // Создаем опции для каждого конфликта
        for (DetectedConflict conflict : conflicts) {
            createOptionsForConflict(conflict);
        }
    }
    
    private void createOptionsForConflict(DetectedConflict conflict) {
        // Опция 1: Отключить Mixin из первого мода
        if (conflict.getMixinClass1() != null) {
            options.add(new ConflictOption(
                "Отключить Mixin " + conflict.getMixinClass1() + " из мода " + conflict.getMod1(),
                () -> ConflictMediator.getInstance().getMixinDisabler().disableMixin(conflict.getMixinClass1())
            ));
        }
        
        // Опция 2: Отключить Mixin из второго мода
        if (conflict.getMixinClass2() != null) {
            options.add(new ConflictOption(
                "Отключить Mixin " + conflict.getMixinClass2() + " из мода " + conflict.getMod2(),
                () -> ConflictMediator.getInstance().getMixinDisabler().disableMixin(conflict.getMixinClass2())
            ));
        }
        
        // Опция 3: Отключить модуль из первого мода (если поддерживается)
        options.add(new ConflictOption(
            "Отключить конфликтующий функционал в моде " + conflict.getMod1(),
            () -> ConflictMediator.getInstance().getModuleRegistry().disableModule(conflict.getMod1(), "conflict_resolution")
        ));
        
        // Опция 4: Отключить модуль из второго мода
        options.add(new ConflictOption(
            "Отключить конфликтующий функционал в моде " + conflict.getMod2(),
            () -> ConflictMediator.getInstance().getModuleRegistry().disableModule(conflict.getMod2(), "conflict_resolution")
        ));
    }
    
    @Override
    protected void init() {
        super.init();
        
        int y = 50;
        int checkboxWidth = 300;
        int checkboxHeight = 20;
        int spacing = 25;
        
        // Заголовок
        addRenderableWidget(Button.builder(
            Component.literal("Обнаружены конфликты модов!"),
            button -> {}
        ).bounds(width / 2 - 150, 20, 300, 20).build());
        
        // Чекбоксы для опций
        for (int i = scrollOffset; i < Math.min(options.size(), scrollOffset + ITEMS_PER_PAGE); i++) {
            ConflictOption option = options.get(i);
            Checkbox checkbox = Checkbox.builder(
                Component.literal(option.description),
                this.font
            ).pos(width / 2 - checkboxWidth / 2, y)
             .selected(option.selected)
             .onValueChange((checkboxWidget, selected) -> option.selected = selected)
             .build();
            
            addRenderableWidget(checkbox);
            y += spacing;
        }
        
        // Кнопки действий
        int buttonY = height - 50;
        addRenderableWidget(Button.builder(
            Component.literal("Применить и перезагрузить"),
            button -> applyAndReload()
        ).bounds(width / 2 - 200, buttonY, 150, 20).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Попробовать снова"),
            button -> tryAgain()
        ).bounds(width / 2 - 50, buttonY, 150, 20).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Выйти из игры"),
            button -> minecraft.stop()
        ).bounds(width / 2 + 100, buttonY, 100, 20).build());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // Заголовок
        guiGraphics.drawCenteredString(font, Component.literal("Разрешение конфликтов модов"), 
                          width / 2, 10, 0xFFFFFF);
        
        // Информация о конфликтах
        int y = 50;
        for (DetectedConflict conflict : conflicts) {
            guiGraphics.drawString(font, 
                      Component.literal("Конфликт: " + conflict.getDescription()),
                      width / 2 - 200, y, 0xFF5555);
            y += 15;
            guiGraphics.drawString(font,
                      Component.literal("Моды: " + conflict.getMod1() + " и " + conflict.getMod2()),
                      width / 2 - 200, y, 0xAAAAAA);
            y += 20;
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private void applyAndReload() {
        // Применяем выбранные опции
        for (ConflictOption option : options) {
            if (option.selected) {
                option.action.run();
            }
        }
        
        // Сохраняем конфигурацию
        ConflictMediator.getInstance().getMixinDisabler().saveBlacklist();
        ConflictMediator.getInstance().getModuleRegistry().saveDisabledModules();
        
        // Вызываем callback
        if (onResolved != null) {
            onResolved.run();
        }
        
        // Показываем сообщение о необходимости перезапуска
        minecraft.setScreen(new InfoScreen(
            Component.literal("Изменения применены"),
            Component.literal("Пожалуйста, перезапустите игру для применения изменений.")
        ));
    }
    
    private void tryAgain() {
        // Закрываем экран и пытаемся продолжить загрузку
        minecraft.setScreen(null);
    }
    
    private static class ConflictOption {
        final String description;
        final Runnable action;
        boolean selected = false;
        
        ConflictOption(String description, Runnable action) {
            this.description = description;
            this.action = action;
        }
    }
    
    private static class InfoScreen extends Screen {
        private final Component title;
        private final Component message;
        
        InfoScreen(Component title, Component message) {
            super(title);
            this.title = title;
            this.message = message;
        }
        
        @Override
        protected void init() {
            addRenderableWidget(Button.builder(
                Component.literal("OK"),
                button -> minecraft.stop()
            ).bounds(width / 2 - 50, height - 50, 100, 20).build());
        }
        
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.drawCenteredString(font, title, width / 2, 50, 0xFFFFFF);
            guiGraphics.drawCenteredString(font, message, width / 2, 80, 0xAAAAAA);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
