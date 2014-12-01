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

package org.forgerock.doc.maven;

import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.forgerock.doc.maven.utils.NameUtils;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AbstractMojo implementation for building core documentation from <a
 * href="http://www.docbook.org/tdg51/en/html/docbook.html">DocBook XML</a>
 * using <a href="http://code.google.com/p/docbkx-tools/">docbkx-tools</a>.
 */
abstract public class AbstractDocbkxMojo extends AbstractMojo {

    /**
     * Whether WinAnsi encoding be used for embedded fonts.
     */
    @Parameter(defaultValue = "false")
    private String ansi;

    /**
     * Whether WinAnsi encoding be used for embedded fonts.
     *
     * <br>
     *
     * Default: {@code false}
     *
     * @return Whether WinAnsi encoded should be used for embedded fonts.
     */
    public final String useAnsi() {
        return ansi;
    }

    /**
     * Should sections have numeric labels?
     *
     * <br>
     *
     * docbkx-tools element: &lt;sectionAutolabel&gt;
     */
    @Parameter(defaultValue = "true")
    private String areSectionsAutolabeled;

    /**
     * Whether sections should have numeric labels.
     *
     * <br>
     *
     * Default: {@code true}
     *
     * <br>
     *
     * docbkx-tools element: &lt;sectionAutolabel&gt;
     *
     * @return Whether sections should have numeric labels.
     */
    public final String areSectionsAutolabeled() {
        return areSectionsAutolabeled;
    }

    /**
     * Get the base configuration applicable to all builds with the docbkx-tools plugin.
     *
     * @return The configuration applicable to all builds.
     */
    public ArrayList<MojoExecutor.Element> getBaseConfiguration() {
        ArrayList<MojoExecutor.Element> cfg = new ArrayList<MojoExecutor.Element>();

        cfg.add(element(name("draftMode"), isDraftMode()));
        cfg.add(element(name("draftWatermarkImage"), getDraftWatermarkURL()));
        cfg.add(element(name("highlightSource"), useSyntaxHighlighting()));
        cfg.add(element(name("sectionAutolabel"), areSectionsAutolabeled()));
        cfg.add(element(name("sectionLabelIncludesComponentLabel"),
                doesSectionLabelIncludeComponentLabel()));
        cfg.add(element(name("xincludeSupported"), isXincludeSupported()));
        cfg.add(element(name("sourceDirectory"), path(getDocbkxModifiableSourcesDirectory())));

        return cfg;
    }

    /**
     * Project base directory, needed to workaround bugs with *target.db and webhelp.
     */
    @Parameter(defaultValue = "${basedir}")
    private File baseDir;

    /**
     * Project base directory, needed to workaround bugs with *target.db and webhelp.
     *
     * <br>
     *
     * Default: {@code ${basedir}}
     *
     * @return The project base directory.
     */
    public File getBaseDir() {
        return baseDir;
    }

    /**
     * The artifactId of the branding to use.
     */
    @Parameter(defaultValue = "forgerock-doc-default-branding")
    private String brandingArtifactId;

    /**
     * Gets the branding artifactId to use.
     *
     * <br>
     *
     * Default: {@code forgerock-doc-default-branding}.
     *
     * @return The branding artifactId.
     */
    public String getBrandingArtifactId() {
        return brandingArtifactId;
    }

    /**
     * The groupId of the branding to use.
     */
    @Parameter(defaultValue = "org.forgerock.commons")
    private String brandingGroupId;

    /**
     * Gets the groupId of the branding artifact to use.
     *
     * <br>
     *
     * Default: {@code org.forgerock.commons}
     *
     * @return The branding groupId.
     */
    public String getBrandingGroupId() {
        return brandingGroupId;
    }

    /**
     * Version of the branding artifact to use.
     */
    @Parameter(defaultValue = "3.0.0-SNAPSHOT")
    private String brandingVersion;

    /**
     * Gets the version of the branding artifact to use.
     *
     * @return The branding artifact version.
     */
    public String getBrandingVersion() {
        return brandingVersion;
    }

    /**
     * The project build directory.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}}.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDirectory;

    /**
     * Get the project build directory for this plugin.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}}.
     *
     * @return The build directory.
     */
    public File getBuildDirectory() {
        return buildDirectory;
    }

    /**
     * Whether to build a .zip of the release content.
     */
    @Parameter(defaultValue = "false", property = "buildReleaseZip")
    private boolean buildReleaseZip;

