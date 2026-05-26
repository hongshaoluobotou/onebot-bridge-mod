# OneBot Bridge Mod

一个轻量的Fabric模组，与野生QQ机器人实现群服互通，支持Minecraft 26.2-pre-1

## 功能

### QQ(OneBotV11) → Minecraft
* [x] 普通文本消息和emoji
* [ ] 引用回复
* [ ] 合并转发消息
* [x] 表情符号（仅初步支持）
* [ ] 图片
* [ ] ...

### Minecraft → QQ(OneBotV11)
* [x] 普通文本消息（聊天、进出服、死亡、重生和获得成就）
* [ ] `/tell` 和 `/w` 私聊回复
* [ ] ...

## 配置

首次启动后，模组会在配置目录生成 `config/onebot-bridge-mod/onebot-bridge-mod.toml`：

```toml
# OneBotV11的正向WebSocket地址
url = "ws://127.0.0.1:8080"

# 鉴权token，如果没有配置可以留空
token = ""

# QQ群号
groupId = "123456789"
```

修改配置后，在游戏内执行 `/onebot-bridge-mod reload` 重载配置文件并重写连接到你的机器人。

## 许可协议

本项目采用 **GNU Affero 通用公共许可证第三版（AGPL-3.0）** 进行许可。

完整许可证文本见：<https://www.gnu.org/licenses/agpl-3.0.html>