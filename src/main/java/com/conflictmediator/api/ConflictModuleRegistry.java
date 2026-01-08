package com.conflictmediator.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Реестр отключаемых модулей модов
 */
public class ConflictModuleRegistry {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DISABLED_MODULES_FILE = FMLPaths.CONFIGDIR.get().resolve("conflict_mediator_disabled_modules.json");
    
    private final Map<String, Set<String>> registeredModules = new HashMap<>(); // modid -> set of module identifiers
    private final Map<String, Set<String>> disabledModules = new HashMap<>(); // modid -> set of disabled module identifiers
    
    /**
     * Регистрация модуля мода
     */
    public void registerModule(String modid, String moduleIdentifier) {
        registeredModules.computeIfAbsent(modid, k -> new HashSet<>()).add(moduleIdentifier);
        LOGGER.debug("Зарегистрирован модуль {} для мода {}", moduleIdentifier, modid);
    }
    
    /**
     * Отключение модуля
     */
    public void disableModule(String modid, String moduleIdentifier) {
        disabledModules.computeIfAbsent(modid, k -> new HashSet<>()).add(moduleIdentifier);
        LOGGER.info("Модуль {} мода {} отключен", moduleIdentifier, modid);
    }
    
    /**
     * Проверка, отключен ли модуль
     */
    public boolean isModuleDisabled(String modid, String moduleIdentifier) {
        return disabledModules.getOrDefault(modid, Collections.emptySet()).contains(moduleIdentifier);
    }
    
    /**
     * Загрузка отключенных модулей из файла
     */
    public void loadDisabledModules() {
        if (!Files.exists(DISABLED_MODULES_FILE)) {
            LOGGER.debug("Файл отключенных модулей не найден: {}", DISABLED_MODULES_FILE);
            return;
        }
        
        try {
            String json = Files.readString(DISABLED_MODULES_FILE);
            Map<String, List<String>> data = GSON.fromJson(json,
                new TypeToken<Map<String, List<String>>>(){}.getType());
            
            if (data != null) {
                for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                    disabledModules.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
                LOGGER.info("Загружено {} модов с отключенными модулями", data.size());
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка при загрузке отключенных модулей", e);
        }
    }
    
    /**
     * Сохранение отключенных модулей в файл
     */
    public void saveDisabledModules() {
        Map<String, List<String>> data = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : disabledModules.entrySet()) {
            data.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        
        try {
            Files.createDirectories(DISABLED_MODULES_FILE.getParent());
            Files.writeString(DISABLED_MODULES_FILE, GSON.toJson(data));
            LOGGER.info("Отключенные модули сохранены: {}", DISABLED_MODULES_FILE);
        } catch (IOException e) {
            LOGGER.error("Ошибка при сохранении отключенных модулей", e);
        }
    }
    
    public Map<String, Set<String>> getRegisteredModules() {
        return Collections.unmodifiableMap(registeredModules);
    }
    
    public Map<String, Set<String>> getDisabledModules() {
        return Collections.unmodifiableMap(disabledModules);
    }
}