    /**
     * Whether to build a .zip containing the release content.
     *
     * <br>
     *
     * Default: {@code false}
     *
     * @return true if the .zip should be built.
     */
    public final boolean doBuildReleaseZip() {
        return buildReleaseZip;
    }

    /**
     * Location of the chunked HTML XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;htmlCustomization&gt;
     */
    @Parameter(defaultValue = "docbkx-stylesheets/html/chunked.xsl")
    private String chunkedHTMLCustomization;

    /**
     * Get the location of the chunked HTML XSL stylesheet customization file.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-stylesheets/html/chunked.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;htmlCustomization&gt;
     *
     * @return The location of the chunked HTML XSL stylesheet.
     */
    public final File getChunkedHTMLCustomization() {
        return new File(getBuildDirectory(), chunkedHTMLCustomization);
    }

    /**
     * The {@code artifactId} of the common content artifact.
     */
    @Parameter(defaultValue = "forgerock-doc-common-content")
    private String commonContentArtifactId;

    /**
     * Get the {@code artifactId} of the common content artifact.
     *
     * <br>
     *
     * Default: {@code forgerock-doc-common-content}.
     *
     * @return The {@code artifactId} of the common content artifact.
     */
    public String getCommonContentArtifactId() {
        return commonContentArtifactId;
    }

    /**
     * The {@code groupId} of the common content artifact.
     */
    @Parameter(defaultValue = "org.forgerock.commons")
    private String commonContentGroupId;

    /**
     * Get the {@code groupId} of the common content artifact.
     *
     * <br>
     *
     * Default: {@code org.forgerock.commons}.
     *
     * @return The {@code groupId} of the common content artifact.
     */
    public String getCommonContentGroupId() {
        return commonContentGroupId;
    }

    /**
     * Version of the common content artifact to use.
     */
    @Parameter(defaultValue = "3.0.0-SNAPSHOT")
    private String commonContentVersion;

    /**
     * Get the version of the common content artifact to use.
     *
     * @return the version of the common content artifact to use.
     */
    public String getCommonContentVersion() {
        return commonContentVersion;
    }

    /**
     * Whether to copy resource files alongside docs for site, release.
     */
    @Parameter(defaultValue = "false")
    private boolean copyResourceFiles;

    /**
     * Whether to copy resource files alongside docs for site, release.
     *
     * <br>
     *
     * Default: false
     *
     * @return true if resource files should be copied.
     */
    public boolean doCopyResourceFiles() {
        return copyResourceFiles;
    }

    /**
     * Base directory for the modifiable copy of DocBook XML source files,
     * relative to the build directory.
     */
    @Parameter(defaultValue = "docbkx-sources")
    private String docbkxModifiableSourcesDirectory;

    /**
     * Get the base directory for the modifiable copy of DocBook XML source files.
     * This copy is modified during preparation for processing.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-sources}
     *
     * @return The base directory for the modifiable copy of DocBook XML source files.
     */
    public File getDocbkxModifiableSourcesDirectory() {
        return new File(getBuildDirectory(), docbkxModifiableSourcesDirectory);
    }

    /**
     * Base directory for built documentation, relative to the build directory.
     */
    @Parameter(defaultValue = "docbkx")
    private String docbkxOutputDirectory;

    /**
     * Base directory for built documentation.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx}
     *
     * @return The base directory for built documentation.
     */
    public File getDocbkxOutputDirectory() {
        return new File(buildDirectory, docbkxOutputDirectory);
    }

    /**
     * Base directory for DocBook XML source files.
     */
    @Parameter(defaultValue = "${basedir}/src/main/docbkx")
    private File docbkxSourceDirectory;

    /**
     * Get the base directory for DocBook XML source files.
     * These files remain unchanged during processing.
     *
     * <br>
     *
     * Default: {@code ${basedir}/src/main/docbkx}.
     *
     * @return The base directory for DocBook XML source files.
     */
    public File getDocbkxSourceDirectory() {
        return docbkxSourceDirectory;
    }

    /**
     * Docbkx Tools plugin version to use.
     */
    @Parameter(defaultValue = "2.0.15")
    private String docbkxVersion;

    /**
     * Get the docbkx-tools plugin version to use.
     *
     * @return The docbkx-tools plugin version to use
     */
    public String getDocbkxVersion() {
        return docbkxVersion;
    }

