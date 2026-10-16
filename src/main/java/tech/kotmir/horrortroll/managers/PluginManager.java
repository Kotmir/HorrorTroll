package tech.kotmir.horrortroll.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import tech.kotmir.horrortroll.HorrorTroll;
import tech.kotmir.horrortroll.listeners.PlayerListener;
import tech.kotmir.horrortroll.utils.TrollCommand;
import tech.kotmir.horrortroll.utils.Utils;

import java.util.*;

public class PluginManager {

    private final HorrorTroll plugin;

    // Списки активных состояний игроков
    public final Set<UUID> frozenPlayers = new HashSet<>();
    public final Set<UUID> fakeOrePlayers = new HashSet<>();
    public final Map<UUID, UUID> adminTargetMap = new HashMap<>(); // Кто кого сейчас троллит через меню

    public static final String MENU_TITLE_PREFIX = Utils.color("&8[&4HorrorTroll&8] &0Жертва: &c");

    public PluginManager(HorrorTroll plugin) {
        this.plugin = plugin;
    }

    public void init() {
        TrollCommand commandExecutor = new TrollCommand(plugin);
        Objects.requireNonNull(plugin.getCommand("error")).setExecutor(commandExecutor);
        Objects.requireNonNull(plugin.getCommand("troll")).setExecutor(commandExecutor);
        Objects.requireNonNull(plugin.getCommand("troll")).setTabCompleter(commandExecutor);
        Objects.requireNonNull(plugin.getCommand("error")).setTabCompleter(commandExecutor);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(plugin), plugin);
    }

    public void cleanup() {
        frozenPlayers.clear();
        fakeOrePlayers.clear();
        adminTargetMap.clear();
    }

    // ==================== ОТКРЫТИЕ GUI МЕНЮ ====================
    public void openTrollMenu(Player admin, Player target) {
        adminTargetMap.put(admin.getUniqueId(), target.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 36, MENU_TITLE_PREFIX + target.getName());

        // Заполняем фон серыми стеклами
        ItemStack glass = Utils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, glass);
        }

        boolean isFrozen = frozenPlayers.contains(target.getUniqueId());
        boolean isFakeOre = fakeOrePlayers.contains(target.getUniqueId());

        // Ряд 1
        inv.setItem(10, Utils.createItem(Material.BARRIER, "&4&lФейк-Ошибка (Зависание + Кик)",
                "&7Замораживает игрока на 4.5 секунды,",
                "&7имитирует зависание клиента и выкидывает",
                "&7с пугающей Java-ошибкой.",
                "", "&eНажми для активации!"));

        inv.setItem(11, Utils.createItem(Material.CARVED_PUMPKIN, "&c&lХоррор Скример",
                "&7Накладывает кромешную тьму,",
                "&7спавнит частицы перед лицом и включает",
                "&7жуткие звуки Хранителя и Эндермена.",
                "", "&eНажми для активации!"));

        inv.setItem(12, Utils.createItem(Material.TNT, "&a&lФейк Крипер за спиной",
                "&7Проигрывает звук шипения крипера",
                "&7прямо за спиной жертвы и взрывается",
                "&7без урона блокам.",
                "", "&eНажми для активации!"));

        inv.setItem(13, Utils.createItem(Material.ENDER_EYE, "&5&lПаранойя (Разворот + Звуки)",
                "&7Создает звуки быстрых шагов позади",
                "&7и резко разворачивает камеру игрока",
                "&7на 180 градусов.",
                "", "&eНажми для активации!"));

        inv.setItem(14, Utils.createItem(Material.NETHER_STAR, "&6&lФейк OP",
                "&7Отправляет игроку сообщение о том,",
                "&7что сервер выдал ему права оператора.",
                "", "&eНажми для активации!"));

        inv.setItem(15, Utils.createItem(Material.DIAMOND_ORE, "&b&lМираж Руды: " + (isFakeOre ? "&aВКЛ" : "&cВЫКЛ"),
                "&7Когда игрок ломает алмазы или незерит,",
                "&7выпадает булыжник под смех ведьмы.",
                "", "&eНажми, чтобы переключить!"));

        inv.setItem(16, Utils.createItem(Material.DROPPER, "&e&lДырявые руки (Выброс предмета)",
                "&7Заставляет игрока выкинуть предмет,",
                "&7который он сейчас держит в главной руке.",
                "", "&eНажми для активации!"));

        // Ряд 2
        inv.setItem(19, Utils.createItem(Material.FERMENTED_SPIDER_EYE, "&d&lГаллюцинации и Тряска",
                "&7Дергает камеру игрока в разные стороны,",
                "&7накладывает тошноту и странные звуки.",
                "", "&eНажми для активации!"));

        inv.setItem(20, Utils.createItem(Material.COBWEB, "&f&lЛовушка Паука",
                "&7Спавнит временную паутину в ногах",
                "&7игрока на 4 секунды со звуками пауков.",
                "", "&eНажми для активации!"));

        inv.setItem(21, Utils.createItem(Material.WITHER_SKELETON_SKULL, "&8&lПроклятие Null / Herobrine",
                "&7Отправляет жертве жуткие глиitch-титры",
                "&7на весь экран и фейковые сообщения в чат.",
                "", "&eНажми для активации!"));

        inv.setItem(22, Utils.createItem(Material.ICE, "&3&lПолная Заморозка: " + (isFrozen ? "&aВКЛ" : "&cВЫКЛ"),
                "&7Полностью блокирует движение, прыжки,",
                "&7повороты головы и взаимодействие.",
                "", "&eНажми, чтобы переключить!"));

        inv.setItem(23, Utils.createItem(Material.ROTTEN_FLESH, "&4&lФейк Урон + Голод",
                "&7Имитирует получение удара (красный экран,",
                "&7звук боли) и сбрасывает голод до минимума.",
                "", "&eНажми для активации!"));

        inv.setItem(24, Utils.createItem(Material.LIGHTNING_ROD, "&b&lФейк Удар Молнии",
                "&7Бьет в игрока визуальной молнией",
                "&7(без настоящего урона и поджога территории).",
                "", "&eНажми для активации!"));

        inv.setItem(25, Utils.createItem(Material.MILK_BUCKET, "&a&lОчистить все эффекты",
                "&7Снимает с игрока заморозку, тьму",
                "&7и все активные пранки.",
                "", "&eНажми для сброса!"));

        admin.openInventory(inv);
    }

    // ==================== МЕХАНИКИ ПРАНКОВ ====================

    // 1. Зависание на 4.5 сек + Кик с ошибкой (Используется и в /error, и в меню)
    public void triggerFreezeAndError(Player admin, Player target, String customErrorText) {
        UUID uuid = target.getUniqueId();
        frozenPlayers.add(uuid);

        // Накладываем эффекты для имитации зависшего клиента
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 255, false, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 120, 250, false, false, false));

        // Каждые 5 тиков проигрываем прерывистый глитч-звук
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!target.isOnline() || ticks >= 90) {
                    this.cancel();
                    return;
                }
                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 0.1f);
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        if (admin != null) {
            admin.sendMessage(Utils.color("&8[&4HorrorTroll&8] &aУ игрока &e" + target.getName() + " &aзависла игра! Кик через 4.5 сек..."));
        }

        // Кик через 90 тиков (4.5 секунды)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            frozenPlayers.remove(uuid);
            if (target.isOnline()) {
                target.kickPlayer(Utils.color(customErrorText));
            }
        }, 90L);
    }

    // 2. Хоррор Скример
    public void triggerScreamer(Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 1, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 5, false, false));

        // Эффект стража/тотема и частицы перед глазами
        target.playEffect(EntityEffect.TOTEM_RESURRECT);
        Location eyeLoc = target.getEyeLocation().add(target.getLocation().getDirection().multiply(1.2));
        target.spawnParticle(Particle.ELDER_GUARDIAN, eyeLoc, 1);
        target.spawnParticle(Particle.SONIC_BOOM, eyeLoc, 3);
        target.spawnParticle(Particle.SQUID_INK, eyeLoc, 40, 0.5, 0.5, 0.5, 0.05);

        // Громкие хоррор звуки
        target.playSound(target.getLocation(), Sound.ENTITY_WARDEN_ROAR, 2.0f, 0.5f);
        target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 2.0f, 0.5f);
        target.playSound(target.getLocation(), Sound.ENTITY_GHAST_HURT, 2.0f, 0.5f);

        target.sendTitle(Utils.color("&4&k|||&r &c&lОБЕЗДВИЖЕН &4&k|||"), Utils.color("&8Оно видит тебя..."), 0, 35, 10);
    }

    // 3. Фейк Крипер
    public void triggerFakeCreeper(Player target) {
        Location behind = target.getLocation().subtract(target.getLocation().getDirection().multiply(1.5));
        target.playSound(behind, Sound.ENTITY_CREEPER_PRIMED, 1.5f, 1.0f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                Location currentBehind = target.getLocation().subtract(target.getLocation().getDirection().multiply(1.0));
                target.spawnParticle(Particle.EXPLOSION_EMITTER, currentBehind, 1);
                target.playSound(currentBehind, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
                // Подбрасываем игрока как от взрыва
                target.setVelocity(target.getLocation().getDirection().multiply(0.8).setY(0.5));
            }
        }, 30L); // 1.5 секунды
    }

    // 4. Паранойя (Шаги и разворот)
    public void triggerParanoia(Player target) {
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (!target.isOnline() || step >= 8) {
                    if (target.isOnline()) {
                        // Резкий разворот на 180 градусов в конце
                        Location loc = target.getLocation();
                        loc.setYaw(loc.getYaw() + 180);
                        target.teleport(loc);
                        target.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 2.0f, 1.0f);
                        target.playSound(loc, Sound.AMBIENT_CAVE, 2.0f, 0.5f);
                    }
                    this.cancel();
                    return;
                }
                Location behind = target.getLocation().subtract(target.getLocation().getDirection().multiply(2.5));
                target.playSound(behind, Sound.BLOCK_STONE_STEP, 1.5f, 0.8f);
                step++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    // 8. Тряска камеры
    public void triggerCameraShake(Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 160, 2, false, false));
        Random random = new Random();
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (!target.isOnline() || count >= 15) {
                    this.cancel();
                    return;
                }
                target.setVelocity(new Vector(
                        (random.nextDouble() - 0.5) * 0.4,
                        0.1,
                        (random.nextDouble() - 0.5) * 0.4
                ));
                target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.8f);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 6L);
    }

    // 9. Временная паутина
    public void triggerCobwebTrap(Player target) {
        Location loc = target.getLocation().getBlock().getLocation();
        if (loc.getBlock().getType() == Material.AIR) {
            loc.getBlock().setType(Material.COBWEB);
            target.playSound(loc, Sound.ENTITY_SPIDER_AMBIENT, 2.0f, 0.7f);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (loc.getBlock().getType() == Material.COBWEB) {
                    loc.getBlock().setType(Material.AIR);
                }
            }, 80L); // Убираем через 4 секунды
        }
    }

    // 10. Проклятие Null / Herobrine
    public void triggerCurseMessages(Player target) {
        target.playSound(target.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 2.0f, 0.5f);
        target.sendMessage(Utils.color("&eHerobrine joined the game"));
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                target.sendMessage(Utils.color("&7<Herobrine> &4Я за твоей спиной, " + target.getName() + "..."));
                target.sendTitle(Utils.color("&4&k01010101"), Utils.color("&cНЕ ОБОРАЧИВАЙСЯ"), 5, 40, 10);
                target.playSound(target.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.3f);
            }
        }, 40L);
    }
}
