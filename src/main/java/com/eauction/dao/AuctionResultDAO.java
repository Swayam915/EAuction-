package com.eauction.dao;

import com.eauction.model.AuctionResult;
import com.eauction.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionResultDAO {

    private static final Logger LOG = Logger.getLogger(AuctionResultDAO.class.getName());

    private static final String BASE_SELECT =
        "SELECT ar.*, p.product_name, u.name AS winner_name "
        + "FROM auction_results ar "
        + "JOIN products p ON ar.product_id = p.product_id "
        + "LEFT JOIN users u ON ar.winner_id = u.user_id ";

    public List<AuctionResult> getAllResults() {
        final String sql = BASE_SELECT + "ORDER BY ar.result_date DESC";
        List<AuctionResult> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapResult(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getAllResults() SQL error", e);
        }
        return list;
    }

    public AuctionResult getResultByProduct(int productId) {
        final String sql = BASE_SELECT + "WHERE ar.product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResult(rs);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getResultByProduct() SQL error, pid=" + productId, e);
        }
        return null;
    }

    public List<AuctionResult> getResultsByWinner(int winnerId) {
        final String sql = BASE_SELECT
            + "WHERE ar.winner_id = ? ORDER BY ar.result_date DESC";
        List<AuctionResult> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, winnerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResult(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "getResultsByWinner() SQL error, uid=" + winnerId, e);
        }
        return list;
    }

    private AuctionResult mapResult(ResultSet rs) throws SQLException {
        AuctionResult ar = new AuctionResult();
        ar.setResultId(rs.getInt("result_id"));
        ar.setProductId(rs.getInt("product_id"));
        ar.setProductName(rs.getString("product_name"));
        ar.setWinnerId(rs.getInt("winner_id"));
        ar.setWinnerName(rs.getString("winner_name"));
        ar.setFinalPrice(rs.getDouble("final_price"));
        Timestamp ts = rs.getTimestamp("result_date");
        if (ts != null) ar.setResultDate(ts.toLocalDateTime());
        return ar;
    }
}
