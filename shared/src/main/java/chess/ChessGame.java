package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class ChessGame {

    private TeamColor teamTurn = TeamColor.WHITE;
    private ChessBoard board = new ChessBoard();
    private boolean gameOver = false;

    public ChessGame() {
        board.resetBoard();
    }

    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    public void setTeamTurn(TeamColor team) {
        teamTurn = team;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public enum TeamColor { WHITE, BLACK }

    public Collection<ChessMove> validMoves(ChessPosition start) {
        if (start == null) return null;
        ChessPiece p = board.getPiece(start);
        if (p == null) return null;

        Collection<ChessMove> raw = p.pieceMoves(board, start);
        if (raw == null) return null;

        var legal = new ArrayList<ChessMove>();
        for (ChessMove m : raw) {
            ChessBoard cp = cp(board);
            mv(cp, m, p.getTeamColor());
            if (!chk(cp, p.getTeamColor())) legal.add(m);
        }
        return legal;
    }

    public void makeMove(ChessMove move) throws InvalidMoveException {
        if (gameOver) {
            throw new InvalidMoveException("Game is over");
        }

        if (move == null || move.getStartPosition() == null || move.getEndPosition() == null) {
            throw new InvalidMoveException("Bad move");
        }

        ChessPosition from = move.getStartPosition();
        ChessPiece p = board.getPiece(from);
        if (p == null) throw new InvalidMoveException("No piece");
        if (p.getTeamColor() != teamTurn) throw new InvalidMoveException("Wrong turn");

        Collection<ChessMove> ok = validMoves(from);
        boolean found = false;
        if (ok != null) {
            for (ChessMove m : ok) {
                if (m.equals(move)) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) throw new InvalidMoveException("Illegal");

        mv(board, move, p.getTeamColor());
        teamTurn = opp(teamTurn);
    }

    public boolean isInCheck(TeamColor teamColor) {
        return chk(board, teamColor);
    }

    public boolean isInCheckmate(TeamColor teamColor) {
        if (teamColor == null || !chk(board, teamColor)) return false;
        return !hasMove(teamColor);
    }

    public boolean isInStalemate(TeamColor teamColor) {
        if (teamColor == null || chk(board, teamColor)) return false;
        return !hasMove(teamColor);
    }

    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    public ChessBoard getBoard() {
        return board;
    }

    private boolean hasMove(TeamColor t) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = board.getPiece(pos);
                if (p != null && p.getTeamColor() == t) {
                    Collection<ChessMove> ms = validMoves(pos);
                    if (ms != null && !ms.isEmpty()) return true;
                }
            }
        }
        return false;
    }

    private static TeamColor opp(TeamColor t) {
        return t == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
    }

    private static ChessBoard cp(ChessBoard o) {
        ChessBoard b = new ChessBoard();
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = o.getPiece(pos);
                if (p != null) {
                    b.addPiece(pos, new ChessPiece(p.getTeamColor(), p.getPieceType()));
                }
            }
        }
        return b;
    }

    private static void mv(ChessBoard b, ChessMove m, TeamColor mover) {
        ChessPosition from = m.getStartPosition();
        ChessPosition to = m.getEndPosition();
        ChessPiece p = b.getPiece(from);
        b.addPiece(from, null);

        ChessPiece.PieceType promo = m.getPromotionPiece();
        if (promo != null && p != null && p.getPieceType() == ChessPiece.PieceType.PAWN) {
            b.addPiece(to, new ChessPiece(mover, promo));
        } else {
            b.addPiece(to, p);
        }
    }

    private static boolean chk(ChessBoard b, TeamColor t) {
        if (t == null) return false;
        ChessPosition k = king(b, t);
        if (k == null) return true;

        TeamColor e = opp(t);
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = b.getPiece(pos);
                if (p != null && p.getTeamColor() == e) {
                    Collection<ChessMove> ms = p.pieceMoves(b, pos);
                    if (ms == null) continue;
                    for (ChessMove m : ms) {
                        if (k.equals(m.getEndPosition())) return true;
                    }
                }
            }
        }
        return false;
    }

    private static ChessPosition king(ChessBoard b, TeamColor t) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = b.getPiece(pos);
                if (p != null && p.getTeamColor() == t && p.getPieceType() == ChessPiece.PieceType.KING) {
                    return pos;
                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ChessGame g
                && teamTurn == g.teamTurn
                && gameOver == g.gameOver
                && Objects.equals(board, g.board));
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamTurn, board, gameOver);
    }
}