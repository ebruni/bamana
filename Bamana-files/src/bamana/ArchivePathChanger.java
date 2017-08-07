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
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class ArchivePathChanger {

	int backupIndex;
	Path archivePath, homePath;

	public ArchivePathChanger(String[] args) {
		backupIndex = Integer.parseInt(args[0]);
		archivePath = Paths.get(args[1]);
		homePath = Paths.get(args[2]);
	}

	public void cap() throws IOException {
		Path dotBamanaPath = Paths.get(homePath + "/.bamana");
		Path backupsPath = Paths.get(dotBamanaPath + "/backups.txt");
		Path backupsPathTemp = Paths.get(dotBamanaPath + "/backups_temp.txt");
		IOutilities.deleteFileIfExists(backupsPathTemp);
		IOutilities.createFileIfNotExists(backupsPathTemp);
		BufferedReader reader = IOutilities.createBufferedReader(backupsPath);
		BufferedWriter writer = IOutilities.createBufferedWriter(backupsPathTemp);
		String version = IOutilities.readLine(reader, backupsPath);
		IOutilities.writeStringAndNewLineToBufferedWriter(version, writer, backupsPathTemp);
		int index = 0;
		String line;
		while ((line = IOutilities.readLine(reader, backupsPath)) != null) {
			if (++index != backupIndex)
				IOutilities.writeStringAndNewLineToBufferedWriter(line, writer, backupsPathTemp);
			else {
				List<String> backup = StaticTools.deserializeBackup(line);
				String newLine = backup.get(0) + "|" + backup.get(1) + "|" + archivePath;
				IOutilities.writeStringAndNewLineToBufferedWriter(newLine, writer, backupsPathTemp);
			}
		}
		IOutilities.flushAndCloseWriter(writer, backupsPathTemp);
		IOutilities.moveFile(backupsPathTemp, backupsPath);
	}
}