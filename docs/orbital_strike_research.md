# Minecraft Plugin — Orbital Strike System Research

## Overview

Идея плагина:
- Орбитальный лазерный удар
- Кастомный блок с GUI
- Эффект страха
- Разрушение и поджог мира
- Cinematic визуальные эффекты

Лучше всего реализовывать как плагин под Paper / Spigot API.

---

# Основные функции плагина

## Орбитальный удар

Система:
1. Игрок ставит блок "Орбитальный Удар"
2. Открывается GUI
3. Игрок вводит:
   - координаты X/Y/Z
   - задержку удара
   - силу удара (1–10)
   - количество ударов
   - силу эффекта страха
   - длительность эффекта
4. Нажимает кнопку "Активировать"
5. Начинается анимация:
   - 6 красных точек в виде шестиугольника
   - белая точка в центре
   - лазер сверху вниз
6. Лазер:
   - ломает блоки
   - создаёт взрыв
   - поджигает местность
7. Игроки получают эффект страха

---

# Аналоги и похожие плагины

## Strike Cannon
https://modrinth.com/plugin/strike-cannon

Функции:
- orbital strike
- particle beam
- configurable power
- bombardment system

Полезно посмотреть:
- архитектуру ударов
- систему визуальных эффектов
- оптимизацию разрушения

---

## NguyenDevs Orbital Strike Cannon
https://modrinth.com/plugin/nguyendevs-orbital-strike-cannon

Есть:
- orbital cannon
- strike payloads
- visual effects
- targeting system

---

## Orbital Paper Nuke
https://modrinth.com/plugin/orbital-paper-nuke

Полезно для:
- разрушения мира
- fire logic
- explosion handling
- kinetic strike effects

---

# Рекомендуемый стек

| Компонент | Рекомендуется |
|---|---|
| Сервер | Paper 26.1.2 |
| Язык | Java 21 |
| Build System | Gradle |
| API | Paper API |
| GUI | Bukkit Inventory API |
| Эффекты камеры | ProtocolLib |
| Кастомный блок | PersistentDataContainer |
| Scheduler | BukkitRunnable |

---

# Важно про версию

Minecraft 26.1.2 — актуальная версия (year-based versioning, выпущена в 2026).

Используется:
- Paper 26.1.2

Скачать: https://papermc.io/downloads/paper

---

# Как реализовать кастомный блок

Без модов нельзя создать настоящий новый block ID.

Обычно используют:
- Dropper
- Observer
- Respawn Anchor
- Smithing Table

И помечают через:
PersistentDataContainer

Пример:

```java
NamespacedKey key = new NamespacedKey(plugin, "orbital_strike");

ItemMeta meta = item.getItemMeta();

meta.getPersistentDataContainer().set(
    key,
    PersistentDataType.STRING,
    "true"
);

item.setItemMeta(meta);
```

---

# GUI интерфейс

Рекомендуется:
- custom InventoryHolder

Документация:
https://docs.papermc.io/paper/dev/custom-inventory-holder/

---

## Пример структуры GUI

| Слот | Значение |
|---|---|
| 10 | X |
| 11 | Y |
| 12 | Z |
| 13 | Delay |
| 14 | Power |
| 15 | Count |
| 22 | Fear Power |
| 23 | Fear Duration |
| 31 | ACTIVATE |

---

# Ввод значений

## Лучший способ — AnvilGUI

GitHub:
https://github.com/WesJD/AnvilGUI

Игрок вводит числа через rename GUI.

---

# Визуальные эффекты

## Красные точки (шестиугольник)

Формула круга:

x = r cos(theta)
y = r sin(theta)

Для 6 точек:
- шаг = 60°

Пример:

```java
for (int i = 0; i < 6; i++) {
    double angle = Math.toRadians(i * 60);

    double x = centerX + radius * Math.cos(angle);
    double z = centerZ + radius * Math.sin(angle);

    world.spawnParticle(
        Particle.DUST,
        x,
        y,
        z,
        1,
        new DustOptions(Color.RED, 2)
    );
}
```

