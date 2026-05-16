package com.hongshaoluobotou.util;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class AdvancementUtil {

    /**
     * 获取成就的本地化标题
     */
    public static String getTitle(AdvancementHolder holder) {
        return holder.value()
                .display()
                .map(DisplayInfo::getTitle)
                .map(Component::getString)
                .orElse(holder.id().toString());
    }

    /**
     * 获取成就的本地化描述
     */
    public static String getDescription(AdvancementHolder holder) {
        return holder.value()
                .display()
                .map(DisplayInfo::getDescription)
                .map(Component::getString)
                .orElse("");
    }

    /**
     * 获取成就类型
     */
    public static AdvancementType getType(AdvancementHolder holder) {
        return holder.value()
                .display()
                .map(DisplayInfo::getType)
                .orElse(AdvancementType.TASK);
    }

    /**
     * 获取成就类型的本地化名称
     */
    public static String getTypeName(AdvancementHolder holder) {
        return switch (getType(holder)) {
            case CHALLENGE -> "挑战";
            case GOAL -> "目标";
            case TASK -> "任务";
        };
    }

    /**
     * 获取成就图标物品的 ID（如 "minecraft:wooden_pickaxe"）
     */
    public static String getIconId(AdvancementHolder holder) {
        return holder.value()
                .display()
                .map(DisplayInfo::getIcon)
                .map(icon -> {
                    // icon 是 ItemStackTemplate (record)
                    // .item() 返回 Holder<Item>
                    // .value() 获取 Item 实例
                    // BuiltInRegistries.ITEM.getKey() 返回 Identifier
                    Identifier id = BuiltInRegistries.ITEM.getKey(icon.item().value());
                    return id.toString();
                })
                .orElse("minecraft:air");
    }

    /**
     * 获取成就图标物品的 Identifier 对象
     */
    public static Identifier getIconIdentifier(AdvancementHolder holder) {
        return holder.value()
                .display()
                .map(DisplayInfo::getIcon)
                .map(icon -> BuiltInRegistries.ITEM.getKey(icon.item().value()))
                .orElse(Identifier.parse("minecraft:air"));
    }

    /**
     * 获取成就图标物品的纯路径名（不含命名空间）
     */
    public static String getIconPath(AdvancementHolder holder) {
        return holder.value()
                .display()
                .map(DisplayInfo::getIcon)
                .map(icon -> BuiltInRegistries.ITEM.getKey(icon.item().value()).getPath())
                .orElse("air");
    }

    /**
     * 获取成就图标物品的命名空间
     */
    public static String getIconNamespace(AdvancementHolder holder) {
        return holder.value()
                .display()
                .map(DisplayInfo::getIcon)
                .map(icon -> BuiltInRegistries.ITEM.getKey(icon.item().value()).getNamespace())
                .orElse("minecraft");
    }

    /**
     * 创建图标对应的 ItemStack（用于渲染等）
     */
    public static ItemStack getIconStack(AdvancementHolder holder) {
        return holder.value()
                .display()
                .map(DisplayInfo::getIcon)
                .map(icon -> icon.create())  // ItemStackTemplate.create() 创建 ItemStack
                .orElse(ItemStack.EMPTY);
    }

    /**
     * 完整信息字符串
     */
    public static String getFullInfo(AdvancementHolder holder) {
        String title = getTitle(holder);
        String desc = getDescription(holder);
        String type = getTypeName(holder);
        String iconId = getIconId(holder);
        return String.format("[%s] %s - %s (图标: %s)", type, title, desc, iconId);
    }
}