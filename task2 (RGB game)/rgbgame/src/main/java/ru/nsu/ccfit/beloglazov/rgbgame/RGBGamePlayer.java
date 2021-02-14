package ru.nsu.ccfit.beloglazov.rgbgame;

import com.sun.tools.javac.util.Pair;
import java.util.LinkedList;
import java.util.Scanner;
import ru.nsu.ccfit.beloglazov.rgbgame.exceptions.InvalidCharacterException;
import ru.nsu.ccfit.beloglazov.rgbgame.exceptions.InvalidFieldWidthException;

public class RGBGamePlayer {
    // ID of game
    private int gameID;
    // sizes of game field
    private final int fieldWidth = 15;
    private final int fieldHeight = 10;
    // field of colored balls
    private char[][] field;
    // clusterIDs[x][y] == ID of cluster that ball (x,y) belongs to
    // -1 == ball belongs to no clusters
    private int [][] clustersIDs;
    // list of clusters
    // one cluster is: color + list of balls (their coordinates)
    private LinkedList<Pair<Character, LinkedList<Pair<Integer, Integer>>>> clusters;

    public RGBGamePlayer(int gameID) {
        this.gameID = gameID;
        field = new char[fieldHeight][fieldWidth];
        clustersIDs = new int[fieldHeight][fieldWidth];
        clusters = new LinkedList<>();
    }

    public void initField() throws InvalidFieldWidthException, InvalidCharacterException {
        Scanner scanner = new Scanner(System.in);
        // filling field with colored balls
        for (int i = 0; i < fieldHeight; i++) {
            String nextLine = scanner.nextLine();
            if (nextLine.length() != fieldWidth) {
                throw new InvalidFieldWidthException("ERROR :: EXPECTED " + fieldWidth + " SYMBOLS, GOT " + nextLine.length());
            }
            for (int j = 0; j < fieldWidth; j++) {
                char color = nextLine.charAt(j);
                if (color != 'R' && color != 'G' && color != 'B') {
                    throw new InvalidCharacterException("ERROR :: EXPECTED 'R'/'G'/'B', GOT '" + color + "'");
                } else {
                    // setting color for every ball
                    field[i][j] = color;
                    // setting -1 as cluster ID for every ball
                    clustersIDs[i][j] = -1;
                }
            }
        }
    }

    public void play() {
        int score = 0;
        int numOfMoves = 0;
        formClusters();
        System.out.println("Game " + gameID + ":");
        while (clusters.size() > 0) {
            // finding cluster with max number of balls
            Pair<Character, LinkedList<Pair<Integer, Integer>>> maxCluster = findMaxCluster();
            // getting first ball of max-cluster
            Pair<Integer, Integer> firstBall = maxCluster.snd.get(0);
            int maxClusterSize = maxCluster.snd.size();
            int points = (maxClusterSize - 2) * (maxClusterSize - 2);
            score += points;
            // removing balls of max cluster from field
            removeCluster(maxCluster);
            // printing info about move
            System.out.println("Move " + ++numOfMoves + " at (" + firstBall.fst + "," + firstBall.snd + "): removed " +
                    maxClusterSize + " balls of color " + maxCluster.fst + ", got " + points + " points.");
            // moving balls down & left
            moveBalls();
            // re-forming clusters
            formClusters();
        }
        // checking if there are remaining balls
        int numOfRemainingBalls = getNumOfRemainingBalls();
        if (numOfRemainingBalls == 0) {
            score += 1000;
        }
        // final print
        System.out.println("Final score: " + score + ", with " + numOfRemainingBalls + " balls remaining.");
    }

