package com.conflictmediator.analysis;

/**
 * Обнаруженный конфликт
 */
public class DetectedConflict {
    public enum ConflictType {
        MIXIN,          // Конфликт Mixin
        METHOD,         // Конфликт методов
        FIELD,          // Конфликт полей
        REGISTRY,       // Конфликт реестра
        CAPABILITY,     // Конфликт Capability
        UNKNOWN         // Неизвестный тип конфликта
    }
    
    private final ConflictType type;
    private final String mod1;
    private final String mod2;
    private final String description;
    private final String errorMessage;
    private String mixinClass1;
    private String mixinClass2;
    
    public DetectedConflict(ConflictType type, String mod1, String mod2, 
                           String description, String errorMessage) {
        this.type = type;
        this.mod1 = mod1;
        this.mod2 = mod2;
        this.description = description;
        this.errorMessage = errorMessage;
    }
    
    public ConflictType getType() {
        return type;
    }
    
    public String getMod1() {
        return mod1;
    }
    
    public String getMod2() {
        return mod2;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getMixinClass1() {
        return mixinClass1;
    }
    
    public void setMixinClass1(String mixinClass1) {
        this.mixinClass1 = mixinClass1;
    }
    
    public String getMixinClass2() {
        return mixinClass2;
    }
    
    public void setMixinClass2(String mixinClass2) {
        this.mixinClass2 = mixinClass2;
    }
    
    @Override
    public String toString() {
        return String.format("Conflict[%s] между %s и %s: %s", 
                           type, mod1, mod2, description);
    }
}
