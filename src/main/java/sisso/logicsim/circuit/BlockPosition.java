package sisso.logicsim.circuit;

/** Integer world coordinates; each evaluator operates on exactly one world. */
public record BlockPosition(int x, int y, int z) {
    public BlockPosition neighbor(HorizontalDirection direction) {
        return new BlockPosition(x + direction.dx(), y, z + direction.dz());
    }
}