    private void formClusters() {
        // removing all clusters from list
        clusters.clear();
        for (int i = 0; i < fieldHeight; i++) {
            for (int j = 0; j < fieldWidth; j++) {
                char currentColor = field[i][j];
                if (currentColor == 'E') {
                    // will not form clusters with empty balls
                    continue;
                }
                // list for clusters that current ball fits to
                LinkedList<Pair<Character, LinkedList<Pair<Integer, Integer>>>> fitClusters = new LinkedList<>();
                int leftClusterID = -1;
                int upClusterID = -1;
                if (j > 0 && field[i][j-1] == currentColor) {
                    leftClusterID = clustersIDs[i][j-1];
                }
                if (i > 0 && field[i-1][j] == currentColor) {
                    upClusterID = clustersIDs[i-1][j];
                }
                if (leftClusterID != -1) {
                    fitClusters.add(clusters.get(leftClusterID));
                }
                if (leftClusterID != upClusterID && upClusterID != -1) {
                    fitClusters.add(clusters.get(upClusterID));
                }
                // uniting fit-clusters
                Pair<Character, LinkedList<Pair<Integer, Integer>>> unitedCluster = new Pair<>(currentColor, new LinkedList<>());
                for (Pair<Character, LinkedList<Pair<Integer, Integer>>> cluster : fitClusters) {
                    unitedCluster.snd.addAll(cluster.snd);
                    clusters.remove(cluster);
                }
                //  adding current ball to united fit-cluster
                unitedCluster.snd.add(new Pair<>(i, j));
                // adding united fit-cluster to clusters-list
                clusters.add(unitedCluster);
                // setting actual cluster ID for checked balls
                setClustersIDs();
            }
        }
        delClustersOfOneBall();
    }

    private void setClustersIDs() {
        // setting cluster ID for every ball from current list of clusters (-1 if current ball does not belong to any cluster)
        for (Pair<Character, LinkedList<Pair<Integer, Integer>>> cluster : clusters) {
            int clusterID = clusters.indexOf(cluster);
            for (Pair<Integer, Integer> ball : cluster.snd) {
                clustersIDs[ball.fst][ball.snd] = clusterID;
            }
        }
    }

    private void delClustersOfOneBall() {
        // removing clusters with only one ball
        clusters.removeIf(currentCluster -> currentCluster.snd.size() == 1);
        // setting actual cluster ID for every ball
        setClustersIDs();
    }

    private Pair<Character, LinkedList<Pair<Integer, Integer>>> findMaxCluster() {
        // finding cluster with max number of balls
        Pair<Character, LinkedList<Pair<Integer, Integer>>> maxCluster = clusters.get(0);
        for (Pair<Character, LinkedList<Pair<Integer, Integer>>> currentCluster : clusters) {
            if (currentCluster.snd.size() > maxCluster.snd.size()) {
                maxCluster = currentCluster;
            }
        }
        return maxCluster;
    }

    private void removeCluster(Pair<Character, LinkedList<Pair<Integer, Integer>>> cluster) {
        // removing balls from cluster & cluster itself from list
        for (Pair<Integer, Integer> currentBall : cluster.snd) {
            field[currentBall.fst][currentBall.snd] = 'E';  // == empty
        }
        clusters.remove(cluster);
    }

    private void moveBalls() {
        downMove();
        // checking if there are empty columns
        for (int j = 0; j < fieldWidth; j++) {
            int count = 0;
            for (int i = 0; i < fieldHeight; i++) {
                if (field[i][j] == 'E') {
                    count++;
                }
            }
            if (count == fieldHeight) {
                leftMove(j);
            }
        }
    }

    private void downMove() {
        // for every ball
        for (int i = 0; i < fieldHeight; i++) {
            for (int j = 0; j < fieldWidth; j++) {
                if (field[i][j] == 'E') {
                    // if ball has no color (empty) => making all balls above "fall" one row down
                    for (int k = i; k >= 1; k--) {
                        swapBalls(k, j, k-1, j);
                    }
                }
            }
        }
    }

    private void leftMove(int column) {
        // moving all balls on right of column to left (filling empty column)
        for (int i = 0; i < fieldHeight; i++) {
            for (int j = column; j < fieldWidth-1; j++) {
                swapBalls(i, j, i, j+1);
            }
        }
    }

    private void swapBalls(int row1, int column1, int row2, int column2) {
        char tmp = field[row1][column1];
        field[row1][column1] = field[row2][column2];
        field[row2][column2] = tmp;
    }

    private int getNumOfRemainingBalls() {
        int count = 0;
        for (int i = 0; i < fieldHeight; i++) {
            for (int j = 0; j < fieldWidth; j++) {
                if (field[i][j] != 'E') {
                    count++;
                }
            }
        }
        return count;
    }
}