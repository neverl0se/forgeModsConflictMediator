package com.conflictmediator.core;

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
 * Система отключения Mixin
 */
public class MixinDisabler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path BLACKLIST_FILE = FMLPaths.CONFIGDIR.get().resolve("conflict_mediator_blacklist.json");
    
    private final Set<String> disabledMixins = new HashSet<>();
    private final Map<String, Set<String>> disabledMixinsByMod = new HashMap<>();
    
    /**
     * Отключение Mixin класса
     */
    public void disableMixin(String mixinClass) {
        if (mixinClass == null || mixinClass.isEmpty()) return;
        
        disabledMixins.add(mixinClass);
        
        // Извлекаем modid из имени класса
        String modid = extractModidFromMixin(mixinClass);
        if (modid != null) {
            disabledMixinsByMod.computeIfAbsent(modid, k -> new HashSet<>()).add(mixinClass);
        }
        
        LOGGER.info("Mixin отключен: {}", mixinClass);
        
        // Примечание: прямое отключение Mixin после загрузки сложно
        // Это должно быть сделано до применения Mixin через конфигурацию
        // Отключенные Mixin будут пропущены при следующем запуске
        LOGGER.info("Mixin {} помечен для отключения при следующем запуске", mixinClass);
    }
    
    /**
     * Проверка, отключен ли Mixin
     */
    public boolean isMixinDisabled(String mixinClass) {
        return disabledMixins.contains(mixinClass);
    }
    
    /**
     * Загрузка черного списка из файла
     */
    public void loadBlacklist() {
        if (!Files.exists(BLACKLIST_FILE)) {
            LOGGER.debug("Файл черного списка не найден: {}", BLACKLIST_FILE);
            return;
        }
        
        try {
            String json = Files.readString(BLACKLIST_FILE);
            Map<String, List<String>> data = GSON.fromJson(json, 
                new TypeToken<Map<String, List<String>>>(){}.getType());
            
            if (data != null && data.containsKey("disabled_mixins")) {
                List<String> mixins = data.get("disabled_mixins");
                for (String mixin : mixins) {
                    disabledMixins.add(mixin);
                }
                LOGGER.info("Загружено {} отключенных Mixin из черного списка", mixins.size());
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка при загрузке черного списка", e);
        }
    }
    
    /**
     * Сохранение черного списка в файл
     */
    public void saveBlacklist() {
        Map<String, Object> data = new HashMap<>();
        data.put("disabled_mixins", new ArrayList<>(disabledMixins));
        data.put("disabled_by_mod", disabledMixinsByMod);
        
        try {
            Files.createDirectories(BLACKLIST_FILE.getParent());
            Files.writeString(BLACKLIST_FILE, GSON.toJson(data));
            LOGGER.info("Черный список сохранен: {}", BLACKLIST_FILE);
        } catch (IOException e) {
            LOGGER.error("Ошибка при сохранении черного списка", e);
        }
    }
    
    /**
     * Извлечение modid из имени Mixin класса
     */
    private String extractModidFromMixin(String mixinClass) {
        // Формат: modid.mixin.ClassName
        int firstDot = mixinClass.indexOf('.');
        if (firstDot > 0) {
            return mixinClass.substring(0, firstDot);
        }
        return null;
    }
    
    public Set<String> getDisabledMixins() {
        return Collections.unmodifiableSet(disabledMixins);
    }
}
