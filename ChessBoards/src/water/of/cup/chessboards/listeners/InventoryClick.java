package water.of.cup.chessboards.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import water.of.cup.chessboards.ChessBoards;
import water.of.cup.chessboards.chessBoard.ChessGame;
import water.of.cup.chessboards.chessBoard.ChessGameState;
import water.of.cup.chessboards.chessBoard.RequestWager;
import water.of.cup.chessboards.chessBoard.Wager;
import water.of.cup.chessboards.inventories.ChessInGameInventory;
import water.of.cup.chessboards.inventories.ChessCreateGameInventory;
import water.of.cup.chessboards.inventories.ChessJoinGameInventory;
import water.of.cup.chessboards.inventories.ChessWagerViewInventory;
import water.of.cup.chessboards.inventories.ChessWaitingPlayerInventory;
import water.of.cup.chessboards.utils.ConfigMessage;

public class InventoryClick implements Listener {

    private ChessBoards pluginInstance = ChessBoards.getInstance();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(!(event.getWhoClicked() instanceof Player)) return;
        if(event.getClickedInventory() == null) return;
        if(event.getClickedInventory().getType().equals(InventoryType.PLAYER) ||
                !event.getView().getTopInventory().getType().equals(InventoryType.CHEST)) return;

        Player player = (Player) event.getWhoClicked();

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR) return;

        ItemMeta itemMeta = itemStack.getItemMeta();
        if(itemMeta == null) return;

        String itemName = ChatColor.stripColor(itemMeta.getDisplayName());

        Material itemType = itemStack.getType();

        if (event.getClickedInventory().getHolder() instanceof ChessCreateGameInventory) {

            if(!pluginInstance.playerHasCreateGame(player)) return;

            event.setCancelled(true);

            ChessCreateGameInventory chessCreateGameInventory = pluginInstance.getCreateGameInventory(player);

            // Exit button
            if(itemType.equals(Material.BARRIER)) {
                player.closeInventory();
                return;
            }

            if(itemName.contains(ConfigMessage.MESSAGE_GUI_CREATEGAME.toRawString()) && (itemType.equals(Material.LIME_STAINED_GLASS_PANE))) {
                if(!chessCreateGameInventory.getChessGame().getGameState().equals(ChessGameState.IDLE)) {
                    player.closeInventory();
                    player.sendMessage(ConfigMessage.MESSAGE_CHAT_GAME_ALREADY_CREATED.toString());
                    return;
                }
                if (pluginInstance.getEconomy() != null && pluginInstance.getEconomy().getBalance(player) < chessCreateGameInventory.getWager()) {
        			player.sendMessage(ConfigMessage.MESSAGE_CHAT_NOT_ENOUGH_MONEY_CREATE_WAGER.toString());
        			return;
        		}

                player.closeInventory();

                chessCreateGameInventory.getChessGame().resetBoard(true);

                chessCreateGameInventory.getChessGame().setGameState(ChessGameState.WAITING_PLAYER);
                chessCreateGameInventory.getChessGame().setWhitePlayer(player);

                // Sets game settings
                chessCreateGameInventory.getChessGame().setRanked(chessCreateGameInventory.isRanked());
                chessCreateGameInventory.getChessGame().setClocks(chessCreateGameInventory.getGameTimeString());
                chessCreateGameInventory.getChessGame().setGameWager(chessCreateGameInventory.getWager());
//                chessCreateGameInventory.getChessGame().setWager(chessCreateGameInventory.getWager());

                chessCreateGameInventory.getChessGame().openWaitingPlayerInventory();

                // Close inventories
                for(Player player1 : pluginInstance.getCreateGamePlayers()) {
                    ChessCreateGameInventory ccInv = pluginInstance.getCreateGameInventory(player1);
                    if(ccInv != null && ccInv.getChessGame().getGameId() == chessCreateGameInventory.getChessGame().getGameId()) {
                        ChessGame game = ccInv.getChessGame();
                        player1.closeInventory();
                        if (game.getPlayerQueue().size() < 3) {
                            game.addPlayerToDecisionQueue(player1);
                        } else {
                            player1.sendMessage(ConfigMessage.MESSAGE_CHAT_GAME_ALREADY_CREATED.toString());
                        }
                    }
                }
                return;
            }

            if((itemName.equals(ConfigMessage.MESSAGE_GUI_RANKEDTEXT.toRawString()) || itemName.equals(ConfigMessage.MESSAGE_GUI_UNRANKEDTEXT.toRawString())) && (itemType.equals(Material.RED_STAINED_GLASS_PANE) ||
                    itemType.equals(Material.GREEN_STAINED_GLASS_PANE))) {
                chessCreateGameInventory.toggleRanked();
                chessCreateGameInventory.displayCreateGame(player, false);
                return;
            }

            if(itemType.equals(Material.PLAYER_HEAD)) {
                // Increment
                if(itemName.equals(ConfigMessage.MESSAGE_GUI_UP.toRawString())) {
                    ItemStack itemBelow = event.getClickedInventory().getItem(event.getRawSlot() + 9);
                    if(itemBelow == null) return;

                    Material materialBelow = itemBelow.getType();
                    switch (materialBelow) {
                        case CLOCK:
                            chessCreateGameInventory.incrementGameTime();
                            break;
                        case GOLD_INGOT:
                            chessCreateGameInventory.incrementWager();
                            break;
                    }

                    chessCreateGameInventory.displayCreateGame(player, false);
                    return;
                }

                // Decrement
                if(itemName.equals(ConfigMessage.MESSAGE_GUI_DOWN.toRawString())) {
                    ItemStack itemAbove = event.getClickedInventory().getItem(event.getRawSlot() - 9);
                    if(itemAbove == null) return;

                    Material materialAbove = itemAbove.getType();
                    switch (materialAbove) {
                        case CLOCK:
                            chessCreateGameInventory.decrementGameTime();
                            break;
                        case GOLD_INGOT:
                            chessCreateGameInventory.decrementWager();
                            break;
                    }

                    chessCreateGameInventory.displayCreateGame(player, false);
                    return;
                }

                return;
            }
        }

        if (event.getClickedInventory().getHolder() instanceof ChessWaitingPlayerInventory) {

            ChessGame chessGame = pluginInstance.getChessBoardManager().getGameByPlayer(player);

            if(chessGame == null) return;

            event.setCancelled(true);

            // Exit button
            if(itemType.equals(Material.BARRIER)) {
                player.closeInventory();
                return;
            }

            // Game owner accepts or declines players in queue
            if(itemType.equals(Material.GREEN_STAINED_GLASS_PANE) ||
                    itemType.equals(Material.RED_STAINED_GLASS_PANE) ) {

                if(itemType.equals(Material.GREEN_STAINED_GLASS_PANE)) {
                    ItemStack playerItem = event.getClickedInventory().getItem(event.getRawSlot() - 18);
                    if(playerItem == null) return;

                    ItemMeta playerItemMeta = playerItem.getItemMeta();
                    if(playerItemMeta == null) return;

                    String playerName = ChatColor.stripColor(playerItemMeta.getDisplayName());

                    Player clickedPlayer = Bukkit.getPlayer(playerName);

                    if(clickedPlayer == null) {
                        Bukkit.getLogger().warning("[ChessBoards] Could not find clicked player " + playerName);
                        return;
                    }

                    player.closeInventory();

                    chessGame.setGameState(ChessGameState.CONFIRM_GAME);

                    if(chessGame.getPlayerQueue().contains(clickedPlayer)) {
                        chessGame.getPlayerQueue().remove(clickedPlayer);
                        clickedPlayer.closeInventory();
                    }

                    // Remove everyone else in GUI
                    for(Player player1 : chessGame.getPlayerQueue()) {
                        player1.sendMessage(ConfigMessage.MESSAGE_CHAT_GAME_ALREADY_STARTED.toString());
                        player1.closeInventory();
                    }

                    for(Player player1 : chessGame.getPlayerDecideQueue()) {
                        player1.sendMessage(ConfigMessage.MESSAGE_CHAT_GAME_ALREADY_STARTED.toString());
                        player1.closeInventory();
                    }

                    chessGame.getPlayerQueue().clear();
                    chessGame.getPlayerDecideQueue().clear();

                    chessGame.setBlackPlayer(clickedPlayer);
                    chessGame.setWhitePlayer(player);

                    // Adds game wager to chess game
                    if(pluginInstance.getEconomy() != null) {
                        Wager wager = new Wager(chessGame.getWhitePlayer(), chessGame.getBlackPlayer(), "WHITE", chessGame.getGameWager());
                        chessGame.addWager(wager);
                    }

                    chessGame.openConfirmGameInventory();

                } else  {
                    ItemStack playerItem = event.getClickedInventory().getItem(event.getRawSlot() - 27);
                    if(playerItem == null) return;

                    ItemMeta playerItemMeta = playerItem.getItemMeta();
                    if(playerItemMeta == null) return;

                    String playerName = ChatColor.stripColor(playerItemMeta.getDisplayName());
                    Player clickedPlayer = Bukkit.getPlayer(playerName);

                    if(clickedPlayer != null)
                        clickedPlayer.closeInventory();
                }

                return;
            }

            return;
        }

        if (event.getClickedInventory().getHolder() instanceof ChessJoinGameInventory) {

            ChessJoinGameInventory joinGameInventory = (ChessJoinGameInventory) event.getView().getTopInventory().getHolder();
            if(joinGameInventory == null) return;

            ChessGame chessGame = pluginInstance.getChessBoardManager().getGameByDecisionQueuePlayer(player);

            if(chessGame == null) chessGame = pluginInstance.getChessBoardManager().getGameByQueuePlayer(player);

            if(chessGame == null) return;

            event.setCancelled(true);

            // Exit button
            if(itemType.equals(Material.BARRIER)) {
                player.closeInventory();
                return;
            }

            // Join game button
            if(itemType.equals(Material.GREEN_STAINED_GLASS_PANE)) {
            	if (pluginInstance.getEconomy() != null && pluginInstance.getEconomy().getBalance(player) < chessGame.getGameWager()) {
        			player.sendMessage(ConfigMessage.MESSAGE_CHAT_NOT_ENOUGH_MONEY_ACCEPT_WAGER.toString());
        			return;
        		}
            	
                chessGame.addPlayerToQueue(player);

            	joinGameInventory.display(player, false, true);
                return;
            }

            // Decline game button
            if(itemType.equals(Material.RED_STAINED_GLASS_PANE)) {
                player.closeInventory();
                return;
            }

        }

        if (event.getClickedInventory().getHolder() instanceof ChessInGameInventory) {

            ChessInGameInventory inGameInventory = (ChessInGameInventory) event.getView().getTopInventory().getHolder();
            if(inGameInventory == null) return;

            ChessGame chessGame = inGameInventory.getChessGame();
            if(chessGame == null) return;

            event.setCancelled(true);

            if (itemType.equals(Material.BOOK)) {
                if(chessGame.getWagerViewByPlayer(player) != null) {
                    chessGame.getWagerViewByPlayer(player).display(true);
                } else {
                    ChessWagerViewInventory chessWagerViewInventory = new ChessWagerViewInventory(chessGame, player);
                    chessWagerViewInventory.display(true);
                    chessGame.addWagerViewInventory(chessWagerViewInventory);
                }
            	return;
            }

            if(itemType.equals(Material.YELLOW_STAINED_GLASS_PANE) ||
                    itemType.equals(Material.LIME_STAINED_GLASS_PANE)) {

                if(!(chessGame.getGameState().equals(ChessGameState.CONFIRM_GAME))) return;

                ItemStack playerItem = event.getClickedInventory().getItem(event.getRawSlot() - 27);
                if(playerItem == null) return;

                ItemMeta playerItemMeta = playerItem.getItemMeta();
                if(playerItemMeta == null) return;

                String playerName = ChatColor.stripColor(playerItemMeta.getDisplayName());
                Player clickedPlayer = Bukkit.getPlayer(playerName);

                if(clickedPlayer == null) {
                    Bukkit.getLogger().warning("[ChessBoards] Could not find clicked player " + playerName);
                    return;
                }

                if(clickedPlayer.equals(player)) {
                    chessGame.getChessConfirmGameInventory().togglePlayerReady(player);
                    return;
                }
                return;
            }

            if(itemType.equals(Material.RED_STAINED_GLASS_PANE)
                    && itemName.contains(ConfigMessage.MESSAGE_GUI_FFTEXT.toRawString())) {
                boolean didForfeit = chessGame.forfeitGame(player, true);
                if(!didForfeit) Bukkit.getLogger().warning("[ChessBoards] Could not forfeit game " + chessGame.getGameId());
            }
            return;
        }

		if (event.getClickedInventory().getHolder() instanceof ChessWagerViewInventory) {

            ChessWagerViewInventory chessWagerViewInventory = (ChessWagerViewInventory) event.getView().getTopInventory().getHolder();
            if(chessWagerViewInventory == null) return;

            ChessGame chessGame = chessWagerViewInventory.getChessGame();
            if(chessGame == null) return;

            event.setCancelled(true);

            RequestWager playerWager = chessGame.getRequestWagerByPlayer(player);

            if(itemType.equals(Material.BARRIER)) {
                player.closeInventory();
                return;
            }

            String increaseText = ConfigMessage.MESSAGE_GUI_WAGER_INCREASE.toRawString();
            String decreaseText = ConfigMessage.MESSAGE_GUI_WAGER_DECREASE.toRawString();

            String createText = ConfigMessage.MESSAGE_GUI_WAGER_CREATE.toRawString();
            String cancelText = ConfigMessage.MESSAGE_GUI_WAGER_CANCEL.toRawString();
            String acceptText = ConfigMessage.MESSAGE_GUI_WAGER_ACCEPT.toRawString();

            if(itemType.equals(Material.PLAYER_HEAD)
                    && (itemName.equals(decreaseText) || itemName.equals(increaseText)) && playerWager == null) {
                boolean shifting = event.getClick().equals(ClickType.SHIFT_LEFT);

                if(itemName.equals(increaseText)) {
                    chessWagerViewInventory.incrementWager(shifting);
                } else {
                    chessWagerViewInventory.decrementWager(shifting);
                }

                chessWagerViewInventory.display(false);
                return;
            }

            // Create wager
            if(itemType.equals(Material.GREEN_STAINED_GLASS_PANE)
                    && itemName.equals(createText) && playerWager == null) {

                int wager = chessWagerViewInventory.getWagerAmount();
                if(wager <= 0) return;
                
                if (pluginInstance.getEconomy().getBalance(player) < wager) {
        			player.sendMessage(ConfigMessage.MESSAGE_CHAT_NOT_ENOUGH_MONEY_CREATE_WAGER.toString());
        			return;
        		}
                
                RequestWager requestWager = new RequestWager(player, chessWagerViewInventory.getCreateWagerColor(), wager);
                chessGame.addRequestWager(requestWager);
                chessGame.updateRequestWagerInventories();
                return;
            }

            // Cancel wager
            if(itemType.equals(Material.YELLOW_STAINED_GLASS_PANE)
                    && itemName.equals(cancelText) && playerWager != null) {

                chessGame.removeRequestWager(playerWager);

                for(ChessWagerViewInventory inv : chessGame.getWagerViewInventories()) {
                    if(inv.getSelectedWager() != null && inv.getSelectedWager().equals(playerWager)) {
                        inv.setSelectedWager(null);
                    }
                }

                chessGame.updateRequestWagerInventories();
                return;
            }

            // Accept wager
            if(itemType.equals(Material.GREEN_STAINED_GLASS_PANE)
                    && itemName.equals(acceptText) && playerWager != chessWagerViewInventory.getSelectedWager()) {
            		
               boolean didCreateWager = chessGame.requestWagerToWager(chessWagerViewInventory.getSelectedWager(), player);

                if(!didCreateWager) return;

                chessWagerViewInventory.setSelectedWager(null);
                chessGame.updateRequestWagerInventories();
                return;
            }

            // Switch wager color Only if they have not created a wager yet
            if(event.getRawSlot() == 25 && playerWager == null) {
                chessWagerViewInventory.toggleWagerColor();
                chessWagerViewInventory.display(false);
                return;
            }

            if(event.getRawSlot() % 9 < 5 && playerWager == null) {
                String playerName = null;
                if(itemType.equals(Material.PLAYER_HEAD)) {
                    playerName = itemName;
                } else if((itemType.equals(Material.BLACK_WOOL) || itemType.equals(Material.WHITE_WOOL)) && event.getRawSlot() % 9 > 0) {
                    ItemStack playerItem = player.getOpenInventory().getItem(event.getRawSlot() - 1);
                    if(playerItem == null) return;

                    ItemMeta playerItemMeta = playerItem.getItemMeta();
                    if(playerItemMeta == null) return;

                    playerName = ChatColor.stripColor(playerItemMeta.getDisplayName());
                }

                if(playerName == null) return;

                Player clickedPlayer = Bukkit.getPlayer(playerName);

                if(clickedPlayer == null) return;

                RequestWager requestWager = chessGame.getRequestWagerByPlayer(clickedPlayer);

                if(requestWager == null) {
                    player.sendMessage("Could not find request wager! This should never happen. Please contact plugin authors.");
                    return;
                }

                // If the player re-selects a wager, deselect it
                if(chessWagerViewInventory.getSelectedWager() != null) {
                    if(chessWagerViewInventory.getSelectedWager().equals(requestWager)) {
                        chessWagerViewInventory.setSelectedWager(null);
                        chessWagerViewInventory.display(false);
                        return;
                    }
                }

                chessWagerViewInventory.setSelectedWager(requestWager);
                chessWagerViewInventory.display(false);
            }
        }
    }

}
