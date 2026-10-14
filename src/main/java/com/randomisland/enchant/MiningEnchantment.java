package com.randomisland.enchant;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 「挖掘」附魔：
 * <ul>
 *   <li>只能附魔在镐（稿子）上（{@link #isAcceptableItem} 限制为 {@link ItemTags#PICKAXES}）。</li>
 *   <li>挖掘任意方块都瞬间破坏，无视硬度，连基岩也能挖（由 mixin 接管挖掘进度实现）。</li>
 *   <li>通过奖励箱（含起步箱）高概率产出对应的附魔书。</li>
 * </ul>
 */
public class MiningEnchantment extends Enchantment {
    public static final Identifier ID = new Identifier("random_island", "mining");
    public static final MiningEnchantment INSTANCE = new MiningEnchantment();

    private MiningEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentTarget.DIGGER, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    public static void register() {
        Registry.register(Registries.ENCHANTMENT, ID, INSTANCE);
    }

    /** 生成一本带「挖掘」附魔（等级 1）的附魔书。 */
    public static ItemStack createBook() {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.addEnchantment(INSTANCE, 1);
        return book;
    }

    /** 判断手持物品是否为带「挖掘」附魔的镐。 */
    public static boolean isMiningPickaxe(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isIn(ItemTags.PICKAXES)
                && EnchantmentHelper.getLevel(INSTANCE, stack) > 0;
    }

    @Override
    public int getMinPower(int level) {
        return 1;
    }

    @Override
    public int getMaxPower(int level) {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isTreasure() {
        return false;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.isIn(ItemTags.PICKAXES);
    }
}
