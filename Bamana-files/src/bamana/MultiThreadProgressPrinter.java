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

public class MultiThreadProgressPrinter extends ProgressPrinter {
	
	public MultiThreadProgressPrinter(double elementsCount) {
		super(elementsCount);
	}

	@Override
	synchronized void elaboratePercentage(boolean last) {
		nanosecondsIn = System.nanoTime();
		if (!last) {
			elementsVisited++;
			if (nanosecondsIn - nanosecondsOut > StaticStuff.NANOSECONDS_MIN_GAP) {
				printProgress();
				nanosecondsOut = System.nanoTime();
			}
		} else {
			printProgress();
		}
	}
}
