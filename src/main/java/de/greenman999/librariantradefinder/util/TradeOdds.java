package de.greenman999.librariantradefinder.util;

import de.greenman999.librariantradefinder.LibrarianTradeFinder;
import de.greenman999.librariantradefinder.config.TradeFinderConfig;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;

// Odds that a freshly reset (novice) librarian offers a matching enchanted book, based on the vanilla 26.3 trade data:
// - the novice trade set picks 2 of 3 trades without duplicates, so the book trade shows up 2/3 of the time
// - the enchantment is picked uniformly from #minecraft:tradeable, the level uniformly from min to max level
// - price = 2 + rand(5 + level * 10) + level * 3, doubled for #minecraft:double_trade_price, capped at 64
public class TradeOdds {
    private static final double BOOK_TRADE_CHANCE = 2.0 / 3.0;
    private static final int MAX_PRICE = 64;

    public static double chance(Enchantment enchantment, int minLevel, int maxPrice) {
        int tradeableCount = LibrarianTradeFinder.getConfig().enchantments.size();
        if (tradeableCount == 0) return 0;

        int priceMultiplier = isDoublePrice(enchantment) ? 2 : 1;
        int lowestLevel = enchantment.getMinLevel();
        int highestLevel = enchantment.getMaxLevel();

        double levelSum = 0;
        for (int level = Math.max(minLevel, lowestLevel); level <= highestLevel; level++) {
            levelSum += priceChance(level, priceMultiplier, maxPrice);
        }
        double levelChance = levelSum / (highestLevel - lowestLevel + 1);

        return BOOK_TRADE_CHANCE / tradeableCount * levelChance;
    }

    public static double chance(TradeFinderConfig.EnchantmentOption option) {
        return chance(option.enchantment, option.getLevel(), option.getMaxPrice());
    }

    // Each try is a single lectern reset, and only one book trade can show up per reset,
    // so the per-enchantment chances add up.
    public static double totalChance() {
        double total = 0;
        for (TradeFinderConfig.EnchantmentOption option : LibrarianTradeFinder.getConfig().enchantments.values()) {
            if (option.isEnabled()) total += chance(option);
        }
        return total;
    }

    // Chance that the price for the given level is at most maxPrice
    private static double priceChance(int level, int priceMultiplier, int maxPrice) {
        if (maxPrice >= MAX_PRICE) return 1;
        int randomRange = 5 + level * 10;
        int maxRoll = maxPrice / priceMultiplier - 2 - level * 3;
        int matchingRolls = Math.clamp(maxRoll + 1, 0, randomRange);
        return (double) matchingRolls / randomRange;
    }

    private static boolean isDoublePrice(Enchantment enchantment) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;
        return minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .wrapAsHolder(enchantment).is(EnchantmentTags.DOUBLE_TRADE_PRICE);
    }

    public static String formatChance(double chance) {
        double percent = chance * 100;
        if (percent == 0) return "0%";
        if (percent < 0.1) return String.format(Locale.ROOT, "%.2f%%", percent);
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }

    public static String formatTries(double chance) {
        if (chance <= 0) return "-";
        double tries = 1 / chance;
        if (tries < 10) return String.format(Locale.ROOT, "%.1f", tries);
        return String.valueOf(Math.round(tries));
    }
}
