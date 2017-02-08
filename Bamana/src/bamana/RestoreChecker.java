/*
 * Bamana - a free incremental backup software for GNU/Linux
 * Copyright (C) 2017 Emanuele Bruni
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package bamana;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import metadataRecords.DirectoryMetadata;
import metadataRecords.FileMetadata;

public class RestoreChecker {

	int backupIndex, snapshotIndex;
	Path currentRestorePath, archivedMetadataFilePath, archiveDataPath;
	Logger logger;

	RestoreChecker(Path archivedMetadataFilePath, Path restorePath, Path archiveDataPath, Logger logger) {
		this.archivedMetadataFilePath = archivedMetadataFilePath;
		this.currentRestorePath = restorePath;
		this.archiveDataPath = archiveDataPath;
		this.logger = logger;
	}

	public void check() throws IOException, NoSuchAlgorithmException {
		System.out.println("\n");
		System.out.println("Checking...");
		BufferedReader br = IOutilities.createBufferedReader(archivedMetadataFilePath);
		String line = IOutilities.readLine(br, archivedMetadataFilePath);
		while ((line = IOutilities.readLine(br, archivedMetadataFilePath)) != null) {
			if (line.charAt(0) == '/') {
				currentRestorePath = Paths.get(currentRestorePath.getParent().toString());
				continue;
			}
			int currentCharIndex = 0;
			if (line.charAt(currentCharIndex) == 'F' || line.charAt(currentCharIndex) == 'S') {
				currentCharIndex = 2;
				FileMetadata metadata = StaticTools.getFileMetadataFromMetadataLine(line, currentCharIndex);
				String nameFromArchivedMetadataFile = metadata.getName();
				File fileDestination = new File(currentRestorePath + "/" + nameFromArchivedMetadataFile);
				FileMetadata metadataFromDisk = (FileMetadata) StaticTools.getFileInfosFromDisk(fileDestination);
				checkFileInfosMatching(metadata, metadataFromDisk);
			} else if (line.charAt(currentCharIndex) == 'D') {
				currentCharIndex = 2;
				DirectoryMetadata metadata = StaticTools.getDirInfosFromMetadataLine(line, currentCharIndex);
				String nameFromArchivedMetadataFile = metadata.getName();
				File dirDestination = new File(currentRestorePath + "/" + nameFromArchivedMetadataFile);
				DirectoryMetadata metadataFromDisk = (DirectoryMetadata) StaticTools.getFileInfosFromDisk(dirDestination);
				checkDirInfosMatching(metadata, metadataFromDisk);
				currentRestorePath = Paths.get(currentRestorePath + "/" + nameFromArchivedMetadataFile);
			}
		}
		if (logger.gotCheckErrors) {
			System.out.println(
					"Check failed for some elements - see the log printed above. Apart from that, the restore has been successful.");
		} else {
			System.out.println("The restore has been successful - no errors found.");
		}
	}

	private void checkFileInfosMatching(FileMetadata metadataFromArchivedMetadata, FileMetadata metadataFromDisk) {
		if (!metadataFromArchivedMetadata.equals(metadataFromDisk)) {
			String path = metadataFromDisk.getPath(); // #182
			logger.logCheckError(path);
		}
	}

	private void checkDirInfosMatching(DirectoryMetadata metadataFromArchivedMetadata, DirectoryMetadata metadataFromDisk) {
		if (!metadataFromArchivedMetadata.equals(metadataFromDisk)) {
			String path = metadataFromDisk.getPath(); // #182
			logger.logCheckError(path);
		}
	}
}