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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.entity.StringEntity;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.apache.sling.testing.junit.rules.SlingRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ServletRegistrationIT {
    
    @Rule
    public SlingRule methodRule = new SlingRule();
    
    public static OsgiConsoleClient CLIENT;
    
    private static final String BUNDLE_SYMBOLICNAME = "org.apache.sling.servlets.annotations.it";
    private static final long BUNDLE_START_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final long SERVICE_START_TIMEOUT = TimeUnit.SECONDS.toMillis(60);
    
    @BeforeClass
    public static void setupOnce() throws ClientException, InterruptedException, TimeoutException, URISyntaxException {
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
        CLIENT.waitExists("/index.html", SERVICE_START_TIMEOUT, 500);

        CLIENT.waitInstallBundle(file, true, -1, BUNDLE_START_TIMEOUT, 500);
        
        // the following method somehow fails sometimes
        CLIENT.waitServiceRegistered("javax.servlet.Servlet", BUNDLE_SYMBOLICNAME, SERVICE_START_TIMEOUT, 500);
        CLIENT.waitComponentRegistered("org.apache.sling.servlets.annotations.testservlets.PathBoundServlet", SERVICE_START_TIMEOUT, 500);
        CLIENT.waitComponentRegistered("org.apache.sling.servlets.annotations.testservletfilters.SimpleServletFilter", SERVICE_START_TIMEOUT, 500);
    }
    
    @AfterClass
    public static void tearDownOnce() throws ClientException {
        //CLIENT.uninstallBundle(BUNDLE_SYMBOLICNAME);
    }

    @Test
    public void testPathBoundServlet() throws ClientException, UnsupportedEncodingException {
        CLIENT.doGet("/bin/PathBoundServlet", 555);
        CLIENT.doGet("/bin/PathBoundServlet.with.some.selector.and.extension", 555);
        CLIENT.doGet("/bin/PathBoundServlet.with.some.selector.and.extension/suffix", 555);
        // other methods should work as well
        CLIENT.doPut("/bin/PathBoundServlet", new StringEntity("some text"), Collections.emptyList(), 555);
    }
    
    @Test
    public void testPathBoundServletWithFilter() throws ClientException {
        CLIENT.doGet("/bin/PathBoundServlet.html/simplefilter", 556);
        CLIENT.doGet("/bin/PathBoundServlet.with.some.selector.and.extension/simplefilter", 556);
    }

    @Test
    public void testPathBoundServletWithPrefix() throws ClientException {
        CLIENT.doGet("/bin/PathBoundServletWithPrefix", 555);
        CLIENT.doGet("/bin/PathBoundServletWithPrefix.with.some.selector.and.extension", 555);
    }

    @Test
    public void testResourceTypeBoundServlet() throws ClientException, UnsupportedEncodingException {
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServlet", 555);
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServlet.html", 555);
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServlet.json", 555);
        // only GET and HEAD are supposed to be working
        CLIENT.doPut("/content/servlettest/resourceTypeBoundServlet.json", new StringEntity("some text"), Collections.emptyList(), 201);
    }

    @Test
    public void testResourceTypeBoundServletWithPrefix() throws ClientException, UnsupportedEncodingException {
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithPrefix", 555);
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithPrefix.html", 555);
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithPrefix.json", 555);
        // only GET and HEAD are supposed to be working
        CLIENT.doPut("/content/servlettest/resourceTypeBoundServletWithPrefix.json", new StringEntity("some text"), Collections.emptyList(), 201);
    }

    @Test
    public void testResourceTypeBoundServletWithExtension() throws ClientException, UnsupportedEncodingException {
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithExtension", 403); // without extension is a index listing, which is forbidden by default
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithExtension.html", 200); // DEFAULT GET Servlet
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithExtension.ext1", 555);
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithExtension.ext2", 555);
        // only GET and HEAD are supposed to be working
        CLIENT.doPut("/content/servlettest/resourceTypeBoundServletWithExtension.ext2", new StringEntity("some text"), Collections.emptyList(), 201);
    }

    @Test
    public void testResourceTypeBoundServletWithSelectors() throws ClientException, UnsupportedEncodingException {
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithSelectors.someext", 404);
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithSelectors.selector1.someext", 404);
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithSelectors.selector3.someext", 555);
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithSelectors.selector1.selector2.someext", 555);
        // some non-registered selector as first selector
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithSelectors.someotherselector.selector1.selector2.someext", 404);
        // some non-registered selector as last selector
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithSelectors.selector1.selector2.someotherselector.someext", 555);
        // only GET and HEAD are supposed to be working
        CLIENT.doPut("/content/servlettest/resourceTypeBoundServletWithSelectors.selector3.someext", new StringEntity("some text"), Collections.emptyList(), 201);
    }

    @Test
    public void testResourceTypeBoundServletWithMethods() throws ClientException, UnsupportedEncodingException {
        CLIENT.doGet("/content/servlettest/resourceTypeBoundServletWithMethods.someext", 404); // DEFAULT Get not triggered due to weird extension
        CLIENT.doPut("/content/servlettest/resourceTypeBoundServletWithMethods.someext", new StringEntity("some text"), Collections.emptyList(), 555);
        CLIENT.doPost("/content/servlettest/resourceTypeBoundServletWithMethods.someext", new StringEntity("some text"), Collections.emptyList(), 555);
    }
}
