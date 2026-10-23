package sisso.logicsim.circuit;

public enum HorizontalDirection {
    NORTH(1, 0, -1), EAST(2, 1, 0), SOUTH(4, 0, 1), WEST(8, -1, 0);

    private final int bit;
    private final int dx;
    private final int dz;

    HorizontalDirection(int bit, int dx, int dz) {
        this.bit = bit;
        this.dx = dx;
        this.dz = dz;
    }

    public int bit() { return bit; }
    public int dx() { return dx; }
    public int dz() { return dz; }
    public HorizontalDirection opposite() { return values()[(ordinal() + 2) & 3]; }
}
