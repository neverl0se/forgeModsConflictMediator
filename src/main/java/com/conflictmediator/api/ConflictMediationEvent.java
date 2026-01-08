package com.conflictmediator.api;

import com.conflictmediator.analysis.DetectedConflict;
import net.minecraftforge.eventbus.api.Event;

/**
 * Событие медиации конфликта
 * Позволяет другим модам реагировать на процесс медиации
 */
public class ConflictMediationEvent extends Event {
    private final DetectedConflict conflict;
    private boolean handled = false;
    private String resolution = null;
    
    public ConflictMediationEvent(DetectedConflict conflict) {
        this.conflict = conflict;
    }
    
    /**
     * Получает обнаруженный конфликт
     */
    public DetectedConflict getConflict() {
        return conflict;
    }
    
    /**
     * Помечает конфликт как обработанный
     */
    public void setHandled(boolean handled) {
        this.handled = handled;
    }
    
    /**
     * Проверяет, обработан ли конфликт
     */
    public boolean isHandled() {
        return handled;
    }
    
    /**
     * Устанавливает резолюцию конфликта
     */
    public void setResolution(String resolution) {
        this.resolution = resolution;
        this.handled = true;
    }
    
    /**
     * Получает резолюцию конфликта
     */
    public String getResolution() {
        return resolution;
    }
}
