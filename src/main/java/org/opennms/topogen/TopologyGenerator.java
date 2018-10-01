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
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.netmgt.model.CdpElement;
import org.opennms.netmgt.model.CdpLink;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OspfElement;
import org.opennms.netmgt.model.monitoringLocations.OnmsMonitoringLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyGenerator {

    private final static Logger LOG = LoggerFactory.getLogger(TopologyGenerator.class);

    private TopologyPersister persister;
    @Option(name="--nodes",usage="generate <N> OmnsNodes")
    private int amountNodes = 3;
    @Option(name="--elements",usage="generate <N> CdpElements")
    private int amountElements = -1;
    @Option(name="--links",usage="generate <N> CdpLinks")
    private int amountLinks = -1;
    @Option(name="--delete",usage="delete existing toplogogy (all OnmsNodes, CdpElements and CdpLinks)")
    private boolean deleteExistingTolology = false;

    public TopologyGenerator(TopologyPersister persister) throws IOException {
        this.persister = persister;
    }

    private void assertSetup() {
        if(amountElements == -1){
            amountElements = amountNodes;
        }
        if(amountLinks == -1){
            amountLinks = (amountElements * amountElements)-amountElements;
        }
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
        if (actual < expected) {
            throw new IllegalArgumentException(message + String.format(" minimum expected=%s but found actual=%s", expected, actual));
        }
    }


    public static void main(String args[]) throws Exception {
        TopologyGenerator generator = new TopologyGenerator(new TopologyPersister());
        generator.doMain(args);
        generator.assertSetup();
        generator.createCdpNetwork();
    }

    private void createCdpNetwork() throws SQLException {
        if(deleteExistingTolology){
            deleteExistingToplogy();
        }
        LOG.info("creating topology with {} {}s, {} {}s and {} {}s.",
                this.amountNodes, OnmsNode.class.getSimpleName() ,
                this.amountElements, CdpElement.class.getSimpleName(),
                this.amountLinks, CdpLink.class.getSimpleName());
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
        node.setId(count); // we assume we have an empty database and can just generate the ids
        node.setLabel("Node" + count);
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
        UndirectedPairGenerator<CdpElement> pairs = new UndirectedPairGenerator<>(cdpElements);
        List<CdpLink> links = new ArrayList<>();
        for (int i = 0; i < amountLinks; i++) {

            // We create 2 links that reference each other, see also LinkdToplologyProvider.matchCdpLinks()
            Pair<CdpElement, CdpElement> pair = pairs.getNextPair();
            CdpElement sourceCdpElement = pair.getLeft();
            CdpElement targetCdpElement = pair.getRight();
            CdpLink sourceLink = createCdpLink(i++,
                    sourceCdpElement.getNode(),
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    targetCdpElement.getCdpGlobalDeviceId()
            );
            links.add(sourceLink);

            String targetCdpCacheDevicePort = sourceLink.getCdpInterfaceName();
            String targetCdpInterfaceName = sourceLink.getCdpCacheDevicePort();
            String targetCdpGlobalDeviceId = sourceCdpElement.getCdpGlobalDeviceId();
            CdpLink targetLink = createCdpLink(i,
                    targetCdpElement.getNode(),
                    targetCdpInterfaceName,
                    targetCdpCacheDevicePort,
                    targetCdpGlobalDeviceId
                    );
            links.add(targetLink);
            LOG.debug("Linked node {} with node {}", sourceCdpElement.getNode().getLabel(), targetCdpElement.getNode().getLabel());
        }
        return links;
    }

    private CdpLink createCdpLink(int id, OnmsNode node, String cdpInterfaceName, String cdpCacheDevicePort,
                                  String cdpCacheDeviceId) {
        CdpLink link = new CdpLink();
        link.setId(id);
        link.setCdpCacheDeviceId(cdpCacheDeviceId);
        link.setCdpInterfaceName(cdpInterfaceName);
        link.setCdpCacheDevicePort(cdpCacheDevicePort);
        link.setNode(node);
        link.setCdpCacheAddressType(CdpLink.CiscoNetworkProtocolType.chaos);
        link.setCdpCacheAddress("CdpCacheAddress");
        link.setCdpCacheDeviceIndex(33);
        link.setCdpCacheDevicePlatform("CdpCacheDevicePlatform");
        link.setCdpCacheIfIndex(33);
        link.setCdpCacheVersion("CdpCacheVersion");
        link.setCdpLinkLastPollTime(new Date());
        return link;
    }

    public void deleteExistingToplogy() throws SQLException {
        this.persister.deleteTopology();
    }
}