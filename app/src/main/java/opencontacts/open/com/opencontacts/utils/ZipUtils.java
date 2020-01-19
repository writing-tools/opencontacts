package opencontacts.open.com.opencontacts.utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ZipUtils {
    public static void exportZip(String password, byte[] bytesToExport, String filePath) throws IOException {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setFileNameInZip("contacts.vcf");
        zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

        ZipFile zipFile = new ZipFile(filePath);
        zipFile.setPassword(password.toCharArray());
        zipFile.addStream(new ByteArrayInputStream(bytesToExport), zipParameters);
    }
    public static InputStream getPlainTextInputStreamFromZip(String password, InputStream inputStream) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(inputStream, password.toCharArray());
        zipInputStream.getNextEntry();
        return zipInputStream;
    }
}
