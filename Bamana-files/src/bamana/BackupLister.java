/*
 * Bamana - a free incremental backup software for GNU/Linux
 * Copyright (C) 2017 Emanuele Bruni

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package bamana;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class BackupLister {

	Path homePath;

	public BackupLister(String[] args) {
		homePath = Paths.get(args[0]);
	}

	public void list() throws IOException {
		Path configDirPath = Paths.get(homePath + "/.bamana");
		Path backupsPath = Paths.get(configDirPath + "/backups.txt");
		List<String> deserializedBackups = StaticTools.deserializeBackups(backupsPath);
		String indexLabel = "Index", backupNameLabel = "Backup name", sourcePathLabel = "Source path",
				archivePathLabel = "Archive path";
		int indexMaxLength = indexLabel.length(), backupNameMaxLength = backupNameLabel.length(),
				sourcePathMaxLength = sourcePathLabel.length(), archivePathMaxLength = archivePathLabel.length();
		int row = 0;
		Iterator<String> it = deserializedBackups.iterator();
		while (it.hasNext()) {
			String backup = it.next();
			List<String> backupDetails = StaticTools.deserializeBackup(backup);
			Iterator<String> it2 = backupDetails.iterator();
			int indexLength = String.valueOf(++row).length();
			if (indexLength > indexMaxLength)
				indexMaxLength = indexLength;
			int backupNameLength = it2.next().length();
			if (backupNameLength > backupNameMaxLength)
				backupNameMaxLength = backupNameLength;
			int sourcePathLength = it2.next().length();
			if (sourcePathLength > sourcePathMaxLength)
				sourcePathMaxLength = sourcePathLength;
			int archivePathLength = it2.next().length();
			if (archivePathLength > archivePathMaxLength)
				archivePathMaxLength = archivePathLength;
		}
		List<String> linesToPrint = new LinkedList<>();
		linesToPrint.add(
				generateSlot(indexLabel, indexMaxLength) + " | " + generateSlot(backupNameLabel, backupNameMaxLength)
						+ " | " + generateSlot(sourcePathLabel, sourcePathMaxLength) + " | "
						+ generateSlot(archivePathLabel, archivePathMaxLength));
		linesToPrint.add(generateHorizontalLine(indexMaxLength) + " + " + generateHorizontalLine(backupNameMaxLength)
				+ " + " + generateHorizontalLine(sourcePathMaxLength) + " + "
				+ generateHorizontalLine(archivePathMaxLength));
		row = 0;
		it = deserializedBackups.iterator();
		while (it.hasNext()) {
			String backup = it.next();
			List<String> backupDetails = StaticTools.deserializeBackup(backup);
			Iterator<String> it2 = backupDetails.iterator();
			linesToPrint.add(generateSlot(String.valueOf(++row), indexMaxLength) + " | "
					+ generateSlot(it2.next(), backupNameMaxLength) + " | "
					+ generateSlot(it2.next(), sourcePathMaxLength) + " | "
					+ generateSlot(it2.next(), archivePathMaxLength));
		}

		it = linesToPrint.iterator();
		while (it.hasNext()) 
			System.out.println(it.next());
	}

	private String generateHorizontalLine(int maxLength) {
		String line = "";
		for (int i = 0; i < maxLength; i++)
			line += "-";
		return line;
	}

	private String generateSlot(String element, int maxLength) {
		int whitespacesToAdd = maxLength - element.length();
		for (int i = 0; i < whitespacesToAdd; i++)
			element += " ";
		return element;
	}
}
