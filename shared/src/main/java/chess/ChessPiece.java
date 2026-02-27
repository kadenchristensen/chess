package chess;

import java.util.*;

public class ChessPiece {
    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    public enum PieceType { KING, QUEEN, BISHOP, KNIGHT, ROOK, PAWN }

    public ChessGame.TeamColor getTeamColor() { return pieceColor; }
    public PieceType getPieceType() { return type; }

    private static final int[][] DIAG = {{1,1},{1,-1},{-1,1},{-1,-1}};
    private static final int[][] ORTH = {{1,0},{-1,0},{0,1},{0,-1}};
    private static final int[][] KN   = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
    private static final int[][] KI   = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};

    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition from) {
        var moves = new ArrayList<ChessMove>();
        switch (type) {
            case BISHOP -> slide(board, from, moves, DIAG);
            case ROOK   -> slide(board, from, moves, ORTH);
            case QUEEN  -> { slide(board, from, moves, DIAG); slide(board, from, moves, ORTH); }
            case KNIGHT -> jump(board, from, moves, KN);
            case KING   -> jump(board, from, moves, KI);
            case PAWN   -> pawn(board, from, moves);
        }
        return moves;
    }

    private void slide(ChessBoard b, ChessPosition from, List<ChessMove> out, int[][] dirs) {
        for (int[] d : dirs) {
            for (int r = from.getRow()+d[0], c = from.getColumn()+d[1]; in(r,c); r += d[0], c += d[1]) {
                ChessPosition to = new ChessPosition(r,c);
                ChessPiece occ = b.getPiece(to);
                if (occ == null) out.add(new ChessMove(from, to, null));
                else { if (occ.pieceColor != pieceColor) out.add(new ChessMove(from, to, null)); break; }
            }
        }
    }

    private void jump(ChessBoard b, ChessPosition from, List<ChessMove> out, int[][] offs) {
        for (int[] o : offs) {
            int r = from.getRow()+o[0], c = from.getColumn()+o[1];
            if (!in(r,c)) continue;
            addIfFreeOrEnemy(b, from, new ChessPosition(r,c), out);
        }
    }

    private void pawn(ChessBoard b, ChessPosition from, List<ChessMove> out) {
        int dir = (pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int start = (pieceColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
        int promo = (pieceColor == ChessGame.TeamColor.WHITE) ? 8 : 1;

        int r1 = from.getRow() + dir, c = from.getColumn();
        ChessPosition one = new ChessPosition(r1, c);

        if (in(r1,c) && b.getPiece(one) == null) {
            pawnAdvance(from, one, out, promo);
            int r2 = from.getRow() + 2*dir;
            ChessPosition two = new ChessPosition(r2, c);
            if (from.getRow() == start && in(r2,c) && b.getPiece(two) == null) out.add(new ChessMove(from, two, null));
        }

        for (int dc : new int[]{-1, 1}) {
            int cc = c + dc;
            if (!in(r1, cc)) continue;
            ChessPosition to = new ChessPosition(r1, cc);
            ChessPiece occ = b.getPiece(to);
            if (occ != null && occ.pieceColor != pieceColor) pawnAdvance(from, to, out, promo);
        }
    }

    private void pawnAdvance(ChessPosition from, ChessPosition to, List<ChessMove> out, int promoRow) {
        if (to.getRow() != promoRow) { out.add(new ChessMove(from, to, null)); return; }
        out.add(new ChessMove(from, to, PieceType.QUEEN));
        out.add(new ChessMove(from, to, PieceType.ROOK));
        out.add(new ChessMove(from, to, PieceType.BISHOP));
        out.add(new ChessMove(from, to, PieceType.KNIGHT));
    }

    private void addIfFreeOrEnemy(ChessBoard b, ChessPosition from, ChessPosition to, List<ChessMove> out) {
        ChessPiece occ = b.getPiece(to);
        if (occ == null || occ.pieceColor != pieceColor) out.add(new ChessMove(from, to, null));
    }

    private static boolean in(int r, int c) { return r>=1 && r<=8 && c>=1 && c<=8; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ChessPiece p && pieceColor == p.pieceColor && type == p.type);
    }

    @Override
    public int hashCode() { return Objects.hash(pieceColor, type); }
}
