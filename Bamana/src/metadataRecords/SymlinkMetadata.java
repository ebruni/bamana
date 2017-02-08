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

public class SymlinkMetadata extends Metadata {

	public SymlinkMetadata(String[] metadataArray) {
		super(metadataArray);
	}

	String link;

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}
	
	@Override
	public void unpackMetadataFromArray(String[] metadataArray) {
		name = metadataArray[0];
		link = metadataArray[1];
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
        if (!(o instanceof SymlinkMetadata)) {
            return false;
        }
        SymlinkMetadata metadata = (SymlinkMetadata) o;
        return super.equals(o) &&
        		Objects.equals(link, metadata.link) &&
        		Objects.equals(creationTime, metadata.creationTime) &&
        		Objects.equals(lastModifiedTime, metadata.lastModifiedTime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), link, creationTime, lastModifiedTime);
    }
}
