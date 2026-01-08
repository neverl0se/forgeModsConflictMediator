package com.conflictmediator.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для пометки отключаемых модулей мода
 * 
 * Использование:
 * <pre>
 * {@code @ConflictModule(identifier = "advanced_ai", description = "Advanced AI features")}
 * public class AdvancedAIModule {
 *     // ...
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConflictModule {
    /**
     * Уникальный идентификатор модуля
     */
    String identifier();
    
    /**
     * Описание модуля (опционально)
     */
    String description() default "";
    
    /**
     * Мод, которому принадлежит модуль (опционально, по умолчанию берется из @Mod)
     */
    String modid() default "";
}
