package tn.esprit.services;

import tn.esprit.config.DBConnection;
import tn.esprit.entities.TransportLocal;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for aggregating dashboard statistics: transports, voyages, hotels,
 * activities, reservations. Used by BackDashboardController for charts.
 */
public class DashboardStatsService {
    private Connection cnx;

    public DashboardStatsService() {
        cnx = DBConnection.getInstance().getCnx();
    }

    /** Count transports by type (VOL, VOITURE, TAXI, etc.) for PieChart. */
    public Map<TransportLocal.TypeTransport, Long> getTransportCountByType() {
        Map<TransportLocal.TypeTransport, Long> map = new LinkedHashMap<>();
        for (TransportLocal.TypeTransport t : TransportLocal.TypeTransport.values()) {
            map.put(t, 0L);
        }
        try {
            String sql = "SELECT type_transport, COUNT(*) AS cnt FROM transportlocal GROUP BY type_transport";
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String type = rs.getString("type_transport");
                long cnt = rs.getLong("cnt");
                try {
                    map.put(TransportLocal.TypeTransport.valueOf(type), cnt);
                } catch (IllegalArgumentException ignored) { }
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            // fallback: return zeros
        }
        return map;
    }

    /** Top compagnies by reservation count (how many times each transport compagnie was booked). */
    public List<Map.Entry<String, Integer>> getTransportReservationsByCompagnie(int limit) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        try {
            String sql = "SELECT t.compagnie, COUNT(*) AS cnt FROM reservationtransport_transport rtt " +
                    "INNER JOIN transportlocal t ON rtt.id_transport = t.id_transport " +
                    "GROUP BY t.compagnie ORDER BY cnt DESC LIMIT ?";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String compagnie = rs.getString("compagnie");
                if (compagnie == null) compagnie = "—";
                list.add(new AbstractMap.SimpleEntry<>(compagnie, rs.getInt("cnt")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            // return empty
        }
        return list;
    }

    /** Count of transports with 0 places (fully booked). */
    public int getTransportSansPlaces() {
        try {
            String sql = "SELECT COUNT(*) FROM transportlocal WHERE COALESCE(nbr_places, 10) = 0";
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                int v = rs.getInt(1);
                rs.close();
                st.close();
                return v;
            }
            rs.close();
            st.close();
        } catch (SQLException e) { }
        return 0;
    }

    /** Module counts: voyages, hotels, transport, activites. */
    public Map<String, Integer> getModuleCounts() {
        Map<String, Integer> m = new LinkedHashMap<>();
        String[] tables = { "voyage", "hotel", "transportlocal", "activite" };
        String[] keys = { "voyages", "hotels", "transport", "activites" };
        for (int i = 0; i < tables.length; i++) {
            int cnt = countTable(tables[i]);
            m.put(keys[i], cnt);
        }
        return m;
    }

    /** Reservation counts by type: voyage, hotel, transport, activite. */
    public Map<String, Integer> getReservationCounts() {
        Map<String, Integer> m = new LinkedHashMap<>();
        String[][] defs = {
                { "reservation_voyage", "voyage" },
                { "reservationhotel", "hotel" },
                { "reservationtransport", "transport" },
                { "reservation_activite", "activite" }
        };
        for (String[] d : defs) {
            m.put(d[1], countTable(d[0]));
        }
        return m;
    }

    /** BarChart data: modules vs count (for overview). */
    public List<Map.Entry<String, Number>> getModuleBarData() {
        return getModuleCounts().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(capitalize(e.getKey()), (Number) e.getValue()))
                .collect(Collectors.toList());
    }

    /** PieChart data for reservation breakdown. */
    public List<Map.Entry<String, Number>> getReservationPieData() {
        return getReservationCounts().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(capitalize(e.getKey()), (Number) e.getValue()))
                .filter(e -> e.getValue().intValue() > 0)
                .collect(Collectors.toList());
    }

    private int countTable(String table) {
        try {
            String sql = "SELECT COUNT(*) FROM " + table;
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                int v = rs.getInt(1);
                rs.close();
                st.close();
                return v;
            }
            rs.close();
            st.close();
        } catch (SQLException e) { }
        return 0;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
