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

package metadataRecords;

import java.util.Objects;

public class FileMetadata extends Metadata {

	public FileMetadata(String[] metadataArray) {
		super(metadataArray);
	}

	String hash;

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	@Override
	public void unpackMetadataFromArray(String[] metadataArray) {
		name = metadataArray[0];
		hash = metadataArray[1];
		owner = metadataArray[2];
		group = metadataArray[3];
		permissions = metadataArray[4];
		creationTime = metadataArray[5];
		lastAccessTime = metadataArray[6];
		lastModifiedTime = metadataArray[7];
	}
	
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof FileMetadata)) {
            return false;
        }
        FileMetadata metadata = (FileMetadata) o;
        return super.equals(o) &&
        		Objects.equals(hash, metadata.hash) &&
        		Objects.equals(creationTime, metadata.creationTime) &&
        		Objects.equals(lastModifiedTime, metadata.lastModifiedTime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), hash, creationTime, lastModifiedTime);
    }

}