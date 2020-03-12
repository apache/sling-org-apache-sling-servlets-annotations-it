/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets.annotations;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.junit.BeforeClass;

public class TestSupport {
    
    static OsgiConsoleClient CLIENT;
    
    private static final String BUNDLE_SYMBOLICNAME = "org.apache.sling.servlets.annotations.it";
    private static final long BUNDLE_START_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final long SERVICE_START_TIMEOUT = TimeUnit.SECONDS.toMillis(60);
    
    @BeforeClass
    public static synchronized void setupOnce() throws ClientException, InterruptedException, TimeoutException, URISyntaxException {
        if(CLIENT != null) {
            return;
        }

        String baseUrl = System.getProperty("baseUrl");
        if (baseUrl == null) {
            throw new IllegalArgumentException("IT must be started with environment variable 'baseUrl' set");
        }
        URI url = new URI(baseUrl);
        CLIENT = new OsgiConsoleClient(url, "admin", "admin");
        
        String bundleFile = System.getProperty("bundleFile");
        if (bundleFile == null) {
            throw new IllegalArgumentException("IT must be started with environment variable 'bundleFile' set");
        }
        // deploy bundle to server
        File file = new File(bundleFile);
        if (!file.exists()) {
            throw new IllegalArgumentException("Test bundle file in " + file + " does not exist!");
        }
        // wait until the server is fully started
        CLIENT.waitExists("/starter/index.html", SERVICE_START_TIMEOUT, 500);

        CLIENT.waitInstallBundle(file, true, -1, BUNDLE_START_TIMEOUT, 500);
        
        // the following method somehow fails sometimes
        CLIENT.waitServiceRegistered("javax.servlet.Servlet", BUNDLE_SYMBOLICNAME, SERVICE_START_TIMEOUT, 500);
        CLIENT.waitComponentRegistered("org.apache.sling.servlets.annotations.testservlets.PathBoundServlet", SERVICE_START_TIMEOUT, 500);
        CLIENT.waitComponentRegistered("org.apache.sling.servlets.annotations.testservletfilters.SimpleServletFilter", SERVICE_START_TIMEOUT, 500);
    }
}
