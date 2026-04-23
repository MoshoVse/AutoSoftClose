# SoftClose — Fabric Mod 1.21.4 FULLY AI CODED


RU:


Автоматизация soft close циклов для контейнерных GUI в Minecraft.

---

## Установка

### Зависимости (обязательно)
- **Fabric Loader** ≥ 0.16.0
- **Fabric API** 0.110.5+1.21.4
- **Cloth Config** 15.0.140

### Опционально
- **ModMenu** 13.0.0 — для доступа к настройкам через меню модов

### Сборка из исходников
```bash
git clone <repo>
cd softclose
./gradlew build
# JAR будет в: build/libs/softclose-1.0.0.jar
```

---

## Управление

| Сочетание | Действие |
|-----------|----------|
| **Alt + F7** | Запустить цикл |
| **END** | Остановить цикл |

---

## Цикл автоматизации (точный)

```
Шаг 1: Отправить команду открытия GUI (из конфига)
Шаг 2: Показать экран "Положите предметы" + кнопка "Я положил"
         (или авто, если Manual Confirm = OFF)
Шаг 3: C2S CloseHandledScreen пакет → переоткрыть GUI
Шаг 4: Забрать предметы по конфигу слотов
         (ручной экран "Забрал" или авто)
Шаг 5: SOFT CLOSE (только клиент, без пакета) → переоткрыть → забрать остатки
Шаг 6: C2S CloseHandledScreen пакет → вернуться к шагу 1
```

---

## Конфигурация

Файлы конфига: `.minecraft/config/softclose/`

### config.json
```json
{
  "autoCycle": false,
  "cycleDelayMs": 500,
  "guiOpenCommand": "/say open",
  "pickupSlots": [0, 1, 2, 3, 4, 5, 6, 7, 8],
  "manualConfirm": true,
  "activeProfileName": "Default"
}
```

| Параметр | Тип | Описание |
|----------|-----|----------|
| `autoCycle` | bool | Авто-цикл без ручного подтверждения |
| `cycleDelayMs` | int | Задержка между шагами (мс) |
| `guiOpenCommand` | string | Команда для открытия GUI (шаг 1) |
| `pickupSlots` | int[] | Слоты для забора предметов (шаг 4) |
| `manualConfirm` | bool | Требовать кнопку-подтверждение |
| `activeProfileName` | string | Активный профиль команд |

### profiles.json
```json
[
  {
    "name": "Default",
    "onOpenCommand": "",
    "afterPickupCommand": "",
    "beforeCloseCommand": ""
  }
]
```

Профили команд поддерживают привязку произвольных команд к событиям цикла.
Управление профилями доступно через GUI в игре.

### Диапазоны слотов
В поле Cloth Config можно вводить диапазоны через дефис:
```
0-8,27-35
```
Эквивалентно слотам 0,1,2,3,4,5,6,7,8,27,28,29,30,31,32,33,34,35

---

## Техническая архитектура

```
SoftCloseMod (entrypoint)
├── ConfigManager         — Cloth Config + JSON профили
├── CycleManager          — State machine (IDLE → OPENING_GUI → ... → FINAL_CLOSE)
├── KeybindManager        — Alt+F7 / END регистрация
├── CommandHandler        — Отправка C/S команд
├── SoftCloseScreen       — GUI для шагов 2 и 4
├── CommandProfileScreen  — GUI управления профилями
└── Mixins
    ├── HandledScreenMixin              — onInit (→ onContainerScreenOpened)
    │                                    removed() (→ подавить пакет при soft close)
    └── ClientPlayNetworkHandlerMixin   — onOpenScreen (логирование)
```

### Soft Close механизм
- `SoftCloseModAccessor.setSoftClosing(true)` перед `mc.setScreen(null)`
- `HandledScreenMixin` перехватывает `removed()` и отменяет `CallbackInfo` если флаг установлен
- Это предотвращает отправку `CloseHandledScreenC2SPacket` серверу

---

## Структура файлов

```
src/main/java/com/softclose/
├── SoftCloseMod.java
├── command/
│   └── CommandHandler.java
├── config/
│   ├── ClothConfigScreen.java
│   ├── ConfigManager.java
│   └── ModMenuIntegration.java
├── manager/
│   ├── CycleManager.java
│   ├── CycleState.java
│   ├── KeybindManager.java
│   └── SoftCloseModAccessor.java
├── mixin/
│   ├── ClientPlayNetworkHandlerMixin.java
│   └── HandledScreenMixin.java
└── screen/
    ├── CommandProfileScreen.java
    └── SoftCloseScreen.java
```

---

## Лицензия

