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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SnapshotLister {

	String backupIndex;
	Path homePath;

	public SnapshotLister(String args[]) {
		backupIndex = args[0];
		homePath = Paths.get(args[1]);
	}

	public void list() throws IOException {
		Path dotBamanaPath = Paths.get(homePath + "/.bamana");
		Path backupsPath = Paths.get(dotBamanaPath + "/backups.txt");
		List<String> deserializedBackups = StaticTools.deserializeBackups(backupsPath);
		String backup = deserializedBackups.get(Integer.parseInt(backupIndex) - 1);
		List<String> deserializedBackup = StaticTools.deserializeBackup(backup);
		Path archivePath = Paths.get(deserializedBackup.get(2));
		String backupName = deserializedBackup.get(0);
		Path sourcePath = Paths.get((deserializedBackup.get(1)));
		String backupLabel = StaticTools.generateBackupLabel(backupName, sourcePath);
		Path snapshotPath = Paths
				.get(archivePath + "/" + backupLabel + "/metadata/index_timestamp_matching.txt");
		List<String> deserializedSnapshots = StaticTools.deserializeSnapshots(snapshotPath.toString());
		String indexLabel = "Index", snapshotNameLabel = "Snapshot name", dateLabel = "Date";
		int indexMaxLength = indexLabel.length(), snapshotNameMaxLength = snapshotNameLabel.length(),
				dateMaxLength = dateLabel.length();
		int row = 0;
		Iterator<String> it = deserializedSnapshots.iterator();
		
		while (it.hasNext()) {
			String snapshot = it.next();
			List<String> snapshotDetails = StaticTools.deserializeSnapshot(snapshot);
			Iterator<String> it2 = snapshotDetails.iterator();
			int indexLength = String.valueOf(++row).length();
			if (indexLength > indexMaxLength)
				indexMaxLength = indexLength;
			int snapshotNameLength = it2.next().length();
			if (snapshotNameLength > snapshotNameMaxLength)
				snapshotNameMaxLength = snapshotNameLength;
			int dateLength = it2.next().length();
			if (dateLength > dateMaxLength)
				dateMaxLength = dateLength;
		}
		
		List<String> linesToPrint = new LinkedList<>();
		linesToPrint.add(
				generateSlot(indexLabel, indexMaxLength) + " | " + generateSlot(snapshotNameLabel, snapshotNameMaxLength)
						+ " | " + generateSlot(dateLabel, dateMaxLength));

		linesToPrint.add(generateHorizontalLine(indexMaxLength) + " + " + generateHorizontalLine(snapshotNameMaxLength)
				+ " + " + generateHorizontalLine(dateMaxLength));
		
		row = 0;
		it = deserializedSnapshots.iterator();
		while (it.hasNext()) {
			String snapshot = it.next();
			List<String> snapshotDetails = StaticTools.deserializeSnapshot(snapshot);
			Iterator<String> it2 = snapshotDetails.iterator();
			linesToPrint.add(generateSlot(String.valueOf(++row), indexMaxLength) + " | "
					+ generateSlot(it2.next(), snapshotNameMaxLength) + " | "
					+ generateSlot(it2.next(), dateMaxLength));
		}

		it = linesToPrint.iterator();
		while (it.hasNext()) {
			System.out.println(it.next());
		}
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