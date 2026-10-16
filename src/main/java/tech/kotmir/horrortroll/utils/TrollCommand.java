package tech.kotmir.horrortroll.utils;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import tech.kotmir.horrortroll.HorrorTroll;

import java.util.*;

public class TrollCommand implements CommandExecutor, TabCompleter, Listener {

    private final HorrorTroll plugin;
    // Храним, какого игрока сейчас троллят через открытое меню (Админ -> Жертва)
    private final Map<UUID, UUID> targetMap = new HashMap<>();

    public TrollCommand(HorrorTroll plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("horrortroll.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды!");
            return true;
        }

        // =================== КОМАНДА /error [НИК] [ТЕКСТ] ===================
        if (command.getName().equalsIgnoreCase("error")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Использование: /error [Ник] [Текст ошибки]");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден или не в сети!");
                return true;
            }

            // Собираем текст ошибки
            StringBuilder errorText = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                errorText.append(args[i]).append(" ");
            }
            String finalError = ChatColor.translateAlternateColorCodes('&', errorText.toString().trim());

            executeFakeError(sender, target, finalError);
            return true;
        }

        // =================== КОМАНДА /troll [НИК] (МЕНЮ) ===================
        if (command.getName().equalsIgnoreCase("troll")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Меню можно открыть только из игры!");
                return true;
            }

            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Использование: /troll [Ник]");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Игрок не найден!");
                return true;
            }

            openTrollMenu(player, target);
            return true;
        }

        return false;
    }

    // Метод для вызова фейк-ошибки с зависанием на 4.5 секунды
    public void executeFakeError(CommandSender sender, Player target, String kickMessage) {
        UUID targetId = target.getUniqueId();
        plugin.frozenPlayers.add(targetId);

        sender.sendMessage(ChatColor.GREEN + "Запущено зависание (4.5 сек) с последующей ошибкой для " + target.getName());

        // Эффекты для имитации зависания и хоррора
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 1, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 255, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 255, false, false));

        // Запускаем задачу, которая каждые 2 тика спамит глитч-звуками и дергает камеру
        new BukkitRunnable() {
            int ticks = 0;
            final Location freezeLoc = target.getLocation();

            @Override
            public void run() {
                if (!target.isOnline()) {
                    plugin.frozenPlayers.remove(targetId);
                    this.cancel();
                    return;
                }

                // Жестко фиксируем позицию (как при лаге) и слегка трясем камеру
                freezeLoc.setYaw(freezeLoc.getYaw() + (float) (Math.random() * 4 - 2));
                freezeLoc.setPitch(freezeLoc.getPitch() + (float) (Math.random() * 4 - 2));
                target.teleport(freezeLoc);

                // Короткий заедающий звук
                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 2.0f, 0.1f);
                if (ticks % 10 == 0) {
                    target.playSound(target.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 2.0f, 0.5f);
                }

                ticks += 2;

                // Через 90 тиков (4.5 секунды) кикаем с кастомным текстом
                if (ticks >= 90) {
                    plugin.frozenPlayers.remove(targetId);
                    target.kickPlayer(
                            ChatColor.DARK_RED + "Internal Exception: io.netty.handler.codec.DecoderException\n" +
                            ChatColor.RED + "java.lang.NullPointerException: " + ChatColor.RESET + kickMessage
                    );
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // =================== СОЗДАНИЕ GUI МЕНЮ ===================
    private void openTrollMenu(Player admin, Player target) {
        targetMap.put(admin.getUniqueId(), target.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Троллинг: " + target.getName());

        inv.setItem(10, createItem(Material.REDSTONE_BLOCK, "&c&lФейк-Краш (Error)",
                "&7Замораживает на 4.5 сек", "&7и кикает с java-ошибкой."));

        inv.setItem(11, createItem(Material.WARDEN_SPAWN_EGG, "&4&lХоррор-Скример",
                "&7Тьма, жуткие звуки Вардена,", "&7ведьмы и тряска экрана."));

        inv.setItem(12, createItem(Material.TNT, "&a&lШипение Крипера",
                "&7Звук крипера прямо за спиной", "&7и фейковый взрыв без урона."));

        inv.setItem(13, createItem(Material.PACKED_ICE, "&b&lЗаморозка / Разморозка",
                "&7Полностью запрещает или разрешает", "&7игроку двигаться и ломать блоки."));

        inv.setItem(14, createItem(Material.ROTTEN_FLESH, "&6&lФейковый Голод и Урон",
                "&7Имитирует удары по игроку", "&7и временно убирает сытость."));

        inv.setItem(15, createItem(Material.FIREWORK_ROCKET, "&e&lЗапуск в космос",
                "&7Подбрасывает игрока высоко вверх."));

        inv.setItem(16, createItem(Material.ENDER_PEARL, "&5&lПаранойя (Шаги и звуки)",
                "&7В течение 10 секунд вокруг игрока", "&7слышатся чужие шаги и шорохи."));

        inv.setItem(19, createItem(Material.DROPPER, "&3&lВыбросить предмет",
                "&7Заставляет игрока выбросить то,", "&7что он держит в главной руке."));

        inv.setItem(20, createItem(Material.LIGHTNING_ROD, "&f&lФейковая Молния",
                "&7Бьет молнией рядом с игроком", "&7без нанесения урона."));

        inv.setItem(21, createItem(Material.CHEST, "&d&lПеремешать Хотбар",
                "&7Случайным образом меняет местами", "&7предметы в быстром доступе."));

        inv.setItem(22, createItem(Material.COBWEB, "&7&lПьяная камера",
                "&7Накладывает сильную тошноту", "&7и замедление на 10 секунд."));

        inv.setItem(23, createItem(Material.CARVED_PUMPKIN, "&6&lСкример Тыквой",
                "&7Надевает тыкву на голову игрока", "&7(предыдущий шлем падает в инвентарь)."));

        admin.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    // =================== ОБРАБОТКА НАЖАТИЙ В МЕНЮ ===================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(ChatColor.DARK_RED + "Троллинг: ")) return;

        event.setCancelled(true); // Запрещаем брать предметы из меню

        if (!(event.getWhoClicked() instanceof Player admin)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        UUID targetUUID = targetMap.get(admin.getUniqueId());
        if (targetUUID == null) return;

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            admin.sendMessage(ChatColor.RED + "Игрок вышел с сервера!");
            admin.closeInventory();
            return;
        }

        switch (event.getRawSlot()) {
            case 10 -> { // Фейк-Краш
                admin.closeInventory();
                executeFakeError(admin, target, "Fatal Horror Exception: Memory Corrupted at 0x0004F2A");
            }
            case 11 -> { // Хоррор-Скример
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1));
                target.playSound(target.getLocation(), Sound.ENTITY_WARDEN_ROAR, 2.0f, 0.5f);
                target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 2.0f, 0.5f);
                target.playSound(target.getLocation(), Sound.ENTITY_GHAST_HURT, 2.0f, 0.5f);
                target.sendTitle(ChatColor.DARK_RED + "Я ВИЖУ ТЕБЯ", ChatColor.BLACK + "Обернись...", 5, 40, 10);
                admin.sendMessage(ChatColor.GREEN + "Хоррор-скример отправлен игроку " + target.getName());
            }
            case 12 -> { // Шипение Крипера
                target.playSound(target.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 2.0f, 1.0f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (target.isOnline()) {
                        target.spawnParticle(Particle.EXPLOSION_EMITTER, target.getLocation(), 1);
                        target.playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
                    }
                }, 30L);
                admin.sendMessage(ChatColor.GREEN + "Фейковый крипер активирован для " + target.getName());
            }
            case 13 -> { // Заморозка / Разморозка
                if (plugin.frozenPlayers.contains(target.getUniqueId())) {
                    plugin.frozenPlayers.remove(target.getUniqueId());
                    admin.sendMessage(ChatColor.YELLOW + "Игрок " + target.getName() + " разморожен!");
                } else {
                    plugin.frozenPlayers.add(target.getUniqueId());
                    admin.sendMessage(ChatColor.AQUA + "Игрок " + target.getName() + " полностью заморожен!");
                }
            }
            case 14 -> { // Фейковый урон
                target.playHurtAnimation(0);
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.5f, 1.0f);
                int oldFood = target.getFoodLevel();
                target.setFoodLevel(1);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (target.isOnline()) target.setFoodLevel(oldFood);
                }, 60L);
                admin.sendMessage(ChatColor.GREEN + "Фейковый урон и голод применены к " + target.getName());
            }
            case 15 -> { // Запуск в космос
                target.setVelocity(new Vector(0, 2.5, 0));
                target.playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 1.0f);
                admin.sendMessage(ChatColor.GREEN + "Игрок " + target.getName() + " запущен в воздух!");
            }
            case 16 -> { // Паранойя
                new BukkitRunnable() {
                    int count = 0;
                    @Override
                    public void run() {
                        if (!target.isOnline() || count >= 10) {
                            this.cancel();
                            return;
                        }
                        Location behind = target.getLocation().subtract(target.getLocation().getDirection().multiply(2));
                        target.playSound(behind, Sound.BLOCK_STONE_STEP, 1.5f, 0.8f);
                        if (count % 3 == 0) {
                            target.playSound(behind, Sound.AMBIENT_CAVE, 2.0f, 0.7f);
                        }
                        count++;
                    }
                }.runTaskTimer(plugin, 0L, 15L);
                admin.sendMessage(ChatColor.GREEN + "Режим паранойи запущен для " + target.getName());
            }
            case 19 -> { // Выбросить предмет
                ItemStack inHand = target.getInventory().getItemInMainHand();
                if (inHand.getType() != Material.AIR) {
                    target.getWorld().dropItemNaturally(target.getLocation(), inHand.clone());
                    target.getInventory().setItemInMainHand(null);
                    target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
                    admin.sendMessage(ChatColor.GREEN + target.getName() + " выронил предмет из руки!");
                } else {
                    admin.sendMessage(ChatColor.RED + "У игрока ничего нет в руке!");
                }
            }
            case 20 -> { // Фейковая молния
                target.getWorld().strikeLightningEffect(target.getLocation());
                admin.sendMessage(ChatColor.GREEN + "Молния ударила в " + target.getName());
            }
            case 21 -> { // Перемешать хотбар
                List<ItemStack> hotbar = new ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    hotbar.add(target.getInventory().getItem(i));
                }
                Collections.shuffle(hotbar);
                for (int i = 0; i < 9; i++) {
                    target.getInventory().setItem(i, hotbar.get(i));
                }
                target.playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                admin.sendMessage(ChatColor.GREEN + "Хотбар игрока " + target.getName() + " перемешан!");
            }
            case 22 -> { // Пьяная камера
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 2));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 2));
                admin.sendMessage(ChatColor.GREEN + "Эффект головокружения выдан " + target.getName());
            }
            case 23 -> { // Тыква на голову
                ItemStack helmet = target.getInventory().getHelmet();
                if (helmet != null && helmet.getType() != Material.AIR) {
                    target.getWorld().dropItemNaturally(target.getLocation(), helmet);
                }
                target.getInventory().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
                target.playSound(target.getLocation(), Sound.ENTITY_GHAST_SCREAM, 2.0f, 0.6f);
                admin.sendMessage(ChatColor.GREEN + "Тыква надета на " + target.getName());
            }
        }
    }

    // =================== ТАБ-КОМПЛИТЕР (ПОДСКАЗКИ ПРИ ВВОДЕ) ===================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return null; // Возвращает стандартный список онлайн-игроков
        }
        if (command.getName().equalsIgnoreCase("error") && args.length == 2) {
            return Arrays.asList(
                    "&4Out_Of_Memory!",
                    "&cConnection_Timed_Out",
                    "&4HE_IS_WATCHING_YOU",
                    "&cChunk_Data_Corrupted"
            );
        }
        return Collections.emptyList();
    }
}