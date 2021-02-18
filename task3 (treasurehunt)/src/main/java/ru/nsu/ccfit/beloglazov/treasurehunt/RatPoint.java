package ru.nsu.ccfit.beloglazov.treasurehunt;

import java.util.Objects;

public class RatPoint implements Comparable<RatPoint> {
    public double x;
    public double y;

    public RatPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean containsNaNs() {
        return !Double.isNaN(x) && !Double.isNaN(y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RatPoint ratPoint = (RatPoint) o;
        return Double.compare(ratPoint.x, x) == 0 && Double.compare(ratPoint.y, y) == 0;
    }

    @Override
    public int hashCode() { return Objects.hash(x, y); }

    @Override
    public String toString() { return "RatPoint{x=" + x + ",y=" + y + '}'; }

    @Override
    public int compareTo(RatPoint o) {
        if (x < o.x) {
            return -1;
        } else if (x > o.x) {
            return 1;
        } else {
            return Double.compare(y, o.y);
        }
    }
}