package chess;

import java.util.Objects;

public class ChessPosition {
    private final int row, col;

    public ChessPosition(int row, int col) { this.row = row; this.col = col; }

    public int getRow() { return row; }
    public int getColumn() { return col; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ChessPosition p && row == p.row && col == p.col);
    }

    @Override
    public int hashCode() { return Objects.hash(row, col); }

    @Override
    public String toString() { return "[" + row + "," + col + "]"; }
}