    /**
     * Get document names for the current project.
     *
     * @return The document names for the current project.
     * @throws MojoExecutionException No document names found.
     */
    public Set<String> getDocNames() throws MojoExecutionException {

        Set<String> docNames = NameUtils.getDocumentNames(
                getDocbkxModifiableSourcesDirectory(), getDocumentSrcName());

        if (docNames.isEmpty()) {
            throw new MojoExecutionException("No document names found.");
        }
        return docNames;
    }

    /**
     * URL to site for published documentation.
     */
    @Parameter(defaultValue = "http://docs.forgerock.org/")
    private String docsSite;

    /**
     * Get the URL to the site for published documentation.
     *
     * <br>
     *
     * Default: {@code http://docs.forgerock.org/}
     *
     * @return The URL to the site for published documentation.
     */
    public String getDocsSite() {
        return docsSite;
    }

    /**
     * Top-level DocBook XML source document name.
     */
    @Parameter(defaultValue = "index.xml", property = "documentSrcName")
    private String documentSrcName;

    /**
     * Get the top-level DocBook XML source document name.
     *
     * <br>
     *
     * Default: {@code index.xml}.
     *
     * <br>
     *
     * Documents included in the documentation set
     * such as books, articles, and references share a common entry point,
     * which is a file having the name specified by this element.
     *
     * <br>
     *
     * For example, if your documentation set has
     * Release Notes, an Installation Guide, an Admin Guide, a Dev Guide, and a Reference,
     * your source layout under the base DocBook XML source directory
     * might look like the following:
     *
     * <pre>
     * src/main/docbkx/
     *  admin-guide/
     *   index.xml
     *   ...other files...
     *  dev-guide/
     *   index.xml
     *   ...other files...
     *  install-guide/
     *   index.xml
     *   ...other files...
     *  reference/
     *   index.xml
     *   ...other files...
     *  release-notes/
     *   index.xml
     *   ...other files...
     *  shared/
     *   ...other files...
     * </pre>
     *
     * <br>
     *
     * The {@code ...other files...} can have whatever names you want,
     * as long as the name does not match the file name you configure.
     * For example, if you were to hand-code an index file
     * you could name it {@code ix.xml}.
     *
     * @return File name of top-level DocBook XML source document.
     */
    public String getDocumentSrcName() {
        return documentSrcName;
    }

    /**
     * Whether section labels should include parent numbers,
     * like 1.1, 1.2, 1.2.1, 1.2.2.
     *
     * <br>
     *
     * docbkx-tools element: &lt;sectionLabelIncludesComponentLabel&gt;
     */
    @Parameter(defaultValue = "true")
    private String doesSectionLabelIncludeComponentLabel;

    /**
     * Whether section labels should include parent numbers,
     * like 1.1, 1.2, 1.2.1, 1.2.2.
     *
     * <br>
     *
     * Default: {@code true}
     *
     * <br>
     *
     * docbkx-tools element: &lt;sectionLabelIncludesComponentLabel&gt;
     *
     * @return Whether section labels should include parent numbers.
     */
    public final String doesSectionLabelIncludeComponentLabel() {
        return doesSectionLabelIncludeComponentLabel;
    }

    /**
     * For draft mode, URL to the background watermark image.
     *
     * <br>
     *
     * docbkx-tools element: &lt;draftWatermarkImage&gt;
     */
    @Parameter(defaultValue = "http://docbook.sourceforge.net/release/images/draft.png")
    private String draftWatermarkURL;

    /**
     * For draft mode, URL to the background watermark image.
     *
     * <br>
     *
     * Default: {@code http://docbook.sourceforge.net/release/images/draft.png}
     *
     * <br>
     *
     * docbkx-tools element: &lt;draftWatermarkImage&gt;
     *
     * @return The URL to the background watermark image.
     */
    public final String getDraftWatermarkURL() {
        return draftWatermarkURL;
    }

    /**
     * URL to JSON object showing EOSL versions for each project.
     */
    @Parameter(defaultValue = "http://docs.forgerock.org/eosl.json")
    private String eoslJson;

    /**
     * Get the URL to JSON object showing EOSL versions for each project.
     *
     * @return The URL to the JSON object.
     */
    public String getEoslJson() {
        return eoslJson;
    }

    /**
     * Location of the EPUB XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;epubCustomization&gt;
     */
    @Parameter(defaultValue = "docbkx-stylesheets/epub/coredoc.xsl")
    private String epubCustomization;

