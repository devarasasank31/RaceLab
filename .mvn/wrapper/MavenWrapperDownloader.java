/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language permissions and limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Maven Wrapper Downloader.
 * Downloads the Maven wrapper JAR if not present.
 */
public class MavenWrapperDownloader {

    private static final String WRAPPER_URL = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar";
    private static final String WRAPPER_JAR = ".mvn/wrapper/maven-wrapper.jar";

    public static void main(String[] args) {
        System.out.println("Downloading Maven wrapper...");
        try {
            downloadWrapper();
            System.out.println("Maven wrapper downloaded successfully.");
        } catch (IOException e) {
            System.err.println("Failed to download Maven wrapper: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void downloadWrapper() throws IOException {
        File wrapperDir = new File(".mvn/wrapper");
        if (!wrapperDir.exists()) {
            wrapperDir.mkdirs();
        }

        File wrapperJar = new File(WRAPPER_JAR);
        if (wrapperJar.exists()) {
            return;
        }

        try (InputStream in = new BufferedInputStream(new URL(WRAPPER_URL).openStream());
             FileOutputStream out = new FileOutputStream(wrapperJar)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}