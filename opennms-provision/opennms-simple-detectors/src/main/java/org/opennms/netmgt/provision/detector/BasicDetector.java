/*
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2008 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a derivative work, containing both original code, included code and modified
 * code that was published under the GNU General Public License. Copyrights for modified
 * and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Modifications;
 * Created 10/16/2008
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 * OpenNMS Licensing       <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 */
package org.opennms.netmgt.provision.detector;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;

import org.opennms.netmgt.provision.DetectorMonitor;
import org.opennms.netmgt.provision.ServiceDetector;
import org.opennms.netmgt.provision.support.AbstractDetector;
import org.opennms.netmgt.provision.support.Client;
import org.opennms.netmgt.provision.support.ClientConversation;
import org.opennms.netmgt.provision.support.ClientConversation.RequestBuilder;
import org.opennms.netmgt.provision.support.ClientConversation.ResponseValidator;

/**
 * 
 * @author <a href=mailto:desloge@opennms.com>Donald Desloge</a>
 *
 */

public abstract class BasicDetector<Request, Response> extends AbstractDetector implements ServiceDetector {
    
    private ClientConversation<Request, Response> m_conversation = new ClientConversation<Request, Response>();
    
    protected BasicDetector(int defaultPort, int defaultTimeout, int defaultRetries) {
        super(defaultPort, defaultTimeout, defaultRetries);
    }
    
    abstract protected void onInit();
    
    public boolean isServiceDetected(InetAddress address, DetectorMonitor detectorMonitor) {
        int port = getPort();
        int retries = getRetries();
        int timeout = getTimeout();
        System.out.printf("Address: %s || port: %s || ", address, getPort());
        detectorMonitor.start(this, "Checking address: %s for %s capability", address, getServiceName());
                
        Client<Request, Response> client = getClient();
        for (int attempts = 0; attempts <= retries; attempts++) {

            try {
                
                client.connect(address, port, timeout);
                
                detectorMonitor.attempt(this, attempts, "Attempting to connect to address: %s attempt #%s",address.getHostAddress(),attempts);
                
                if (attemptConversation(client)) {
                    return true;
                }
                
            } catch (ConnectException cE) {
                // Connection refused!! Continue to retry.
                cE.printStackTrace();
                detectorMonitor.info(this, cE, "Attempting to connect to address: %s attempt #%s",address.getHostAddress(),attempts);
            } catch (NoRouteToHostException e) {
                // No Route to host!!!
                e.printStackTrace();
                detectorMonitor.info(this, e, "%s: No route to address %s was available", getServiceName(), address.getHostAddress());
                throw new UndeclaredThrowableException(e);
            } catch (InterruptedIOException e) {
                // Expected exception
                e.printStackTrace();
                detectorMonitor.info(this, e, "%s: Did not connect to to address within timeout: %d attempt: %d", getServiceName(), timeout, attempts);
            } catch (IOException e) {
                e.printStackTrace();
                detectorMonitor.info(this, e, "%s: An unexpected I/O exception occured contacting address %s",getServiceName(), address.getHostAddress());
            } catch (Throwable t) {
                t.printStackTrace();
                detectorMonitor.failure(this, "%s: Failed to detect %s on address %s", getServiceName(), getServiceName(), address.getHostAddress());
                detectorMonitor.error(this, t, "%s: An undeclared throwable exception was caught contating address %s", getServiceName(), address.getHostAddress());
            } finally {
                client.close();
            }
        }
        
        return false;
    }

    /**
     * @return
     */
    abstract protected Client<Request, Response> getClient();

    private boolean attemptConversation(Client<Request, Response> client) throws IOException, Exception {
        return getConversation().attemptConversation(client);
    }
    
    protected void expectBanner(ResponseValidator<Response> bannerValidator) {
        getConversation().expectBanner(bannerValidator);
    }
    
    protected void send(RequestBuilder<Request> requestBuilder, ResponseValidator<Response> responseValidator) {
        getConversation().addExchange(requestBuilder, responseValidator);
    }
    protected void send(Request request, ResponseValidator<Response> responseValidator) {
        getConversation().addExchange(request, responseValidator);
    }
    
    private ClientConversation<Request, Response> getConversation() {
        return m_conversation;
    }
    
}