    /**
     * Get the location of the EPUB XSL stylesheet customization file.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-stylesheets/epub/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;epubCustomization&gt;
     *
     * @return The location of the EPUB XSL stylesheet customization file.
     */
    public final File getEpubCustomization() {
        return new File(getBuildDirectory(), epubCustomization);
    }

    /**
     * Favicon link element for the pre-site version of the HTML.
     */
    @Parameter(defaultValue = "<link rel=\"shortcut icon\" href=\"http://forgerock.org/favicon.ico\">")
    private String faviconLink;

    /**
     * Get the favicon link element for the pre-site version of the HTML.
     *
     * @return The link element.
     */
    public final String getFaviconLink() {
        return faviconLink;
    }

    /**
     * Location of the FO XSL stylesheet customization file (for PDF, RTF),
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;foCustomization&gt;
     */
    @Parameter(defaultValue = "docbkx-stylesheets/fo/coredoc.xsl")
    private String foCustomization;

    /**
     * Get the location of the FO XSL stylesheet customization file (for PDF, RTF).
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-stylesheets/fo/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;foCustomization&gt;
     *
     * @return The location of the FO XSL stylesheet.
     */
    public final File getFoCustomization() {
        return new File(getBuildDirectory(), foCustomization);
    }

    /**
     * Directory where fonts and font metrics are stored,
     * relative to the build directory.
     */
    @Parameter(defaultValue = "fonts")
    private String fontsDirectory;

    /**
     * Directory where fonts and font metrics are stored.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/fonts}
     *
     * @return The directory where fonts and font metrics are stored.
     */
    public final File getFontsDirectory() {
        return new File(getBuildDirectory(), fontsDirectory);
    }

    /**
     * Supported output formats.
     */
    public enum Format {
        /**
         * EPUB v2 without styling; not ready for publication.
         */
        epub,

        /**
         * Styled single-page and chunked HTML 4.
         */
        html,

        /**
         * Reference manual pages for use with the {@code man} command.
         */
        man,

        /**
         * PDF.
         */
        pdf,

        /**
         * RTF without styling; not ready for publication.
         */
        rtf,

        /**
         * Styled DocBook Webhelp format.
         */
        webhelp,

        /**
         * Single-page XHTML5 without styling except syntax highlighting;
         * not ready for publication as is.
         */
        xhtml5
    };

    /**
     * Comma-separated list of output formats to generate.
     */
    @Parameter(property = "formats", defaultValue = "epub,html,man,pdf,webhelp")
    private List<Format> formats;

    /**
     * Return a list of output formats to generate.
     *
     * <br>
     *
     * Default: epub,html,man,pdf,webhelp
     *
     * @return List of output formats.
     */
    public List<Format> getFormats() {
        return this.formats;
    }

    /**
     * Google Analytics identifier for the project.
     *
     * <br>
     *
     * The identifier for docs.forgerock.org is {@code UA-23412190-14}.
     */
    @Parameter(defaultValue = "UA-23412190-14")
    private String googleAnalyticsId;

    /**
     * Google Analytics identifier for the project.
     *
     * <br>
     *
     * Default: {@code UA-23412190-14}
     *
     * @return The Google Analytics identifier.
     */
    public String getGoogleAnalyticsId() {
        return googleAnalyticsId;
    }

    /**
     * Whether these are draft documents, rather than final documents.
     *
     * <br>
     *
     * docbkx-tools element: &lt;draftMode&gt;
     */
    @Parameter(defaultValue = "yes", property = "isDraftMode")
    private String isDraftMode;

    /**
     * Whether these are draft documents, rather than final documents.
     *
     * <br>
     *
     * Default: {@code yes}
     *
     * <br>
     *
     * docbkx-tools element: &lt;draftMode&gt;
     *
     * @return Whether these are draft documents.
     */
    public final String isDraftMode() {
        return isDraftMode;
    }

    /**
     * Whether documents should be allowed to include other documents.
     *
     * <br>
     *
     * docbkx-tools element: &lt;xincludeSupported&gt;
     */
    @Parameter(defaultValue = "true")
    private String isXincludeSupported;

    /**
     * Whether documents should be allowed to include other documents.
     *
     * <br>
     *
     * Default: {@code true}
     *
     * <br>
     *
     * docbkx-tools element: &lt;xincludeSupported&gt;
     *
     * @return Where documents should be allowed to include other documents.
     */
    public final String isXincludeSupported() {
        return isXincludeSupported;
    }