---

# Белая центральная точка

Рекомендуемые particles:
- Particle.END_ROD
- Particle.FLASH

---

# Лазерный луч

Лучше всего:
- Particle.DUST
- Particle.END_ROD
- BlockDisplay entities

Пример:

```java
for (double y = skyY; y > groundY; y -= 0.5) {
    world.spawnParticle(
        Particle.END_ROD,
        x,
        y,
        z,
        3
    );
}
```

---

# Разрушение мира

## Простой вариант

```java
world.createExplosion(
    location,
    power,
    true,
    true
);
```

---

## Кастомное разрушение

Позволяет:
- ограничивать типы блоков
- учитывать hardness
- контролировать производительность

---

# Поджог блоков

```java
block.setType(Material.FIRE);
```

---

# Эффект "Страх"

## Что должен делать эффект

- тряска камеры
- мигание экрана
- эффекты темноты
- дезориентация

---

# Реализация тряски камеры

Vanilla API не поддерживает camera shake.

Используется:
- ProtocolLib

GitHub:
https://github.com/dmulloy2/ProtocolLib

---

## Возможные эффекты

### Darkness

```java
player.addPotionEffect(
    new PotionEffect(
        PotionEffectType.DARKNESS,
        duration,
        amplifier
    )
);
```

---

### Nausea

```java
PotionEffectType.NAUSEA
```

---

### Camera jitter

```java
Location loc = player.getLocation();

loc.setYaw(loc.getYaw() + random(-3, 3));
loc.setPitch(loc.getPitch() + random(-2, 2));

player.teleport(loc);
```

Лучше делать через packets.

---

# Чёрные вспышки экрана

Можно комбинировать:
- DARKNESS
- Titles
- packet overlays

---

# Производительность

Орбитальные удары могут сильно нагружать сервер.

Обязательно:
- ограничение количества блоков
- batching
- async calculations
- sync world modification only

---

# Полезные библиотеки

## GUI

Triumph GUI
https://github.com/TriumphTeam/triumph-gui

Inventory Framework
https://github.com/stefvanschie/IF

---

## Packets

ProtocolLib
https://github.com/dmulloy2/ProtocolLib

---

# Рекомендуемая структура проекта

```text
orbitalstrike/
 ├── commands/
 ├── gui/
 ├── laser/
 ├── effects/
 ├── strike/
 ├── fear/
 ├── config/
 └── util/
```

---

# Главные классы

| Класс | Назначение |
|---|---|
| OrbitalStrikePlugin | main |
| StrikeManager | управление ударами |
| LaserRenderer | визуализация лазера |
| FearEffectManager | эффект страха |
| OrbitalGUI | GUI |
| StrikeTask | таймер удара |
| BlockTagService | кастомный блок |
| ExplosionService | разрушение |

---

# Самые сложные части

1. Cinematic laser animation
2. Camera shake
3. Оптимизация разрушения мира

---

# Что рекомендуется использовать

- Paper API
- Java 21
- Gradle
- ProtocolLib
- Particle system
- PersistentDataContainer
- Inventory GUI

---

# Полезные ссылки

## Документация

PaperMC Docs
https://docs.papermc.io/

Paper API Javadocs
https://jd.papermc.io/paper/

Spigot GUI Tutorial
https://www.spigotmc.org/wiki/creating-a-gui-inventory/

---

# Возможные улучшения

Можно добавить:
- shockwave ring
- satellite targeting beam
- sirens
- heat distortion
- custom sounds
- shaders через resource pack
- anti-lag chunk protection

---

# Итог

Проект полностью реализуем как:
- Paper plugin
- без модов
- с GUI
- с orbital laser system
- с cinematic visual effects
- с эффектом страха
- с разрушением мира
