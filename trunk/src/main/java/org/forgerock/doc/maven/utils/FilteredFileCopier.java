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
 *     Copyright 2013-2014 ForgeRock AS
 *
 */

package org.forgerock.doc.maven.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Copy files not having a particular extension.
 */
public final class FilteredFileCopier {

    /**
     * Copy all files not having the specified extension from
     * the source directory to the destination directory.
     *
     * @param extension Extension of files to skip.
     * @param sourceDir Source directory for files to copy.
     * @param destinationDir Destination directory for files to copy.
     * @throws IOException Failed to copy the files.
     */
    public static void copyOthers(final String extension,
                                  final File sourceDir,
                                  final File destinationDir)
            throws IOException {

        IOFileFilter nonExtFilter = FileFilterUtils.notFileFilter(
                FileFilterUtils.suffixFileFilter(extension));

        IOFileFilter nonExtFiles = FileFilterUtils.and(
                FileFileFilter.FILE, nonExtFilter);

        FileFilter filter = FileFilterUtils.or(
                DirectoryFileFilter.DIRECTORY, nonExtFiles);

        FileUtils.copyDirectory(sourceDir, destinationDir, filter);
    }

    private FilteredFileCopier() {
        // Not used.
    }
}
