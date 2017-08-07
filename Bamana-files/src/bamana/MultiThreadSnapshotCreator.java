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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import metadataRecords.DirectoryMetadata;
import metadataRecords.FileMetadata;
import metadataRecords.Metadata;
import metadataRecords.SymlinkMetadata;

public class MultiThreadSnapshotCreator {

	volatile boolean abruptExitDueToDirectoryLevelBuilderException; // #1
	volatile Exception dlbException;
	volatile String dlbExceptionMessage;
	private final Object dirFlag = new Object();
	private final Object fileFlag = new Object();
	List<Object[]> metadataList;
	boolean dsCompleted;
	BlockingQueue<Object[]> mgQueue, dsQueue;
	int filesBeingProcessed, directoriesBeingNavigated; // #4
	double elementsCount, filesAndSymlinksCount;
	String[] excludedPaths;
	Path archiveDataPath, archiveMetadataPath, metadataListOnDiskPath, sourcePath;
	BufferedWriter writer;
	ExecutorService es;
	ProgressPrinter progressPrinter;
	String snapshotStartTimestamp, snapshotName;
	Logger logger;

	public MultiThreadSnapshotCreator(String archivePath, String sourcePath, String name, String[] excludedPaths,
			String backupName, Logger logger) throws UnsupportedEncodingException, FileNotFoundException, IOException { // #5
		this.sourcePath = Paths.get(sourcePath);
		this.snapshotStartTimestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
		this.snapshotName = name;
		this.logger = logger;
		this.elementsCount = StaticTools.countElementsInFolder(Paths.get(sourcePath)); // #10
		this.progressPrinter = new MultiThreadProgressPrinter(elementsCount);
		this.metadataList = new LinkedList<>();
		initializeQueues();
		initializePathsAndDirectories(sourcePath, archivePath, backupName, excludedPaths);
	}

	private void initializePathsAndDirectories(String sourcePath, String archivePath, String backupName,
			String[] excludedPaths) throws IOException {
		this.excludedPaths = excludedPaths; // #139
		String backupLabel = StaticTools.generateBackupLabel(backupName, Paths.get(sourcePath));
		this.archiveDataPath = Paths.get(archivePath + "/" + backupLabel + "/data");
		this.archiveMetadataPath = Paths.get(archivePath + "/" + backupLabel + "/metadata");
		this.metadataListOnDiskPath = Paths.get(archiveMetadataPath + "/" + snapshotStartTimestamp);
		IOutilities.createDirectories(archiveDataPath); // #6
		IOutilities.createDirectories(archiveMetadataPath);// #7
		IOutilities.createFileIfNotExists(metadataListOnDiskPath);
		this.writer = IOutilities.createBufferedWriter(metadataListOnDiskPath);
	}

	private void initializeQueues() {
		this.mgQueue = new LinkedBlockingQueue<>();
		this.dsQueue = new LinkedBlockingQueue<>();
	}

	public void snap(Path dir) throws Exception {
		Object[] slot = new Object[2];
		metadataList.add(slot);
		es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		generateMetadata(es, dir, slot);
		storeData();
		completed(); // #11
	}

	private void storeData() throws IOException, NoSuchAlgorithmException {
		if (dsQueue.isEmpty())
			dsCompleted = true;
		while (!dsCompleted) {
			Object[] bundle = StaticTools.takeFromQueue(dsQueue);
			Path itemPath = (Path) bundle[2];
			String hash = (String) bundle[3];
			Object[] slot = (Object[]) bundle[4];
			Path tempPath = Paths.get(archiveDataPath + "/" + hash + "_temp");
			Path defPath = Paths.get(archiveDataPath + "/" + hash);
			if (Files.notExists(defPath) || !defPath.toFile().exists()) { // #15,
																			// #163
				boolean copied = copyFile(itemPath, tempPath, slot); // #164
				if (copied)
					moveFile(tempPath, defPath, itemPath, slot);
			}
			progressPrinter.elaboratePercentage(false);
			decreaseFilesBeingProcessed();
		}
	}

