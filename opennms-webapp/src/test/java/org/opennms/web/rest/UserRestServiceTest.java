/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.model.OnmsUserList;

public class UserRestServiceTest extends AbstractSpringJerseyRestTestCase {

    @Test
    public void testUser() throws Exception {
        String url = "/users";

        // Testing GET Collection
        String xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("admin"));
        OnmsUserList list = JaxbUtils.unmarshal(OnmsUserList.class, xml);
        assertEquals(1, list.getUsers().size());
        assertEquals(xml, "admin", list.getUsers().get(0).getUsername());

        xml = sendRequest(GET, url + "/admin", 200);
        assertTrue(xml.contains(">admin<"));

        sendRequest(GET, url + "/idontexist", 404);
    }
    
    @Test
    public void testWriteUser() throws Exception {
        createUser("test");
        
        String xml = sendRequest(GET, "/users/test", 200);
        assertTrue(xml.contains("<user><user-id>test</user-id>"));

        sendPut("/users/test", "password=MONKEYS", 303, "/users/test");

        xml = sendRequest(GET, "/users/test", 200);
        assertTrue(xml.contains(">MONKEYS<"));
    }

    @Test
    public void testWriteALotOfUsers() throws Exception {
        int userCount = 40;

        ExecutorService pool = Executors.newCachedThreadPool();
        List<Future<?>> createFutures = new ArrayList<Future<?>>();
        for (int i = 0; i < userCount; i++) {
            final String userName = "test" + i;
            createFutures.add(pool.submit(Executors.callable(new Runnable() {
                @Override
                public void run() {
                    try {
                        createUser(userName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail(e.getMessage());
                    }
                }
            })));
        }

        // Wait for all of the REST operations to complete
        for(Future<?> future : createFutures) {
            future.get();
        }

        sendRequest(GET, "/users", 200);

        // Try changing the password for every user to make sure that they
        // are properly accessible in the UserManager
        for (int i = 0; i < userCount; i++) {
            String xml = sendRequest(GET, "/users/test" + i, 200);
            assertTrue(xml.contains(String.format("<user><user-id>test%d</user-id>", i)));

            sendPut("/users/test" + i, "password=MONKEYS", 303, "/users/test" + i);

            xml = sendRequest(GET, "/users/test" + i, 200);
            assertTrue(xml.contains(">MONKEYS<"));
        }
    }

    @Test
    public void testDeleteUser() throws Exception {
        createUser("deleteMe");
        
        String xml = sendRequest(GET, "/users", 200);
        assertTrue(xml.contains("deleteMe"));

        sendRequest(DELETE, "/users/idontexist", 400);
        
        sendRequest(DELETE, "/users/deleteMe", 200);

        sendRequest(GET, "/users/deleteMe", 404);
    }

    protected void createUser(final String username) throws Exception {
        String user = "<user>" +
                "<user-id>" + username + "</user-id>" +
                "<full-name>" + username + " Full Name</full-name>" +
                "<user-comments>Autogenerated by a unit test...</user-comments>" +
                "<password>21232F297A57A5A743894A0E4A801FC3</password>" +
                "</user>";
        sendPost("/users", user, 303, "/users/" + username);
    }
    

}
