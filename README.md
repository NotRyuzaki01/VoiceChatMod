# 🎙️ VoiceChatMod

A simple Minecraft plugin to **mute and unmute players in voice chat** by dynamically changing their LuckPerms group.

---

## 📦 Features

- ✅ `/vmute <player>` — Mutes a player's voice chat by switching them to a muted group.
- ✅ `/vunmute <player>` — Restores the player's original group to unmute them.
- ✅ Configurable group mappings
- ✅ Integration with LuckPerms API.
- ✅ Action bar + chat feedback for both player and sender.

---

## ⚙️ Configuration Example

```yaml
# config.yml
voicechat-groups:
  default: default_muted
  media: media_muted
  plus: plus_muted
