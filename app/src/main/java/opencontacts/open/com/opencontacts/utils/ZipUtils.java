package opencontacts.open.com.opencontacts.utils;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ZipUtils {
    public static void exportZip(String password, byte[] bytesToExport, OutputStream outputStream) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, password.toCharArray());

        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setFileNameInZip("contacts.vcf");
        zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

        zipOutputStream.putNextEntry(zipParameters);
        zipOutputStream.write(bytesToExport);
        zipOutputStream.closeEntry();
        zipOutputStream.flush();
        zipOutputStream.close();
    }

    public static InputStream getPlainTextInputStreamFromZip(String password, InputStream inputStream) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(inputStream, password.toCharArray());
        zipInputStream.getNextEntry();
        return zipInputStream;
    }
}