    /**
     * JavaScript file name, found under {@code /js/} in plugin resources.
     */
    @Parameter(defaultValue = "uses-jquery.js")
    private String javaScriptFileName;

    /**
     * Get the main JavaScript file name, found under {@code /js/} in plugin resources.
     *
     * <br>
     *
     * Default: {@code uses-jquery.js}
     *
     * @return The JavaScript file name.
     */
    public String getJavaScriptFileName() {
        return javaScriptFileName;
    }

    /**
     * The set of source paths where cited Java files are found.
     */
    @Parameter
    private List<File> jCiteSourcePaths;

    /**
     * Get the source paths where cited Java files are found.
     *
     * <br>
     *
     * If source paths are not set, {@code src/main/java} is used.
     *
     * @return the set of source paths where cited Java files are found.
     */
    public List<File> getJCiteSourcePaths() {
        return jCiteSourcePaths;
    }

    /**
     * JCite version to use for code citations.
     */
    @Parameter(defaultValue = "1.13.0")
    private String jCiteVersion;

    /**
     * Get the JCite artifact version to use for Java code citations.
     *
     * @return The JCite artifact version to use for Java code citations.
     */
    public String getJCiteVersion() {
        return jCiteVersion;
    }

    /**
     * Whether to keep a custom index.html file for the documentation set.
     */
    @Parameter(defaultValue = "false")
    private boolean keepCustomIndexHtml;

    /**
     * Whether to keep a custom index.html file for the documentation set.
     *
     * <br>
     *
     * Default: {@code false}
     *
     * @return Whether to keep a custom index.html file.
     */
    public boolean keepCustomIndexHtml() {
        return keepCustomIndexHtml;
    }

    /**
     * URL to JSON object showing latest versions for each project.
     */
    @Parameter(defaultValue = "http://docs.forgerock.org/latest.json")
    private String latestJson;

    /**
     * Get the URL to JSON object showing latest versions for each project.
     *
     * @return The URL to the JSON object.
     */
    public String getLatestJson() {
        return latestJson;
    }

    /**
     * ForgeRock link tester plugin version to use.
     */
    @Parameter(defaultValue = "1.2.0")
    private String linkTesterVersion;

    /**
     * ForgeRock link tester plugin version to use.
     *
     * @return The link tester plugin version to use.
     */
    public String getLinkTesterVersion() {
        return linkTesterVersion;
    }

    /**
     * Location of the man page XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;manpagesCustomization&gt;
     */
    @Parameter(defaultValue = "docbkx-stylesheets/man/coredoc.xsl")
    private String manpagesCustomization;

    /**
     * Get the location of the man page XSL stylesheet customization file.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-stylesheets/man/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;manpagesCustomization&gt;
     *
     * @return The location of the man page XSL stylesheet.
     */
    public final File getManpagesCustomization() {
        return new File(getBuildDirectory(), manpagesCustomization);
    }

    /**
     * Maximum height for PNG images used in PDF, in inches.
     */
    @Parameter(defaultValue = "5")
    private int maxImageHeightInInches;

    /**
     * Get maximum height for PNG images used in PDF, in inches.
     *
     * @return Maximum height for PNG images used in PDF, in inches.
     */
    public int getMaxImageHeightInInches() {
        return maxImageHeightInInches;
    }

    /**
     * Overwrite the copy of DocBook sources if it exists.
     */
    @Parameter(defaultValue = "true")
    private boolean overwriteModifiableCopy;

    /**
     * Whether to overwrite the copy of DocBook sources if it exists.
     *
     * <br>
     *
     * One of the first things the plugin does when preparing DocBook sources
     * is to make a working copy of the files that is separate from the sources.
     * This allows the plugin to make changes to the files as necessary.
     *
     * <br>
     *
     * If for some reason you must provide the copy yourself,
     * and your copy must be in the {@code docbkxModifiableSourcesDirectory},
     * then you can set this to {@code false}
     * to prevent the plugin from replacing the copy.
     * The plugin with then pre-process the copy, however,
     * so expect the files in the modifiable copy to be changed.
     *
     * <br>
     *
     * Default: true
     *
     * @return Whether to overwrite the copy of DocBook sources if it exists.
     */
    public boolean doOverwriteModifiableCopy() {
        return overwriteModifiableCopy;
    }

    /**
     * Overwrite project files with shared content.
     */
    @Parameter(defaultValue = "true", property = "overwriteProjectFilesWithSharedContent")
    private boolean overwriteProjectFilesWithSharedContent;