MIT



ENG:

Automation of **Soft Close** cycles for container GUIs in Minecraft.

---

## Installation

### Prerequisites (Required)
- **Fabric Loader** ≥ 0.16.0
- **Fabric API** 0.110.5 + 1.21.4
- **Cloth Config** 15.0.140

### Optional
- **ModMenu** 13.0.0 — for accessing configuration via the in-game mod list.

### Building from Source
```bash
git clone <repo_url>
cd softclose
./gradlew build
# The JAR will be located at: build/libs/softclose-1.0.0.jar
```

---

## Controls

| Keybind | Action |
|-----------|----------|
| **Alt + F7** | Start Cycle |
| **END** | Stop Cycle |

---

## Automation Cycle (Logic Flow)

1. **Step 1:** Send the GUI opening command (defined in config).
2. **Step 2:** Display "Deposit Items" screen + "Done" button (skipped if `Manual Confirm` is **OFF**).
3. **Step 3:** Send `C2S CloseHandledScreen` packet → Re-open GUI.
4. **Step 4:** Collect items based on configured slot IDs (manual "Collected" screen or auto).
5. **Step 5:** **SOFT CLOSE** (Client-side only, no packet sent) → Re-open → Collect remaining items.
6. **Step 6:** Send `C2S CloseHandledScreen` packet → Return to **Step 1**.

---

## Configuration

Config files are stored in: `.minecraft/config/softclose/`

### config.json
```json
{
  "autoCycle": false,
  "cycleDelayMs": 500,
  "guiOpenCommand": "/say open",
  "pickupSlots": [0, 1, 2, 3, 4, 5, 6, 7, 8],
  "manualConfirm": true,
  "activeProfileName": "Default"
}
```

| Parameter | Type | Description |
|----------|-----|----------|
| `autoCycle` | bool | Automatically progress through cycles without manual confirmation. |
| `cycleDelayMs` | int | Delay between steps in milliseconds. |
| `guiOpenCommand` | string | Command used to trigger the GUI opening (Step 1). |
| `pickupSlots` | int[] | List of slot IDs to interact with during Step 4. |
| `manualConfirm` | bool | If true, requires manual button clicks to proceed between stages. |
| `activeProfileName`| string | The name of the currently active command profile. |

### profiles.json
```json
[
  {
    "name": "Default",
    "onOpenCommand": "",
    "afterPickupCommand": "",
    "beforeCloseCommand": ""
  }
]
```
Command profiles allow you to bind custom commands to specific cycle events. Profiles can be managed via the in-game GUI.

### Slot Ranges
In the Cloth Config field, you can input ranges using hyphens:
`0-8, 27-35`
This is equivalent to slots: `0, 1, 2, 3, 4, 5, 6, 7, 8, 27, 28, 29, 30, 31, 32, 33, 34, 35`.

---

## Technical Architecture

```
SoftCloseMod (entrypoint)
├── ConfigManager         — Cloth Config + JSON profiles
├── CycleManager          — State machine (IDLE → OPENING_GUI → ... → FINAL_CLOSE)
├── KeybindManager        — Alt+F7 / END registration
├── CommandHandler        — C/S command dispatching
├── SoftCloseScreen       — UI for steps 2 and 4
├── CommandProfileScreen  — UI for profile management
└── Mixins
    ├── HandledScreenMixin              — onInit (→ onContainerScreenOpened)
    │                                     removed() (→ suppress packet during soft close)
    └── ClientPlayNetworkHandlerMixin   — onOpenScreen (logging/tracking)
```

### Soft Close Mechanism
- The mod calls `SoftCloseModAccessor.setSoftClosing(true)` immediately before `mc.setScreen(null)`.
- `HandledScreenMixin` intercepts the `removed()` method and cancels the `CallbackInfo` if the flag is active.
- **Result:** The client closes the UI locally without sending a `CloseHandledScreenC2SPacket` to the server, allowing for specific GUI state manipulations.

---

## Project Structure

```
src/main/java/com/softclose/
├── SoftCloseMod.java
├── command/
│   └── CommandHandler.java
├── config/
│   ├── ClothConfigScreen.java
│   ├── ConfigManager.java
│   └── ModMenuIntegration.java
├── manager/
│   ├── CycleManager.java
│   ├── CycleState.java
│   ├── KeybindManager.java
│   └── SoftCloseModAccessor.java
├── mixin/
│   ├── ClientPlayNetworkHandlerMixin.java
│   └── HandledScreenMixin.java
└── screen/
    ├── CommandProfileScreen.java
    └── SoftCloseScreen.java
```

---

## License

**MIT License**.
