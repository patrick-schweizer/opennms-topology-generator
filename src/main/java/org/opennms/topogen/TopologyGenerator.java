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

import static org.kohsuke.args4j.OptionHandlerFilter.ALL;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.netmgt.model.CdpElement;
import org.opennms.netmgt.model.CdpLink;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OspfElement;
import org.opennms.netmgt.model.monitoringLocations.OnmsMonitoringLocation;

public class TopologyGenerator {

    private TopologyPersister persister;
    private Random random;
    @Option(name="-n",usage="generate <n> OmnsNodes")
    private int amountNodes = 2028;
    @Option(name="-e",usage="generate <e> CdpElements")
    private int amountElements = 1844;
    @Option(name="-l",usage="generate <l> CdpLinks")
    private int amountLinks = 35717;

    public TopologyGenerator() throws IOException {
        random = new Random(42);
        persister = new TopologyPersister();
    }

    void assertSetup() {
        // do basic checks to get configuration right:
        assertMoreOrEqualsThan("we need at least as many nodes as elements", amountElements, amountNodes);
        assertMoreOrEqualsThan("we need at least 2 nodes", 2, amountNodes);
        assertMoreOrEqualsThan("we need at least 2 elements", 2, amountElements);
        assertMoreOrEqualsThan("we need at least 1 link", 1, amountLinks);
    }


    private void doMain(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch( CmdLineException e ) {
            System.err.println(e.getMessage());
            System.err.println("java TopologyGenerator [options...]");
            parser.printUsage(System.err);
            System.err.println();
            System.err.println("  Example: java TopologyGenerator"+parser.printExample(ALL));
        }
    }


    private static void assertMoreOrEqualsThan(String message, int expected, int actual) {
        if (actual <= expected) {
            throw new IllegalArgumentException(message + String.format(" minimum expected=%s but found actual=%s", expected, actual));
        }
    }


    public static void main(String args[]) throws Exception {
        TopologyGenerator generator = new TopologyGenerator();
        generator.doMain(args);
        generator.assertSetup();
        generator.createCdpNetwork();
    }

    public void createCdpNetwork() throws SQLException {
        OnmsMonitoringLocation location = createMonitoringLocation();
        List<OnmsNode> nodes = createNodes(location);
        persister.persistNodes(nodes);
        List<CdpElement> cdpElements = createCdpElements(nodes);
        persister.persistElements(cdpElements);
        List<CdpLink> links = createCdpLinks(cdpElements);
        persister.persistLinks(links);
    }

    private OnmsMonitoringLocation createMonitoringLocation() {
        OnmsMonitoringLocation location = new OnmsMonitoringLocation();
        location.setLocationName("Default");
        location.setMonitoringArea("localhost");
        return location;
    }

    private List<OnmsNode> createNodes(OnmsMonitoringLocation location) {

        ArrayList<OnmsNode> nodes = new ArrayList<>();
        for (int i = 0; i < amountNodes; i++) {
            nodes.add(createNode(i, location));
        }
        return nodes;
    }

    private OnmsNode createNode(int count, OnmsMonitoringLocation location) {

        OnmsNode node = new OnmsNode();
        node.setId(100 + count); // we assume we have an empty database and can just generate the ids
        node.setLabel("myNode" + count);
        node.setLocation(location);
        return node;
    }

    private List<CdpElement> createCdpElements(List<OnmsNode> nodes) {
        ArrayList<CdpElement> cdpElements = new ArrayList<>();
        for (int i = 0; i < amountElements; i++) {
            OnmsNode node = nodes.get(i);
            cdpElements.add(createCdpElement(node));
        }
        return cdpElements;
    }

    private CdpElement createCdpElement(OnmsNode node) {
        CdpElement cdpElement = new CdpElement();
        cdpElement.setId(node.getId()); // we use the same id for simplicity
        cdpElement.setNode(node);
        cdpElement.setCdpGlobalDeviceId("CdpElementForNode" + node.getId());
        cdpElement.setCdpGlobalRun(OspfElement.TruthValue.FALSE);
        cdpElement.setCdpNodeLastPollTime(new Date());
        return cdpElement;
    }

    private List<CdpLink> createCdpLinks(List<CdpElement> cdpElements) {
        List<CdpLink> links = new ArrayList<>();
        for (int i = 0; i < amountLinks; i++) {

            CdpLink cdpLink = createCdpLink(i,
                    getRandom(cdpElements).getNode(),
                    getRandom(cdpElements).getCdpGlobalDeviceId(),
                    Integer.toString(amountLinks - i - 1),
                    Integer.toString(i));
            links.add(cdpLink);
        }
        return links;
    }

    private CdpLink createCdpLink(int id, OnmsNode node, String cdpCacheDeviceId, String cdpInterfaceName, String cdpCacheDevicePort) {
        CdpLink link = new CdpLink();
        link.setId(id);
        link.setCdpCacheDeviceId(cdpCacheDeviceId);
        link.setCdpInterfaceName(cdpInterfaceName);
        link.setCdpCacheDevicePort(cdpCacheDevicePort);
        link.setNode(node);
        link.setCdpCacheAddressType(CdpLink.CiscoNetworkProtocolType.chaos);
        link.setCdpCacheAddress("CdpCacheAddress");
        link.setCdpCacheDeviceIndex(33);
        link.setCdpCacheDeviceId("CdpCachDeviceId");
        link.setCdpCacheDevicePlatform("CdpCacheDevicePlatform");
        link.setCdpCacheIfIndex(33);
        link.setCdpCacheVersion("CdpCacheVersion");
        link.setCdpLinkLastPollTime(new Date());
        return link;
    }

    private <E> E getRandom(List<E> list) {
        return list.get(random.nextInt(list.size()));
    }

    public void deleteCdpNetwork() throws SQLException {
        this.persister.deleteTopology();
    }
}

