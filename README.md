## JItemSaver

**Ядро**: Paper / Spigot  
**Версия**: 1.16.5 - 1.21.11  
**Java**: 16+  
**Зависимости**: ExecutableItems, SCore

### Возможности
- Сохраненение предмета
- Сохранение предмета с указанными зачарованиями и их уровнями
- Сохраненение кастомных предметов (ExecutableItems)
- Установка слота сохранения для предмета (описано в `config.yml`)

### Установка
1. Скачай `JItemSaver-v1.0.jar`
2. Положи файл в папку `plugins`
3. Перезапусти сервер

### Конфигурация (config.yml)
```yaml
messages:
  no-permission: "&4✗ &cНедостаточно прав &fдля использования команды"
  reload: "&2✔ &fПлагин успешно &aперезагружен"
  itemsaver-message: "&2✔ &fПредметы были сохранены &7(&a{count} &7шт.)"

# Список предметов для сохранения
# Форматы:
#
# Просто материал (любой слот):
#   - DIAMOND
#   - TOTEM_OF_UNDYING
#   - NETHERITE_SWORD
#
# Материал с обязательными чарами (любой слот):
#   - NETHERITE_SWORD;SHARPNESS:5,MENDING:1
#   - DIAMOND_SWORD;SHARPNESS:10,LOOT_BONUS_MOBS:3
#
# Кастомный предмет ExecutableItems (любой слот):
#   - CUSTOM:super_sword
#   - CUSTOM:my_special_totem
#
# Указание конкретного слота (только если предмет в этом слоте):
#   - DIAMOND_HELMET:HEAD           # только шлем на голове
#   - NETHERITE_CHESTPLATE:CHEST    # только нагрудник
#   - DIAMOND_LEGGINGS:LEGS
#   - DIAMOND_BOOTS:FEET
#   - GOLDEN_APPLE:OFFHAND      # только в оффхенде
#   - CUSTOM:god_helmet:HEAD        # только кастомный шлем на голове
#   - CUSTOM:god_sword:OFFHAND
#   - DIAMOND_HELMET;UNBREAKING:3:HEAD
#
# Доступные слоты:
#   HEAD     — шлем
#   CHEST    — нагрудник
#   LEGS     — поножи
#   FEET     — ботинки
#   OFFHAND  — вторая рука (оффхенд)
# Если слот НЕ указан — предмет проверяется во всех слотах (инвентарь + броня + оффхенд)

itemsaver-list:
  - TOTEM_OF_UNDYING
  - NETHERITE_SWORD;SHARPNESS:5,MENDING:1
  - GOLDEN_APPLE:OFFHAND
  - DIAMOND_HELMET:HEAD
  - CUSTOM:super_sword
  # и т.д.
```

### Формат записи предметов
- Просто название материала: `DIAMOND`, `TOTEM_OF_UNDYING`, `ELYTRA`
- С указанными зачарованиями: `NETHERITE_SWORD;SHARPNESS:5,MENDING:1`
  - Чары разделяются запятой `,`
  - Название чара пишется в `ВЕРХНЕМ` регистре
  - Уровень указывается после `:`

### Команды
| Команда      | Описание             | Право               | По умолчанию |
|--------------|----------------------|---------------------|--------------|
| `/jisreload` | Перезагрузка плагина | `jitemsaver.reload` | OP           |

<img width="654" height="66" alt="Знімок екрана з 2026-01-20 20-45-15" src="https://github.com/user-attachments/assets/901e002a-4c6d-47bd-944c-b4ebc749d53b" />
