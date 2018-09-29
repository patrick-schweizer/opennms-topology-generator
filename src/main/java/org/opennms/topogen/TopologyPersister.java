/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.topogen;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.opennms.netmgt.model.CdpElement;
import org.opennms.netmgt.model.CdpLink;
import org.opennms.netmgt.model.OnmsNode;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class TopologyPersister {

    private final static String NODES_INSERT = "INSERT INTO node (nodeid, nodelabel, location, nodecreatetime) VALUES (?, ?, ?, now());";
    private final static String NODES_DELETE = "delete from node where nodeid > 5;";
    private final static String ELEMENTS_INSERT = "INSERT INTO cdpelement (id, nodeid, cdpglobalrun, cdpglobaldeviceid, cdpnodelastpolltime, cdpnodecreatetime) VALUES (?, ?, ?, ?, ?, now());";
    private final static String ELEMENTS_DELETE = "delete from cdpelement where nodeid > 5;";
    private final static String LINKS_INSERT = "INSERT INTO cdplink (id, nodeid, cdpcacheifindex, cdpinterfacename, cdpcacheaddresstype, cdpcacheaddress, cdpcacheversion, cdpcachedeviceid, cdpcachedeviceport, cdpcachedeviceplatform, cdplinklastpolltime, cdpcachedeviceindex, cdplinkcreatetime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now());";
    private final static String LINKS_DELETE = "delete from cdplink where nodeid > 5;";

    private DataSource ds;

    TopologyPersister() throws IOException {
        setUpDatasource();
    }

    public void setUpDatasource() throws IOException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/opennms");
        config.setUsername("opennms");
        config.setPassword("opennms");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(config);
    }


    public void persistNodes(List<OnmsNode> nodes) throws SQLException {
        batchInsert(NODES_INSERT, nodes, new BiConsumerWithException<PreparedStatement, OnmsNode>() {
            @Override
            public void accept(PreparedStatement stmt, OnmsNode node) throws SQLException {
                stmt.setInt(1, node.getId());
                stmt.setString(2, node.getLabel());
                stmt.setString(3, node.getLocation().getLocationName());
            }
        });
    }

    public void persistElements(List<CdpElement> elements) throws SQLException {
        batchInsert(ELEMENTS_INSERT, elements, new BiConsumerWithException<PreparedStatement, CdpElement>() {
            @Override
            public void accept(PreparedStatement stmt, CdpElement element) throws SQLException {
                stmt.setInt(1, element.getId());
                stmt.setInt(2, element.getNode().getId());
                stmt.setInt(3, element.getCdpGlobalRun().getValue());
                stmt.setString(4, element.getCdpGlobalDeviceId());
                stmt.setDate(5, new java.sql.Date(element.getCdpNodeLastPollTime().getTime()));
            }
        });
    }

    public void persistLinks(List<CdpLink> links) throws SQLException {
        batchInsert(LINKS_INSERT, links, new BiConsumerWithException<PreparedStatement, CdpLink>() {
            @Override
            public void accept(PreparedStatement stmt, CdpLink link) throws SQLException {
                int i = 1;
                stmt.setInt(i++, link.getId());
                stmt.setInt(i++, link.getNode().getId());
                stmt.setInt(i++, link.getCdpCacheIfIndex());
                stmt.setString(i++, link.getCdpInterfaceName());
                stmt.setInt(i++, link.getCdpCacheAddressType().getValue());
                stmt.setString(i++, link.getCdpCacheAddress());
                stmt.setString(i++, link.getCdpCacheVersion());
                stmt.setString(i++, link.getCdpCacheDeviceId());
                stmt.setString(i++, link.getCdpCacheDeviceId());
                stmt.setString(i++, link.getCdpCacheDevicePlatform());
                stmt.setDate(i++, new java.sql.Date(link.getCdpLinkLastPollTime().getTime()));
                stmt.setInt(i, link.getCdpCacheDeviceIndex());
            }
        });
    }

    @FunctionalInterface
    public interface BiConsumerWithException<T, R> {
        void accept(T t, R r) throws SQLException;
    }

    private <T> void batchInsert(String statement, List<T> elements, BiConsumerWithException<PreparedStatement, T> statementFiller) throws SQLException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement insStmt = c.prepareStatement(statement)) {
                int i;
                for (i = 0; i < elements.size(); i++) {
                    T element = elements.get(i);
                    statementFiller.accept(insStmt, element);
                    insStmt.executeUpdate();
                    insStmt.getGeneratedKeys();
                    if (i % 100 == 0) { // batches of 100
                        insStmt.executeBatch();
                    }
                }
                if (i % 100 != 0) {
                    insStmt.executeBatch(); // insert last elements of batch
                }
            }

        }
    }

    public void deleteTopology() throws SQLException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement insStmt = c.prepareStatement(NODES_DELETE)) {
                insStmt.execute();
            }
            try (PreparedStatement insStmt = c.prepareStatement(ELEMENTS_DELETE)) {
                insStmt.execute();
            }
            try (PreparedStatement insStmt = c.prepareStatement(LINKS_DELETE)) {
                insStmt.execute();
            }
        }

    }
}


