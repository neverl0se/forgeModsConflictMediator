package com.conflictmediator.core;

import com.conflictmediator.ConflictMediator;
import com.conflictmediator.analysis.ConflictAnalyzer;
import com.conflictmediator.analysis.DetectedConflict;
import com.conflictmediator.gui.ConflictResolutionScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// MixinApplyError может быть недоступен, используем общий Throwable

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Перехватчик ошибок загрузки модов
 */
public class ErrorInterceptor {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ConflictMediator mediator;
    private final ConflictAnalyzer analyzer;
    private boolean guiInitialized = false;
    
    public ErrorInterceptor(ConflictMediator mediator) {
        this.mediator = mediator;
        this.analyzer = new ConflictAnalyzer();
    }
    
    /**
     * Регистрация обработчиков ошибок
     */
    public void register() {
        // Регистрация глобального обработчика необработанных исключений
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            handleError(throwable, "Uncaught exception in thread: " + thread.getName());
        });
        
        // Регистрация обработчика для Mixin ошибок
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    /**
     * Обработка ошибки
     */
    public void handleError(Throwable error, String context) {
        if (error == null) return;
        
        LOGGER.error("Обнаружена ошибка загрузки: {}", context, error);
        
        // Анализ конфликта
        List<DetectedConflict> conflicts = analyzer.analyzeError(error);
        
        if (!conflicts.isEmpty()) {
            LOGGER.warn("Обнаружено {} потенциальных конфликтов", conflicts.size());
            
            // Пытаемся показать GUI для разрешения конфликта
            showConflictResolutionGUI(conflicts, error);
        } else {
            // Если не удалось определить конфликт, логируем и продолжаем
            LOGGER.warn("Не удалось определить конфликт из ошибки");
        }
    }
    
    /**
     * Показ GUI для разрешения конфликта
     */
    private void showConflictResolutionGUI(List<DetectedConflict> conflicts, Throwable error) {
        // Проверяем, можем ли мы показать GUI
        if (!FMLLoader.isProduction() && Minecraft.getInstance() != null) {
            try {
                Minecraft.getInstance().execute(() -> {
                    ConflictResolutionScreen screen = new ConflictResolutionScreen(
                        conflicts, 
                        error,
                        () -> {
                            // Callback после применения решений
                            LOGGER.info("Конфликты разрешены, перезагрузка...");
                            // Перезагрузка невозможна без перезапуска JVM
                            // Показываем сообщение пользователю
                        }
                    );
                    Minecraft.getInstance().setScreen(screen);
                });
            } catch (Exception e) {
                LOGGER.error("Не удалось показать GUI разрешения конфликтов", e);
            }
        } else {
            // В production или на сервере - логируем и сохраняем информацию
            LOGGER.error("Конфликт обнаружен, но GUI недоступен. Информация сохранена в лог.");
            saveConflictInfo(conflicts, error);
        }
    }
    
    /**
     * Сохранение информации о конфликте
     */
    private void saveConflictInfo(List<DetectedConflict> conflicts, Throwable error) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);
        
        LOGGER.error("=== КОНФЛИКТ ОБНАРУЖЕН ===");
        LOGGER.error("Стек трейс:\n{}", sw.toString());
        for (DetectedConflict conflict : conflicts) {
            LOGGER.error("Конфликт: {}", conflict);
        }
        LOGGER.error("=== КОНЕЦ ИНФОРМАЦИИ О КОНФЛИКТЕ ===");
    }
    
    /**
     * Перехват ошибок Mixin
     * Примечание: MixinApplyError может быть недоступен, поэтому перехватываем через общий обработчик
     */
    // Обработка Mixin ошибок происходит через общий handleError
}
