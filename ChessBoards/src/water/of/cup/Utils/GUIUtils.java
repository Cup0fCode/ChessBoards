package water.of.cup.Utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import water.of.cup.chessBoard.ChessGame;

public class GUIUtils {

    public static void fillBackground(Inventory inventory, ItemStack itemStack) {
        for(int i = 0; i < 54; i++) {
            inventory.setItem(i, itemStack);
        }
    }

    public static void fillRect(Inventory inventory, int[] upper, int[] lower, ItemStack itemStack) {
        int index = 0;
        for(int y = 0; y < 6; y++) {
            for(int x = 0; x < 9; x++) {
                if(y >= upper[1] && x >= upper[0] && y <= lower[1] && x <= lower[0]) {
                    inventory.setItem(index, itemStack);
                }
                index++;
            }
        }
    }

    public static ItemStack createItemStack(String displayName, Material material) {
        ItemStack itemStack = new ItemStack(material, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static ItemStack createGuiPlayerItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a" + player.getName());

        // TODO: Add lore containing player's stats

        SkullMeta skullMeta = (SkullMeta) meta;
        skullMeta.setOwningPlayer(player);
        item.setItemMeta(skullMeta);
        return item;
    }

    public static void renderGameData(Inventory inventory, ChessGame chessGame) {
        if(chessGame.getWhitePlayer() == null) return;

        ItemStack playerHead = GUIUtils.createGuiPlayerItem(chessGame.getWhitePlayer());

        String rankedString = chessGame.isRanked() ? ChatColor.RED + "Ranked" : ChatColor.GREEN + "Unranked";
        ItemStack ranked =  GUIUtils.createItemStack(rankedString, Material.EXPERIENCE_BOTTLE);

        String gameTimeString = chessGame.getGameTime() / (60*60) + "h" + (chessGame.getGameTime() % (60*60)) / 60 + "m";
        ItemStack gameTimeItem = GUIUtils.createItemStack(ChatColor.GREEN + "Game Time: " + ChatColor.DARK_GREEN + gameTimeString, Material.CLOCK);

        ItemStack wager = GUIUtils.createItemStack(ChatColor.GREEN + "Wager Amount: $" + ChatColor.DARK_GREEN + chessGame.getWager(), Material.GOLD_INGOT);

        inventory.setItem(11, playerHead);
        inventory.setItem(20, ranked);
        inventory.setItem(29, gameTimeItem);
        inventory.setItem(38, wager);
    }
}