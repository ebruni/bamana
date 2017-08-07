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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import metadataRecords.DirectoryMetadata;
import metadataRecords.FileMetadata;
import metadataRecords.Metadata;
import metadataRecords.SymlinkMetadata;

// #120
public class StaticTools {

	static double countElementsInMetadataFile(Path snapshotMetadataPath) throws Exception {
		double elementsCount = 0;
		BufferedReader br = IOutilities.createBufferedReader(snapshotMetadataPath);
		IOutilities.readLine(br, snapshotMetadataPath);
		String line = null;
		while ((line = IOutilities.readLine(br, snapshotMetadataPath)) != null) {
			if (line.charAt(0) == '/') {
				continue;
			} else
				elementsCount++;
		}
		return elementsCount;
	}

	public static int countElementsInFolder(Path dir) throws IOException {
		List<String> unreadableDirectories = new LinkedList<>();
		int count = 0;
		count = countElementsInFolderAux(dir, unreadableDirectories);
		return count;
	}

	private static int countElementsInFolderAux(Path dirPath, List<String> unreadableDirectories) throws IOException {
		if ((!dirPath.toFile().exists() || Files.notExists(dirPath))) // #121
			return 0;
		if (!Files.isReadable(dirPath)) {
			unreadableDirectories.add(dirPath.toString());
			return 0;
		}
		int count = 0;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
			Iterator<Path> iter = stream.iterator();
			while (iter.hasNext()) {
				count++;
				Path path = iter.next();
				if (path.toFile().isDirectory()) {
					if (Files.isSymbolicLink(path)) // #122
						continue;
					count += countElementsInFolderAux(path, unreadableDirectories);
				}
			}
		}
		return count;
	}

	public static int countFilesAndSymlinksInFolder(Path dir) throws IOException {
		List<String> unreadableDirectories = new LinkedList<>();
		int count = 0;
		count = countFilesAndSymlinksInFolderAux(dir, unreadableDirectories);
		return count;
	}

	private static int countFilesAndSymlinksInFolderAux(Path dirPath, List<String> unreadableDirectories) // #185
			throws IOException {
		if ((!dirPath.toFile().exists() || Files.notExists(dirPath))) // #121
			return 0;
		if (!Files.isReadable(dirPath)) {
			unreadableDirectories.add(dirPath.toString());
			return 0;
		}
		int count = 0;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
			Iterator<Path> iter = stream.iterator();
			while (iter.hasNext()) {
				Path path = iter.next();
				if (path.toFile().isDirectory()) {
					if (Files.isSymbolicLink(path)) // #122
						continue;
					count += countElementsInFolderAux(path, unreadableDirectories);
				} else
					count++;
			}
		}
		return count;
	}

	public static List<String> deserializeBackups(Path path) throws IOException { // #123
		List<String> serializedBackups = new LinkedList<>();
		BufferedReader br = IOutilities.createBufferedReader(path);
		String line = IOutilities.readLine(br, path);
		while ((line = IOutilities.readLine(br, path)) != null)
			serializedBackups.add(line);
		return serializedBackups;
	}

	public static String getFilesystemFromDf(String df) {
		Scanner s = new Scanner(df);
		String titles = s.nextLine();
		String data = s.nextLine();
		String type = "";
		int currentCharIndex = 0;
		while (titles.charAt(currentCharIndex) != 'T')
			currentCharIndex++;
		while (data.charAt(currentCharIndex) != ' ') { // #125
			type += (data.charAt(currentCharIndex));
			currentCharIndex++;
		}
		s.close();
		return type;
	}

	public static String getArchive(String backupLine) {
		int currentCharIndex = 0;
		while (backupLine.charAt(currentCharIndex) != '|')
			currentCharIndex++;
		currentCharIndex++;
		while (backupLine.charAt(currentCharIndex) != '|')
			currentCharIndex++;
		currentCharIndex++;
		while (backupLine.charAt(currentCharIndex) != '|')
			currentCharIndex++;
		String archive = backupLine.substring(currentCharIndex + 1);
		return archive;
	}

	static List<String> deserializeBackup(String backup) {
		List<String> deserializedBackup = new LinkedList<>();
		String name = "";
		String source = "";
		String archive = "";
		int currentCharIndex = 0;
		while (backup.charAt(currentCharIndex) != '|') {
			name += (backup.charAt(currentCharIndex));
			currentCharIndex++;
		}
		currentCharIndex++;
		while (backup.charAt(currentCharIndex) != '|') {
			source += (backup.charAt(currentCharIndex));
			currentCharIndex++;
		}
		archive = backup.substring(currentCharIndex + 1);
		deserializedBackup.add(name);
		deserializedBackup.add(source);
		deserializedBackup.add(archive);
		return deserializedBackup;
	}

	public static String generateMD5(final File file) throws NoSuchAlgorithmException, IOException { // #128
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new NoSuchAlgorithmException("No such algorithm: MD5", e);
		}
		try (FileInputStream fis = new FileInputStream(file); InputStream is = new BufferedInputStream(fis)) { // #129
			final byte[] buffer = new byte[1024];
			for (int read = 0; (read = is.read(buffer)) != -1;) {
				messageDigest.update(buffer, 0, read);
			}
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("File not found: " + file);
		} catch (IOException e) {
			throw new IOException("Can't read from file: " + file);
		}
		try (Formatter formatter = new Formatter()) { // #130
			for (final byte b : messageDigest.digest()) {
				formatter.format("%02x", b);
			}
			return formatter.toString();
		}
	}

	static List<String> deserializeSnapshot(String snapshot) {
		List<String> deserializedSnapshot = new LinkedList<>();
		String nome = "";
		String date = "";
		int currentCharIndex = 0;
		while (snapshot.charAt(currentCharIndex) != '|') {
			nome += (snapshot.charAt(currentCharIndex));
			currentCharIndex++;
		}
		date = snapshot.substring(currentCharIndex + 1);
		deserializedSnapshot.add(nome);
		deserializedSnapshot.add(date);
		return deserializedSnapshot;
	}

	static List<String> deserializeSnapshots(String snapshots) throws FileNotFoundException, IOException { // #131, #197
		BufferedReader br = IOutilities.createBufferedReader(Paths.get(snapshots));
		List<String> serializedBackups = new LinkedList<>();
		String line = IOutilities.readLine(br, Paths.get(snapshots));
		while ((line = IOutilities.readLine(br, Paths.get(snapshots))) != null)
			serializedBackups.add(line);
		return serializedBackups;
	}

	static void deserializePathsToExclude(String excludePaths) {
	}

	static boolean checkIfExcluded(Path dirPath, String[] excludedPaths, int index) { // #198
		boolean isExcluded = false;
		for (int i = 0; i < excludedPaths.length; i++) {
			if (dirPath.toString().substring(0, excludedPaths[i].length()).equals(excludedPaths[i])) {
				isExcluded = true;
				break;
			}
		}
		return isExcluded;
	}

	public static List<String> getBackupDetails(Path backups, int backupIndex) throws IOException {
		List<String> deserializedBackups = StaticTools.deserializeBackups(backups);
		String deserializedBackupLine = deserializedBackups.get(backupIndex - 1);
		List<String> deserializedBackup = StaticTools.deserializeBackup(deserializedBackupLine);
		return deserializedBackup;
	}

	static DirectoryMetadata getDirInfosFromMetadataLine(String line, int currentCharIndex) {
		String[] dirInfos = new String[7];
		for (int i = 0; i < 6; i++) {
				String field = "";
				while (line.charAt(currentCharIndex) != ' ') {
					if (line.charAt(currentCharIndex) == StaticStuff.escapeChar)
						currentCharIndex++;
					field += (line.charAt(currentCharIndex));
					currentCharIndex++;
				}
				dirInfos[i] = field;
				currentCharIndex++;
		}
		dirInfos[6] = line.substring(currentCharIndex);
		DirectoryMetadata metadata = new DirectoryMetadata(dirInfos);
		return metadata;
	}

	static FileMetadata getFileMetadataFromMetadataLine(String line, int currentCharIndex) { // #132
		String[] fileInfos = new String[8];
		for (int i = 0; i < 7; i++) {
			String field = "";
			while (line.charAt(currentCharIndex) != ' ') {
				if (line.charAt(currentCharIndex) == StaticStuff.escapeChar)
					currentCharIndex++;
				field += (line.charAt(currentCharIndex));
				currentCharIndex++;
			}
			fileInfos[i] = field;
			currentCharIndex++;
		}
		fileInfos[7] = line.substring(currentCharIndex); // #133
		Metadata metadata = new FileMetadata(fileInfos);
		return (FileMetadata) metadata;
	}

	static SymlinkMetadata getSymlinkInfos(String line, int currentCharIndex) {
		String[] symlinkInfos = new String[8];
		for (int i = 0; i < 7; i++) {
			String field = "";
			while (line.charAt(currentCharIndex) != ' ') {
				if (line.charAt(currentCharIndex) == StaticStuff.escapeChar)
					currentCharIndex++;
				field += (line.charAt(currentCharIndex));
				currentCharIndex++;
			}
			symlinkInfos[i] = field;
			currentCharIndex++;
		}
		symlinkInfos[7] = line.substring(currentCharIndex);
		SymlinkMetadata metadata = new SymlinkMetadata(symlinkInfos);
		return metadata;
	}

	static Metadata getFileInfosFromDisk(File file) throws NoSuchAlgorithmException, IOException { // #134
		String name_dest = file.getName();
		String hash_dest = "";
		if (file.isFile())
			hash_dest = StaticTools.generateMD5(file);
		BasicFileAttributes attr_dest = null;
		PosixFileAttributes attr_posix_dest = null;
		attr_dest = getBasicFileAttributes(file.toPath());
		attr_posix_dest = getPosixFileAttributes(file.toPath());
		String link_dest = "";
		if (Files.isSymbolicLink(file.toPath()) || attr_dest.isSymbolicLink()) // #135
			link_dest = Files.readSymbolicLink(file.toPath()).toString(); // #180
		String creationTime_dest = ((Long) (attr_dest.creationTime().to(TimeUnit.SECONDS))).toString();
		String lastAccessTime_dest = ((Long) (attr_dest.lastAccessTime().to(TimeUnit.SECONDS))).toString();
		String lastModifiedTime_dest = ((Long) (attr_dest.lastModifiedTime().to(TimeUnit.SECONDS))).toString();
		String owner_dest = attr_posix_dest.owner().getName();
		String group_dest = attr_posix_dest.group().getName();
		String permissions_dest = PosixFilePermissions.toString(attr_posix_dest.permissions());
		String[] fileInfos = new String[8];
		Metadata metadata = null;
		if (file.isFile() && !(Files.isSymbolicLink(file.toPath()) || attr_dest.isSymbolicLink())) {
			fileInfos = new String[]{ name_dest, hash_dest, owner_dest, group_dest, permissions_dest, creationTime_dest,
				lastAccessTime_dest, lastModifiedTime_dest /*,file.getAbsolutePath()*/ };
			metadata = new FileMetadata(fileInfos);
		}
		else if (file.isDirectory() && !(Files.isSymbolicLink(file.toPath()) || attr_dest.isSymbolicLink())) { // #202
			fileInfos = new String[]{ name_dest, owner_dest, group_dest, permissions_dest, creationTime_dest,
					lastAccessTime_dest, lastModifiedTime_dest /*,file.getAbsolutePath()*/ };
			metadata = new DirectoryMetadata(fileInfos);
		}
		else if (Files.isSymbolicLink(file.toPath()) || attr_dest.isSymbolicLink()) {
			fileInfos = new String[]{ name_dest, link_dest, owner_dest, group_dest, permissions_dest, creationTime_dest,
					lastAccessTime_dest, lastModifiedTime_dest /*,file.getAbsolutePath()*/ };
			metadata = new SymlinkMetadata(fileInfos);
		}
		metadata.setPath(file.getAbsolutePath());
		return metadata;
	}

	static PosixFileAttributes getPosixFileAttributes(Path path) throws IOException {
		PosixFileAttributes attributes = null;
		try {
			attributes = Files.readAttributes(path, PosixFileAttributes.class);
		} catch (IOException ex) {
			throw new IOException("Can't read PosixFileAttributes from " + path, ex);
		}
		return attributes;
	}

	static BasicFileAttributes getBasicFileAttributes(Path path) throws IOException {
		BasicFileAttributes attributes = null;
		try {
			attributes = Files.readAttributes(path, BasicFileAttributes.class);
		} catch (IOException ex) {
			throw new IOException("Can't read BasicFileAttributes from " + path, ex);
		}
		return attributes;
	}

	static String generateBackupLabel(String backupName, Path sourcePath) { // #145, #201
		String backupLabel = null;
		if (!backupName.equals("")) {
			if (!sourcePath.toString().equals("/")) {
				backupLabel = backupName + " (" + sourcePath.getFileName().toString() + ")";
			} else {
				backupLabel = backupName + " (root)";
			}
		} else if (!sourcePath.toString().equals("/")) {
			backupLabel = sourcePath.getFileName().toString();
		} else {
			backupLabel = "root";
		}
		return backupLabel;
	}

	static void setAttributes(Metadata metadata, Path filePath, String type) throws IOException { // #199
		String owner = metadata.getOwner();
		String group = metadata.getGroup();
		String permissions = metadata.getPermissions();
		String creationTime = metadata.getCreationTime();
		String lastAccessTime = metadata.getLastAccessTime();
		String lastModifiedTime = metadata.getLastModifiedTime();

		FileTime creationTime_ft = FileTime.from(Long.parseLong(creationTime), TimeUnit.SECONDS);
		FileTime lastAccessTime_ft = FileTime.from(Long.parseLong(lastAccessTime), TimeUnit.SECONDS);
		FileTime lastModifiedTime_ft = FileTime.from(Long.parseLong(lastModifiedTime), TimeUnit.SECONDS);

		try {
			if (!type.equals("S")) {
				BasicFileAttributeView attributes = Files.getFileAttributeView(filePath, BasicFileAttributeView.class);
				attributes.setTimes(lastModifiedTime_ft, lastAccessTime_ft, creationTime_ft);
			}
			UserPrincipal owner_up = filePath.getFileSystem().getUserPrincipalLookupService()
					.lookupPrincipalByName(owner);
			Files.setOwner(filePath, owner_up);
			GroupPrincipal group_gp = filePath.getFileSystem().getUserPrincipalLookupService()
					.lookupPrincipalByGroupName(group);
			Files.getFileAttributeView(filePath, PosixFileAttributeView.class).setGroup(group_gp);
			Set<PosixFilePermission> perms = PosixFilePermissions.fromString(permissions);

			FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
			Path p = Files.setPosixFilePermissions(filePath, perms);

			PosixFileAttributes attr_posix = Files.readAttributes(p, PosixFileAttributes.class);
			String permissionxxx = PosixFilePermissions.toString(attr_posix.permissions());

		} catch (IOException ex) {
			throw new IOException("Can't apply metadata to file: " + filePath, ex);
		}
	}

	static void putInQueue(BlockingQueue<Object[]> queue, Object[] input) { // #200
		try {
			queue.put(input);
		} catch (InterruptedException e) {
		}
	}

	static Object[] takeFromQueue(BlockingQueue<Object[]> queue) { // #200
		Object[] bundle = null;
		try {
			bundle = queue.take();
		} catch (InterruptedException ex) {
		}
		return bundle;
	}
}
