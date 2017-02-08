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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BackupInitializer {

	String name;
	Path archivePath, sourcePath, homePath;

	public BackupInitializer(String[] args) {
		archivePath = Paths.get(args[0]);
		sourcePath = Paths.get(args[1]); // #82
		name = args[2];
		homePath = Paths.get(args[3]);
	}

	public void init() throws IOException {
		Path dotBamanaPath = Paths.get(homePath + "/.bamana"); // #83
		Path backupsPath = Paths.get(dotBamanaPath + "/backups.txt");
		IOutilities.createDirectories(dotBamanaPath);
		IOutilities.createFileIfNotExists(backupsPath);
		if (backupsPath.toFile().length() == 0) { // #159
			String version = StaticStuff.MAJOR + "." + StaticStuff.MINOR + "." + StaticStuff.REVISION;
			IOutilities.appendLineToFile(version, backupsPath);
		}
		String lineToWrite = name + "|" + sourcePath + "|" + archivePath;
		IOutilities.appendLineToFile(lineToWrite, backupsPath);
		String backupLabel = StaticTools.generateBackupLabel(name, sourcePath);
		Path archiveDataPath = Paths.get(archivePath + "/" + backupLabel + "/data");
		Path archiveMetadataPath = Paths.get(archivePath + "/" + backupLabel + "/metadata");
		IOutilities.createDirectories(archiveDataPath);
		IOutilities.createDirectories(archiveMetadataPath);
		System.out.println("Backup initialized");
	}
}