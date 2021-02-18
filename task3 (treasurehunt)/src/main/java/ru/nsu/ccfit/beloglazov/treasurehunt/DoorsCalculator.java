package ru.nsu.ccfit.beloglazov.treasurehunt;

import java.util.*;

public class DoorsCalculator {
    // list of ((x1, y1), (x2, y2)) - coordinates of big walls' end-points
    private final LinkedList<Wall> bigWalls;
    // list of coordinates of small walls' end-points
    private final HashSet<Wall> smallWalls;
    // coordinates of treasure
    private RatPoint treasure;
    // graph of treasure coordinates + walls' mid-points
    private final LinkedList<GraphNode> graph;
    // node for treasure
    private GraphNode treasureNode;

    public DoorsCalculator() {
        bigWalls = new LinkedList<>();
        smallWalls = new HashSet<>();
        graph = new LinkedList<>();
        // initializing corners of square
        RatPoint[] corners = new RatPoint[4];
        corners[0] = new RatPoint(0.0, 0.0);
        corners[1] = new RatPoint(0.0, 100.0);
        corners[2] = new RatPoint(100.0, 100.0);
        corners[3] = new RatPoint(100.0, 0.0);
        // adding outer walls
        bigWalls.add(new Wall(corners[0], corners[1], WALL_TYPE.OUTER));
        bigWalls.add(new Wall(corners[1], corners[2], WALL_TYPE.OUTER));
        bigWalls.add(new Wall(corners[2], corners[3], WALL_TYPE.OUTER));
        bigWalls.add(new Wall(corners[3], corners[0], WALL_TYPE.OUTER));
    }

    public void input() {
        inputWalls();
        inputTreasureCoordinates();
    }

    private void inputWalls() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("INPUT NUMBER OF INNER WALLS:");
        int numOfInnerWalls = scanner.nextInt();
        for (int i = 1; i <= numOfInnerWalls; i++) {
            System.out.println("INPUT COORDINATES OF INNER WALL " + i + " END-POINTS (X1, Y1, X2, Y2):");
            double x1 = scanner.nextInt();
            double y1 = scanner.nextInt();
            double x2 = scanner.nextInt();
            double y2 = scanner.nextInt();
            bigWalls.add(new Wall(x1, y1, x2, y2, WALL_TYPE.INNER));
        }
    }

    private void inputTreasureCoordinates() {
        System.out.println("INPUT COORDINATES OF TREASURE:");
        Scanner scanner = new Scanner(System.in);
        double x = scanner.nextDouble();
        double y = scanner.nextDouble();
        treasure = new RatPoint(x, y);
    }

    public void countNumOfBlasts() {
        setSmallWalls();
        //System.out.println(smallWalls);
        fillGraph();
        //System.out.println(graph.toString());
        // preparing for using Dijkstra's algorithm
        for (GraphNode node: graph) {
            node.weight = Integer.MAX_VALUE;
        }
        treasureNode.weight = 0;
        // using Dijkstra's algorithm for treasure-node
        dijkstra(treasureNode);
        // finding shortest way from treasure to any outer wall
        int shortestWay = Integer.MAX_VALUE;
        for (GraphNode node: graph) {
            if (node.wall != null) {
                if (node.wall.type == WALL_TYPE.OUTER) {
                    if (node.weight < shortestWay) {
                        shortestWay = node.weight;
                    }
                }
            }
        }
        // printing shortest way == number of doors we have to make
        System.out.println("Number of doors: " + shortestWay);
    }

    private void setSmallWalls() {
        // going through all walls
        for (Wall wall1 : bigWalls) {
            // creating list for points on current wall (ends + intersections)
            LinkedList<RatPoint> points = new LinkedList<>();
            // adding wall's end-points
            points.add(wall1.p1);
            points.add(wall1.p2);
            // going through all walls
            for (Wall wall2 : bigWalls) {
                // next iteration if wall1 == wall2
                if (wall1.equals(wall2)) {
                    continue;
                }
                // finding walls' intersection point
                RatPoint intersection = wall1.intersection(wall2);
                // if walls actually intersect -> adding this point
                if (intersection.containsNaNs() && !points.contains(intersection)) {
                    points.add(intersection);
                }
            }
            // sorting points
            Collections.sort(points);
            // dividing big wall on small ones (from point to point)
            for (int i = 0; i < points.size() - 1; i++) {
                WALL_TYPE type = wall1.type;
                Wall newWall = new Wall(points.get(i).x, points.get(i).y, points.get(i+1).x, points.get(i+1).y, type);
                smallWalls.add(newWall);
            }
        }
    }

    private void fillGraph() {
        // adding treasure in graph
        treasureNode = new GraphNode(treasure, null);
        graph.add(treasureNode);
        // adding mid-points of small walls in graph
        for (Wall wall : smallWalls) {
            graph.add(new GraphNode(wall.mid(), wall));
        }
        setNeighbours(treasureNode);
        // setting all nodes unchecked
        for (GraphNode node : graph) {
            node.checked = false;
        }
    }

    private void setNeighbours(GraphNode node) {
        if (node.checked) {
            return;
        }
        // going through all nodes in graph...
        for (GraphNode next : graph) {
            // ...except of original one
            if (node == next || next.checked) {
                continue;
            }
            // building 'phantom' wall that unites original point and current
            Wall phantomWall = new Wall(node.point, next.point, WALL_TYPE.PHANTOM);
            // checking if this 'phantom' wall intersects at lease one wall
            boolean intersection = false;
            for (Wall wall : smallWalls) {
                if (!phantomWall.intersects(wall)) {
                    if (wall != node.wall && wall != next.wall) {
                        intersection = true;
                    }
                }
            }
            // if no intersections were detected -> we cen go from original point to current
            if (!intersection) {
                // 0. node ... next
                if (!node.neighbours.contains(next)) {
                    // 1. node -> next
                    node.addNeighbour(next);
                    node.checked = true;
                    if (!next.neighbours.contains(node)) {
                        // 2. node <-> next
                        next.addNeighbour(node);
                    }
                }
            }
        }
        // recursive call for collected neighbours
        for (GraphNode next : node.neighbours) {
            setNeighbours(next);
        }
    }

    private void dijkstra(GraphNode node) {
        // correcting node's weight
        for (GraphNode neighbour: node.neighbours) {
            if (neighbour.weight != Integer.MAX_VALUE) {
                if (neighbour.weight + 1 < node.weight) {
                    node.weight = neighbour.weight + 1;
                }
            }
        }
        // correcting neighbours' weights
        for (GraphNode neighbour : node.neighbours) {
            if (neighbour.weight > node.weight + 1) {
                neighbour.weight = node.weight + 1;
            }
        }
        // marking current node checked
        node.checked = true;
        // recursive call for unchecked neighbours
        for (GraphNode neighbour : node.neighbours) {
            if (!neighbour.checked) {
                dijkstra(neighbour);
            }
        }
    }
}