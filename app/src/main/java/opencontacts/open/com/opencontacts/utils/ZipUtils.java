package opencontacts.open.com.opencontacts.utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
}
