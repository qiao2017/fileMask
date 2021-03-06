package com.qzw.demo.filemask;

import com.qzw.filemask.component.GlobalPasswordHolder;
import com.qzw.filemask.fileencoder.FileContentEncoder;
import com.qzw.filemask.model.TailModel;
import com.qzw.filemask.service.TailModelService;
import com.qzw.filemask.util.ByteUtil;
import com.qzw.filemask.service.PasswordService;
import com.qzw.filemask.util.TestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Base64;

/**
 * @author quanzongwei
 * @date 2020/5/16
 */
public class FileContentTest {

    static String filename = "D:\\Data测试\\aaaa.txt";
    static String password = "cccccc";
    static String contentText = "中国中国";

    @Before
    public void setup() {
        TestUtil.uuid = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        File parent = new File("D:\\Data测试");
        if (!parent.exists()) {
            parent.mkdir();
        }
        File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void fileContentTest() throws IOException {
        GlobalPasswordHolder.setPassword(password);
        System.out.println("pass:" + GlobalPasswordHolder.getPassword());
        System.out.println("md51:" + base64(PasswordService.getMd51ForFileAuthentication()));
        System.out.println("md523:" + base64(PasswordService.getMd523ForContentEncrypt()));
        System.out.println("md545:" + base64(PasswordService.getMd545ForUuidEncrypt()));
        System.out.println("uuid:" + TestUtil.uuid);

        FileContentEncoder contentEncoder = new FileContentEncoder();
        File file = new File(filename);

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            System.out.println("文件长度:" + raf.length());
            raf.setLength(0);
            raf.write(contentText.getBytes("UTF-8"));
            raf.getFD().sync();
        }

        contentEncoder.executeEncrypt(file);

        System.out.println();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            TailModel model = TailModelService.getExistsTailModelInfo(raf);
            Assert.assertEquals(base64(PasswordService.getMd51ForFileAuthentication()), base64(model.getBelongUserMd516()));
            Assert.assertEquals(ByteUtil.byteToHex(model.getEncodeType16()), getHexFlagString(false, false, true));
            Assert.assertEquals(new String(model.getUuid32()), TestUtil.uuid);
            Assert.assertEquals(base64(model.getHead4()), base64(new byte[]{0, 0, 0, 0}));
            Assert.assertTrue(model.getFileNameX().length == 0);
            Assert.assertEquals(ByteUtil.bytesToLong(model.getOriginTextSize8()), contentText.getBytes("UTF-8").length);
            Assert.assertEquals(new String(model.getTailFlag16()), TailModelService.FILE_MASK_TAIL_FLAG);
            raf.getFD().sync();
        }
        //解密验证
        contentEncoder.executeDecrypt(file);
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            boolean exists = TailModelService.existsTailModel(raf);
            Assert.assertEquals(exists, false);
            Assert.assertEquals(raf.length(), contentText.getBytes("UTF-8").length);
            byte[] content = new byte[(int) raf.length()];
            raf.read(content);
            Assert.assertEquals(new String(content, "UTF-8"), new StringBuilder(contentText).toString());
            raf.getFD().sync();
        }

    }

    static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    static String getHexFlagString(boolean name, boolean head, boolean content) {
        String s1 = "00";
        if (name) {
            s1 = "01";
        }
        String s2 = "00";
        if (head) {
            s2 = "01";
        }
        String s3 = "00";
        if (content) {
            s3 = "01";
        }
        return new StringBuilder().append(s1).append(s2).append(s3).append("00000000000000000000000000").toString();
    }
}
