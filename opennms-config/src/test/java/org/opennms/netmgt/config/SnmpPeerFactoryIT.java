/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.config;

import static org.opennms.netmgt.config.SnmpPeerFactory.ENABLE_ENCRYPTION;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.MockDatabase;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.config.snmp.Definition;
import org.opennms.netmgt.config.snmp.SnmpConfig;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-postgresJsonStore.xml",
        "classpath:/META-INF/opennms/applicationContext-encrypt-util.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase(tempDbClass = MockDatabase.class)
public class SnmpPeerFactoryIT {

    static {
        System.setProperty(ENABLE_ENCRYPTION, "true");
    }

    @Autowired
    private SnmpPeerFactory snmpPeerFactory;

    @Test
    public void testEncryption() {
        Assert.assertTrue(snmpPeerFactory.getEncryptionEnabled());
        Definition definition = new Definition();
        definition.setSpecifics(Arrays.asList("127.0.0.1"));
        definition.setReadCommunity("public");
        definition.setReadCommunity("private");
        definition.setAuthPassphrase("OpenNMS");
        definition.setPrivacyPassphrase("Minion");
        snmpPeerFactory.saveDefinition(definition);
        String configAsString = snmpPeerFactory.getSnmpConfigAsString();
        SnmpConfig config = JaxbUtils.unmarshal(SnmpConfig.class, configAsString);
        config.getDefinitions().forEach(def -> {
            Assert.assertNotEquals(def.getAuthPassphrase(), "OpenNMS");
        });
        config = snmpPeerFactory.getSnmpConfig();
        config.getDefinitions().forEach(def -> {
            Assert.assertEquals(def.getAuthPassphrase(), "OpenNMS");
        });
        SnmpAgentConfig agentConfig = snmpPeerFactory.getAgentConfig(InetAddressUtils.getInetAddress("127.0.0.1"));
        Assert.assertEquals(agentConfig.getAuthPassPhrase(), "OpenNMS");
        Assert.assertEquals(agentConfig.getPrivPassPhrase(), "Minion");
    }

}