    /**
     * Whether to overwrite project files with shared content.
     *
     * <br>
     *
     * Default: true
     *
     * @return Whether to overwrite project files with shared content.
     */
    public boolean doOverwriteProjectFilesWithSharedContent() {
        return overwriteProjectFilesWithSharedContent;
    }

    /**
     * Get path name in UNIX format.
     *
     * @param file Path to return in UNIX format.
     * @return The path in UNIX format.
     */
    public String path(final File file) {
        String result = "";
        if (file != null) {
            result = FilenameUtils.separatorsToUnix(file.getPath());
        }
        return result;
    }

    /**
     * Version of the PlantUML artifact to use.
     */
    @Parameter(defaultValue = "7993")
    private String plantUmlVersion;

    /**
     * Get the version of the PlantUML artifact.
     *
     * @return The version of the PlantUML artifact.
     */
    public String getPlantUmlVersion() {
        return plantUmlVersion;
    }

    /**
     * The version of Plexus Utils used by the XCite Maven plugin.
     */
    @Parameter(defaultValue = "3.0.17")
    private String plexusUtilsVersion;

    /**
     * Return the version of Plexus Utils used by the XCite Maven plugin.
     *
     * @return The version of Plexus Utils used by the XCite Maven plugin.
     */
    public String getPlexusUtilsVersion() {
        return plexusUtilsVersion;
    }

    /**
     * The Maven {@code BuildPluginManager} object.
     */
    @Component
    private BuildPluginManager pluginManager;

    /**
     * Get the Maven {@code BuildPluginManager} object.
     *
     * @return The Maven {@code BuildPluginManager} object.
     */
    public BuildPluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * CSS file for the pre-site version of the HTML,
     * relative to the build directory.
     */
    @Parameter(defaultValue = "coredoc.css")
    private String preSiteCssFileName;

    /**
     * Get the CSS file for the pre-site version of the HTML.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/coredoc.css}
     *
     * @return The CSS file.
     */
    public final File getPreSiteCss() {
        return new File(getBuildDirectory(), preSiteCssFileName);
    }

    /**
     * The {@code MavenProject} object, which is read-only.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    /**
     * Get the {@code MavenProject} object.
     *
     * @return The {@code MavenProject} object.
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * Short name of the project, such as OpenAM, OpenDJ, OpenIDM.
     */
    @Parameter(property = "projectName", required = true)
    private String projectName;

    /**
     * Short name of the project, such as OpenAM, OpenDJ, OpenIDM.
     *
     * @return The short name of the project.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Project version.
     */
    @Parameter(property = "projectVersion", required = true)
    private String projectVersion;

    /**
     * Get the project version.
     *
     * @return The project version.
     */
    public String getProjectVersion() {
        return projectVersion;
    }

    /**
     * CSS file for the release version of the HTML,
     * relative to the build directory.
     */
    @Parameter(defaultValue = "dfo.css")
    private String releaseCssFileName;

    /**
     * Get the CSS file for the release version of the HTML.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/dfo.css}
     *
     * @return The CSS file.
     */
    public final File getReleaseCss() {
        return new File(getBuildDirectory(), releaseCssFileName);
    }

    /**
     * File system directory for release layout documentation,
     * relative to the build directory.
     */
    @Parameter(defaultValue = "release")
    private String releaseDirectory;

    /**
     * Get the file system directory for release layout documentation.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/release}
     *
     * @return {@link #releaseDirectory}
     */
    public final File getReleaseDirectory() {
        return new File(getBuildDirectory(), releaseDirectory);
    }

    /**
     * Favicon link element for the release version of the HTML.
     */
    @Parameter(defaultValue = "<link rel=\"shortcut icon\" href=\"http://forgerock.org/favicon.ico\">")
    private String releaseFaviconLink;

    /**
     * Get the favicon link element for the release version of the HTML.
     *
     * @return The link element.
     */
    public final String getReleaseFaviconLink() {
        return releaseFaviconLink;
    }

    /**
     * Version for this release.
     */
    @Parameter(property = "releaseVersion", required = true)
    private String releaseVersion;

    /**
     * Get the version for this release.
     *
     * @return The version for this release.
     */
    public final String getReleaseVersion() {
        return releaseVersion;
    }

