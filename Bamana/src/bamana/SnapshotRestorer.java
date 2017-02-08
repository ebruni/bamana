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
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import metadataRecords.DirectoryMetadata;
import metadataRecords.FileMetadata;
import metadataRecords.Metadata;
import metadataRecords.SymlinkMetadata;

public class SnapshotRestorer {
	ProgressPrinter progressPrinter;
	Path archiveDataPath, archiveMetadataPath, snapshotMetadataPath, destinationPath;
	double elementsCount = 0;
	Logger logger;

	public SnapshotRestorer(Logger logger, String[] args) throws Exception {
		this.logger = logger;
		int backupIndex = Integer.parseInt(args[0]); // #147
		destinationPath = Paths.get(args[1]);
		int snapshotIndex = Integer.parseInt(args[2]);
		Path homePath = Paths.get(args[3]);
		Path backupsPath = Paths.get(homePath + "/.bamana/backups.txt");
		List<String> backupDetails = StaticTools.getBackupDetails(backupsPath, backupIndex);
		String backupName = backupDetails.get(0);
		Path sourcePath = Paths.get(backupDetails.get(1));
		Path archivePath = Paths.get(backupDetails.get(2));
		String backupLabel = StaticTools.generateBackupLabel(backupName, sourcePath);
		archiveDataPath = Paths.get(archivePath + "/" + backupLabel + "/" + "data");
		archiveMetadataPath = Paths.get(archivePath + "/" + backupLabel + "/" + "metadata");
		snapshotMetadataPath = getSnapshotPathFromSnapshotIndex(snapshotIndex);
	}

	private Path getSnapshotPathFromSnapshotIndex(int snapshotIndex) throws Exception {
		String line = getSnapshotLineFromSnapshotIndex(snapshotIndex); // #148
		String timestamp = StaticTools.deserializeSnapshot(line).get(1);
		return Paths.get(archiveMetadataPath + "/" + timestamp);
	}

	public void restore() throws Exception {
		elementsCount = StaticTools.countElementsInMetadataFile(snapshotMetadataPath);
		progressPrinter = new SingleThreadProgressPrinter(elementsCount);
		progressPrinter.elaboratePercentage(false);

		restoreFilesAndDirectories();
		restoreSymlinks();

		new RestoreChecker(snapshotMetadataPath, destinationPath, archiveDataPath, logger).check();
	}

	private void restoreSymlinks() throws IOException {
		Path currentRestorePath = destinationPath;
		BufferedReader br = IOutilities.createBufferedReader(snapshotMetadataPath); // #194
		String line = IOutilities.readLine(br, snapshotMetadataPath);
		while ((line = IOutilities.readLine(br, snapshotMetadataPath)) != null) {
			if (line.charAt(0) == '/') {
				currentRestorePath = Paths.get(currentRestorePath.getParent().toString());
				continue;
			} // #116
			int currentCharIndex = 0;
			if (line.charAt(currentCharIndex) == 'D') {
				currentCharIndex = 2;
				DirectoryMetadata metadata = StaticTools.getDirInfosFromMetadataLine(line, currentCharIndex); // #149
				String nameFromArchivedMetadataFile = metadata.getName();
				currentRestorePath = Paths.get(currentRestorePath + "/" + nameFromArchivedMetadataFile);
			} else if (line.charAt(currentCharIndex) == 'S') {
				currentCharIndex = 2;
				SymlinkMetadata metadata = StaticTools.getSymlinkInfos(line, currentCharIndex);
				String nameFromArchivedMetadataFile = metadata.getName();
				String linkFromArchivedMetadataFile = metadata.getLink();
				Path restoredFilePath = Paths.get(currentRestorePath.toString() + "/" + nameFromArchivedMetadataFile);
				IOutilities.createSymlink(restoredFilePath, Paths.get(linkFromArchivedMetadataFile)); // #117,
																										// #150
				setAttributes(metadata, restoredFilePath, "S"); // #118
				progressPrinter.elaboratePercentage(false);
			}
		}
	}

	private void restoreFilesAndDirectories() throws Exception {
		Path currentRestorePath = destinationPath;
		BufferedReader br = IOutilities.createBufferedReader(snapshotMetadataPath);
		String line = IOutilities.readLine(br, snapshotMetadataPath);
		while ((line = IOutilities.readLine(br, snapshotMetadataPath)) != null) {
			if (line.charAt(0) == '/') {
				currentRestorePath = Paths.get(currentRestorePath.getParent().toString());
				continue;
			} // #106
			int currentCharIndex = 0;
			if (line.charAt(currentCharIndex) == 'F') {
				currentCharIndex = 2;
				FileMetadata metadata = StaticTools.getFileMetadataFromMetadataLine(line, currentCharIndex); // #107
				String nameFromArchivedMetadataFile = metadata.getName();
				String hashFromArchivedMetadataFile = metadata.getHash();
				Path restoredFilePath = Paths.get(currentRestorePath.toString() + "/" + nameFromArchivedMetadataFile);
				Path archivedFilePath = Paths.get(archiveDataPath.toString() + "/" + hashFromArchivedMetadataFile);
				copyFile(archivedFilePath, restoredFilePath); // #108
				setAttributes(metadata, restoredFilePath, "F");
			} else if (line.charAt(currentCharIndex) == 'D') { // #109
				currentCharIndex = 2;
				DirectoryMetadata metadata = StaticTools.getDirInfosFromMetadataLine(line, currentCharIndex);
				String nameFromArchivedMetadataFile = metadata.getName();
				currentRestorePath = Paths.get(currentRestorePath + "/" + nameFromArchivedMetadataFile);
				IOutilities.createDirectories(currentRestorePath); // #110
				setAttributes(metadata, currentRestorePath, "D"); // #111
			}
			progressPrinter.elaboratePercentage(false);
			// #112
		}
	}

	private void setAttributes(Metadata metadata, Path restoredFilePath, String type) {
		try {
			StaticTools.setAttributes(metadata, restoredFilePath, type);
		} catch (IOException e) { // #195
			logger.logMetadataRestoreError(restoredFilePath.toString());
		}
	}

	private void copyFile(Path sourcePath, Path destinationPath) throws IOException {
		try {
			Files.copy(sourcePath, destinationPath, StandardCopyOption.COPY_ATTRIBUTES,
					/* StandardCopyOption.REPLACE_EXISTING, */ LinkOption.NOFOLLOW_LINKS); // #152
		} catch (FileAlreadyExistsException ex) {
			throw new FileAlreadyExistsException(destinationPath.toString(), sourcePath.toString(),
					"File already exists: " + destinationPath);
		} catch (IOException ex) {
			throw new IOException("Can't copy " + sourcePath + " to " + destinationPath, ex);
		}
	}

	private String getSnapshotLineFromSnapshotIndex(int snapshotIndex) throws Exception {
		Path indexTimestampMatchingPath = Paths.get(archiveMetadataPath + "/index_timestamp_matching.txt");
		BufferedReader br = IOutilities.createBufferedReader(indexTimestampMatchingPath);
		int index = 0;
		boolean found = false;
		String line = IOutilities.readLine(br, indexTimestampMatchingPath);
		while ((line = IOutilities.readLine(br, indexTimestampMatchingPath)) != null) {
			if (++index == snapshotIndex) {
				found = true;
				break;
			}
		}
		if (found)
			return line;
		else
			throw new Exception("Can't find the requested snapshot."); // #151
	}

}
