package com.conflictmediator;

import com.conflictmediator.api.ConflictModuleRegistry;
import com.conflictmediator.core.ErrorInterceptor;
import com.conflictmediator.core.MixinDisabler;
import com.conflictmediator.gui.ConflictResolutionScreen;
import com.forgemodcomprfc.api.ModificationRegistry;
import com.forgemodcomprfc.api.events.ConflictDetectedEvent;
import com.forgemodcomprfc.api.ConflictResolver.ConflictResolution;
import com.forgemodcomprfc.api.ConflictResolver.ResolutionStrategy;
import com.forgemodcomprfc.manifest.ModificationManifest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;

/**
 * Главный класс мода-медиатора конфликтов
 * Уровень совместимости: GOLD
 */
@Mod(ConflictMediator.MOD_ID)
public class ConflictMediator {
    public static final String MOD_ID = "conflictmediator";
    public static final Logger LOGGER = LogManager.getLogger();
    
    private static ConflictMediator instance;
    private final ErrorInterceptor errorInterceptor;
    private final MixinDisabler mixinDisabler;
    private final ConflictModuleRegistry moduleRegistry;
    
    public ConflictMediator() {
        instance = this;
        
        LOGGER.info("Инициализация Conflict Mediator Mod...");
        
        // Инициализация компонентов
        this.mixinDisabler = new MixinDisabler();
        this.moduleRegistry = new ConflictModuleRegistry();
        this.errorInterceptor = new ErrorInterceptor(this);
        
        // Регистрация обработчиков ошибок
        errorInterceptor.register();
        
        // Регистрация событий
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("Conflict Mediator Mod загружен");
    }
    
    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Загружаем манифест мода-медиатора (уровень GOLD)
            try {
                InputStream manifestStream = getClass().getClassLoader()
                    .getResourceAsStream("META-INF/modification_manifest.json");
                
                if (manifestStream != null) {
                    ModificationManifest manifest = ModificationManifest.fromJson(manifestStream);
                    ModificationRegistry.INSTANCE.registerManifest(manifest);
                    LOGGER.info("Манифест мода-медиатора зарегистрирован (уровень GOLD)");
                } else {
                    LOGGER.warn("Манифест не найден для conflictmediator");
                }
            } catch (Exception e) {
                LOGGER.error("Ошибка при загрузке манифеста", e);
            }
            
            // Загружаем сохраненные конфигурации отключений
            mixinDisabler.loadBlacklist();
            moduleRegistry.loadDisabledModules();
            
            LOGGER.info("Конфигурации медиатора загружены");
        });
    }
    
    /**
     * Обработка конфликтов через forgeModCompRFC API (уровень GOLD)
     */
    @SubscribeEvent
    public void onConflictDetected(ConflictDetectedEvent event) {
        var conflict = event.getConflict();
        LOGGER.warn("Обнаружен конфликт через forgeModCompRFC API: {}", conflict);
        
        // Мод-медиатор может программно разрешить конфликт
        // Здесь можно добавить логику автоматического разрешения
        // Если не установлено разрешение, будет показан GUI
    }
    
    public static ConflictMediator getInstance() {
        return instance;
    }
    
    public ErrorInterceptor getErrorInterceptor() {
        return errorInterceptor;
    }
    
    public MixinDisabler getMixinDisabler() {
        return mixinDisabler;
    }
    
    public ConflictModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }
}
