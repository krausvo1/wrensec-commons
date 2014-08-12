/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * If applicable, add the following below this MPL 2.0 HEADER, replacing
 * the fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *     Portions Copyright [yyyy] [name of copyright owner]
 *
 *     Copyright 2012-2014 ForgeRock AS
 *
 */

package org.forgerock.doc.maven.build;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.forgerock.doc.maven.AbstractDocbkxMojo;
import org.forgerock.doc.maven.utils.ImageCopier;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

/**
 * Build webhelp output.
 */
public class Webhelp {

    /**
     * The Mojo that holds configuration and related methods.
     */
    private AbstractDocbkxMojo m;

    /**
     * Constructor setting the Mojo that holds the configuration.
     *
     * @param mojo The Mojo that holds the configuration.
     */
    public Webhelp(final AbstractDocbkxMojo mojo) {
        m = mojo;
    }

    /**
     * Build documents from DocBook XML sources.
     *
     * @throws MojoExecutionException Failed to build output.
     */
    public void execute() throws MojoExecutionException {
        Executor executor = new Executor();
        executor.prepareOlinkDB();
        executor.build();
    }

    /**
     * Get absolute path to a temporary Olink target database XML document
     * that points to the individual generated Olink DB files, for webhelp.
     *
     * @return Absolute path to the temporary file
     * @throws MojoExecutionException Could not write target DB file.
     */
    final String getTargetDB() throws MojoExecutionException {
        File targetDB = new File(m.getBuildDirectory() + File.separator + "olinkdb-webhelp.xml");

        try {
            StringBuilder content = new StringBuilder();
            content.append("<?xml version='1.0' encoding='utf-8'?>\n")
                    .append("<!DOCTYPE targetset [\n");

            String targetDbDtd = IOUtils.toString(getClass()
                    .getResourceAsStream("/targetdatabase.dtd"));
            content.append(targetDbDtd).append("\n");

            final Set<String> docNames = m.getDocNames();

            for (String docName : docNames) {

/*  <targetsFilename> is ignored with docbkx-tools 2.0.15.
                String sysId = getBuildDirectory().getAbsolutePath()
                        + File.separator + docName + "-webhelp.target.db";
*/
                String sysId = m.getBaseDir().getAbsolutePath()
                        + "/target/docbkx/webhelp/" + docName + "/index.webhelp.target.db";

                content.append("<!ENTITY ").append(docName)
                        .append(" SYSTEM '").append(sysId).append("'>\n");
            }

            content.append("]>\n")

                    .append("<targetset>\n")
                    .append(" <targetsetinfo>Target DB for DocBook content,\n")
                    .append(" for use with webhelp only.</targetsetinfo>\n")
                    .append(" <sitemap>\n")
                    .append("  <dir name='doc'>\n");

            for (String docName : docNames) {
                content.append("   <document targetdoc='").append(docName).append("'\n")
                        .append("             baseuri='../").append(docName).append("/'>\n")
                        .append("    &").append(docName).append(";\n")
                        .append("   </document>\n");
            }
            content.append("  </dir>\n")
                    .append(" </sitemap>\n")
                    .append("</targetset>\n");

            FileUtils.writeStringToFile(targetDB, content.toString());
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to write link target database: " + e.getMessage());
        }
        return targetDB.getPath();
    }

    /**
     * Enclose methods to run plugins.
     */
    class Executor extends MojoExecutor {

        /**
         * Prepare olink target database from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build target database.
         */
        void prepareOlinkDB() throws MojoExecutionException {

            for (String docName : m.getDocNames()) {
                ArrayList<Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.addAll(m.getBaseConfiguration());

                cfg.add(element(name("webhelpAutolabel"), "1"));
                cfg.add(element(name("webhelpCustomization"), m.path(m.getWebHelpCustomization())));
                cfg.add(element(name("collectXrefTargets"), "only"));

                cfg.add(element(name("currentDocid"), docName));
                cfg.add(element(name("includes"), docName + "/" + m.getDocumentSrcName()));

/*  <targetsFilename> is ignored with docbkx-tools 2.0.15.
                cfg.add(element(
                        name("targetsFilename"),
                        FilenameUtils.separatorsToUnix(getBuildDirectory()
                                .getPath())
                                + "/"
                                + docName
                                + "-webhelp.target.db"));
*/
                executeMojo(
                        plugin(groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(m.getDocbkxVersion())),
                        goal("generate-webhelp"),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));
            }
        }

        /**
         * Build documents from DocBook XML sources.
         *
         * @throws MojoExecutionException Failed to build the output.
         */
        void build() throws MojoExecutionException {

            ImageCopier.copyImages("webhelp", "", m.getDocumentSrcName(),
                    m.getDocbkxModifiableSourcesDirectory(), m.getDocbkxOutputDirectory());

            for (String docName : m.getDocNames()) {
                ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();
                cfg.addAll(m.getBaseConfiguration());

                cfg.add(element(name("webhelpAutolabel"), "1"));
                cfg.add(element(name("webhelpCustomization"),
                        m.path(m.getWebHelpCustomization())));

                cfg.add(element(name("targetDatabaseDocument"), getTargetDB()));

                final File webHelpBase = new File(m.getDocbkxOutputDirectory(), "webhelp");
                cfg.add(element(name("targetDirectory"), m.path(webHelpBase)));

                cfg.add(element(name("currentDocid"), docName));
                cfg.add(element(name("includes"), docName + "/" + m.getDocumentSrcName()));

                executeMojo(
                        plugin(groupId("com.agilejava.docbkx"),
                                artifactId("docbkx-maven-plugin"),
                                version(m.getDocbkxVersion())),
                        goal("generate-webhelp"),
                        configuration(cfg.toArray(new Element[cfg.size()])),
                        executionEnvironment(m.getProject(), m.getSession(), m.getPluginManager()));

                // Copy CSS and logo into place in the new webhelp output.
                final File webHelpCss = new File(webHelpBase,
                        docName + File.separator
                                + "common" + File.separator
                                + "css" + File.separator
                                + "positioning.css");
                final File webHelpLogo = new File(webHelpBase,
                        docName + File.separator
                                + "common" + File.separator
                                + "images" + File.separator
                                + "logo.png");
                try {
                    FileUtils.copyFile(m.getWebHelpCss(), webHelpCss);
                    FileUtils.copyFile(m.getWebHelpLogo(), webHelpLogo);
                } catch (IOException ie) {
                    throw new MojoExecutionException("Failed to copy file", ie);
                }
            }
        }
    }
}
