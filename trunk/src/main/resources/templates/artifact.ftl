<?xml version="1.0" encoding="UTF-8"?>
<!--
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS
-->
<assembly xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2
                              http://maven.apache.org/xsd/assembly-1.1.2.xsd">
  <id>doc-sources</id>

  <formats>
   <format>jar</format>
  </formats>

  <includeBaseDirectory>false</includeBaseDirectory>

  <fileSets>
   <fileSet>
    <directory>${modifiedSourcesDirectory}</directory>
    <outputDirectory>/</outputDirectory>
    <excludes>
     <exclude>META-INF/**</exclude>
    </excludes>
   </fileSet>
  </fileSets>
 </assembly>
