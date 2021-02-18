package ru.nsu.ccfit.beloglazov.treasurehunt;

import java.util.Objects;

public class Wall {
    public WALL_TYPE type;
    public RatPoint p1;
    public RatPoint p2;

    public Wall(double p1x, double p1y, double p2x, double p2y, WALL_TYPE type) {
        if (p1x < p2x) {
            p1 = new RatPoint(p1x, p1y);
            p2 = new RatPoint(p2x, p2y);
        }
        if (p1x > p2x) {
            p1 = new RatPoint(p2x, p2y);
            p2 = new RatPoint(p1x, p1y);
        }
        if (p1x == p2x) {
            if (p1y < p2y) {
                p1 = new RatPoint(p1x, p1y);
                p2 = new RatPoint(p2x, p2y);
            } else {
                p1 = new RatPoint(p2x, p2y);
                p2 = new RatPoint(p1x, p1y);
            }
        }

        this.type = type;
    }

    public Wall(RatPoint p1, RatPoint p2, WALL_TYPE type) {
        this(p1.x, p1.y, p2.x, p2.y, type);
    }

    public boolean intersects(Wall other) {
        return !intersection(other).containsNaNs();
    }

    public RatPoint intersection(Wall other) {
        double x1 = p1.x, y1 = p1.y;
        double x2 = p2.x, y2 = p2.y;
        double x3 = other.p1.x, y3 = other.p1.y;
        double x4 = other.p2.x, y4 = other.p2.y;
        double n;
        if (y2 - y1 != 0) { // a(y)
            double q = (x2 - x1) / (y1 - y2);
            double sn = (x3 - x4) + (y3 - y4) * q;
            if (sn == 0) {
                return new RatPoint(Double.NaN, Double.NaN);
            }   // c(x) + c(y)*q
            double fn = (x3 - x1) + (y3 - y1) * q;  // b(x) + b(y)*q
            n = fn / sn;
        }
        else {
            if ((y3 - y4) == 0) {
                return new RatPoint(Double.NaN, Double.NaN);
            }   // b(y)
            n = (y3 - y1) / (y3 - y4);   // c(y)/b(y)
        }
        double x5 = x3 + (x4 - x3) * n; // x3 + (-b(x))*n
        double y5 = y3 + (y4 - y3) * n; // y3 +(-b(y))*n
        RatPoint intersection = new RatPoint(x5, y5);
        if (contains(intersection) && other.contains(intersection)) {
            return intersection;
        } else {
            return new RatPoint(Double.NaN, Double.NaN);
        }
    }

    public boolean contains(RatPoint point) {
        // line is vertical or horizontal
        if ((equality(p1.x, p2.x, 0.001) && equality(p1.x, point.x, 0.001)
                && point.y >= Math.min(p1.y, p2.y)
                && point.y <= Math.max(p1.y, p2.y))
                || (equality(p1.y, p2.y, 0.001) && equality(p1.y, point.y, 0.001)
                && point.x >= Math.min(p1.x, p2.x)
                && point.x <= Math.max(p1.x, p2.x))) {
            return true;
        }
        // (x-x1)/(x2-x1)=(y-y1)/(y2-y1)? - is point on LINE
        // + are coordinates in ranges? - is point on SEGMENT
        double v1 = (point.x - p1.x) / (p2.x - p1.x);
        double v2 = (point.y - p1.y) / (p2.y - p1.y);
        boolean check1 = equality(v1, v2, 0.0001);
        boolean check2 = point.x >= Math.min(p1.x, p2.x) && point.x <= Math.max(p1.x, p2.x)
                && point.y >= Math.min(p1.y, p2.y)
                && point.y <= Math.max(p1.y, p2.y);
        return check1 && check2;
    }

    public RatPoint mid() {
        double midX = (p1.x + p2.x) / 2;
        double midY = (p1.y + p2.y) / 2;
        return new RatPoint(midX, midY);
    }

    private boolean equality(double value1, double value2, double epsilon) {
        return Math.abs(value1 - value2) < epsilon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wall wall = (Wall) o;
        return Objects.equals(p1, wall.p1) && Objects.equals(p2, wall.p2);
    }

    @Override
    public int hashCode() { return Objects.hash(p1, p2); }

    @Override
    public String toString() { return "Wall{p1=" + p1 + ",p2=" + p2 + '}'; }
}