package chess;

import java.util.Arrays;

public class ChessBoard {

    private ChessPiece[][] squares = new ChessPiece[8][8];

    public ChessBoard() {}

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ChessBoard b && Arrays.deepEquals(squares, b.squares));
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(squares);
    }

    public void addPiece(ChessPosition position, ChessPiece piece) {
        squares[position.getRow() - 1][position.getColumn() - 1] = piece;
    }

    public ChessPiece getPiece(ChessPosition position) {
        return squares[position.getRow() - 1][position.getColumn() - 1];
    }

    public void resetBoard() {
        squares = new ChessPiece[8][8];

        for (int c = 1; c <= 8; c++) {
            p(2, c, ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
            p(7, c, ChessGame.TeamColor.BLACK, ChessPiece.PieceType.PAWN);
        }

        ChessPiece.PieceType[] back = {
                ChessPiece.PieceType.ROOK, ChessPiece.PieceType.KNIGHT, ChessPiece.PieceType.BISHOP,
                ChessPiece.PieceType.QUEEN, ChessPiece.PieceType.KING,
                ChessPiece.PieceType.BISHOP, ChessPiece.PieceType.KNIGHT, ChessPiece.PieceType.ROOK
        };

        for (int c = 1; c <= 8; c++) {
            p(1, c, ChessGame.TeamColor.WHITE, back[c - 1]);
            p(8, c, ChessGame.TeamColor.BLACK, back[c - 1]);
        }
    }

    private void p(int r, int c, ChessGame.TeamColor color, ChessPiece.PieceType type) {
        addPiece(new ChessPosition(r, c), new ChessPiece(color, type));
    }
}
