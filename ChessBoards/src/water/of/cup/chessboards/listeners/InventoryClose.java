package water.of.cup.chessboards.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import water.of.cup.chessboards.ChessBoards;
import water.of.cup.chessboards.chessBoard.ChessBoardManager;
import water.of.cup.chessboards.chessBoard.ChessGame;
import water.of.cup.chessboards.chessBoard.ChessGameState;
import water.of.cup.chessboards.chessBoard.Wager;

import java.util.HashSet;
import java.util.Set;

public class InventoryClose implements Listener {

    private ChessBoards instance = ChessBoards.getInstance();
    private ChessBoardManager chessBoardManager = instance.getChessBoardManager();

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        // Player closing create game menu
        if(instance.playerHasCreateGame(player)) {
            // player.sendMessage("Leaving create game");
            instance.removeCreateGamePlayer(player);
            return;
        }

        // Player closing waiting menu
        if(chessBoardManager.getGameByPlayer(player) != null
                && chessBoardManager.getGameByPlayer(player).getGameState().equals(ChessGameState.WAITING_PLAYER)) {
            ChessGame chessGame = chessBoardManager.getGameByPlayer(player);

            // player.sendMessage("Leaving waiting player menu");

            chessGame.setWhitePlayer(null);
            chessGame.setGameState(ChessGameState.IDLE);

            Set<Player> playerQueueCopy = new HashSet<>(chessGame.getPlayerQueue());
            for(Player player1 : playerQueueCopy) {
                // player1.sendMessage("Game owner has ended the game.");
                player1.closeInventory();
            }

            Set<Player> playerDecideQueueCopy = new HashSet<>(chessGame.getPlayerDecideQueue());
            for(Player player1 : playerDecideQueueCopy) {
                // player1.sendMessage("Game owner has ended the game.");
                player1.closeInventory();
            }

            chessGame.getPlayerQueue().clear();
            chessGame.getPlayerDecideQueue().clear();
            return;
        }

        // Player closing confirm game menu
        if(chessBoardManager.getGameByPlayer(player) != null
                && chessBoardManager.getGameByPlayer(player).getGameState().equals(ChessGameState.CONFIRM_GAME)) {
            ChessGame chessGame = chessBoardManager.getGameByPlayer(player);

            chessGame.forfeitGame(player, false);
        }

        // Player is leaving decision screen
        if(chessBoardManager.getGameByDecisionQueuePlayer(player) != null) {
            chessBoardManager.getGameByDecisionQueuePlayer(player).getPlayerDecideQueue().remove(player);
            return;
        }

        // Player is leaving the queue
        if(chessBoardManager.getGameByQueuePlayer(player) != null) {
            chessBoardManager.getGameByQueuePlayer(player).removePlayerFromQueue(player);
            return;
        }
    }

}
