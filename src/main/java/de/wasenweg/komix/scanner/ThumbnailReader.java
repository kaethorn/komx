package de.wasenweg.komix.scanner;

import de.wasenweg.komix.comics.Comic;
import de.wasenweg.komix.util.ZipReader;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ThumbnailReader {

    public static void set(final ZipFile file, final Comic comic)
            throws IOException {

        final List<ZipEntry> sortedEntries = ZipReader.getImages(file);

        if (sortedEntries.size() > 0) {
            comic.setThumbnail(Thumbnail.get(file.getInputStream(sortedEntries.get(0))).toByteArray());
        }
    }
}
