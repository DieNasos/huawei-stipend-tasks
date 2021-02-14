package ru.nsu.ccfit.beloglazov.rgbgame;

import ru.nsu.ccfit.beloglazov.rgbgame.exceptions.InvalidCharacterException;
import ru.nsu.ccfit.beloglazov.rgbgame.exceptions.InvalidFieldWidthException;

import java.util.LinkedList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        LinkedList<RGBGamePlayer> players = new LinkedList<>();
        Scanner scanner = new Scanner(System.in);
        int numOfGames = Integer.parseInt(scanner.nextLine());
        for (int i = 1; i <= numOfGames; i++) {
            scanner.nextLine();
            RGBGamePlayer player = new RGBGamePlayer(i);
            try {
                player.initField();
            } catch (InvalidFieldWidthException | InvalidCharacterException e) {
                System.out.println(e.getMessage());
                return;
            }
            players.add(player);
        }
        for (RGBGamePlayer player : players) {
            player.play();
            System.out.println();
        }
    }
}