// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah;
/** Read-only synthetic test data. Never packaged in QA or release. */
public class AudioFixtureProvider extends android.content.ContentProvider {
    @Override public boolean onCreate() {
        try {
            java.io.File folder = getContext().getCacheDir();
            java.nio.ByteBuffer wave = java.nio.ByteBuffer.allocate(32044).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            wave.put(new byte[]{'R','I','F','F'}).putInt(32036).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16);
            wave.putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16);
            wave.put(new byte[]{'d','a','t','a'}).putInt(32000);
            for (int i=0;i<16000;i++) wave.putShort((short)(Math.sin(i * 2 * Math.PI * 440 / 16000) * 1000));
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(folder,"valid.wav"))) { out.write(wave.array()); }
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(folder,"invalid.bin"))) { out.write(new byte[]{1,2,3,4,5}); }
            return true;
        } catch (java.io.IOException error) { return false; }
    }
    private java.io.File file(android.net.Uri uri) throws java.io.FileNotFoundException {
        String name=uri.getLastPathSegment();
        if (!"valid.wav".equals(name) && !"invalid.bin".equals(name)) throw new java.io.FileNotFoundException();
        return new java.io.File(getContext().getCacheDir(),name);
    }
    @Override public android.os.ParcelFileDescriptor openFile(android.net.Uri uri,String mode) throws java.io.FileNotFoundException {
        if (!"r".equals(mode)) throw new java.io.FileNotFoundException();
        return android.os.ParcelFileDescriptor.open(file(uri),android.os.ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public android.database.Cursor query(android.net.Uri uri,String[] projection,String selection,String[] args,String order) {
        android.database.MatrixCursor cursor = new android.database.MatrixCursor(new String[]{android.provider.OpenableColumns.DISPLAY_NAME,android.provider.OpenableColumns.SIZE});
        try { java.io.File f=file(uri); cursor.addRow(new Object[]{f.getName(),f.length()}); } catch (java.io.FileNotFoundException ignored) { }
        return cursor;
    }
    @Override public String getType(android.net.Uri uri) { return "valid.wav".equals(uri.getLastPathSegment()) ? "audio/wav" : "application/octet-stream"; }
    @Override public android.net.Uri insert(android.net.Uri uri,android.content.ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(android.net.Uri uri,String selection,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(android.net.Uri uri,android.content.ContentValues values,String selection,String[] args) { throw new UnsupportedOperationException(); }
}