	private void moveFile(Path tempPath, Path defPath, Path itemPath, Object[] slot) throws IOException { // #168,
																											// #174
		try {
			Files.move(tempPath, defPath, StandardCopyOption.REPLACE_EXISTING); // #165
		} catch (IOException ex) { // #18
			
			logger.logCopyError(itemPath.toString()); // #166, #167
			slot[1] = '0'; // #154, #155
		}
	}

	private boolean copyFile(Path sourcePath, Path destinationPath, Object[] slot) throws IOException {
		try {
			Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS); // #20,
																														// #169
			return true;
		} catch (IOException ioe) { // #153
			logger.logCopyError(sourcePath.toString());
			slot[1] = '0'; // #154, #155
			return false;
		}
	}

	void exportMetadata(List<Object[]> metadataList) throws IOException {
		if (metadataList != null) { // #21
			Iterator<Object[]> it = metadataList.iterator();
			while (it.hasNext()) {
				Object[] o = it.next();
				String metadataLine = (String) o[0];
				if (metadataLine.charAt(0) == 'D' || o[1] == null) // #156, #157
					IOutilities.writeStringAndNewLineToBufferedWriter(metadataLine, writer, metadataListOnDiskPath);
				if (metadataLine.charAt(0) == 'D') {
					@SuppressWarnings("unchecked")
					List<Object[]> l = (List<Object[]>) o[1];
					exportMetadata(l);
					writeSlashAndNewLineToBufferedWriter();
				} else if (metadataLine.charAt(0) == 'S') {
				}
			}
		} // #22
	}

	void checkMemToDiskMetadata(List<Object[]> metadataList) throws IOException { // #23
		System.out.println("\n");
		System.out.println("Checking snapshot's metadata integrity...");
		BufferedReader br = IOutilities.createBufferedReader(metadataListOnDiskPath);
		IOutilities.readLine(br, metadataListOnDiskPath);
		checkMemToDiskMetadataAux(metadataList, br); // #24
	}

	void checkMemToDiskMetadataAux(List<Object[]> metadataList, BufferedReader br) throws IOException { // #26
		if (metadataList != null) {
			Iterator<Object[]> it = metadataList.iterator();
			while (it.hasNext()) {
				Object[] o = it.next();
				String metadataLineMem = (String) o[0];
				if (metadataLineMem.charAt(0) != 'D' && o[1] != null) // # 158
					continue;
				String metadataLineDisk = IOutilities.readLine(br, metadataListOnDiskPath);
				if (!metadataLineMem.equals(metadataLineDisk))
					logger.logMetadataError("Memory: " + metadataLineMem + "\nFile  : " + metadataLineDisk + "\n");
				if (metadataLineDisk.charAt(0) == 'D') { // #28
					@SuppressWarnings("unchecked")
					List<Object[]> l = (List<Object[]>) o[1];
					checkMemToDiskMetadataAux(l, br);
					metadataLineDisk = IOutilities.readLine(br, metadataListOnDiskPath); // #173
				} // #30
			}
		}
	} // #31

	void checkArchive(List<Object[]> metadataList) throws NoSuchAlgorithmException, IOException {
		if (metadataList != null) {
			Iterator<Object[]> it = metadataList.iterator();
			while (it.hasNext()) {
				Object[] o = it.next();
				String metadataLineMem = (String) o[0];
				if (metadataLineMem.charAt(0) == 'F') { // #32
					if (o[1] != null) // #171
						continue;
					int currentCharIndex = 2;
					FileMetadata metadata = StaticTools.getFileMetadataFromMetadataLine(metadataLineMem,
							currentCharIndex);
					String hashInMem = metadata.getHash();
					File fileInArchive = new File(archiveDataPath + "/" + hashInMem);
					String hashInArchive = StaticTools.generateMD5(fileInArchive);
					if (!hashInMem.equals(hashInArchive)) // #172
						logger.logDataError(metadataLineMem); // #175
				} else if (metadataLineMem.charAt(0) == 'D') {
					@SuppressWarnings("unchecked")
					List<Object[]> l = (List<Object[]>) o[1];
					checkArchive(l);
				}
			}
		}
	}

	private void writeSlashAndNewLineToBufferedWriter() throws IOException {
		try {
			writer.write("/");
			writer.newLine();
		} catch (IOException ex) {
			throw new IOException("Can't write to " + metadataListOnDiskPath.toString(), ex);
		}
	}

	private void completed() throws IOException, NoSuchAlgorithmException {
		if (directoriesBeingNavigated == 0) { // #36
			progressPrinter.elaboratePercentage(true);
			IOutilities.writeStringAndNewLineToBufferedWriter(
					StaticStuff.MAJOR + "." + StaticStuff.MINOR + "." + StaticStuff.REVISION, writer,
					metadataListOnDiskPath);
			exportMetadata(metadataList); // #37
			IOutilities.flushAndCloseWriter(writer, metadataListOnDiskPath);
			writeSnapshotReferenceToSnapshotsList();
			checkMemToDiskMetadata(metadataList);
			System.out.println("Checking snapshot's integrity..."); // #38
			checkArchive(metadataList);
			if (logger.gotArchiveCheckErrors || logger.gotCheckMemToDiskErrors || logger.gotIoExceptions || logger.gotImmediateCheckErrors.get()
					|| logger.gotUnreadableElements.get())
				System.out.println(
						"Some events occurred during the snapshot - see the logs printed above. "
						+ "Apart from that, the snapshot has been successful.");
			else
				System.out.println("The snapshot has been successful - no errors found.");
		}
	}

	private void writeSnapshotReferenceToSnapshotsList() throws IOException {
		Path indexTimestampMatchingPath = Paths.get(archiveMetadataPath + "/index_timestamp_matching.txt");
		boolean created = IOutilities.createFileIfNotExists(indexTimestampMatchingPath);
		if (created)
			IOutilities.appendLineToFile(StaticStuff.MAJOR + "." + StaticStuff.MINOR + "." + StaticStuff.REVISION,
					indexTimestampMatchingPath);
		IOutilities.appendLineToFile(snapshotName + "|" + snapshotStartTimestamp, indexTimestampMatchingPath); // #8,
		// #9
	}

	private void generateMetadata(ExecutorService es, Path dir, Object[] slot) throws Exception {
		StaticTools.putInQueue(mgQueue, new Object[] { dir, slot });
		System.out.println("Snapping...");
		increaseDirectoriesBeingNavigated();
		while (true) {
			if (abruptExitDueToDirectoryLevelBuilderException) // #41
				break;
			Object[] data = StaticTools.takeFromQueue(mgQueue);
			if (data[0] instanceof String)
				break;
			Path dirPath = (Path) data[0];
			Object[] dirSlot = (Object[]) data[1];
			DirectoryLevelBuilder ccc = new DirectoryLevelBuilder(dirPath, dirSlot, mgQueue, this);
			es.execute(ccc);
		}
		es.shutdown();
		if (dlbException != null) // #42
			throw new Exception(dlbExceptionMessage, dlbException);
		// #43
	}

	void increaseDirectoriesBeingNavigated() {
		synchronized (dirFlag) {
			directoriesBeingNavigated++;
		}
	}

	void decreaseDirectoriesBeingNavigated() {
		synchronized (dirFlag) {
			directoriesBeingNavigated--;
			if (directoriesBeingNavigated == 0)
				StaticTools.putInQueue(mgQueue, new Object[] { "bye" });
		}
	}

	void increaseFilesBeingProcessed() {
		synchronized (fileFlag) {
			filesBeingProcessed++;
		}
	}

	void decreaseFilesBeingProcessed() {
		synchronized (fileFlag) {
			filesBeingProcessed--;
			if (filesBeingProcessed == 0 && directoriesBeingNavigated == 0) // #35!!!
				dsCompleted = true;
		}
	}

	private class DirectoryLevelBuilder implements Runnable { // #45
		Path dirPath;
		Object[] slot;
		BlockingQueue<Object[]> queue;
		MultiThreadSnapshotCreator cs;

		public DirectoryLevelBuilder(Path dirPath, Object[] slot, BlockingQueue<Object[]> queue,
				MultiThreadSnapshotCreator cs) {
			this.dirPath = dirPath;
			this.slot = slot;
			this.queue = queue;
			this.cs = cs;
		}

		@Override
		public void run() { // #46
			String objectType = "D";
			String name = setDirName(); // #47
			DirectoryMetadata metadata = (DirectoryMetadata) getFileInfosFromDisk(dirPath.toFile());
			DirectoryMetadata metadataSecondRead = (DirectoryMetadata) getFileInfosFromDisk(dirPath.toFile());
			checkDirectory(metadata, metadataSecondRead, dirPath); // #49
			String decontrolledName = name.replaceAll("[\u0000-\u001f]", " "); // #48 
			String[] fieldsToEscape = new String[] {objectType, decontrolledName, metadata.getOwner(),
					metadata.getGroup(), metadata.getPermissions(), metadata.getCreationTime(), metadata.getLastAccessTime(),
					metadata.getLastModifiedTime()};
			String[] escapedFields = setEscapeChars(fieldsToEscape); // #50
			List<Object[]> dirList = new LinkedList<>(); // #51
			String currentDirDescription = escapedFields[0] + " " + escapedFields[1] + " " + escapedFields[2] + " "
					+ escapedFields[3] + " " + escapedFields[4] + " " + escapedFields[5] + " " + escapedFields[6] + " "
					+ escapedFields[7];
			slot[0] = currentDirDescription;
			slot[1] = dirList;
			DirectoryStream<Path> stream = generateNewDirectoryStream(dirPath); // #177
			Iterator<Path> iter = stream.iterator();
			while (iter.hasNext()) {
				Path itemPath = iter.next();
				if (!Files.isReadable(itemPath)) { // #52
					cs.logger.logUnreadable(cs.sourcePath.relativize(itemPath).toString());
					cs.progressPrinter.elaboratePercentage(false);
					continue;
				}
				if (!itemPath.toFile().exists() || Files.notExists(itemPath)) { // #54
					cs.progressPrinter.elaboratePercentage(false);
					continue;
				}
				name = itemPath.getFileName().toString();
				decontrolledName = name.replaceAll("[\u0000-\u001f]", " ");
				if (Files.isSymbolicLink(itemPath) || isSymbolicLink(itemPath)) {
					if (Files.notExists(itemPath) || !itemPath.toFile().exists()) { // #55
						cs.progressPrinter.elaboratePercentage(false);
						continue; // #178 !!!
					}
					elaborateSymlink(itemPath, decontrolledName, dirList);
					cs.progressPrinter.elaboratePercentage(false);
				} else if (itemPath.toFile().isDirectory()) { // #56!!!, #57!!!
					if (!StaticTools.checkIfExcluded(itemPath, cs.excludedPaths, 0)) { // #58
						Object[] slot = elaborateDirectory(dirList); // #59
						if (!Files.isSymbolicLink(itemPath) || !isSymbolicLink(itemPath)) { // #179
							cs.increaseDirectoriesBeingNavigated();
							StaticTools.putInQueue(queue, new Object[] { itemPath, slot });
						}
						cs.progressPrinter.elaboratePercentage(false);
					}
				} else if (Files.isRegularFile(itemPath)) { // #61!!!
					Object[] record = elaborateFile(itemPath, decontrolledName, dirList); // #62
					String hash = (String) record[0];
					Object[] slot = (Object[]) record[1];
					copyFile(itemPath, hash, decontrolledName, slot);
				} else { // #64
					cs.progressPrinter.elaboratePercentage(false);
					continue;
				}
			}
			cs.decreaseDirectoriesBeingNavigated();
			closeDirectoryStream(stream, dirPath);
		}

		private boolean isSymbolicLink(Path path) {
			BasicFileAttributes attr = getBasicFileAttributes(path);
			return attr.isSymbolicLink();
		}

		private String setDirName() {
			String name;
			if (!dirPath.toString().equals("/"))
				name = dirPath.getFileName().toString();
			else
				name = "root";
			return name;
		}

		private void closeDirectoryStream(DirectoryStream<Path> stream, Path path) {
			try {
				stream.close();
			} catch (IOException ex) {
				cs.dlbException = ex;
				cs.dlbExceptionMessage = "Can't close the DirectoryStream of " + path;
				cs.abruptExitDueToDirectoryLevelBuilderException = true;
			}
		}

		private DirectoryStream<Path> generateNewDirectoryStream(Path path) { // #66
			DirectoryStream<Path> stream = null;
			try {
				stream = Files.newDirectoryStream(path);
			} catch (IOException ex) {
				cs.dlbException = ex;
				cs.dlbExceptionMessage = "Can't generate a DirectoryStream for " + path;
				cs.abruptExitDueToDirectoryLevelBuilderException = true;
			}
			return stream;
		}

		private PosixFileAttributes getPosixFileAttributes(Path path) { // #67
			PosixFileAttributes attributes = null;
			try {
				attributes = Files.readAttributes(path, PosixFileAttributes.class);
			} catch (IOException ex) {
				cs.dlbException = ex;
				cs.dlbExceptionMessage = "Can't read PosixFileAttributes from " + path;
				cs.abruptExitDueToDirectoryLevelBuilderException = true;
			}
			return attributes;
		}

		public BasicFileAttributes getBasicFileAttributes(Path path) {
			BasicFileAttributes attributes = null;
			try {
				attributes = Files.readAttributes(path, BasicFileAttributes.class);
			} catch (IOException ex) { // #68
				cs.dlbException = ex;
				cs.dlbExceptionMessage = "Can't read BasicFileAttributes from " + path;
				cs.abruptExitDueToDirectoryLevelBuilderException = true;
				// #69
			}
			return attributes;
		}

		private String[] setEscapeChars(String[] fieldsToCheckSpecialCharsIn) { // #70
			String[] adjustedFields = new String[fieldsToCheckSpecialCharsIn.length];
			for (int i = 0; i < fieldsToCheckSpecialCharsIn.length; i++) {
				adjustedFields[i] = fieldsToCheckSpecialCharsIn[i];
				for (int i2 = 0; i2 < StaticStuff.specialChars.length; i2++) {
					String specialChar = StaticStuff.specialChars[i2];
					if (adjustedFields[i].contains(specialChar)) { // #71
						String adjustedField = "";
						for (int i3 = 0; i3 < adjustedFields[i].length(); i3++) { // #72
							if (adjustedFields[i].charAt(i3) == specialChar.charAt(0)) {
								adjustedField += StaticStuff.escapeCharString + specialChar; // #73
							} else {
								adjustedField += adjustedFields[i].charAt(i3);
							}
						}
						adjustedFields[i] = adjustedField;
					}
				}
			}
			return adjustedFields;
		}

		private void elaborateSymlink(Path itemPath, String decontrolledName, List<Object[]> dirList) { // #181
			SymlinkMetadata metadata = (SymlinkMetadata) getFileInfosFromDisk(itemPath.toFile());
			SymlinkMetadata metadataSecondRead = (SymlinkMetadata) getFileInfosFromDisk(itemPath.toFile());
			checkSymlink(metadata, metadataSecondRead, itemPath);
			String[] escapedFields = setEscapeChars(new String[] { "S", decontrolledName, metadata.getLink(),
					metadata.getOwner(), metadata.getGroup(), metadata.getPermissions(), metadata.getCreationTime(),
					metadata.getLastAccessTime(), metadata.getLastModifiedTime()}); // #74
			String currentSymlinkDescription = escapedFields[0] + " " + escapedFields[1] + " " + escapedFields[2] + " "
					+ escapedFields[3] + " " + escapedFields[4] + " " + escapedFields[5] + " " + escapedFields[6] + " "
					+ escapedFields[7] + " " + escapedFields[8];
			Object[] slot = new Object[2];
			slot[0] = currentSymlinkDescription;
			dirList.add(slot);
		}

		private void checkSymlink(SymlinkMetadata metadataFirstRead, SymlinkMetadata metadataSecondRead, Path itemPath) {
			if (!metadataFirstRead.equals(metadataSecondRead)) {
				cs.logger.logImmediateCheckError(itemPath.toString()); // #193	
			}
		}

		// #141

		private Object[] elaborateFile(Path itemPath, String decontrolledName, List<Object[]> dirList) {
			cs.increaseFilesBeingProcessed();
			FileMetadata metadata = (FileMetadata) getFileInfosFromDisk(itemPath.toFile());
			FileMetadata metadataSecondRead = (FileMetadata) getFileInfosFromDisk(itemPath.toFile());
			checkFile(metadata, metadataSecondRead, itemPath);
			String objectType = "F";
			String[] escapedFields = setEscapeChars(new String[] { objectType, decontrolledName,
					metadata.getHash(), metadata.getOwner(), metadata.getGroup(), metadata.getPermissions(),
					metadata.getCreationTime(), metadata.getLastAccessTime(), metadata.getLastModifiedTime() }); // #77
			String currentFileDescription = escapedFields[0] + " " + escapedFields[1] + " " + escapedFields[2] + " "
					+ escapedFields[3] + " " + escapedFields[4] + " " + escapedFields[5] + " " + escapedFields[6] + " "
					+ escapedFields[7] + " " + escapedFields[8];
			Object[] slot = new Object[2];
			slot[0] = currentFileDescription;
			dirList.add(slot);
			Object[] toReturn = new Object[2];
			toReturn[0] = metadata.getHash();
			toReturn[1] = slot;
			return toReturn;
		}

		private void checkFile(FileMetadata metadataFirstRead, FileMetadata metadataSecondRead, Path itemPath) { // #80
			if (!metadataFirstRead.equals(metadataSecondRead)) {
				cs.logger.logImmediateCheckError(itemPath.toString()); // #193	
			}
		}

		private Metadata getFileInfosFromDisk(File file) {
			Metadata fileInfosFromDisk = null;
			try {
				fileInfosFromDisk = StaticTools.getFileInfosFromDisk(file);
			} catch (NoSuchAlgorithmException e) {
				cs.dlbException = e;
				cs.dlbExceptionMessage = e.getMessage();
				cs.abruptExitDueToDirectoryLevelBuilderException = true;
			} catch (IOException e) {
				cs.dlbException = e;
				cs.dlbExceptionMessage = e.getMessage();
				cs.abruptExitDueToDirectoryLevelBuilderException = true;
			}
			return fileInfosFromDisk;
		}

		private void copyFile(Path itemPath, String hash, String decontrolledName, Object[] slot) {
			Path fileInArchivePath = Paths.get(cs.archiveDataPath + "/" + hash);
			Object[] bundle = new Object[] { fileInArchivePath, decontrolledName, itemPath, hash, slot };
			StaticTools.putInQueue(cs.dsQueue, bundle);
		}

		private Object[] elaborateDirectory(List<Object[]> dirList) { // #81
			Object[] slot = new Object[2];
			dirList.add(slot);
			return slot;
		}

		private void checkDirectory(DirectoryMetadata metadataFirstRead, DirectoryMetadata metadataSecondRead, Path dirPath) {
			if (!metadataFirstRead.equals(metadataSecondRead)){
				cs.logger.logImmediateCheckError(dirPath.toString()); // #193		
			}
		}
	}
}