    /**
     * Get the path to the directory to hold the release version documents,
     * such as {@code ${project.build.directory}/release/1.0.0}.
     *
     * @return The path to the release version directory.
     */
    public final String getReleaseVersionPath() {
        return getReleaseDirectory().getPath() + File.separator + getReleaseVersion();
    }

    /**
     * Maven resources plugin version.
     * Executions seem to hit an NPE when the version is not specified.
     */
    @Parameter(defaultValue = "2.5")
    private String resourcesVersion;

    /**
     * Get the Maven resources plugin version.
     * Executions seem to hit an NPE when the version is not specified.
     *
     * @return The Maven resources plugin version.
     */
    public String getResourcesVersion() {
        return resourcesVersion;
    }

    /**
     * File system directory for arbitrary documentation set resources,
     * relative to the modifiable sources directory.
     */
    @Parameter(defaultValue = "resources")
    private String resourcesDirectory;

    /**
     * Path to arbitrary documentation set resources,
     * relative to the modifiable sources directory.
     *
     * <br>
     *
     * Default: {@code resources}
     *
     * @return The resources directory path, relative to the modifiable sources directory.
     */
    public String getRelativeResourcesDirectoryPath() {
        return resourcesDirectory;
    }

    /**
     * Directory for arbitrary documentation set resources.
     *
     * <br>
     *
     * Default: {@code ${basedir}/src/main/docbkx/resources}
     *
     * @return The resources directory.
     */
    public File getResourcesDirectory() {
        return new File(getDocbkxModifiableSourcesDirectory(), resourcesDirectory);
    }

    /**
     * Whether to run the ForgeRock link tester plugin.
     */
    @Parameter(defaultValue = "true", property = "runLinkTester")
    private String runLinkTester;

    /**
     * Whether to run the ForgeRock link tester plugin.
     *
     * <br>
     *
     * You only need to run the link test from the top level of a project.
     *
     * <br>
     *
     * Default: {@code "true"}
     *
     * @return Whether to run the ForgeRock link tester plugin.
     */
    public String runLinkTester() {
        return runLinkTester;
    }

    /**
     * The {@code MavenSession} object, which is read-only.
     */
    @Parameter(property = "session", required = true, readonly = true)
    private MavenSession session;

    /**
     * Get the {@code MavenSession} object.
     * @return The {@code MavenSession} object.
     */
    public MavenSession getSession() {
        return session;
    }

    /**
     * Location of the single page HTML XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;htmlCustomization&gt;
     */
    @Parameter(defaultValue = "/docbkx-stylesheets/html/coredoc.xsl")
    private String singleHTMLCustomization;

    /**
     * Get the location of the single page HTML XSL stylesheet customization file.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-stylesheets/html/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;htmlCustomization&gt;
     *
     * @return The location of the single-page HTML XSL stylesheet.
     */
    public final File getSingleHTMLCustomization() {
        return new File(getBuildDirectory(), singleHTMLCustomization);
    }

    /**
     * File system directory for site content, relative to the build directory.
     */
    @Parameter(defaultValue = "site")
    private String siteDirectory;

    /**
     * Get the file system directory for site content.
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/site}
     *
     * @return The file system directory for site content.
     */
    public final File getSiteDirectory() {
        return new File(getBuildDirectory(), siteDirectory);
    }

    /**
     * Whether the ForgeRock link tester plugin should skip checking
     * that external URLs are valid.
     *
     * <br>
     *
     * See the {@code skipUrls} parameter of the <a
     * href="https://github.com/aldaris/docbook-linktester/">linktester plugin</a>.
     */
    @Parameter(defaultValue = "false", property = "skipLinkCheck")
    private String skipLinkCheck;

    /**
     * Whether the ForgeRock link tester plugin should skip checking
     * that external URLs are valid.
     *
     * <br>
     *
     * See the {@code skipUrls} parameter of the <a
     * href="https://github.com/aldaris/docbook-linktester/">linktester plugin</a>.
     *
     * <br>
     *
     * Default: {@code false}
     *
     * @return Whether to test that external URLs are valid.
     */
    public String skipLinkCheck() {
        return skipLinkCheck;
    }

    /**
     * Regex patterns of URLs to skip when checking external links.
     *
     * <br>
     *
     * See the {@code skipUrlPatterns} parameter of the <a
     * href="https://github.com/aldaris/docbook-linktester/">linktester plugin</a>.
     */
    @Parameter
    private String[] skipUrlPatterns;

