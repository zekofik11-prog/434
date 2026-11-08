# Artisan (этап 1)
Кастомные предметы из items.yml + ресурспак из plugins/Artisan/pack + встроенная раздача пака.

Сборка: `gradle build` -> build/libs/Artisan-0.1.0.jar в plugins/.
Настрой в config.yml: pack.host (публичный IP/домен), pack.port (открыть порт), pack.pack-format.
Команды: /artisan give <игрок> <id> [кол-во], /artisan list, /artisan reload, /artisan pack
Права: artisan.admin
