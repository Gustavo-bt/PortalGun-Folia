package pl.by.fentisdev.portalgun;

import co.aikar.commands.BukkitCommandManager;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pl.by.fentisdev.portalgun.cmd.PortalCommands;
import pl.by.fentisdev.portalgun.listeners.GriefPreventionListeners;
import pl.by.fentisdev.portalgun.listeners.PortalListeners;
import pl.by.fentisdev.portalgun.listeners.WorldGuardListeners;
import pl.by.fentisdev.portalgun.portalgun.PortalGun;
import pl.by.fentisdev.portalgun.portalgun.PortalGunManager;
import pl.by.fentisdev.portalgun.portalgun.PortalModel;
import pl.by.fentisdev.portalgun.utils.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PortalGunMain extends JavaPlugin {

    @Getter
    private static PortalGunMain instance;
    private BukkitCommandManager commandManager;

    @Override
    public void onLoad() {
        instance = this;
        registryWorldGuardFlags();
    }

    @Override
    public void onEnable() {
        HoldingFile.getInstance().readHoldings();
        Bukkit.getPluginManager().registerEvents(new PortalListeners(),this);
        //getCommand("portalgun").setExecutor(new PortalCMD());
        registerCommands();
        PortalConfig.getInstance().createConfig();
        registryCraft();

        registryNbtApi();
        registryWorldGuard();
        registryGriefPrevention();
        PortalGunManager.getInstance().registryPortals();
        SchedulerUtil.runRepeatingGlobal(this, () -> {
            PortalGunManager.getInstance().savePortals();
            HoldingFile.getInstance().saveHoldings();
        }, 40, 40);
        PortalGunManager.getInstance().startPortalScheduler();
        Metrics metrics = new Metrics(this,15397);
        updateChecker();
    }

    @Override
    public void onDisable() {
        PortalGunManager.getInstance().savePortals();
        PortalGunManager.getInstance().stopPortalScheduler();
        for (PortalGun pg : PortalGunManager.getInstance().getPortalGuns()){
            pg.resetPortals();
        }
        for (Entity entity : PortalGunManager.getInstance().getHolding().values()) {
            if (entity!=null){
                entity.setGravity(true);
            }
        }
    }
    public void registerCommands(){
        commandManager = new BukkitCommandManager(this);
        commandManager.getCommandCompletions().registerAsyncCompletion("portalmodels", c ->
                Arrays.stream(PortalModel.values()).map(Objects::toString).collect(Collectors.toList()));
        commandManager.getCommandCompletions().registerAsyncCompletion("blockwhitelist", c ->
                PortalConfig.getInstance().getWhiteListBlocks().stream().map(Objects::toString).collect(Collectors.toList()));
        commandManager.registerCommand(new PortalCommands());
    }


    public void registryWorldGuardFlags(){
        if ((Bukkit.getPluginManager().getPlugin("WorldGuard"))!=null){
            PortalWorldGuard.getInstance().load();
        }
    }

    private void registryNbtApi(){
        Plugin plugin;
        if ((plugin=Bukkit.getPluginManager().getPlugin("NBTAPI"))!=null&&plugin.isEnabled()){
            Bukkit.getConsoleSender().sendMessage("Ãƒâ€šÃ‚Â§6[PortalGun] Ãƒâ€šÃ‚Â§eNBT-API found!");
            PortalUtils.getInstance().setNbtApi(true);
        }else{
            Bukkit.getConsoleSender().sendMessage("Ãƒâ€šÃ‚Â§6[PortalGun] Ãƒâ€šÃ‚Â§eNBT-API not found! It's ok! This is only necessary if this server has already run previous versions of PortalGun 2.0.0-SNAPSHOT");
        }
    }

    private void registryWorldGuard(){
        Plugin plugin;
        if ((plugin=Bukkit.getPluginManager().getPlugin("WorldGuard"))!=null&&plugin.isEnabled()){
            Bukkit.getConsoleSender().sendMessage("Ãƒâ€šÃ‚Â§6[PortalGun] Ãƒâ€šÃ‚Â§eWorldGuard found!");
            Bukkit.getPluginManager().registerEvents(new WorldGuardListeners(),this);
        }
    }

    private void registryGriefPrevention(){
        Plugin plugin;
        if ((plugin=Bukkit.getPluginManager().getPlugin("GriefPrevention"))!=null&&plugin.isEnabled()){
            Bukkit.getConsoleSender().sendMessage("Ãƒâ€šÃ‚Â§6[PortalGun] Ãƒâ€šÃ‚Â§eGriefPrevention found!");
            Bukkit.getPluginManager().registerEvents(new GriefPreventionListeners(),this);
        }
    }

    private void registryCraft(){
        if (getConfig().getBoolean("PortalCraftable")){
            Bukkit.getConsoleSender().sendMessage("Ãƒâ€šÃ‚Â§6[PortalGun] Ãƒâ€šÃ‚Â§eRegistering PortalGun's Crafts");
            for (PortalModel portalModel : PortalModel.values()) {
                if (getConfig().getBoolean("PortalGunCrafts."+ portalModel.toString().toLowerCase()+".Craft")){
                    registryPortalRecipe(portalModel);
                }
            }
        }
    }

    private void registryPortalRecipe(PortalModel po){
        Bukkit.getConsoleSender().sendMessage("Ãƒâ€šÃ‚Â§6[PortalGun] Ãƒâ€šÃ‚Â§7Adding recipe for Ãƒâ€šÃ‚Â§e"+po.getName().replace("Ãƒâ€šÃ‚Â§f",""));
        List<String> shape = getConfig().getStringList("PortalGunCrafts."+po.toString().toLowerCase()+".Shape");
        List<String> ingredients = getConfig().getStringList("PortalGunCrafts."+po.toString().toLowerCase()+".Ingredients");
        RecipeCreator rc = new RecipeCreator("portalgun_"+po.toString().toLowerCase(),po.createItem());
        rc.addShape(shape.toArray(new String[0]));
        for (String ingredient : ingredients) {
            String[] split = ingredient.split(":");
            char c = split[0].toCharArray()[0];
            Material m = Material.getMaterial(split[1]);
            rc.setIngredient(c,m);
        }
        rc.addRecipe();
    }

    private void updateChecker(){
        new UpdateChecker(this, 96641).getVersion(version -> {
            Bukkit.getConsoleSender().sendMessage("Ãƒâ€šÃ‚Â§6[PortalGun] Ãƒâ€šÃ‚Â§eChecking for updates...");
            if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
                Bukkit.getConsoleSender().sendMessage("Ãƒâ€šÃ‚Â§6[PortalGun] Ãƒâ€šÃ‚Â§aThe plugin is already in the most updated version!");
            } else {
                Bukkit.getConsoleSender().sendMessage("Ãƒâ€šÃ‚Â§6[PortalGun] Ãƒâ€šÃ‚Â§cIts current version is Ãƒâ€šÃ‚Â§e"+this.getDescription().getVersion()+" Ãƒâ€šÃ‚Â§cA new version is available! Download version Ãƒâ€šÃ‚Â§a"+version+"Ãƒâ€šÃ‚Â§c at Ãƒâ€šÃ‚Â§bhttps://www.spigotmc.org/resources/portal-gun.96641/");
            }
        });
    }
}