    /**
     * Get regex patterns of URLs to skip when checking external links.
     *
     * <br>
     *
     * Default: {@code null}
     *
     * @return Regex patterns of URLs to skip when checking external links.
     */
    public String[] getSkipUrlPatterns() {
        return skipUrlPatterns;
    }

    /**
     * Whether to stop execution after pre-processing source files.
     */
    @Parameter(defaultValue = "false", property = "stopAfterPreProcessing")
    private boolean stopAfterPreProcessing;

    /**
     * Whether to stop execution after pre-processing source files.
     *
     * <br>
     *
     * Default: {@code false}
     *
     * @return True if execution should stop after pre-processing. False otherwise.
     */
    public boolean stopAfterPreProcessing() {
        return stopAfterPreProcessing;
    }

    /**
     * Whether &lt;programlisting&gt; content has syntax highlighting.
     *
     * <br>
     *
     * docbkx-tools element: &lt;highlightSource&gt;
     */
    @Parameter(defaultValue = "1")
    private String useSyntaxHighlighting;

    /**
     * Whether &lt;programlisting&gt; content has syntax highlighting.
     *
     * <br>
     *
     * Default: {@code 1} (true)
     *
     * <br>
     *
     * docbkx-tools element: &lt;highlightSource&gt;
     *
     * @return Where program listings use syntax highlighting.
     */
    public final String useSyntaxHighlighting() {
        return useSyntaxHighlighting;
    }

    /**
     * Location of the main CSS for webhelp documents,
     * relative to the build directory.
     */
    @Parameter(defaultValue = "docbkx-stylesheets/webhelp/positioning.css")
    private String webhelpCss;

    /**
     * Get the location of the main CSS file for webhelp documents.
     *
     * <br>
     *
     * Default: {@code ${project.build.dir}/docbkx-stylesheets/webhelp/positioning.css}
     *
     * @return The main CSS file for webhelp documents.
     */
    public final File getWebHelpCss() {
        return new File(getBuildDirectory(), webhelpCss);
    }

    /**
     * Loctain of the webhelp XSL stylesheet customization file, relative to the build
     * directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;webhelpCustomization&gt;
     */
    @Parameter(defaultValue = "docbkx-stylesheets/webhelp/coredoc.xsl")
    private String webhelpCustomization;

    /**
     * Get the location of the webhelp XSL stylesheet customization file.
     *
     * <br>
     *
     * Default: {@code ${project.build.dir}/docbkx-stylesheets/webhelp/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;webhelpCustomization&gt;
     *
     * @return The location of the webhelp XSL stylesheet.
     */
    public final File getWebHelpCustomization() {
        return new File(getBuildDirectory(), webhelpCustomization);
    }

    /**
     * Location of the logo image for webhelp documents,
     * relative to the build directory.
     */
    @Parameter(defaultValue = "docbkx-stylesheets/webhelp/logo.png")
    private String webhelpLogo;

    /**
     * Get the location of the logo image for webhelp documents.
     *
     * <br>
     *
     * Default: {@code ${project.build.dir}/docbkx-stylesheets/webhelp/logo.png}
     *
     * @return The logo image for webhelp documents.
     */
    public final File getWebHelpLogo() {
        return new File(getBuildDirectory(), webhelpLogo);
    }

    /**
     * Version of the XCite Maven plugin to use.
     */
    @Parameter(defaultValue = "1.0.0-SNAPSHOT")
    private String xCiteVersion;

    /**
     * Return the version of the XCite Maven plugin to use.
     *
     * @return The version of the XCite Maven plugin to use.
     */
    public String getXCiteVersion() {
        return xCiteVersion;
    }

    /**
     * Location of the XHTML5 XSL stylesheet customization file,
     * relative to the build directory.
     *
     * <br>
     *
     * docbkx-tools element: &lt;xhtml5Customization&gt;
     */
    @Parameter(defaultValue = "docbkx-stylesheets/xhtml5/coredoc.xsl")
    private String xhtml5Customization;

    /**
     * Get the location of the FO XSL stylesheet customization file (for PDF, RTF).
     *
     * <br>
     *
     * Default: {@code ${project.build.directory}/docbkx-stylesheets/fo/coredoc.xsl}
     *
     * <br>
     *
     * docbkx-tools element: &lt;foCustomization&gt;
     *
     * @return The location of the FO XSL stylesheet.
     */
    public final File getXhtml5Customization() {
        return new File(getBuildDirectory(), xhtml5Customization);
    }
}
