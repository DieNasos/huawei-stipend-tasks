package ru.nsu.ccfit.beloglazov.treasurehunt;

import java.util.ArrayList;
import java.util.Objects;

public class GraphNode {
    public RatPoint point;
    public Wall wall;
    public ArrayList<GraphNode> neighbours;
    public boolean checked = false;
    public int weight;

    public GraphNode(RatPoint point, Wall wall) {
        this.point = point;
        this.wall = wall;
        neighbours = new ArrayList<>();
    }

    public void addNeighbour(GraphNode neighbour) { neighbours.add(neighbour); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GraphNode(point=").append(point).append(",neighbours=");
        for (GraphNode neighbour : neighbours) {
            sb.append(neighbour.point);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return point == graphNode.point;
    }

    @Override
    public int hashCode() {
        return Objects.hash(point, wall, neighbours, checked);
    }
}