package com.conflictmediator.analysis;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Анализатор конфликтов из стектрейсов и ошибок
 */
public class ConflictAnalyzer {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Паттерны для поиска модов в стектрейсе
    private static final Pattern MODID_PATTERN = Pattern.compile(
        "([a-z][a-z0-9_]{2,})", // Простой паттерн для modid
        Pattern.CASE_INSENSITIVE
    );
    
    // Паттерны для Mixin классов
    private static final Pattern MIXIN_PATTERN = Pattern.compile(
        "([a-z][a-z0-9_]+)\\.mixin\\.[A-Z][a-zA-Z0-9_]*",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Анализ ошибки и поиск конфликтов
     */
    public List<DetectedConflict> analyzeError(Throwable error) {
        List<DetectedConflict> conflicts = new ArrayList<>();
        
        if (error == null) return conflicts;
        
        // Получаем стектрейс
        StackTraceElement[] stackTrace = error.getStackTrace();
        String errorMessage = error.getMessage();
        
        // Анализируем сообщение об ошибке
        if (errorMessage != null) {
            conflicts.addAll(analyzeErrorMessage(errorMessage, stackTrace));
        }
        
        // Анализируем стектрейс
        conflicts.addAll(analyzeStackTrace(stackTrace));
        
        // Анализируем причину (cause)
        if (error.getCause() != null) {
            conflicts.addAll(analyzeError(error.getCause()));
        }
        
        return conflicts;
    }
    
    /**
     * Анализ сообщения об ошибке
     */
    private List<DetectedConflict> analyzeErrorMessage(String message, StackTraceElement[] stackTrace) {
        List<DetectedConflict> conflicts = new ArrayList<>();
        
        // Поиск упоминаний Mixin
        if (message.contains("Mixin") || message.contains("mixin")) {
            Matcher mixinMatcher = MIXIN_PATTERN.matcher(message);
            List<String> foundMixins = new ArrayList<>();
            
            while (mixinMatcher.find()) {
                String mixinClass = mixinMatcher.group(0);
                String modid = extractModidFromMixin(mixinClass);
                if (modid != null) {
                    foundMixins.add(mixinClass);
                }
            }
            
            if (foundMixins.size() >= 2) {
                conflicts.add(new DetectedConflict(
                    DetectedConflict.ConflictType.MIXIN,
                    foundMixins.get(0),
                    foundMixins.get(1),
                    "Конфликт Mixin: " + String.join(" и ", foundMixins),
                    message
                ));
            }
        }
        
        // Поиск упоминаний дублирующихся методов
        if (message.contains("duplicate") || message.contains("already exists")) {
            conflicts.add(new DetectedConflict(
                DetectedConflict.ConflictType.METHOD,
                extractModidFromStackTrace(stackTrace, 0),
                extractModidFromStackTrace(stackTrace, 1),
                "Дублирующийся метод или поле",
                message
            ));
        }
        
        return conflicts;
    }
    
    /**
     * Анализ стектрейса
     */
    private List<DetectedConflict> analyzeStackTrace(StackTraceElement[] stackTrace) {
        List<DetectedConflict> conflicts = new ArrayList<>();
        List<String> foundMods = new ArrayList<>();
        
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            
            // Ищем modid в имени класса
            String modid = extractModidFromClassName(className);
            if (modid != null && !foundMods.contains(modid)) {
                foundMods.add(modid);
            }
            
            // Ищем Mixin классы
            if (className.contains(".mixin.")) {
                String mixinModid = extractModidFromMixin(className);
                if (mixinModid != null && !foundMods.contains(mixinModid)) {
                    foundMods.add(mixinModid);
                }
            }
        }
        
        // Если найдено 2+ мода в стектрейсе, возможно это конфликт
        if (foundMods.size() >= 2) {
            conflicts.add(new DetectedConflict(
                DetectedConflict.ConflictType.UNKNOWN,
                foundMods.get(0),
                foundMods.get(1),
                "Потенциальный конфликт между модами: " + String.join(" и ", foundMods),
                "Обнаружено в стектрейсе"
            ));
        }
        
        return conflicts;
    }
    
    /**
     * Извлечение modid из имени класса Mixin
     */
    private String extractModidFromMixin(String className) {
        Matcher matcher = MIXIN_PATTERN.matcher(className);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Извлечение modid из имени класса
     */
    private String extractModidFromClassName(String className) {
        // Проверяем, есть ли этот мод в списке загруженных
        for (String modid : ModList.get().getMods().stream()
            .map(mod -> mod.getModId())
            .toList()) {
            
            if (className.contains(modid)) {
                return modid;
            }
        }
        return null;
    }
    
    /**
     * Извлечение modid из элемента стектрейса
     */
    private String extractModidFromStackTrace(StackTraceElement[] stackTrace, int index) {
        if (index >= 0 && index < stackTrace.length) {
            return extractModidFromClassName(stackTrace[index].getClassName());
        }
        return "unknown";
    }
}
