package pl.twojnick.echomysliwego;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EchoMysliwego extends JavaPlugin implements Listener, CommandExecutor {

    private final String ITEM_NAME = ChatColor.GOLD + "Echo Myśliwego";
    private final Map<UUID, UUID> trackedTargets = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("echogive").setExecutor(this);
        getLogger().info("Echo Myśliwego zostało włączone pomyślnie!");
    }

    @Override
    public void onDisable() {
        trackedTargets.clear();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player hunter = (Player) event.getDamager();
        Entity victim = event.getEntity();

        ItemStack handItem = hunter.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR || !handItem.hasItemMeta()) return;
        
        ItemMeta meta = handItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.getDisplayName().equals(ITEM_NAME)) return;

        UUID victimUUID = victim.getUniqueId();

        if (!trackedTargets.containsKey(victimUUID)) {
            trackedTargets.put(victimUUID, hunter.getUniqueId());

            hunter.sendMessage(ChatColor.RED + "Zraniłeś cel! " + ChatColor.GOLD + "Echo Myśliwego " + ChatColor.GRAY + "zaczyna działać. Widzisz smugę dymu przez 1 minutę.");
            if (victim instanceof Player) {
                ((Player) victim).sendMessage(ChatColor.DARK_RED + "Jesteś śledzony! " + ChatColor.GRAY + "Zostawiasz za sobą smugę dymu...");
            }

            new BukkitRunnable() {
                int elapsedTicks = 0;
                final int maxTicks = 20 * 60; // 1 minuta

                @Override
                public void run() {
                    if (victim.isDead() || !victim.isValid() || elapsedTicks >= maxTicks || !trackedTargets.containsKey(victimUUID)) {
                        trackedTargets.remove(victimUUID);
                        if (hunter.isOnline()) {
                            hunter.sendMessage(ChatColor.GRAY + "Efekt " + ChatColor.GOLD + "Echa Myśliwego " + ChatColor.GRAY + "na tym celu dobiegł końca.");
                        }
                        if (victim instanceof Player && ((Player) victim).isOnline()) {
                            ((Player) victim).sendMessage(ChatColor.GREEN + "Nie jesteś już śledzony.");
                        }
                        this.cancel();
                        return;
                    }

                    if (hunter.isOnline()) {
                        // Zmieniono na CAMPFIRE_COSY_SMOKE, który działa w nowym API
                        hunter.spawnParticle(
                                Particle.CAMPFIRE_COSY_SMOKE, 
                                victim.getLocation().add(0, 0.5, 0), 
                                4,             // Zmniejszona ilość, bo ten dym jest większy i ładniejszy
                                0.1, 0.1, 0.1, 
                                0.01
                        );
                    }

                    elapsedTicks += 5;
                }
            }.runTaskTimer(this, 0L, 5L);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ta komenda może być użyta tylko przez gracza!");
            return true;
        }

        Player player = (Player) sender;
        ItemStack echoItem = new ItemStack(Material.COMPASS);
        ItemMeta meta = echoItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ITEM_NAME);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Uderz wroga tym przedmiotem,",
                    ChatColor.GRAY + "aby widzieć smugę dymu,",
                    ChatColor.GRAY + "gdy zacznie uciekać."
            ));
            echoItem.setItemMeta(meta);
        }

        player.getInventory().addItem(echoItem);
        player.sendMessage(ChatColor.GREEN + "Otrzymałeś " + ChatColor.GOLD + "Echo Myśliwego" + ChatColor.GREEN + "!");
        return true;
    }
}
